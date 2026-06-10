package com.hyorita.balletlog.data

import android.content.Context

/**
 * 1.9 History tab prefs. Currently just the "don't show again" choice for the
 * monthly "unlogged activities" banner — a global, persistent opt-out. (The
 * lighter per-month "dismiss" lives in screen state and resets each launch.)
 */
object HistoryPreferences {

    private const val PREFS = "balletlog_history"
    private const val KEY_UNLOGGED_BANNER_HIDDEN = "unlogged_banner_hidden"

    fun isUnloggedBannerHidden(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_UNLOGGED_BANNER_HIDDEN, false)

    fun hideUnloggedBanner(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_UNLOGGED_BANNER_HIDDEN, true)
            .apply()
    }
}
