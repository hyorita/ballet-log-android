package com.hyorita.balletlog.ui.stats

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyorita.balletlog.LocalBottomBarVisible
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.ui.common.shareStatsImage
import com.hyorita.balletlog.ui.common.statsWatermark
import com.hyorita.balletlog.ui.home.DetailScreen
import com.hyorita.balletlog.ui.home.EditorScreen
import com.hyorita.balletlog.ui.home.HomeViewModel
import com.hyorita.balletlog.ui.photolog.EditorTarget
import com.hyorita.balletlog.ui.photolog.PhotoLogEditScreen
import com.hyorita.balletlog.ui.photolog.PhotoLogPager
import com.hyorita.balletlog.ui.photolog.PhotoLogViewModel
import com.hyorita.balletlog.ui.theme.PinkLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * 1.16 Stats tab — iOS StatsView parity. Streak hero card (always "as of
 * now"), then period-scoped 2×2 metrics, one trend chart and By studio.
 * The cards are shared with the share image so the two can't drift apart.
 */

// Trend bar pinks, straight from iOS. The current bar is a step darker.
private val BarPast = PinkLight
private val BarCurrent = Color(0xFFF4B4BA)

private val cardShape = RoundedCornerShape(16.dp)

@Composable
fun StatsScreen(
    vm: StatsViewModel = viewModel(),
    homeVm: HomeViewModel = viewModel(),
    photoVm: PhotoLogViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val logs by homeVm.logs.collectAsState()
    val photoLogs by photoVm.photoLogs.collectAsState()
    val context = LocalContext.current

    var detailLog by remember { mutableStateOf<ClassLog?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var viewerStartId by remember { mutableStateOf<String?>(null) }
    var photoEditorTarget by remember { mutableStateOf<EditorTarget?>(null) }
    var sharing by remember { mutableStateOf(false) }

    val onHardestTap: (() -> Unit)? = state.hardest?.let { h ->
        when {
            h.classLog != null -> { { detailLog = h.classLog } }
            h.photoLog != null -> { { viewerStartId = h.photoLog.id } }
            else -> null
        }
    }

    // M3's default Text style carries a 24sp line height whatever the font
    // size, which spreads these small iOS-sized labels apart (and pushed the
    // tallest trend bar's label out of its column). Let the font set it.
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified)
    ) {
    Box(Modifier.fillMaxSize()) {
        // Share render sits underneath the opaque screen: composed and drawn
        // only while a share is in flight, never visible.
        if (sharing) {
            val title = "Ballet Log · ${state.periodLabel}"
            ShareCapture(state = state, watermark = statsWatermark(context)) { bitmap ->
                sharing = false
                shareStatsImage(context, bitmap, title)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            StatsHeader(
                sharing = sharing,
                onAdd = { photoEditorTarget = EditorTarget.New },
                onShare = { sharing = true }
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PeriodSegments(state.period, vm::setPeriod)
                        PeriodNavigator(
                            label = state.periodLabel,
                            canGoForward = state.canGoForward,
                            onPrevious = vm::previousPeriod,
                            onNext = vm::nextPeriod
                        )
                    }
                }
                item { StreakCard(state) }
                item { MetricsCard(state, onHardestTap) }
                item { TrendCard(state) }
                if (state.studios.isNotEmpty()) item { StudioCard(state.studios) }
                if (!state.hasClassLogs) item { StatsEmptyState() }
            }
        }
    }
    }

    // Overlays — same inline full-screen pattern as the other tabs, NavBar
    // hidden while any is up.
    val bottomBarVisible = LocalBottomBarVisible.current
    val anyModalActive = detailLog != null || showEditor ||
        viewerStartId != null || photoEditorTarget != null
    DisposableEffect(anyModalActive) {
        if (anyModalActive) bottomBarVisible.value = false
        onDispose { bottomBarVisible.value = true }
    }

    detailLog?.let { log ->
        val liveLog = logs.find { it.id == log.id } ?: log
        BackHandler { detailLog = null }
        Surface(modifier = Modifier.fillMaxSize()) {
            key(liveLog.workoutJson) {
                DetailScreen(
                    log = liveLog,
                    onDismiss = { detailLog = null },
                    onEdit = { showEditor = true },
                    onDelete = { homeVm.deleteLog(liveLog); detailLog = null },
                    onToggleFavorite = { homeVm.toggleFavorite(liveLog) },
                    onFetchWorkout = { homeVm.fetchAndSaveWorkout(liveLog) },
                    onView = { homeVm.incrementViewCount(liveLog.id) }
                )
            }
        }
    }

    if (showEditor) {
        val liveLog = detailLog?.let { sel -> logs.find { it.id == sel.id } } ?: detailLog
        BackHandler { showEditor = false }
        Surface(modifier = Modifier.fillMaxSize()) {
            EditorScreen(existingLog = liveLog, onDismiss = { showEditor = false }, vm = homeVm)
        }
    }

    viewerStartId?.let { startId ->
        BackHandler { viewerStartId = null }
        Surface(modifier = Modifier.fillMaxSize()) {
            PhotoLogPager(
                logs = photoLogs,
                startId = startId,
                onDismiss = { viewerStartId = null },
                onEdit = { log ->
                    viewerStartId = null
                    photoEditorTarget = EditorTarget.Edit(log)
                },
                onDelete = { log ->
                    photoVm.delete(log)
                    viewerStartId = null
                },
                onToggleFavorite = { photoVm.toggleFavorite(it) },
                onAttachPhoto = { log, uri ->
                    photoVm.savePhotoFromUri(uri) { saved, _ ->
                        if (saved != null) photoVm.attachPhoto(log, saved)
                    }
                },
                onRemovePhoto = { photoVm.removePhoto(it) }
            )
        }
    }

    photoEditorTarget?.let { target ->
        BackHandler { photoEditorTarget = null }
        Surface(modifier = Modifier.fillMaxSize()) {
            PhotoLogEditScreen(target = target, vm = photoVm, onDismiss = { photoEditorTarget = null })
        }
    }
}

// region Header

@Composable
private fun StatsHeader(sharing: Boolean, onAdd: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Opens the photo log editor, not the class editor — same as iOS.
        IconButton(onClick = onAdd) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.photolog_new),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            stringResource(R.string.stats_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(34.dp)
                .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = !sharing, onClick = onShare),
            contentAlignment = Alignment.Center
        ) {
            if (sharing) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Capsule segmented control — the iOS look, not M3's outlined buttons. */
@Composable
private fun PeriodSegments(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(2.dp)
    ) {
        StatsPeriod.entries.forEach { p ->
            val isSelected = p == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isSelected) Modifier
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                        else Modifier.clip(CircleShape)
                    )
                    .clickable { onSelect(p) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(
                        when (p) {
                            StatsPeriod.WEEK -> R.string.stats_period_week
                            StatsPeriod.MONTH -> R.string.stats_period_month
                            StatsPeriod.YEAR -> R.string.stats_period_year
                        }
                    ),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun PeriodNavigator(
    label: String,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
        }
        Text(
            label,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
        }
    }
}

// endregion

// region Cards (shared by the screen and the share image)

@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(padding),
        content = content
    )
}

@Composable
private fun secondaryColor() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun StreakCard(state: StatsUiState) {
    val locale = Locale.getDefault()
    val streak = state.streak
    StatsCard {
        Row(verticalAlignment = Alignment.Top) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.stats_streak_current), fontSize = 15.sp, color = secondaryColor())
                if (streak.isNewRecord) NewRecordPill()
            }
            Spacer(Modifier.weight(1f).widthIn(min = 12.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                StreakStatLine(stringResource(R.string.stats_streak_longest), "${streak.longest}")
                StreakStatLine(stringResource(R.string.stats_streak_total), formatKcal(state.streakKcal, locale), "kcal")
            }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Text(
                "${streak.current}",
                modifier = Modifier.alignByBaseline(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-1.1).sp,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.stats_streak_days),
                modifier = Modifier.alignByBaseline(),
                fontSize = 15.sp,
                color = secondaryColor()
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(streakHintText(streak), fontSize = 13.sp, color = secondaryColor())
    }
}

@Composable
private fun streakHintText(streak: Streak): String {
    val locale = Locale.getDefault()
    return when (val hint = streakHint(streak)) {
        StreakHint.StartNew -> stringResource(R.string.stats_streak_hint_start)
        is StreakHint.RecordSince ->
            stringResource(R.string.stats_streak_hint_record, skeletonDate("MMMd", hint.startDay, locale))
        StreakHint.OneMoreTies -> stringResource(R.string.stats_streak_hint_one_more)
        is StreakHint.LastClass ->
            stringResource(R.string.stats_streak_hint_last, skeletonDate("Md", hint.day, locale))
    }
}

private fun skeletonDate(skeleton: String, millis: Long, locale: Locale): String =
    SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(Date(millis))

/** Label and unit quiet, the number itself full-strength. */
@Composable
private fun StreakStatLine(label: String, number: String, suffix: String? = null) {
    val secondary = secondaryColor()
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = secondary)) { append("$label ") }
            append(number)
            if (suffix != null) withStyle(SpanStyle(color = secondary)) { append(" $suffix") }
        },
        fontSize = 15.sp
    )
}

@Composable
private fun NewRecordPill() {
    Text(
        stringResource(R.string.stats_streak_new_record),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PinkLight)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        // Always dark: the pill is light pink in both themes.
        color = Color.Black
    )
}

/** One card, 2×2, hairlines between — not four separate icon cards. */
@Composable
private fun MetricsCard(state: StatsUiState, onHardestTap: (() -> Unit)?) {
    val locale = Locale.getDefault()
    val hardest = state.hardest
    val hardestLabel = stringResource(R.string.stats_hardest).let { label ->
        hardest?.let { "$label · ${skeletonDate("Md", it.date, locale)}" } ?: label
    }
    val hairline = MaterialTheme.colorScheme.outlineVariant
    StatsCard(padding = PaddingValues(0.dp)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            MetricCell(
                MetricValue("${state.totalClasses}", null, false),
                stringResource(R.string.stats_classes),
                Modifier.weight(1f)
            )
            Box(Modifier.width(1.dp).fillMaxHeight().background(hairline))
            MetricCell(
                workoutTimeValue(state.totalMinutes),
                stringResource(R.string.stats_workout_time),
                Modifier.weight(1f)
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(hairline))
        Row(Modifier.height(IntrinsicSize.Min)) {
            MetricCell(
                kcalValue(state.totalCalories, locale),
                stringResource(R.string.stats_active_energy),
                Modifier.weight(1f)
            )
            Box(Modifier.width(1.dp).fillMaxHeight().background(hairline))
            MetricCell(
                hardest?.let { kcalValue(it.activeCalories, locale) } ?: EMPTY_METRIC,
                hardestLabel,
                Modifier
                    .weight(1f)
                    .then(if (onHardestTap != null) Modifier.clickable(onClick = onHardestTap) else Modifier)
            )
        }
    }
}

@Composable
private fun MetricCell(value: MetricValue, label: String, modifier: Modifier = Modifier) {
    // Zero is a quiet "—", not a bold "0m" — an empty period isn't a data point.
    val quiet = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row {
            Text(
                value.big,
                modifier = Modifier.alignByBaseline(),
                fontSize = 28.sp,
                color = if (value.isEmpty) quiet else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            if (value.unit != null) {
                Spacer(Modifier.width(3.dp))
                Text(
                    value.unit,
                    modifier = Modifier.alignByBaseline(),
                    fontSize = 15.sp,
                    color = if (value.isEmpty) quiet else secondaryColor(),
                    maxLines = 1
                )
            }
        }
        Text(label, fontSize = 15.sp, color = secondaryColor(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TrendCard(state: StatsUiState) {
    val locale = Locale.getDefault()
    val bars = state.bars
    val elapsed = state.elapsed
    val maxValue = maxOf(bars.take(elapsed.coerceAtLeast(1)).maxOfOrNull { it.value } ?: 0.0, 1.0)
    val avg = chartAverage(bars, elapsed)
    val title = stringResource(
        when (state.period) {
            StatsPeriod.WEEK -> R.string.stats_chart_week
            StatsPeriod.MONTH -> R.string.stats_chart_month
            StatsPeriod.YEAR -> R.string.stats_chart_year
        }
    )
    val avgLabel = when (state.period) {
        StatsPeriod.WEEK -> stringResource(R.string.stats_avg_week, Math.round(avg).toInt())
        StatsPeriod.MONTH -> stringResource(R.string.stats_avg_month, String.format(locale, "%.1f", avg))
        StatsPeriod.YEAR -> stringResource(R.string.stats_avg_year, String.format(locale, "%.1f", avg))
    }
    val gap = when (state.period) {
        StatsPeriod.WEEK -> 10.dp
        StatsPeriod.MONTH -> 14.dp
        StatsPeriod.YEAR -> 5.dp
    }
    val emptyBar = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    StatsCard {
        Row {
            Text(title, modifier = Modifier.alignByBaseline(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(avgLabel, modifier = Modifier.alignByBaseline(), fontSize = 13.sp, color = secondaryColor())
        }
        Spacer(Modifier.height(10.dp))
        Row(
            // A floor, not a fixed height — a larger system font must grow the
            // chart rather than overflow the tallest bar's column.
            modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { index, bar ->
                val isFuture = index >= elapsed
                val isCurrent = index == state.currentIndex
                // A past zero still gets its "0"; only bars yet to come are blank.
                val hasValue = !isFuture && bar.value > 0
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (isFuture) " " else "${Math.round(bar.value)}",
                        fontSize = 12.sp,
                        color = secondaryColor(),
                        maxLines = 1
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(if (hasValue) maxOf((bar.value / maxValue * 76).dp, 6.dp) else 6.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                when {
                                    !hasValue -> emptyBar
                                    isCurrent -> BarCurrent
                                    else -> BarPast
                                }
                            )
                    )
                    Text(
                        bar.label,
                        fontSize = 12.sp,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.onSurface else secondaryColor(),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Comparison data, so full-strength onSurface bars rather than the pink —
 * the app barely uses the accent, and onSurface stays visible in dark mode
 * where a literal black would vanish.
 */
@Composable
private fun StudioCard(entries: List<Pair<String, Int>>) {
    val maxCount = entries.first().second.coerceAtLeast(1)
    val ink = MaterialTheme.colorScheme.onSurface
    StatsCard {
        Text(stringResource(R.string.stats_by_studio), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            entries.forEach { (studio, count) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(
                            studio,
                            modifier = Modifier.weight(1f),
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$count",
                            fontSize = 17.sp,
                            color = secondaryColor(),
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ink.copy(alpha = 0.08f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(count.toFloat() / maxCount)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(ink)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🩰", fontSize = 44.sp)
        Text(stringResource(R.string.stats_empty_title), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(R.string.stats_empty_subtitle),
            fontSize = 15.sp,
            color = secondaryColor(),
            textAlign = TextAlign.Center
        )
    }
}

// endregion

// region Share image

/**
 * The share image is the same cards rendered at a fixed 390dp width (as
 * iOS's ShareableStatsView), recorded into a graphics layer and handed back
 * as a bitmap after the frame it first draws in.
 */
@Composable
private fun ShareCapture(
    state: StatsUiState,
    watermark: String,
    onCaptured: (android.graphics.Bitmap) -> Unit
) {
    val layer = rememberGraphicsLayer()
    Box(
        Modifier
            .wrapContentSize(align = Alignment.TopStart, unbounded = true)
            .requiredWidth(390.dp)
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🩰 Ballet Log", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(state.periodLabel, fontSize = 15.sp, color = secondaryColor())
            }
            StreakCard(state)
            MetricsCard(state, onHardestTap = null)
            TrendCard(state)
            if (state.studios.isNotEmpty()) StudioCard(state.studios)
            Text(
                watermark,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = secondaryColor()
            )
        }
    }
    LaunchedEffect(Unit) {
        // Two frames: one to compose/measure, one to be sure the layer recorded.
        withFrameNanos { }
        withFrameNanos { }
        onCaptured(layer.toImageBitmap().asAndroidBitmap())
    }
}

// endregion
