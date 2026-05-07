package com.hyorita.balletlog.ui.notes

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.PhotoManager
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.Note
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteDetailScreen(
    note: Note,
    classLogs: List<ClassLog>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenLog: (ClassLog) -> Unit,
    onOpenPhoto: (Int) -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    val linkedLog = remember(note.linkedLogId, classLogs) {
        note.linkedLogId?.let { id -> classLogs.find { it.id == id } }
    }
    val openUrl: (String) -> Unit = { url ->
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin"
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date (top, small) — iOS NoteDetailView puts the timestamp above
            // the title in tertiary text.
            item {
                Text(
                    text = dateFormat.format(Date(note.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Title (bold)
            if (note.title.isNotEmpty()) {
                item {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            // Body — plain text, no card wrapping
            if (note.content.isNotEmpty()) {
                item {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            // Tags — accent-tinted capsules with FlowRow for natural wrapping
            if (note.tags.isNotEmpty()) {
                item {
                    NoteDetailTagsRow(tags = note.tags)
                }
            }

            // 사진 캐러셀
            if (note.photoFileNames.isNotEmpty()) {
                item {
                    NotePhotoCarousel(
                        photoFileNames = note.photoFileNames,
                        onTap = { idx -> onOpenPhoto(idx) }
                    )
                }
            }

            // Linked Class — clickable, navigates to ClassLog DetailScreen
            linkedLog?.let { log ->
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(stringResource(R.string.linked_class),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp))
                        Card(
                            onClick = { onOpenLog(log) },
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_ballet_shoe),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                        .format(Date(log.date)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // Reference Link — domain-specific card (YouTube / Instagram / generic)
            note.urlLink?.takeIf { it.isNotEmpty() }?.let { urlStr ->
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(stringResource(R.string.reference_link),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp))
                        val ytId = extractYouTubeId(urlStr)
                        when {
                            ytId != null -> YouTubeThumbnailCard(ytId) { openUrl(urlStr) }
                            isInstagramUrl(urlStr) ->
                                InstagramLinkCard(displayDomain(urlStr)) { openUrl(urlStr) }
                            else ->
                                GenericLinkCard(displayDomain(urlStr)) { openUrl(urlStr) }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_note_title)) },
            text = { Text(stringResource(R.string.delete_note_title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

// MARK: - Tags row

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteDetailTagsRow(tags: List<String>) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = tag,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

// MARK: - Photo carousel

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotePhotoCarousel(
    photoFileNames: List<String>,
    onTap: (Int) -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { photoFileNames.size })
    val multi = photoFileNames.size > 1

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            pageSpacing = 8.dp
        ) { page ->
            AsyncImage(
                model = PhotoManager.getPhotoFile(context, photoFileNames[page]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onTap(page) }
            )
        }

        // Bottom gradient — keeps the indicator readable on bright photos
        if (multi) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )
        }

        // Top-right page counter "1 / 3"
        if (multi) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Text(
                    "${pagerState.currentPage + 1} / ${photoFileNames.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Active dot wider, inactive smaller — bottom-center
        if (multi) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(photoFileNames.size) { i ->
                    val isActive = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .let { if (isActive) it.width(18.dp) else it.width(6.dp) }
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color.White
                                else Color.White.copy(alpha = 0.55f)
                            )
                    )
                }
            }
        }
    }
}

// MARK: - URL helpers

private fun extractYouTubeId(url: String): String? {
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

private fun isInstagramUrl(url: String): Boolean {
    val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return false
    return host == "instagram.com" || host.endsWith(".instagram.com")
}

private fun displayDomain(url: String): String {
    val host = runCatching { Uri.parse(url).host }.getOrNull() ?: return url
    return host.removePrefix("www.")
}

// MARK: - Reference Link cards

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YouTubeThumbnailCard(videoId: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Icon(
            Icons.Default.PlayCircleFilled,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(56.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstagramLinkCard(domain: String, onClick: () -> Unit) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF53579),
            Color(0xFFFD8221),
            Color(0xFFFDC74D)
        )
    )
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Instagram",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenericLinkCard(domain: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                domain,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
