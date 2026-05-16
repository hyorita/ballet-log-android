package com.hyorita.balletlog.ui.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.model.ClassLog
import com.hyorita.balletlog.data.model.Note
import com.hyorita.balletlog.ui.common.NoteCard
import com.hyorita.balletlog.ui.home.DetailScreen
import com.hyorita.balletlog.ui.home.EditorScreen
import com.hyorita.balletlog.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    vm: NotesViewModel = viewModel(),
    homeVm: HomeViewModel = viewModel()
) {
    val notes by vm.notes.collectAsState()
    val classLogs by vm.classLogs.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var tagToDelete by remember { mutableStateOf<String?>(null) }
    var showAllTags by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var openedLinkedLog by remember { mutableStateOf<ClassLog?>(null) }
    var showLinkedClassEditor by remember { mutableStateOf(false) }
    var photoViewerStartIndex by remember { mutableStateOf<Int?>(null) }

    val allTags = remember(notes) {
        notes.flatMap { it.tags }.distinct().sorted()
    }

    val filtered = remember(notes, searchQuery, selectedTag) {
        val matched = notes.filter { note ->
            val matchesSearch = searchQuery.isEmpty() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true) ||
                note.tags.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesTag = selectedTag == null || note.tags.contains(selectedTag)
            matchesSearch && matchesTag
        }
        // Pinned notes float to the top, then most-recent.
        matched.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_notes),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { selectedNote = null; showEditor = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.new_note)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = {
                    searchQuery = ""
                    selectedTag = null
                }
            )
        }
    ) { padding ->
        if (notes.isEmpty()) {
            EmptyNotesState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tag filter row — FlowRow with "+N more" toggle, hidden during search
                if (allTags.isNotEmpty() && searchQuery.isEmpty()) {
                    item {
                        TagChipsRow(
                            allTags = allTags,
                            selectedTag = selectedTag,
                            showAll = showAllTags,
                            onTagTap = { tag ->
                                selectedTag = if (selectedTag == tag) null else tag
                            },
                            onTagLongPress = { tag -> tagToDelete = tag },
                            onToggleShowAll = { showAllTags = !showAllTags }
                        )
                    }
                }

                // Selected tag chip when searching
                if (selectedTag != null && searchQuery.isNotEmpty()) {
                    item {
                        SelectedTagChip(
                            tag = selectedTag!!,
                            onClear = {
                                selectedTag = null
                                searchQuery = ""
                            }
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onTap = { selectedNote = note; showDetail = true },
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .combinedClickable(
                                    onClick = { selectedNote = note; showDetail = true },
                                    onLongClick = { noteToDelete = note }
                                ),
                            untitledLabel = stringResource(R.string.untitled)
                        )
                    }
                }
            }
        }
    }

    // Hide root NavBar whenever any of the full-screen overlays below are
    // visible so they truly cover the screen and IME padding doesn't
    // double-count the NavBar inset.
    val bottomBarVisible = com.hyorita.balletlog.LocalBottomBarVisible.current
    val anyModalActive = showDetail || openedLinkedLog != null ||
        showLinkedClassEditor || photoViewerStartIndex != null || showEditor
    androidx.compose.runtime.DisposableEffect(anyModalActive) {
        if (anyModalActive) bottomBarVisible.value = false
        onDispose { bottomBarVisible.value = true }
    }

    if (showDetail) {
        val note = selectedNote ?: return
        val liveNote = notes.find { it.id == note.id } ?: note
        androidx.activity.compose.BackHandler { showDetail = false }
        Surface(modifier = Modifier.fillMaxSize()) {
            NoteDetailScreen(
                note = liveNote,
                classLogs = classLogs,
                onDismiss = { showDetail = false },
                onEdit = { showEditor = true },
                onDelete = { vm.delete(note); showDetail = false },
                onTogglePin = { vm.togglePin(note) },
                onOpenLog = { log -> openedLinkedLog = log },
                onOpenPhoto = { idx -> photoViewerStartIndex = idx }
            )
        }
    }

    openedLinkedLog?.let { log ->
        val liveLog = classLogs.find { it.id == log.id } ?: log
        androidx.activity.compose.BackHandler { openedLinkedLog = null }
        Surface(modifier = Modifier.fillMaxSize()) {
            key(liveLog.workoutJson) {
                DetailScreen(
                    log = liveLog,
                    onDismiss = { openedLinkedLog = null },
                    onEdit = { showLinkedClassEditor = true },
                    onDelete = {
                        homeVm.deleteLog(liveLog)
                        openedLinkedLog = null
                    },
                    onToggleFavorite = { homeVm.toggleFavorite(liveLog) },
                    onFetchWorkout = { homeVm.fetchAndSaveWorkout(liveLog) },
                    onView = { homeVm.incrementViewCount(liveLog.id) }
                )
            }
        }
    }

    if (showLinkedClassEditor) {
        val liveLog = openedLinkedLog?.let { sel -> classLogs.find { it.id == sel.id } }
            ?: openedLinkedLog
        androidx.activity.compose.BackHandler { showLinkedClassEditor = false }
        Surface(modifier = Modifier.fillMaxSize()) {
            EditorScreen(
                existingLog = liveLog,
                onDismiss = { _ -> showLinkedClassEditor = false },
                vm = homeVm
            )
        }
    }

    photoViewerStartIndex?.let { startIdx ->
        val note = selectedNote?.let { sel -> notes.find { it.id == sel.id } } ?: selectedNote
        val photos = note?.photoFileNames.orEmpty()
        if (photos.isNotEmpty()) {
            androidx.activity.compose.BackHandler { photoViewerStartIndex = null }
            Surface(modifier = Modifier.fillMaxSize()) {
                NotePhotoViewer(
                    photoFileNames = photos,
                    startIndex = startIdx,
                    onDismiss = { photoViewerStartIndex = null }
                )
            }
        }
    }

    if (showEditor) {
        androidx.activity.compose.BackHandler { showEditor = false }
        Surface(modifier = Modifier.fillMaxSize()) {
            NoteEditorScreen(
                existingNote = selectedNote,
                classLogs = classLogs,
                allTags = allTags,
                onDismiss = { showEditor = false },
                vm = vm
            )
        }
    }

    // Long-press delete confirmation (from NotesScreen list)
    noteToDelete?.let { n ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(stringResource(R.string.delete_note_title)) },
            text = { Text(stringResource(R.string.delete_note_message)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(n)
                    noteToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Tag delete confirmation
    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text(stringResource(R.string.notes_delete_tag_title)) },
            text = { Text(stringResource(R.string.notes_delete_tag_message, tag)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTagFromAllNotes(tag)
                    if (selectedTag == tag) selectedTag = null
                    tagToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun EmptyNotesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 60.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("✏️", style = MaterialTheme.typography.displayMedium)
        Text(
            stringResource(R.string.no_notes_yet),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.tap_new_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun TagChipsRow(
    allTags: List<String>,
    selectedTag: String?,
    showAll: Boolean,
    onTagTap: (String) -> Unit,
    onTagLongPress: (String) -> Unit,
    onToggleShowAll: () -> Unit
) {
    // ContextualFlowRow exposes shownItemCount during composition (FlowRow's
    // overflow scope only exposes it at draw time, which crashes). Collapsed
    // path uses 2 lines + "+N more"; expanded path falls back to plain
    // FlowRow + "Show less" trailing chip.
    val rowMod = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    val hSpacing = Arrangement.spacedBy(8.dp)
    val vSpacing = Arrangement.spacedBy(8.dp)

    if (!showAll) {
        ContextualFlowRow(
            itemCount = allTags.size,
            maxLines = 2,
            overflow = ContextualFlowRowOverflow.expandIndicator {
                val hidden = totalItemCount - shownItemCount
                ToggleChip(
                    text = stringResource(R.string.notes_show_more, hidden),
                    onTap = onToggleShowAll
                )
            },
            modifier = rowMod,
            horizontalArrangement = hSpacing,
            verticalArrangement = vSpacing
        ) { index ->
            val tag = allTags[index]
            TagChip(
                text = tag,
                selected = selectedTag == tag,
                onTap = { onTagTap(tag) },
                onLongPress = { onTagLongPress(tag) }
            )
        }
    } else {
        FlowRow(
            modifier = rowMod,
            horizontalArrangement = hSpacing,
            verticalArrangement = vSpacing
        ) {
            allTags.forEach { tag ->
                TagChip(
                    text = tag,
                    selected = selectedTag == tag,
                    onTap = { onTagTap(tag) },
                    onLongPress = { onTagLongPress(tag) }
                )
            }
            ToggleChip(
                text = stringResource(R.string.notes_show_less),
                onTap = onToggleShowAll
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagChip(
    text: String,
    selected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = if (selected)
            MaterialTheme.colorScheme.onSurface
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.combinedClickable(
            onClick = onTap,
            onLongClick = onLongPress
        )
    ) {
        Text(
            text = text,
            color = if (selected)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ToggleChip(text: String, onTap: () -> Unit) {
    Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.CircleShape)
                .clickable(onClick = onTap)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SelectedTagChip(tag: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.onSurface
        ) {
            Text(
                text = tag,
                color = MaterialTheme.colorScheme.surface,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Bottom-pinned search bar — mirrors iOS NotesView.safeAreaInset(.bottom).
 * Sits above the keyboard via imePadding so typing never hides the field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    Surface(
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.imePadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focused = it.isFocused },
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text(
                                        stringResource(R.string.search),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.weight(1f)) { inner() }
                                    if (query.isNotEmpty()) {
                                        IconButton(
                                            onClick = onClear,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
                if (focused) {
                    IconButton(onClick = { keyboard?.hide() }) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.photolog_close),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

