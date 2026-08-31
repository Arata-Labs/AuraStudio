package com.aurastudio.ui.screens.project

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aurastudio.R
import com.aurastudio.data.terminal.ProjectTerminal
import com.aurastudio.data.terminal.TerminalWorkspace
import com.termux.view.TerminalView

/**
 * The in-project terminal panel, faithfully mirroring the acs `TerminalFragment` +
 * `fragment_terminal.xml`: with no sessions a full-screen empty state ("Initialize Terminal") is
 * shown; otherwise a 48dp surface header holds the session menu + M3 scrollable session tabs, then
 * the Termux [TerminalView] on the acs `filled_chip_background` (#0C0A14, 1dp ring) and the two-row
 * extra-keys "quick actions" bar (ESC/TAB/CTRL/ALT…).
 */
@Composable
fun TerminalPanel(projectDir: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val workspace = remember(projectDir) { ProjectTerminal.workspace(projectDir, context) }
    var extraKeysVisible by rememberSaveable { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize()) {
        if (workspace.sessions.isEmpty()) {
            TerminalEmptyState(
                modifier = Modifier.weight(1f),
                onCreate = { workspace.newSession(context) }
            )
        } else {
            TerminalHeader(
                workspace = workspace,
                extraKeysVisible = extraKeysVisible,
                onNewSession = { workspace.newSession(context) },
                onToggleExtraKeys = { extraKeysVisible = !extraKeysVisible }
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalSurfaceBg)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                AndroidView(
                    factory = { c ->
                        TerminalView(c, null).also { view ->
                            ProjectTerminal.attach(c, view, projectDir)
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    onRelease = { ProjectTerminal.detach(projectDir) }
                )
            }
            if (extraKeysVisible) {
                ExtraKeysRow(workspace)
            }
        }
    }
}

/** acs `filled_chip_background`: rounded-8dp near-black surface behind the terminal. */
private val TerminalSurfaceBg = Color(0xFF0C0A14)

/** acs empty state — a centered card with a large add-glyph, title, hint and "Initialize Terminal". */
@Composable
private fun TerminalEmptyState(modifier: Modifier = Modifier, onCreate: () -> Unit) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    Modifier.size(80.dp).alpha(0.6f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                stringResource(R.string.project_no_sessions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.project_no_sessions_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            Button(
                onClick = onCreate,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.project_create_session))
            }
        }
    }
}

/**
 * acs terminal header: a 48dp `colorSurface` bar with a menu button (new session / show-hide extra
 * keys) followed by the session tabs as an M3 scrollable tab row (the TabLayout look). Long-press a
 * tab to close that session, exactly like acs's PopupMenu("Close Session").
 */
@Composable
private fun TerminalHeader(
    workspace: TerminalWorkspace,
    extraKeysVisible: Boolean,
    onNewSession: () -> Unit,
    onToggleExtraKeys: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var closeTarget by remember { mutableStateOf(-1) }

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.project_more_actions),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                CaDropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.project_new_session)) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = { menuOpen = false; onNewSession() }
                    )
                    HorizontalDivider(thickness = 1.dp)
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (extraKeysVisible) R.string.project_hide_extra_keys
                                    else R.string.project_show_extra_keys
                                )
                            )
                        },
                        onClick = { menuOpen = false; onToggleExtraKeys() }
                    )
                }
            }
            PrimaryScrollableTabRow(
                selectedTabIndex = workspace.activeIndex.coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                workspace.sessions.forEachIndexed { index, session ->
                    SessionTab(index = index, workspace = workspace) { closeTarget = index }
                }
            }
            if (closeTarget >= 0) {
                CaDropdownMenu(
                    expanded = true,
                    onDismissRequest = { closeTarget = -1 },
                    modifier = Modifier.width(180.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.project_close_session)) },
                        onClick = {
                            closeTarget.let { workspace.closeSession(it) }
                            closeTarget = -1
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionTab(
    index: Int,
    workspace: TerminalWorkspace,
    onCloseRequest: () -> Unit
) {
    val session = workspace.sessions.getOrNull(index) ?: return
    val active = workspace.activeIndex == index
    Tab(
        selected = active,
        onClick = { workspace.selectSession(index) },
        text = {
            Box(
                Modifier.combinedClickable(
                    onClick = { workspace.selectSession(index) },
                    onLongClick = { onCloseRequest() }
                )
            ) {
                Text(
                    session.mSessionName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

private data class ExtraKey(
    val label: String,
    val glyph: Boolean = false
)

private val EXTRA_KEYS_ROW1 = listOf(
    ExtraKey("ESC"),
    ExtraKey("TAB"),
    ExtraKey("CTRL"),
    ExtraKey("ALT"),
    ExtraKey("/", glyph = true),
    ExtraKey("-", glyph = true),
    ExtraKey("|", glyph = true)
)

private val EXTRA_KEYS_ROW2 = listOf(
    ExtraKey("UP", glyph = true),
    ExtraKey("DOWN", glyph = true),
    ExtraKey("LEFT", glyph = true),
    ExtraKey("RIGHT", glyph = true),
    ExtraKey("HOME"),
    ExtraKey("END"),
    ExtraKey("PGUP"),
    ExtraKey("PGDN")
)

/**
 * acs extra-keys bar: two horizontally-scrollable rows of 48dp outlined buttons inside a tonal
 * surface. CTRL/ALT are sticky (highlighted #64B5F6) and released by any other key press.
 */
@Composable
private fun ExtraKeysRow(workspace: TerminalWorkspace) {
    val ctrl = workspace.ctrlDown
    val alt = workspace.altDown

    fun send(label: String, ctrlDown: Boolean, altDown: Boolean) {
        val session = workspace.activeSession ?: return
        val char = label[0]
        when {
            ctrlDown && char.isLetter() ->
                session.write((char.lowercaseChar().code and 0x1F).toChar().toString())
            altDown && label.length == 1 -> session.write("\u001b$label")
            else -> when (label) {
                "ESC" -> session.write("\u001b")
                "TAB" -> session.write("\u0009")
                "UP" -> session.write("\u001b[A")
                "DOWN" -> session.write("\u001b[B")
                "RIGHT" -> session.write("\u001b[C")
                "LEFT" -> session.write("\u001b[D")
                "HOME" -> session.write("\u001b[H")
                "END" -> session.write("\u001b[F")
                "PGUP" -> session.write("\u001b[5~")
                "PGDN" -> session.write("\u001b[6~")
                else -> session.write(label)
            }
        }
        // acs ExtraKeysHandler: any non-modifier key also releases sticky modifiers.
        workspace.ctrlDown = false
        workspace.altDown = false
    }

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
        Column {
            val activeLabels = buildSet {
                if (ctrl) add("CTRL")
                if (alt) add("ALT")
            }
            MiniKeyRow(EXTRA_KEYS_ROW1, activeLabels) { key ->
                when (key.label) {
                    "CTRL" -> workspace.ctrlDown = !ctrl
                    "ALT" -> workspace.altDown = !alt
                    else -> send(key.label, ctrl, alt)
                }
            }
            MiniKeyRow(EXTRA_KEYS_ROW2, emptySet()) { key -> send(key.label, ctrl, alt) }
        }
    }
}

@Composable
private fun MiniKeyRow(keys: List<ExtraKey>, activeLabels: Set<String>, onKey: (ExtraKey) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        keys.forEach { key ->
            val isActive = key.label in activeLabels
            OutlinedButton(
                onClick = { onKey(key) },
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 2.dp)
                    .width(if (key.glyph) 54.dp else 64.dp),
                shape = RoundedCornerShape(8.dp),
                colors = if (isActive) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF64B5F6),
                        contentColor = Color.White
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF555555)
                    )
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    if (key.glyph) arrowGlyph(key.label) else key.label,
                    fontSize = if (key.glyph) 16.sp else 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun arrowGlyph(label: String): String = when (label) {
    "UP" -> "↑"
    "DOWN" -> "↓"
    "LEFT" -> "←"
    "RIGHT" -> "→"
    else -> label
}