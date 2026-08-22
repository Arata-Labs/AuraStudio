package com.hinohara.aurastudio.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hinohara.aurastudio.R
import com.hinohara.aurastudio.ui.theme.*

@Composable
fun SettingsScreen(
    scaffoldPadding: PaddingValues = PaddingValues()
) {
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
            SettingsHeader()
        }

        item {
            GeneralSection()
        }

        item {
            EnvironmentSection()
        }

        item {
            TerminalSection()
        }

        item {
            AboutSection()
        }
    }
}

@Composable
private fun SettingsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun GeneralSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(
                icon = Icons.Filled.Tune,
                title = stringResource(R.string.settings_section_general),
                gradient = Brush.linearGradient(listOf(Indigo40, Purple40))
            )
            Spacer(modifier = Modifier.height(16.dp))

            var selectedTheme by remember { mutableIntStateOf(0) }
            val themes = listOf(
                stringResource(R.string.settings_theme_dark),
                stringResource(R.string.settings_theme_light),
                stringResource(R.string.settings_theme_system)
            )

            SettingsRow(
                icon = Icons.Filled.DarkMode,
                title = stringResource(R.string.settings_theme),
                subtitle = themes[selectedTheme],
                iconGradient = Brush.linearGradient(listOf(Purple40, Indigo40))
            ) {
                SingleChoiceSegmentedButtonRow {
                    themes.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = themes.size
                            ),
                            onClick = { selectedTheme = index },
                            selected = selectedTheme == index,
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp
                                )
                            },
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow(
                icon = Icons.Filled.Language,
                title = stringResource(R.string.settings_language),
                subtitle = stringResource(R.string.settings_language_value),
                iconGradient = Brush.linearGradient(listOf(Cyan40, Indigo40))
            )
        }
    }
}

@Composable
private fun EnvironmentSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(
                icon = Icons.Filled.Folder,
                title = stringResource(R.string.settings_section_environment),
                gradient = Brush.linearGradient(listOf(Green40, Cyan40))
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingsRow(
                icon = Icons.Filled.PhoneAndroid,
                title = stringResource(R.string.settings_sdk_path),
                subtitle = "~/android-sdk",
                iconGradient = Brush.linearGradient(listOf(Green40, Cyan40)),
                showDivider = true
            )
            SettingsRow(
                icon = Icons.Filled.Memory,
                title = stringResource(R.string.settings_ndk_path),
                subtitle = "~/android-sdk/ndk",
                iconGradient = Brush.linearGradient(listOf(Amber40, Red40)),
                showDivider = true
            )
            SettingsRow(
                icon = Icons.Filled.Coffee,
                title = stringResource(R.string.settings_java_path),
                subtitle = "java-21-openjdk",
                iconGradient = Brush.linearGradient(listOf(Green40, Amber40))
            )
        }
    }
}

@Composable
private fun TerminalSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(
                icon = Icons.Filled.Terminal,
                title = stringResource(R.string.settings_section_terminal),
                gradient = Brush.linearGradient(listOf(Amber40, Red40))
            )
            Spacer(modifier = Modifier.height(16.dp))

            var fontSize by remember { mutableFloatStateOf(13f) }
            SettingsRow(
                icon = Icons.Filled.FormatSize,
                title = stringResource(R.string.settings_terminal_font_size),
                subtitle = "${fontSize.toInt()} sp",
                iconGradient = Brush.linearGradient(listOf(Indigo40, Purple40)),
                showDivider = true
            ) {
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 10f..18f,
                    steps = 7,
                    modifier = Modifier.width(120.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            var vibrate by remember { mutableStateOf(true) }
            SettingsRow(
                icon = Icons.Filled.Vibration,
                title = stringResource(R.string.settings_terminal_vibrate),
                iconGradient = Brush.linearGradient(listOf(Purple40, Indigo40))
            ) {
                Switch(
                    checked = vibrate,
                    onCheckedChange = { vibrate = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun AboutSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.settings_section_about),
                gradient = Brush.linearGradient(listOf(Purple40, Indigo40))
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingsRow(
                icon = Icons.Filled.Tag,
                title = stringResource(R.string.settings_version),
                subtitle = "1.2",
                iconGradient = Brush.linearGradient(listOf(Indigo40, Purple40)),
                showDivider = true
            )
            SettingsRow(
                icon = Icons.Filled.Code,
                title = stringResource(R.string.settings_build),
                subtitle = "dev-app",
                iconGradient = Brush.linearGradient(listOf(Cyan40, Indigo40)),
                showDivider = true
            )
            SettingsRow(
                icon = Icons.Filled.Person,
                title = stringResource(R.string.settings_developer),
                subtitle = stringResource(R.string.settings_developer_value),
                iconGradient = Brush.linearGradient(listOf(Green40, Cyan40)),
                showDivider = true
            )
            SettingsRow(
                icon = Icons.Filled.Link,
                title = stringResource(R.string.settings_github),
                subtitle = "github.com/Arata-Labs/AuraStudio",
                iconGradient = Brush.linearGradient(listOf(Amber40, Red40))
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    gradient: Brush
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(gradient, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconGradient: Brush = Brush.linearGradient(listOf(Indigo40, Purple40)),
    showDivider: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconGradient, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailing != null) {
                trailing()
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}
