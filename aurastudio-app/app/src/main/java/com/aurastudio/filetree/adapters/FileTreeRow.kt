package com.aurastudio.filetree.adapters

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurastudio.R
import com.aurastudio.filetree.models.FileIcon
import com.aurastudio.ui.screens.project.CaDropdownMenu
import java.io.File

/**
 * acs `FileTreeViewHolder` port: one tree row — chevron (folders) + type icon + name, 15dp of left
 * padding per level, ripple/selection background, long-press opens the file-tree context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TreeRow(
    file: File,
    depth: Int,
    selected: File?,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    onOpenFile: (File) -> Unit,
    onNewFile: (File) -> Unit,
    onNewFolder: (File) -> Unit,
    onCopyPath: (File) -> Unit,
    onOpenWith: (File) -> Unit,
    onRename: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    val isDir = file.isDirectory
    val isOpen = file.absolutePath in expanded
    val isSelected = file.absolutePath == selected?.absolutePath
    val caretAngle by animateFloatAsState(if (isOpen) 90f else 0f, label = "caret")

    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else Color.Transparent
            )
            .combinedClickable(
                onClick = { if (isDir) onToggle(file.absolutePath) else onOpenFile(file) },
                onLongClick = { menuOpen = true }
            )
            .padding(start = (8 + depth * 15).dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDir) {
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(14.dp)
                    .rotate(caretAngle)
            )
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        if (isDir) {
            Icon(
                if (isOpen) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
        } else {
            FileIcon(file.name)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            file.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        CaDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.project_copy_path)) },
                onClick = { menuOpen = false; onCopyPath(file) }
            )
            if (isDir) {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.project_delete), color = MaterialTheme.colorScheme.error)
                    },
                    onClick = { menuOpen = false; onDelete(file) }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.project_new_file)) },
                    onClick = { menuOpen = false; onNewFile(file) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.project_new_folder)) },
                    onClick = { menuOpen = false; onNewFolder(file) }
                )
            } else {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.project_delete), color = MaterialTheme.colorScheme.error)
                    },
                    onClick = { menuOpen = false; onDelete(file) }
                )
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.project_open_with)) },
                onClick = { menuOpen = false; onOpenWith(file) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.project_rename)) },
                onClick = { menuOpen = false; onRename(file) }
            )
        }
    }
}