package com.hyorita.balletlog.data

import android.content.Context

/**
 * 1.11 Log-grid month collapse state. Stores the set of collapsed month keys
 * ("yyyy-MM") as a comma-joined string, mirroring iOS `@AppStorage
 * "collapsedLogMonths"`. Empty (default) → all months expanded (non-destructive);
 * collapsed months persist across launches.
 */
object CollapsedMonthsPreferences {

    private const val PREFS = "balletlog_log_grid"
    private const val KEY_COLLAPSED_MONTHS = "collapsed_log_months"

    fun get(context: Context): Set<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_COLLAPSED_MONTHS, "") ?: ""
        return raw.split(",").filter { it.isNotEmpty() }.toSet()
    }

    fun set(context: Context, keys: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_COLLAPSED_MONTHS, keys.sorted().joined())
            .apply()
    }

    private fun List<String>.joined() = joinToString(",")
}
