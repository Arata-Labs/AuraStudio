package com.hinohara.aurastudio.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.hinohara.aurastudio.R

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Editor : Screen("editor")
    data object Terminal : Screen("terminal")
    data object Files : Screen("files")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, Screen.Dashboard.route),
    BottomNavItem(R.string.nav_editor, Icons.Filled.Edit, Icons.Outlined.Edit, Screen.Editor.route),
    BottomNavItem(R.string.nav_terminal, Icons.Filled.Terminal, Icons.Outlined.Terminal, Screen.Terminal.route),
    BottomNavItem(R.string.nav_files, Icons.Filled.Folder, Icons.Outlined.Folder, Screen.Files.route),
    BottomNavItem(R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, Screen.Settings.route),
)
