package com.hyorita.balletlog.ui.home

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The word fragment immediately before the caret — used to filter the chip
 * bar to terms whose english (diacritic-insensitive) or korean form starts
 * with the fragment. Walks backward from the caret until whitespace.
 */
fun wordFragmentBeforeCaret(tfv: TextFieldValue): String {
    val cursor = tfv.selection.start.coerceIn(0, tfv.text.length)
    if (cursor == 0) return ""
    val text = tfv.text
    var i = cursor
    while (i > 0 && !text[i - 1].isWhitespace()) i--
    return text.substring(i, cursor)
}

/**
 * Replace the word being typed at the caret with the chosen term + a trailing
 * space, so the user can continue typing the next word immediately. Caret
 * lands right after the inserted space.
 */
fun insertTermAtWordBoundary(tfv: TextFieldValue, term: String): TextFieldValue {
    val cursor = tfv.selection.start.coerceIn(0, tfv.text.length)
    val text = tfv.text
    var wordStart = cursor
    while (wordStart > 0 && !text[wordStart - 1].isWhitespace()) wordStart--

    val replacement = "$term "
    val newText = buildString {
        append(text, 0, wordStart)
        append(replacement)
        append(text, cursor, text.length)
    }
    val newCursor = wordStart + replacement.length
    return TextFieldValue(newText, TextRange(newCursor))
}
