package com.hyorita.balletlog.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.HealthConnectManager
import com.hyorita.balletlog.data.HistoryPreferences
import com.hyorita.balletlog.data.PhotoLogStorage
import com.hyorita.balletlog.data.PhotoManager
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.Note
import com.hyorita.balletlog.data.model.PhotoLog
import com.hyorita.balletlog.ui.common.LogCard
import com.hyorita.balletlog.ui.common.NoteCard
import com.hyorita.balletlog.ui.home.DetailScreen
import com.hyorita.balletlog.ui.home.EditorScreen
import com.hyorita.balletlog.ui.home.HomeViewModel
import com.hyorita.balletlog.ui.notes.NoteDetailScreen
import com.hyorita.balletlog.ui.notes.NoteEditorScreen
import com.hyorita.balletlog.ui.notes.NotesViewModel
import com.hyorita.balletlog.ui.photolog.EditorTarget
import com.hyorita.balletlog.ui.photolog.PhotoLogCard
import com.hyorita.balletlog.ui.photolog.PhotoLogEditScreen
import com.hyorita.balletlog.ui.photolog.PhotoLogPager
import com.hyorita.balletlog.ui.photolog.PhotoLogViewModel
import com.hyorita.balletlog.ui.stats.StatsScreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    vm: HomeViewModel = viewModel(),
    notesVm: NotesViewModel = viewModel(),
    photoLogVm: PhotoLogViewModel = viewModel()
) {
    val logs by vm.logs.collectAsState()
    val notes by notesVm.notes.collectAsState()
    val photoLogs by photoLogVm.photoLogs.collectAsState()
    var selectedPhotoLog by remember { mutableStateOf<PhotoLog?>(null) }
    var photoEditorTarget by remember { mutableStateOf<EditorTarget?>(null) }

    var currentYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedDayKey by remember { mutableStateOf<String?>(null) }

    var showDetail by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<ClassLog?>(null) }

    var showNoteDetail by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }

    var showStats by remember { mutableStateOf(false) }
    val statsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    val photoLogsByDay = remember(photoLogs, currentYear, currentMonth) {
        photoLogs.filter { p ->
            val cal = Calendar.getInstance().also { it.timeInMillis = p.date }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }.groupBy { dayKeyFormat.format(Date(it.date)) }
    }

    val monthPhotoLogs = remember(photoLogs, currentYear, currentMonth) {
        photoLogs.filter { p ->
            val cal = Calendar.getInstance().also { it.timeInMillis = p.date }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }.sortedByDescending { it.date }
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

    val displayPhotoLogs = if (selectedDayKey != null)
        photoLogs.filter { dayKeyFormat.format(Date(it.date)) == selectedDayKey }.sortedByDescending { it.date }
    else monthPhotoLogs

    // 1.9: Health Connect workouts for the viewed month that aren't logged yet.
    // Scan when the month changes; the banner / per-day rows let the user add
    // them. Already-logged identities come from PhotoLog placeholders and from
    // ClassLog workouts (1.9 WorkoutInfo.externalWorkoutId).
    val context = LocalContext.current
    var monthWorkouts by remember { mutableStateOf<List<HealthConnectManager.ScannedWorkout>>(emptyList()) }
    var dismissedMonths by remember { mutableStateOf(setOf<String>()) }
    var bannerHidden by remember { mutableStateOf(HistoryPreferences.isUnloggedBannerHidden(context)) }
    val monthKey = "%04d-%02d".format(currentYear, currentMonth + 1)

    LaunchedEffect(currentYear, currentMonth) {
        val cal = Calendar.getInstance().also { it.clear(); it.set(currentYear, currentMonth, 1) }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        photoLogVm.scanWorkouts(start, end) { monthWorkouts = it }
    }

    val loggedIds = remember(photoLogs, logs) {
        (photoLogs.mapNotNull { it.externalWorkoutId } +
            logs.mapNotNull { it.workout?.externalWorkoutId }).toHashSet()
    }
    val unloggedMonth = remember(monthWorkouts, loggedIds) {
        monthWorkouts.filter { it.externalId !in loggedIds }
    }
    val unloggedDay = if (selectedDayKey != null)
        unloggedMonth.filter { dayKeyFormat.format(Date(it.startTimeMillis)) == selectedDayKey }
    else emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_history),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = { showStats = true }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Stats")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1.9: month-level "unlogged activities" banner — above the calendar
            // to match iOS HistoryView (banner precedes calendarSection).
            if (selectedDayKey == null && !bannerHidden &&
                monthKey !in dismissedMonths && unloggedMonth.isNotEmpty()
            ) {
                item {
                    HistoryUnloggedBanner(
                        count = unloggedMonth.size,
                        onAddAll = { photoLogVm.importWorkouts(unloggedMonth) },
                        onDismiss = { dismissedMonths = dismissedMonths + monthKey },
                        onHide = { HistoryPreferences.hideUnloggedBanner(context); bannerHidden = true }
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

                        // Locale-aware weekday header — first letter of each
                        // short symbol, ordered starting at the locale's first
                        // day of week (Sunday on most en/ko/ja systems).
                        val localeFirstDay = remember { Calendar.getInstance().firstDayOfWeek } // 1=Sun..7=Sat
                        val weekdaySymbols = remember(localeFirstDay) {
                            val sym = java.text.DateFormatSymbols.getInstance().shortWeekdays
                            val order = (localeFirstDay..7).toList() + (1 until localeFirstDay).toList()
                            order.map { sym.getOrNull(it)?.firstOrNull()?.toString().orEmpty() }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            weekdaySymbols.forEach { day ->
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
                            it.firstDayOfWeek = localeFirstDay
                        }
                        val firstDayOfWeek = ((cal.get(Calendar.DAY_OF_WEEK) - localeFirstDay) + 7) % 7
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
                                        val hasPhotoLog = photoLogsByDay.containsKey(key)
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
                                                .then(
                                                    when {
                                                        isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                                                        isToday -> Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                        else -> Modifier
                                                    }
                                                )
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
                                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                                        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                // Activity dots — log (primary), photoLog (pink), note (tertiary)
                                                if ((hasLog || hasNote || hasPhotoLog) && !isSelected && !isFuture) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        if (hasLog) Box(
                                                            modifier = Modifier.size(4.dp).clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primary)
                                                        )
                                                        if (hasPhotoLog) Box(
                                                            modifier = Modifier.size(4.dp).clip(CircleShape)
                                                                .background(Color(0xFFE91E63))
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

            // 1.9: per-day unlogged Health Connect workouts → tap a row to add
            if (selectedDayKey != null && unloggedDay.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.history_unlogged_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(unloggedDay, key = { "unlogged-${it.externalId}" }) { w ->
                    HistoryUnloggedWorkoutRow(
                        workout = w,
                        onAdd = { photoLogVm.importWorkouts(listOf(w)) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // 날짜 선택 && 기록 없음 → iOS 스타일 빈 상태
            if (selectedDayKey != null && displayLogs.isEmpty() && displayNotes.isEmpty() &&
                displayPhotoLogs.isEmpty() && unloggedDay.isEmpty()) {
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

            // Photo Logs 섹션 — first (iOS 1.6.1 reorder)
            if (displayPhotoLogs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.history_photo_logs),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "${displayPhotoLogs.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(displayPhotoLogs, key = { "photo-${it.id}" }) { p ->
                    HistoryPhotoLogBanner(
                        photoLog = p,
                        onTap = { selectedPhotoLog = p }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

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
                    LogCard(
                        log = log,
                        onTap = { selectedLog = log; showDetail = true },
                        modifier = Modifier.padding(horizontal = 16.dp)
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
                    NoteCard(
                        note = note,
                        onTap = { selectedNote = note; showNoteDetail = true },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        untitledLabel = stringResource(R.string.untitled)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            } // end else (has records)
        }
    }

    // Full-screen modals as inline overlays. Hide the root NavBar while any
    // is active to avoid double-counting its inset and so overlays cover the
    // entire screen.
    val bottomBarVisible = com.hyorita.balletlog.LocalBottomBarVisible.current
    val anyModalActive = showDetail || showEditor || showNoteDetail ||
        showNoteEditor || selectedPhotoLog != null || photoEditorTarget != null
    androidx.compose.runtime.DisposableEffect(anyModalActive) {
        if (anyModalActive) bottomBarVisible.value = false
        onDispose { bottomBarVisible.value = true }
    }

    if (showDetail) {
        val log = selectedLog ?: return
        androidx.activity.compose.BackHandler { showDetail = false }
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

    if (showEditor) {
        androidx.activity.compose.BackHandler { showEditor = false }
        Surface(modifier = Modifier.fillMaxSize()) {
            val initialDateMillis = selectedDayKey?.let { key ->
                runCatching { dayKeyFormat.parse(key)?.time }.getOrNull()
            }
            EditorScreen(
                existingLog = selectedLog,
                onDismiss = { _ -> showEditor = false },
                vm = vm,
                initialDate = if (selectedLog == null) initialDateMillis else null
            )
        }
    }

    if (showNoteDetail) {
        val note = selectedNote ?: return
        androidx.activity.compose.BackHandler { showNoteDetail = false }
        Surface(modifier = Modifier.fillMaxSize()) {
            NoteDetailScreen(
                note = notes.find { it.id == note.id } ?: note,
                classLogs = logs,
                onDismiss = { showNoteDetail = false },
                onEdit = { showNoteEditor = true },
                onDelete = { notesVm.delete(note); showNoteDetail = false },
                onTogglePin = { notesVm.togglePin(note) },
                onOpenLog = { log ->
                    showNoteDetail = false
                    selectedLog = log
                    showDetail = true
                }
            )
        }
    }

    // Stats sheet
    if (showStats) {
        ModalBottomSheet(
            onDismissRequest = { showStats = false },
            sheetState = statsSheetState,
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            StatsScreen(
                onDismiss = { showStats = false },
                onNavigateToLog = { log ->
                    showStats = false
                    selectedLog = log
                    showDetail = true
                },
                referenceYearMonth = currentYear to currentMonth
            )
        }
    }

    // 1.8: same full-screen viewer as the Log tab — share / edit / favorite /
    // remove-photo / delete all live here, not just the card.
    selectedPhotoLog?.let { p ->
        androidx.activity.compose.BackHandler { selectedPhotoLog = null }
        Surface(modifier = Modifier.fillMaxSize()) {
            PhotoLogPager(
                logs = photoLogs,
                startId = p.id,
                onDismiss = { selectedPhotoLog = null },
                onEdit = { log ->
                    selectedPhotoLog = null
                    photoEditorTarget = EditorTarget.Edit(log)
                },
                onDelete = { log ->
                    photoLogVm.delete(log)
                    selectedPhotoLog = null
                },
                onToggleFavorite = { photoLogVm.toggleFavorite(it) },
                onAttachPhoto = { log, uri ->
                    photoLogVm.savePhotoFromUri(uri) { saved, _ ->
                        if (saved != null) photoLogVm.attachPhoto(log, saved)
                    }
                },
                onRemovePhoto = { photoLogVm.removePhoto(it) }
            )
        }
    }

    photoEditorTarget?.let { target ->
        androidx.activity.compose.BackHandler { photoEditorTarget = null }
        Surface(modifier = Modifier.fillMaxSize()) {
            PhotoLogEditScreen(
                target = target,
                vm = photoLogVm,
                onDismiss = { photoEditorTarget = null }
            )
        }
    }

    if (showNoteEditor) {
        val allTags = notes.flatMap { it.tags }.distinct().sorted()
        androidx.activity.compose.BackHandler { showNoteEditor = false }
        Surface(modifier = Modifier.fillMaxSize()) {
            NoteEditorScreen(
                existingNote = selectedNote,
                classLogs = logs,
                allTags = allTags,
                onDismiss = { showNoteEditor = false },
                vm = notesVm
            )
        }
    }
}


@Composable
private fun HistoryUnloggedBanner(
    count: Int,
    onAddAll: () -> Unit,
    onDismiss: () -> Unit,
    onHide: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.history_unlogged_banner, count),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAddAll,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.history_unlogged_add_all), fontWeight = FontWeight.SemiBold)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.history_unlogged_dismiss))
                }
                TextButton(onClick = onHide) {
                    Text(
                        stringResource(R.string.history_unlogged_hide),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryUnloggedWorkoutRow(
    workout: HealthConnectManager.ScannedWorkout,
    onAdd: () -> Unit
) {
    val timeText = remember(workout.startTimeMillis) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(workout.startTimeMillis))
    }
    val subText = buildString {
        if (workout.durationMin > 0) append("${workout.durationMin} min")
        if (workout.kcal > 0) { if (isNotEmpty()) append(" · "); append("${workout.kcal} kcal") }
        if (workout.avgBpm > 0) { if (isNotEmpty()) append(" · "); append("${workout.avgBpm} bpm") }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF4F4F6))
            .clickable(onClick = onAdd)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = Color(0xFFE91E63),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                timeText,
                color = Color(0xFF1A1A1A),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (subText.isNotEmpty()) {
                Text(subText, color = Color(0xFF8A8A8A), fontSize = 11.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                stringResource(R.string.history_unlogged_add),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun HistoryPhotoLogBanner(photoLog: PhotoLog, onTap: () -> Unit) {
    if (photoLog.isWorkoutOnly) {
        HistoryWorkoutBanner(photoLog = photoLog, onTap = onTap)
        return
    }

    val context = LocalContext.current
    val displayName = photoLog.filteredPhotoPath ?: photoLog.photoPath
    val photoFile = remember(displayName) { PhotoLogStorage.fileFor(context, displayName) }
    val dateText = remember(photoLog.date) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(photoLog.date))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Gray.copy(alpha = 0.25f))
            .clickable(onClick = onTap)
    ) {
        AsyncImage(
            model = photoFile,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom gradient for text legibility
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )

        if (photoLog.isFavorite) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFE91E63),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(14.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                dateText,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            photoLog.kcal?.let { k ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        "${k}kcal",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryWorkoutBanner(photoLog: PhotoLog, onTap: () -> Unit) {
    val dateText = remember(photoLog.date) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(photoLog.date))
    }
    val subText = buildString {
        photoLog.durationMin?.let { append("$it min") }
        photoLog.avgBPM?.let {
            if (isNotEmpty()) append(" · ")
            append("$it bpm")
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF4F4F6))
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (photoLog.isFavorite) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFE91E63),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
            )
        }
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                dateText,
                color = Color(0xFF1A1A1A),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (subText.isNotEmpty()) {
                Text(
                    subText,
                    color = Color(0xFF8A8A8A),
                    fontSize = 11.sp
                )
            }
        }
        photoLog.kcal?.takeIf { it > 0 }?.let { k ->
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    "$k",
                    color = Color(0xFF111111),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 44.sp
                )
                Text(
                    "kcal",
                    color = Color(0xFF999999),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                )
            }
        }
    }
}

