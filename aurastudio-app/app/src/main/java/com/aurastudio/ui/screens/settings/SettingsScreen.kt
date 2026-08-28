package com.aurastudio.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aurastudio.R
import com.aurastudio.ui.theme.*

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun SettingsScreen(
    scaffoldPadding: PaddingValues = PaddingValues(),
    themeMode: Int = THEME_SYSTEM,
    iconMode: Int = ICON_SYSTEM,
    onThemeChange: (Int) -> Unit = {},
    onIconChange: (Int) -> Unit = {}
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
        item { AppearanceSection(themeMode = themeMode, iconMode = iconMode, onThemeChange = onThemeChange, onIconChange = onIconChange) }
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
        colors = CardDefaults.cardColors(containerColor = cardBg()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
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
        }
    }
}

@Composable
private fun IconOptionItem(
    modifier: Modifier = Modifier,
    title: String,
    @DrawableRes iconRes: Int,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    showSplitBg: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier.height(92.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (showSplitBg) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFF1C1C22))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFFF5F5FA))
                        )
                    }
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        tint = Color.Unspecified,
                            modifier = Modifier.size(40.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = title,
                            tint = Color.Unspecified,
                        modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AppearanceSection(
    themeMode: Int = THEME_SYSTEM,
    iconMode: Int = ICON_SYSTEM,
    onThemeChange: (Int) -> Unit = {},
    onIconChange: (Int) -> Unit = {}
) {
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
            text = stringResource(R.string.settings_theme_desc),
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
                icon = Icons.Filled.DarkMode,
                isSelected = themeMode == THEME_DARK,
                onClick = { onThemeChange(THEME_DARK) }
            )
            ThemeOptionItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_theme_light),
                icon = Icons.Filled.LightMode,
                isSelected = themeMode == THEME_LIGHT,
                onClick = { onThemeChange(THEME_LIGHT) }
            )
            ThemeOptionItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_theme_system),
                icon = Icons.Filled.SettingsBrightness,
                isSelected = themeMode == THEME_SYSTEM,
                onClick = { onThemeChange(THEME_SYSTEM) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.settings_icon),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.settings_icon_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconOptionItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_icon_dark),
                iconRes = R.drawable.ic_logo_dark,
                bgColor = Color(0xFF1C1C22),
                isSelected = iconMode == ICON_DARK,
                onClick = { onIconChange(ICON_DARK) }
            )
            IconOptionItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_icon_light),
                iconRes = R.drawable.ic_logo,
                bgColor = Color(0xFFF5F5FA),
                isSelected = iconMode == ICON_LIGHT,
                onClick = { onIconChange(ICON_LIGHT) }
            )
            IconOptionItem(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.settings_icon_system),
                iconRes = R.drawable.ic_logo_dark,
                bgColor = MaterialTheme.colorScheme.primary,
                isSelected = iconMode == ICON_SYSTEM,
                onClick = { onIconChange(ICON_SYSTEM) },
                showSplitBg = true
            )
        }
    }
}

@Composable
private fun EnvironmentSection() {
    var selectedPath by remember { mutableIntStateOf(0) }
    val paths = listOf(
        Quadruple(stringResource(R.string.settings_java_path), "/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk", Icons.Filled.Coffee, stringResource(R.string.settings_java_desc)),
        Quadruple(stringResource(R.string.settings_sdk_path), "~/android-sdk", Icons.Filled.PhoneAndroid, stringResource(R.string.settings_sdk_desc)),
        Quadruple(stringResource(R.string.settings_ndk_path), "~/android-sdk/ndk", Icons.Filled.Memory, stringResource(R.string.settings_ndk_desc)),
        Quadruple(stringResource(R.string.settings_cmake_path), "~/android-sdk/cmake", Icons.Filled.AccountTree, stringResource(R.string.settings_cmake_desc)),
        Quadruple(stringResource(R.string.settings_build_tools_path), "~/android-sdk/build-tools", Icons.Filled.Engineering, stringResource(R.string.settings_build_tools_desc)),
        Quadruple(stringResource(R.string.settings_aapt2_path), "~/.gradle/gradle.properties", Icons.Filled.Settings, stringResource(R.string.settings_aapt2_desc)),
        Quadruple(stringResource(R.string.settings_gradle_path), "~/android-sdk/cmdline-tools", Icons.Filled.Build, stringResource(R.string.settings_gradle_desc)),
        Quadruple(stringResource(R.string.settings_config_path), "~/.config/aurastudio", Icons.Filled.Folder, stringResource(R.string.settings_config_desc)),
        Quadruple(stringResource(R.string.settings_plugins_path), "~/.config/aurastudio/plugins", Icons.Filled.Extension, stringResource(R.string.settings_plugins_desc)),
        Quadruple(stringResource(R.string.settings_cache_path), "~/.cache/aurastudio", Icons.Filled.Cached, stringResource(R.string.settings_cache_desc)),
        Quadruple(stringResource(R.string.settings_env_file), "~/.config/aurastudio/env.sh", Icons.Filled.Description, stringResource(R.string.settings_env_file_desc)),
        Quadruple(stringResource(R.string.settings_home_path), "~/", Icons.Filled.Home, stringResource(R.string.settings_home_desc))
    )

    SettingsCategory(
        icon = Icons.Filled.Folder,
        title = stringResource(R.string.settings_section_environment)
    ) {
        Text(
            text = stringResource(R.string.settings_env_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_env_active),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = paths[selectedPath].second,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.settings_env_available),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        paths.forEachIndexed { index, (title, path, icon, desc) ->
            val isSelected = selectedPath == index
            Surface(
                onClick = { selectedPath = index },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
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
            subtitle = stringResource(R.string.settings_terminal_vibrate_desc),
            checked = vibrate,
            onCheckedChange = { vibrate = it }
        )
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    SettingsCategory(
        icon = Icons.Filled.Info,
        title = stringResource(R.string.settings_section_about)
    ) {
        Text(
            text = stringResource(R.string.settings_about_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_about_info),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_about_version_value),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.settings_section_about),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        val aboutItems = listOf(
            AboutItem(
                titleRes = R.string.settings_version,
                subtitle = "1.2",
                icon = Icons.Filled.Tag,
                onClick = {
                    copyToClipboard(context, "1.2", "Version")
                }
            ),
            AboutItem(
                titleRes = R.string.settings_build,
                subtitle = "dev-app",
                icon = Icons.Filled.Build,
                onClick = {
                    copyToClipboard(context, "dev-app", "Build")
                }
            ),
            AboutItem(
                titleRes = R.string.settings_developer,
                subtitleRes = R.string.settings_developer_value,
                icon = Icons.Filled.Person,
                onClick = { showDeveloperDialog = true }
            ),
            AboutItem(
                titleRes = R.string.settings_github,
                subtitleRes = R.string.settings_github_value,
                icon = Icons.Filled.Link,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Arata-Labs/AuraStudio"))
                    context.startActivity(intent)
                }
            ),
            AboutItem(
                titleRes = R.string.settings_about_license,
                subtitleRes = R.string.settings_about_license_value,
                icon = Icons.Filled.Description,
                onClick = { showLicenseDialog = true }
            )
        )

        aboutItems.forEach { item ->
            Surface(
                onClick = item.onClick,
                shape = RoundedCornerShape(12.dp),
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(item.titleRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (item.subtitleRes != null) stringResource(item.subtitleRes) else item.subtitle ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showDeveloperDialog) {
        DeveloperDialog(onDismiss = { showDeveloperDialog = false })
    }

    if (showLicenseDialog) {
        LicenseDialog(onDismiss = { showLicenseDialog = false })
    }
}

private data class AboutItem(
    val titleRes: Int,
    val subtitle: String? = null,
    val subtitleRes: Int? = null,
    val icon: ImageVector,
    val onClick: () -> Unit
)

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

@Composable
private fun DeveloperDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg()),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.dev_profile_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = stringResource(R.string.dev_profile_role),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Spacer(modifier = Modifier.height(12.dp))

                val socials = listOf(
                    Triple(stringResource(R.string.dev_social_github), stringResource(R.string.dev_social_github_value), Icons.Filled.Code),
                    Triple(stringResource(R.string.dev_social_email), stringResource(R.string.dev_social_email_value), Icons.Filled.Email),
                    Triple(stringResource(R.string.dev_social_website), stringResource(R.string.dev_social_website_value), Icons.Filled.Language),
                )

                socials.forEach { (label, value, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.dev_profile_bio),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.env_details_close),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseDialog(onDismiss: () -> Unit) {
    val licenseText = """
MIT License

Copyright (c) 2026 HinohArata (AuraStudio CLI)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
    """.trimIndent()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = termBg()),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(min = 200.dp, max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(termGreen().copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = termGreen(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LICENSE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = termGreen(),
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "MIT License",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = termGreen(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Copyright (c) 2026 HinohArata",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = licenseText.removePrefix("MIT License\n\nCopyright (c) 2026 HinohArata (AuraStudio CLI)\n\n"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.env_details_close),
                        modifier = Modifier.padding(vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
