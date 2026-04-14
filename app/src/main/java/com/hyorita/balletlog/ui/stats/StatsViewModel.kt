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
    val hardestClass: ClassLog? = null,
    val topViewed: List<ClassLog> = emptyList(),
    val cubeData: List<Int> = emptyList(),
    val cubeLabels: List<String> = emptyList(),
    val monthlyClassCounts: List<Int> = emptyList(),
    val monthlyAvgMinutes: List<Int> = emptyList()
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = BalletLogDatabase.getInstance(app).classLogDao()

    val selectedPeriod = MutableStateFlow(StatsPeriod.WEEK)
    val periodOffset = MutableStateFlow(0)

    private val allLogs = dao.getAll().stateIn(
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

    val aggregates: StateFlow<StatsAggregates> = combine(
        filteredLogs, selectedPeriod
    ) { logs, period ->
        computeAggregates(logs, period)
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

    private fun computeAggregates(logs: List<ClassLog>, period: StatsPeriod): StatsAggregates {
        val workouts = logs.mapNotNull { it.workout }
        val totalMinutes = workouts.sumOf { it.durationMinutes }
        val totalCalories = workouts.sumOf { it.activeCalories }
        val hardest = logs.filter { it.workout != null }
            .maxByOrNull { it.workout?.activeCalories ?: 0 }
        val topViewed = logs.filter { it.viewCount > 0 }
            .sortedByDescending { it.viewCount }
            .take(5)

        val (cubeData, cubeLabels) = buildCubeData(logs, period)
        val monthlyClassCounts = if (period == StatsPeriod.YEAR) buildMonthlyCounts(logs) else emptyList()
        val monthlyAvgMinutes = if (period == StatsPeriod.YEAR) buildMonthlyAvgMinutes(logs) else emptyList()

        return StatsAggregates(
            totalClasses = logs.size,
            totalMinutes = totalMinutes,
            totalCalories = totalCalories,
            hardestClass = hardest,
            topViewed = topViewed,
            cubeData = cubeData,
            cubeLabels = cubeLabels,
            monthlyClassCounts = monthlyClassCounts,
            monthlyAvgMinutes = monthlyAvgMinutes
        )
    }

    private fun buildCubeData(
        logs: List<ClassLog>,
        period: StatsPeriod
    ): Pair<List<Int>, List<String>> {
        return when (period) {
            StatsPeriod.WEEK -> {
                val counts = IntArray(7)
                logs.forEach { log ->
                    val cal = Calendar.getInstance().also { it.timeInMillis = log.date }
                    val dow = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
                    counts[dow]++
                }
                counts.toList() to listOf("S", "M", "T", "W", "T", "F", "S")
            }
            StatsPeriod.MONTH -> {
                val counts = IntArray(5)
                logs.forEach { log ->
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = log.date
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
                logs.forEach { log ->
                    val cal = Calendar.getInstance().also { it.timeInMillis = log.date }
                    counts[cal.get(Calendar.MONTH)]++
                }
                counts.toList() to listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
            }
        }
    }

    private fun buildMonthlyCounts(logs: List<ClassLog>): List<Int> {
        val counts = IntArray(12)
        logs.forEach {
            val cal = Calendar.getInstance().also { c -> c.timeInMillis = it.date }
            counts[cal.get(Calendar.MONTH)]++
        }
        return counts.toList()
    }

    private fun buildMonthlyAvgMinutes(logs: List<ClassLog>): List<Int> {
        val sum = IntArray(12)
        val count = IntArray(12)
        logs.forEach { log ->
            log.workout?.let { w ->
                val cal = Calendar.getInstance().also { c -> c.timeInMillis = log.date }
                val m = cal.get(Calendar.MONTH)
                sum[m] += w.durationMinutes
                count[m]++
            }
        }
        return (0 until 12).map { if (count[it] > 0) sum[it] / count[it] else 0 }
    }
}
