package com.aurastudio.data.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import java.io.File

/**
 * Owns the embedded terminal sessions used by the in-project terminal panel,
 * mirroring the acs in-project terminal fragment: one workspace per projectDir,
 * each with an arbitrary number of named sessions ("Session N"). The shell keeps
 * running while the panel is hidden; re-attaching a [TerminalView] resumes it.
 */
class TerminalWorkspace(
    val projectDir: String,
    private val appContext: Context
) {

    val sessions = mutableStateListOf<TerminalSession>()

    var activeIndex by mutableIntStateOf(-1)

    /** Reference size used as the 1.0x anchor for pinch-zoom ([TerminalViewClient.onScale]). */
    var baseTextSize by mutableIntStateOf(14)

    /** Sticky CTRL/ALT state, toggled from the extra-keys row exactly like acs's ExtraKeysHandler. */
    var ctrlDown by mutableStateOf(false)
    var altDown by mutableStateOf(false)

    var view: TerminalView? = null
        set(value) {
            field = value
            attachActive()
        }

    val activeSession: TerminalSession? get() = sessions.getOrNull(activeIndex)

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
    }

    private val sessionClient = object : TerminalSessionClient {
        override fun onTextChanged(session: TerminalSession) {
            if (sessions[activeIndex] == session) view?.onScreenUpdated()
        }

        override fun onTitleChanged(session: TerminalSession) = Unit

        override fun onSessionFinished(session: TerminalSession) {
            onMain {
                val index = sessions.indexOf(session)
                if (index >= 0) {
                    sessions.removeAt(index)
                    if (sessions.isEmpty()) {
                        activeIndex = -1
                    } else if (activeIndex >= sessions.size) {
                        activeIndex = sessions.size - 1
                    }
                    attachActive()
                }
            }
        }

        override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
            val cm = appContext.getSystemService(ClipboardManager::class.java) ?: return
            cm.setPrimaryClip(ClipData.newPlainText("AuraStudio", text))
        }

        override fun onPasteTextFromClipboard(session: TerminalSession) {
            val cm = appContext.getSystemService(ClipboardManager::class.java) ?: return
            val text = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: return
            view?.mEmulator?.paste(text)
        }

        override fun onBell(session: TerminalSession) = vibrate()

        override fun onColorsChanged(session: TerminalSession) = Unit

        override fun onTerminalCursorStateChange(state: Boolean) {
            view?.setTerminalCursorBlinkerState(state, false)
        }

        override fun getTerminalCursorStyle(): Int? = null

        override fun logError(tag: String, message: String) { Log.e(tag, message) }
        override fun logWarn(tag: String, message: String) { Log.w(tag, message) }
        override fun logInfo(tag: String, message: String) { Log.i(tag, message) }
        override fun logDebug(tag: String, message: String) { Log.d(tag, message) }
        override fun logVerbose(tag: String, message: String) { Log.v(tag, message) }
        override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {
            Log.e(tag, message, e)
        }
        override fun logStackTrace(tag: String, e: Exception) {
            Log.e(tag, "error", e)
        }
    }

    /** Create a new bash session rooted at [projectDir], named "Session N". */
    fun newSession(context: Context): Int {
        val shell = File(TermuxEnv.prefix(context), "bin/bash").absolutePath
        val session = TerminalSession(
            shell,
            projectDir,
            null,
            TermuxEnv.toArray(context),
            null,
            sessionClient
        )
        session.mSessionName = "Session ${sessions.size + 1}"
        sessions.add(session)
        activeIndex = sessions.lastIndex
        attachActive()
        return activeIndex
    }

    fun selectSession(index: Int) {
        if (index !in sessions.indices) return
        activeIndex = index
        attachActive()
    }

    fun closeSession(index: Int) {
        onMain {
            runCatching { sessions.getOrNull(index)?.finishIfRunning() }
            if (index in sessions.indices) sessions.removeAt(index)
            if (sessions.isEmpty()) {
                activeIndex = -1
            } else if (activeIndex >= sessions.size) {
                activeIndex = sessions.size - 1
            }
            attachActive()
        }
    }

    fun finishAll() {
        sessions.toList().forEach { runCatching { it.finishIfRunning() } }
        sessions.clear()
        activeIndex = -1
    }

    private fun attachActive() {
        val v = view ?: return
        val active = activeSession
        if (active != null) {
            v.attachSession(active)
            v.requestFocus()
        } else {
            v.attachSession(null)
        }
        v.onScreenUpdated()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

/** Entry point for the in-project terminal panel. */
object ProjectTerminal {

    private val workspaces = mutableMapOf<String, TerminalWorkspace>()

    fun workspace(projectDir: String, context: Context): TerminalWorkspace =
        workspaces.getOrPut(projectDir) { TerminalWorkspace(projectDir, context.applicationContext) }

    /** Bind [view] to the workspace for [projectDir]. */
    fun attach(context: Context, view: TerminalView, projectDir: String) {
        view.isFocusableInTouchMode = true
        view.setTextSize(14)
        view.setTypeface(android.graphics.Typeface.MONOSPACE)
        view.setTerminalViewClient(viewClient)
        workspace(projectDir, context).also {
            it.view = view
            it.baseTextSize = 14
        }
    }

    /** Unbind without killing the shell sessions. */
    fun detach(projectDir: String) {
        workspaces[projectDir]?.view = null
    }

    /** Kill all sessions for [projectDir] and drop the workspace. */
    fun shutdown(projectDir: String) {
        workspaces.remove(projectDir)?.finishAll()
    }

    private fun visibleWorkspace(): TerminalWorkspace? =
        workspaces.values.firstOrNull { it.view != null }

    private val viewClient = object : TerminalViewClient {

        /**
         * The vendored TerminalView only accumulates [TerminalView.mScaleFactor] and hands it to this
         * callback — it never applies it, so pinch-zoom is implemented here by resizing the font,
         * clamped to the same 0.5f–2.0f range acs/Termux uses.
         */
        override fun onScale(scale: Float): Float {
            val clamped = scale.coerceIn(0.5f, 2.0f)
            val ws = visibleWorkspace()
            if (ws != null) {
                val size = (ws.baseTextSize * clamped).toInt().coerceIn(6, 32)
                ws.view?.setTextSize(size)
            }
            return clamped
        }

        override fun onSingleTapUp(event: MotionEvent): Unit {
            val v = visibleWorkspace()?.view ?: return
            val imm = v.context.getSystemService(InputMethodManager::class.java) ?: return
            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
        }

        override fun shouldBackButtonBeMappedToEscape(): Boolean = false
        override fun shouldEnforceCharBasedInput(): Boolean = true
        override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
        override fun isTerminalViewSelected(): Boolean = true
        override fun copyModeChanged(copyMode: Boolean): Unit = Unit
        override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false
        override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
        override fun onLongPress(event: MotionEvent): Boolean = true
        override fun readControlKey(): Boolean = visibleWorkspace()?.ctrlDown == true
        override fun readAltKey(): Boolean = visibleWorkspace()?.altDown == true
        override fun readShiftKey(): Boolean = false
        override fun readFnKey(): Boolean = false
        override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession):
            Boolean = false
        override fun onEmulatorSet(): Unit {
            val v = visibleWorkspace()?.view
            if (v != null) {
                v.setTerminalCursorBlinkerRate(1000)
                v.setTerminalCursorBlinkerState(true, true)
            }
        }

        override fun logError(tag: String, message: String) { Log.e(tag, message) }
        override fun logWarn(tag: String, message: String) { Log.w(tag, message) }
        override fun logInfo(tag: String, message: String) { Log.i(tag, message) }
        override fun logDebug(tag: String, message: String) { Log.d(tag, message) }
        override fun logVerbose(tag: String, message: String) { Log.v(tag, message) }
        override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {
            Log.e(tag, message, e)
        }
        override fun logStackTrace(tag: String, e: Exception) {
            Log.e(tag, "error", e)
        }
    }
}