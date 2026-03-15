package com.hyorita.balletlog.ui.notes

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hyorita.balletlog.data.PhotoManager
import androidx.compose.ui.res.stringResource
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.model.Note

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    existingNote: Note?,
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
    var selectedTags = remember { mutableStateListOf<String>().also {
        it.addAll(existingNote?.tags ?: emptyList())
    }}
    var newTagInput by remember { mutableStateOf("") }
    var photoFileNames = remember { mutableStateListOf<String>().also {
        it.addAll(existingNote?.photoFileNames ?: emptyList())
    }}
    var urlLink by remember { mutableStateOf(existingNote?.urlLink ?: "") }
    var showDiscardAlert by remember { mutableStateOf(false) }

    val originalContent = remember {
        if (existingNote != null) {
            if (existingNote.content.isNotEmpty()) "${existingNote.title}\n${existingNote.content}"
            else existingNote.title
        } else ""
    }
    val hasChanges = content != originalContent || urlLink != (existingNote?.urlLink ?: "")

    // 백버튼 인터셉트
    BackHandler(enabled = hasChanges) {
        showDiscardAlert = true
    }

    val displayTags by remember(allTags) {
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

    Scaffold(
        contentWindowInsets = WindowInsets.ime,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingNote == null) stringResource(R.string.new_note) else stringResource(R.string.edit_note),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = {
                        if (hasChanges) showDiscardAlert = true else onDismiss()
                    }) { Text(stringResource(R.string.cancel)) }
                },
                actions = {
                    // 사진 추가
                    IconButton(onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Add photo")
                    }
                    // Save
                    TextButton(
                        onClick = {
                            val lines = content.lines()
                            val extractedTitle = lines.firstOrNull()?.trim() ?: ""
                            val extractedContent = lines.drop(1).joinToString("\n").trimStart()
                            val note = Note.create(
                                title = extractedTitle,
                                content = extractedContent,
                                tags = selectedTags.toList(),
                                photoFileNames = photoFileNames.toList(),
                                urlLink = urlLink.ifEmpty { null },
                                pinned = existingNote?.pinned ?: false
                            ).let {
                                if (existingNote != null) it.copy(
                                    id = existingNote.id,
                                    createdAt = existingNote.createdAt
                                ) else it
                            }
                            if (existingNote == null) vm.insert(note) else vm.update(note)
                            onDismiss()
                        },
                        enabled = content.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
                    }
                }
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
                        placeholder = { Text("First line becomes the title...",
                            style = MaterialTheme.typography.bodyLarge) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
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

            // 태그 chips (선택된 태그 + 기존 태그 토글)
            if (displayTags.isNotEmpty()) {
                item {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayTags.forEach { tag ->
                            val isSelected = selectedTags.contains(tag)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedTags.remove(tag)
                                    else selectedTags.add(tag)
                                },
                                label = { Text(tag) },
                                shape = RoundedCornerShape(20.dp)
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

            // 사진 썸네일 (있을 때만)
            if (photoFileNames.isNotEmpty()) {
                item {
                    Text("Photos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp))
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    Icon(Icons.Default.Close, contentDescription = "Remove",
                                        modifier = Modifier.size(12.dp))
                                }
                            }
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
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f),
                            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.0f)
                        )
                    )
                }
            }
        }
    }

    // 뒤로가기 시 저장 확인 알럿
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
                    onDismiss()
                }) { Text(stringResource(R.string.discard)) }
            }
        )
    }
}
