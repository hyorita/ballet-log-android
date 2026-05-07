package com.hyorita.balletlog.ui.photolog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.BackupBannerPreferences
import com.hyorita.balletlog.data.PhotoLogStorage
import com.hyorita.balletlog.data.model.PhotoLog
import com.hyorita.balletlog.ui.common.BackupBanner
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
    var bannerVisible by remember { mutableStateOf(BackupBannerPreferences.shouldShow(context)) }

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
                    IconButton(onClick = { editorTarget = EditorTarget.New }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.photolog_new))
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
            if (bannerVisible) {
                BackupBanner(
                    onTapAction = { showSettings = true },
                    onDismiss = {
                        BackupBannerPreferences.setDismissed(context)
                        bannerVisible = false
                    }
                )
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
    viewerStartId?.let { startId ->
        Dialog(
            onDismissRequest = { viewerStartId = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
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
                onToggleFavorite = { vm.toggleFavorite(it) }
            )
        }
    }

    editorTarget?.let { target ->
        Dialog(
            onDismissRequest = { editorTarget = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            PhotoLogEditScreen(
                target = target,
                vm = vm,
                onDismiss = { editorTarget = null }
            )
        }
    }

    if (showSettings) {
        Dialog(
            onDismissRequest = {
                showSettings = false
                // Re-check whether the banner should still show (export may
                // have happened inside Settings, which marks the pref).
                bannerVisible = BackupBannerPreferences.shouldShow(context)
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                SettingsScreen(onDismiss = {
                    showSettings = false
                    bannerVisible = BackupBannerPreferences.shouldShow(context)
                })
            }
        }
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
                val fullHeight = if (heightBucket <= 1) base * 1.6f else base
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
