package com.hyorita.balletlog.data

import android.content.Context

/**
 * 1.13 Music tab badge state. Mirrors iOS `@AppStorage "musicLastSeenRelease"`.
 *
 * Stores the newest release date the user has already seen, not a count — a
 * date survives the catalog growing between launches, while a count would drift
 * the moment a publish adds albums the user already scrolled past.
 */
object MusicPreferences {

    private const val PREFS = "balletlog_music"
    private const val KEY_LAST_SEEN_RELEASE = "musicLastSeenRelease"

    fun lastSeenRelease(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_SEEN_RELEASE, "") ?: ""

    fun setLastSeenRelease(context: Context, date: String) {
        if (date.isEmpty()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_SEEN_RELEASE, date)
            .apply()
    }
}
