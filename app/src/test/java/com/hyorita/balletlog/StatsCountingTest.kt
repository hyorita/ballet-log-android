package com.hyorita.balletlog

import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.PhotoLog
import com.hyorita.balletlog.data.model.WorkoutInfo
import com.hyorita.balletlog.ui.stats.classEvents
import com.hyorita.balletlog.ui.stats.hardestWorkout
import com.hyorita.balletlog.ui.stats.workoutSessions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * 1.16: Stats counting matches iOS `classEventDates` / `workoutSessions`.
 * A mismatch here doesn't crash anything — it just shows a different class
 * count than the same person's iPhone, which is why it's pinned down.
 */
class StatsCountingTest {

    private fun at(day: Int, hour: Int): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.MAY, day, hour, 0)
    }.timeInMillis

    private fun classLog(day: Int, hour: Int = 10, min: Int = 0, kcal: Int = 0, id: String? = null) =
        ClassLog.create(
            date = at(day, hour),
            workout = if (min > 0 || kcal > 0 || id != null)
                WorkoutInfo(durationMinutes = min, activeCalories = kcal, externalWorkoutId = id) else null
        )

    private fun photo(day: Int, hour: Int = 12, min: Int? = null, kcal: Int? = null, bpm: Int? = null, id: String? = null) =
        PhotoLog(date = at(day, hour), durationMin = min, kcal = kcal, avgBPM = bpm, externalWorkoutId = id)

    // classEvents

    @Test fun everyClassLogCounts_evenTwoOnOneDay_andWithoutWorkout() {
        val logs = listOf(classLog(1, 10), classLog(1, 18), classLog(2, min = 60))
        assertEquals(3, classEvents(logs, emptyList()).size)
    }

    @Test fun unidentifiedPhotoWorkout_onClassDay_isNotCounted() {
        val events = classEvents(listOf(classLog(1)), listOf(photo(1, kcal = 300)))
        assertEquals(1, events.size)
    }

    @Test fun unidentifiedPhotoWorkouts_countOncePerDay() {
        val photos = listOf(photo(3, 9, kcal = 100), photo(3, 20, min = 30), photo(4, min = 45))
        assertEquals(2, classEvents(emptyList(), photos).size)
    }

    @Test fun identifiedPhotoWorkout_sameSessionAsClass_isNotCounted() {
        val events = classEvents(listOf(classLog(1, min = 60, id = "s1")), listOf(photo(1, kcal = 300, id = "s1")))
        assertEquals(1, events.size)
    }

    @Test fun identifiedPhotoWorkout_differentSession_countsEvenOnClassDay() {
        val events = classEvents(listOf(classLog(1, min = 60, id = "s1")), listOf(photo(1, kcal = 300, id = "s2")))
        assertEquals(2, events.size)
    }

    @Test fun identifiedPhotoWorkout_coversItsDayForUnidentifiedOnes() {
        val photos = listOf(photo(5, 9, kcal = 200, id = "s9"), photo(5, 19, kcal = 100))
        assertEquals(1, classEvents(emptyList(), photos).size)
    }

    @Test fun heartRateOnlyPhoto_isNotAWorkout() {
        assertEquals(0, classEvents(emptyList(), listOf(photo(6, bpm = 140))).size)
        assertEquals(0, workoutSessions(emptyList(), listOf(photo(6, bpm = 140))).size)
    }

    // workoutSessions

    @Test fun sessions_skipClassLogsWithoutWorkout() {
        assertEquals(1, workoutSessions(listOf(classLog(1), classLog(2, min = 60)), emptyList()).size)
    }

    @Test fun sessions_unidentifiedClassAndPhotoSameDay_countOnce_classWins() {
        val sessions = workoutSessions(listOf(classLog(1, min = 90, kcal = 400)), listOf(photo(1, min = 30, kcal = 100)))
        assertEquals(1, sessions.size)
        assertEquals(400, sessions.single().activeCalories)
        assertNotNull(sessions.single().classLog)
    }

    @Test fun sessions_twoDistinctIdentifiedSameDay_bothCount() {
        val sessions = workoutSessions(
            listOf(classLog(1, min = 60, kcal = 300, id = "a")),
            listOf(photo(1, min = 40, kcal = 200, id = "b"))
        )
        assertEquals(500, sessions.sumOf { it.activeCalories })
    }

    @Test fun sessions_unidentifiedSkippedOnDayCoveredByIdentified() {
        val sessions = workoutSessions(
            listOf(classLog(1, min = 60, kcal = 300)),
            listOf(photo(1, min = 40, kcal = 200, id = "b"))
        )
        assertEquals(200, sessions.sumOf { it.activeCalories })
    }

    // hardestWorkout

    @Test fun hardest_classWinsTie() {
        val best = hardestWorkout(listOf(classLog(1, kcal = 500)), listOf(photo(2, kcal = 500)))
        assertNotNull(best?.classLog)
    }

    @Test fun hardest_nullWhenNoCalories() {
        assertNull(hardestWorkout(listOf(classLog(1)), listOf(photo(2, min = 30))))
    }
}
