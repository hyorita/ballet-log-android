package com.hyorita.balletlog.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.AlbumFavoritesPreferences
import com.hyorita.balletlog.data.CatalogRepository
import com.hyorita.balletlog.data.model.CatalogAlbum
import com.hyorita.balletlog.data.model.CatalogArtist
import java.util.Locale

/**
 * Music tab — discover ballet class accompanists and their albums.
 *
 * Content, not user data. Everything here comes from the shared catalog and
 * degrades quietly: no network and no cache means the sections just don't
 * appear. Never show an error — the rest of the app is unaffected.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusicScreen() {
    val context = LocalContext.current
    val catalog by CatalogRepository.catalog.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var showAllArtists by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(AlbumFavoritesPreferences.get(context)) }

    // Two-level stack: the tab opens an artist or an album, and an artist can
    // open one of its albums on top. Matches the depth iOS reaches with
    // NavigationPath without pulling a nested NavHost into a single tab.
    var openedArtist by remember { mutableStateOf<CatalogArtist?>(null) }
    var openedAlbum by remember { mutableStateOf<CatalogAlbum?>(null) }

    LaunchedEffect(Unit) { CatalogRepository.load(context) }

    // An empty focused field shows the suggestion chips instead of the sections,
    // so focus has to end when the keyboard does. Without this, dismissing the
    // keyboard (or clearing the query) strands the tab on the chips with no way
    // back to Today's discovery short of typing something. iOS gets this free
    // from scrollDismissesKeyboard.
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (!imeVisible) focusManager.clearFocus()
    }

    val query = searchQuery.trim().lowercase()
    val isSearching = query.isNotEmpty()

    // Editorial order, never album count — the most prolific are not the most
    // worth hearing, and sorting by volume surfaces exactly the wrong ones.
    val artists = remember(catalog) { catalog?.artists.orEmpty().sortedBy { it.rank } }

    val searchedArtists = remember(catalog, query) {
        if (query.isEmpty()) emptyList() else artists.filter { artist ->
            // Every translation, not just the displayed one. Album titles are
            // English while a Korean reader sees Korean notes, so matching only
            // the current locale makes "vaganova" miss the very people the
            // Vaganova chip is meant to surface.
            val haystack = buildList {
                add(artist.name)
                artist.nameLocal?.let { add(it) }
                addAll(artist.affiliation.values)
                addAll(artist.note.values)
                addAll(regionTerms(artist.region))
            }
            haystack.any { it.lowercase().contains(query) }
        }
    }

    val searchedAlbums = remember(catalog, query) {
        if (query.isEmpty()) emptyList()
        else catalog?.albums.orEmpty()
            .filter { it.title.lowercase().contains(query) }
            .sortedByDescending { it.releaseDate }
            .take(40)
    }

    val favoriteAlbums = remember(catalog, favorites) {
        favorites.mapNotNull { CatalogRepository.album(it) }
    }

    Scaffold(
        topBar = {
            // Longer than the tab label on purpose — "Music" alone reads as a
            // music app. Same split the Log tab already uses (tab "Log",
            // header "Ballet Log").
            Text(
                stringResource(R.string.music_header),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        },
        bottomBar = {
            MusicSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" },
                onFocusChange = { searchFocused = it }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            if (catalog == null) {
                item { UnavailableState() }
                return@LazyColumn
            }

            if (searchFocused && !isSearching) {
                item {
                    SearchSuggestions(onPick = { searchQuery = it })
                }
                return@LazyColumn
            }

            if (isSearching) {
                if (searchedArtists.isEmpty() && searchedAlbums.isEmpty()) {
                    item { NoResults() }
                } else {
                    if (searchedArtists.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.music_accompanists))
                                ArtistList(searchedArtists) { openedArtist = it }
                            }
                        }
                    }
                    if (searchedAlbums.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionLabel(stringResource(R.string.music_albums))
                                AlbumRowList(searchedAlbums) { openedAlbum = it }
                            }
                        }
                    }
                }
                return@LazyColumn
            }

            CatalogRepository.discovery()?.let { album ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(stringResource(R.string.music_discovery))
                        DiscoveryCard(
                            album = album,
                            artist = CatalogRepository.artist(album.artistId),
                            onClick = { openedAlbum = album }
                        )
                    }
                }
            }

            val recent = CatalogRepository.recentAlbums()
            if (recent.isNotEmpty()) {
                item {
                    AlbumStrip(
                        title = stringResource(R.string.music_new_releases),
                        albums = recent,
                        onClick = { openedAlbum = it }
                    )
                }
            }

            if (favoriteAlbums.isNotEmpty()) {
                item {
                    AlbumStrip(
                        title = stringResource(R.string.music_saved),
                        albums = favoriteAlbums,
                        onClick = { openedAlbum = it }
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Not "Pianists" — some of the credited names are teachers,
                    // a ballet master, a dancer, or an orchestral release. They
                    // all made class accompaniment, whatever is behind it.
                    SectionLabel(stringResource(R.string.music_accompanists))
                    if (artists.isEmpty()) {
                        NoResults()
                    } else {
                        ArtistList(if (showAllArtists) artists else artists.take(12)) {
                            openedArtist = it
                        }
                        if (!showAllArtists && artists.size > 12) {
                            Text(
                                stringResource(R.string.music_show_all),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAllArtists = true }
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Hide the root NavigationBar while a detail overlay covers the screen,
    // matching how Notes and Log present their detail views.
    val bottomBarVisible = com.hyorita.balletlog.LocalBottomBarVisible.current
    val anyModalActive = openedArtist != null || openedAlbum != null
    DisposableEffect(anyModalActive) {
        if (anyModalActive) {
            bottomBarVisible.value = false
            // Opening a detail from search results leaves the field focused, so
            // the keyboard would otherwise sit on top of the album it just
            // opened. Drop focus as well or it springs back on return.
            focusManager.clearFocus()
            keyboard?.hide()
        }
        onDispose { bottomBarVisible.value = true }
    }

    // Leaving the tab stops playback — a preview must never outlive the screen
    // that started it.
    DisposableEffect(Unit) {
        onDispose { PreviewPlayer.stop() }
    }

    openedArtist?.let { artist ->
        BackHandler(enabled = openedAlbum == null) { openedArtist = null }
        Surface(modifier = Modifier.fillMaxSize()) {
            ArtistDetailScreen(
                artist = artist,
                onDismiss = { openedArtist = null },
                onOpenAlbum = { openedAlbum = it }
            )
        }
    }

    openedAlbum?.let { album ->
        BackHandler { openedAlbum = null }
        Surface(modifier = Modifier.fillMaxSize()) {
            AlbumDetailScreen(
                album = album,
                isFavorite = favorites.contains(album.id),
                onToggleFavorite = {
                    favorites = AlbumFavoritesPreferences.toggle(context, album.id)
                },
                onDismiss = { openedAlbum = null }
            )
        }
    }
}

/**
 * The country in the reader's language, in English, and as its code — so
 * "프랑스", "France" and "FR" all match. Searchable, never displayed.
 */
private fun regionTerms(code: String): List<String> {
    if (code.isEmpty()) return emptyList()
    val locale = Locale("", code)
    return listOf(
        code,
        locale.getDisplayCountry(Locale.getDefault()),
        locale.getDisplayCountry(Locale.US)
    ).filter { it.isNotEmpty() }
}

// MARK: - Sections

@Composable
private fun DiscoveryCard(
    album: CatalogAlbum,
    artist: CatalogArtist?,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AlbumArtwork(album, size = 132.dp, corner = 12.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    album.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (artist != null) {
                    Text(
                        artist.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    artist.affiliationText?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    artist.noteText?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumStrip(
    title: String,
    albums: List<CatalogAlbum>,
    onClick: (CatalogAlbum) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumTile(
                    album = album,
                    artist = CatalogRepository.artist(album.artistId),
                    onClick = { onClick(album) }
                )
            }
        }
    }
}

@Composable
private fun ArtistList(artists: List<CatalogArtist>, onClick: (CatalogArtist) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Column {
            artists.forEachIndexed { index, artist ->
                ArtistRow(artist = artist, onClick = { onClick(artist) })
                if (index < artists.size - 1) RowDivider()
            }
        }
    }
}

@Composable
private fun AlbumRowList(albums: List<CatalogAlbum>, onClick: (CatalogAlbum) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Column {
            albums.forEachIndexed { index, album ->
                AlbumRow(
                    album = album,
                    artist = CatalogRepository.artist(album.artistId),
                    onClick = { onClick(album) }
                )
                if (index < albums.size - 1) RowDivider()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchSuggestions(onPick: (String) -> Unit) {
    // Curated ways in, deliberately mixing axes — a country, a school, a style,
    // a composer. A row of countries alone would read as a classification of
    // these people; a mixed row reads as "try one of these", which is what it is.
    //
    // The label is translated but the query is not: album titles in this
    // repertoire are English, so a Korean query for "바가노바" would miss
    // "Vaganova Ballet Class Music" entirely.
    val suggestions = listOf(
        stringResource(R.string.music_suggest_france) to "france",
        stringResource(R.string.music_suggest_italy) to "italy",
        stringResource(R.string.music_suggest_vaganova) to "vaganova",
        stringResource(R.string.music_suggest_pop) to "pop",
        stringResource(R.string.music_suggest_jazz) to "jazz",
        stringResource(R.string.music_suggest_disney) to "disney",
        stringResource(R.string.music_suggest_chopin) to "chopin",
        stringResource(R.string.music_suggest_improvised) to "improvis"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(stringResource(R.string.music_try_searching))
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { (label, query) ->
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clickable { onPick(query) }
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// MARK: - Shared pieces

@Composable
fun AlbumArtwork(album: CatalogAlbum, size: Dp, corner: Dp = 10.dp) {
    val pixels = with(LocalDensity.current) { size.roundToPx() }
    AsyncImage(
        model = album.artworkUrl(pixels),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

@Composable
private fun AlbumTile(album: CatalogAlbum, artist: CatalogArtist?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(124.dp)
            .height(190.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AlbumArtwork(album, size = 124.dp)
        Text(
            album.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (artist != null) {
            Text(
                artist.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Album as a row — used in search results, where a mixed list of accompanists
 * and albums needs one consistent shape.
 */
@Composable
private fun AlbumRow(album: CatalogAlbum, artist: CatalogArtist?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AlbumArtwork(album, size = 52.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                album.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(artist?.name, album.year).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Chevron()
    }
}

/**
 * Artists get rows and albums get artwork tiles so the two never read as the
 * same kind of thing.
 */
@Composable
private fun ArtistRow(artist: CatalogArtist, onClick: () -> Unit) {
    val cover = artist.coverAlbumId?.let { CatalogRepository.album(it) }
        ?: CatalogRepository.albums(artist.id).firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (cover != null) {
            AlbumArtwork(cover, size = 52.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // The base shows as an affiliation, never as a country label.
            artist.affiliationText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                stringResource(R.string.music_album_count, artist.albumCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Chevron()
    }
}

@Composable
private fun Chevron() {
    Icon(
        Icons.Default.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(18.dp)
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        modifier = Modifier.padding(start = 78.dp)
    )
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun NoResults() {
    Text(
        stringResource(R.string.no_results),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
    )
}

/**
 * Shown when neither the cache, the bundled seed, nor the network produced a
 * catalog. Phrased as a state, not an error — this is content, and the app is
 * fine without it.
 */
@Composable
private fun UnavailableState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 40.dp, end = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(34.dp)
        )
        Text(
            stringResource(R.string.music_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Bottom-pinned, matching the Notes tab's search idiom. */
@Composable
private fun MusicSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.imePadding()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { onFocusChange(it.isFocused) },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    stringResource(R.string.search),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) { inner() }
                                if (query.isNotEmpty()) {
                                    IconButton(
                                        onClick = onClear,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
