package com.hinohara.aurastudio.terminal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration

// Standard ANSI 16-color palette
private val ANSI_COLORS = arrayOf(
    Color(0xFF1E1E2E), // Black
    Color(0xFFCC3333), // Red
    Color(0xFF33CC33), // Green
    Color(0xFFCCCC33), // Yellow
    Color(0xFF3333CC), // Blue
    Color(0xFFCC33CC), // Magenta
    Color(0xFF33CCCC), // Cyan
    Color(0xFFCCCCCC), // White
    Color(0xFF555555), // Bright Black
    Color(0xFF555555), // Bright Red
    Color(0xFF55CC55), // Bright Green
    Color(0xFFCCCC55), // Bright Yellow
    Color(0xFF5555CC), // Bright Blue
    Color(0xFFCC55CC), // Bright Magenta
    Color(0xFF55CCCC), // Bright Cyan
    Color(0xFFAAAAAA), // Bright White
)

object AnsiParser {
    fun parse(text: String): AnnotatedString {
        return buildAnnotatedString {
            var i = 0
            var fg: Color? = null
            var bg: Color? = null
            var bold = false
            var underline = false
            var italic = false
            var dim = false
            var strikethrough = false

            while (i < text.length) {
                val c = text[i]

                if (c == '\u001B' && i + 1 < text.length && text[i + 1] == '[') {
                    // Parse CSI sequence
                    var j = i + 2
                    val params = StringBuilder()
                    while (j < text.length && text[j] in '0'..'9' || text[j] == ';') {
                        params.append(text[j])
                        j++
                    }
                    if (j < text.length) {
                        val command = text[j]
                        val paramStr = params.toString()
                        val codes = if (paramStr.isEmpty()) {
                            intArrayOf()
                        } else {
                            paramStr.split(';').mapNotNull { it.toIntOrNull() }.toIntArray()
                        }

                        when (command) {
                            'm' -> {
                                // SGR - Select Graphic Rendition
                                if (codes.isEmpty()) {
                                    // Reset
                                    fg = null; bg = null; bold = false; underline = false
                                    italic = false; dim = false; strikethrough = false
                                } else {
                                    var ci = 0
                                    while (ci < codes.size) {
                                        val code = codes[ci]
                                        when (code) {
                                            0 -> {
                                                fg = null; bg = null; bold = false; underline = false
                                                italic = false; dim = false; strikethrough = false
                                            }
                                            1 -> bold = true
                                            2 -> dim = true
                                            3 -> italic = true
                                            4 -> underline = true
                                            9 -> strikethrough = true
                                            22 -> { bold = false; dim = false }
                                            23 -> italic = false
                                            24 -> underline = false
                                            29 -> strikethrough = false
                                            in 30..37 -> fg = ANSI_COLORS[code - 30]
                                            38 -> {
                                                // Extended foreground
                                                if (ci + 1 < codes.size) {
                                                    when (codes[ci + 1]) {
                                                        5 -> {
                                                            if (ci + 2 < codes.size) {
                                                                fg = ansi256ToColor(codes[ci + 2])
                                                                ci += 2
                                                            }
                                                        }
                                                        2 -> {
                                                            if (ci + 4 < codes.size) {
                                                                fg = Color(
                                                                    codes[ci + 2],
                                                                    codes[ci + 3],
                                                                    codes[ci + 4]
                                                                )
                                                                ci += 4
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            39 -> fg = null
                                            in 40..47 -> bg = ANSI_COLORS[code - 40]
                                            48 -> {
                                                // Extended background
                                                if (ci + 1 < codes.size) {
                                                    when (codes[ci + 1]) {
                                                        5 -> {
                                                            if (ci + 2 < codes.size) {
                                                                bg = ansi256ToColor(codes[ci + 2])
                                                                ci += 2
                                                            }
                                                        }
                                                        2 -> {
                                                            if (ci + 4 < codes.size) {
                                                                bg = Color(
                                                                    codes[ci + 2],
                                                                    codes[ci + 3],
                                                                    codes[ci + 4]
                                                                )
                                                                ci += 4
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            49 -> bg = null
                                            in 90..97 -> fg = ANSI_COLORS[code - 90 + 8]
                                            in 100..107 -> bg = ANSI_COLORS[code - 100 + 8]
                                        }
                                        ci++
                                    }
                                }
                            }
                            'H', 'f' -> {
                                // Cursor position - skip (clear to end of line)
                            }
                            'J' -> {
                                // Erase display - skip
                            }
                            'K' -> {
                                // Erase line - skip
                            }
                        }
                        i = j + 1
                    } else {
                        i++
                    }
                } else if (c == '\r') {
                    i++
                } else {
                    // Regular character
                    val start = i
                    while (i < text.length && text[i] != '\u001B' && text[i] != '\r') {
                        i++
                    }
                    val span = text.substring(start, i)
                    pushStyle(SpanStyle(
                        color = fg ?: Color.Unspecified,
                        background = bg ?: Color.Unspecified,
                        fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null,
                        fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else null,
                        textDecoration = buildTextDecoration(underline, strikethrough)
                    ))
                    append(span)
                    pop()
                }
            }
        }
    }

    private fun ansi256ToColor(code: Int): Color {
        if (code < 16) return ANSI_COLORS[code]
        if (code < 232) {
            val c = code - 16
            val r = c / 36
            val g = (c / 6) % 6
            val b = c % 6
            return Color(
                if (r == 0) 0 else 55 + r * 40,
                if (g == 0) 0 else 55 + g * 40,
                if (b == 0) 0 else 55 + b * 40
            )
        }
        val gray = 8 + (code - 232) * 10
        return Color(gray, gray, gray)
    }

    private fun buildTextDecoration(underline: Boolean, strikethrough: Boolean): TextDecoration? {
        val decorations = mutableListOf<TextDecoration>()
        if (underline) decorations.add(TextDecoration.Underline)
        if (strikethrough) decorations.add(TextDecoration.LineThrough)
        return if (decorations.isEmpty()) null else TextDecoration.combine(decorations)
    }
}
