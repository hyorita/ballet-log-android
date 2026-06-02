package com.hyorita.balletlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hyorita.balletlog.data.BalletTerm
import com.hyorita.balletlog.data.TermLanguage
import com.hyorita.balletlog.data.TermLanguagePreferences
import com.hyorita.balletlog.data.TermStore

/**
 * Sticky chip bar above the keyboard. Shows context-relevant ballet terms
 * based on the focused step's name and the word the user is currently
 * typing. Tapping a chip replaces the current word fragment with the term.
 *
 * Visibility / sizing is decided by the caller — this composable just draws
 * the chips for the current TermStore state. Callers usually wrap it in a
 * `Box(Modifier.imePadding())` aligned to the bottom so it floats just above
 * the keyboard while a note field has focus.
 */
@Composable
fun TermChipBar(
    onInsertTerm: (BalletTerm) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        TermStore.loadIfNeeded(context)
    }

    val stats by TermStore.stats.collectAsState()
    val stepName by TermStore.currentStepName.collectAsState()
    val fragment by TermStore.currentWordFragment.collectAsState()

    // Read the saved preference once per composition. The Settings picker
    // writes synchronously, so this stays in sync the next time the bar
    // appears (focus is transient, so re-read on every show is fine).
    val language: TermLanguage = remember(context) { TermLanguagePreferences.get(context) }

    val terms = remember(stats, stepName, fragment) {
        TermStore.computeOrderedTerms(stats, stepName, fragment)
    }

    if (terms.isEmpty()) return

    // Reset horizontal scroll when the focused step changes so the user
    // doesn't see the old step's scroll position bleed into the new step's
    // (different) chip ordering.
    val listState = rememberLazyListState()
    LaunchedEffect(stepName) {
        if (stepName.isNotEmpty()) listState.scrollToItem(0)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.height(48.dp)
        ) {
            items(terms, key = { it.english }) { term ->
                ChipButton(
                    label = term.text(language),
                    onClick = { onInsertTerm(term) }
                )
            }
        }
    }
}

@Composable
private fun ChipButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
