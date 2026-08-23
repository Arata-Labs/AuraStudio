package com.hinohara.aurastudio.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import com.hinohara.aurastudio.terminal.engine.TerminalSession
import com.hinohara.aurastudio.terminal.engine.TerminalSessionClient
import java.util.UUID

data class TerminalSessionInfo(
    val handle: String = UUID.randomUUID().toString(),
    var session: TerminalSession,
    var title: String = "Terminal",
    val createdAt: Long = System.currentTimeMillis()
)

class TerminalSessionManager(private val context: Context) : TerminalSessionClient {

    val sessions = mutableStateListOf<TerminalSessionInfo>()
    val currentIndex = mutableIntStateOf(0)

    private val mainHandler = Handler(Looper.getMainLooper())

    var onScreenUpdate: ((TerminalSession) -> Unit)? = null
    var onSessionFinished: ((TerminalSessionInfo) -> Unit)? = null

    val sessionCount: Int get() = sessions.size
    val maxSessions = 8

    fun createSession(): TerminalSessionInfo {
        val shellPath = ShellEnvironment.getShellPath()
        val cwd = ShellEnvironment.getWorkingDirectory(context)
        val env = ShellEnvironment.getEnvironment(context)

        val session = TerminalSession(
            shellPath,
            cwd,
            arrayOf(shellPath, "--norc", "--noprofile"),
            env,
            5000,
            this
        )

        val info = TerminalSessionInfo(session = session)
        sessions.add(info)
        currentIndex.intValue = sessions.size - 1
        return info
    }

    fun switchTo(index: Int) {
        if (index in sessions.indices) {
            currentIndex.intValue = index
        }
    }

    fun removeSession(index: Int) {
        if (index !in sessions.indices) return
        val info = sessions[index]
        info.session.finishIfRunning()
        sessions.removeAt(index)
        if (sessions.isEmpty()) {
            currentIndex.intValue = 0
        } else {
            currentIndex.intValue = currentIndex.intValue.coerceIn(0, sessions.size - 1)
        }
    }

    fun getCurrent(): TerminalSessionInfo? {
        return sessions.getOrNull(currentIndex.intValue)
    }

    fun getCurrentSession(): TerminalSession? {
        return getCurrent()?.session
    }

    fun getSessionAt(index: Int): TerminalSession? {
        return sessions.getOrNull(index)?.session
    }

    fun getSessionInfoAt(index: Int): TerminalSessionInfo? {
        return sessions.getOrNull(index)
    }

    fun updateSessionTitle(session: TerminalSession) {
        val info = sessions.find { it.session == session }
        if (info != null) {
            val title = session.title
            if (!title.isNullOrEmpty()) {
                info.title = title
            }
        }
    }

    override fun onTextChanged(changedSession: TerminalSession) {
        mainHandler.post {
            onScreenUpdate?.invoke(changedSession)
        }
    }

    override fun onTitleChanged(changedSession: TerminalSession) {
        updateSessionTitle(changedSession)
    }

    override fun onSessionFinished(finishedSession: TerminalSession) {
        mainHandler.post {
            val info = sessions.find { it.session == finishedSession }
            if (info != null) {
                onSessionFinished?.invoke(info)
            }
        }
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("terminal", text))
    }

    override fun onPasteTextFromClipboard(session: TerminalSession) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (text != null) {
                session.write(text.toByteArray(), 0, text.toByteArray().size)
            }
        }
    }

    override fun onBell(session: TerminalSession) {}

    override fun onColorsChanged(session: TerminalSession) {
        mainHandler.post {
            onScreenUpdate?.invoke(session)
        }
    }

    override fun onTerminalCursorStateChange(state: Boolean) {}

    override fun getTerminalCursorStyle(): Int? = null

    override fun logError(tag: String, message: String) {}
    override fun logWarn(tag: String, message: String) {}
    override fun logInfo(tag: String, message: String) {}
    override fun logDebug(tag: String, message: String) {}
    override fun logVerbose(tag: String, message: String) {}
    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
    override fun logStackTrace(tag: String, e: Exception) {}

    fun finishAll() {
        for (info in sessions) {
            info.session.finishIfRunning()
        }
        sessions.clear()
        currentIndex.intValue = 0
    }
}
