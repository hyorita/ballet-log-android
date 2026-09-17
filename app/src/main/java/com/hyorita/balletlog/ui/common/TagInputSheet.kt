package com.hyorita.balletlog.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.hyorita.balletlog.R
import com.hyorita.balletlog.data.model.PhotoLogTag
import com.hyorita.balletlog.ui.photolog.SheetHeader
import kotlinx.coroutines.delay

/**
 * Bottom-sheet tag editor against the shared `photo_log_tags` pool. Shared
 * between the Log tab (studio/level/teacher on a photo) and the Class tab
 * (same three fields, same pool, so a tag typed on either tab suggests on
 * both) — one widget, not a parallel one per tab.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagInputSheet(
    title: String,
    placeholder: String,
    icon: ImageVector,
    value: String,
    tags: List<PhotoLogTag>,
    onValueChange: (String) -> Unit,
    onDeleteTag: (PhotoLogTag) -> Unit,
    onDismiss: () -> Unit
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Own selection so re-opening the sheet drops the cursor at the end of
    // the existing value instead of jumping to position 0.
    var tfv by remember {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    LaunchedEffect(title) {
        delay(80)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tagToDeleteConfirm by remember { mutableStateOf<PhotoLogTag?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = {
            BottomSheetDefaults.windowInsets.union(WindowInsets.ime)
        },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            SheetHeader(title = title, onConfirm = onDismiss)

            // Borderless input row + hairline divider — iOS style
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.height(20.dp).width(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = tfv,
                        onValueChange = { newTfv ->
                            tfv = newTfv
                            onValueChange(newTfv.text)
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                        decorationBox = { inner ->
                            if (tfv.text.isEmpty()) {
                                Text(
                                    placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )
            }

            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.photolog_recent_tags),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.take(20).forEach { tag ->
                        SuggestionChip(
                            value = tag.value,
                            onTap = {
                                tfv = TextFieldValue(tag.value, TextRange(tag.value.length))
                                onValueChange(tag.value)
                                // Picking a suggestion is a complete choice — no
                                // reason to make the user also tap the checkmark.
                                onDismiss()
                            },
                            onLongPress = { tagToDeleteConfirm = tag }
                        )
                    }
                }
            }
        }
    }

    tagToDeleteConfirm?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDeleteConfirm = null },
            title = { Text(stringResource(R.string.photolog_tag_delete_title)) },
            text = { Text("\"${tag.value}\"") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTag(tag)
                    tagToDeleteConfirm = null
                }) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDeleteConfirm = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuggestionChip(
    value: String,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = Modifier.combinedClickable(
            onClick = onTap,
            onLongClick = onLongPress
        )
    ) {
        Text(
            value,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
