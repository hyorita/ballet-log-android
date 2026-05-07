package com.hyorita.balletlog.ui.notes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hyorita.balletlog.data.PhotoManager
import androidx.compose.ui.res.stringResource
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.Note
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    existingNote: Note?,
    classLogs: List<ClassLog>,
    allTags: List<String>,
    onDismiss: () -> Unit,
    vm: NotesViewModel
) {
    val context = LocalContext.current

    var content by remember { mutableStateOf(
        if (existingNote != null) {
            if (existingNote.content.isNotEmpty()) "${existingNote.title}\n${existingNote.content}"
            else existingNote.title
        } else ""
    )}
    val selectedTags = remember { mutableStateListOf<String>().also {
        it.addAll(existingNote?.tags ?: emptyList())
    }}
    var newTagInput by remember { mutableStateOf("") }
    val photoFileNames = remember { mutableStateListOf<String>().also {
        it.addAll(existingNote?.photoFileNames ?: emptyList())
    }}
    var urlLink by remember { mutableStateOf(existingNote?.urlLink ?: "") }
    var linkedLogId by remember { mutableStateOf(existingNote?.linkedLogId) }
    var showLogPicker by remember { mutableStateOf(false) }
    val linkedLog = remember(linkedLogId, classLogs) {
        linkedLogId?.let { id -> classLogs.find { it.id == id } }
    }

    val displayTags by remember(allTags, selectedTags.toList()) {
        derivedStateOf {
            (allTags + selectedTags).distinct().sorted()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { uri ->
            val fileName = PhotoManager.savePhoto(context, uri)
            if (fileName != null) photoFileNames.add(fileName)
        }
    }

    // Auto-save matches iOS NoteEditorView: any dismissal (back press, X tap,
    // future drag-down) persists the current state if the body has content,
    // unless the explicit ✓ already saved.
    var didSaveExplicitly by remember { mutableStateOf(false) }
    val persist: () -> Unit = persist@{
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return@persist
        val lines = content.lines()
        val extractedTitle = lines.firstOrNull()?.trim() ?: ""
        val extractedContent = lines.drop(1).joinToString("\n").trimStart()
        val note = Note.create(
            title = extractedTitle,
            content = extractedContent,
            tags = selectedTags.toList(),
            photoFileNames = photoFileNames.toList(),
            linkedLogId = linkedLogId,
            urlLink = urlLink.ifEmpty { null },
            pinned = existingNote?.pinned ?: false
        ).let {
            if (existingNote != null) it.copy(
                id = existingNote.id,
                createdAt = existingNote.createdAt,
                updatedAt = System.currentTimeMillis()
            ) else it
        }
        if (existingNote == null) vm.insert(note) else vm.update(note)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!didSaveExplicitly) persist()
        }
    }

    // Auto-focus the body field on entry (matches iOS 0.3s focus delay).
    val bodyFocus = remember { FocusRequester() }
    var bodyFocused by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        delay(300)
        runCatching { bodyFocus.requestFocus() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.ime,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (existingNote == null) stringResource(R.string.new_note) else stringResource(R.string.edit_note),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!didSaveExplicitly) persist()
                        didSaveExplicitly = true
                        onDismiss()
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                actions = {
                    // Keyboard dismiss appears only while the body is focused —
                    // mirrors iOS's keyboard-toolbar chevron-down.
                    if (bodyFocused) {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                        }) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.photolog_close),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            persist()
                            didSaveExplicitly = true
                            onDismiss()
                        },
                        enabled = content.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 본문 카드 (첫 줄이 제목)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.what_to_remember),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp)
                            .focusRequester(bodyFocus)
                            .onFocusChanged { bodyFocused = it.isFocused },
                        maxLines = Int.MAX_VALUE,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f),
                            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f)
                        )
                    )
                }
            }

            // Tags 섹션
            item {
                Text(stringResource(R.string.tags),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp))
            }

            // 태그 chips — selected (X to remove) + available (tap to add),
            // grouped in one FlowRow so the layout stays compact.
            val availableTags = allTags.filter { !selectedTags.contains(it) }
            if (selectedTags.isNotEmpty() || availableTags.isNotEmpty()) {
                item {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedTags.forEach { tag ->
                            SelectedTagChip(
                                text = tag,
                                onRemove = { selectedTags.remove(tag) }
                            )
                        }
                        availableTags.forEach { tag ->
                            AvailableTagChip(
                                text = tag,
                                onAdd = { selectedTags.add(tag) }
                            )
                        }
                    }
                }
            }

            // 새 태그 입력
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        placeholder = { Text("e.g. épaulement, alignment") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f),
                            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f)
                        ),
                        trailingIcon = {
                            if (newTagInput.isNotEmpty()) {
                                TextButton(onClick = {
                                    val tag = newTagInput.trim()
                                    if (tag.isNotEmpty() && !selectedTags.contains(tag)) {
                                        selectedTags.add(tag)
                                    }
                                    newTagInput = ""
                                }) { Text("Add") }
                            }
                        }
                    )
                }
            }

            // Photos — placeholder camera card always shown, attached
            // thumbnails follow with a corner X to remove (iOS matches).
            item {
                Text(
                    stringResource(R.string.photos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.photos),
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    items(photoFileNames) { fileName ->
                        Box {
                            AsyncImage(
                                model = PhotoManager.getPhotoFile(context, fileName),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            IconButton(
                                onClick = { photoFileNames.remove(fileName) },
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Linked Class — pick a ClassLog to link this note to
            item {
                Text(
                    stringResource(R.string.link_to_class),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(6.dp))
                Card(
                    onClick = { showLogPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_ballet_shoe),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        if (linkedLog != null) {
                            Text(
                                SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                    .format(Date(linkedLog.date)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { linkedLogId = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Text(
                                stringResource(R.string.select_a_class_optional),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Reference Link 카드
            item {
                Text(stringResource(R.string.reference_link),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    OutlinedTextField(
                        value = urlLink,
                        onValueChange = { urlLink = it },
                        placeholder = { Text(stringResource(R.string.youtube_instagram_link)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f),
                            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f)
                        ),
                        trailingIcon = {
                            if (urlLink.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            runCatching {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(urlLink)
                                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.OpenInNew,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    IconButton(onClick = { urlLink = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showLogPicker) {
        LogPickerSheet(
            classLogs = classLogs,
            selectedLogId = linkedLogId,
            onSelect = { log ->
                linkedLogId = log.id
                showLogPicker = false
            },
            onDismiss = { showLogPicker = false }
        )
    }
}

@Composable
private fun SelectedTagChip(text: String, onRemove: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.surface,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .size(12.dp)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
private fun AvailableTagChip(text: String, onAdd: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onAdd)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogPickerSheet(
    classLogs: List<ClassLog>,
    selectedLogId: String?,
    onSelect: (ClassLog) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val sorted = remember(classLogs) { classLogs.sortedByDescending { it.date } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.select_class),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            if (sorted.isEmpty()) {
                Text(
                    stringResource(R.string.no_classes_logged_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(sorted, key = { it.id }) { log ->
                        val stepCount = (log.barreSteps + log.centerSteps)
                            .count { it.name.isNotBlank() || it.note.isNotBlank() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(log) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    dateFmt.format(Date(log.date)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (stepCount > 0) {
                                    Text(
                                        "$stepCount steps",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            if (selectedLogId == log.id) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
