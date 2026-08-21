package com.hinohara.aurastudio.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Editor : Screen("editor")
    data object Terminal : Screen("terminal")
    data object Files : Screen("files")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, Screen.Dashboard.route),
    BottomNavItem("Editor", Icons.Filled.Edit, Icons.Outlined.Edit, Screen.Editor.route),
    BottomNavItem("Terminal", Icons.Filled.Terminal, Icons.Outlined.Terminal, Screen.Terminal.route),
    BottomNavItem("Files", Icons.Filled.Folder, Icons.Outlined.Folder, Screen.Files.route),
    BottomNavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, Screen.Settings.route),
)
