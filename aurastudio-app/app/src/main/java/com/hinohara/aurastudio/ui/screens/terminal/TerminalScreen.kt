package com.hinohara.aurastudio.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinohara.aurastudio.terminal.AnsiParser
import com.hinohara.aurastudio.terminal.ShellEnvironment
import com.hinohara.aurastudio.terminal.TerminalSession
import com.hinohara.aurastudio.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(initialCommand: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var outputLines by remember { mutableStateOf(listOf("\u001B[1;32mAuraStudio Terminal\u001B[0m\n")) }
    var input by remember { mutableStateOf(initialCommand ?: "") }
    var isSessionReady by remember { mutableStateOf(false) }

    val session = remember {
        val shellPath = ShellEnvironment.getShellPath()
        val cwd = ShellEnvironment.getWorkingDirectory(context)
        val env = ShellEnvironment.getEnvironment(context)
        val lineBuffer = StringBuilder()

        TerminalSession(
            shellPath = shellPath,
            cwd = cwd,
            args = arrayOf(shellPath),
            env = env,
            rows = 24,
            columns = 80,
            onOutput = { text ->
                lineBuffer.append(text)
                val content = lineBuffer.toString()
                // Process complete lines
                val lines = content.split("\n")
                // Last element is the incomplete line (keep in buffer)
                lineBuffer.clear()
                if (!content.endsWith("\n") && lines.isNotEmpty()) {
                    lineBuffer.append(lines.last())
                }
                val completeLines = if (content.endsWith("\n")) {
                    lines.dropLast(1)
                } else {
                    lines.dropLast(1)
                }
                if (completeLines.isNotEmpty()) {
                    scope.launch(Dispatchers.Main) {
                        outputLines += completeLines
                        // Auto-scroll to bottom
                        kotlinx.coroutines.delay(50)
                        listState.animateScrollToItem(outputLines.size - 1)
                    }
                }
            },
            onProcessExited = { exitCode ->
                scope.launch(Dispatchers.Main) {
                    outputLines += "\u001B[31m[Process exited with code $exitCode]\u001B[0m"
                    isSessionReady = false
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        session.start()
        isSessionReady = true
        if (initialCommand != null) {
            session.write("$initialCommand\n")
        }
    }

    DisposableEffect(Unit) {
        onDispose { session.finish() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(termBg())
    ) {
        // Terminal output
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(outputLines) { line ->
                    val styled = try {
                        AnsiParser.parse(line)
                    } catch (_: Exception) {
                        androidx.compose.ui.text.AnnotatedString(line)
                    }
                    Text(
                        text = styled,
                        color = termFg(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

        // Input bar
        Surface(color = termSurface()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "❯ ",
                    color = termGreen(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = termFg()
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        cursorColor = termGreen()
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (input.isNotBlank() && isSessionReady) {
                            session.write("$input\n")
                            input = ""
                        }
                    }),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (input.isNotBlank() && isSessionReady) {
                            session.write("$input\n")
                            input = ""
                        }
                    },
                    enabled = isSessionReady
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (isSessionReady) termGreen() else termFg().copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
