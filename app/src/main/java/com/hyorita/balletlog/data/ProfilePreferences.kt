package com.hyorita.balletlog.data

import android.content.Context

/**
 * Tiny SharedPreferences wrapper for the user's profile (Instagram handle).
 * Same key/intent as iOS @AppStorage("instagramID") so future export/import
 * can round-trip the value.
 */
object ProfilePreferences {

    private const val PREFS = "balletlog_profile"
    private const val KEY_INSTAGRAM = "instagram_id"

    fun getInstagramId(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_INSTAGRAM, "")
            ?.removePrefix("@")
            ?: ""

    fun setInstagramId(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_INSTAGRAM, value.removePrefix("@").trim())
            .apply()
    }
}
