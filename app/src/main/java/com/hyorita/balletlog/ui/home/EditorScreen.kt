package com.hyorita.balletlog.ui.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.DefaultSteps
import com.hyorita.balletlog.data.PhotoManager
import com.hyorita.balletlog.data.TermLanguagePreferences
import com.hyorita.balletlog.data.TermStore
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.PhotoItem
import com.hyorita.balletlog.data.model.Step
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    existingLog: ClassLog?,
    onDismiss: (saved: Boolean) -> Unit,
    vm: HomeViewModel = viewModel(),
    initialDate: Long? = null
) {
    val context = LocalContext.current
    var date by remember {
        mutableStateOf(
            existingLog?.date ?: initialDate ?: System.currentTimeMillis()
        )
    }
    val barreSteps = remember {
        mutableStateListOf<Step>().also {
            it.addAll(existingLog?.barreSteps ?: DefaultSteps.barre)
        }
    }
    val centerSteps = remember {
        mutableStateListOf<Step>().also {
            it.addAll(existingLog?.centerSteps ?: DefaultSteps.center)
        }
    }
    val photos = remember { mutableStateListOf<PhotoItem>().also { it.addAll(existingLog?.photos ?: emptyList()) } }
    var barreMusic by remember { mutableStateOf(existingLog?.barreMusic ?: "") }
    var centerMusic by remember { mutableStateOf(existingLog?.centerMusic ?: "") }
    var notes by remember { mutableStateOf(existingLog?.notes ?: "") }
    var favorite by remember { mutableStateOf(existingLog?.favorite ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var tabIndex by remember { mutableIntStateOf(0) }
    var showDiscardAlert by remember { mutableStateOf(false) }
    var showWorkoutNotFoundAlert by remember { mutableStateOf(false) }
    var fetchedWorkout by remember { mutableStateOf<com.hyorita.balletlog.data.model.WorkoutInfo?>(null) }
    var savedLogId by remember { mutableStateOf<String?>(existingLog?.id) }

    val hasChanges = existingLog == null ||
        savedLogId != existingLog.id ||
        date != existingLog.date ||
        notes != existingLog.notes ||
        barreMusic != existingLog.barreMusic ||
        centerMusic != existingLog.centerMusic ||
        photos.map { it.fileName } != existingLog.photos.map { it.fileName } ||
        barreSteps.map { it.name + it.note } != existingLog.barreSteps.map { it.name + it.note } ||
        centerSteps.map { it.name + it.note } != existingLog.centerSteps.map { it.name + it.note }

    // 1.8: auto-save covers both existing logs (always update) and new
    // logs with meaningful content typed (insert). Brand-new drafts with
    // no real content are still skipped so the user doesn't end up with
    // ghost rows just from opening the editor.
    val hasMeaningfulContent = notes.isNotBlank() ||
        photos.isNotEmpty() ||
        barreSteps.any { it.note.isNotBlank() } ||
        centerSteps.any { it.note.isNotBlank() } ||
        barreMusic.isNotBlank() ||
        centerMusic.isNotBlank()

    var didSaveExplicitly by remember { mutableStateOf(false) }
    var didDiscardExplicitly by remember { mutableStateOf(false) }
    val persistInPlace = persist@{
        val gson = com.google.gson.Gson()
        val targetId = savedLogId ?: existingLog?.id
        if (targetId != null) {
            val updated = ClassLog.create(
                date = date,
                barreSteps = barreSteps.toList(),
                centerSteps = centerSteps.toList(),
                photos = photos.toList(),
                barreMusic = barreMusic,
                centerMusic = centerMusic,
                notes = notes,
                favorite = favorite
            ).copy(
                id = targetId,
                workoutJson = fetchedWorkout?.let { gson.toJson(it) }
                    ?: existingLog?.workoutJson
            )
            vm.updateLog(updated)
        } else if (hasMeaningfulContent) {
            val inserted = ClassLog.create(
                date = date,
                barreSteps = barreSteps.toList(),
                centerSteps = centerSteps.toList(),
                photos = photos.toList(),
                barreMusic = barreMusic,
                centerMusic = centerMusic,
                notes = notes,
                favorite = favorite
            ).copy(
                workoutJson = fetchedWorkout?.let { gson.toJson(it) }
            )
            vm.insertLog(inserted)
            // Prevent a double-insert if onDispose somehow fires twice in
            // the same composition lifetime.
            savedLogId = inserted.id
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!didSaveExplicitly && !didDiscardExplicitly) persistInPlace()
        }
    }

    // Discard confirmation only when the user is creating a brand-new log
    // and actually typed something. Edits to existing logs auto-save on
    // dispose; empty new drafts vanish silently.
    val needsDiscardConfirm = existingLog == null && savedLogId == null && hasMeaningfulContent
    BackHandler(enabled = needsDiscardConfirm || fetchedWorkout != null) {
        when {
            fetchedWorkout != null -> onDismiss(true)
            needsDiscardConfirm -> showDiscardAlert = true
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { uri ->
            PhotoManager.savePhoto(context, uri)?.let { fileName ->
                photos.add(PhotoItem(fileName = fileName))
            }
        }
    } // 0 = Barre, 1 = Center

    // Chip bar state — the focused step.note field, its TextFieldValue cache,
    // and the live currentStepName/wordFragment in TermStore.
    LaunchedEffect(Unit) { TermStore.loadIfNeeded(context) }
    var activeStepId by remember { mutableStateOf<String?>(null) }
    val stepTfvs = remember { mutableStateMapOf<String, TextFieldValue>() }
    DisposableEffect(Unit) {
        onDispose { TermStore.clearContext() }
    }

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val currentSteps = if (tabIndex == 0) barreSteps else centerSteps

    // Collapse the top bar on scroll so the editor's input area gets more
    // room when the user starts typing / scrolling steps.
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = topBarScrollBehavior,
                title = {
                    Text(
                        if (existingLog == null) stringResource(R.string.new_class) else stringResource(R.string.edit_class),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            fetchedWorkout != null -> onDismiss(true)
                            needsDiscardConfirm -> showDiscardAlert = true
                            else -> onDismiss(false)
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = stringResource(R.string.photos))
                    }
                    IconButton(onClick = { favorite = !favorite }) {
                        Icon(
                            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorites),
                            tint = if (favorite) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = {
                        val gson = com.google.gson.Gson()
                        val newLog = ClassLog.create(
                            date = date,
                            barreSteps = barreSteps.toList(),
                            centerSteps = centerSteps.toList(),
                            photos = photos.toList(),
                            barreMusic = barreMusic,
                            centerMusic = centerMusic,
                            notes = notes,
                            favorite = favorite
                        ).let {
                            val id = savedLogId
                            if (id != null) it.copy(
                                id = id,
                                workoutJson = fetchedWorkout?.let { w -> gson.toJson(w) }
                                    ?: existingLog?.workoutJson
                            ) else it.copy(
                                workoutJson = fetchedWorkout?.let { w -> gson.toJson(w) }
                            )
                        }
                        if (savedLogId == null) vm.insertLog(newLog) else vm.updateLog(newLog)
                        didSaveExplicitly = true
                        onDismiss(true)
                    }) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .background(MaterialTheme.colorScheme.surface)
        ) {
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        // LazyColumn items in order:
        //   0  date
        //   1? workout info (only when fetchedWorkout != null)
        //   _  tab card
        //   _? photos card (only when photos.isNotEmpty())
        //   _+ items(currentSteps.size) — each step
        // Steps start at this offset, computed reactively.
        val stepIndexOffset = 1 + (if (fetchedWorkout != null) 1 else 0) +
            1 + (if (photos.isNotEmpty()) 1 else 0)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 날짜
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(stringResource(R.string.date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(dateFormat.format(Date(date)))
                        }
                    }
                }
            }

            // 워크아웃 정보 (찾은 경우 표시)
            fetchedWorkout?.let { workout ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.ballet_workout_found),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("⏱ ${if (workout.durationMinutes >= 60) "${workout.durationMinutes / 60}h ${workout.durationMinutes % 60}m" else "${workout.durationMinutes}m"}",
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("🔥 ${workout.activeCalories}kcal",
                                    style = MaterialTheme.typography.bodyMedium)
                                if (workout.avgHeartRate > 0)
                                    Text("❤️ ${workout.avgHeartRate}bpm",
                                        style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // 탭 카드
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    TabRow(
                        selectedTabIndex = tabIndex,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 },
                            text = { Text(stringResource(R.string.barre)) })
                        Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 },
                            text = { Text(stringResource(R.string.center)) })
                    }
                }
            }

            // 사진 썸네일 (있을 때만 표시)
            if (photos.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(stringResource(R.string.photos),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(photos) { photo ->
                                    Box {
                                        AsyncImage(
                                            model = PhotoManager.getPhotoFile(context, photo.fileName),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(MaterialTheme.shapes.small)
                                        )
                                        IconButton(
                                            onClick = { photos.remove(photo) },
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.TopEnd)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove",
                                                modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 각 Step 독립 카드
            items(currentSteps.size) { index ->
                val step = currentSteps[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = String.format("%02d", index + 1),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(28.dp)
                            )
                            BasicTextField(
                                value = step.name,
                                onValueChange = { currentSteps[index] = step.copy(name = it) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                textStyle = MaterialTheme.typography.titleSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                decorationBox = { inner ->
                                    Box {
                                        if (step.name.isEmpty()) {
                                            Text(stringResource(R.string.step_name),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        inner()
                                    }
                                }
                            )
                            IconButton(
                                onClick = { currentSteps.removeAt(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        // TextFieldValue keeps the caret so the chip bar can
                        // replace the word fragment under the caret cleanly.
                        val tfv = stepTfvs.getOrPut(step.id) {
                            TextFieldValue(step.note, TextRange(step.note.length))
                        }
                        // External step.note changes (e.g. chip insert) must
                        // propagate into the cached tfv.
                        val syncedTfv = if (tfv.text != step.note) {
                            TextFieldValue(step.note, TextRange(step.note.length))
                                .also { stepTfvs[step.id] = it }
                        } else tfv

                        val scrollScope = rememberCoroutineScope()
                        OutlinedTextField(
                            value = syncedTfv,
                            onValueChange = { new ->
                                stepTfvs[step.id] = new
                                currentSteps[index] = step.copy(note = new.text)
                                if (activeStepId == step.id) {
                                    TermStore.setWordFragment(wordFragmentBeforeCaret(new))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .onFocusChanged { state ->
                                    if (state.hasFocus) {
                                        activeStepId = step.id
                                        TermStore.setStepName(step.name)
                                        TermStore.setWordFragment(wordFragmentBeforeCaret(syncedTfv))
                                        scrollScope.launch {
                                            // Park the focused step at the
                                            // top of the visible area so the
                                            // user sees what they're editing
                                            // instead of the page header.
                                            listState.animateScrollToItem(
                                                stepIndexOffset + index
                                            )
                                        }
                                    } else if (activeStepId == step.id) {
                                        // Only clear when the active field
                                        // loses focus — not when a different
                                        // step's blur fires.
                                        activeStepId = null
                                        TermStore.clearContext()
                                    }
                                },
                            placeholder = { Text(stringResource(R.string.combination_notes), style = MaterialTheme.typography.bodySmall) },
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }

            // Add Step 버튼
            item {
                OutlinedButton(
                    onClick = { currentSteps.add(Step()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_step))
                }
            }

            // 메모
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(stringResource(R.string.notes_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text(stringResource(R.string.class_notes)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            maxLines = 10,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                        )
                    }
                }
            }

            // YouTube URL 카드
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(stringResource(R.string.youtube_url),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = if (tabIndex == 0) barreMusic else centerMusic,
                            onValueChange = {
                                if (tabIndex == 0) barreMusic = it else centerMusic = it
                            },
                            placeholder = { Text("https://youtube.com/...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            )
                        )
                    }
                }
            }

            // Find Ballet Workout 버튼
            item {
                OutlinedButton(
                    onClick = {
                        android.util.Log.d("HealthConnect", "Find Ballet Workout clicked, existingLog=${existingLog?.id ?: "NULL"}")
                        // 먼저 저장 후 워크아웃 fetch
                        val newLog = ClassLog.create(
                            date = date,
                            barreSteps = barreSteps.toList(),
                            centerSteps = centerSteps.toList(),
                            photos = photos.toList(),
                            barreMusic = barreMusic,
                            centerMusic = centerMusic,
                            notes = notes,
                            favorite = favorite
                        ).let {
                            val id = savedLogId
                            if (id != null) it.copy(id = id) else it
                        }
                        if (savedLogId == null) {
                            vm.insertAndFetchWorkout(newLog) { workout ->
                                savedLogId = newLog.id
                                if (workout != null) fetchedWorkout = workout
                                else showWorkoutNotFoundAlert = true
                            }
                        } else {
                            vm.updateAndFetchWorkout(newLog) { workout ->
                                if (workout != null) fetchedWorkout = workout
                                else showWorkoutNotFoundAlert = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Watch, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.find_ballet_workout))
                }
            }
        }

        if (activeStepId != null) {
            TermChipBar(
                onInsertTerm = { term ->
                    val id = activeStepId ?: return@TermChipBar
                    val cur = stepTfvs[id] ?: return@TermChipBar
                    val lang = TermLanguagePreferences.get(context)
                    val newTfv = insertTermAtWordBoundary(cur, term.text(lang))
                    stepTfvs[id] = newTfv
                    val barreIdx = barreSteps.indexOfFirst { it.id == id }
                    if (barreIdx >= 0) {
                        barreSteps[barreIdx] = barreSteps[barreIdx].copy(note = newTfv.text)
                    } else {
                        val centerIdx = centerSteps.indexOfFirst { it.id == id }
                        if (centerIdx >= 0) {
                            centerSteps[centerIdx] = centerSteps[centerIdx].copy(note = newTfv.text)
                        }
                    }
                    TermStore.setWordFragment("")
                    TermStore.recordUsage(context, term)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showWorkoutNotFoundAlert) {
        AlertDialog(
            onDismissRequest = { showWorkoutNotFoundAlert = false },
            title = { Text(stringResource(R.string.no_workout_found)) },
            text = { Text(stringResource(R.string.no_workout_found_message)) },
            confirmButton = {
                TextButton(onClick = { showWorkoutNotFoundAlert = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (showDiscardAlert) {
        AlertDialog(
            onDismissRequest = { showDiscardAlert = false },
            title = { Text(stringResource(R.string.leave_page)) },
            text = { Text(stringResource(R.string.changes_not_saved)) },
            confirmButton = {
                TextButton(onClick = { showDiscardAlert = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDiscardAlert = false
                    didDiscardExplicitly = true
                    onDismiss(false)
                }) { Text(stringResource(R.string.discard)) }
            }
        )
    }
}
