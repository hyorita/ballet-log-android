package com.hyorita.balletlog.data

import android.content.Context

/**
 * 1.16.1 "Find My Ballet Persona" — the balletclass.cc web quiz, opened from
 * a Log-tab banner and a Settings row. Mirrors iOS `MbtiLink` plus the
 * `mbtiLinkLastSeenVersion` / `hasDismissedMbtiBanner` AppStorage keys.
 */
object MbtiPreferences {

    /**
     * `x7h=8aq6brde1s` unlocks every art style on the result screen — someone
     * opening this from inside the app has already installed it, so the
     * "install the app" carousel lock has nothing left to sell. The web side's
     * `lib/mbti/appUnlock.js` is the canonical source; it must match exactly.
     */
    const val QUIZ_URL = "https://www.balletclass.cc/mbti?x7h=8aq6brde1s"

    /** Bump to re-flag the Settings dot for users who cleared a previous one. */
    const val FEATURE_VERSION = 1

    private const val PREFS = "balletlog_mbti"
    private const val KEY_LAST_SEEN_VERSION = "mbtiLinkLastSeenVersion"
    private const val KEY_BANNER_DISMISSED = "hasDismissedMbtiBanner"

    /**
     * The red dot on the Log tab's gear and the "New" pill in Settings — same
     * gate as the Stats badge: existing users only, and an install with no
     * records is marked seen on the spot so it never lights up later.
     */
    suspend fun hasNewLink(context: Context): Boolean {
        if (lastSeenVersion(context) >= FEATURE_VERSION) return false
        val hasRecords = hasAnyRecords(context)
        if (!hasRecords) markSeen(context)
        return hasRecords
    }

    fun markSeen(context: Context) {
        prefs(context).edit().putInt(KEY_LAST_SEEN_VERSION, FEATURE_VERSION).apply()
    }

    /**
     * The banner retires after its first open — anyone who taps it has seen
     * the quiz, and the Settings row's "New" pill shows where it went. No
     * version: once gone, it stays gone.
     */
    fun isBannerDismissed(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BANNER_DISMISSED, false)

    fun dismissBanner(context: Context) {
        prefs(context).edit().putBoolean(KEY_BANNER_DISMISSED, true).apply()
    }

    private fun lastSeenVersion(context: Context): Int =
        prefs(context).getInt(KEY_LAST_SEEN_VERSION, 0)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
