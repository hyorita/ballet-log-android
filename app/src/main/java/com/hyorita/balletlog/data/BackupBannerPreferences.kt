package com.hyorita.balletlog.data

import android.content.Context

/**
 * Tracks whether the in-app "back up before Play Store transition" banner
 * should still be shown. Hidden as soon as the user either:
 *   - dismisses it manually (X button), or
 *   - completes a backup export at least once.
 */
object BackupBannerPreferences {

    private const val PREFS = "balletlog_banner"
    private const val KEY_DISMISSED = "backup_banner_dismissed"
    private const val KEY_LAST_EXPORT = "last_export_at"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun shouldShow(context: Context): Boolean {
        val p = prefs(context)
        return !p.getBoolean(KEY_DISMISSED, false) &&
            p.getLong(KEY_LAST_EXPORT, 0L) == 0L
    }

    fun setDismissed(context: Context) {
        prefs(context).edit().putBoolean(KEY_DISMISSED, true).apply()
    }

    fun markExported(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_EXPORT, System.currentTimeMillis()).apply()
    }
}
