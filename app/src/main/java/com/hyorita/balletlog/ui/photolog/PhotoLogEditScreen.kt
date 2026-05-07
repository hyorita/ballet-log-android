package com.hyorita.balletlog.ui.photolog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.roundToInt
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.PhotoLogStorage
import com.hyorita.balletlog.data.model.PhotoLog
import com.hyorita.balletlog.data.model.PhotoLogTag
import com.hyorita.balletlog.data.model.WorkoutInfo
import kotlinx.coroutines.delay

private enum class MetaField { Workout, Studio, Level, Teacher }

private sealed class WorkoutFetchState {
    object Idle : WorkoutFetchState()
    object Fetching : WorkoutFetchState()
    data class Found(val workout: WorkoutInfo) : WorkoutFetchState()
    object NotFound : WorkoutFetchState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoLogEditScreen(
    target: EditorTarget,
    vm: PhotoLogViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val existing = (target as? EditorTarget.Edit)?.log

    // --- State ---
    var photoName by remember { mutableStateOf(existing?.photoPath) }
    var caption by remember { mutableStateOf(existing?.caption.orEmpty()) }
    var captionIsWhite by remember { mutableStateOf(existing?.captionIsWhite ?: true) }
    var kcal by remember { mutableStateOf(existing?.kcal) }
    var durationMin by remember { mutableStateOf(existing?.durationMin) }
    var avgBPM by remember { mutableStateOf(existing?.avgBPM) }
    var maxBPM by remember { mutableStateOf(existing?.maxBPM) }
    var studio by remember {
        mutableStateOf(existing?.tags?.getOrNull(0).orEmpty())
    }
    var level by remember {
        mutableStateOf(existing?.tags?.getOrNull(1).orEmpty())
    }
    var teacher by remember {
        mutableStateOf(existing?.tags?.getOrNull(2).orEmpty())
    }
    var isFavorite by remember { mutableStateOf(existing?.isFavorite ?: false) }
    var date by remember { mutableStateOf(existing?.date ?: System.currentTimeMillis()) }

    // Caption transform — dp-relative offsets so position is roughly stable
    // across screen densities. Scale is unitless (1.0 == base size).
    var captionOffsetXDp by remember {
        mutableStateOf((existing?.captionX ?: 0.0).toFloat())
    }
    var captionOffsetYDp by remember {
        mutableStateOf((existing?.captionY ?: 0.0).toFloat())
    }
    var captionScale by remember {
        mutableStateOf((existing?.captionScale ?: 1.0).toFloat())
    }
    val density = LocalDensity.current

    var isComposingText by remember { mutableStateOf(false) }
    var activeMeta by remember { mutableStateOf<MetaField?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Match ClassLog editor's picker contract so the same OS picker UI
    // shows up across both editors (avoids OEM routing inconsistencies).
    // PickMultipleVisualMedia requires maxItems > 1; we keep the default
    // and just take the first picked Uri, since PhotoLog is one-photo-per-log.
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        val uri = uris.firstOrNull()
        if (uri == null && photoName == null) {
            onDismiss()
            return@rememberLauncherForActivityResult
        }
        uri?.let {
            vm.savePhotoFromUri(it) { saved, takenDate ->
                if (saved != null) photoName = saved
                if (takenDate != null && existing == null) date = takenDate
            }
        }
    }

    // Auto-open picker for new logs
    LaunchedEffect(target) {
        if (target == EditorTarget.New && photoName == null) {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Photo background
        photoName?.let { name ->
            AsyncImage(
                model = PhotoLogStorage.fileFor(context, name),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom gradient
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.25f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (captionIsWhite) Color.Black.copy(alpha = 0.88f)
                            else Color.White.copy(alpha = 0.5f)
                        )
                    )
                )
        )

        // Top bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.cancel),
                onClick = onDismiss
            )
            Spacer(Modifier.weight(1f))
            CircleIconButton(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                tint = if (isFavorite) Color(0xFFE91E63) else Color.White,
                contentDescription = stringResource(R.string.photolog_favorite),
                onClick = { isFavorite = !isFavorite }
            )
            Spacer(Modifier.width(10.dp))
            CircleIconButton(
                icon = Icons.Default.Check,
                contentDescription = stringResource(R.string.save),
                enabled = photoName != null && !isSaving,
                onClick = onClick@{
                    val name = photoName ?: return@onClick
                    isSaving = true
                    // Preserve index meaning: tags[0]=studio, [1]=level, [2]=teacher.
                    // Filtering blanks here would shift e.g. a "level only" entry
                    // into the studio slot on reload — what reproduced the
                    // "wrong category recent tags" bug.
                    val tags = listOf(studio, level, teacher)
                    val log = if (existing != null) {
                        existing.copy(
                            photoPath = name,
                            caption = caption,
                            captionX = captionOffsetXDp.toDouble(),
                            captionY = captionOffsetYDp.toDouble(),
                            captionScale = captionScale.toDouble(),
                            captionIsWhite = captionIsWhite,
                            kcal = kcal,
                            durationMin = durationMin,
                            avgBPM = avgBPM,
                            maxBPM = maxBPM,
                            tagsJson = com.google.gson.Gson().toJson(tags),
                            isFavorite = isFavorite,
                            date = date
                        )
                    } else {
                        PhotoLog.create(
                            photoPath = name,
                            caption = caption,
                            captionX = captionOffsetXDp.toDouble(),
                            captionY = captionOffsetYDp.toDouble(),
                            captionScale = captionScale.toDouble(),
                            captionIsWhite = captionIsWhite,
                            kcal = kcal,
                            durationMin = durationMin,
                            avgBPM = avgBPM,
                            maxBPM = maxBPM,
                            tags = tags,
                            isFavorite = isFavorite,
                            date = date
                        )
                    }
                    vm.upsert(log, isNew = existing == null)
                    onDismiss()
                }
            )
        }

        // Right pictogram column
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.End
        ) {
            CirclePicto(
                icon = Icons.Default.TextFormat,
                active = caption.isNotBlank(),
                contentDescription = stringResource(R.string.photolog_meta_text),
                onClick = { isComposingText = true }
            )
            CirclePicto(
                icon = Icons.Default.Tonality,
                active = false,
                contentDescription = stringResource(R.string.photolog_caption_color),
                onClick = { captionIsWhite = !captionIsWhite }
            )
            CirclePicto(
                icon = Icons.Default.MonitorHeart,
                active = kcal != null || durationMin != null || avgBPM != null || maxBPM != null,
                contentDescription = stringResource(R.string.photolog_meta_workout),
                onClick = { activeMeta = MetaField.Workout }
            )
            CirclePicto(
                icon = Icons.Default.Place,
                active = studio.isNotBlank(),
                contentDescription = stringResource(R.string.photolog_meta_studio),
                onClick = { activeMeta = MetaField.Studio }
            )
            CirclePicto(
                icon = Icons.Default.BarChart,
                active = level.isNotBlank(),
                contentDescription = stringResource(R.string.photolog_meta_level),
                onClick = { activeMeta = MetaField.Level }
            )
            CirclePicto(
                icon = Icons.Default.Person,
                active = teacher.isNotBlank(),
                contentDescription = stringResource(R.string.photolog_meta_teacher),
                onClick = { activeMeta = MetaField.Teacher }
            )
        }

        // Live workout overlay
        val hasWorkout = kcal != null || durationMin != null || avgBPM != null || maxBPM != null
        val tags = listOf(studio, level, teacher).filter { it.isNotBlank() }
        if (hasWorkout) {
            LiveWorkoutOverlay(
                kcal = kcal,
                durationMin = durationMin,
                avgBPM = avgBPM,
                maxBPM = maxBPM,
                hasTags = tags.isNotEmpty(),
                isWhite = captionIsWhite,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        if (tags.isNotEmpty()) {
            LiveTagsOverlay(
                tags = tags,
                isWhite = captionIsWhite,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        // Caption sticker — drag to move, pinch to scale, tap to edit.
        if (caption.isNotEmpty() && !isComposingText) {
            val captionColor = if (captionIsWhite) Color.White else Color.Black
            Text(
                text = caption,
                color = captionColor,
                fontSize = captionFontSize(caption),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset {
                        IntOffset(
                            with(density) { captionOffsetXDp.dp.toPx().roundToInt() },
                            with(density) { captionOffsetYDp.dp.toPx().roundToInt() }
                        )
                    }
                    .graphicsLayer {
                        scaleX = captionScale
                        scaleY = captionScale
                    }
                    .padding(horizontal = 24.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                            captionOffsetXDp += with(density) { pan.x.toDp().value }
                            captionOffsetYDp += with(density) { pan.y.toDp().value }
                            captionScale = (captionScale * zoom).coerceIn(0.5f, 3.0f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { isComposingText = true })
                    }
            )
        }
    }

    // Composer overlay
    if (isComposingText) {
        CaptionComposer(
            caption = caption,
            isWhite = captionIsWhite,
            onCaptionChange = { caption = it.take(140) },
            onClose = { isComposingText = false }
        )
    }

    activeMeta?.let { field ->
        if (field == MetaField.Workout) {
            WorkoutSheet(
                date = date,
                kcal = kcal, durationMin = durationMin,
                avgBPM = avgBPM, maxBPM = maxBPM,
                onApply = { k, d, a, m ->
                    kcal = k; durationMin = d; avgBPM = a; maxBPM = m
                    activeMeta = null
                },
                onFetch = { onResult ->
                    vm.fetchWorkoutForDate(date) { wo ->
                        onResult(wo)
                    }
                },
                onDismiss = { activeMeta = null }
            )
        } else {
            val (type, current, setter) = when (field) {
                MetaField.Studio -> Triple("studio", studio) { v: String -> studio = v }
                MetaField.Level -> Triple("level", level) { v: String -> level = v }
                MetaField.Teacher -> Triple("teacher", teacher) { v: String -> teacher = v }
                else -> Triple("", "") { _: String -> }
            }
            val tagFlow = when (field) {
                MetaField.Studio -> vm.studioTags
                MetaField.Level -> vm.levelTags
                MetaField.Teacher -> vm.teacherTags
                else -> vm.studioTags
            }
            TagInputSheet(
                field = field,
                value = current,
                tags = tagFlow.collectAsState().value,
                onValueChange = setter,
                onDeleteTag = { vm.deleteTag(it) },
                onDismiss = { activeMeta = null }
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(icon, contentDescription = contentDescription, tint = tint)
        }
    }
}

@Composable
private fun CirclePicto(
    icon: ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primary
                else Color.Black.copy(alpha = 0.30f)
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = Color.White)
        }
    }
}

@Composable
private fun LiveWorkoutOverlay(
    kcal: Int?,
    durationMin: Int?,
    avgBPM: Int?,
    maxBPM: Int?,
    hasTags: Boolean,
    isWhite: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isWhite) Color.White else Color.Black
    // Padding mirrors PhotoLogCard so workout block sits in the same spot
    // before and after save (124 with tags, 64 without).
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(end = 18.dp, bottom = if (hasTags) 124.dp else 64.dp)
    ) {
        kcal?.let { k ->
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$k",
                    color = textColor,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "kcal",
                    color = textColor.copy(alpha = 0.55f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        val parts = listOfNotNull(
            durationMin?.let { "${it}min" },
            avgBPM?.let { "avg $it" },
            maxBPM?.let { "max $it" }
        )
        if (parts.isNotEmpty()) {
            Text(
                parts.joinToString(" · "),
                color = textColor.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LiveTagsOverlay(
    tags: List<String>,
    isWhite: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isWhite) Color.White else Color.Black
    val bg = if (isWhite) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.10f)
    val dividerColor = if (isWhite) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)
    // Bottom padding 82 matches PhotoLogCard's tag row position; the hairline
    // divider also mirrors the saved card so the editor and final view align.
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 82.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(dividerColor)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        tag,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptionComposer(
    caption: String,
    isWhite: Boolean,
    onCaptionChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // TextFieldValue lets us own selection so re-entering the composer keeps
    // the cursor at the end of the existing caption instead of jumping to 0.
    var tfv by remember {
        mutableStateOf(TextFieldValue(caption, TextRange(caption.length)))
    }
    LaunchedEffect(Unit) {
        delay(50)
        focus.requestFocus()
        keyboard?.show()
    }
    val textColor = if (isWhite) Color.White else Color.Black
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.30f))
            .clickable { onClose() }
            .imePadding()
    ) {
        BasicTextField(
            value = tfv,
            onValueChange = { newTfv ->
                tfv = newTfv
                onCaptionChange(newTfv.text)
            },
            textStyle = TextStyle(
                color = textColor,
                fontSize = captionFontSize(tfv.text),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(textColor),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .focusRequester(focus)
        )
    }
}

private fun captionFontSize(text: String) = when {
    text.length <= 60 -> 22.sp
    text.length <= 120 -> 19.sp
    else -> 16.sp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutSheet(
    date: Long,
    kcal: Int?, durationMin: Int?, avgBPM: Int?, maxBPM: Int?,
    onApply: (Int?, Int?, Int?, Int?) -> Unit,
    onFetch: (onResult: (WorkoutInfo?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var k by remember { mutableStateOf(kcal) }
    var d by remember { mutableStateOf(durationMin) }
    var a by remember { mutableStateOf(avgBPM) }
    var m by remember { mutableStateOf(maxBPM) }
    var fetchState by remember { mutableStateOf<WorkoutFetchState>(WorkoutFetchState.Idle) }

    // Auto-apply on any dismissal (drag-down, scrim tap, ✓ button, back press).
    // Manual edits stay even if user closes the sheet without explicit confirm.
    val applyAndClose: () -> Unit = { onApply(k, d, a, m) }

    // skipPartiallyExpanded → sheet opens fully so the keyboard never covers
    // the manual-entry rows on phones with shorter screens.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = applyAndClose,
        sheetState = sheetState,
        contentWindowInsets = {
            BottomSheetDefaults.windowInsets.union(WindowInsets.ime)
        },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            SheetHeader(
                title = stringResource(R.string.photolog_meta_workout),
                onConfirm = applyAndClose
            )

            // Health Connect fetch — primary action card
            Surface(
                onClick = {
                    if (fetchState != WorkoutFetchState.Fetching) {
                        fetchState = WorkoutFetchState.Fetching
                        onFetch { wo ->
                            if (wo != null) {
                                fetchState = WorkoutFetchState.Found(wo)
                                k = wo.activeCalories
                                d = wo.durationMinutes
                                a = wo.avgHeartRate.takeIf { it > 0 }
                                m = wo.maxHeartRate.takeIf { it > 0 }
                            } else {
                                fetchState = WorkoutFetchState.NotFound
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.MonitorHeart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.find_ballet_workout),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        when (val s = fetchState) {
                            is WorkoutFetchState.Found -> Text(
                                "${s.workout.durationMinutes}min · ${s.workout.activeCalories} kcal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            WorkoutFetchState.NotFound -> Text(
                                stringResource(R.string.no_workout_found),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            else -> {}
                        }
                    }
                    if (fetchState == WorkoutFetchState.Fetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.photolog_manual_entry),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            CompactNumberRow(stringResource(R.string.active_cal), k, "kcal") { k = it }
            CompactNumberRow(stringResource(R.string.duration), d, "min") { d = it }
            CompactNumberRow(stringResource(R.string.avg_bpm), a, "bpm") { a = it }
            CompactNumberRow(stringResource(R.string.max_bpm), m, "bpm") { m = it }

            if (k != null || d != null || a != null || m != null) {
                TextButton(
                    onClick = { k = null; d = null; a = null; m = null },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.photolog_clear_workout),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onConfirm: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 16.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface)
                .clickable(onClick = onConfirm),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CompactNumberRow(
    label: String,
    value: Int?,
    suffix: String,
    onChange: (Int?) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        BasicTextField(
            value = value?.toString().orEmpty(),
            onValueChange = { v ->
                val trimmed = v.filter { it.isDigit() }
                onChange(trimmed.toIntOrNull())
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.width(80.dp),
            decorationBox = { inner ->
                if (value == null) {
                    Text(
                        "0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                inner()
            }
        )
        Spacer(Modifier.width(6.dp))
        Text(
            suffix,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagInputSheet(
    field: MetaField,
    value: String,
    tags: List<PhotoLogTag>,
    onValueChange: (String) -> Unit,
    onDeleteTag: (PhotoLogTag) -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (field) {
        MetaField.Studio -> stringResource(R.string.photolog_meta_studio)
        MetaField.Level -> stringResource(R.string.photolog_meta_level)
        MetaField.Teacher -> stringResource(R.string.photolog_meta_teacher)
        else -> ""
    }
    val placeholder = when (field) {
        MetaField.Studio -> stringResource(R.string.photolog_studio_placeholder)
        MetaField.Level -> stringResource(R.string.photolog_meta_level)
        MetaField.Teacher -> stringResource(R.string.photolog_meta_teacher)
        else -> ""
    }
    val icon = when (field) {
        MetaField.Studio -> Icons.Default.Place
        MetaField.Level -> Icons.Default.BarChart
        MetaField.Teacher -> Icons.Default.Person
        else -> Icons.Default.Place
    }

    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Own selection so re-opening the sheet drops the cursor at the end of
    // the existing value instead of jumping to position 0.
    var tfv by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    LaunchedEffect(field) {
        delay(80)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tagToDeleteConfirm by remember { mutableStateOf<PhotoLogTag?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = {
            BottomSheetDefaults.windowInsets.union(WindowInsets.ime)
        },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            SheetHeader(title = title, onConfirm = onDismiss)

            // Borderless input row + hairline divider — iOS style
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = tfv,
                        onValueChange = { newTfv ->
                            tfv = newTfv
                            onValueChange(newTfv.text)
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focus),
                        decorationBox = { inner ->
                            if (tfv.text.isEmpty()) {
                                Text(
                                    placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )
            }

            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.photolog_recent_tags),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.take(20).forEach { tag ->
                        SuggestionChip(
                            value = tag.value,
                            onTap = {
                                tfv = TextFieldValue(tag.value, TextRange(tag.value.length))
                                onValueChange(tag.value)
                            },
                            onLongPress = { tagToDeleteConfirm = tag }
                        )
                    }
                }
            }
        }
    }

    tagToDeleteConfirm?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDeleteConfirm = null },
            title = { Text(stringResource(R.string.photolog_tag_delete_title)) },
            text = { Text("\"${tag.value}\"") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTag(tag)
                    tagToDeleteConfirm = null
                }) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDeleteConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionChip(
    value: String,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = Modifier.combinedClickable(
            onClick = onTap,
            onLongClick = onLongPress
        )
    ) {
        Text(
            value,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
