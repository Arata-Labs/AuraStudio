package com.aurastudio.ui.screens.project

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurastudio.R
import com.aurastudio.data.terminal.ProjectTerminal
import com.aurastudio.editor.core.EditorHost
import com.aurastudio.editor.text.buildDiagnostics
import com.aurastudio.editor.ui.EditorView
import com.aurastudio.filetree.FileTreePanel
import com.aurastudio.filetree.models.FileIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private val TEXT_EXTENSIONS = setOf(
    "txt", "kt", "java", "xml", "gradle", "kts", "properties", "sh", "c", "cpp",
    "h", "hpp", "md", "json", "yml", "yaml", "toml", "py", "ts", "js", "css", "html",
    "pro", "cfg", "conf", "ini", "bat", "cmd", "makefile", "gitignore", "editorconfig",
    "bat", "groovy", "rs", "go", "rb", "php", "sql", "csv", "log", "aapt2", "bat",
    "cmake", "mk", "pro", "gitattributes", "env", "sh", "zsh",
)

/** A pending bulk tab-close whose targets contain unsaved files (prompt before discarding). */
private sealed interface BulkClose {
    data object All : BulkClose
    data class Others(val keep: File) : BulkClose
}

// Persisted auto-save preference (persist across project open/close & app restarts).
private const val PREFS_NAME = "aurastudio_settings"
private const val KEY_AUTO_SAVE = "auto_save"

private fun isTextFile(file: File): Boolean {
    if (file.length() > 2_097_152) return false
    val name = file.name
    if (name.startsWith(".")) return true
    val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    if (ext.isNotEmpty()) return ext in TEXT_EXTENSIONS
    val lower = name.lowercase(Locale.ROOT)
    return lower in setOf(
        "makefile", "dockerfile", "license", "readme", "changelog", "authors",
        "vagrantfile", "gemfile", "rakefile", "procfile", "guardfile", "justfile",
    )
}

/** acs `UniqueNameBuilder` port (JetBrains trie): minimal unambiguous relative path per file. */
private class UniqueNameBuilder {
    private class TrieNode {
        var count = 0
        val children = HashMap<String, TrieNode>()
    }

    private val root = TrieNode()

    fun addPath(key: String, path: String) {
        var node = root
        node.count++
        for (part in path.split('/')) {
            val child = node.children.getOrPut(part) { TrieNode() }
            child.count++
            node = child
        }
    }

    fun getShortPath(key: String): String {
        var node = root
        var result = ""
        var divider = ""
        for (part in key.split('/')) {
            node = node.children[part] ?: return result
            if (node.count == 1) {
                result += part
                return result
            }
            result += divider + part
            divider = "/"
        }
        return result
    }
}

/**
 * CodeAssist `EditorScreen`/`CompactLayout` port (phone): a standalone project editor screen with a
 * left PushDrawer (file tree with search), a floating pill top bar + tabs strip + breadcrumb + code
 * editor, and a bottom [ProjectDock] whose collapsed face is a single Console pill. Swiping it up (or
 * tapping the top-bar console menu) reveals the Build Console sheet (Build Output / Problems / App
 * Logs / Terminal). The soft keyboard hides the dock to make room for the symbol bar.
 */
@Composable
fun ProjectScreen(
    scaffoldPadding: PaddingValues,
    projectDir: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val root = remember(projectDir) { File(projectDir) }
    val host = remember { EditorHost() }
    val buildScope = rememberCoroutineScope()
    val buildState = remember(projectDir) { BuildState(context, projectDir, buildScope) }
    val workspace = remember(projectDir) { ProjectTerminal.workspace(projectDir, context) }
    val openedFiles = remember { mutableStateListOf<File>() }

    var filesOpen by rememberSaveable { mutableStateOf(false) }
    var dockOpen by rememberSaveable { mutableStateOf(false) }
    var autoSave by rememberSaveable {
        mutableStateOf(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_SAVE, false)
        )
    }
    // Hoisted so closing and reopening the dock restores the last active console tab.
    var consoleTab by remember(projectDir) { mutableStateOf(ConsoleTab.BUILD) }
    var activeFile by remember { mutableStateOf<File?>(null) }
    // Drawer open-fraction mirror for the top-bar sidebar icon (0 closed → 1 open).
    var navProgress by remember { mutableFloatStateOf(0f) }
    // Dialogs: single dirty-file close, bulk dirty-file close, close-project confirm.
    var closeTarget by remember { mutableStateOf<File?>(null) }
    var pendingBulkClose by remember { mutableStateOf<BulkClose?>(null) }
    var confirmCloseProject by remember { mutableStateOf(false) }
    // Notice shown when the user tries to open a file whose type can't be edited.
    var unsupportedFile by remember { mutableStateOf<File?>(null) }
    // Soft keyboard state — reads the raw IME inset (not via a consuming modifier, so scaffold padding
    // doesn't zero it).
    val keyboardOpen = WindowInsets.ime.getBottom(density) > 0

    // Poll editor state so dirty-dot / save / undo / redo stay fresh without hooking Sora's event pipeline.
    LaunchedEffect(activeFile) {
        while (true) {
            delay(600)
            host.ping()
        }
    }

    // acs auto-save: every 2s persist every open buffer whose content differs from disk.
    LaunchedEffect(projectDir, autoSave) {
        if (!autoSave) return@LaunchedEffect
        while (true) {
            delay(2000)
            openedFiles.forEach { host.save(it) }
        }
    }

    // Parse compiler output (gradle/kotlin) into per-file diagnostics once a build settles.
    LaunchedEffect(buildState.exitCode, buildState.output.size) {
        if (buildState.isRunning) return@LaunchedEffect
        val diags = buildDiagnostics(root, buildState.output)
        openedFiles.forEach { file ->
            host.setDiagnostics(file.absolutePath, diags[file.absolutePath] ?: emptyList())
        }
    }

    fun openFile(file: File) {
        if (!isTextFile(file)) {
            unsupportedFile = file
            return
        }
        if (openedFiles.none { it.absolutePath == file.absolutePath }) {
            openedFiles.add(file)
        }
        activeFile = file
    }

    fun closeFile(file: File) {
        val idx = openedFiles.indexOf(file)
        openedFiles.remove(file)
        host.discard(file.absolutePath)
        if (file.absolutePath == activeFile?.absolutePath) {
            activeFile = if (idx < openedFiles.size) openedFiles.getOrNull(idx) else openedFiles.lastOrNull()
        }
    }

    fun closeOthers(file: File) {
        val keep = file.absolutePath
        openedFiles.toList().forEach {
            if (it.absolutePath != keep) closeFile(it)
        }
        activeFile = file
    }

    fun closeAll() {
        openedFiles.toList().forEach { closeFile(it) }
        activeFile = null
    }

    /** Closes [file], prompting with a discard/save/cancel dialog when it has unsaved changes. */
    fun requestClose(file: File) {
        if (host.isDirty(file)) closeTarget = file else closeFile(file)
    }

    /** Close-others, prompting once when any of the to-be-closed files are dirty. */
    fun requestCloseOthers(file: File) {
        val others = openedFiles.filter { it.absolutePath != file.absolutePath }
        if (others.any { host.isDirty(it) }) pendingBulkClose = BulkClose.Others(file) else closeOthers(file)
    }

    /** Close-all, prompting once when any open file is dirty. */
    fun requestCloseAll() {
        if (openedFiles.any { host.isDirty(it) }) pendingBulkClose = BulkClose.All else closeAll()
    }

    /** Saves every dirty open buffer (used by close-project and the dirty-close dialogs). */
    fun saveAllOpen() {
        openedFiles.toList().forEach { host.save(it) }
    }

    DisposableEffect(projectDir) {
        onDispose {
            buildState.dispose()
            ProjectTerminal.shutdown(projectDir)
        }
    }

    // CodeAssist back routing: a dialog/sheet closes before the screen pops; here the drawer, then
    // the dock, then the app-level close-project confirmation.
    BackHandler {
        when {
            filesOpen -> filesOpen = false
            dockOpen -> dockOpen = false
            else -> confirmCloseProject = true
        }
    }

    // Drop the Scaffold's bottom inset here so the dock (like a bottom bar) owns the area all the way to
    // the bottom edge — no leftover surface-colored strip behind the pill.
    val projectPadding = PaddingValues(
        start = scaffoldPadding.calculateStartPadding(LocalLayoutDirection.current),
        top = scaffoldPadding.calculateTopPadding(),
        end = scaffoldPadding.calculateEndPadding(LocalLayoutDirection.current),
        bottom = 0.dp,
    )
    Box(
        Modifier
            .fillMaxSize()
            .padding(projectPadding),
    ) {
        PushDrawer(
            open = filesOpen,
            onOpenChange = { filesOpen = it },
            gesturesEnabled = true,
            onProgress = { navProgress = it },
            drawerContent = {
                FileTreePanel(
                    root = root,
                    selected = activeFile,
                    onOpenFile = { openFile(it); filesOpen = false },
                    modifier = Modifier.fillMaxSize(),
                )
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier
                    .fillMaxSize()
                    .background(editorBg()),
                ) {
                    Column(Modifier.weight(1f).fillMaxWidth()) {
                        // Recompute undo/redo/dirty live: host.revision ticks every 600ms poll after
                        // edits, and this read keeps the toolbar reacting without switching views.
                        val liveCanUndo = remember(host.revision, activeFile?.absolutePath) { host.canUndo() }
                        val liveCanRedo = remember(host.revision, activeFile?.absolutePath) { host.canRedo() }
                        val liveDirty = remember(host.revision, activeFile?.absolutePath) {
                            activeFile?.let { host.isDirty(it) } == true
                        }
                        EditorTopBar(
                            projectName = root.name,
                            navFraction = navProgress,
                            onToggleFiles = { filesOpen = !filesOpen },
                            hasActiveFile = activeFile != null,
                            canUndo = liveCanUndo,
                            canRedo = liveCanRedo,
                            onUndo = { host.undo() },
                            onRedo = { host.redo() },
                            dirty = liveDirty,
                            onSave = { activeFile?.let { host.saveIfActive(it) } },
                            onOpenConsole = { dockOpen = true },
                            onCloseProject = { confirmCloseProject = true },
                            autoSave = autoSave,
                            onToggleAutoSave = {
                                autoSave = !autoSave
                                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                    .edit().putBoolean(KEY_AUTO_SAVE, autoSave).apply()
                            },
                        )
                        TabsStrip(
                            openFiles = openedFiles.toList(),
                            active = activeFile,
                            root = root,
                            isDirty = { file -> host.isDirty(file) },
                            onSelect = { activeFile = it },
                            onClose = ::requestClose,
                            onCloseOthers = ::requestCloseOthers,
                            onCloseAll = ::requestCloseAll,
                        )
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        activeFile?.let { file ->
                            BreadcrumbBar(root = root, file = file)
                        }
                        Box(Modifier.fillMaxWidth().weight(1f)) {
                            val file = activeFile
                            if (file != null) {
                                EditorView(host = host, file = file, modifier = Modifier.fillMaxSize())
                            } else {
                                Text(
                                    stringResource(R.string.project_select_file),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                )
                            }
                        }
                    }
                    // CodeAssist: while the keyboard is up the coding-symbol accessory bar owns the bottom
                    // slot; otherwise a spacer reserves the dock's collapsed-bar height so the editor column
                    // isn't hidden behind it.
                    if (keyboardOpen && activeFile != null) {
                        EditorSymbolBar(
                            symbols = DEFAULT_SYMBOL_KEYS,
                            onInsert = { text -> host.currentEditor?.let { insertAtCaret(it, text) } },
                        )
                    } // else if (!keyboardOpen) {
                    //     Spacer(Modifier.height(DockBarHeight))
                    // }
                }
                // The dock is the collapsed face of the sheet: swipe it up (or tap the console pill /
                // top-bar console menu) and it expands into the Build Console (Output/Problems/Logs/Terminal).
                ProjectDock(
                    open = dockOpen,
                    onOpenChange = { dockOpen = it },
                    isRunning = buildState.isRunning,
                    exitCode = buildState.exitCode,
                    hidden = keyboardOpen && !dockOpen,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 2.dp),
                    bar = {
                        val problems = buildProblems(buildState.output)
                        ConsoleDockBar(
                            running = buildState.isRunning,
                            exitCode = buildState.exitCode,
                            errors = problems.count { it.level == LineLevel.Error },
                            warnings = problems.count { it.level == LineLevel.Warn },
                            terminalSessions = workspace.sessions.size,
                            onTap = { dockOpen = true },
                        )
                    },
                ) {
                    BuildConsolePanel(
                        buildState = buildState,
                        projectDir = projectDir,
                        selected = consoleTab,
                        onTabSelected = { consoleTab = it },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }

            // --- Close-a-dirty-file dialog (Discard / Save & close / Cancel) ---
            closeTarget?.let { file ->
                DirtyCloseDialog(
                    file = file,
                    onDiscard = {
                        closeTarget = null
                        closeFile(file)
                    },
                    onSaveAndClose = {
                        closeTarget = null
                        host.save(file)
                        closeFile(file)
                    },
                    onCancel = { closeTarget = null },
                )
            }

            // --- Notice: file type can't be opened in the editor ---
            unsupportedFile?.let { file ->
                AlertDialog(
                    onDismissRequest = { unsupportedFile = null },
                    title = { Text(stringResource(R.string.project_unsupported_title)) },
                    text = { Text(stringResource(R.string.project_unsupported_message, file.name)) },
                    confirmButton = {
                        TextButton(onClick = { unsupportedFile = null }) {
                            Text(stringResource(R.string.project_ok))
                        }
                    },
                )
            }

            // --- Bulk close (others/all) with unsaved files ---
            pendingBulkClose?.let { bulk ->
                val dirtyCount = when (bulk) {
                    is BulkClose.All -> openedFiles.count { host.isDirty(it) }
                    is BulkClose.Others -> openedFiles.count {
                        it.absolutePath != bulk.keep.absolutePath && host.isDirty(it)
                    }
                }
                DirtyBulkCloseDialog(
                    count = dirtyCount,
                    onDiscard = {
                        pendingBulkClose = null
                        when (bulk) {
                            is BulkClose.All -> closeAll()
                            is BulkClose.Others -> closeOthers(bulk.keep)
                        }
                    },
                    onSaveAndClose = {
                        pendingBulkClose = null
                        saveAllOpen()
                        when (bulk) {
                            is BulkClose.All -> closeAll()
                            is BulkClose.Others -> closeOthers(bulk.keep)
                        }
                    },
                    onCancel = { pendingBulkClose = null },
                )
            }

            // --- Close-project confirmation ---
            if (confirmCloseProject) {
                AlertDialog(
                    onDismissRequest = { confirmCloseProject = false },
                    title = { Text(stringResource(R.string.project_close_project)) },
                    text = { Text(stringResource(R.string.project_close_project_message)) },
                    confirmButton = {
                        Button(onClick = {
                            confirmCloseProject = false
                            saveAllOpen()
                            onBack()
                        }) {
                            Text(stringResource(R.string.project_close))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmCloseProject = false }) {
                            Text(stringResource(R.string.project_cancel))
                        }
                    },
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Editor top bar                                                      */
/* ------------------------------------------------------------------ */

/**
 * CodeAssist `EditorTopBar` (compact) restyled as a floating pill: `SidebarToggle → project name
 * (weight 1f) → Save (accent when dirty) → Undo → Redo (dimmed when unavailable) → Console menu`.
 * The menu holds Build Console and Close project (with confirmation handled by the screen).
 */
@Composable
private fun EditorTopBar(
    projectName: String,
    navFraction: Float,
    onToggleFiles: () -> Unit,
    hasActiveFile: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    dirty: Boolean,
    onSave: () -> Unit,
    onOpenConsole: () -> Unit,
    onCloseProject: () -> Unit,
    autoSave: Boolean,
    onToggleAutoSave: () -> Unit,
) {
    var overflowOpen by remember { mutableStateOf(false) }
    val disabledTint = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Surface(
            shape = RoundedCornerShape(ProjectRadius.pill),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SidebarToggleButton(navFraction = navFraction, onClick = onToggleFiles)
                Text(
                    projectName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButtonCa(
                    Icons.Filled.Save,
                    stringResource(R.string.project_save),
                    onClick = onSave,
                    tint = if (dirty) MaterialTheme.colorScheme.primary else disabledTint,
                    boxSize = 30,
                    iconSize = 18,
                )
                IconButtonCa(
                    Icons.Filled.Undo,
                    stringResource(R.string.project_undo),
                    onClick = onUndo,
                    tint = if (hasActiveFile && canUndo) MaterialTheme.colorScheme.onSurface else disabledTint,
                    boxSize = 30,
                    iconSize = 18,
                )
                IconButtonCa(
                    Icons.Filled.Redo,
                    stringResource(R.string.project_redo),
                    onClick = onRedo,
                    tint = if (hasActiveFile && canRedo) MaterialTheme.colorScheme.onSurface else disabledTint,
                    boxSize = 30,
                    iconSize = 18,
                )
                Box {
                    IconButtonCa(
                        Icons.Filled.MoreVert,
                        stringResource(R.string.project_more_actions),
                        onClick = { overflowOpen = true },
                        boxSize = 30,
                        iconSize = 18,
                    )
                    CaDropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        CaMenuItem(
                            text = stringResource(R.string.project_build_console),
                            icon = Icons.Filled.Build,
                            onClick = { overflowOpen = false; onOpenConsole() },
                        )
                        CaMenuItem(
                            text = stringResource(R.string.project_auto_save),
                            icon = Icons.Filled.Check,
                            tint = if (autoSave) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0f),
                            onClick = { overflowOpen = false; onToggleAutoSave() },
                        )
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        CaMenuItem(
                            text = stringResource(R.string.project_close_project),
                            icon = Icons.Filled.Close,
                            tint = MaterialTheme.colorScheme.error,
                            textColor = MaterialTheme.colorScheme.error,
                            onClick = { overflowOpen = false; onCloseProject() },
                        )
                    }
                }
            }
        }
    }
}

/**
 * CodeAssist `SidebarToggleButton` — the file-tree sidebar toggle. A `ViewAgenda` glyph (a compact
 * panel of rows) that hints at the project-files list; [navFraction] is retained for API stability.
 */
@Composable
private fun SidebarToggleButton(navFraction: Float, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .size(34.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.ViewAgenda,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
    }
}

/* ------------------------------------------------------------------ */
/* Tabs strip                                                          */
/* ------------------------------------------------------------------ */

/**
 * CodeAssist `TabsStrip` — a 40dp editorBg row of pills (secondaryContainer when active, radius 14,
 * 14dp horizontal padding), each with a file-type icon, display name (unique short path for duplicate
 * names, a `*` prefix when modified), and close affordance. Long-press a tab for Close / Close others /
 * Close all. An overflow `⌄` appears once tabs overflow the row.
 */
@Composable
private fun TabsStrip(
    openFiles: List<File>,
    active: File?,
    root: File,
    isDirty: (File) -> Boolean,
    onSelect: (File) -> Unit,
    onClose: (File) -> Unit,
    onCloseOthers: (File) -> Unit,
    onCloseAll: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var contextFile by remember { mutableStateOf<File?>(null) }
    var overflowOpen by remember { mutableStateOf(false) }

    // acs `updateTabs`: duplicate basenames get a minimal unique relative path; dirty → `*` prefix.
    val labels = remember(openFiles) {
        val countByName = openFiles.groupingBy { it.name }.eachCount()
        val builder = UniqueNameBuilder()
        openFiles.forEach { file ->
            runCatching { file.relativeTo(root).path }.getOrNull()?.let {
                builder.addPath(file.absolutePath, it)
            }
        }
        openFiles.associate { file ->
            val base = when {
                (countByName[file.name] ?: 1) > 1 -> builder.getShortPath(file.absolutePath)
                else -> file.name
            }
            file.absolutePath to base
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(editorBg()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(openFiles, key = { it.absolutePath }) { file ->
                val selected = file.absolutePath == active?.absolutePath
                Row(
                    modifier = Modifier
                        .padding(PaddingValues(end = 4.dp, top = 4.dp, bottom = 4.dp))
                        .clip(RoundedCornerShape(ProjectRadius.md))
                        .background(
                            if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent,
                        )
                        .combinedClickable(
                            onClick = { onSelect(file) },
                            onLongClick = { contextFile = file },
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FileIcon(file.name, size = 15.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        (if (isDirty(file)) "*" else "") + (labels[file.absolutePath] ?: file.name),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onClose(file) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.project_close),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
        val ctx = contextFile
        if (ctx != null) {
            CaDropdownMenu(expanded = true, onDismissRequest = { contextFile = null }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.project_close)) },
                    onClick = { contextFile = null; onClose(ctx) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.project_close_others)) },
                    onClick = { contextFile = null; onCloseOthers(ctx) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.project_close_all)) },
                    onClick = { contextFile = null; onCloseAll() },
                )
            }
        }
        if (listState.canScrollForward) {
            Box {
                IconButtonCa(
                    Icons.Filled.KeyboardArrowDown,
                    stringResource(R.string.project_more_actions),
                    onClick = { overflowOpen = true },
                    boxSize = 28,
                    iconSize = 18,
                )
                CaDropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                    openFiles.forEachIndexed { index, file ->
                        val isActive = file.absolutePath == active?.absolutePath
                        DropdownMenuItem(
                            text = {
                                Text(
                                    (if (isDirty(file)) "*" else "") + (labels[file.absolutePath] ?: file.name),
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            onClick = {
                                overflowOpen = false
                                onSelect(file)
                                scope.launch { listState.animateScrollToItem(index) }
                            },
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Breadcrumb                                                          */
/* ------------------------------------------------------------------ */

/** CodeAssist `Breadcrumb` — the active file's relative path, last segment SemiBold onSurface. */
@Composable
private fun BreadcrumbBar(root: File, file: File) {
    val rel = file.relativeTo(root).path.split('/')
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(editorBg())
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        rel.forEachIndexed { index, segment ->
            if (index > 0) {
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                segment,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (index == rel.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                color = if (index == rel.lastIndex) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

/* ------------------------------------------------------------------ */
/* Dirty-close dialogs                                                 */
/* ------------------------------------------------------------------ */

/**
 * Shown when the user tries to close a single file with unsaved changes.
 * Buttons (left → right): Discard, Cancel, Save & close.
 */
@Composable
private fun DirtyCloseDialog(
    file: File,
    onDiscard: () -> Unit,
    onSaveAndClose: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.project_unsaved_changes)) },
        text = { Text(stringResource(R.string.project_dirty_message, file.name)) },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onDiscard,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.project_discard)) }
                TextButton(onClick = onCancel) { Text(stringResource(R.string.project_cancel)) }
            }
        },
        confirmButton = {
            Button(onClick = onSaveAndClose) { Text(stringResource(R.string.project_save_close)) }
        },
    )
}

/**
 * Shown when the user tries to close a group (close-others / close-all) that contains dirty files.
 * Buttons: Discard, Cancel, Save & close.
 */
@Composable
private fun DirtyBulkCloseDialog(
    count: Int,
    onDiscard: () -> Unit,
    onSaveAndClose: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.project_unsaved_changes)) },
        text = { Text(stringResource(R.string.project_dirty_multi_message, count)) },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onDiscard,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.project_discard)) }
                TextButton(onClick = onCancel) { Text(stringResource(R.string.project_cancel)) }
            }
        },
        confirmButton = {
            Button(onClick = onSaveAndClose) { Text(stringResource(R.string.project_save_close)) }
        },
    )
}

/* ------------------------------------------------------------------ */
/* Breadcrumb                                                          */
/* ------------------------------------------------------------------ */