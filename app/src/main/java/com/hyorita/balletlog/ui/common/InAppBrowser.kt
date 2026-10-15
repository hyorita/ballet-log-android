package com.hyorita.balletlog.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.hyorita.balletlog.util.debugLog

/**
 * Opens [url] in a Custom Tab — the Android counterpart of iOS's in-app
 * SafariView, so the user stays in the app and comes back with Back. Falls
 * through to whatever the browser does when it has no Custom Tabs support;
 * fails silently if there's no browser at all.
 */
fun openInAppBrowser(context: Context, url: String) {
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, Uri.parse(url))
    } catch (e: ActivityNotFoundException) {
        debugLog("InAppBrowser", "no browser for $url", e)
    }
}
