package com.aurastudio.editor.ui

import android.graphics.Typeface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.view.ContextThemeWrapper
import com.aurastudio.editor.core.EditorHost
import com.aurastudio.editor.language.editorLanguageFor
import com.aurastudio.editor.schemes.rememberAppColorScheme
import io.github.rosemoe.sora.text.LineSeparator
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

/**
 * Compose wrapper around the Sora [CodeEditor] (acs `CodeEditorView`): one instance per active file,
 * configured acs-style — app-themed scheme, 14sp font, LF line separator, 2dp divider, monospace,
 * highlighted current line/block, 4-space tabs. The scheme is re-applied whenever the app theme
 * (light/dark) changes by driving it off the remember'd [SchemeColors] in [editor.colorScheme].
 */
@Composable
internal fun EditorView(
    host: EditorHost,
    file: File,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val dividerPx = with(density) { 2.dp.toPx() }
    val scheme = rememberAppColorScheme()

    key(file.absolutePath) {
        AndroidView(
            factory = { ctx ->
                val themed = ContextThemeWrapper(ctx, androidx.appcompat.R.style.Theme_AppCompat_DayNight)
                CodeEditor(themed).also { editor ->
                    host.bind(file.absolutePath, editor)
                    editor.setText(host.textFor(file))
                    editor.setTypefaceText(Typeface.MONOSPACE)
                    editor.lineSeparator = LineSeparator.LF
                    editor.setTextSize(14f)
                    editor.tabWidth = 4
                    editor.isLineNumberEnabled = true
                    editor.isWordwrap = false
                    editor.isHighlightCurrentLine = true
                    editor.isHighlightCurrentBlock = true
                    editor.dividerWidth = dividerPx
                    editor.colorScheme = scheme
                    editor.setEditorLanguage(editorLanguageFor(file.name))
                }
            },
            update = { editor ->
                // Re-apply whenever the (theme-driven) scheme instance changes.
                editor.colorScheme = scheme
            },
            modifier = modifier.fillMaxSize(),
            onRelease = { host.unbindIf(file.absolutePath) }
        )
    }
}
