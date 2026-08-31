package com.aurastudio.ui.screens.project

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurastudio.R
import com.aurastudio.data.terminal.TermuxEnv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class BuildTask(val label: String, val command: String)

private val BUILD_OUTPUT_CAP = 800
internal val RUN_GREEN = Color(0xFF34D058)
internal val WARN_AMBER = Color(0xFFFFB340)

/** Holds the run/cancel state of the in-project build runner. Created once per
 *  project at the screen level so it survives Terminal/Build tab switches. */
class BuildState(
    val context: Context,
    val projectDir: String,
    private val scope: CoroutineScope
) {
    val tasks: List<BuildTask> = buildTaskList(context, File(projectDir))

    val output = mutableStateListOf<String>()

    var isRunning by mutableStateOf(false)
        private set
    var exitCode by mutableStateOf<Int?>(null)
        private set
    var selectedTask by mutableStateOf<BuildTask?>(if (tasks.isNotEmpty()) tasks.first() else null)
        private set
    var cancelled by mutableStateOf(false)
        private set

    private var process: Process? = null

    fun setTask(task: BuildTask) {
        selectedTask = task
    }

    fun launch() {
        val task = selectedTask ?: return
        if (isRunning) return
        isRunning = true
        exitCode = null
        cancelled = false
        output.clear()
        scope.launch(Dispatchers.IO) {
            val bash = TermuxEnv.prefix(context).resolve("bin/bash").absolutePath
            val pb = ProcessBuilder(bash, "-lc", task.command)
            pb.directory(File(projectDir))
            pb.redirectErrorStream(true)
            pb.environment().apply {
                putAll(System.getenv())
                putAll(TermuxEnv.toMap(context))
            }
            val p = runCatching { pb.start() }.getOrNull()
            if (p == null) {
                withContext(Dispatchers.Main) {
                    isRunning = false
                    cancelled = true
                }
                return@launch
            }
            process = p
            val reader = p.inputStream.bufferedReader()
            while (true) {
                val line = runCatching { reader.readLine() }.getOrNull() ?: break
                withContext(Dispatchers.Main) {
                    if (output.size >= BUILD_OUTPUT_CAP) output.removeAt(0)
                    output.add(line)
                }
            }
            val code = runCatching { p.waitFor() }.getOrDefault(-1)
            withContext(Dispatchers.Main) {
                if (!cancelled) exitCode = code
                isRunning = false
            }
        }
    }

    fun cancel() {
        if (!isRunning) return
        cancelled = true
        runCatching { process?.destroy() }
        output += context.getString(R.string.project_cancelled)
        isRunning = false
        exitCode = null
    }

    fun dispose() {
        cancelled = true
        runCatching { process?.destroy() }
    }
}

private fun buildTaskList(context: Context, root: File): List<BuildTask> {
    val gradleRoot = File(root, "settings.gradle").exists() ||
        File(root, "settings.gradle.kts").exists()
    return if (gradleRoot) {
        listOf(
            BuildTask(context.getString(R.string.project_task_assemble_debug), "gradle assembleDebug --no-daemon"),
            BuildTask(context.getString(R.string.project_task_assemble_release), "gradle assembleRelease --no-daemon"),
            BuildTask(context.getString(R.string.project_task_clean), "gradle clean --no-daemon"),
            BuildTask(context.getString(R.string.project_task_test), "gradle testDebugUnitTest --no-daemon")
        )
    } else if (File(root, "CMakeLists.txt").exists()) {
        listOf(
            BuildTask(context.getString(R.string.project_task_build), "bash build.sh"),
            BuildTask(context.getString(R.string.project_task_clean), "rm -rf build")
        )
    } else {
        emptyList()
    }
}

internal enum class LineLevel { Error, Warn, Info }

internal data class BuildProblem(val line: String, val level: LineLevel)

internal fun buildProblems(output: List<String>): List<BuildProblem> =
    output.mapNotNull { l -> levelOf(l).takeUnless { it == LineLevel.Info }?.let { BuildProblem(l, it) } }

private fun levelOf(line: String): LineLevel = when {
    line.contains("error", ignoreCase = true) ||
        line.startsWith("e: ") ||
        line.contains("FAILURE", ignoreCase = true) ||
        line.contains("failed", ignoreCase = true) ||
        line.contains("BUILD FAILED", ignoreCase = true) -> LineLevel.Error
    line.contains("warning", ignoreCase = true) ||
        line.contains("w: ") ->
        LineLevel.Warn
    else -> LineLevel.Info
}

private fun lineLevel(line: String): LineLevel = levelOf(line)

@Composable
internal fun lineColor(level: LineLevel): Color = when (level) {
    LineLevel.Error -> MaterialTheme.colorScheme.error
    LineLevel.Warn -> WARN_AMBER
    LineLevel.Info -> Color(0xFFE0E0E0)
}

private fun errorCount(lines: List<String>): Int =
    lines.count { lineLevel(it) == LineLevel.Error }

private fun warnCount(lines: List<String>): Int =
    lines.count { lineLevel(it) == LineLevel.Warn }

/** Build console panel per the CodeAssist BuildConsole spec: task dropdown,
 *  error/warn mini-counts, status pill, run/stop, colored streaming log. */
@Composable
fun BuildPanel(
    state: BuildState,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val errors = errorCount(state.output)
    val warnings = warnCount(state.output)

    val statusLabel: String
    val statusColor: Color
    when {
        state.isRunning -> {
            statusLabel = stringResource(R.string.project_running, state.selectedTask?.label.orEmpty())
            statusColor = MaterialTheme.colorScheme.primary
        }
        state.exitCode == 0 -> {
            statusLabel = stringResource(R.string.project_succeeded)
            statusColor = RUN_GREEN
        }
        state.exitCode != null -> {
            statusLabel = stringResource(R.string.project_failed)
            statusColor = MaterialTheme.colorScheme.error
        }
        else -> {
            statusLabel = stringResource(R.string.project_idle)
            statusColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    LaunchedEffect(state.output.size, state.isRunning) {
        if (state.isRunning && state.output.isNotEmpty()) {
            listState.animateScrollToItem((state.output.size - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
    ) {
        if (state.isRunning) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                OutlinedButton(onClick = { menuOpen = true }, enabled = state.tasks.isNotEmpty()) {
                    Text(
                        state.selectedTask?.label ?: stringResource(R.string.project_no_build_task),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                CaDropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    state.tasks.forEach { task ->
                        DropdownMenuItem(
                            text = { Text(task.label) },
                            onClick = {
                                state.setTask(task)
                                menuOpen = false
                            }
                        )
                    }
                }
            }
            if (errors > 0) {
                MiniCount(
                    count = errors,
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Filled.Error
                )
            }
            if (warnings > 0) {
                MiniCount(
                    count = warnings,
                    color = WARN_AMBER,
                    icon = Icons.Filled.Warning
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
            }
            StatusPill(label = statusLabel, color = statusColor)
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = { if (state.isRunning) state.cancel() else state.launch() },
                enabled = state.selectedTask != null
            ) {
                Icon(
                    if (state.isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isRunning) {
                        stringResource(R.string.project_stop)
                    } else {
                        stringResource(R.string.project_run)
                    },
                    tint = if (state.isRunning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        RUN_GREEN
                    }
                )
            }
        }
        HorizontalDivider(color = Color(0xFF2A2A2A))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = listState
        ) {
            items(state.output.size) { index ->
                val line = state.output[index]
                val level = lineLevel(line)
                Text(
                    text = line,
                    color = if (level == LineLevel.Info && line.startsWith("> ")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        lineColor(level)
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
internal fun MiniCount(count: Int, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                "$count",
                color = color,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun StatusPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}