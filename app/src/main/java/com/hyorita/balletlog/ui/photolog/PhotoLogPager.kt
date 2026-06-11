package com.hyorita.balletlog.ui.photolog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HideImage
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.model.PhotoLog
import com.hyorita.balletlog.ui.common.sharePhotoLog
import com.hyorita.balletlog.util.debugLog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoLogPager(
    logs: List<PhotoLog>,
    startId: String,
    onDismiss: () -> Unit,
    onEdit: (PhotoLog) -> Unit,
    onDelete: (PhotoLog) -> Unit,
    onToggleFavorite: (PhotoLog) -> Unit,
    onAttachPhoto: (PhotoLog, Uri) -> Unit = { _, _ -> },
    onRemovePhoto: (PhotoLog) -> Unit = {}
) {
    val startIndex = remember(startId, logs) {
        logs.indexOfFirst { it.id == startId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startIndex) { logs.size }
    val current = logs.getOrNull(pagerState.currentPage)

    var showMore by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRemovePhotoConfirm by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var dragY by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    val context = LocalContext.current

    // 1.8: photo picker for "+ Add Photo" / tap-to-add on workout-only cards.
    // PickMultipleVisualMedia keeps parity with the Edit screen's picker so
    // the same OS sheet appears across both surfaces.
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        val uri = uris.firstOrNull() ?: return@rememberLauncherForActivityResult
        current?.let { onAttachPhoto(it, uri) }
    }
    val launchPhotoPicker: () -> Unit = {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

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
                PhotoLogCard(
                    photoLog = log,
                    onWorkoutCardTap = if (log.isWorkoutOnly) launchPhotoPicker else null,
                    addPhotoSlot = if (log.isWorkoutOnly) {
                        {
                            AddPhotoPill(
                                label = stringResource(R.string.photolog_add_photo),
                                onClick = launchPhotoPicker
                            )
                        }
                    } else null
                )
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
                            debugLog("PhotoLogShare", "render failed", e)
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
                    // 1.8: drop just the photo, keep workout data so the
                    // card returns to placeholder state. Only meaningful
                    // when the current log actually has a photo.
                    if (current?.isWorkoutOnly == false && current.hasWorkoutData) {
                        ActionButton(
                            icon = Icons.Default.HideImage,
                            contentDescription = stringResource(R.string.photolog_remove_photo),
                            onClick = { showRemovePhotoConfirm = true }
                        )
                    }
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

    if (showRemovePhotoConfirm && current != null) {
        AlertDialog(
            onDismissRequest = { showRemovePhotoConfirm = false },
            title = { Text(stringResource(R.string.photolog_remove_photo_title)) },
            text = { Text(stringResource(R.string.photolog_remove_photo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemovePhotoConfirm = false
                    showMore = false
                    onRemovePhoto(current)
                }) { Text(stringResource(R.string.photolog_remove_photo)) }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePhotoConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun AddPhotoPill(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.55f)
        )
    ) {
        Text(
            text = "+  $label",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
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
