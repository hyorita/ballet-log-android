package com.hyorita.balletlog.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.PhotoManager
import com.hyorita.balletlog.data.model.ClassLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val logs by vm.logs.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<ClassLog?>(null) }

    val sortedLogs = remember(logs) { logs.sortedByDescending { it.date } }
    val photoLogs = remember(sortedLogs) { sortedLogs.filter { it.photos.isNotEmpty() }.take(10) }
    val favoriteLogs = remember(sortedLogs) { sortedLogs.filter { it.favorite }.take(10) }
    val recentLogs = remember(sortedLogs) { sortedLogs.take(10) }

    val headerDateFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val todayText = remember { headerDateFormat.format(Date()).uppercase() }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. Header
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = todayText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 2. Record Class button
            item {
                Button(
                    onClick = { selectedLog = null; showEditor = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.record_class),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                }
            }

            // 3. Photo carousel
            if (photoLogs.isNotEmpty()) {
                item {
                    PhotoCarouselSection(
                        logs = photoLogs,
                        onTap = { log -> selectedLog = log; showDetail = true }
                    )
                }
            }

            // 4. Favorites
            if (favoriteLogs.isNotEmpty()) {
                item {
                    CardSectionView(
                        title = stringResource(R.string.favorites),
                        icon = Icons.Default.Favorite,
                        iconTint = Color(0xFFE91E63)
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favoriteLogs, key = { "fav_${it.id}" }) { log ->
                                SquareLogCard(
                                    log = log,
                                    onTap = { selectedLog = log; showDetail = true }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Recent
            if (recentLogs.isNotEmpty()) {
                item {
                    CardSectionView(
                        title = stringResource(R.string.recent),
                        icon = Icons.Default.Schedule,
                        iconTint = MaterialTheme.colorScheme.onSurface
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentLogs, key = { "recent_${it.id}" }) { log ->
                                SquareLogCard(
                                    log = log,
                                    onTap = { selectedLog = log; showDetail = true }
                                )
                            }
                        }
                    }
                }
            }

            // Empty state
            if (logs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.no_classes_logged_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.tap_record_class),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Detail
    if (showDetail) {
        val log = selectedLog ?: return
        val liveLog = logs.find { it.id == log.id } ?: log
        Dialog(
            onDismissRequest = { showDetail = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
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
    }

    // Editor
    if (showEditor) {
        val liveLog = selectedLog?.let { sel -> logs.find { it.id == sel.id } } ?: selectedLog
        Dialog(
            onDismissRequest = { showEditor = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoCarouselSection(
    logs: List<ClassLog>,
    onTap: (ClassLog) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val cardWidth = configuration.screenWidthDp.dp * 0.82f
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LazyRow(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(logs, key = { "photo_${it.id}" }) { log ->
            Card(
                modifier = Modifier
                    .width(cardWidth)
                    .aspectRatio(1f / 1.1f),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                onClick = { onTap(log) }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = PhotoManager.getPhotoFile(context, log.photos.first().fileName),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.55f)
                                    ),
                                    startY = 260f
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            dateFormat.format(Date(log.date)),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (log.favorite) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardSectionView(
    title: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquareLogCard(log: ClassLog, onTap: () -> Unit) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Card(
        modifier = Modifier.size(150.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        onClick = onTap
    ) {
        if (log.photos.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = PhotoManager.getPhotoFile(context, log.photos.first().fileName),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.65f)
                                ),
                                startY = 110f
                            )
                        )
                )
                Text(
                    text = dateFormat.format(Date(log.date)),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
                if (log.favorite) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE91E63),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(16.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    dateFormat.format(Date(log.date)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Column {
                    log.workout?.activeCalories?.let { cal ->
                        Text(
                            "$cal kcal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (log.viewCount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "${log.viewCount}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
