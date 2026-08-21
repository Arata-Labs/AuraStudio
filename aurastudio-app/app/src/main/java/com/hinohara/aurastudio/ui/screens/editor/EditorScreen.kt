package com.hinohara.aurastudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.hinohara.aurastudio.ui.theme.*

@Composable
fun EditorScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No file open\nOpen a file from the Files tab",
            color = DarkOnSurfaceVariant,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        )
    }
}
