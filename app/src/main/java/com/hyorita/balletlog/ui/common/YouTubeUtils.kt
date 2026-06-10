package com.hyorita.balletlog.ui.common

import android.net.Uri

/**
 * Extracts a YouTube video id from a watch / youtu.be / embed / shorts URL,
 * or null if the string isn't a recognizable YouTube link. Shared by the Notes
 * reference-link card and the Class music card/editor thumbnail preview so all
 * three derive thumbnails the same way.
 */
fun youTubeVideoId(url: String): String? {
    val u = runCatching { Uri.parse(url) }.getOrNull() ?: return null
    val host = u.host?.lowercase() ?: return null
    return when {
        host == "youtu.be" -> u.path?.removePrefix("/")?.takeIf { it.isNotEmpty() }
        host == "youtube.com" || host.endsWith(".youtube.com") -> when {
            u.path == "/watch" -> u.getQueryParameter("v")?.takeIf { it.isNotEmpty() }
            u.path?.startsWith("/embed/") == true ->
                u.path?.removePrefix("/embed/")?.takeIf { it.isNotEmpty() }
            u.path?.startsWith("/shorts/") == true ->
                u.path?.removePrefix("/shorts/")?.takeIf { it.isNotEmpty() }
            else -> null
        }
        else -> null
    }
}
