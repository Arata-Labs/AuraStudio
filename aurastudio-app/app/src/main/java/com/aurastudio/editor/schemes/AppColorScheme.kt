package com.aurastudio.editor.schemes

import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.aurastudio.ui.theme.LocalIsAppDark
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlin.math.max
import kotlin.math.min

/**
 * App-theme editor scheme — mirrors AuraStudio's Material You palette (dynamic light/dark).
 * Colors are derived from the active MaterialTheme so the editor always matches the surrounding UI,
 * with a luminance-based contrast pass so syntax stays readable on BOTH light and dark canvases
 * (M3's pastel tokens are high-contrast on dark but can wash out on light).
 */
internal class AppColorScheme(
    private val c: SchemeColors?,
) : EditorColorScheme() {

    override fun applyDefault() {
        super.applyDefault()

        // The EditorColorScheme base ctor already calls applyDefault() via virtual dispatch BEFORE this
        // instance's properties are assigned, so c is null on that pre-init pass — bail out to defaults.
        val colors = c ?: return

        // Surfaces
        setColor(WHOLE_BACKGROUND, colors.background)
        setColor(LINE_NUMBER_BACKGROUND, colors.background)
        setColor(LINE_NUMBER_PANEL, colors.background)
        setColor(LINE_NUMBER_PANEL_TEXT, colors.lineNumber)
        setColor(LINE_NUMBER, colors.lineNumber)
        setColor(LINE_NUMBER_CURRENT, colors.onSurface)
        setColor(LINE_DIVIDER, AndroidColor.TRANSPARENT)
        setColor(CURRENT_LINE, colors.currentLine)
        setColor(SELECTED_TEXT_BACKGROUND, colors.primaryContainer)
        setColor(MATCHED_TEXT_BACKGROUND, colors.primaryContainer)
        setColor(SELECTION_INSERT, colors.primary)
        setColor(SELECTION_HANDLE, colors.primary)

        // Text & chrome
        setColor(TEXT_NORMAL, colors.onSurface)
        setColor(TEXT_SELECTED, colors.onPrimaryContainer)
        setColor(UNDERLINE, colors.primary)
        setColor(BLOCK_LINE, colors.outline)
        setColor(BLOCK_LINE_CURRENT, colors.primary)
        setColor(SCROLL_BAR_THUMB, colors.outline)
        setColor(SCROLL_BAR_THUMB_PRESSED, colors.primary)
        setColor(SCROLL_BAR_TRACK, colors.currentLine)
        setColor(NON_PRINTABLE_CHAR, colors.outline)

        // Completion window
        setColor(COMPLETION_WND_BACKGROUND, colors.surfaceContainerHigh)
        setColor(COMPLETION_WND_CORNER, colors.outline)
        setColor(COMPLETION_WND_TEXT_PRIMARY, colors.onSurface)
        setColor(COMPLETION_WND_TEXT_SECONDARY, colors.onSurfaceVariant)
        setColor(COMPLETION_WND_ITEM_CURRENT, colors.primary)

        // Syntax
        setColor(KEYWORD, colors.keyword)
        setColor(OPERATOR, colors.operator)
        setColor(LITERAL, colors.literal)
        setColor(TYPE_NAME, colors.typeName)
        setColor(ANNOTATION, colors.annotation)
        setColor(FIELD, colors.field)
        setColor(FUNCTION_NAME, colors.function)
        setColor(IDENTIFIER_NAME, colors.onSurface)
        setColor(IDENTIFIER_VAR, colors.field)
        setColor(XML_TAG, colors.xmlTag)
        setColor(COMMENT, colors.comment)
        setColor(TODO_COMMENT, colors.annotation)
        setColor(FIXME_COMMENT, colors.error)

        // Problems
        setColor(PROBLEM_ERROR, colors.error)
        setColor(PROBLEM_WARNING, colors.warning)
        setColor(PROBLEM_TYPO, colors.warning)
    }

    override fun isDark(): Boolean = c?.dark ?: false

    private companion object {
        // Custom color ids (after EditorColorScheme.END_COLOR_ID) — scheme-internal.
        const val COMPLETION_WND_BG_CURRENT_ITEM = 67
        const val COMPLETION_WND_TEXT_LABEL = 68
        const val COMPLETION_WND_TEXT_TYPE = 69
        const val COMPLETION_WND_TEXT_API = 70
        const val COMPLETION_WND_TEXT_DETAIL = 71
        const val LOG_TEXT_INFO = 72
        const val LOG_TEXT_DEBUG = 73
        const val LOG_TEXT_VERBOSE = 74
        const val LOG_TEXT_ERROR = 75
        const val LOG_TEXT_WARNING = 76
        const val XML_TAG = 77
        const val FIELD = 78
        const val TYPE_NAME = 79
        const val TODO_COMMENT = 80
        const val FIXME_COMMENT = 81
    }
}

/** AuraStudio Material You surface + syntax colors for the active theme. */
internal data class SchemeColors(
    val background: Int,
    val currentLine: Int,
    val surfaceContainerHigh: Int,
    val primary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondary: Int,
    val tertiary: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val lineNumber: Int,
    val outline: Int,
    val error: Int,
    val keyword: Int,
    val operator: Int,
    val literal: Int,
    val typeName: Int,
    val annotation: Int,
    val field: Int,
    val function: Int,
    val xmlTag: Int,
    val comment: Int,
    val warning: Int,
    val dark: Boolean,
)

/** Builds the scheme from the active Material Theme (handles dynamic color + light/dark). */
@Composable
internal fun rememberAppColorScheme(): AppColorScheme {
    val cs = MaterialTheme.colorScheme
    val dark = LocalIsAppDark.current
    return remember(cs, dark) {
        // App editor-bg token so the canvas blends with the surrounding editor strip.
        val bg = Color(if (dark) 0xFF1B1C1F else 0xFFFAF9F6)
        // Current-line highlight: surfaceContainerHigh on dark; a soft warm tint on light so it doesn't
        // clash with the warm white canvas.
        val currentLine = if (dark) cs.surfaceContainerHigh else Color(0xFFF0EDE7)
        // Readable() guarantees ≥2.6:1 contrast against the canvas in light mode; dark keeps M3 tokens.
        val onLight = !dark
        fun boost(color: Color): Int {
            if (!onLight) return color.toArgb()
            return readable(color, bg)
        }

        AppColorScheme(
            SchemeColors(
                background = bg.toArgb(),
                currentLine = currentLine.toArgb(),
                surfaceContainerHigh = cs.surfaceContainerHigh.toArgb(),
                primary = cs.primary.toArgb(),
                primaryContainer = cs.primaryContainer.toArgb(),
                onPrimaryContainer = cs.onPrimaryContainer.toArgb(),
                secondary = cs.secondary.toArgb(),
                tertiary = cs.tertiary.toArgb(),
                onSurface = cs.onSurface.toArgb(),
                onSurfaceVariant = cs.onSurfaceVariant.toArgb(),
                lineNumber = cs.outline.toArgb(),
                outline = cs.outline.toArgb(),
                error = cs.error.toArgb(),
                keyword = boost(cs.primary),
                operator = boost(cs.onSurfaceVariant),
                literal = boost(cs.tertiary),
                typeName = boost(cs.tertiary),
                annotation = boost(cs.secondary),
                field = boost(cs.onSurfaceVariant),
                function = boost(cs.primary),
                xmlTag = boost(cs.primary),
                comment = commentColor(cs.outline, dark, bg),
                warning = boost(cs.tertiary),
                dark = dark,
            )
        )
    }
}

/** Darkens [color] (keeping its hue) until it has ≥ [minContrast]:1 against [bg]. */
private fun readable(color: Color, bg: Color, minContrast: Float = 2.6f): Int {
    var c = color
    val bgL = bg.luminance()
    repeat(8) {
        if (contrast(c.luminance(), bgL) >= minContrast) return c.toArgb()
        c = Color(
            red = c.red * 0.70f,
            green = c.green * 0.70f,
            blue = c.blue * 0.70f,
        )
    }
    return c.toArgb()
}

private fun contrast(l1: Float, l2: Float): Float {
    val a = max(l1, l2)
    val b = min(l1, l2)
    return (a + 0.05f) / (b + 0.05f)
}

/** Comment is lower-emphasis: on dark use M3 outline; on light darken for legibility. */
private fun commentColor(outline: Color, dark: Boolean, bg: Color): Int =
    if (dark) outline.toArgb() else readable(outline.copy(alpha = 1f), bg, 2.2f)
