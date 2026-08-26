package com.hyorita.balletlog.data.model

import java.util.Locale

/**
 * Remote ballet-class music catalog. Plain Gson data classes — deliberately not
 * Room entities. This is read-only content fetched from a static host, so it
 * never touches the user's database and carries no migration risk.
 *
 * The same files feed iOS. Schema contract lives in the iOS repo
 * (`catalog/CATALOG-SCHEMA.md`); do not fork a variant for Android — add fields
 * to the shared file instead, old apps ignore what they don't know.
 *
 * Live: https://hyorita.github.io/ballet-log-catalog/v1/
 */
data class MusicCatalog(
    val schemaVersion: Int = 0,
    val catalogVersion: Int = 0,
    val artists: List<CatalogArtist> = emptyList(),
    val albums: List<CatalogAlbum> = emptyList()
)

data class CatalogArtist(
    val id: String = "",
    val name: String = "",
    val nameLocal: String? = null,
    /**
     * Editorial order. Sorting by [albumCount] instead surfaces the most
     * prolific rather than the most worth hearing — they are not the same.
     */
    val rank: Int = 0,
    /** ISO country code. Search key only — never shown. [affiliation] is what the UI displays. */
    val region: String = "",
    val regionVerified: Boolean = false,
    val affiliation: Map<String, String> = emptyMap(),
    val kind: String = "",
    val tier: Int = 0,
    val note: Map<String, String> = emptyMap(),
    val appleUrl: String = "",
    val coverAlbumId: String? = null,
    val albumCount: Int = 0,
    val latestRelease: String = ""
) {
    val affiliationText: String? get() = affiliation.localized()
    val noteText: String? get() = note.localized()
}

data class CatalogAlbum(
    val id: String = "",
    val artistId: String = "",
    val title: String = "",
    val releaseDate: String = "",
    val trackCount: Int = 0,
    val artwork: String = "",
    val appleUrl: String = "",
    val level: List<String> = emptyList(),
    val part: List<String> = emptyList(),
    val themes: List<String> = emptyList()
) {
    val year: String get() = releaseDate.take(4)

    /**
     * Apple's CDN renders any size from the same path, so ask for what will
     * actually be drawn rather than always pulling the 600px file: a 52dp row
     * thumbnail is ~11 KB at 156px against ~99 KB at 600px.
     *
     * Sizes snap to a short ladder on purpose — arbitrary widths would give
     * every device its own URL and defeat both Apple's cache and Coil's.
     */
    fun artworkUrl(pixels: Int): String? {
        if (artwork.isEmpty()) return null
        val size = LADDER.firstOrNull { it >= pixels } ?: 600
        return artwork.replace("600x600bb", "${size}x${size}bb")
    }

    private companion object {
        val LADDER = listOf(120, 200, 300, 400, 600)
    }
}

data class AlbumTracks(
    val albumId: String = "",
    val tracks: List<CatalogTrack> = emptyList()
)

data class CatalogTrack(
    /** Apple track id. Needed later to build real Apple Music playlists. */
    val id: Long? = null,
    val n: Int? = null,
    val title: String = "",
    val seconds: Int = 0,
    val preview: String? = null,
    /** Parsed from the title; null when it couldn't be determined (~16% of tracks). */
    val exercise: String? = null,
    val meter: String? = null,
    val counts: String? = null
) {
    /** Stable identity for list keys — track ids are occasionally missing. */
    val rowId: String get() = id?.toString() ?: "${n ?: 0}-$title"

    val durationText: String
        get() = String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}

/**
 * Current language → en → whatever exists. The catalog only guarantees `en`.
 */
fun Map<String, String>.localized(): String? {
    val code = Locale.getDefault().language
    this[code]?.takeIf { it.isNotEmpty() }?.let { return it }
    this["en"]?.takeIf { it.isNotEmpty() }?.let { return it }
    return values.firstOrNull { it.isNotEmpty() }
}

/**
 * The exercise keys the catalog emits, in class order.
 *
 * Labels stay as ballet vocabulary in every locale — these read the same to
 * dancers in all three languages the app supports, like `kcal` and `BPM`.
 */
enum class ClassExercise(val key: String, val label: String) {
    WARMUP("warmup", "Warm Up"),
    STRETCH("stretch", "Stretch"),
    PLIE("plie", "Plié"),
    TENDU("tendu", "Tendu"),
    DEGAGE("degage", "Dégagé"),
    ROND_DE_JAMBE("rond-de-jambe", "Rond de jambe"),
    FONDU("fondu", "Fondu"),
    FRAPPE("frappe", "Frappé"),
    DEVELOPPE("developpe", "Développé"),
    PETIT_BATTEMENT("petit-battement", "Petit battement"),
    GRAND_BATTEMENT("grand-battement", "Grand battement"),
    PORT_DE_BRAS("port-de-bras", "Port de bras"),
    ADAGIO("adagio", "Adagio"),
    PIROUETTE("pirouette", "Pirouette"),
    ALLEGRO("allegro", "Allegro"),
    PETIT_ALLEGRO("petit-allegro", "Petit allegro"),
    GRAND_ALLEGRO("grand-allegro", "Grand allegro"),
    ECHAPPE("echappe", "Échappé"),
    VARIATION("variation", "Variation"),
    CODA("coda", "Coda"),
    REVERENCE("reverence", "Révérence");

    companion object {
        fun labelFor(key: String?): String? =
            key?.let { k -> entries.firstOrNull { it.key == k }?.label }
    }
}
