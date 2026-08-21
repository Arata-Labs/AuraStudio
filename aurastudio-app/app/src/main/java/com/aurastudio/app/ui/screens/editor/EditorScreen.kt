package com.aurastudio.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurastudio.app.data.viewmodel.MainViewModel
import com.aurastudio.app.ui.theme.*

@Composable
fun EditorScreen(viewModel: MainViewModel) {
    val currentFile by viewModel.currentFile.collectAsState()
    val fileContent by viewModel.fileContent.collectAsState()
    var editableContent by remember(fileContent) { mutableStateOf(fileContent) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        // File name bar
        if (currentFile != null) {
            Surface(
                color = DarkSurface,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = currentFile!!.substringAfterLast("/"),
                    color = Cyan,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Editor
        if (fileContent.isNotEmpty()) {
            BasicTextField(
                value = editableContent,
                onValueChange = { editableContent = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = TerminalFg
                ),
                cursorBrush = SolidColor(Cyan),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(4.dp)) {
                        innerTextField()
                    }
                }
            )
        } else {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
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
    }
}
