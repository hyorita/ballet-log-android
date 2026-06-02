package com.hyorita.balletlog.ui.photolog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hyorita.balletlog.data.PhotoLogStorage
import com.hyorita.balletlog.data.model.PhotoLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-bleed photo card matching iOS `PhotoLogCardView`.
 *
 * Layout: photo (cover) + top/bottom gradients + draggable caption sticker +
 * bottom-right meta block (date / kcal / sub-stats) + bottom tag chip row.
 */
@Composable
fun PhotoLogCard(
    photoLog: PhotoLog,
    modifier: Modifier = Modifier,
    addPhotoSlot: (@Composable () -> Unit)? = null,
    onWorkoutCardTap: (() -> Unit)? = null
) {
    if (photoLog.isWorkoutOnly) {
        WorkoutPlaceholderCard(
            photoLog = photoLog,
            modifier = modifier,
            addPhotoSlot = addPhotoSlot,
            onTap = onWorkoutCardTap
        )
        return
    }

    val context = LocalContext.current
    val displayName = photoLog.filteredPhotoPath ?: photoLog.photoPath
    val photoFile = remember(displayName) { PhotoLogStorage.fileFor(context, displayName) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = photoFile,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top gradient (40%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        // Bottom gradient (25%)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.25f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )

        // Caption sticker (centered, position/scale applied)
        if (photoLog.caption.isNotEmpty()) {
            val captionColor = if (photoLog.captionIsWhite) Color.White else Color.Black
            val captionFontSize = when {
                photoLog.caption.length <= 60 -> 22.sp
                photoLog.caption.length <= 120 -> 19.sp
                else -> 16.sp
            }
            Text(
                text = photoLog.caption,
                color = captionColor,
                fontSize = captionFontSize,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 8,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = photoLog.captionX.dp,
                        y = photoLog.captionY.dp
                    )
                    .graphicsLayer {
                        scaleX = photoLog.captionScale.toFloat()
                        scaleY = photoLog.captionScale.toFloat()
                    }
                    .padding(horizontal = 24.dp)
            )
        }

        // Bottom-right meta block
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = bottomMetaPadding(photoLog))
        ) {
            Text(
                text = headerDate(photoLog.date),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            photoLog.kcal?.let { k ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$k",
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 56.sp
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "kcal",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            val subs = subStats(photoLog)
            if (subs.isNotEmpty()) {
                Text(
                    text = subs.joinToString(" · "),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom tag chips — skip blank slots (PhotoLog.tags keeps fixed
        // index meaning so an empty studio/level/teacher comes through as "").
        val visibleTags = photoLog.tags.filter { it.isNotBlank() }
        if (visibleTags.isNotEmpty()) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp)
                    .padding(bottom = 82.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    visibleTags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                tag,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1.8: full-screen layout for workout-only entries (no photo yet).
 * Mirrors iOS — dark charcoal background, all meta and the "+ Add Photo"
 * pill stacked in the bottom-right corner (so the layout reads as a
 * "photo-shaped void waiting for a photo", not as a centered stat card).
 */
@Composable
private fun WorkoutPlaceholderCard(
    photoLog: PhotoLog,
    modifier: Modifier = Modifier,
    addPhotoSlot: (@Composable () -> Unit)? = null,
    onTap: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier)
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            Text(
                text = headerDate(photoLog.date),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            val kcal = photoLog.kcal
            if (kcal != null && kcal > 0) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$kcal",
                        color = Color.White,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 72.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "kcal",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
            }
            val subs = subStats(photoLog)
            if (subs.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subs.joinToString(" · "),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            if (addPhotoSlot != null) {
                Spacer(Modifier.height(16.dp))
                addPhotoSlot()
            }
        }
    }
}

private fun headerDate(millis: Long): String {
    val fmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    return fmt.format(Date(millis))
}

private fun subStats(log: PhotoLog): List<String> {
    val parts = mutableListOf<String>()
    log.durationMin?.let { parts.add("${it}min") }
    log.avgBPM?.let { parts.add("avg $it") }
    log.maxBPM?.let { parts.add("max $it") }
    return parts
}

private fun bottomMetaPadding(log: PhotoLog): androidx.compose.ui.unit.Dp {
    val hasWorkout = log.kcal != null || log.durationMin != null ||
        log.avgBPM != null || log.maxBPM != null
    val hasTags = log.tags.isNotEmpty()
    return when {
        hasWorkout && hasTags -> 124.dp
        hasWorkout -> 64.dp
        hasTags -> 118.dp
        else -> 64.dp
    }
}
