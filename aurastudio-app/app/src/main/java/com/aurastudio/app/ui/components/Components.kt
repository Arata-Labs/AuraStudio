package com.aurastudio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurastudio.app.data.models.EditorTab
import com.aurastudio.app.ui.theme.*

@Composable
fun AuraStudioTopBar(
    currentTab: EditorTab,
    onTabSelected: (EditorTab) -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App name
            Text(
                text = "⚡ AuraStudio",
                color = Indigo,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                modifier = Modifier.padding(end = 16.dp, vertical = 12.dp)
            )

            // Tabs
            EditorTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val icon = when (tab) {
                    EditorTab.EDITOR -> Icons.Filled.Edit
                    EditorTab.TERMINAL -> Icons.Filled.Terminal
                    EditorTab.FILES -> Icons.Filled.Folder
                }

                Row(
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .background(
                            if (isSelected) DarkSurfaceVariant else DarkSurface,
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) Cyan else DarkOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.label,
                        color = if (isSelected) Cyan else DarkOnSurfaceVariant,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
fun AuraStudioBottomBar(
    isRunning: Boolean,
    onAuraStudioCommand: (String) -> Unit
) {
    var showCommands by remember { mutableStateOf(false) }

    Surface(
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isRunning) Amber else Green,
                            MaterialTheme.shapes.extraSmall
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "Running..." else "Ready",
                    color = if (isRunning) Amber else Green,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
            }

            // Quick commands
            Row {
                IconButton(
                    onClick = { showCommands = !showCommands },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Quick Commands",
                        tint = Cyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Quick commands dropdown
        if (showCommands) {
            DropdownMenu(
                expanded = showCommands,
                onDismissRequest = { showCommands = false }
            ) {
                listOf(
                    "setup" to "Run Setup",
                    "status" to "Show Status",
                    "doctor" to "Run Doctor",
                    "install sdk" to "Install SDK",
                    "install ndk" to "Install NDK",
                    "install cmake" to "Install CMake",
                    "clean" to "Clean Cache",
                    "version" to "Show Version"
                ).forEach { (cmd, label) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                style = TextStyle(fontFamily = FontFamily.Monospace)
                            )
                        },
                        onClick = {
                            onAuraStudioCommand(cmd)
                            showCommands = false
                        }
                    )
                }
            }
        }
    }
}
