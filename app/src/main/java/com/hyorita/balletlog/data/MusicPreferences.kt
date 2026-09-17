package com.hyorita.balletlog.data

import android.content.Context

/**
 * 1.14 Music tab badge state. Mirrors iOS `@AppStorage "musicLastSeenVersion"`.
 *
 * Stores the catalog version the user last saw, not a date — a date-based
 * watermark broke when an album was dated ahead of its actual publish (a future
 * `releaseDate` could pin the "seen" line past releases that hadn't landed yet).
 * A monotonic version number has no such failure mode.
 */
object MusicPreferences {

    private const val PREFS = "balletlog_music"
    private const val KEY_LAST_SEEN_VERSION = "musicLastSeenVersion"

    fun lastSeenVersion(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_SEEN_VERSION, 0)

    fun setLastSeenVersion(context: Context, version: Int) {
        if (version <= 0) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_LAST_SEEN_VERSION, version)
            .apply()
    }
}
