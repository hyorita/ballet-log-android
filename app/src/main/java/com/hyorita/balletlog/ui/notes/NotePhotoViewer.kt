package com.hyorita.balletlog.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hyorita.balletlog.data.PhotoManager

/**
 * Full-screen photo viewer for Note photos. Mirrors iOS NotePhotoViewerView —
 * horizontal pager between photos, pinch-to-zoom and pan within each photo,
 * top-right close button. Reset zoom when the user swipes to a new page.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotePhotoViewer(
    photoFileNames: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (photoFileNames.size - 1).coerceAtLeast(0))
    ) { photoFileNames.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ZoomablePhoto(
                fileName = photoFileNames[page],
                resetKey = page == pagerState.currentPage
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.30f))
                .size(44.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ZoomablePhoto(
    fileName: String,
    resetKey: Boolean
) {
    val context = LocalContext.current
    var scale by remember(fileName) { mutableStateOf(1f) }
    var offsetX by remember(fileName) { mutableStateOf(0f) }
    var offsetY by remember(fileName) { mutableStateOf(0f) }

    // Reset zoom/pan when this page becomes inactive so swiping back shows
    // the photo at base scale.
    LaunchedEffect(resetKey) {
        if (!resetKey) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(fileName) {
                // Custom gesture loop: pinch (2+ pointers) and zoomed pan (1
                // pointer while scale > 1) are consumed; a single-pointer drag
                // at base scale is left for the parent HorizontalPager so the
                // user can swipe between photos.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            if (newScale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                            event.changes.forEach { it.consume() }
                        } else if (scale > 1f) {
                            val pan = event.calculatePan()
                            if (pan != androidx.compose.ui.geometry.Offset.Zero) {
                                offsetX += pan.x
                                offsetY += pan.y
                                event.changes.forEach {
                                    if (it.positionChange() != androidx.compose.ui.geometry.Offset.Zero) {
                                        it.consume()
                                    }
                                }
                            }
                        }
                        // pressed == 1 && scale == 1 → don't consume,
                        // pager picks up the horizontal drag.
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(fileName) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = PhotoManager.getPhotoFile(context, fileName),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}
