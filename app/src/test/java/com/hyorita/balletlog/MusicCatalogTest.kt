package com.hyorita.balletlog

import com.google.gson.Gson
import com.hyorita.balletlog.data.CatalogRepository
import com.hyorita.balletlog.data.model.CatalogAlbum
import com.hyorita.balletlog.data.model.CatalogArtist
import com.hyorita.balletlog.data.model.ClassExercise
import com.hyorita.balletlog.data.model.MusicCatalog
import com.hyorita.balletlog.data.model.localized
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 1.13 Music tab. Covers the parts that fail silently on a device: a wrong
 * artwork URL just loads a bigger file, and a drifting daily hash quietly shows
 * a different album than iOS shows the same day.
 *
 * Runs against the real bundled seed so a malformed publish is caught here
 * rather than by an empty Music tab.
 */
class MusicCatalogTest {

    private val seed: MusicCatalog by lazy {
        val file = File("src/main/assets/catalog.json")
        assertTrue("bundled seed missing at ${file.absolutePath}", file.exists())
        Gson().fromJson(file.readText(), MusicCatalog::class.java)
    }

    private fun day(text: String) = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .parse(text)!!

    @Test
    fun `seed parses into the shape the UI expects`() {
        assertEquals(1, seed.schemaVersion)
        assertTrue(seed.artists.size > 50)
        assertTrue(seed.albums.size > 700)
        // Every album must resolve to an artist, or rows render without a name.
        val artistIds = seed.artists.map { it.id }.toHashSet()
        assertTrue(seed.albums.all { it.artistId in artistIds })
        // rank drives the default order — a missing one collapses the curation.
        assertTrue(seed.artists.all { it.rank > 0 })
    }

    /**
     * The hash must match Swift's byte for byte; if it drifts, both platforms
     * still "work" but stop agreeing on the album of the day.
     */
    @Test
    fun `stable hash matches the reference FNV-1a values`() {
        assertEquals(4394694982058420934UL, CatalogRepository.stableHash("2026-08-16"))
        assertEquals(4394696081570049145UL, CatalogRepository.stableHash("2026-08-17"))
        assertEquals(18099244625767376899UL, CatalogRepository.stableHash("2026-01-01"))
    }

    @Test
    fun `discovery is fixed for a day and changes the next`() {
        val today = CatalogRepository.discoveryFrom(seed, day("2026-08-16"))
        assertNotNull(today)
        assertEquals(today, CatalogRepository.discoveryFrom(seed, day("2026-08-16")))
        assertTrue(today != CatalogRepository.discoveryFrom(seed, day("2026-08-17")))
    }

    @Test
    fun `discovery only draws from tier 1 albums with real content`() {
        val tier1 = seed.artists.filter { it.tier == 1 }.map { it.id }.toHashSet()
        // A month of picks — a single day could pass by luck.
        (1..30).forEach { d ->
            val album = CatalogRepository.discoveryFrom(seed, day("2026-06-%02d".format(d)))!!
            assertTrue(album.artistId in tier1)
            assertTrue(album.artwork.isNotEmpty())
            // Singles show nothing about how someone plays a class.
            assertTrue(album.trackCount >= 5)
        }
    }

    @Test
    fun `seasonal albums only enter the discovery pool in season`() {
        val christmas = CatalogAlbum(themes = listOf("christmas"))
        val halloween = CatalogAlbum(themes = listOf("halloween"))
        val yearRound = CatalogAlbum(themes = listOf("pop"))

        assertTrue(CatalogRepository.isInSeason(christmas, 11))
        assertTrue(CatalogRepository.isInSeason(christmas, 12))
        assertTrue(!CatalogRepository.isInSeason(christmas, 6))

        assertTrue(CatalogRepository.isInSeason(halloween, 10))
        assertTrue(!CatalogRepository.isInSeason(halloween, 9))
        assertTrue(!CatalogRepository.isInSeason(halloween, 11))

        assertTrue(CatalogRepository.isInSeason(yearRound, 1))
        assertTrue(CatalogRepository.isInSeason(yearRound, 10))
    }

    @Test
    fun `discovery skips albums marked excludeFromDiscovery`() {
        val artist = CatalogArtist(id = "artist", tier = 1)
        val excluded = CatalogAlbum(
            id = "excluded", artistId = "artist", artwork = "x", trackCount = 6,
            excludeFromDiscovery = true
        )
        val eligible = CatalogAlbum(
            id = "eligible", artistId = "artist", artwork = "x", trackCount = 6
        )
        val catalog = MusicCatalog(artists = listOf(artist), albums = listOf(excluded, eligible))

        // A month of picks — one lucky day could pass by chance if the filter
        // were silently ignored.
        (1..30).forEach { d ->
            val pick = CatalogRepository.discoveryFrom(catalog, day("2026-06-%02d".format(d)))
            assertEquals(eligible, pick)
        }
    }

    @Test
    fun `new release count only counts albums added after the last seen version`() {
        val catalog = MusicCatalog(
            catalogVersion = 12,
            albums = listOf(
                CatalogAlbum(id = "a", addedIn = 10),
                CatalogAlbum(id = "b", addedIn = 11),
                CatalogAlbum(id = "c", addedIn = 12),
                CatalogAlbum(id = "d", addedIn = null)
            )
        )
        assertEquals(2, CatalogRepository.newReleaseCountFrom(catalog, sinceVersion = 10))
        assertEquals(0, CatalogRepository.newReleaseCountFrom(catalog, sinceVersion = 12))
        // sinceVersion 0 means "never seen a badge" (first-run guard) — not a count of everything.
        assertEquals(0, CatalogRepository.newReleaseCountFrom(catalog, sinceVersion = 0))
        assertEquals(0, CatalogRepository.newReleaseCountFrom(null, sinceVersion = 10))
    }

    @Test
    fun `artwork url snaps to the size ladder`() {
        val album = CatalogAlbum(
            artwork = "https://is1-ssl.mzstatic.com/image/thumb/x/600x600bb.jpg"
        )
        assertTrue(album.artworkUrl(52)!!.contains("120x120bb"))
        assertTrue(album.artworkUrl(124)!!.contains("200x200bb"))
        assertTrue(album.artworkUrl(396)!!.contains("400x400bb"))
        // Beyond the ladder falls back to the published size, not a made-up one.
        assertTrue(album.artworkUrl(1200)!!.contains("600x600bb"))
        assertNull(CatalogAlbum(artwork = "").artworkUrl(52))
    }

    @Test
    fun `localized dictionary falls back to english then anything`() {
        Locale.setDefault(Locale.KOREAN)
        assertEquals("한국어", mapOf("en" to "English", "ko" to "한국어").localized())
        assertEquals("English", mapOf("en" to "English").localized())
        // Empty strings are holes, not values — the catalog does not fill every locale.
        assertEquals("English", mapOf("ko" to "", "en" to "English").localized())
        assertEquals("日本語", mapOf("ja" to "日本語").localized())
        assertNull(emptyMap<String, String>().localized())
    }

    @Test
    fun `exercise keys map to ballet labels`() {
        // An unmapped key renders as no chip at all, silently losing the one
        // thing that makes the track list useful for choosing class music. These
        // are the keys the catalog's parser emits (CATALOG-SCHEMA.md).
        val keys = ClassExercise.entries.map { it.key }
        assertTrue(
            keys.containsAll(
                listOf(
                    "warmup", "stretch", "plie", "tendu", "degage", "rond-de-jambe",
                    "fondu", "frappe", "developpe", "petit-battement", "grand-battement",
                    "port-de-bras", "adagio", "pirouette", "allegro", "petit-allegro",
                    "grand-allegro", "echappe", "variation", "coda", "reverence"
                )
            )
        )
        assertEquals("Dégagé", ClassExercise.labelFor("degage"))
        assertEquals("Grand battement", ClassExercise.labelFor("grand-battement"))
        assertNull(ClassExercise.labelFor("not-a-step"))
        assertNull(ClassExercise.labelFor(null))
    }
}
