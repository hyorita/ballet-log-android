package com.hyorita.balletlog.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.hyorita.balletlog.data.model.WorkoutInfo
import com.hyorita.balletlog.util.debugLog
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object HealthConnectManager {

    val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    // Exercise session types we auto-import into the Photo Log. Health
    // Connect doesn't expose a dedicated Barre constant (iOS does, via
    // HKWorkoutActivityType.barre), so we fall back to a keyword match
    // on session title/notes for sessions that report EXERCISE_TYPE_OTHER
    // or another generic bucket.
    private val SUPPORTED_TYPES = setOf(
        ExerciseSessionRecord.EXERCISE_TYPE_DANCING,
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA
    )

    private val BARRE_KEYWORDS = listOf("barre", "ballet", "발레", "バレエ")

    private fun isBalletRelated(session: ExerciseSessionRecord): Boolean {
        if (session.exerciseType in SUPPORTED_TYPES) return true
        val haystack = ((session.title ?: "") + " " + (session.notes ?: "")).lowercase()
        return BARRE_KEYWORDS.any { haystack.contains(it) }
    }

    /**
     * One auto-importable workout session, with everything 1.8's Photo Log
     * placeholders need pre-computed. `externalId` is the Health Connect
     * session metadata id — used by [PhotoLogDao.findByExternalWorkoutId] to
     * dedupe repeat imports across the 24h lookback window.
     */
    data class ScannedWorkout(
        val externalId: String,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val durationMin: Int,
        val kcal: Int,
        val avgBpm: Int,
        val maxBpm: Int
    )

    fun isAvailable(context: Context): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasPermissions(context: Context): Boolean {
        if (!isAvailable(context)) return false
        val client = HealthConnectClient.getOrCreate(context)
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    /**
     * Read all ballet-related workouts in the trailing `lookbackHours` window
     * — used by 1.8's auto-PhotoLog importer. Returns empty if Health Connect
     * is unavailable, permissions aren't granted, or no matching sessions
     * exist.
     */
    suspend fun scanRecentWorkouts(
        context: Context,
        lookbackHours: Long = 24
    ): List<ScannedWorkout> {
        val end = Instant.now()
        val start = end.minusSeconds(lookbackHours * 3600)
        return scanWorkouts(context, start.toEpochMilli(), end.toEpochMilli())
    }

    /**
     * Read all ballet-related workouts in an explicit `[startMillis, endMillis)`
     * range — the generalized form behind the 1.9 History per-day import and the
     * 30-day connect-time backfill. Returns empty if Health Connect is
     * unavailable, permissions aren't granted, or no matching sessions exist.
     */
    suspend fun scanWorkouts(
        context: Context,
        startMillis: Long,
        endMillis: Long
    ): List<ScannedWorkout> {
        if (!isAvailable(context)) return emptyList()
        if (!hasPermissions(context)) return emptyList()
        val client = HealthConnectClient.getOrCreate(context)
        val start = Instant.ofEpochMilli(startMillis)
        val end = Instant.ofEpochMilli(endMillis)

        return try {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            ).records
                .filter { isBalletRelated(it) }
                .map { session -> session.toScannedWorkout(client) }
        } catch (e: SecurityException) {
            debugLog("HealthConnect", "scanWorkouts permission denied: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            debugLog("HealthConnect", "scanWorkouts error: ${e.javaClass.simpleName} - ${e.message}")
            emptyList()
        }
    }

    private suspend fun ExerciseSessionRecord.toScannedWorkout(
        client: HealthConnectClient
    ): ScannedWorkout {
        val durMin = ((endTime.epochSecond - startTime.epochSecond) / 60).toInt()
        val kcal = readKilocalories(client, startTime, endTime)
        val hrs = readHeartRates(client, startTime, endTime)
        return ScannedWorkout(
            externalId = metadata.id,
            startTimeMillis = startTime.toEpochMilli(),
            endTimeMillis = endTime.toEpochMilli(),
            durationMin = durMin,
            kcal = kcal,
            avgBpm = if (hrs.isNotEmpty()) hrs.average().toInt() else 0,
            maxBpm = hrs.maxOrNull()?.toInt() ?: 0
        )
    }

    private suspend fun readKilocalories(
        client: HealthConnectClient,
        start: Instant,
        end: Instant
    ): Int {
        val active = client.readRecords(
            ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        ).records.sumOf { it.energy.inKilocalories }.toInt()
        if (active > 0) return active
        // Galaxy Watch and several other sources don't write
        // ActiveCaloriesBurned — they only write Total. Fall back so we
        // don't lose the kcal entirely.
        return runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            ).records.sumOf { it.energy.inKilocalories }.toInt()
        }.getOrDefault(0)
    }

    private suspend fun readHeartRates(
        client: HealthConnectClient,
        start: Instant,
        end: Instant
    ): List<Long> = client.readRecords(
        ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
    ).records.flatMap { it.samples }.map { it.beatsPerMinute }

    // 특정 날짜(밀리초)에 기록된 발레 워크아웃 찾기
    suspend fun readWorkoutForDate(context: Context, dateMillis: Long): WorkoutInfo? {
        if (!isAvailable(context)) return null
        val client = HealthConnectClient.getOrCreate(context)

        // 해당 날짜 00:00 ~ 23:59
        val zone = ZoneId.systemDefault()
        val startOfDay = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateMillis), zone)
            .toLocalDate().atStartOfDay(zone).toInstant()
        val endOfDay = startOfDay.plusSeconds(86400)

        return try {
            val sessions = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            ).records

            // 디버그: 해당 날짜 모든 세션 로그 출력 (release 컴파일아웃)
            debugLog("HealthConnect", "Found ${sessions.size} sessions on this date")
            sessions.forEach { s ->
                debugLog("HealthConnect", "Session: type=${s.exerciseType}, title=${s.title}, start=${s.startTime}, end=${s.endTime}")
            }

            // 모든 타입 허용 - 해당 날짜 첫 번째 워크아웃 사용
            val session = sessions.firstOrNull() ?: return null
            val durationMinutes = ((session.endTime.epochSecond - session.startTime.epochSecond) / 60).toInt()
            val calories = readKilocalories(client, session.startTime, session.endTime)
            val heartRates = readHeartRates(client, session.startTime, session.endTime)
            val avgBpm = if (heartRates.isNotEmpty()) heartRates.average().toInt() else 0
            val maxBpm = heartRates.maxOrNull()?.toInt() ?: 0

            WorkoutInfo(
                durationMinutes = durationMinutes,
                activeCalories = calories,
                avgHeartRate = avgBpm,
                maxHeartRate = maxBpm,
                sourceName = "발레",
                // 1.9: carry the session identity so Stats can dedupe this
                // ClassLog workout against the same session imported elsewhere.
                externalWorkoutId = session.metadata.id
            )
        } catch (e: SecurityException) {
            debugLog("HealthConnect", "Permission denied: ${e.message}")
            null
        } catch (e: Exception) {
            debugLog("HealthConnect", "Error reading workout: ${e.javaClass.simpleName} - ${e.message}")
            null
        }
    }
}
