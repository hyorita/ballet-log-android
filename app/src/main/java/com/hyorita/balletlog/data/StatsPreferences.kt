package com.hyorita.balletlog.data

import android.content.Context
import com.hyorita.balletlog.data.db.BalletLogDatabase

/**
 * 1.16 Stats tab "New" badge. Mirrors iOS `@AppStorage "statsLastSeenVersion"`.
 *
 * Same shape as [MusicPreferences] — a version the user last saw, not a flag —
 * but hand-maintained: Stats has no server to source a version from. Bump
 * [FEATURE_VERSION] when a Stats change is worth re-flagging to users who
 * already cleared a previous one.
 */
object StatsPreferences {

    const val FEATURE_VERSION = 1

    private const val PREFS = "balletlog_stats"
    private const val KEY_LAST_SEEN_VERSION = "statsLastSeenVersion"

    /**
     * Shown only to people who already had records before this version — a
     * brand-new user shouldn't see "New" on a tab they've never known any
     * other way. An install with no records yet is marked seen on the spot,
     * so the badge can't appear later once they start logging.
     *
     * iOS gates on `hasSeenLogTutorial` instead. That flag flips on the Log
     * tab's `+`, not on having data: a user who restored a backup, or only
     * ever used the Class tab, never gets it, and a new user gets it on their
     * first tap. Record count is the thing the flag was standing in for.
     */
    suspend fun hasNewStats(context: Context): Boolean {
        if (lastSeenVersion(context) >= FEATURE_VERSION) return false
        val db = BalletLogDatabase.getInstance(context)
        val hasRecords = db.classLogDao().count() > 0 ||
            db.photoLogDao().count() > 0 ||
            db.noteDao().count() > 0
        if (!hasRecords) markSeen(context)
        return hasRecords
    }

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_LAST_SEEN_VERSION, FEATURE_VERSION)
            .apply()
    }

    private fun lastSeenVersion(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_SEEN_VERSION, 0)
}
