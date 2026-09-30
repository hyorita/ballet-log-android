package com.hyorita.balletlog.ui.stats

import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.PhotoLog
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToLong

/*
 * Stats rules, kept free of Android/Compose so they can be pinned to iOS
 * `StatsView.swift` (1.16) by unit tests. Everything here takes plain lists
 * already filtered to whatever range the caller wants.
 */

enum class StatsPeriod { WEEK, MONTH, YEAR }

/**
 * 1.9: one counted activity in the stats window, from either a ClassLog or a
 * workout-bearing PhotoLog. The source record rides along so the Hardest cell
 * can open it — class detail or the photo viewer.
 */
internal data class StatItem(
    val date: Long,
    val durationMinutes: Int,
    val activeCalories: Int,
    val externalWorkoutId: String?,
    val classLog: ClassLog?,
    val photoLog: PhotoLog? = null
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
    classLog = null,
    photoLog = this
)

/**
 * A PhotoLog that Stats counts as a workout. Narrower than
 * [PhotoLog.hasWorkoutData] on purpose — iOS ignores heart-rate-only photos
 * here, and the two platforms should report the same numbers.
 */
private val PhotoLog.isStatsWorkout: Boolean get() = kcal != null || durationMin != null

// region Days

internal fun dayKey(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Calendar-day arithmetic — not `± 86_400_000`, which drifts across DST. */
internal fun addDays(day: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = day
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis.let(::dayKey)

/** Whole calendar days from [fromDay] to [toDay], DST-safe. */
internal fun daysBetween(fromDay: Long, toDay: Long): Int =
    ((dayKey(toDay) - dayKey(fromDay)) / 86_400_000.0).roundToLong().toInt()

// endregion

// region Counting

/**
 * 1.16: every counted "class" — iOS `classEventDates`. Every ClassLog counts
 * (two the same day = two, workout or not). A photo workout counts only if it
 * isn't already one of those classes: by session id when it has one, otherwise
 * at most once per day and never on a day that already has a class.
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

/**
 * By studio: tag slot 0 across class *and* photo logs in the period. A class
 * tagged on both a ClassLog and a same-day PhotoLog counts twice — iOS accepts
 * that approximation for a secondary breakdown; so do we.
 */
internal fun studioBreakdown(logs: List<ClassLog>, photos: List<PhotoLog>): List<Pair<String, Int>> {
    val counts = HashMap<String, Int>()
    (logs.map { it.tags } + photos.map { it.tags }).forEach { tags ->
        val studio = tags.getOrNull(0)
        if (!studio.isNullOrBlank()) counts[studio] = (counts[studio] ?: 0) + 1
    }
    return counts.entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key to it.value }
}

// endregion

// region Streak

/**
 * Not period-scoped — a streak is always "as of now", read across all history.
 * [startDay] is the first day of the run (for "record since …"), [mostRecentDay]
 * the day the count starts from (for "last class …").
 */
internal data class Streak(
    val current: Int,
    val longest: Int,
    val startDay: Long?,
    val mostRecentDay: Long?
) {
    val isNewRecord: Boolean get() = current > 0 && current == longest

    companion object {
        val NONE = Streak(0, 0, null, null)
    }
}

/**
 * [classDays] are `dayKey`s of every counted class ([classEvents], unbounded).
 * Counts back from today — or from yesterday when today has no class yet, so an
 * in-progress streak doesn't read as broken before the day is even over.
 */
internal fun computeStreak(classDays: Set<Long>, now: Long): Streak {
    if (classDays.isEmpty()) return Streak.NONE
    val longest = longestStreak(classDays)
    var cursor = dayKey(now)
    if (cursor !in classDays) {
        cursor = addDays(cursor, -1)
        if (cursor !in classDays) return Streak(0, longest, null, null)
    }
    val mostRecent = cursor
    var count = 0
    var start = cursor
    while (cursor in classDays) {
        count++
        start = cursor
        cursor = addDays(cursor, -1)
    }
    return Streak(count, longest, start, mostRecent)
}

private fun longestStreak(classDays: Set<Long>): Int {
    val sorted = classDays.sorted()
    var longest = 1
    var run = 1
    for (i in 1 until sorted.size) {
        run = if (addDays(sorted[i - 1], 1) == sorted[i]) run + 1 else 1
        longest = maxOf(longest, run)
    }
    return longest
}

/** The one-line hint under the streak — four states, first match wins. */
internal sealed interface StreakHint {
    data object StartNew : StreakHint
    data class RecordSince(val startDay: Long) : StreakHint
    data object OneMoreTies : StreakHint
    data class LastClass(val day: Long) : StreakHint
}

internal fun streakHint(streak: Streak): StreakHint = when {
    streak.current == 0 -> StreakHint.StartNew
    streak.current >= streak.longest -> StreakHint.RecordSince(streak.startDay!!)
    streak.current == streak.longest - 1 -> StreakHint.OneMoreTies
    else -> StreakHint.LastClass(streak.mostRecentDay!!)
}

// endregion

// region Trend chart

internal data class ChartBar(val label: String, val value: Double)

/**
 * One bar per slot. Week plots workout *minutes* per day (a class-count chart
 * is meaningless at 7-day resolution); Month and Year plot class counts.
 * Month slots are days 1–7, 8–14, … (`W1`…), not calendar weeks — same as iOS.
 */
internal fun chartBars(
    period: StatsPeriod,
    rangeStart: Long,
    events: List<StatItem>,
    sessions: List<StatItem>,
    locale: Locale
): List<ChartBar> {
    val cal = Calendar.getInstance()
    return when (period) {
        StatsPeriod.WEEK -> (0 until 7).map { i ->
            val day = addDays(rangeStart, i)
            val minutes = sessions.filter { dayKey(it.date) == day }.sumOf { it.durationMinutes }
            ChartBar(weekdayLetter(day, locale), minutes.toDouble())
        }
        StatsPeriod.MONTH -> {
            cal.timeInMillis = rangeStart
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val weeks = ceil(daysInMonth / 7.0).toInt()
            val counts = IntArray(weeks)
            events.forEach {
                cal.timeInMillis = it.date
                counts[((cal.get(Calendar.DAY_OF_MONTH) - 1) / 7).coerceAtMost(weeks - 1)]++
            }
            counts.mapIndexed { i, c -> ChartBar("W${i + 1}", c.toDouble()) }
        }
        StatsPeriod.YEAR -> {
            val counts = IntArray(12)
            events.forEach {
                cal.timeInMillis = it.date
                counts[cal.get(Calendar.MONTH)]++
            }
            counts.mapIndexed { m, c ->
                cal.timeInMillis = rangeStart
                cal.set(Calendar.MONTH, m)
                ChartBar(monthLetter(cal.timeInMillis, locale), c.toDouble())
            }
        }
    }
}

/**
 * How many bars have actually happened. A past period is entirely elapsed;
 * the live one stops at the bar containing today — the rest render as grey
 * placeholders, and the average divides by this, not by the bar count, so a
 * W5 that hasn't arrived yet doesn't dilute it.
 */
internal fun chartElapsedCount(
    period: StatsPeriod,
    offset: Int,
    rangeStart: Long,
    barCount: Int,
    now: Long
): Int {
    if (offset != 0) return barCount
    val days = daysBetween(rangeStart, now)
    return when (period) {
        StatsPeriod.WEEK -> (days + 1).coerceIn(1, barCount)
        StatsPeriod.MONTH -> (days / 7 + 1).coerceAtMost(barCount)
        StatsPeriod.YEAR -> Calendar.getInstance().apply { timeInMillis = now }
            .get(Calendar.MONTH).plus(1).coerceIn(1, barCount)
    }
}

internal fun chartAverage(bars: List<ChartBar>, elapsed: Int): Double {
    val n = elapsed.coerceAtLeast(1)
    return bars.take(n).sumOf { it.value } / n
}

private fun weekdayLetter(day: Long, locale: Locale): String =
    SimpleDateFormat("E", locale).format(Date(day)).take(1)

/**
 * The year chart packs 12 labels into one row, so one letter (J F M …) —
 * except locales whose month names start with a number (ko `10월`, ja `10月`),
 * where the first character alone would turn Oct/Nov/Dec into three `1`s.
 */
internal fun monthLetter(monthStart: Long, locale: Locale): String {
    val full = SimpleDateFormat("MMM", locale).format(Date(monthStart))
    return if (full.firstOrNull()?.isDigit() == true) full.takeWhile { it.isDigit() }
    else full.take(1)
}

// endregion

// region Metric formatting

/** Big number + small unit, or a quiet "—" when there's genuinely no data. */
internal data class MetricValue(val big: String, val unit: String?, val isEmpty: Boolean)

internal val EMPTY_METRIC = MetricValue("—", null, true)

internal fun workoutTimeValue(totalMinutes: Int): MetricValue = when {
    totalMinutes <= 0 -> EMPTY_METRIC
    totalMinutes >= 60 -> MetricValue(
        "${totalMinutes / 60}h",
        (totalMinutes % 60).takeIf { it > 0 }?.let { "${it}m" },
        false
    )
    else -> MetricValue("$totalMinutes", "min", false)
}

internal fun formatKcal(kcal: Int, locale: Locale): String =
    NumberFormat.getIntegerInstance(locale).format(kcal)

internal fun kcalValue(kcal: Int, locale: Locale): MetricValue =
    if (kcal <= 0) EMPTY_METRIC else MetricValue(formatKcal(kcal, locale), "kcal", false)

// endregion
