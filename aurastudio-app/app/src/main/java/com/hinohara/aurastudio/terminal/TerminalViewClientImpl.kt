package com.hinohara.aurastudio.terminal

import android.view.KeyEvent
import android.view.MotionEvent
import com.hinohara.aurastudio.terminal.engine.TerminalSession
import com.hinohara.aurastudio.terminal.view.TerminalViewClient

class TerminalViewClientImpl : TerminalViewClient {

    var ctrlPressed: () -> Boolean = { false }
    var altPressed: () -> Boolean = { false }
    var shiftPressed: () -> Boolean = { false }
    var fnPressed: () -> Boolean = { false }

    override fun onScale(scale: Float): Float = scale

    override fun onSingleTapUp(e: MotionEvent) {}

    override fun shouldBackButtonBeMappedToEscape(): Boolean = true

    override fun shouldEnforceCharBasedInput(): Boolean = false

    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false

    override fun isTerminalViewSelected(): Boolean = true

    override fun copyModeChanged(copyMode: Boolean) {}

    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false

    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false

    override fun onLongPress(event: MotionEvent): Boolean = false

    override fun readControlKey(): Boolean = ctrlPressed()

    override fun readAltKey(): Boolean = altPressed()

    override fun readShiftKey(): Boolean = shiftPressed()

    override fun readFnKey(): Boolean = fnPressed()

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false

    override fun onEmulatorSet() {}

    override fun logError(tag: String, message: String) {}
    override fun logWarn(tag: String, message: String) {}
    override fun logInfo(tag: String, message: String) {}
    override fun logDebug(tag: String, message: String) {}
    override fun logVerbose(tag: String, message: String) {}
    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
    override fun logStackTrace(tag: String, e: Exception) {}
}
