package com.hyorita.balletlog.ui.stats

import android.app.Application
import android.text.format.DateFormat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyorita.balletlog.data.db.BalletLogDatabase
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.PhotoLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Everything the Stats tab (and its share image) draws, frozen together. */
internal data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.MONTH,
    val offset: Int = 0,
    val periodLabel: String = "",
    val streak: Streak = Streak.NONE,
    val streakKcal: Int = 0,
    val totalClasses: Int = 0,
    val totalMinutes: Int = 0,
    val totalCalories: Int = 0,
    val hardest: StatItem? = null,
    val bars: List<ChartBar> = emptyList(),
    val elapsed: Int = 0,
    val studios: List<Pair<String, Int>> = emptyList(),
    // iOS shows its empty state off ClassLogs in the period, not the total —
    // a month of photo-only workouts still reads "No classes yet".
    val hasClassLogs: Boolean = false
) {
    val canGoForward: Boolean get() = offset < 0
    /** The bar holding today; -1 when browsing a past period. */
    val currentIndex: Int get() = if (offset == 0) elapsed - 1 else -1
}

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BalletLogDatabase.getInstance(app)

    // Month first, as on iOS — a week of one or two classes is too thin to
    // open on.
    private val selectedPeriod = MutableStateFlow(StatsPeriod.MONTH)
    private val periodOffset = MutableStateFlow(0)

    private val allLogs = db.classLogDao().getAll()
    private val allPhotoLogs = db.photoLogDao().getAll()

    internal val state: StateFlow<StatsUiState> = combine(
        allLogs, allPhotoLogs, selectedPeriod, periodOffset
    ) { logs, photos, period, offset ->
        buildState(logs, photos, period, offset, System.currentTimeMillis())
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

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

    private fun buildState(
        logs: List<ClassLog>,
        photos: List<PhotoLog>,
        period: StatsPeriod,
        offset: Int,
        now: Long
    ): StatsUiState {
        val locale = Locale.getDefault()
        val (start, end) = computeRange(period, offset, now)
        val periodLogs = logs.filter { it.date in start until end }
        val periodPhotos = photos.filter { it.date in start until end }
        val events = classEvents(periodLogs, periodPhotos)
        val sessions = workoutSessions(periodLogs, periodPhotos)
        val bars = chartBars(period, start, events, sessions, locale)

        // Streak reads all history, independent of the period being browsed.
        val classDays = classEvents(logs, photos).mapTo(HashSet()) { dayKey(it.date) }
        val streak = computeStreak(classDays, now)
        val streakKcal = if (streak.current > 0) {
            val from = streak.startDay!!
            val until = addDays(streak.mostRecentDay!!, 1)
            workoutSessions(
                logs.filter { it.date in from until until },
                photos.filter { it.date in from until until }
            ).sumOf { it.activeCalories }
        } else 0

        return StatsUiState(
            period = period,
            offset = offset,
            periodLabel = formatPeriodLabel(period, start, end, locale),
            streak = streak,
            streakKcal = streakKcal,
            totalClasses = events.size,
            totalMinutes = sessions.sumOf { it.durationMinutes },
            totalCalories = sessions.sumOf { it.activeCalories },
            hardest = hardestWorkout(periodLogs, periodPhotos),
            bars = bars,
            elapsed = chartElapsedCount(period, offset, start, bars.size, now),
            studios = studioBreakdown(periodLogs, periodPhotos),
            hasClassLogs = periodLogs.isNotEmpty()
        )
    }

    /**
     * Week is the Sunday-start calendar week containing today — not iOS's
     * rolling last 7 days. Android has always labelled it "This week"; the
     * trend chart's elapsed/placeholder handling covers the days still ahead.
     */
    private fun computeRange(period: StatsPeriod, offset: Int, now: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayKey(now)
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

    /** Locale skeletons, as iOS does — ko reads `2026년 5월`, not `5월 2026`. */
    private fun formatPeriodLabel(period: StatsPeriod, start: Long, end: Long, locale: Locale): String {
        fun fmt(skeleton: String, millis: Long) =
            SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(Date(millis))
        return when (period) {
            StatsPeriod.WEEK -> {
                val last = addDays(end, -1)
                val sameMonth = Calendar.getInstance().run {
                    timeInMillis = start
                    val y = get(Calendar.YEAR)
                    val m = get(Calendar.MONTH)
                    timeInMillis = last
                    y == get(Calendar.YEAR) && m == get(Calendar.MONTH)
                }
                val tail = if (sameMonth) fmt("d", last) else fmt("MMMd", last)
                "${fmt("MMMd", start)} – $tail"
            }
            StatsPeriod.MONTH -> fmt("yMMMM", start)
            StatsPeriod.YEAR -> fmt("y", start)
        }
    }
}
