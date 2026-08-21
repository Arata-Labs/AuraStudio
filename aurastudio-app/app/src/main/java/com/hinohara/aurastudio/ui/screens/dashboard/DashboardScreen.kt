package com.hinohara.aurastudio.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hinohara.aurastudio.data.models.*
import com.hinohara.aurastudio.data.viewmodel.DashboardViewModel
import com.hinohara.aurastudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header
        item {
            DashboardHeader(status)
        }

        // Health Score Card
        item {
            HealthScoreCard(status.healthScore)
        }

        // Quick Actions
        item {
            SectionTitle("Quick Actions")
        }
        item {
            QuickActionsRow(
                onSetup = { onNavigateToTerminal("aurastudio setup") },
                onInstallSdk = { onNavigateToTerminal("aurastudio install sdk") },
                onDoctor = { onNavigateToTerminal("aurastudio doctor") },
                onStatus = { onNavigateToTerminal("aurastudio status") }
            )
        }

        // Environment Status
        item {
            SectionTitle("Environment")
        }
        item {
            EnvironmentStatusCard(status)
        }

        // Recent Projects
        item {
            SectionTitle("Recent Projects")
        }
        if (recentProjects.isEmpty()) {
            item {
                EmptyProjectsCard()
            }
        } else {
            items(recentProjects) { project ->
                ProjectCard(
                    project = project,
                    onClick = { onOpenProject(project.path) }
                )
            }
        }

        // Installed Platforms
        if (status.platforms.isNotEmpty()) {
            item {
                SectionTitle("Installed Platforms")
            }
            item {
                InstalledChips(status.platforms, MaterialTheme.colorScheme.primaryContainer)
            }
        }

        // Installed NDK
        if (status.ndk.isNotEmpty()) {
            item {
                SectionTitle("Installed NDK")
            }
            item {
                InstalledChips(status.ndk, MaterialTheme.colorScheme.tertiaryContainer)
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun DashboardHeader(status: EnvironmentStatus) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = "Aura Studio",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Build Android, Anywhere",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HealthScoreCard(score: Int) {
    val color = when {
        score >= 80 -> Green40
        score >= 50 -> Amber40
        else -> Red40
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score ring
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Environment Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = when {
                        score >= 80 -> "All systems operational"
                        score >= 50 -> "Some components missing"
                        else -> "Setup required"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Filled.Psychology,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun QuickActionsRow(
    onSetup: () -> Unit,
    onInstallSdk: () -> Unit,
    onDoctor: () -> Unit,
    onStatus: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickActionChip(
                icon = Icons.Filled.Rocket,
                label = "Setup",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onSetup
            )
        }
        item {
            QuickActionChip(
                icon = Icons.Filled.Download,
                label = "Install SDK",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onInstallSdk
            )
        }
        item {
            QuickActionChip(
                icon = Icons.Filled.MedicalServices,
                label = "Doctor",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onDoctor
            )
        }
        item {
            QuickActionChip(
                icon = Icons.Filled.BarChart,
                label = "Status",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onStatus
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun EnvironmentStatusCard(status: EnvironmentStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusRow("Java", status.java.version, status.java.isInstalled)
            StatusRow("Gradle", status.gradle.version, status.gradle.isInstalled)
            StatusRow("AAPT2", status.aapt2.version, status.aapt2.isInstalled)
            StatusRow("cmdline-tools", status.cmdlineTools.version, status.cmdlineTools.isInstalled)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatusRow("Platforms", "${status.platforms.size} installed", status.platforms.isNotEmpty())
            StatusRow("Build-Tools", "${status.buildTools.size} installed", status.buildTools.isNotEmpty())
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String?, isInstalled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isInstalled) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isInstalled) Green40 else Red40
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value ?: "Not installed",
            style = MaterialTheme.typography.bodySmall,
            color = if (isInstalled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ProjectCard(project: Project, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (project.type) {
                    ProjectType.GRADLE_JAVA -> Icons.Filled.Code
                    ProjectType.GRADLE_KOTLIN -> Icons.Filled.Code
                    ProjectType.NATIVE_CPP -> Icons.Filled.Memory
                    ProjectType.NDK_SHARED_LIB -> Icons.Filled.Layers
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = project.path.substringAfterLast("/"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyProjectsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No projects yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Run 'aurastudio init' to create one",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun InstalledChips(items: List<String>, containerColor: androidx.compose.ui.graphics.Color) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = containerColor
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
