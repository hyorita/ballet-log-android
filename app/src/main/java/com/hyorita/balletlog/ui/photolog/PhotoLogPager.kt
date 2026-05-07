package com.hyorita.balletlog.ui.photolog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.model.PhotoLog
import com.hyorita.balletlog.ui.common.sharePhotoLog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoLogPager(
    logs: List<PhotoLog>,
    startId: String,
    onDismiss: () -> Unit,
    onEdit: (PhotoLog) -> Unit,
    onDelete: (PhotoLog) -> Unit,
    onToggleFavorite: (PhotoLog) -> Unit
) {
    val startIndex = remember(startId, logs) {
        logs.indexOfFirst { it.id == startId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startIndex) { logs.size }
    val current = logs.getOrNull(pagerState.currentPage)

    var showMore by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var dragY by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY > with(density) { 120.dp.toPx() }) {
                            onDismiss()
                        } else {
                            dragY = 0f
                        }
                    },
                    onDragCancel = { dragY = 0f }
                ) { _, delta ->
                    if (delta > 0) dragY = (dragY + delta).coerceAtLeast(0f)
                }
            }
            .offset { IntOffset(0, dragY.roundToInt()) }
    ) {
        HorizontalPager(state = pagerState) { page ->
            logs.getOrNull(page)?.let { log ->
                PhotoLogCard(photoLog = log)
            }
        }

        // Top-right action column — Share is always visible (matches iOS),
        // the rest collapse behind the More toggle.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.End
        ) {
            ActionButton(
                icon = Icons.Default.Share,
                contentDescription = stringResource(R.string.photolog_share),
                onClick = {
                    if (isSharing) return@ActionButton
                    val log = current ?: return@ActionButton
                    isSharing = true
                    Thread {
                        try {
                            sharePhotoLog(context, log)
                        } catch (e: Throwable) {
                            android.util.Log.e("PhotoLogShare", "render failed", e)
                        } finally {
                            isSharing = false
                        }
                    }.start()
                }
            )
            ActionButton(
                icon = if (showMore) Icons.Default.Close else Icons.Default.MoreVert,
                contentDescription = if (showMore)
                    stringResource(R.string.photolog_close)
                else
                    stringResource(R.string.photolog_more),
                onClick = { showMore = !showMore }
            )
            AnimatedVisibility(
                visible = showMore,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val isFav = current?.isFavorite ?: false
                    ActionButton(
                        icon = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.photolog_favorite),
                        tint = if (isFav) Color(0xFFE91E63) else Color.White,
                        onClick = { current?.let(onToggleFavorite) }
                    )
                    ActionButton(
                        icon = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.photolog_edit),
                        onClick = { current?.let(onEdit) }
                    )
                    ActionButton(
                        icon = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        onClick = { showDeleteConfirm = true }
                    )
                }
            }
        }

        // Top-left close
        ActionButton(
            icon = Icons.Default.Close,
            contentDescription = stringResource(R.string.photolog_close),
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 12.dp, start = 12.dp)
        )
    }

    if (showDeleteConfirm && current != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.photolog_delete_title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(current)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = tint)
        }
    }
}
