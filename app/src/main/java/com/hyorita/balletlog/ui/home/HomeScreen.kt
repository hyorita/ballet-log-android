package com.hyorita.balletlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hyorita.balletlog.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.hyorita.balletlog.data.PhotoManager
import com.hyorita.balletlog.data.model.ClassLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val logs by vm.logs.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<ClassLog?>(null) }

    val today = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val todayLogs = logs.filter { it.date >= today }
    val recentLogs = logs.sortedByDescending { it.date }.take(5)
    val favoriteLogs = logs.filter { it.favorite }.sortedByDescending { it.date }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.home_title), fontWeight = FontWeight.Bold, fontSize = 28.sp) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Record Class 버튼
            item {
                Button(
                    onClick = { selectedLog = null; showEditor = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.record_class), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }
                Spacer(Modifier.height(24.dp))
            }

            // Today 섹션
            item {
                Text(stringResource(R.string.today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
            }
            if (todayLogs.isEmpty()) {
                item {
                    Text(stringResource(R.string.no_classes_logged_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                }
            } else {
                items(todayLogs, key = { it.id }) { log ->
                    LogCard(log = log,
                        onTap = { selectedLog = log; showDetail = true },
                        onFavorite = { vm.toggleFavorite(log) })
                    Spacer(Modifier.height(8.dp))
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            // Recent 섹션
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.recent), fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (recentLogs.isEmpty()) {
                item {
                    Text(stringResource(R.string.no_classes_logged_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                }
            } else {
                items(recentLogs, key = { "r_${it.id}" }) { log ->
                    LogCard(log = log,
                        onTap = { selectedLog = log; showDetail = true },
                        onFavorite = { vm.toggleFavorite(log) })
                    Spacer(Modifier.height(8.dp))
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            // Favorites 섹션
            if (favoriteLogs.isNotEmpty()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Black)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.favorites), fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                items(favoriteLogs, key = { "f_${it.id}" }) { log ->
                    LogCard(log = log,
                        onTap = { selectedLog = log; showDetail = true },
                        onFavorite = { vm.toggleFavorite(log) })
                    Spacer(Modifier.height(8.dp))
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
                        onFetchWorkout = { vm.fetchAndSaveWorkout(liveLog) }
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

@Composable
fun LogCard(log: ClassLog, onTap: () -> Unit, onFavorite: () -> Unit) {
    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onTap
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 썸네일 (사진 있으면 첫 장, 없으면 placeholder)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (log.photos.isNotEmpty()) {
                    AsyncImage(
                        model = PhotoManager.getPhotoFile(context, log.photos.first().fileName),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_ballet_shoe),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 날짜 + 하트
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dateFormat.format(Date(log.date)),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge)
                    if (log.favorite) {
                        Spacer(Modifier.width(6.dp))
                        Text("❤️", fontSize = 14.sp)
                    }
                }

                // Center/Barre steps
                val centerCount = log.centerSteps.size
                val barreCount = log.barreSteps.size
                val stepText = when {
                    centerCount > 0 && barreCount > 0 -> "Barre: $barreCount  Center: $centerCount steps"
                    centerCount > 0 -> "Center: $centerCount steps"
                    barreCount > 0 -> "Barre: $barreCount steps"
                    else -> null
                }
                stepText?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 워크아웃 정보 (있을 때만)
                log.workout?.let { w ->
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("⏱ ${w.durationMinutes}min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("🔥 ${w.activeCalories}kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 화살표
            Text("›", fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
