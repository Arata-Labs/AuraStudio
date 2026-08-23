package com.hinohara.aurastudio.ui.screens.terminal

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hinohara.aurastudio.terminal.*
import com.hinohara.aurastudio.terminal.view.TerminalView
import com.hinohara.aurastudio.ui.theme.*

@Composable
fun TerminalScreen(
    initialCommand: String? = null,
    scaffoldPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current

    val manager = remember { TerminalSessionManager(context) }
    val viewClient = remember { TerminalViewClientImpl() }

    var screenUpdateCounter by remember { mutableIntStateOf(0) }
    var terminalView by remember { mutableStateOf<TerminalView?>(null) }

    LaunchedEffect(Unit) {
        manager.onScreenUpdate = { screenUpdateCounter++ }
    }

    LaunchedEffect(Unit) {
        manager.createSession()
    }

    DisposableEffect(Unit) {
        onDispose { manager.finishAll() }
    }

    val currentIndex = manager.currentIndex.intValue

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
                            attachSession(session)
                            terminalView = this
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
            modifier = Modifier.fillMaxWidth()
        )
    }
}
