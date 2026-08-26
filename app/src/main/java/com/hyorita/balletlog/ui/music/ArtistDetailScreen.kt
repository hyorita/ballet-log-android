package com.hyorita.balletlog.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.CatalogRepository
import com.hyorita.balletlog.data.model.CatalogAlbum
import com.hyorita.balletlog.data.model.CatalogArtist

/**
 * One accompanist: who they are, and everything they've released.
 *
 * Plain flow on the background rather than the card idiom, matching
 * NoteDetailScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artist: CatalogArtist,
    onDismiss: () -> Unit,
    onOpenAlbum: (CatalogAlbum) -> Unit
) {
    val albums = remember(artist.id) { CatalogRepository.albums(artist.id) }
    var showAllAlbums by remember(artist.id) { mutableStateOf(false) }
    val shown = if (showAllAlbums) albums else albums.take(6)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        artist.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        artist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    // Only a genuine local spelling, never a transliteration —
                    // the catalog guarantees this holds real script.
                    artist.nameLocal?.takeIf { it != artist.name }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // The base shows as an affiliation, never as a country label.
                    artist.affiliationText?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // The curation note is the point of this screen. Shown as written in
            // the catalog — the app does not compose its own copy here.
            artist.noteText?.let { note ->
                item {
                    Text(note, style = MaterialTheme.typography.bodyLarge)
                }
            }

            item {
                StreamingLinks(
                    appleUrl = artist.appleUrl,
                    youTubeQuery = artist.name
                )
            }

            item {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }

            item {
                SectionLabel(stringResource(R.string.music_albums))
            }

            items(shown.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { album ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenAlbum(album) },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AlbumGridArtwork(album)
                            Text(
                                album.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                album.year,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    // Keep a lone last album at column width instead of letting
                    // it stretch across the row.
                    if (pair.size == 1) Column(modifier = Modifier.weight(1f)) {}
                }
            }

            if (!showAllAlbums && albums.size > 6) {
                item {
                    Text(
                        stringResource(R.string.music_show_all_albums, albums.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAllAlbums = true }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/** Grid cells are as wide as the column, so the artwork sizes itself. */
@Composable
private fun AlbumGridArtwork(album: CatalogAlbum) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        AlbumArtwork(album, size = maxWidth, corner = 10.dp)
    }
}
