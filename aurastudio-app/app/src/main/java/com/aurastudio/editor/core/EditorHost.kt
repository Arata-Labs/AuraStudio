package com.aurastudio.editor.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

/**
 * Document store + active-editor tracker (acs `FileManager` (core/projects) + `EditorViewModel`):
 * keeps an in-memory buffer per open file so unsaved edits survive tab switches, detects dirty state,
 * drives acs-style auto-save (2 s loop), and pushes build-output diagnostics into editors.
 */
internal class EditorHost {
    private data class ActiveEditor(val path: String, val editor: CodeEditor)

    val buffers = mutableMapOf<String, String>()

    /** Snapshot of on-disk content, used to detect unsaved changes cheaply. */
    private val diskCache = mutableMapOf<String, String>()

    /** Last diagnostics parsed from build output, keyed by absolute path. */
    private val diagnostics = mutableMapOf<String, List<DiagnosticRegion>>()

    /** Bump on any save / buffer change so Compose recomputes dirty flags. */
    var revision by mutableIntStateOf(0)
        private set

    private var active: ActiveEditor? = null

    val currentEditor: CodeEditor? get() = active?.editor
    val currentPath: String? get() = active?.path

    fun canUndo(): Boolean = currentEditor?.canUndo() == true
    fun canRedo(): Boolean = currentEditor?.canRedo() == true

    fun undo() {
        runCatching { currentEditor?.undo() }.onSuccess { revision++ }
    }

    fun redo() {
        runCatching { currentEditor?.redo() }.onSuccess { revision++ }
    }

    /** Bump the revision so Compose re-evaluates dirty/undo/redo flags. */
    fun ping() {
        revision++
    }

    fun textFor(file: File): String {
        val path = file.absolutePath
        if (path !in diskCache) {
            diskCache[path] = runCatching { file.readText() }.getOrDefault("")
        }
        return buffers[path] ?: diskCache[path] ?: ""
    }

    /** True when the buffer/live text differs from what is saved on disk. */
    fun isDirty(file: File): Boolean {
        val path = file.absolutePath
        val live = if (active?.path == path) active?.editor?.text?.toString() else null
        val current = live ?: buffers[path] ?: diskCache[path] ?: ""
        val disk = diskCache[path] ?: runCatching { file.readText() }.getOrDefault("")
        return current != disk
    }

    fun fileText(file: File): String {
        val path = file.absolutePath
        return if (active?.path == path) (active?.editor?.text?.toString() ?: "")
        else buffers[path] ?: diskCache[path] ?: ""
    }

    /** Attaches the editor that is about to be shown; captures any previous one. */
    fun bind(path: String, editor: CodeEditor) {
        active?.let { buffers[it.path] = it.editor.text.toString() }
        active = ActiveEditor(path, editor)
        diagnostics[path]?.let { regions -> applyTo(editor, regions) }
    }

    fun unbindIf(path: String) {
        if (active?.path == path) {
            active?.let { buffers[it.path] = it.editor.text.toString() }
            active = null
        }
    }

    fun discard(path: String) {
        buffers.remove(path)
        diskCache.remove(path)
        diagnostics.remove(path)
        revision++
    }

    /** Persists the visible editor's text to disk and refreshes the buffer. */
    fun saveIfActive(file: File): Boolean {
        val a = active ?: return false
        if (a.path != file.absolutePath) return false
        return save(file)
    }

    /** Persists any buffer/live text for [file] regardless of tab visibility (auto-save). */
    fun save(file: File): Boolean {
        val path = file.absolutePath
        val text = fileText(file)
        val disk = diskCache[path] ?: runCatching { file.readText() }.getOrDefault("")
        if (text == disk) return false
        return runCatching { file.writeText(text) }.isSuccess.also {
            if (it) {
                diskCache[path] = text
                buffers[path] = text
                revision++
            }
        }
    }

    /** Pushes build-derived diagnostics into the open/active editor for [path]. */
    fun setDiagnostics(path: String, regions: List<DiagnosticRegion>) {
        diagnostics[path] = regions
        val a = active
        if (a?.path == path) applyTo(a.editor, regions)
        revision++
    }

    private fun applyTo(editor: CodeEditor, regions: List<DiagnosticRegion>) {
        val container = DiagnosticsContainer()
        regions.forEach { container.addDiagnostic(it) }
        editor.setDiagnostics(container)
    }
}