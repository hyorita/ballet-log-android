package com.hyorita.balletlog.data

import android.content.Context

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
     * Shown only to installs that predate 1.16. [TutorialPreferences] is the
     * sentinel (as on iOS): it's already set for anyone who has used the Log
     * tab, but not for a fresh install — a brand-new user shouldn't see "New"
     * on a tab they've never known any other way.
     *
     * Unlike iOS, a fresh install is marked seen right here. Otherwise the
     * sentinel flips on the new user's first Log `+` tap and the badge shows
     * up on their next launch anyway.
     */
    fun hasNewStats(context: Context): Boolean {
        if (!TutorialPreferences.hasSeenLogTutorial(context)) {
            markSeen(context)
            return false
        }
        return lastSeenVersion(context) < FEATURE_VERSION
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
