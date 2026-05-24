package com.hyorita.balletlog.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.ui.common.LogCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Class log list — iOS `ClassView` parity.
 *
 * Pinned header (+ button + "Class Log" title) over a monthly accordion.
 * Each month folds open by default; tap header to collapse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassScreen(vm: HomeViewModel = viewModel()) {
    val logs by vm.logs.collectAsState()
    val sorted = remember(logs) { logs.sortedByDescending { it.date } }
    val groups = remember(sorted) { groupByMonth(sorted) }

    var showEditor by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<ClassLog?>(null) }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.class_log),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { selectedLog = null; showEditor = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_class))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (sorted.isEmpty()) {
            EmptyClassState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                groups.forEach { group ->
                    val isCollapsed = collapsed[group.key] == true
                    item(key = "h_${group.key}") {
                        MonthHeader(
                            label = group.label,
                            count = group.logs.size,
                            collapsed = isCollapsed,
                            onToggle = { collapsed[group.key] = !isCollapsed }
                        )
                    }
                    item(key = "rows_${group.key}") {
                        AnimatedVisibility(
                            visible = !isCollapsed,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                group.logs.forEach { log ->
                                    LogCard(
                                        log = log,
                                        onTap = {
                                            selectedLog = log
                                            showDetail = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail / Editor as inline full-screen overlays. The root NavBar is
    // hidden while either is active so the overlay covers the full screen
    // and IME padding doesn't double-count the NavBar inset.
    val bottomBarVisible = com.hyorita.balletlog.LocalBottomBarVisible.current
    androidx.compose.runtime.DisposableEffect(showDetail, showEditor) {
        if (showDetail || showEditor) bottomBarVisible.value = false
        onDispose { bottomBarVisible.value = true }
    }

    if (showDetail) {
        val log = selectedLog ?: return
        val liveLog = logs.find { it.id == log.id } ?: log
        androidx.activity.compose.BackHandler { showDetail = false }
        Surface(modifier = Modifier.fillMaxSize()) {
            key(liveLog.workoutJson) {
                DetailScreen(
                    log = liveLog,
                    onDismiss = { showDetail = false },
                    onEdit = { showEditor = true },
                    onDelete = { vm.deleteLog(liveLog); showDetail = false },
                    onToggleFavorite = { vm.toggleFavorite(liveLog) },
                    onFetchWorkout = { vm.fetchAndSaveWorkout(liveLog) },
                    onView = { vm.incrementViewCount(liveLog.id) }
                )
            }
        }
    }

    if (showEditor) {
        val liveLog = selectedLog?.let { sel -> logs.find { it.id == sel.id } } ?: selectedLog
        androidx.activity.compose.BackHandler {
            showEditor = false
            if (selectedLog == null) showDetail = false
        }
        Surface(modifier = Modifier.fillMaxSize()) {
            EditorScreen(
                existingLog = liveLog,
                onDismiss = { saved ->
                    showEditor = false
                    if (selectedLog == null && !saved) showDetail = false
                },
                vm = vm
            )
        }
    }
}

@Composable
private fun MonthHeader(
    label: String,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) 0f else 90f,
        animationSpec = tween(220),
        label = "chevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .size(14.dp)
                .rotate(rotation)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun EmptyClassState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 60.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🩰", style = MaterialTheme.typography.displayMedium)
        Text(
            stringResource(R.string.no_classes_logged_yet),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.tap_record_class),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private data class ClassMonthGroup(
    val key: String,
    val label: String,
    val logs: List<ClassLog>
)

private fun groupByMonth(logs: List<ClassLog>): List<ClassMonthGroup> {
    val keyFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val labelFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
    val out = mutableListOf<ClassMonthGroup>()
    for (log in logs) {
        val date = Date(log.date)
        val key = keyFmt.format(date)
        if (out.lastOrNull()?.key == key) {
            out[out.lastIndex] = out.last().copy(logs = out.last().logs + log)
        } else {
            out.add(ClassMonthGroup(key, labelFmt.format(date), listOf(log)))
        }
    }
    return out
}
