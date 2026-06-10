package com.hyorita.balletlog.data

import android.content.Context

/**
 * 1.9 one-time connect-time backfill. The first time the app runs with Health
 * Connect granted (covers both brand-new users and existing 1.8 users updating
 * to 1.9), we offer to pull in the last 30 days of workouts that aren't logged
 * yet. The flag is only set once the user has actually responded to the prompt,
 * mirroring iOS — so a launch that finds nothing to import doesn't burn the
 * one-time offer.
 */
object BackfillPreferences {

    private const val PREFS = "balletlog_backfill"
    private const val KEY_PROMPTED = "backfill_prompted"

    fun hasPrompted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PROMPTED, false)

    fun setPrompted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PROMPTED, true)
            .apply()
    }
}
