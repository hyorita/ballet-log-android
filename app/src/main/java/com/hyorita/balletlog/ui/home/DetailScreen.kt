package com.hyorita.balletlog.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.PhotoManager
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.ui.common.WorkoutCard
import com.hyorita.balletlog.ui.common.findActivity
import com.hyorita.balletlog.ui.common.shareLogCard
import com.hyorita.balletlog.util.debugLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    log: ClassLog,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onFetchWorkout: () -> Unit = {},
    onView: () -> Unit = {}
) {
    LaunchedEffect(log.id) {
        onView()
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var tabIndex by remember { mutableIntStateOf(0) }
    var isSharing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val strBallet = stringResource(R.string.ballet_workout)
    val strBarre = stringResource(R.string.barre)
    val strCenter = stringResource(R.string.center)
    val strDuration = stringResource(R.string.duration)
    val strActiveCal = stringResource(R.string.active_cal)
    val strAvgBpm = stringResource(R.string.avg_bpm)
    val strMaxBpm = stringResource(R.string.max_bpm)
    val strNotes = stringResource(R.string.notes_label)
    val strDelete = stringResource(R.string.delete)
    val strCancel = stringResource(R.string.cancel)
    val strDeleteClassTitle = stringResource(R.string.delete_class_title)
    val strDeleteClassMessage = stringResource(R.string.delete_class_message)

    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val currentSteps = if (tabIndex == 0) log.barreSteps else log.centerSteps
    val currentMusic = if (tabIndex == 0) log.barreMusic else log.centerMusic

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (log.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (log.favorite) Color(0xFFE91E63)
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSharing) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            isSharing = true
                            Thread {
                                try {
                                    shareLogCard(context, log, tabIndex)
                                } catch (e: Exception) {
                                    debugLog("ShareLog", "Share failed: ${e.message}", e)
                                } finally {
                                    context.findActivity().runOnUiThread {
                                        isSharing = false
                                    }
                                }
                            }.start()
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 날짜/시간 (항상 최상단)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = dateFormat.format(Date(log.date)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeFormat.format(Date(log.date)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (log.viewCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "${log.viewCount}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 사진 페이저 (있을 때만)
            if (log.photos.isNotEmpty()) {
                item {
                    val pagerState = rememberPagerState(pageCount = { log.photos.size })
                    Box {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) { page ->
                            AsyncImage(
                                model = PhotoManager.getPhotoFile(context, log.photos[page].fileName),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                        }
                        // 페이지 인디케이터
                        if (log.photos.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(log.photos.size) { i ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (i == pagerState.currentPage) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (i == pagerState.currentPage) Color.White
                                                else Color.White.copy(alpha = 0.5f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 사진 없을 때 placeholder 없음

            // 워크아웃 카드 (있을 때만)
            log.workout?.let { workout ->
                item {
                    WorkoutCard(
                        workout = workout,
                        title = strBallet,
                        durationLabel = strDuration,
                        activeCalLabel = strActiveCal,
                        avgBpmLabel = strAvgBpm,
                        maxBpmLabel = strMaxBpm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            // Barre / Center 세그먼트 탭
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    TabRow(
                        selectedTabIndex = tabIndex,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        indicator = {}
                    ) {
                        Tab(
                            selected = tabIndex == 0,
                            onClick = { tabIndex = 0 },
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (tabIndex == 0) MaterialTheme.colorScheme.surface
                                    else Color.Transparent
                                )
                        ) {
                            Text(strBarre,
                                modifier = Modifier.padding(vertical = 10.dp),
                                fontWeight = if (tabIndex == 0) FontWeight.SemiBold else FontWeight.Normal)
                        }
                        Tab(
                            selected = tabIndex == 1,
                            onClick = { tabIndex = 1 },
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (tabIndex == 1) MaterialTheme.colorScheme.surface
                                    else Color.Transparent
                                )
                        ) {
                            Text(strCenter,
                                modifier = Modifier.padding(vertical = 10.dp),
                                fontWeight = if (tabIndex == 1) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }

            // YouTube Music 카드 (있을 때만)
            if (currentMusic.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("♩", fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("▶ ${stringResource(R.string.music)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold)
                                }
                                Text(currentMusic,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1)
                            }
                            Text("↗", fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Steps 목록
            if (currentSteps.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column {
                            currentSteps.forEachIndexed { index, step ->
                                if (index > 0) HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = String.format("%02d", index + 1),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(36.dp)
                                        )
                                        Column {
                                            Text(
                                                text = step.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (step.note.isNotEmpty()) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = step.note,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notes (있을 때만)
            if (log.notes.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(strNotes,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text(log.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strDeleteClassTitle) },
            text = { Text(strDeleteClassMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                    onDismiss()
                }) { Text(strDelete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(strCancel) }
            }
        )
    }
}

@Composable
fun WorkoutStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        Text(label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

