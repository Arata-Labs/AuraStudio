package com.aurastudio.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurastudio.app.data.models.FileNode
import com.aurastudio.app.data.viewmodel.MainViewModel
import com.aurastudio.app.ui.theme.*

@Composable
fun FileExplorerScreen(viewModel: MainViewModel) {
    val fileTree by viewModel.fileTree.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        // Toolbar
        Surface(
            color = DarkSurface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateUp() }) {
                    Icon(
                        Icons.Filled.ArrowUpward,
                        contentDescription = "Up",
                        tint = Cyan
                    )
                }
                Text(
                    text = "Files",
                    color = Cyan,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                )
            }
        }

        // File tree
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            fileTree.forEach { node ->
                renderFileNode(node, viewModel, 0)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.lazy.LazyListScope.renderFileNode(
    node: FileNode,
    viewModel: MainViewModel,
    depth: Int
) {
    item(key = node.path) {
        FileNodeRow(node, viewModel, depth)
    }
    if (node.isDirectory) {
        node.children.forEach { child ->
            renderFileNode(child, viewModel, depth + 1)
        }
    }
}

@Composable
private fun FileNodeRow(
    node: FileNode,
    viewModel: MainViewModel,
    depth: Int
) {
    val icon: ImageVector = when {
        node.isDirectory -> Icons.Filled.Folder
        node.name.endsWith(".kt") -> Icons.Filled.Code
        node.name.endsWith(".java") -> Icons.Filled.Code
        node.name.endsWith(".cpp") || node.name.endsWith(".c") -> Icons.Filled.Code
        node.name.endsWith(".xml") -> Icons.Filled.DataObject
        node.name.endsWith(".gradle") || node.name.endsWith(".gradle.kts") -> Icons.Filled.Build
        node.name.endsWith(".json") -> Icons.Filled.DataObject
        else -> Icons.Filled.InsertDriveFile
    }

    val tint = when {
        node.isDirectory -> Amber
        node.name.endsWith(".kt") -> Purple
        node.name.endsWith(".java") -> IndigoLight
        node.name.endsWith(".cpp") || node.name.endsWith(".c") -> Cyan
        else -> DarkOnSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16 + 4).dp, end = 4.dp)
            .clickable {
                if (node.isDirectory) {
                    viewModel.navigateTo(node.path)
                } else {
                    viewModel.openFile(node.path)
                }
            }
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = node.name,
            color = if (node.isDirectory) DarkOnSurface else DarkOnSurfaceVariant,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            modifier = Modifier.weight(1f)
        )
        if (!node.isDirectory && node.size > 0) {
            Text(
                text = formatSize(node.size),
                color = DarkOnSurfaceVariant,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024))}MB"
    }
}
