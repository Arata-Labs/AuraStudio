package com.hinohara.aurastudio.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinohara.aurastudio.ui.theme.*

@Composable
fun SessionTabs(
    manager: TerminalSessionManager,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableIntStateOf(-1) }
    var renameText by remember { mutableStateOf("") }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(termSurface().copy(alpha = 0.9f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(manager.sessions) { index, info ->
            val isSelected = index == manager.currentIndex.intValue
            val isDead = !info.session.isRunning

            val bgColor = when {
                isSelected -> termGreen().copy(alpha = 0.2f)
                isDead -> Color.White.copy(alpha = 0.02f)
                else -> Color.White.copy(alpha = 0.05f)
            }
            val borderColor = when {
                isSelected -> termGreen().copy(alpha = 0.5f)
                else -> Color.Transparent
            }
            val textColor = when {
                isDead -> termFg().copy(alpha = 0.4f)
                isSelected -> termGreen()
                else -> termFg().copy(alpha = 0.7f)
            }

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .clickable { manager.switchTo(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = info.title.ifEmpty { "${index + 1}" },
                    color = textColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (isDead) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1
                )
            }
        }

        // Add session button
        item {
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(termGreen().copy(alpha = 0.15f))
                    .border(1.dp, termGreen().copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .clickable {
                        if (manager.sessionCount < manager.maxSessions) {
                            manager.createSession()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "New Session",
                    tint = termGreen(),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Rename dialog
    if (showRenameDialog >= 0) {
        val info = manager.getSessionInfoAt(showRenameDialog)
        if (info != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = -1 },
                title = { Text("Rename Session") },
                text = {
                    TextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        info.title = renameText
                        showRenameDialog = -1
                    }) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = -1 }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
