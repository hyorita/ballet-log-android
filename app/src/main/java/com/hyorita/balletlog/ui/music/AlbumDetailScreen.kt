package com.hyorita.balletlog.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.CatalogRepository
import com.hyorita.balletlog.data.model.CatalogAlbum
import com.hyorita.balletlog.data.model.CatalogTrack
import com.hyorita.balletlog.data.model.ClassExercise

/**
 * One album: artwork, track list with 30-second previews, and ways out to a
 * streaming service for full playback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    album: CatalogAlbum,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val artist = remember(album.artistId) { CatalogRepository.artist(album.artistId) }

    var tracks by remember(album.id) { mutableStateOf<List<CatalogTrack>>(emptyList()) }
    var isLoadingTracks by remember(album.id) { mutableStateOf(true) }

    LaunchedEffect(album.id) {
        tracks = CatalogRepository.loadTracks(album.id)?.tracks.orEmpty()
        isLoadingTracks = false
    }

    DisposableEffect(album.id) {
        onDispose { PreviewPlayer.stop() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        album.title,
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
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AlbumArtwork(album, size = 132.dp, corner = 12.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            album.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (artist != null) {
                            Text(
                                artist.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        }
                        val count = if (tracks.isEmpty()) album.trackCount else tracks.size
                        Text(
                            "${album.year} · ${stringResource(R.string.music_track_count, count)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.clickable(onClick = onToggleFavorite),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) Color(0xFFE91E63)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(
                                    if (isFavorite) R.string.music_saved else R.string.music_save
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Apple Music stays even though YouTube Music is the more
                    // likely destination here: the artwork and previews are
                    // Apple's, and using them without linking back is a display
                    // requirement problem. Both, never one.
                    StreamingLinks(
                        appleUrl = album.appleUrl,
                        youTubeQuery = listOfNotNull(artist?.name, album.title)
                            .joinToString(" ")
                    )
                }
            }

            item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }

            // Playback is Apple's 30-second preview, but the duration on each
            // row is the full track — without saying so, a clip cutting out at
            // 0:30 reads as a bug rather than a limit.
            if (tracks.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.music_preview_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            when {
                isLoadingTracks && tracks.isEmpty() -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }

                tracks.isEmpty() -> item {
                    // Some albums have no track listing available. Not an error
                    // worth shouting about — the album still opens in a service.
                    Text(
                        stringResource(R.string.music_tracks_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp)
                    )
                }

                else -> items(tracks, key = { it.rowId }) { track ->
                    TrackRow(
                        track = track,
                        isPlaying = PreviewPlayer.isPlaying(track.rowId),
                        onTap = {
                            track.preview?.let { PreviewPlayer.toggle(track.rowId, it) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackRow(track: CatalogTrack, isPlaying: Boolean, onTap: () -> Unit) {
    val playable = !track.preview.isNullOrEmpty()
    val playLabel = stringResource(
        if (isPlaying) R.string.music_stop_preview else R.string.music_play_preview
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = playable, onClick = onTap)
                .semantics { contentDescription = "$playLabel, ${track.title}" }
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircleOutline,
                contentDescription = null,
                tint = if (playable) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val exercise = ClassExercise.labelFor(track.exercise)
                if (exercise != null || track.meter != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        exercise?.let {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        track.meter?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
            if (track.seconds > 0) {
                Text(
                    track.durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(start = 34.dp)
        )
    }
}

/**
 * Apple Music plus a YouTube Music search.
 *
 * Android-only addition: Apple Music's share is low among Korean Android users,
 * so a single Apple-only exit would strand most of them. The catalog carries no
 * YouTube ids — the query is assembled from artist and album name, which works
 * because ballet class album titles are unusually specific
 * ("Music for Ballet Class - Repertoire, Vol. 4").
 */
@Composable
internal fun StreamingLinks(appleUrl: String, youTubeQuery: String) {
    val context = LocalContext.current
    val open: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url)
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (appleUrl.isNotEmpty()) {
            LinkRow(stringResource(R.string.music_open_apple)) { open(appleUrl) }
        }
        if (youTubeQuery.isNotBlank()) {
            val encoded = android.net.Uri.encode(youTubeQuery)
            LinkRow(stringResource(R.string.music_open_youtube)) {
                open("https://music.youtube.com/search?q=$encoded")
            }
        }
    }
}

@Composable
internal fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
