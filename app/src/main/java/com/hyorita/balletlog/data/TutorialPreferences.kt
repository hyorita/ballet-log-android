package com.hyorita.balletlog.data

import android.content.Context

/**
 * One-time onboarding flags. Currently just the PhotoLog "tap here" arrow,
 * mirroring iOS @AppStorage("hasSeenLogTutorial").
 */
object TutorialPreferences {

    private const val PREFS = "balletlog_tutorial"
    private const val KEY_LOG_TUTORIAL_SEEN = "has_seen_log_tutorial"

    fun hasSeenLogTutorial(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOG_TUTORIAL_SEEN, false)

    fun setLogTutorialSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_LOG_TUTORIAL_SEEN, true)
            .apply()
    }
}
