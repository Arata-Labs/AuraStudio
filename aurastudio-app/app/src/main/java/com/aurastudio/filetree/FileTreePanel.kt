package com.aurastudio.filetree

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurastudio.R
import com.aurastudio.filetree.actions.FileTreeActions
import com.aurastudio.filetree.adapters.TreeRow
import com.aurastudio.filetree.callables.FlatRow
import com.aurastudio.filetree.callables.SortMode
import com.aurastudio.filetree.callables.TreeScopeMode
import com.aurastudio.filetree.callables.allowed
import com.aurastudio.filetree.callables.effectiveExpanded
import com.aurastudio.filetree.callables.flattenVisible
import com.aurastudio.filetree.callables.sortedChildren
import com.aurastudio.ui.screens.project.CaDropdownMenu
import com.aurastudio.ui.screens.project.CaMenuItem
import com.aurastudio.ui.screens.project.ProjectRadius
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * CodeAssist-style file navigator (acs `fragments/sidebar/FileTreeFragment`): rounded header w/ project
 * tile (acs toolbar), scope dropdown, overflow menu (new/refresh/expand/collapse/sort), search box,
 * pull-to-refresh tree with per-extension icons, 15dp/level indentation, single-child auto-expand, and an
 * acs-ordered long-press context menu.
 */
internal const val PREFS_NAME = "aurastudio_settings"
private const val KEY_FILE_TREE_SCOPE = "file_tree_scope"
private const val KEY_FILE_TREE_SORT = "file_tree_sort"

@Composable
internal fun FileTreePanel(
    root: File,
    selected: File?,
    onOpenFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var expanded by remember(root) {
        mutableStateOf(
            root.listFiles()
                ?.filter { it.isDirectory && allowed(it, TreeScopeMode.Project) }
                ?.map { it.absolutePath }
                ?.toSet().orEmpty()
        )
    }
    var mode by remember(root) {
        mutableStateOf(
            TreeScopeMode.entries.getOrElse(
                prefs.getInt(KEY_FILE_TREE_SCOPE, TreeScopeMode.Project.ordinal)
            ) { TreeScopeMode.Project }
        )
    }
    var sort by remember(root) {
        mutableStateOf(
            SortMode.entries.getOrElse(
                prefs.getInt(KEY_FILE_TREE_SORT, SortMode.Name.ordinal)
            ) { SortMode.Name }
        )
    }
    var refresh by remember(root) { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    var headerMenuOpen by remember { mutableStateOf(false) }
    var scopeMenuOpen by remember { mutableStateOf(false) }

    var newFileTarget by remember { mutableStateOf<File?>(null) }
    var newFolderTarget by remember { mutableStateOf<File?>(null) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var query by remember { mutableStateOf("") }

    val effective = remember(expanded, mode, sort) {
        effectiveExpanded(root, expanded, mode, sort)
    }
    // Persist user-scope/sort settings so they survive closing & reopening the file tree.
    LaunchedEffect(mode, sort) {
        prefs.edit()
            .putInt(KEY_FILE_TREE_SCOPE, mode.ordinal)
            .putInt(KEY_FILE_TREE_SORT, sort.ordinal)
            .apply()
    }
    val visible = remember(expanded, mode, sort, refresh, query) {
        if (query.isBlank()) {
            flattenVisible(root, expanded, mode, sort)
        } else {
            val matches = ArrayList<FlatRow>()
            fun search(dir: File) {
                if (matches.size >= 300) return
                val children = sortedChildren(dir, sort).filter { allowed(it, mode) }
                for (f in children) {
                    if (matches.size >= 300) return
                    if (f.name.contains(query, ignoreCase = true)) matches.add(FlatRow(f, 0))
                    if (f.isDirectory) search(f)
                }
            }
            search(root)
            matches
        }
    }

    Column(modifier = modifier) {
        TreeHeader(
            root = root,
            mode = mode,
            onModeChange = { mode = it },            scopeMenuOpen = scopeMenuOpen,
            onScopeMenuChange = { scopeMenuOpen = it },
            onNewFile = { newFileTarget = root },
            onNewFolder = { newFolderTarget = root },
            onRefresh = { refresh++ },
            onExpandAll = {
                expanded = visible.filter { it.file.isDirectory }.map { it.file.absolutePath }.toSet()
            },
            onCollapseAll = { expanded = emptySet() },
            sort = sort,
            onSort = { sort = it },
            headerMenuOpen = headerMenuOpen,
            onHeaderMenuChange = { headerMenuOpen = it }
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.project_search_hint)) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(17.dp))
            },
            shape = RoundedCornerShape(ProjectRadius.sm),
        )

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    refresh++
                    delay(400)
                    refreshing = false
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (visible.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.project_empty_dir),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                items(visible, key = { it.file.absolutePath }) { row ->
                    TreeRow(
                        file = row.file,
                        depth = row.depth,
                        selected = selected,
                        expanded = effective,
                        onToggle = { path ->
                            expanded = if (path in expanded) expanded - path else expanded + path
                        },
                        onOpenFile = onOpenFile,
                        onNewFile = { dir -> newFileTarget = dir },
                        onNewFolder = { dir -> newFolderTarget = dir },
                        onCopyPath = { f ->
                            FileTreeActions.copyPath(context, f)
                            Toast.makeText(context, R.string.project_path_copied, Toast.LENGTH_SHORT).show()
                        },
                        onOpenWith = { f ->
                            FileTreeActions.openWith(context, f) { e ->
                                Toast.makeText(context, e, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRename = { f -> renameTarget = f },
                        onDelete = { f -> deleteTarget = f }
                    )
                }
            }
        }
    }

    newFileTarget?.let { dir ->
        NameDialog(
            title = stringResource(R.string.project_new_file),
            initial = "",
            onConfirm = { name ->
                if (FileTreeActions.newFile(dir, name)) refresh++
            },
            onDismiss = { newFileTarget = null }
        )
    }
    newFolderTarget?.let { dir ->
        NameDialog(
            title = stringResource(R.string.project_new_folder),
            initial = "",
            onConfirm = { name ->
                if (FileTreeActions.newFolder(dir, name)) refresh++
            },
            onDismiss = { newFolderTarget = null }
        )
    }
    renameTarget?.let { file ->
        NameDialog(
            title = stringResource(R.string.project_rename),
            initial = file.name,
            onConfirm = { name ->
                if (FileTreeActions.rename(file, name)) {
                    expanded = (expanded - file.absolutePath) + ((file.parentFile ?: root).absolutePath)
                    refresh++
                } else {
                    Toast.makeText(context, R.string.project_rename_failed, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.project_delete_title)) },
            text = { Text(stringResource(R.string.project_delete_message, file.name)) },
            confirmButton = {
                TextButton(onClick = {
                    if (FileTreeActions.delete(file)) refresh++
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.project_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.project_cancel))
                }
            }
        )
    }
}

private fun scopeLabel(mode: TreeScopeMode): Int =
    if (mode == TreeScopeMode.Project) R.string.project_scope_project else R.string.project_scope_all

@Composable
private fun TreeHeader(
    root: File,
    mode: TreeScopeMode,
    onModeChange: (TreeScopeMode) -> Unit,
    scopeMenuOpen: Boolean,
    onScopeMenuChange: (Boolean) -> Unit,
    onNewFile: () -> Unit,
    onNewFolder: () -> Unit,
    onRefresh: () -> Unit,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    sort: SortMode,
    onSort: (SortMode) -> Unit,
    headerMenuOpen: Boolean,
    onHeaderMenuChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProjectTile(name = root.name, size = 32)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    root.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    root.absolutePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { onHeaderMenuChange(true) }, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.project_more_actions),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                CaDropdownMenu(
                    expanded = headerMenuOpen,
                    onDismissRequest = { onHeaderMenuChange(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.project_new_file)) },
                        onClick = { onHeaderMenuChange(false); onNewFile() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.project_new_folder)) },
                        onClick = { onHeaderMenuChange(false); onNewFolder() }
                    )
                    HorizontalDivider()
                    CaMenuItem(
                        text = stringResource(R.string.project_refresh),
                        icon = Icons.Filled.Refresh,
                        onClick = { onHeaderMenuChange(false); onRefresh() }
                    )
                    CaMenuItem(
                        text = stringResource(R.string.project_expand_all),
                        icon = Icons.Filled.UnfoldMore,
                        onClick = { onHeaderMenuChange(false); onExpandAll() }
                    )
                    CaMenuItem(
                        text = stringResource(R.string.project_collapse_all),
                        icon = Icons.Filled.UnfoldLess,
                        onClick = { onHeaderMenuChange(false); onCollapseAll() }
                    )
                    HorizontalDivider()
                    Text(
                        stringResource(R.string.project_sort),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    CheckableMenuItem(
                        label = stringResource(R.string.project_sort_name),
                        checked = sort == SortMode.Name,
                        onClick = { onSort(SortMode.Name); onHeaderMenuChange(false) }
                    )
                    CheckableMenuItem(
                        label = stringResource(R.string.project_sort_type),
                        checked = sort == SortMode.Type,
                        onClick = { onSort(SortMode.Type); onHeaderMenuChange(false) }
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(start = 12.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(12.dp)
                )
                .clickable { onScopeMenuChange(!scopeMenuOpen) }
                .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(scopeLabel(mode)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(15.dp)
            )
            CaDropdownMenu(expanded = scopeMenuOpen, onDismissRequest = { onScopeMenuChange(false) }) {
                TreeScopeMode.entries.forEach { m ->
                    CheckableMenuItem(
                        label = stringResource(scopeLabel(m)),
                        checked = mode == m,
                        onClick = {
                            onModeChange(m)
                            onScopeMenuChange(false)
                        }
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
    }
}

@Composable
private fun CheckableMenuItem(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (checked) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        onClick = onClick
    )
}

@Composable
private fun ProjectTile(name: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    )
                ),
                RoundedCornerShape(9.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.take(1).uppercase(Locale.ROOT),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.42f).sp,
            maxLines = 1
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.project_name_label)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.project_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.project_cancel))
            }
        }
    )
}