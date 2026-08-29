package com.aurastudio.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurastudio.R
import com.aurastudio.data.models.*
import com.aurastudio.ui.theme.*
import com.aurastudio.data.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    scaffoldPadding: PaddingValues = PaddingValues(),
    onNavigateToTerminal: (String) -> Unit = {},
    onNavigateToEditor: () -> Unit = {},
    onOpenProject: (String) -> Unit = {}
) {
    val status by viewModel.status.collectAsState()
    val recentProjects by viewModel.recentProjects.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(
            top = scaffoldPadding.calculateTopPadding() + 8.dp,
            bottom = scaffoldPadding.calculateBottomPadding() + 16.dp
        )
    ) {
        item {
            HealthBanner(score = status.healthScore, status = status)
        }



        item {
            EnvironmentCard(
                status = status,
                onInstall = { key, version, name ->
                    viewModel.startInstall(key, version, name)
                },
                onUninstall = { key, version, name ->
                    viewModel.startUninstall(key, version, name)
                },
                onJavaSwitch = { version ->
                    viewModel.switchJavaVersion(version)
                }
            )
        }

        item {
            RecentProjectsCard(
                projects = recentProjects,
                onOpenProject = onOpenProject
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }

    val installState by viewModel.installState.collectAsState()
    installState?.let { state ->
        InstallProgressDialog(
            state = state,
            onDismiss = { viewModel.dismissInstallDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallProgressDialog(
    state: com.aurastudio.data.viewmodel.InstallState,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.log.size) {
        if (state.log.isNotEmpty()) listState.animateScrollToItem(state.log.lastIndex)
    }

    AlertDialog(
        onDismissRequest = { if (!state.isInstalling) onDismiss() },
        containerColor = cardBg(),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (state.isFinished && !state.isSuccess) Icons.Filled.Error else Icons.Filled.Download,
                        contentDescription = null,
                        tint = when {
                            state.isFinished && state.isSuccess -> termGreen()
                            state.isFinished -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (state.isFinished) {
                            if (state.isSuccess) "${state.componentName} ${state.version}" else "Install failed"
                        } else {
                            "Installing ${state.componentName} ${state.version}…"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        text = {
            Column {
                if (state.isInstalling) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    text = if (state.isFinished && !state.isSuccess) state.error else "Tapping elsewhere won't dismiss this while the operation runs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.isFinished && !state.isSuccess) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = termBg(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (state.log.isEmpty()) {
                            item {
                                Text(
                                    text = "Waiting for output…",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = termFg().copy(alpha = 0.6f)
                                )
                            }
                        }
                        itemsIndexed(state.log) { index, line ->
                            val isLast = index == state.log.lastIndex
                            val isErrorLine = line.contains("error", ignoreCase = true) || line.contains("error:", ignoreCase = true)
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = when {
                                    isErrorLine && !isLast -> MaterialTheme.colorScheme.error
                                    isLast -> termFg()
                                    else -> termFg().copy(alpha = 0.85f)
                                }
                            )
                        }
                    }
                }
                if (state.isSuccess && state.isFinished) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Installed successfully.",
                        style = MaterialTheme.typography.bodySmall,
                        color = termGreen(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isInstalling
            ) {
                Text(if (state.isFinished) "Done" else "Cancel")
            }
        }
    )
}

@Composable
private fun HealthBanner(score: Int, status: EnvironmentStatus) {
    val scoreColor = when {
        score >= 80 -> Green40
        score >= 50 -> Amber40
        else -> Red40
    }

    val (statusTextRes, statusDescRes) = when {
        score >= 90 -> Pair(R.string.health_excellent, R.string.health_desc_all_operational)
        score >= 80 -> Pair(R.string.health_good, R.string.health_desc_all_operational)
        score >= 60 -> Pair(R.string.health_fair, R.string.health_desc_some_missing)
        score >= 40 -> Pair(R.string.health_needs_attention, R.string.health_desc_partially_configured)
        else -> Pair(R.string.health_critical, R.string.health_desc_run_setup)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = scoreColor.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(88.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progress by animateFloatAsState(
                        targetValue = score / 100f,
                        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                        label = "progress"
                    )

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = scoreColor.copy(alpha = 0.2f),
                        trackColor = Color.Transparent,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = scoreColor,
                        trackColor = Color.Transparent,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_health_score_format, score),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = stringResource(R.string.dashboard_health_score_max),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dashboard_health_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(statusTextRes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scoreColor
                    )
                    Text(
                        text = stringResource(statusDescRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val components = listOf(
                    Triple(stringResource(R.string.component_java), status.java.isInstalled, Icons.Filled.Coffee),
                    Triple(stringResource(R.string.component_gradle), status.gradle.isInstalled, Icons.Filled.Build),
                    Triple(stringResource(R.string.component_sdk), status.cmdlineTools.isInstalled, Icons.Filled.PhoneAndroid),
                    Triple(stringResource(R.string.component_aapt2), status.aapt2.isInstalled, Icons.Filled.Settings)
                )

                components.forEach { (name, installed, icon) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (installed) scoreColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, if (installed) scoreColor.copy(alpha = 0.2f) else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (installed) scoreColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (installed) scoreColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onSetup: () -> Unit,
    onInstallSdk: () -> Unit,
    onDoctor: () -> Unit,
    onStatus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Rocket,
                label = stringResource(R.string.action_setup),
                description = stringResource(R.string.action_setup_desc),
                onClick = onSetup
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Download,
                label = stringResource(R.string.action_install_sdk),
                description = stringResource(R.string.action_install_sdk_desc),
                onClick = onInstallSdk
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.MedicalServices,
                label = stringResource(R.string.action_doctor),
                description = stringResource(R.string.action_doctor_desc),
                onClick = onDoctor
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.BarChart,
                label = stringResource(R.string.action_status),
                description = stringResource(R.string.action_status_desc),
                onClick = onStatus
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(88.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class EnvComponentItem(
    val name: String,
    val icon: ImageVector,
    val isInstalled: Boolean,
    val version: String?,
    val installedVersions: List<String>,
    val availableVersions: List<String>,
    val key: String // maps to PackageInstaller command
)

@Composable
private fun EnvironmentCard(
    status: EnvironmentStatus,
    onInstall: (String, String, String) -> Unit,
    onUninstall: (String, String, String) -> Unit,
    onJavaSwitch: (String) -> Unit
) {
    var showAllDialog by remember { mutableStateOf(false) }
    var selectedComponent by remember { mutableStateOf<EnvComponentItem?>(null) }

    val components = listOf(
        EnvComponentItem(
            name = stringResource(R.string.env_component_java),
            icon = Icons.Filled.Coffee,
            isInstalled = status.java.isInstalled,
            version = status.java.version,
            installedVersions = if (status.java.isInstalled) listOfNotNull(status.java.version) else emptyList(),
            availableVersions = status.java.availableVersions.ifEmpty { listOf("21.0.12", "17.0.20") },
            key = "java"
        ),
        EnvComponentItem(
            name = stringResource(R.string.env_component_gradle),
            icon = Icons.Filled.Build,
            isInstalled = status.gradle.isInstalled,
            version = status.gradle.version,
            installedVersions = if (status.gradle.isInstalled) listOfNotNull(status.gradle.version) else emptyList(),
            availableVersions = status.gradle.availableVersions.ifEmpty { listOf("9.7.1") },
            key = "gradle"
        ),
        EnvComponentItem(
            name = stringResource(R.string.env_component_aapt2),
            icon = Icons.Filled.Settings,
            isInstalled = status.aapt2.isInstalled,
            version = status.aapt2.version,
            installedVersions = if (status.aapt2.isInstalled) listOfNotNull(status.aapt2.version) else emptyList(),
            availableVersions = status.aapt2.availableVersions.ifEmpty { listOf("16.0.0.4-1") },
            key = "aapt2"
        ),
        EnvComponentItem(
            name = stringResource(R.string.env_component_sdk),
            icon = Icons.Filled.PhoneAndroid,
            isInstalled = status.cmdlineTools.isInstalled,
            version = status.cmdlineTools.version,
            installedVersions = if (status.cmdlineTools.isInstalled) listOfNotNull(status.cmdlineTools.version) else emptyList(),
            availableVersions = listOf("12.0"),
            key = "cmdline_tools"
        ),
        EnvComponentItem(
            name = stringResource(R.string.env_component_platforms),
            icon = Icons.Filled.Apps,
            isInstalled = status.platforms.isNotEmpty(),
            version = status.platforms.firstOrNull(),
            installedVersions = status.platforms,
            availableVersions = listOf("37", "36", "35", "34", "33", "32", "31", "30"),
            key = "platforms"
        ),
        EnvComponentItem(
            name = stringResource(R.string.env_component_build_tools),
            icon = Icons.Filled.Engineering,
            isInstalled = status.buildTools.isNotEmpty(),
            version = status.buildTools.firstOrNull(),
            installedVersions = status.buildTools,
            availableVersions = listOf("37.0.0", "36.0.0", "35.0.0", "34.0.0", "33.0.2", "32.0.0", "31.0.0", "30.0.3"),
            key = "build_tools"
        ),
        EnvComponentItem(
            name = stringResource(R.string.env_component_ndk),
            icon = Icons.Filled.Memory,
            isInstalled = status.ndk.isNotEmpty(),
            version = status.ndk.firstOrNull(),
            installedVersions = status.ndk,
            availableVersions = listOf("r30-beta2", "r29", "r28c", "r27d", "r26d"),
            key = "ndk"
        ),
        EnvComponentItem(
            name = stringResource(R.string.env_component_cmake),
            icon = Icons.Filled.AccountTree,
            isInstalled = status.cmake.isNotEmpty(),
            version = status.cmake.firstOrNull(),
            installedVersions = status.cmake,
            availableVersions = listOf("4.1.2", "4.1.1", "4.1.0", "4.0.3", "4.0.2", "3.25.1", "3.22.1", "3.18.1", "3.10.2"),
            key = "cmake"
        )
    )

    val installedCount = components.count { it.isInstalled }
    val notInstalledCount = components.count { !it.isInstalled }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Memory,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.env_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    onClick = { showAllDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.env_details_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Component grid - 2 columns x 4 rows
            val rows = components.chunked(2)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { component ->
                        EnvComponentCard(
                            modifier = Modifier.weight(1f),
                            component = component,
                            onClick = { selectedComponent = component }
                        )
                    }
                    // Fill empty slots
                    repeat(2 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (row != rows.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (installedCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Green40, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$installedCount ${stringResource(R.string.env_details_installed).lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Green40
                    )
                }
                if (installedCount > 0 && notInstalledCount > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (notInstalledCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Red40, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$notInstalledCount ${stringResource(R.string.env_details_not_installed).lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Red40
                    )
                }
            }
        }
    }

    // All components dialog
    if (showAllDialog) {
        EnvironmentAllDialog(
            components = components,
            onDismiss = { showAllDialog = false },
            onComponentClick = { component ->
                showAllDialog = false
                selectedComponent = component
            }
        )
    }

    // Single component dialog
    selectedComponent?.let { component ->
        ComponentDetailDialog(
            component = component,
            onDismiss = { selectedComponent = null },
            onInstall = { version -> onInstall(component.key, version, component.name) },
            onUninstall = { version -> onUninstall(component.key, version, component.name) },
            onJavaSwitch = { version -> 
                if (component.key == "java") onJavaSwitch(version)
            }
        )
    }
}

@Composable
private fun EnvComponentCard(
    modifier: Modifier = Modifier,
    component: EnvComponentItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = cardContentBg(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (component.isInstalled) Green40.copy(alpha = 0.15f) else Red40.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    component.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (component.isInstalled) Green40 else Red40
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = component.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (component.isInstalled) Green40 else Red40,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (component.isInstalled) {
                        component.version ?: stringResource(R.string.env_installed)
                    } else {
                        stringResource(R.string.env_not_installed)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (component.isInstalled) Green40 else Red40.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EnvironmentAllDialog(
    components: List<EnvComponentItem>,
    onDismiss: () -> Unit,
    onComponentClick: (EnvComponentItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg(),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.env_details_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            LazyColumn {
                // Installed section
                val installed = components.filter { it.isInstalled }
                if (installed.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.env_details_installed),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Green40
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(installed.size) { index ->
                        val comp = installed[index]
                        Surface(
                            onClick = { onComponentClick(comp) },
                            shape = RoundedCornerShape(12.dp),
                            color = Green40.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Green40.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Green40, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    comp.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Green40
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comp.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = comp.version ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Green40
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // Not installed section
                val notInstalled = components.filter { !it.isInstalled }
                if (notInstalled.isNotEmpty()) {
                    if (installed.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.env_details_not_installed),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Red40
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(notInstalled.size) { index ->
                        val comp = notInstalled[index]
                        Surface(
                            onClick = { onComponentClick(comp) },
                            shape = RoundedCornerShape(12.dp),
                            color = Red40.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Red40.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Red40, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    comp.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Red40
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comp.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.env_not_installed),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Red40
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.env_details_close),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun ComponentDetailDialog(
    component: EnvComponentItem,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onJavaSwitch: (String) -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg(),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (component.isInstalled) Green40.copy(alpha = 0.15f) else Red40.copy(alpha = 0.15f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        component.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (component.isInstalled) Green40 else Red40
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = component.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column {
                // Java version switch (when multiple JDKs installed)
                if (component.key == "java" && component.installedVersions.size > 1) {
                    Text(
                        text = stringResource(R.string.env_java_active, component.version ?: ""),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    component.installedVersions.forEach { version ->
                        val isActive = version == component.version
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Filled.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = version,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!isActive) {
                                    Surface(
                                        onClick = {
                                            val ver = when {
                                                version.contains("21") -> "21"
                                                version.contains("17") -> "17"
                                                else -> version
                                            }
                                            if (ver == "21" || ver == "17") onJavaSwitch(ver)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.env_java_switch_to, version),
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.env_java_active_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Installed versions
                if (component.installedVersions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.env_versions_installed),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Green40
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    component.installedVersions.forEach { version ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Green40.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Green40.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Green40, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = version,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    onClick = { onUninstall(version) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Red40.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Red40.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Red40
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.env_uninstall),
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Red40
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Available versions
                val availableNotInstalled = component.availableVersions.filter { it !in component.installedVersions }
                if (availableNotInstalled.isNotEmpty()) {
                    if (component.installedVersions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = stringResource(R.string.env_versions_available),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    availableNotInstalled.forEach { version ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = version,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    onClick = { onInstall(version) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Green40.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Green40.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.InstallMobile,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Green40
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.env_install),
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Green40
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (component.installedVersions.isEmpty() && component.availableVersions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.env_no_versions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.env_details_close),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
@Composable
private fun RecentProjectsCard(
    projects: List<Project>,
    onOpenProject: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.projects_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (projects.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.projects_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.projects_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                projects.forEachIndexed { index, project ->
                    ProjectRow(
                        project = project,
                        onClick = { onOpenProject(project.path) }
                    )
                    if (index < projects.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(project: Project, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cardContentBg(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (project.type) {
                        ProjectType.GRADLE_JAVA -> Icons.Filled.Code
                        ProjectType.GRADLE_KOTLIN -> Icons.Filled.Code
                        ProjectType.NATIVE_CPP -> Icons.Filled.Memory
                        ProjectType.NDK_SHARED_LIB -> Icons.Filled.Layers
                    },
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = project.path.substringBeforeLast("/"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
