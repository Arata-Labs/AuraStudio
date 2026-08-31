package com.aurastudio.filetree.callables

import java.io.File
import java.util.Locale

/* acs `FileTreeCallable` + sorting port: list, filter, sort (folders first + name), single-child-chain
 * auto-expansion, and flattening into visible rows. Hidden files are shown (acs HiddenFilesFilter is
 * commented out); build/version-control dirs are skipped in Project scope. */

internal val IGNORED_DIRS = setOf(
    ".git", ".gradle", ".idea", "build", ".kotlin",
    ".externalNativeBuild", ".cxx", "captures", "node_modules"
)

internal enum class TreeScopeMode {
    Project, AllFiles
}

internal enum class SortMode {
    Name, Type
}

internal data class FlatRow(val file: File, val depth: Int)

internal fun allowed(node: File, mode: TreeScopeMode): Boolean = when (mode) {
    TreeScopeMode.Project -> node.name !in IGNORED_DIRS && !node.name.startsWith(".")
    TreeScopeMode.AllFiles -> node.name !in setOf(".git", ".gradle", "build")
}

internal fun sortedChildren(dir: File, sort: SortMode): List<File> {
    val files = dir.listFiles().orEmpty()
    return when (sort) {
        SortMode.Name ->
            files.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) })
        SortMode.Type ->
            files.sortedWith(
                compareBy<File>(
                    { !it.isDirectory },
                    { it.name.substringAfterLast('.', "").lowercase(Locale.ROOT) },
                    { it.name.lowercase(Locale.ROOT) }
                )
            )
    }
}

/** acs `listNode`: a directory with exactly one child that is itself a directory
 *  auto-expands (so chains like `src/` → `main/` → `java/` stay unfolded). */
internal fun effectiveExpanded(
    root: File,
    expanded: Set<String>,
    mode: TreeScopeMode,
    sort: SortMode
): Set<String> {
    val effective = expanded.toMutableSet()
    fun dive(dir: File) {
        val children = sortedChildren(dir, sort).filter { allowed(it, mode) }
        if (children.size == 1 && children.first().isDirectory) {
            val only = children.first()
            effective += only.absolutePath
            dive(only)
        }
    }
    dive(root)
    return effective
}

internal fun flattenVisible(
    root: File,
    expanded: Set<String>,
    mode: TreeScopeMode,
    sort: SortMode
): List<FlatRow> {
    val result = ArrayList<FlatRow>()
    val effective = effectiveExpanded(root, expanded, mode, sort)
    val rootChildren = sortedChildren(root, sort).filter { allowed(it, mode) }
    fun File.walk(depth: Int) {
        result.add(FlatRow(this, depth))
        if (isDirectory && absolutePath in effective) {
            sortedChildren(this, sort).filter { allowed(it, mode) }.forEach { it.walk(depth + 1) }
        }
    }
    rootChildren.forEach { it.walk(0) }
    return result
}