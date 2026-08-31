package com.aurastudio.ui.screens.project

import android.os.Process
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurastudio.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Build Console sheet destinations — the single console entry holds all four panes. */
internal enum class ConsoleTab(val labelRes: Int) {
    BUILD(R.string.project_build_output),
    PROBLEMS(R.string.project_problems),
    APP_LOGS(R.string.project_app_logs),
    TERMINAL(R.string.project_terminal),
}

/**
 * `Build Console` — the one bottom-sheet panel. Renders a Material-You tab row wrapped in a rounded,
 * elevated card (Build Output / Problems / App Logs / Terminal) above the active pane (each also inside
 * a rounded box). The active tab is wrapped in its own rounded container. Swipe up on the dock (or tap
 * the top-bar console menu) to reveal it.
 *
 * [selected] is hoisted by the caller so closing and reopening the dock restores the last active tab.
 */
@Composable
internal fun BuildConsolePanel(
    buildState: BuildState,
    projectDir: String,
    selected: ConsoleTab,
    onTabSelected: (ConsoleTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // Segmented tab row — each tab is its own rounded tonal cell, the active one gets a
        // primaryContainer card. No TabRow/indicator, so nothing can bleed across the whole row.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(ProjectRadius.lg),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConsoleTab.entries.forEach { tab ->
                    val isSelected = selected == tab
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(ProjectRadius.sm))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                            )
                            .clickable { onTabSelected(tab) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
        when (selected) {
            ConsoleTab.BUILD -> PanelCard(Modifier.fillMaxWidth().weight(1f), Color(0xFF111111)) {
                BuildPanel(buildState, it)
            }
            ConsoleTab.PROBLEMS -> PanelCard(Modifier.fillMaxWidth().weight(1f), Color(0xFF111111)) {
                ProblemsPanel(buildState, it)
            }
            ConsoleTab.APP_LOGS -> PanelCard(Modifier.fillMaxWidth().weight(1f), Color(0xFF111111)) {
                LogcatPanel(it)
            }
            ConsoleTab.TERMINAL -> PanelCard(Modifier.fillMaxWidth().weight(1f), Color.Transparent) {
                TerminalPanel(projectDir, it)
            }
        }
    }
}

/** Rounded elevated box each console pane lives in, so the sheet reads as one tidy Material-You card. */
@Composable
private fun PanelCard(
    modifier: Modifier,
    bg: Color,
    content: @Composable (Modifier) -> Unit,
) {
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(ProjectRadius.lg),
        color = bg,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        content(Modifier.fillMaxSize())
    }
}

/** Collapsed face of the [ProjectDock] — a perfectly centered Console pill that summarizes each pane:
 *  build state (run / ok / fail), problem counts and live terminal sessions. */
@Composable
internal fun ConsoleDockBar(
    running: Boolean,
    exitCode: Int?,
    errors: Int,
    warnings: Int,
    terminalSessions: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onTap,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp),
        shape = RoundedCornerShape(ProjectRadius.pill),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 4.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Build,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            VerticalDivider(
                modifier = Modifier.height(22.dp).padding(horizontal = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    stringResource(R.string.project_build_console),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                // The per-pane summary row sits beneath the label.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    when {
                        running -> SummaryChip(MaterialTheme.colorScheme.primary) {
                            CircularProgressIndicator(
                                Modifier.size(11.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 1.6.dp,
                            )
                        }
                        exitCode != null && exitCode == 0 -> SummaryChip(runGreen(), Icons.Filled.Check, "OK")
                        exitCode != null -> SummaryChip(MaterialTheme.colorScheme.error, Icons.Filled.Close, "FAIL")
                    }
                    if (errors > 0) SummaryChip(MaterialTheme.colorScheme.error, Icons.Filled.Error, "$errors")
                    if (warnings > 0) SummaryChip(WARN_AMBER, Icons.Filled.Warning, "$warnings")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            Icons.Filled.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "$terminalSessions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** A compact rounded box summarizing one console pane (icon + value in tinted tonal fill). */
@Composable
private fun SummaryChip(
    color: Color,
    icon: ImageVector? = null,
    label: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .background(color.copy(alpha = 0.13f), RoundedCornerShape(ProjectRadius.sm))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (trailing != null) {
            trailing()
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = color)
        }
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}

/** Problems pane — parses the build log into error/warning rows. */
@Composable
private fun ProblemsPanel(state: BuildState, modifier: Modifier = Modifier) {
    val problems = buildProblems(state.output)
    val errors = problems.count { it.level == LineLevel.Error }
    val warnings = problems.count { it.level == LineLevel.Warn }
    Column(modifier.fillMaxSize().background(Color(0xFF111111))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.project_problems),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (errors > 0) {
                MiniCount(errors, MaterialTheme.colorScheme.error, Icons.Filled.Error)
            }
            if (warnings > 0) {
                MiniCount(warnings, WARN_AMBER, Icons.Filled.Warning)
            }
            if (problems.isEmpty()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        stringResource(R.string.project_no_problems),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(color = Color(0xFF2A2A2A))
        if (problems.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = RUN_GREEN,
                        modifier = Modifier.size(40.dp).padding(2.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.project_no_problems),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(problems.size) { index ->
                    val p = problems[index]
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (p.level == LineLevel.Error) Icons.Filled.Error else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = lineColor(p.level),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            p.line,
                            color = lineColor(p.level),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** App Logs pane — tail of this process's `logcat` stream. */
@Composable
private fun LogcatPanel(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    fun reload() {
        scope.launch {
            loading = true
            lines = withContext(Dispatchers.IO) {
                runCatching {
                    val pid = Process.myPid()
                    val p = ProcessBuilder("logcat", "-d", "-t", "300", "--pid=$pid").redirectErrorStream(true).start()
                    val out = p.inputStream.bufferedReader().readLines()
                    p.waitFor()
                    out
                }.getOrElse { listOf("logcat unavailable: ${it.message}") }
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(modifier.fillMaxSize().background(Color(0xFF111111))) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.project_app_logs),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = ::reload, enabled = !loading) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.project_refresh),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        HorizontalDivider(color = Color(0xFF2A2A2A))
        if (!loading && lines.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.project_no_logs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(lines.size) { index ->
                    val line = lines[index]
                    val level = LOG_SEVERITY.find(line)?.groupValues?.get(1)?.firstOrNull()
                    val color = when (level) {
                        'E' -> MaterialTheme.colorScheme.error
                        'W' -> WARN_AMBER
                        'V', 'D' -> Color(0xFFAAAAAA)
                        else -> Color(0xFFE0E0E0)
                    }
                    Text(
                        line,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private val LOG_SEVERITY = Regex("^\\S+\\s+\\S+\\s+\\d+\\s+\\d+\\s+([VDIWEF])\\s")