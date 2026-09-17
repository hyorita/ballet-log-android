package com.hyorita.balletlog.data

import android.content.Context
import com.google.gson.Gson
import com.hyorita.balletlog.data.model.AlbumTracks
import com.hyorita.balletlog.data.model.CatalogAlbum
import com.hyorita.balletlog.data.model.CatalogArtist
import com.hyorita.balletlog.data.model.MusicCatalog
import com.hyorita.balletlog.util.debugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Loads the shared music catalog.
 *
 * Order of preference: cached copy → bundled seed → remote refresh in the
 * background. The catalog is content, not user data — if every path fails the
 * Music tab simply shows nothing. **Never surface an error for this**; the rest
 * of the app is unaffected.
 *
 * Plain [HttpURLConnection] rather than a HTTP client dependency: two static
 * JSON files, no auth, no interceptors.
 */
object CatalogRepository {

    private const val BASE = "https://hyorita.github.io/ballet-log-catalog/v1/"
    private const val CACHE_FILE = "music-catalog-v1.json"
    private const val SEED_ASSET = "catalog.json"
    private const val TIMEOUT_MS = 20_000

    private val gson = Gson()

    private val _catalog = MutableStateFlow<MusicCatalog?>(null)
    val catalog: StateFlow<MusicCatalog?> = _catalog.asStateFlow()

    private val trackCache = mutableMapOf<String, AlbumTracks>()
    private val trackMutex = Mutex()
    private val refreshMutex = Mutex()

    private var lastRefreshAt: Long? = null
    private const val MIN_REFRESH_INTERVAL_MS = 60 * 60 * 1000L

    // MARK: - Index

    /** Show something immediately, then refresh in the background. */
    suspend fun load(context: Context) {
        if (_catalog.value == null) {
            _catalog.value = loadCached(context) ?: loadSeed(context)
        }
        refresh(context)
    }

    private suspend fun loadCached(context: Context): MusicCatalog? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(context.cacheDir, CACHE_FILE)
            if (!file.exists()) return@runCatching null
            gson.fromJson(file.readText(), MusicCatalog::class.java)
        }.getOrNull()
    }

    private suspend fun loadSeed(context: Context): MusicCatalog? = withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open(SEED_ASSET).bufferedReader().use {
                gson.fromJson(it.readText(), MusicCatalog::class.java)
            }
        }.getOrNull()
    }

    private suspend fun refresh(context: Context) {
        if (refreshMutex.isLocked) return
        val last = lastRefreshAt
        if (last != null && System.currentTimeMillis() - last < MIN_REFRESH_INTERVAL_MS) return
        refreshMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    // Stamped before the fetch, not after — a failed attempt
                    // still counts against the window, or a flaky network would
                    // retry on every foreground instead of backing off.
                    lastRefreshAt = System.currentTimeMillis()
                    val body = fetch("${BASE}catalog.json") ?: return@runCatching
                    val fresh = gson.fromJson(body, MusicCatalog::class.java) ?: return@runCatching
                    // Only adopt a newer catalog — a partial or rolled-back
                    // publish must not replace good cached content.
                    if (fresh.catalogVersion < (_catalog.value?.catalogVersion ?: 0)) return@runCatching
                    _catalog.value = fresh
                    File(context.cacheDir, CACHE_FILE).writeText(body)
                }.onFailure { debugLog("Catalog", "refresh failed", it) }
            }
        }
    }

    // MARK: - Tracks (fetched per album, on demand)

    suspend fun loadTracks(albumId: String): AlbumTracks? {
        trackMutex.withLock { trackCache[albumId] }?.let { return it }

        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                val body = fetch("${BASE}albums/$albumId.json") ?: return@runCatching null
                gson.fromJson(body, AlbumTracks::class.java)
            }.onFailure { debugLog("Catalog", "track load failed for $albumId", it) }
                .getOrNull()
        } ?: return null

        trackMutex.withLock { trackCache[albumId] = decoded }
        return decoded
    }

    private fun fetch(urlString: String): String? {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
        }
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) null
            else connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    // MARK: - Derived views of the catalog

    fun artist(id: String): CatalogArtist? = _catalog.value?.artists?.firstOrNull { it.id == id }

    fun album(id: String): CatalogAlbum? = _catalog.value?.albums?.firstOrNull { it.id == id }

    fun albums(artistId: String): List<CatalogAlbum> =
        _catalog.value?.albums.orEmpty()
            .filter { it.artistId == artistId }
            .sortedByDescending { it.releaseDate }

    /** Newest first. */
    fun recentAlbums(limit: Int = 20): List<CatalogAlbum> =
        _catalog.value?.albums.orEmpty()
            .sortedByDescending { it.releaseDate }
            .take(limit)

    /**
     * Albums added to the catalog after the version the user last saw on the
     * Music tab. Drives the tab badge — without it the tab sits at the far
     * right with nothing to say a month's worth of new music has arrived.
     *
     * Version-based, not date-based: a date watermark broke whenever an album's
     * `releaseDate` was ahead of its actual publish, pinning "seen" past
     * releases that hadn't landed yet. `catalogVersion` only moves forward.
     */
    fun newReleaseCount(sinceVersion: Int): Int =
        newReleaseCountFrom(_catalog.value, sinceVersion)

    internal fun newReleaseCountFrom(catalog: MusicCatalog?, sinceVersion: Int): Int {
        if (sinceVersion <= 0) return 0
        return catalog?.albums.orEmpty().count { (it.addedIn ?: 0) > sinceVersion }
    }

    val catalogVersion: Int
        get() = _catalog.value?.catalogVersion ?: 0

    /**
     * One album per day, stable for the whole day, same for everyone.
     * Drawn from tier 1 only so the daily slot stays high quality.
     */
    fun discovery(date: Date = Date()): CatalogAlbum? =
        _catalog.value?.let { discoveryFrom(it, date) }

    internal fun discoveryFrom(catalog: MusicCatalog, date: Date): CatalogAlbum? {
        val tier1 = catalog.artists.filter { it.tier == 1 }.map { it.id }.toHashSet()
        // Local calendar month, not UTC — matches iOS's Calendar.current, even
        // though the day-seed key just below stays UTC. Keep that mismatch: both
        // platforms need to land on the identical album on the identical day.
        val month = Calendar.getInstance().apply { time = date }.get(Calendar.MONTH) + 1
        val pool = catalog.albums
            // Singles are a poor thing to open the tab with — one track shows
            // nothing about how someone plays a class.
            .filter { it.artistId in tier1 && it.artwork.isNotEmpty() && it.trackCount >= 5 }
            .filter { it.excludeFromDiscovery != true }
            .filter { isInSeason(it, month) }
            .sortedBy { it.id }          // stable ordering across launches
        if (pool.isEmpty()) return null

        val key = dayFormatter.format(date)
        return pool[(stableHash(key) % pool.size.toULong()).toInt()]
    }

    /** Christmas/Halloween albums only surface in their season; everything else is always eligible. */
    internal fun isInSeason(album: CatalogAlbum, month: Int): Boolean = when {
        album.themes.contains("christmas") -> month == 11 || month == 12
        album.themes.contains("halloween") -> month == 10
        else -> true
    }

    private val dayFormatter: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    /**
     * FNV-1a. Deterministic across launches and devices, unlike [String.hashCode]
     * — and identical to the iOS implementation, so both platforms surface the
     * same album on the same day.
     */
    internal fun stableHash(string: String): ULong {
        var hash = 0xcbf29ce484222325UL
        for (byte in string.toByteArray()) {
            hash = hash xor byte.toUByte().toULong()
            hash *= 0x100000001b3UL
        }
        return hash
    }
}
