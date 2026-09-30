package com.hyorita.balletlog.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyorita.balletlog.data.db.BalletLogDatabase
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.PhotoLog
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
    // The ClassLog behind the hardest workout, if it was a class (not a
    // placeholder) — lets the Stats card navigate to its detail.
    val hardestLog: ClassLog? = null,
    val topViewed: List<ClassLog> = emptyList(),
    val cubeData: List<Int> = emptyList(),
    val cubeLabels: List<String> = emptyList(),
    // Per-period workout-time chart values (WEEK: min/day, MONTH: avg min/week).
    val timeData: List<Int> = emptyList(),
    val monthlyClassCounts: List<Int> = emptyList(),
    val monthlyAvgMinutes: List<Int> = emptyList()
)

/**
 * 1.9: one counted activity in the stats window, from either a ClassLog or a
 * workout-bearing PhotoLog. `classLog` is set when it came from a class, so the
 * Hardest card can open its detail.
 */
internal data class StatItem(
    val date: Long,
    val durationMinutes: Int,
    val activeCalories: Int,
    val externalWorkoutId: String?,
    val classLog: ClassLog?
)

private fun ClassLog.toStatItem() = StatItem(
    date = date,
    durationMinutes = workout?.durationMinutes ?: 0,
    activeCalories = workout?.activeCalories ?: 0,
    externalWorkoutId = workout?.externalWorkoutId,
    classLog = this
)

private fun PhotoLog.toStatItem() = StatItem(
    date = date,
    durationMinutes = durationMin ?: 0,
    activeCalories = kcal ?: 0,
    externalWorkoutId = externalWorkoutId,
    classLog = null
)

/**
 * A PhotoLog that Stats counts as a workout. Narrower than
 * [PhotoLog.hasWorkoutData] on purpose — iOS ignores heart-rate-only photos
 * here, and the two platforms should report the same numbers.
 */
private val PhotoLog.isStatsWorkout: Boolean get() = kcal != null || durationMin != null

private fun dayKey(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/**
 * 1.16: every counted "class" — iOS `classEventDates`. Every ClassLog counts
 * (two the same day = two, workout or not). A photo workout counts only if it
 * isn't already one of those classes: by session id when it has one, otherwise
 * at most once per day and never on a day that already has a class.
 * Callers pass logs already filtered to the range they want.
 */
internal fun classEvents(logs: List<ClassLog>, photos: List<PhotoLog>): List<StatItem> {
    val events = logs.mapTo(ArrayList()) { it.toStatItem() }
    val seenIds = logs.mapNotNullTo(HashSet()) { it.workout?.externalWorkoutId }
    val coveredDays = logs.mapTo(HashSet()) { dayKey(it.date) }
    val photoWorkouts = photos.filter { it.isStatsWorkout }
    for (photo in photoWorkouts) {
        val id = photo.externalWorkoutId ?: continue
        if (!seenIds.add(id)) continue
        coveredDays.add(dayKey(photo.date))
        events.add(photo.toStatItem())
    }
    for (photo in photoWorkouts) {
        if (photo.externalWorkoutId != null) continue
        if (!coveredDays.add(dayKey(photo.date))) continue
        events.add(photo.toStatItem())
    }
    return events
}

/**
 * 1.16: deduped workout sessions for time/kcal totals — iOS `workoutSessions`.
 * Identified workouts dedupe by session id (distinct same-day sessions each
 * count); unidentified ones fall back to one per day, skipping any day an
 * identified workout already covers. ClassLogs go first so they win a tie.
 */
internal fun workoutSessions(logs: List<ClassLog>, photos: List<PhotoLog>): List<StatItem> {
    val entries = logs.filter { it.workout != null }.map { it.toStatItem() } +
        photos.filter { it.isStatsWorkout }.map { it.toStatItem() }
    val sessions = ArrayList<StatItem>()
    val seenIds = HashSet<String>()
    val coveredDays = HashSet<Long>()
    for (e in entries) {
        val id = e.externalWorkoutId ?: continue
        if (!seenIds.add(id)) continue
        coveredDays.add(dayKey(e.date))
        sessions.add(e)
    }
    for (e in entries) {
        if (e.externalWorkoutId != null) continue
        if (!coveredDays.add(dayKey(e.date))) continue
        sessions.add(e)
    }
    return sessions.sortedBy { it.date }
}

/**
 * Highest-kcal workout across class and photo workouts, no dedupe (a max
 * doesn't need it). ClassLogs come first so they win a tie — the richer detail.
 */
internal fun hardestWorkout(logs: List<ClassLog>, photos: List<PhotoLog>): StatItem? =
    (logs.filter { it.workout != null }.map { it.toStatItem() } +
        photos.filter { it.kcal != null }.map { it.toStatItem() })
        .filter { it.activeCalories > 0 }
        .maxByOrNull { it.activeCalories }

private data class StatSources(
    val events: List<StatItem>,
    val sessions: List<StatItem>,
    val hardest: StatItem?
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

    // 1.16: counted classes, deduped workout sessions and the hardest workout
    // for the period — same rules as iOS (see classEvents / workoutSessions).
    private val filteredSources: StateFlow<StatSources> = combine(
        allLogs, allPhotoLogs, selectedPeriod, periodOffset
    ) { logs, photos, period, offset ->
        val (start, end) = computeRange(period, offset)
        val periodLogs = logs.filter { it.date in start until end }
        val periodPhotos = photos.filter { it.date in start until end }
        StatSources(
            events = classEvents(periodLogs, periodPhotos),
            sessions = workoutSessions(periodLogs, periodPhotos),
            hardest = hardestWorkout(periodLogs, periodPhotos)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatSources(emptyList(), emptyList(), null))

    val aggregates: StateFlow<StatsAggregates> = combine(
        filteredSources, filteredLogs, selectedPeriod
    ) { sources, logs, period ->
        computeAggregates(sources, logs, period)
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
        sources: StatSources,
        classLogs: List<ClassLog>,
        period: StatsPeriod
    ): StatsAggregates {
        val events = sources.events
        val sessions = sources.sessions
        val totalMinutes = sessions.sumOf { it.durationMinutes }
        val totalCalories = sessions.sumOf { it.activeCalories }
        val hardest = sources.hardest
        // Top viewed stays ClassLog-only — placeholders have no view count.
        val topViewed = classLogs.filter { it.viewCount > 0 }
            .sortedByDescending { it.viewCount }
            .take(5)

        val (cubeData, cubeLabels) = buildCubeData(events, period)
        val timeData = if (period == StatsPeriod.YEAR) emptyList() else buildTimeData(sessions, period)
        val monthlyClassCounts = if (period == StatsPeriod.YEAR) buildMonthlyCounts(events) else emptyList()
        val monthlyAvgMinutes = if (period == StatsPeriod.YEAR) buildMonthlyAvgMinutes(sessions) else emptyList()

        return StatsAggregates(
            totalClasses = events.size,
            totalMinutes = totalMinutes,
            totalCalories = totalCalories,
            hardestCalories = hardest?.activeCalories,
            hardestDate = hardest?.date,
            hardestLog = hardest?.classLog,
            topViewed = topViewed,
            cubeData = cubeData,
            cubeLabels = cubeLabels,
            timeData = timeData,
            monthlyClassCounts = monthlyClassCounts,
            monthlyAvgMinutes = monthlyAvgMinutes
        )
    }

    /**
     * 1.9: workout-time chart values for WEEK (total minutes per weekday) and
     * MONTH (average minutes per week-of-month). YEAR uses monthlyAvgMinutes.
     */
    private fun buildTimeData(items: List<StatItem>, period: StatsPeriod): List<Int> {
        return when (period) {
            StatsPeriod.WEEK -> {
                val sums = IntArray(7)
                items.forEach { item ->
                    if (item.durationMinutes > 0) {
                        val cal = Calendar.getInstance().also { it.timeInMillis = item.date }
                        sums[cal.get(Calendar.DAY_OF_WEEK) - 1] += item.durationMinutes
                    }
                }
                sums.toList()
            }
            StatsPeriod.MONTH -> {
                val sum = IntArray(5)
                val count = IntArray(5)
                items.forEach { item ->
                    if (item.durationMinutes > 0) {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = item.date
                            firstDayOfWeek = Calendar.SUNDAY
                            minimalDaysInFirstWeek = 1
                        }
                        val w = (cal.get(Calendar.WEEK_OF_MONTH) - 1).coerceIn(0, 4)
                        sum[w] += item.durationMinutes
                        count[w]++
                    }
                }
                (0 until 5).map { if (count[it] > 0) sum[it] / count[it] else 0 }
            }
            StatsPeriod.YEAR -> emptyList()
        }
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
