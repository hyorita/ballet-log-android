package com.hyorita.balletlog.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import java.util.Calendar

/**
 * Switches to the Halloween launcher icon for Oct 29–31, back to the default
 * icon outside that window. Both activity-aliases ship inside every build
 * once this lands — toggling which one is enabled is a local PackageManager
 * call, not a new Play Store submission, so returning to the default icon on
 * November 1st needs no further review.
 *
 * Unlike iOS, Android shows no "icon changed" system alert, so there's no
 * need to dedupe the call against the current state the way iOS's
 * applyIfNeeded() does — but we still skip the PackageManager calls when the
 * desired state already matches, since toggling component-enabled state can
 * briefly restart the launcher's icon cache for no reason.
 */
object SeasonalAppIcon {
    private const val DEFAULT_ALIAS = "com.hyorita.balletlog.icon.LauncherDefault"
    private const val HALLOWEEN_ALIAS = "com.hyorita.balletlog.icon.LauncherHalloween"

    private fun isHalloweenWindow(calendar: Calendar): Boolean =
        calendar.get(Calendar.MONTH) + 1 == 10 && calendar.get(Calendar.DAY_OF_MONTH) >= 29

    fun applyIfNeeded(context: Context, calendar: Calendar = Calendar.getInstance()) {
        val pm = context.packageManager
        val wantHalloween = isHalloweenWindow(calendar)
        val defaultAlias = ComponentName(context, DEFAULT_ALIAS)
        val halloweenAlias = ComponentName(context, HALLOWEEN_ALIAS)

        val halloweenEnabled =
            pm.getComponentEnabledSetting(halloweenAlias) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        if (halloweenEnabled == wantHalloween) return

        pm.setComponentEnabledSetting(
            halloweenAlias,
            if (wantHalloween) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            defaultAlias,
            if (wantHalloween) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
