package com.hyorita.balletlog.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.PhotoManager
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.Note
import com.hyorita.balletlog.ui.home.DetailScreen
import com.hyorita.balletlog.ui.home.EditorScreen
import com.hyorita.balletlog.ui.home.HomeViewModel
import com.hyorita.balletlog.ui.notes.NoteDetailScreen
import com.hyorita.balletlog.ui.notes.NoteEditorScreen
import com.hyorita.balletlog.ui.notes.NotesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vm: HomeViewModel = viewModel(),
    notesVm: NotesViewModel = viewModel()
) {
    val logs by vm.logs.collectAsState()
    val notes by notesVm.notes.collectAsState()

    var currentYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedDayKey by remember { mutableStateOf<String?>(null) }

    var showDetail by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<ClassLog?>(null) }

    var showNoteDetail by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }

    val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    val listDateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

    val today = Calendar.getInstance()

    val logsByDay = remember(logs, currentYear, currentMonth) {
        logs.filter { log ->
            val cal = Calendar.getInstance().also { it.timeInMillis = log.date }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }.groupBy { dayKeyFormat.format(Date(it.date)) }
    }

    val notesByDay = remember(notes, currentYear, currentMonth) {
        notes.filter { note ->
            val cal = Calendar.getInstance().also { it.timeInMillis = note.createdAt }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }.groupBy { dayKeyFormat.format(Date(it.createdAt)) }
    }

    val monthLogs = remember(logs, currentYear, currentMonth) {
        logs.filter { log ->
            val cal = Calendar.getInstance().also { it.timeInMillis = log.date }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }.sortedByDescending { it.date }
    }

    val monthNotes = remember(notes, currentYear, currentMonth) {
        notes.filter { note ->
            val cal = Calendar.getInstance().also { it.timeInMillis = note.createdAt }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }.sortedByDescending { it.createdAt }
    }

    val displayLogs = if (selectedDayKey != null)
        logs.filter { dayKeyFormat.format(Date(it.date)) == selectedDayKey }.sortedByDescending { it.date }
    else monthLogs

    val displayNotes = if (selectedDayKey != null)
        notes.filter { dayKeyFormat.format(Date(it.createdAt)) == selectedDayKey }.sortedByDescending { it.createdAt }
    else monthNotes

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 헤더
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = dateFormat.format(Date()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.nav_history),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 캘린더 카드
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 월 네비게이션
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (currentMonth == 0) { currentMonth = 11; currentYear-- }
                                else currentMonth--
                                selectedDayKey = null
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                            }
                            Text(
                                text = monthFormat.format(
                                    Calendar.getInstance().also { it.set(currentYear, currentMonth, 1) }.time
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = {
                                if (currentMonth == 11) { currentMonth = 0; currentYear++ }
                                else currentMonth++
                                selectedDayKey = null
                            }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // 요일 헤더 Sun~Sat
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // 날짜 그리드
                        val cal = Calendar.getInstance().also {
                            it.set(currentYear, currentMonth, 1)
                            it.minimalDaysInFirstWeek = 1
                            it.firstDayOfWeek = Calendar.SUNDAY
                        }
                        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val rows = (firstDayOfWeek + daysInMonth + 6) / 7

                        for (row in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0 until 7) {
                                    val day = row * 7 + col - firstDayOfWeek + 1
                                    if (day < 1 || day > daysInMonth) {
                                        Box(modifier = Modifier.weight(1f).height(40.dp))
                                    } else {
                                        val key = "%04d-%02d-%02d".format(currentYear, currentMonth + 1, day)
                                        val hasLog = logsByDay.containsKey(key)
                                        val hasNote = notesByDay.containsKey(key)
                                        val isSelected = selectedDayKey == key
                                        val isToday = currentYear == today.get(Calendar.YEAR) &&
                                            currentMonth == today.get(Calendar.MONTH) &&
                                            day == today.get(Calendar.DAY_OF_MONTH)
                                        val isFuture = run {
                                            val d = Calendar.getInstance().also { it.set(currentYear, currentMonth, day) }
                                            d.after(today) && !isToday
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .clip(CircleShape)
                                                .then(if (isSelected) Modifier.background(Color.Black) else Modifier)
                                                .clickable(enabled = !isFuture) {
                                                    selectedDayKey = if (selectedDayKey == key) null else key
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = day.toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                // 기록 있는 날 dot
                                                if ((hasLog || hasNote) && !isSelected && !isFuture) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        if (hasLog) Box(
                                                            modifier = Modifier.size(4.dp).clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primary)
                                                        )
                                                        if (hasNote) Box(
                                                            modifier = Modifier.size(4.dp).clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.tertiary)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                    }
                }
            }

            // 날짜 선택 && 기록 없음 → iOS 스타일 빈 상태
            if (selectedDayKey != null && displayLogs.isEmpty() && displayNotes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.no_classes_on_this_day),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { selectedLog = null; showEditor = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White
                            )
                        ) {
                            Text("+ Record Class", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        }
                    }
                }
            } else {

            // Classes 섹션
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_ballet_shoe),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.classes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "${displayLogs.size} class${if (displayLogs.size != 1) "es" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (displayLogs.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_classes_logged_yet),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(displayLogs, key = { it.id }) { log ->
                    HistoryLogCard(
                        log = log,
                        dateFormat = listDateFormat,
                        onTap = { selectedLog = log; showDetail = true },
                        onFavorite = { vm.toggleFavorite(log) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Notes 섹션
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📝", fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.nav_notes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "${displayNotes.size} note${if (displayNotes.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (displayNotes.isEmpty()) {
                item {
                    Text(
                        text = "No notes recorded",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(displayNotes, key = { "note-${it.id}" }) { note ->
                    HistoryNoteCard(
                        note = note,
                        onTap = { selectedNote = note; showNoteDetail = true }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            } // end else (has records)
        }
    }

    // Log Detail
    if (showDetail) {
        val log = selectedLog ?: return
        Dialog(
            onDismissRequest = { showDetail = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                DetailScreen(
                    log = logs.find { it.id == log.id } ?: log,
                    onDismiss = { showDetail = false },
                    onEdit = { showEditor = true },
                    onDelete = { vm.deleteLog(log); showDetail = false },
                    onToggleFavorite = { vm.toggleFavorite(log) }
                )
            }
        }
    }

    // Log Editor
    if (showEditor) {
        Dialog(
            onDismissRequest = { showEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                EditorScreen(existingLog = selectedLog, onDismiss = { _ -> showEditor = false }, vm = vm)
            }
        }
    }

    // Note Detail
    if (showNoteDetail) {
        val note = selectedNote ?: return
        Dialog(
            onDismissRequest = { showNoteDetail = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                NoteDetailScreen(
                    note = notes.find { it.id == note.id } ?: note,
                    onDismiss = { showNoteDetail = false },
                    onEdit = { showNoteEditor = true },
                    onDelete = { notesVm.delete(note); showNoteDetail = false },
                    onTogglePin = { notesVm.togglePin(note) }
                )
            }
        }
    }

    // Note Editor
    if (showNoteEditor) {
        val allTags = notes.flatMap { it.tags }.distinct().sorted()
        Dialog(
            onDismissRequest = { showNoteEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                NoteEditorScreen(
                    existingNote = selectedNote,
                    allTags = allTags,
                    onDismiss = { showNoteEditor = false },
                    vm = notesVm
                )
            }
        }
    }
}

@Composable
fun HistoryLogCard(log: ClassLog, dateFormat: SimpleDateFormat, onTap: () -> Unit, onFavorite: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onTap
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
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
                Text(
                    text = dateFormat.format(Date(log.date)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Barre ${log.barreSteps.size} · Center ${log.centerSteps.size} steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                log.workout?.let { w ->
                    Text(
                        text = "⏱ ${w.durationMinutes}min  🔥 ${w.activeCalories.toInt()}kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = if (log.favorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun HistoryNoteCard(note: Note, onTap: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onTap
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title.ifEmpty { "Untitled" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    if (note.pinned) {
                        Spacer(Modifier.width(6.dp))
                        Text("📌", fontSize = 14.sp)
                    }
                }
                if (note.content.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
