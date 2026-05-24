package com.hyorita.balletlog.data

import android.content.Context

/**
 * SharedPreferences-backed accessor for the user's chip bar language
 * preference. Mirrors iOS @AppStorage("termLanguage").
 */
object TermLanguagePreferences {

    private const val PREFS = "balletlog_term_language"
    private const val KEY = "termLanguage"

    fun get(context: Context): TermLanguage =
        TermLanguage.fromKey(
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null)
        )

    fun set(context: Context, value: TermLanguage) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, TermLanguage.toKey(value))
            .apply()
    }
}
