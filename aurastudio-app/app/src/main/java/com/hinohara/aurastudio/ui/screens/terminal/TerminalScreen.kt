package com.hinohara.aurastudio.ui.screens.terminal

import android.content.Context
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hinohara.aurastudio.terminal.*
import com.hinohara.aurastudio.terminal.view.TerminalView
import com.hinohara.aurastudio.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TerminalScreen(
    initialCommand: String? = null,
    scaffoldPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bootstrapState by remember { mutableStateOf(
        if (BootstrapInstaller.isInstalled(context)) BootstrapInstaller.State.READY
        else BootstrapInstaller.State.NOT_INSTALLED
    ) }
    var bootstrapProgress by remember { mutableFloatStateOf(0f) }
    var bootstrapMessage by remember { mutableStateOf("") }
    var bootstrapError by remember { mutableStateOf<String?>(null) }

    val manager = remember { TerminalSessionManager(context) }
    val viewClient = remember { TerminalViewClientImpl() }

    var screenUpdateCounter by remember { mutableIntStateOf(0) }
    var terminalView by remember { mutableStateOf<TerminalView?>(null) }

    LaunchedEffect(Unit) {
        manager.onScreenUpdate = {
            screenUpdateCounter++
            terminalView?.onScreenUpdated()
        }
        @Suppress("UNUSED_EXPRESSION")
        screenUpdateCounter
    }

    LaunchedEffect(Unit) {
        if (!BootstrapInstaller.isInstalled(context)) {
            withContext(Dispatchers.IO) {
                BootstrapInstaller.install(context) { progress ->
                    bootstrapState = progress.state
                    bootstrapProgress = progress.progress
                    bootstrapMessage = progress.message
                    bootstrapError = progress.error
                }
            }
        }
    }

    if (bootstrapState != BootstrapInstaller.State.READY) {
        BootstrapLoadingScreen(
            state = bootstrapState,
            progress = bootstrapProgress,
            message = bootstrapMessage,
            error = bootstrapError,
            scaffoldPadding = scaffoldPadding,
            onRetry = {
                bootstrapError = null
                bootstrapState = BootstrapInstaller.State.NOT_INSTALLED
                scope.launch(Dispatchers.IO) {
                    BootstrapInstaller.install(context) { progress ->
                        bootstrapState = progress.state
                        bootstrapProgress = progress.progress
                        bootstrapMessage = progress.message
                        bootstrapError = progress.error
                    }
                }
            }
        )
        return
    }

    LaunchedEffect(Unit) {
        manager.createSession()
    }

    DisposableEffect(Unit) {
        onDispose { manager.finishAll() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(termBg())
            .padding(
                top = scaffoldPadding.calculateTopPadding(),
                bottom = scaffoldPadding.calculateBottomPadding()
            )
    ) {
        SessionTabs(manager = manager)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val session = manager.getCurrentSession()
            if (session != null) {
                AndroidView(
                    factory = { ctx ->
                        TerminalView(ctx, null).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(0xFF111118.toInt())
                            setTerminalViewClient(viewClient)
                            setTextSize(14)
                            isFocusable = true
                            isFocusableInTouchMode = true
                            setOnTouchListener { v, event ->
                                if (event.action == android.view.MotionEvent.ACTION_UP) {
                                    v.requestFocus()
                                    showKeyboard(ctx)
                                }
                                false
                            }
                            attachSession(session)
                            terminalView = this

                            post {
                                requestFocus()
                                showKeyboard(ctx)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        val s = manager.getCurrentSession()
                        if (s != null && view.mTermSession != s) {
                            view.attachSession(s)
                        }
                        view.onScreenUpdated()
                    }
                )
            }
        }

        ExtraKeysRow(
            terminalView = terminalView,
            viewClient = viewClient,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BootstrapLoadingScreen(
    state: BootstrapInstaller.State,
    progress: Float,
    message: String,
    error: String?,
    scaffoldPadding: PaddingValues,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(termBg())
            .padding(
                top = scaffoldPadding.calculateTopPadding(),
                bottom = scaffoldPadding.calculateBottomPadding()
            )
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AuraStudio",
            color = Color(0xFF82AAFF),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Terminal Environment Setup",
            color = Color(0xFF888899),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (error != null) {
            Text(
                text = error,
                color = Color(0xFFFF5555),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .background(Color(0xFF82AAFF).copy(alpha = 0.15f))
                    .clickableNoIndication(onRetry)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Retry",
                    color = Color(0xFF82AAFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            when (state) {
                BootstrapInstaller.State.EXTRACTING,
                BootstrapInstaller.State.SETTING_UP -> {
                    CircularProgressIndicator(
                        color = Color(0xFF82AAFF),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = message.ifEmpty {
                    when (state) {
                        BootstrapInstaller.State.NOT_INSTALLED -> "Preparing environment..."
                        BootstrapInstaller.State.EXTRACTING -> "Extracting packages..."
                        BootstrapInstaller.State.SETTING_UP -> "Setting up..."
                        else -> ""
                    }
                },
                color = Color(0xFF888899),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier {
    return this.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick
    )
}

private fun showKeyboard(context: Context) {
    @Suppress("DEPRECATION")
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
}
