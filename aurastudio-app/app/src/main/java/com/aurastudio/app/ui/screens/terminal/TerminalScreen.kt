package com.aurastudio.app.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurastudio.app.data.models.LineType
import com.aurastudio.app.data.viewmodel.MainViewModel
import com.aurastudio.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(viewModel: MainViewModel) {
    val state by viewModel.terminalState.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var historyIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty()) {
            listState.animateScrollToItem(state.lines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        // Terminal output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(state.lines) { line ->
                TerminalLineView(line)
            }
        }

        // Input bar
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
                Text(
                    text = "$ ",
                    color = TerminalGreen,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.padding(end = 4.dp)
                )

                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = TerminalFg
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = TerminalGreen
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (input.isNotBlank()) {
                                viewModel.executeCommand(input)
                                input = ""
                                historyIndex = -1
                            }
                        }
                    ),
                    singleLine = true
                )

                // Send / Stop button
                if (state.isRunning) {
                    IconButton(onClick = { viewModel.cancelCommand() }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Stop",
                            tint = TerminalRed
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                viewModel.executeCommand(input)
                                input = ""
                                historyIndex = -1
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = TerminalGreen
                        )
                    }
                }

                // Clear button
                IconButton(onClick = { viewModel.clearTerminal() }) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = "Clear",
                        tint = TerminalMagenta
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalLineView(line: com.aurastudio.app.data.models.TerminalLine) {
    val color = when (line.type) {
        LineType.INPUT -> TerminalGreen
        LineType.OUTPUT -> TerminalFg
        LineType.ERROR -> TerminalRed
        LineType.SYSTEM -> TerminalYellow
    }

    Text(
        text = line.text,
        color = color,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
