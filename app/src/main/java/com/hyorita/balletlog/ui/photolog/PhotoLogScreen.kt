package com.hyorita.balletlog.ui.photolog

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.PhotoLogStorage
import com.hyorita.balletlog.data.TutorialPreferences
import com.hyorita.balletlog.data.model.PhotoLog
import com.hyorita.balletlog.ui.settings.SettingsScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoLogScreen(
    vm: PhotoLogViewModel = viewModel(),
    onOpenSettings: () -> Unit = {}
) {
    val logs by vm.photoLogs.collectAsState()
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }
    var viewerStartId by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var hasSeenTutorial by remember { mutableStateOf(TutorialPreferences.hasSeenLogTutorial(context)) }
    val showTutorial = !hasSeenTutorial && logs.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!hasSeenTutorial) {
                            TutorialPreferences.setLogTutorialSeen(context)
                            hasSeenTutorial = true
                        }
                        editorTarget = EditorTarget.New
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.photolog_new),
                            tint = if (showTutorial) MaterialTheme.colorScheme.primary
                            else LocalContentColor.current
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.photolog_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (showTutorial) {
                TutorialArrow()
            }
            if (logs.isEmpty()) {
                EmptyState()
            } else {
                PhotoLogGrid(
                    logs = logs,
                    onTap = { viewerStartId = it.id }
                )
            }
        }
    }

    // Full-screen pageable card viewer
    // Hide root NavBar whenever any inline overlay below is active so they
    // cover the full screen and IME padding doesn't double-count its inset.
    val bottomBarVisible = com.hyorita.balletlog.LocalBottomBarVisible.current
    val anyModalActive = viewerStartId != null || editorTarget != null || showSettings
    androidx.compose.runtime.DisposableEffect(anyModalActive) {
        if (anyModalActive) bottomBarVisible.value = false
        onDispose { bottomBarVisible.value = true }
    }

    viewerStartId?.let { startId ->
        androidx.activity.compose.BackHandler { viewerStartId = null }
        Surface(modifier = Modifier.fillMaxSize()) {
            PhotoLogPager(
                logs = logs,
                startId = startId,
                onDismiss = { viewerStartId = null },
                onEdit = { log ->
                    viewerStartId = null
                    editorTarget = EditorTarget.Edit(log)
                },
                onDelete = { log ->
                    vm.delete(log)
                    viewerStartId = null
                },
                onToggleFavorite = { vm.toggleFavorite(it) },
                onAttachPhoto = { log, uri ->
                    vm.savePhotoFromUri(uri) { saved, _ ->
                        if (saved != null) vm.attachPhoto(log, saved)
                    }
                },
                onRemovePhoto = { vm.removePhoto(it) }
            )
        }
    }

    editorTarget?.let { target ->
        androidx.activity.compose.BackHandler { editorTarget = null }
        Surface(modifier = Modifier.fillMaxSize()) {
            PhotoLogEditScreen(
                target = target,
                vm = vm,
                onDismiss = { editorTarget = null }
            )
        }
    }

    if (showSettings) {
        androidx.activity.compose.BackHandler { showSettings = false }
        Surface(modifier = Modifier.fillMaxSize()) {
            SettingsScreen(onDismiss = { showSettings = false })
        }
    }
}

@Composable
private fun TutorialArrow() {
    val infinite = rememberInfiniteTransition(label = "tutorialBounce")
    val bounce by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceOffset"
    )
    val accent = Color(0xFFFF9800)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 48dp wide so the arrow lines up with the navigationIcon's IconButton above.
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .size(16.dp)
                    .offset(y = bounce.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(R.string.photolog_tutorial_tap_here),
            color = accent,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

sealed class EditorTarget {
    data object New : EditorTarget()
    data class Edit(val log: PhotoLog) : EditorTarget()
}

@Composable
private fun PhotoLogGrid(
    logs: List<PhotoLog>,
    onTap: (PhotoLog) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = remember(logs) { groupByMonth(logs) }
    val collageGroups = remember(groups) {
        var offset = 0
        groups.map { g ->
            val rows = buildCollageRows(g.logs, indexOffset = offset)
            offset += g.logs.size
            CollageGroup(g.key, g.label, rows)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(COLLAGE_SPACING)
    ) {
        collageGroups.forEach { group ->
            item(key = "header_${group.key}") {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            group.rows.forEach { row ->
                item(key = row.rowKey) {
                    CollageRowView(row = row, onTap = onTap)
                }
            }
        }
    }
}

@Composable
private fun CollageRowView(row: CollageRow, onTap: (PhotoLog) -> Unit) {
    when (row) {
        is CollageRow.Full -> {
            GridThumb(
                log = row.log,
                onTap = { onTap(row.log) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(row.height)
            )
        }
        is CollageRow.EqualPair -> {
            Row(horizontalArrangement = Arrangement.spacedBy(COLLAGE_SPACING)) {
                GridThumb(
                    log = row.a,
                    onTap = { onTap(row.a) },
                    modifier = Modifier
                        .weight(1f)
                        .height(row.height)
                )
                GridThumb(
                    log = row.b,
                    onTap = { onTap(row.b) },
                    modifier = Modifier
                        .weight(1f)
                        .height(row.height)
                )
            }
        }
        is CollageRow.AsymmetricPair -> {
            Row(horizontalArrangement = Arrangement.spacedBy(COLLAGE_SPACING)) {
                GridThumb(
                    log = row.a,
                    onTap = { onTap(row.a) },
                    modifier = Modifier
                        .weight(row.leftWeight)
                        .height(row.height)
                )
                GridThumb(
                    log = row.b,
                    onTap = { onTap(row.b) },
                    modifier = Modifier
                        .weight(1f - row.leftWeight)
                        .height(row.height)
                )
            }
        }
    }
}

@Composable
private fun GridThumb(
    log: PhotoLog,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (log.isWorkoutOnly) {
        WorkoutGridThumb(log = log, onTap = onTap, modifier = modifier)
        return
    }

    val context = LocalContext.current
    val displayName = log.filteredPhotoPath ?: log.photoPath
    val photoFile = remember(displayName) { PhotoLogStorage.fileFor(context, displayName) }
    val shortDate = remember(log.date) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(log.date))
    }

    Box(
        modifier = modifier
            .background(Color.Gray.copy(alpha = 0.25f))
            .clickable(onClick = onTap)
    ) {
        AsyncImage(
            model = photoFile,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom gradient
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
        )

        if (log.isFavorite) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFE91E63),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(14.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                shortDate,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            log.kcal?.let { k ->
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "$k",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 1.8: workout-only grid card. Mirrors iOS layout — light/off-white surface,
 * date top-left, large kcal right-of-center, sub-stats bottom-left.
 */
@Composable
private fun WorkoutGridThumb(
    log: PhotoLog,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortDate = remember(log.date) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(log.date))
    }
    val subText = buildString {
        log.durationMin?.let { append("$it min") }
        log.avgBPM?.let {
            if (isNotEmpty()) append(" · ")
            append("$it bpm")
        }
    }
    Box(
        modifier = modifier
            // Unify with the Class editor input cards (surfaceContainerHigh) —
            // also makes the placeholder adapt to the theme instead of a fixed grey.
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (log.isFavorite) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFE91E63),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
            )
        }
        Text(
            shortDate,
            color = Color(0xFF1A1A1A),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopStart)
        )
        val kcal = log.kcal
        if (kcal != null && kcal > 0) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            ) {
                Text(
                    "$kcal",
                    color = Color(0xFF111111),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 44.sp
                )
                Text(
                    "kcal",
                    color = Color(0xFF999999),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                )
            }
        }
        if (subText.isNotEmpty()) {
            Text(
                subText,
                color = Color(0xFF8A8A8A),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            stringResource(R.string.photolog_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.photolog_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

// MARK: - Collage layout (mirrors iOS PhotoLogView)
//
// Pattern-based justified-row collage so a list of photos doesn't read as a
// rigid grid: each row is full-width / equal pair / asymmetric pair, with a
// per-row height chosen from a small palette (tall / short / base). The
// pattern is deterministic per (month, index) so it stays stable across
// recompositions and only shifts when actual data changes.

private val COLLAGE_SPACING = 2.dp
private val COLLAGE_BASE_HEIGHT = 200.dp

private sealed class CollageRow {
    abstract val rowKey: String

    data class Full(val log: PhotoLog, val height: Dp) : CollageRow() {
        override val rowKey get() = "f_${log.id}"
    }

    data class EqualPair(val a: PhotoLog, val b: PhotoLog, val height: Dp) : CollageRow() {
        override val rowKey get() = "e_${a.id}"
    }

    data class AsymmetricPair(
        val a: PhotoLog,
        val b: PhotoLog,
        val leftWeight: Float,
        val height: Dp
    ) : CollageRow() {
        override val rowKey get() = "a_${a.id}"
    }
}

private data class CollageGroup(
    val key: String,
    val label: String,
    val rows: List<CollageRow>
)

/** Cheap deterministic hash so the same index yields the same pattern. */
private fun stableHash(x: Int): Int {
    var v = x * -1640531535  // 2654435761 in two's-complement Int32
    v = v xor (v ushr 13)
    return v
}

/**
 * Mirrors iOS `buildCollageRows` — bucket 0/1 → full row, 2..7 → asymmetric
 * pair (left weight 0.65 or 0.38), else equal pair. heightBucket adds a
 * tall / short / base variant per row for vertical rhythm.
 */
private fun buildCollageRows(logs: List<PhotoLog>, indexOffset: Int): List<CollageRow> {
    val rows = mutableListOf<CollageRow>()
    var i = 0
    val base = COLLAGE_BASE_HEIGHT
    while (i < logs.size) {
        val seed = indexOffset + i
        val bucket = abs(stableHash(seed)) % 20
        val heightBucket = abs(stableHash(seed + 17)) % 10
        val height: Dp = when (heightBucket) {
            0, 1 -> base * 1.4f
            2 -> base * 0.7f
            else -> base
        }
        val remaining = logs.size - i
        when {
            bucket == 0 || bucket == 1 || remaining == 1 -> {
                // Workout-only cards have no photo content to fill the space,
                // so a tall Full row reads as a giant empty rectangle. Use a
                // compact height regardless of the heightBucket pattern.
                val fullHeight = when {
                    logs[i].isWorkoutOnly -> base * 0.9f
                    heightBucket <= 1 -> base * 1.6f
                    else -> base
                }
                rows.add(CollageRow.Full(logs[i], fullHeight))
                i += 1
            }
            bucket in 2..7 -> {
                val leftWeight = if (bucket % 2 == 0) 0.65f else 0.38f
                rows.add(CollageRow.AsymmetricPair(logs[i], logs[i + 1], leftWeight, height))
                i += 2
            }
            else -> {
                rows.add(CollageRow.EqualPair(logs[i], logs[i + 1], height))
                i += 2
            }
        }
    }
    return rows
}

private data class MonthGroup(
    val key: String,
    val label: String,
    val logs: List<PhotoLog>
)

private fun groupByMonth(logs: List<PhotoLog>): List<MonthGroup> {
    val keyFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val labelFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
    val out = mutableListOf<MonthGroup>()
    for (log in logs) {
        val date = Date(log.date)
        val key = keyFmt.format(date)
        if (out.lastOrNull()?.key == key) {
            out[out.lastIndex] = out.last().copy(logs = out.last().logs + log)
        } else {
            out.add(MonthGroup(key = key, label = labelFmt.format(date), logs = listOf(log)))
        }
    }
    return out
}
