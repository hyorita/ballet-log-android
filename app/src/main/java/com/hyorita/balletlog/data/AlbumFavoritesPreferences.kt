package com.hyorita.balletlog.data

import android.content.Context

/**
 * Albums the user has saved from the catalog.
 *
 * SharedPreferences, **not Room**, on purpose: the database holds production
 * user data and every schema change carries migration risk. A saved album is
 * just a catalog id — cheap, replaceable, and not worth touching the store for.
 * Mirrors iOS `UserDefaults "favoriteCatalogAlbums"`.
 *
 * Consequence: favorites are not included in a backup, matching iOS. Revisit if
 * this ever grows into something users would be upset to lose.
 *
 * Order is newest-first, so the list is stored joined rather than as a Set —
 * `getStringSet` would not preserve it.
 */
object AlbumFavoritesPreferences {

    private const val PREFS = "balletlog_music"
    private const val KEY_FAVORITES = "favoriteCatalogAlbums"

    fun get(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FAVORITES, "") ?: ""
        return raw.split(",").filter { it.isNotEmpty() }
    }

    /** Returns the new list so callers can update state without re-reading. */
    fun toggle(context: Context, albumId: String): List<String> {
        val current = get(context).toMutableList()
        if (!current.remove(albumId)) current.add(0, albumId)   // newest first
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_FAVORITES, current.joinToString(","))
            .apply()
        return current
    }
}
