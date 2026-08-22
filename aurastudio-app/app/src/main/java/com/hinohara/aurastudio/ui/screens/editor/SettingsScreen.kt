package com.hinohara.aurastudio.ui.screens.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinohara.aurastudio.R

@Composable
fun SettingsScreen(
    scaffoldPadding: PaddingValues = PaddingValues()
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            top = scaffoldPadding.calculateTopPadding() + 14.dp,
            bottom = scaffoldPadding.calculateBottomPadding() + 136.dp
        )
    ) {
        item { SettingsHeader() }
        item { AppearanceSection() }
        item { EnvironmentSection() }
        item { TerminalSection() }
        item { AboutSection() }
    }
}

@Composable
private fun SettingsHeader() {
    Text(
        text = stringResource(R.string.settings_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun SettingsCategory(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ThemeOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier.height(76.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AppearanceSection() {
    var selectedTheme by remember { mutableIntStateOf(0) }

    SettingsCategory(
        icon = Icons.Filled.Palette,
        title = stringResource(R.string.settings_section_general)
    ) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Choose your preferred theme",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeOptionItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_theme_dark),
                subtitle = "Dark",
                icon = Icons.Filled.DarkMode,
                isSelected = selectedTheme == 0,
                onClick = { selectedTheme = 0 }
            )
            ThemeOptionItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_theme_light),
                subtitle = "Light",
                icon = Icons.Filled.LightMode,
                isSelected = selectedTheme == 1,
                onClick = { selectedTheme = 1 }
            )
            ThemeOptionItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_theme_system),
                subtitle = "Auto",
                icon = Icons.Filled.SettingsBrightness,
                isSelected = selectedTheme == 2,
                onClick = { selectedTheme = 2 }
            )
        }
    }
}

@Composable
private fun EnvironmentSection() {
    SettingsCategory(
        icon = Icons.Filled.Folder,
        title = stringResource(R.string.settings_section_environment)
    ) {
        SettingsInfoRow(
            title = stringResource(R.string.settings_sdk_path),
            subtitle = "~/android-sdk"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        SettingsInfoRow(
            title = stringResource(R.string.settings_ndk_path),
            subtitle = "~/android-sdk/ndk"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        SettingsInfoRow(
            title = stringResource(R.string.settings_java_path),
            subtitle = "java-21-openjdk"
        )
    }
}

@Composable
private fun SettingsInfoRow(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun TerminalSection() {
    var fontSize by remember { mutableFloatStateOf(13f) }
    var vibrate by remember { mutableStateOf(true) }

    SettingsCategory(
        icon = Icons.Filled.Terminal,
        title = stringResource(R.string.settings_section_terminal)
    ) {
        Text(
            text = stringResource(R.string.settings_terminal_font_size),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${fontSize.toInt()} sp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = fontSize,
            onValueChange = { fontSize = it },
            valueRange = 10f..18f,
            steps = 7,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        SettingsSwitchRow(
            title = stringResource(R.string.settings_terminal_vibrate),
            subtitle = "Haptic feedback on keypress",
            checked = vibrate,
            onCheckedChange = { vibrate = it }
        )
    }
}

@Composable
private fun AboutSection() {
    SettingsCategory(
        icon = Icons.Filled.Info,
        title = stringResource(R.string.settings_section_about)
    ) {
        SettingsInfoRow(
            title = stringResource(R.string.settings_version),
            subtitle = "1.2"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        SettingsInfoRow(
            title = stringResource(R.string.settings_build),
            subtitle = "dev-app"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        SettingsInfoRow(
            title = stringResource(R.string.settings_developer),
            subtitle = stringResource(R.string.settings_developer_value)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        SettingsInfoRow(
            title = stringResource(R.string.settings_github),
            subtitle = "github.com/Arata-Labs/AuraStudio"
        )
    }
}
