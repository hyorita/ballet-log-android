package com.hyorita.balletlog.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.hyorita.balletlog.data.model.WorkoutInfo
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object HealthConnectManager {

    val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    fun isAvailable(context: Context): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasPermissions(context: Context): Boolean {
        if (!isAvailable(context)) return false
        val client = HealthConnectClient.getOrCreate(context)
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

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

            // 디버그: 해당 날짜 모든 세션 로그 출력
            android.util.Log.d("HealthConnect", "Found ${sessions.size} sessions on this date")
            sessions.forEach { s ->
                android.util.Log.d("HealthConnect", "Session: type=${s.exerciseType}, title=${s.title}, start=${s.startTime}, end=${s.endTime}")
            }

            // 모든 타입 허용 - 해당 날짜 첫 번째 워크아웃 사용
            val session = sessions.firstOrNull() ?: return null

            val durationMinutes = (session.endTime.epochSecond - session.startTime.epochSecond) / 60

            // 칼로리
            val calories = client.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
                )
            ).records.sumOf { it.energy.inKilocalories }.toInt()

            // 심박수
            val heartRates = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
                )
            ).records.flatMap { it.samples }.map { it.beatsPerMinute }

            val avgBpm = if (heartRates.isNotEmpty()) heartRates.average().toInt() else 0
            val maxBpm = heartRates.maxOrNull()?.toInt() ?: 0

            WorkoutInfo(
                durationMinutes = durationMinutes.toInt(),
                activeCalories = calories,
                avgHeartRate = avgBpm,
                maxHeartRate = maxBpm,
                sourceName = "발레"
            )
        } catch (e: SecurityException) {
            android.util.Log.e("HealthConnect", "Permission denied: ${e.message}")
            null
        } catch (e: Exception) {
            android.util.Log.e("HealthConnect", "Error reading workout: ${e.javaClass.simpleName} - ${e.message}")
            null
        }
    }
}
