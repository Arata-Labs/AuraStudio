package com.hinohara.aurastudio.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinohara.aurastudio.R
import com.hinohara.aurastudio.data.models.*
import com.hinohara.aurastudio.ui.theme.*
import com.hinohara.aurastudio.data.viewmodel.DashboardViewModel

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
            QuickActionsGrid(
                onSetup = { onNavigateToTerminal("aurastudio setup") },
                onInstallSdk = { onNavigateToTerminal("aurastudio install sdk") },
                onDoctor = { onNavigateToTerminal("aurastudio doctor") },
                onStatus = { onNavigateToTerminal("aurastudio status") }
            )
        }

        item {
            EnvironmentCard(status)
        }

        item {
            RecentProjectsCard(
                projects = recentProjects,
                onOpenProject = onOpenProject
            )
        }

        if (status.platforms.isNotEmpty() || status.ndk.isNotEmpty()) {
            item {
                InstalledComponentsCard(
                    platforms = status.platforms,
                    ndk = status.ndk,
                    buildTools = status.buildTools
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
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
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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

@Composable
private fun EnvironmentCard(status: EnvironmentStatus) {
    var showDialog by remember { mutableStateOf(false) }

    val allComponents = listOf(
        Triple(stringResource(R.string.component_java), status.java.isInstalled, status.java.version),
        Triple(stringResource(R.string.component_gradle), status.gradle.isInstalled, status.gradle.version),
        Triple(stringResource(R.string.component_aapt2), status.aapt2.isInstalled, status.aapt2.version),
        Triple(stringResource(R.string.component_sdk), status.cmdlineTools.isInstalled, status.cmdlineTools.version),
        Triple(stringResource(R.string.env_platforms), status.platforms.isNotEmpty(), status.platforms.joinToString(", ")),
        Triple(stringResource(R.string.env_build_tools), status.buildTools.isNotEmpty(), status.buildTools.joinToString(", ")),
        Triple(stringResource(R.string.env_ndk), status.ndk.isNotEmpty(), status.ndk.joinToString(", "))
    )

    val installed = allComponents.filter { it.second }
    val notInstalled = allComponents.filter { !it.second }
    val maxVisible = 4

    Card(
        onClick = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Installed components
            if (installed.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.env_details_installed),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Green40
                )
                Spacer(modifier = Modifier.height(8.dp))
                val visibleInstalled = installed.take(maxVisible)
                val remainingCount = installed.size - maxVisible

                visibleInstalled.forEach { (name, _, version) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Green40.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Green40.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Green40, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = version ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = Green40
                            )
                        }
                    }
                }
                if (remainingCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Green40.copy(alpha = 0.05f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.MoreHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Green40
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.env_and_others, remainingCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = Green40
                            )
                        }
                    }
                }
            }

            // Not installed components
            if (notInstalled.isNotEmpty()) {
                if (installed.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    text = stringResource(R.string.env_details_not_installed),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Red40
                )
                Spacer(modifier = Modifier.height(8.dp))
                notInstalled.forEach { (name, _, _) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Red40.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Red40.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Red40, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.env_not_installed),
                                style = MaterialTheme.typography.labelSmall,
                                color = Red40
                            )
                        }
                    }
                }
            }

            // Tap hint
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.env_details_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }

    // Dialog
    if (showDialog) {
        EnvironmentDetailsDialog(
            installed = installed,
            notInstalled = notInstalled,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun EnvironmentDetailsDialog(
    installed: List<Triple<String, Boolean, String?>>,
    notInstalled: List<Triple<String, Boolean, String?>>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
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
            Column {
                if (installed.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.env_details_installed),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Green40
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    installed.forEach { (name, _, version) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Green40, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = version ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Green40
                            )
                        }
                    }
                }

                if (notInstalled.isNotEmpty()) {
                    if (installed.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = stringResource(R.string.env_details_not_installed),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Red40
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    notInstalled.forEach { (name, _, _) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Red40, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
private fun ComponentMiniCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    name: String,
    version: String?,
    isInstalled: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            if (isInstalled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(7.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isInstalled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isInstalled) Green40 else MaterialTheme.colorScheme.error,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = version ?: stringResource(R.string.env_not_installed),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isInstalled) Green40 else MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
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
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
        color = CardSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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

@Composable
private fun InstalledComponentsCard(
    platforms: List<String>,
    ndk: List<String>,
    buildTools: List<String>
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
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.installed_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (platforms.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.installed_platforms),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(platforms) { platform ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = platform,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (ndk.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.installed_ndk),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ndk) { ndkVersion ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = ndkVersion,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            if (platforms.isEmpty() && ndk.isEmpty()) {
                Text(
                    text = stringResource(R.string.installed_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
