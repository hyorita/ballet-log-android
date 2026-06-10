package com.hyorita.balletlog.data

import android.content.Context
import com.hyorita.balletlog.data.db.BalletLogDatabase
import com.hyorita.balletlog.data.db.PhotoLogDao
import com.hyorita.balletlog.data.model.PhotoLog

/**
 * 1.8 auto-PhotoLog flow. Scans Health Connect for recent ballet-related
 * exercise sessions and inserts a placeholder [PhotoLog] (no photo yet,
 * workout fields populated) for each one we haven't seen before.
 *
 * Dedupe key: [PhotoLog.externalWorkoutId] holds the Health Connect
 * `ExerciseSessionRecord.metadata.id`, so a re-import inside the lookback
 * window won't create a duplicate even if the user opens the app multiple
 * times the same day.
 *
 * Returns the number of placeholders newly inserted so the caller can show
 * a "synced N workouts" toast if it wants to.
 */
object HealthConnectAutoImport {

    suspend fun importRecent(
        context: Context,
        lookbackHours: Long = 24
    ): Int {
        val dao = BalletLogDatabase.getInstance(context).photoLogDao()
        return importRecent(context, dao, lookbackHours)
    }

    suspend fun importRecent(
        context: Context,
        dao: PhotoLogDao,
        lookbackHours: Long = 24
    ): Int {
        val records = HealthConnectManager.scanRecentWorkouts(context, lookbackHours)
        return importWorkouts(dao, records)
    }

    /**
     * Shared dedupe + insert path for every 1.9 import entry point (foreground
     * auto-import, History per-day tap, connect-time 30-day backfill). Skips any
     * session whose identity is already on a PhotoLog. Returns the number of
     * placeholders newly inserted.
     */
    suspend fun importWorkouts(
        dao: PhotoLogDao,
        records: List<HealthConnectManager.ScannedWorkout>
    ): Int {
        if (records.isEmpty()) return 0
        val alreadyImported = dao.getAllExternalWorkoutIds().toHashSet()
        var inserted = 0
        for (rec in records) {
            if (rec.externalId in alreadyImported) continue
            val placeholder = PhotoLog.create(
                photoPath = "",
                date = rec.startTimeMillis,
                kcal = rec.kcal.takeIf { it > 0 },
                durationMin = rec.durationMin.takeIf { it > 0 },
                avgBPM = rec.avgBpm.takeIf { it > 0 },
                maxBPM = rec.maxBpm.takeIf { it > 0 },
                externalWorkoutId = rec.externalId
            )
            dao.insert(placeholder)
            alreadyImported += rec.externalId
            inserted++
        }
        return inserted
    }
}
