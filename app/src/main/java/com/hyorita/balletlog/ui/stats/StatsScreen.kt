package com.hyorita.balletlog.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.ui.common.shareStatsCard
import com.hyorita.balletlog.ui.theme.PinkDark
import com.hyorita.balletlog.ui.theme.PinkLight
import com.hyorita.balletlog.ui.theme.PinkMid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    vm: StatsViewModel = viewModel(),
    onDismiss: () -> Unit = {},
    onNavigateToLog: (ClassLog) -> Unit = {},
    referenceYearMonth: Pair<Int, Int>? = null
) {
    val period by vm.selectedPeriod.collectAsState()
    val periodLabel by vm.periodLabel.collectAsState()
    val canGoForward by vm.canGoForward.collectAsState()
    val aggregates by vm.aggregates.collectAsState()
    val context = LocalContext.current

    // 1.9: anchor to the month being viewed in History (iOS referenceDate).
    LaunchedEffect(referenceYearMonth) {
        referenceYearMonth?.let { (y, m) -> vm.showMonth(y, m) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            )
        }

        // Title + Share
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Stats",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                val chartTitle = when (period) {
                    StatsPeriod.WEEK -> "Classes — last 7 days"
                    StatsPeriod.MONTH -> "Classes per week"
                    StatsPeriod.YEAR -> "Classes per month"
                }
                val chartData = if (period == StatsPeriod.YEAR) {
                    val months = listOf("J","F","M","A","M","J","J","A","S","O","N","D")
                    aggregates.monthlyClassCounts.mapIndexed { i, count -> months[i] to count }
                } else {
                    aggregates.cubeLabels.zip(aggregates.cubeData).map { (label, count) -> label to count }
                }
                shareStatsCard(
                    context = context,
                    period = period.name,
                    periodLabel = periodLabel,
                    totalClasses = aggregates.totalClasses,
                    totalMinutes = aggregates.totalMinutes.toDouble(),
                    totalCalories = aggregates.totalCalories.toDouble(),
                    hardestCalories = aggregates.hardestCalories,
                    hardestDate = aggregates.hardestDate,
                    cubeData = chartData,
                    isCubeChart = period != StatsPeriod.YEAR,
                    chartTitle = chartTitle,
                    monthlyAvgMinutes = if (period == StatsPeriod.YEAR)
                        aggregates.monthlyAvgMinutes.map { it.toDouble() }
                    else emptyList()
                )
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PeriodSelector(period = period, onPeriodChange = vm::setPeriod)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = vm::previousPeriod) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                    }
                    Text(
                        periodLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    IconButton(
                        onClick = vm::nextPeriod,
                        enabled = canGoForward
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = if (canGoForward) LocalContentColor.current
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            item { MetricsGrid(aggregates) }

            item {
                if (period == StatsPeriod.YEAR) {
                    YearChart(aggregates)
                } else {
                    CubeChart(aggregates)
                }
            }

            if (aggregates.topViewed.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Top Viewed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(aggregates.topViewed, key = { "tv_${it.id}" }) { log ->
                    TopViewedRow(log = log, onTap = { onNavigateToLog(log) })
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    period: StatsPeriod,
    onPeriodChange: (StatsPeriod) -> Unit
) {
    val options = StatsPeriod.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, p ->
            SegmentedButton(
                selected = p == period,
                onClick = { onPeriodChange(p) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        p.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    }
}

@Composable
private fun MetricsGrid(aggregates: StatsAggregates) {
    val hardestSub = aggregates.hardestDate?.let {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it))
    }
    val hardestValue = aggregates.hardestCalories?.let { "$it kcal" } ?: "—"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.FitnessCenter,
                iconTint = Color(0xFFE91E63),
                label = "Classes",
                value = aggregates.totalClasses.toString()
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Schedule,
                iconTint = Color(0xFF2196F3),
                label = "Workout Time",
                value = formatDuration(aggregates.totalMinutes)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalFireDepartment,
                iconTint = Color(0xFFFF6B35),
                label = "kcal",
                value = "%,d".format(aggregates.totalCalories)
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                iconTint = Color(0xFFFFB300),
                label = "Hardest Workout",
                value = hardestValue,
                sub = hardestSub
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    sub: String? = null
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    value,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                if (sub != null) {
                    Text(
                        sub,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDuration(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "0m"
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

@Composable
private fun CubeChart(aggregates: StatsAggregates) {
    val counts = aggregates.cubeData
    val labels = aggregates.cubeLabels
    val maxCount = counts.maxOrNull() ?: 0
    val rowCount = maxOf(3, maxCount).coerceAtMost(5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Classes",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                counts.forEachIndexed { index, count ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.height(16.dp)) {
                            if (count > 0) {
                                Text(
                                    "$count",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (row in 0 until rowCount) {
                                val cubeIndex = rowCount - 1 - row // 0 = bottom
                                val filled = cubeIndex < count
                                val color = if (filled) {
                                    when (cubeIndex) {
                                        0 -> PinkLight
                                        1 -> PinkMid
                                        else -> PinkDark
                                    }
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(color)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            labels.getOrNull(index) ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearChart(aggregates: StatsAggregates) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Avg workout time (min)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            LineChartView(
                values = aggregates.monthlyAvgMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                lineColor = PinkDark
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Classes per month",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            BarChartView(
                values = aggregates.monthlyClassCounts,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                barColor = PinkMid
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D").forEach {
                    Text(
                        it,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LineChartView(
    values: List<Int>,
    modifier: Modifier = Modifier,
    lineColor: Color
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val maxV = (values.maxOrNull()?.takeIf { it > 0 } ?: 1).toFloat()
        // baseline
        drawLine(
            color = gridColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 1f
        )
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        val points = values.mapIndexed { i, v ->
            Offset(i * step, size.height - (v / maxV) * size.height * 0.92f)
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3f
            )
        }
        points.forEach { p ->
            drawCircle(color = lineColor, radius = 4f, center = p)
        }
    }
}

@Composable
private fun BarChartView(
    values: List<Int>,
    modifier: Modifier = Modifier,
    barColor: Color
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val maxV = (values.maxOrNull()?.takeIf { it > 0 } ?: 1).toFloat()
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(1f)
        val corner = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        values.forEachIndexed { i, v ->
            val h = (v / maxV) * size.height
            if (h > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(i * (barWidth + gap), size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = corner
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopViewedRow(log: ClassLog, onTap: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onTap
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                dateFormat.format(Date(log.date)),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "${log.viewCount}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
