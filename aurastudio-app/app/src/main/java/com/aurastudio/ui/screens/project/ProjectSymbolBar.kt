package com.aurastudio.ui.screens.project

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.rosemoe.sora.widget.CodeEditor

/** One key in the symbol bar: [label] on screen, [insert] committed at the caret when tapped. */
internal data class UiSymbolKey(val label: String, val insert: String, val pinned: Boolean = false)

/** CodeAssist `DEFAULT_SYMBOL_KEYS` — pinned Tab/`//` group + the scrolling text-key set. */
internal val DEFAULT_SYMBOL_KEYS: List<UiSymbolKey> = listOf(
    UiSymbolKey("Tab", "\t", pinned = true),
    UiSymbolKey("//", "//", pinned = true),
) + listOf(
    "{", "}", "(", ")", ";", "=", ".", ",", "\"", "'", ":", "<", ">", "/", "*",
    "[", "]", "+", "-", "&", "|", "!", "?", "@", "#", "_", "%", "\\",
).map { UiSymbolKey(it, it) }

/** Insert [text] at the editor's caret (raw content insert — the Sora Content API moves no selection). */
internal fun insertAtCaret(editor: CodeEditor, text: String) {
    val c = editor.getCursor()
    val line = c.getLeftLine()
    val col = c.getLeftColumn()
    editor.getText().insert(line, col, text)
    editor.setSelection(line, col + text.length)
}

/**
 * CodeAssist `EditorSymbolBar` — the keyboard accessory row shown above the IME while typing on touch.
 * Keys insert at the caret via a raw pointer tap (NOT `clickable`, which would take focus and dismiss
 * the keyboard — the editor must keep its input connection).
 */
@Composable
internal fun EditorSymbolBar(
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier,
    symbols: List<UiSymbolKey> = DEFAULT_SYMBOL_KEYS,
) {
    val separator = MaterialTheme.colorScheme.outlineVariant
    val pinned = symbols.filter { it.pinned }
    val scrolling = symbols.filter { !it.pinned }
    Row(
        modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .drawBehind { drawLine(separator, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (key in pinned) SymbolKey(key.label, accent = key.label == "Tab", onClick = { onInsert(key.insert) })
        if (pinned.isNotEmpty()) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(separator))
        }
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (key in scrolling) SymbolKey(key.label, onClick = { onInsert(key.insert) })
        }
    }
}

@Composable
private fun SymbolKey(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Boolean = false) {
    Box(
        modifier
            .fillMaxHeight()
            .widthIn(min = 36.dp)
            .pointerInput(label) { detectTapGestures { onClick() } }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}