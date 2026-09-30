package com.hyorita.balletlog

import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.PhotoLog
import com.hyorita.balletlog.ui.stats.ChartBar
import com.hyorita.balletlog.ui.stats.StatsPeriod
import com.hyorita.balletlog.ui.stats.StreakHint
import com.hyorita.balletlog.ui.stats.chartAverage
import com.hyorita.balletlog.ui.stats.chartBars
import com.hyorita.balletlog.ui.stats.chartElapsedCount
import com.hyorita.balletlog.ui.stats.classEvents
import com.hyorita.balletlog.ui.stats.computeStreak
import com.hyorita.balletlog.ui.stats.dayKey
import com.hyorita.balletlog.ui.stats.monthLetter
import com.hyorita.balletlog.ui.stats.streakHint
import com.hyorita.balletlog.ui.stats.studioBreakdown
import com.hyorita.balletlog.ui.stats.workoutTimeValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

/**
 * 1.16 Stats redesign — streak, trend chart and metric formatting, pinned to
 * iOS StatsView.swift. These fail quietly on a device (a wrong number, a
 * mislabelled bar), so they're checked here instead.
 */
class StatsMathTest {

    private fun day(month: Int, d: Int, hour: Int = 10): Long = Calendar.getInstance().apply {
        clear()
        set(2026, month, d, hour, 0)
    }.timeInMillis

    private fun days(vararg ds: Int, month: Int = Calendar.SEPTEMBER) = ds.map { dayKey(day(month, it)) }.toSet()

    private val now = day(Calendar.SEPTEMBER, 30, 16) // Wed 2026-09-30

    // region Streak

    @Test fun streak_countsBackFromToday() {
        val s = computeStreak(days(28, 29, 30), now)
        assertEquals(3, s.current)
        assertEquals(dayKey(day(Calendar.SEPTEMBER, 28)), s.startDay)
        assertEquals(dayKey(now), s.mostRecentDay)
    }

    @Test fun streak_noClassTodayYet_startsFromYesterday() {
        val s = computeStreak(days(27, 28, 29), now)
        assertEquals(3, s.current)
        assertEquals(dayKey(day(Calendar.SEPTEMBER, 29)), s.mostRecentDay)
    }

    @Test fun streak_brokenByYesterday_isZero_butLongestSurvives() {
        val s = computeStreak(days(20, 21, 22, 23, 28), now)
        assertEquals(0, s.current)
        assertEquals(4, s.longest)
        assertEquals(StreakHint.StartNew, streakHint(s))
    }

    @Test fun streak_empty() {
        val s = computeStreak(emptySet(), now)
        assertEquals(0, s.current)
        assertEquals(0, s.longest)
    }

    @Test fun streak_crossesMonthBoundary() {
        val set = setOf(dayKey(day(Calendar.AUGUST, 31))) + days(1, 2, month = Calendar.SEPTEMBER)
        val s = computeStreak(set, day(Calendar.SEPTEMBER, 2, 9))
        assertEquals(3, s.current)
    }

    @Test fun hint_currentIsLongest_isRecordSinceStart_andNewRecord() {
        val s = computeStreak(days(29, 30), now)
        assertTrue(s.isNewRecord)
        assertEquals(StreakHint.RecordSince(dayKey(day(Calendar.SEPTEMBER, 29))), streakHint(s))
    }

    @Test fun hint_oneShortOfRecord() {
        val s = computeStreak(days(10, 11, 12, 29, 30), now)
        assertEquals(StreakHint.OneMoreTies, streakHint(s))
        assertFalse(s.isNewRecord)
    }

    @Test fun hint_wellShortOfRecord_isLastClass() {
        val s = computeStreak(days(10, 11, 12, 13, 29), now)
        assertEquals(StreakHint.LastClass(dayKey(day(Calendar.SEPTEMBER, 29))), streakHint(s))
    }

    // endregion

    // region Trend chart

    @Test fun month_bucketsByDayOfMonth_notCalendarWeek() {
        // 2026-05-07 is a Thursday in the 2nd calendar week, but days 1–7 are W1.
        val may1 = day(Calendar.MAY, 1, 0)
        val events = classEvents(listOf(ClassLog.create(date = day(Calendar.MAY, 7))), emptyList())
        val bars = chartBars(StatsPeriod.MONTH, dayKey(may1), events, emptyList(), Locale.US)
        assertEquals(5, bars.size) // 31 days → ceil(31/7)
        assertEquals(listOf(1.0, 0.0, 0.0, 0.0, 0.0), bars.map { it.value })
        assertEquals("W1", bars[0].label)
    }

    @Test fun month_february_hasFourBars() {
        val feb1 = dayKey(day(Calendar.FEBRUARY, 1, 0))
        assertEquals(4, chartBars(StatsPeriod.MONTH, feb1, emptyList(), emptyList(), Locale.US).size)
    }

    @Test fun elapsed_currentMonth_stopsAtTodaysBucket() {
        val sep1 = dayKey(day(Calendar.SEPTEMBER, 1, 0))
        assertEquals(5, chartElapsedCount(StatsPeriod.MONTH, 0, sep1, 5, now)) // 30th → W5
        assertEquals(2, chartElapsedCount(StatsPeriod.MONTH, 0, sep1, 5, day(Calendar.SEPTEMBER, 8)))
        assertEquals(5, chartElapsedCount(StatsPeriod.MONTH, -1, sep1, 5, day(Calendar.SEPTEMBER, 8)))
    }

    @Test fun elapsed_currentWeekAndYear() {
        val sun = dayKey(day(Calendar.SEPTEMBER, 27, 0))
        assertEquals(4, chartElapsedCount(StatsPeriod.WEEK, 0, sun, 7, now)) // Sun..Wed
        val jan1 = dayKey(day(Calendar.JANUARY, 1, 0))
        assertEquals(9, chartElapsedCount(StatsPeriod.YEAR, 0, jan1, 12, now))
    }

    @Test fun average_usesElapsedBarsOnly() {
        val bars = listOf(3.0, 0.0, 3.0, 1.0, 2.0).map { ChartBar("", it) }
        assertEquals(1.8, chartAverage(bars, 5), 1e-9) // the iOS screenshot: 9 / 5
        assertEquals(1.5, chartAverage(bars, 2), 1e-9) // W3..W5 not here yet
    }

    @Test fun monthLetter_numericLocalesKeepWholeNumber() {
        val oct = day(Calendar.OCTOBER, 1)
        assertEquals("O", monthLetter(oct, Locale.US))
        assertEquals("10", monthLetter(oct, Locale.KOREA))
        assertEquals("10", monthLetter(oct, Locale.JAPAN))
    }

    // endregion

    // region Metrics / breakdown

    @Test fun workoutTime_format() {
        assertTrue(workoutTimeValue(0).isEmpty)
        assertEquals("—", workoutTimeValue(0).big)
        assertEquals(Pair("40", "min"), workoutTimeValue(40).let { it.big to it.unit })
        assertEquals(Pair("5h", "23m"), workoutTimeValue(323).let { it.big to it.unit })
        assertEquals(Pair("2h", null), workoutTimeValue(120).let { it.big to it.unit })
    }

    @Test fun studio_countsClassAndPhotoTags_slotZeroOnly_mostFirst() {
        val logs = listOf(
            ClassLog.create(tags = listOf("Studio A", "Beginner", "")),
            ClassLog.create(tags = listOf("Studio B")),
            ClassLog.create(tags = listOf("", "Advanced"))
        )
        val photos = listOf(
            PhotoLog.create(photoPath = "", tags = listOf("Studio B")),
            PhotoLog.create(photoPath = "", tags = listOf("Studio B"))
        )
        assertEquals(listOf("Studio B" to 3, "Studio A" to 1), studioBreakdown(logs, photos))
    }

    // endregion
}
