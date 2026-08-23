package com.hinohara.aurastudio.terminal

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.hinohara.aurastudio.terminal.engine.TerminalSession
import com.hinohara.aurastudio.terminal.view.TerminalView
import com.hinohara.aurastudio.terminal.view.TerminalViewClient

@Composable
fun TerminalViewWrapper(
    session: TerminalSession?,
    terminalViewClient: TerminalViewClient,
    modifier: Modifier = Modifier,
    screenUpdateTrigger: Int = 0
) {
    if (session == null) return

    val context = LocalContext.current

    val terminalView = remember(session) {
        TerminalView(context, null).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFF111118.toInt())
            setTerminalViewClient(terminalViewClient)
            attachSession(session)
        }
    }

    DisposableEffect(session) {
        onDispose {}
    }

    AndroidView(
        factory = { terminalView },
        modifier = modifier,
        update = { view ->
            if (view.mTermSession != session) {
                view.attachSession(session)
            }
        }
    )
}
