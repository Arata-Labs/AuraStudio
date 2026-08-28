package com.aurastudio.ui.screens.bootstrap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurastudio.R
import com.aurastudio.data.bootstrap.BootstrapState
import com.aurastudio.ui.theme.LocalIsAppDark
import com.aurastudio.ui.theme.termBg
import com.aurastudio.ui.theme.termFg
import com.aurastudio.ui.theme.termGreen

/**
 * First-run bootstrap installer. Background follows the app theme (Material You
 * dynamic color on Android 12+, app accent palette otherwise). A terminal-style
 * log box streams the extraction / second-stage output.
 */
@Composable
fun BootstrapSetupScreen(
    state: BootstrapState,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
) {
    val title = stringResource(R.string.bootstrap_title)
    val desc = stringResource(R.string.bootstrap_desc)
    val logTitle = stringResource(R.string.bootstrap_log_title)
    val extractedLabel = stringResource(R.string.bootstrap_extracted_label)
    val isDark = LocalIsAppDark.current
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else onBackground,
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        StatusCard(state = state, extractedLabel = extractedLabel)

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = logTitle,
                style = MaterialTheme.typography.labelLarge,
                color = if (isDark) Color.White else onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))

        TerminalLogBox(
            log = state.log,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Spacer(Modifier.height(16.dp))

        ActionRow(
            state = state,
            onDone = onDone,
            onRetry = onRetry,
            onSkip = onSkip,
        )
    }
}

@Composable
private fun StatusCard(state: BootstrapState, extractedLabel: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            when {
                state.hasError -> {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp),
                    )
                }
                state.done -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = termGreen(),
                        modifier = Modifier.size(32.dp),
                    )
                }
                else -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.phase.ifBlank {
                        when {
                            state.done -> stringResource(R.string.bootstrap_status_done)
                            state.hasError -> stringResource(R.string.bootstrap_status_failed)
                            else -> stringResource(R.string.bootstrap_status_waiting)
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (state.running && state.extractedFiles > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$extractedLabel ${state.extractedFiles}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (state.done) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.bootstrap_ready),
                        style = MaterialTheme.typography.bodySmall,
                        color = termGreen(),
                    )
                }

                if (state.hasError) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalLogBox(log: List<String>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.lastIndex)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = termBg(),
    ) {
        SelectionContainer {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(termBg())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (log.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.bootstrap_log_empty),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = termFg().copy(alpha = 0.6f),
                        )
                    }
                }
                itemsIndexed(log) { index, line ->
                    LogLine(line = line, isLast = index == log.lastIndex)
                }
            }
        }
    }
}

@Composable
private fun LogLine(line: String, isLast: Boolean) {
    val color = when {
        line.contains("[*]") -> termGreen()
        line.startsWith("extracting", ignoreCase = true) -> Color(0xFF4FC3F7)
        else -> termFg()
    }
    Text(
        text = line,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = if (isLast) Color.White else color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
    )
}

@Composable
private fun ActionRow(
    state: BootstrapState,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
) {
    val doneLabel = stringResource(R.string.bootstrap_done)
    val retryLabel = stringResource(R.string.bootstrap_retry)
    val skipLabel = stringResource(R.string.bootstrap_skip)

    when {
        state.done -> {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(
                    text = doneLabel,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        state.hasError -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = retryLabel,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = skipLabel,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        else -> {
            // Running — no action yet.
        }
    }
}