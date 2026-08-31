package com.aurastudio.editor.text

import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import java.io.File

/**
 * Editor diagnostics: converts compiler output (gradle / kotlin / javac) into per-file sora
 * [DiagnosticRegion]s. Mirrors acs `Diagnostics.kt` `asDiagnosticRegion` (LSP → sora regions)
 * and `CodeEditorView.startDiagnosticAnalysis`, minus the language-server round-trip.
 */

/** Maps a 1-based line/column in [content] to an absolute character offset. */
internal fun lineColumnToIndex(content: CharSequence, line: Int, column: Int): Int {
    if (line < 1) return 0
    var offset = 0
    var current = 1
    while (current < line && offset < content.length) {
        val c = content[offset]
        offset++
        if (c == '\n') current++
    }
    return (offset + (column - 1).coerceAtLeast(0)).coerceIn(0, content.length)
}

/** Parses one gradle/kotlin compiler diagnostic line. Returns null if not diagnostic. */
private fun parseLine(line: String): ParsedDiagnostic? {
    val kotlin = KOTLIN_DIAGNOSTIC.find(line) ?: return null
    return ParsedDiagnostic(
        filePath = kotlin.groupValues[1],
        line = kotlin.groupValues[2].toIntOrNull() ?: 1,
        column = kotlin.groupValues[3].toIntOrNull() ?: 1,
        severity = if (kotlin.groupValues[4] == "error") DiagnosticRegion.SEVERITY_ERROR
        else DiagnosticRegion.SEVERITY_WARNING,
        message = kotlin.groupValues[5].trim()
    )
}

private data class ParsedDiagnostic(
    val filePath: String,
    val line: Int,
    val column: Int,
    val severity: Short,
    val message: String
)

private val KOTLIN_DIAGNOSTIC = Regex(
    """^\s*(?:e:|w:)?\s*([^:]+\.(?:kt|java|kts|xml|gradle|properties)): \((\d+), (\d+)\): (error|warning): (.+)$"""
)

/**
 * Converts compiler output into per-file [DiagnosticRegion]s keyed by absolute path.
 * Diagnostics for files outside [projectRoot] are skipped.
 */
internal fun buildDiagnostics(
    projectRoot: File,
    output: List<String>
): Map<String, List<DiagnosticRegion>> {
    val root = projectRoot.absolutePath
    val result = LinkedHashMap<String, MutableList<DiagnosticRegion>>()
    val contentCache = HashMap<String, String>()
    for (line in output) {
        val parsed = parseLine(line) ?: continue
        if (parsed.severity == DiagnosticRegion.SEVERITY_NONE) continue
        val resolved = resolvePath(root, parsed.filePath) ?: continue
        val content = contentCache.getOrPut(resolved) {
            runCatching { File(resolved).readText() }.getOrDefault("")
        }
        val start = lineColumnToIndex(content, parsed.line, parsed.column)
        val end = (lineColumnToIndex(content, parsed.line, parsed.column + 10))
            .coerceAtLeast(start + 1)
        val list = result.getOrPut(resolved) { ArrayList() }
        list.add(DiagnosticRegion(start, end, parsed.severity))
    }
    return result
}

private fun resolvePath(root: String, filePath: String): String? {
    val f = File(filePath)
    return if (f.isAbsolute) {
        if (f.absolutePath.startsWith(root)) f.absolutePath else null
    } else {
        root + File.separator + filePath
    }
}