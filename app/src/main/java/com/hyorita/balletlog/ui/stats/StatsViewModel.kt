package com.hyorita.balletlog.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyorita.balletlog.data.db.BalletLogDatabase
import com.hyorita.balletlog.data.model.ClassLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class StatsPeriod { WEEK, MONTH, YEAR }

data class StatsAggregates(
    val totalClasses: Int = 0,
    val totalMinutes: Int = 0,
    val totalCalories: Int = 0,
    // 1.9: hardest workout (was "hardest class") — held as plain values since
    // the source may be a ClassLog workout or an imported PhotoLog placeholder.
    val hardestCalories: Int? = null,
    val hardestDate: Long? = null,
    val topViewed: List<ClassLog> = emptyList(),
    val cubeData: List<Int> = emptyList(),
    val cubeLabels: List<String> = emptyList(),
    val monthlyClassCounts: List<Int> = emptyList(),
    val monthlyAvgMinutes: List<Int> = emptyList()
)

/**
 * 1.9: one counted activity in the stats window. Built from both ClassLogs and
 * workout-bearing PhotoLog placeholders, then deduped by [externalWorkoutId] so
 * the same Health Connect session logged twice counts once. `isClassLog` lets a
 * ClassLog win the identity tie (it's the richer record).
 */
private data class StatItem(
    val date: Long,
    val durationMinutes: Int,
    val activeCalories: Int,
    val externalWorkoutId: String?,
    val isClassLog: Boolean
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BalletLogDatabase.getInstance(app)
    private val dao = db.classLogDao()
    private val photoDao = db.photoLogDao()

    val selectedPeriod = MutableStateFlow(StatsPeriod.WEEK)
    val periodOffset = MutableStateFlow(0)

    private val allLogs = dao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val allPhotoLogs = photoDao.getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredLogs: StateFlow<List<ClassLog>> = combine(
        allLogs, selectedPeriod, periodOffset
    ) { logs, period, offset ->
        val (start, end) = computeRange(period, offset)
        logs.filter { it.date in start until end }
            .sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deduped counted activities (ClassLog workouts + imported placeholders).
    private val filteredItems: StateFlow<List<StatItem>> = combine(
        allLogs, allPhotoLogs, selectedPeriod, periodOffset
    ) { logs, photos, period, offset ->
        val (start, end) = computeRange(period, offset)
        val classItems = logs.filter { it.date in start until end }.map { log ->
            StatItem(
                date = log.date,
                durationMinutes = log.workout?.durationMinutes ?: 0,
                activeCalories = log.workout?.activeCalories ?: 0,
                externalWorkoutId = log.workout?.externalWorkoutId,
                isClassLog = true
            )
        }
        val photoItems = photos
            .filter { it.date in start until end && it.hasWorkoutData }
            .map { p ->
                StatItem(
                    date = p.date,
                    durationMinutes = p.durationMin ?: 0,
                    activeCalories = p.kcal ?: 0,
                    externalWorkoutId = p.externalWorkoutId,
                    isClassLog = false
                )
            }
        dedupeByIdentity(classItems + photoItems)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aggregates: StateFlow<StatsAggregates> = combine(
        filteredItems, filteredLogs, selectedPeriod
    ) { items, logs, period ->
        computeAggregates(items, logs, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsAggregates())

    val periodLabel: StateFlow<String> = combine(selectedPeriod, periodOffset) { p, o ->
        formatPeriodLabel(p, o)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val canGoForward: StateFlow<Boolean> = periodOffset
        .map { it < 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setPeriod(p: StatsPeriod) {
        selectedPeriod.value = p
        periodOffset.value = 0
    }

    fun previousPeriod() {
        periodOffset.value -= 1
    }

    fun nextPeriod() {
        if (periodOffset.value < 0) periodOffset.value += 1
    }

    /**
     * 1.9: open stats anchored to a specific month (the one being viewed in
     * History), mirroring iOS's referenceDate. Clamped to not point at a future
     * month.
     */
    fun showMonth(year: Int, month: Int) {
        val now = Calendar.getInstance()
        val offset = (year - now.get(Calendar.YEAR)) * 12 + (month - now.get(Calendar.MONTH))
        selectedPeriod.value = StatsPeriod.MONTH
        periodOffset.value = offset.coerceAtMost(0)
    }

    private fun dedupeByIdentity(items: List<StatItem>): List<StatItem> {
        val seen = HashSet<String>()
        val out = ArrayList<StatItem>(items.size)
        // ClassLog items first so they win an externalId tie over a placeholder.
        items.sortedByDescending { it.isClassLog }.forEach { item ->
            val id = item.externalWorkoutId
            if (id == null || seen.add(id)) out.add(item)
        }
        return out
    }

    private fun computeRange(period: StatsPeriod, offset: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return when (period) {
            StatsPeriod.WEEK -> {
                cal.firstDayOfWeek = Calendar.SUNDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                cal.add(Calendar.WEEK_OF_YEAR, offset)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 7)
                start to cal.timeInMillis
            }
            StatsPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, offset)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                start to cal.timeInMillis
            }
            StatsPeriod.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.add(Calendar.YEAR, offset)
                val start = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                start to cal.timeInMillis
            }
        }
    }

    private fun formatPeriodLabel(period: StatsPeriod, offset: Int): String {
        val (start, end) = computeRange(period, offset)
        return when (period) {
            StatsPeriod.WEEK -> {
                val df = SimpleDateFormat("MMM d", Locale.getDefault())
                "${df.format(Date(start))} – ${df.format(Date(end - 1))}"
            }
            StatsPeriod.MONTH ->
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(start))
            StatsPeriod.YEAR ->
                SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(start))
        }
    }

    private fun computeAggregates(
        items: List<StatItem>,
        classLogs: List<ClassLog>,
        period: StatsPeriod
    ): StatsAggregates {
        val totalMinutes = items.sumOf { it.durationMinutes }
        val totalCalories = items.sumOf { it.activeCalories }
        val hardest = items.filter { it.activeCalories > 0 }.maxByOrNull { it.activeCalories }
        // Top viewed stays ClassLog-only — placeholders have no view count.
        val topViewed = classLogs.filter { it.viewCount > 0 }
            .sortedByDescending { it.viewCount }
            .take(5)

        val (cubeData, cubeLabels) = buildCubeData(items, period)
        val monthlyClassCounts = if (period == StatsPeriod.YEAR) buildMonthlyCounts(items) else emptyList()
        val monthlyAvgMinutes = if (period == StatsPeriod.YEAR) buildMonthlyAvgMinutes(items) else emptyList()

        return StatsAggregates(
            totalClasses = items.size,
            totalMinutes = totalMinutes,
            totalCalories = totalCalories,
            hardestCalories = hardest?.activeCalories,
            hardestDate = hardest?.date,
            topViewed = topViewed,
            cubeData = cubeData,
            cubeLabels = cubeLabels,
            monthlyClassCounts = monthlyClassCounts,
            monthlyAvgMinutes = monthlyAvgMinutes
        )
    }

    private fun buildCubeData(
        items: List<StatItem>,
        period: StatsPeriod
    ): Pair<List<Int>, List<String>> {
        return when (period) {
            StatsPeriod.WEEK -> {
                val counts = IntArray(7)
                items.forEach { item ->
                    val cal = Calendar.getInstance().also { it.timeInMillis = item.date }
                    val dow = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
                    counts[dow]++
                }
                counts.toList() to listOf("S", "M", "T", "W", "T", "F", "S")
            }
            StatsPeriod.MONTH -> {
                val counts = IntArray(5)
                items.forEach { item ->
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = item.date
                        firstDayOfWeek = Calendar.SUNDAY
                        minimalDaysInFirstWeek = 1
                    }
                    val w = (cal.get(Calendar.WEEK_OF_MONTH) - 1).coerceIn(0, 4)
                    counts[w]++
                }
                counts.toList() to listOf("W1", "W2", "W3", "W4", "W5")
            }
            StatsPeriod.YEAR -> {
                val counts = IntArray(12)
                items.forEach { item ->
                    val cal = Calendar.getInstance().also { it.timeInMillis = item.date }
                    counts[cal.get(Calendar.MONTH)]++
                }
                counts.toList() to listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
            }
        }
    }

    private fun buildMonthlyCounts(items: List<StatItem>): List<Int> {
        val counts = IntArray(12)
        items.forEach {
            val cal = Calendar.getInstance().also { c -> c.timeInMillis = it.date }
            counts[cal.get(Calendar.MONTH)]++
        }
        return counts.toList()
    }

    private fun buildMonthlyAvgMinutes(items: List<StatItem>): List<Int> {
        val sum = IntArray(12)
        val count = IntArray(12)
        items.forEach { item ->
            if (item.durationMinutes > 0) {
                val cal = Calendar.getInstance().also { c -> c.timeInMillis = item.date }
                val m = cal.get(Calendar.MONTH)
                sum[m] += item.durationMinutes
                count[m]++
            }
        }
        return (0 until 12).map { if (count[it] > 0) sum[it] / count[it] else 0 }
    }
}
