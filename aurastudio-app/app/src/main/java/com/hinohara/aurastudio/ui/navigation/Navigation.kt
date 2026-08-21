package com.hinohara.aurastudio.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.hinohara.aurastudio.R

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Projects : Screen("projects")
    data object CreateProject : Screen("create_project")
    data object Terminal : Screen("terminal")
    data object Settings : Screen("settings")
}

data class NavItem(
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String,
    val isCreateButton: Boolean = false
)

val bottomNavItems = listOf(
    NavItem(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, Screen.Dashboard.route),
    NavItem(R.string.nav_projects, Icons.Filled.Folder, Icons.Outlined.Folder, Screen.Projects.route),
    NavItem(R.string.nav_create_project, Icons.Filled.AddCircle, Icons.Outlined.AddCircle, Screen.CreateProject.route, isCreateButton = true),
    NavItem(R.string.nav_terminal, Icons.Filled.Terminal, Icons.Outlined.Terminal, Screen.Terminal.route),
    NavItem(R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, Screen.Settings.route),
)
