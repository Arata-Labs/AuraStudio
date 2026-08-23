package com.hinohara.aurastudio.terminal

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinohara.aurastudio.terminal.view.TerminalView
import com.hinohara.aurastudio.ui.theme.*

@Composable
fun ExtraKeysRow(
    terminalView: TerminalView?,
    modifier: Modifier = Modifier
) {
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(termSurface().copy(alpha = 0.95f))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            EscKey("Esc") { sendKeyCode(terminalView, KeyEvent.KEYCODE_ESCAPE) }
            EscKey("Tab") { sendKeyCode(terminalView, KeyEvent.KEYCODE_TAB) }
            EscKey("Ctrl", isActive = ctrlActive) { ctrlActive = !ctrlActive }
            EscKey("Alt", isActive = altActive) { altActive = !altActive }
            EscKey("↑") { sendKeyCode(terminalView, KeyEvent.KEYCODE_DPAD_UP) }
            EscKey("Pipe") { sendCodePoint(terminalView, '|'.code, ctrlActive); ctrlActive = false }
            EscKey("/") { sendCodePoint(terminalView, '/'.code, ctrlActive); ctrlActive = false }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            EscKey("←") { sendKeyCode(terminalView, KeyEvent.KEYCODE_DPAD_LEFT) }
            EscKey("↓") { sendKeyCode(terminalView, KeyEvent.KEYCODE_DPAD_DOWN) }
            EscKey("→") { sendKeyCode(terminalView, KeyEvent.KEYCODE_DPAD_RIGHT) }
            EscKey("…") {
                terminalView?.mTermSession?.let { s ->
                    val text = "../"
                    s.write(text.toByteArray(), 0, text.toByteArray().size)
                }
            }
            EscKey("-") { sendCodePoint(terminalView, '-'.code, ctrlActive); ctrlActive = false }
            EscKey("_") { sendCodePoint(terminalView, '_'.code, ctrlActive); ctrlActive = false }
        }

        if (ctrlActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (letter in "abcdefghijklmnopqrstuvwxyz".toList()) {
                    EscKey(letter.uppercase()) {
                        sendCodePoint(terminalView, letter.code, true)
                        ctrlActive = false
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.EscKey(
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = when {
        isActive -> termGreen().copy(alpha = 0.3f)
        else -> Color.White.copy(alpha = 0.06f)
    }
    val textColor = when {
        isActive -> termGreen()
        else -> termFg().copy(alpha = 0.85f)
    }
    val borderColor = when {
        isActive -> termGreen().copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.1f)
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 18.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun sendKeyCode(terminalView: TerminalView?, keyCode: Int) {
    if (terminalView == null) return
    terminalView.onKeyDown(keyCode, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
}

private fun sendCodePoint(terminalView: TerminalView?, codePoint: Int, ctrlDown: Boolean) {
    if (terminalView == null) return
    val session = terminalView.mTermSession ?: return
    if (ctrlDown) {
        val ctrlCode = when {
            codePoint in 'a'.code..'z'.code -> codePoint - 'a'.code + 1
            codePoint in 'A'.code..'Z'.code -> codePoint - 'A'.code + 1
            else -> codePoint
        }
        session.writeCodePoint(false, ctrlCode)
    } else {
        session.writeCodePoint(false, codePoint)
    }
}
