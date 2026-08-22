package com.hinohara.aurastudio

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hinohara.aurastudio.data.viewmodel.DashboardViewModel
import com.hinohara.aurastudio.ui.components.AuraStudioTopBar
import com.hinohara.aurastudio.ui.components.ModernBottomNavBar
import com.hinohara.aurastudio.ui.navigation.Screen
import com.hinohara.aurastudio.ui.navigation.bottomNavItems
import com.hinohara.aurastudio.ui.screens.dashboard.DashboardScreen
import com.hinohara.aurastudio.ui.screens.projects.CreateProjectScreen
import com.hinohara.aurastudio.ui.screens.projects.ProjectsScreen
import com.hinohara.aurastudio.ui.screens.terminal.TerminalScreen
import com.hinohara.aurastudio.ui.theme.AuraStudioTheme
import com.hinohara.aurastudio.ui.theme.THEME_SYSTEM

private const val PREFS_NAME = "aurastudio_settings"
private const val KEY_THEME_MODE = "theme_mode"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
            var themeMode by remember { mutableIntStateOf(prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM)) }

            AuraStudioTheme(themeMode = themeMode) {
                AuraStudioApp(
                    themeMode = themeMode,
                    onThemeChange = { mode ->
                        themeMode = mode
                        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun AuraStudioApp(
    themeMode: Int,
    onThemeChange: (Int) -> Unit,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var terminalCommand by remember { mutableStateOf<String?>(null) }

    fun navigateToTab(route: String) {
        if (route == Screen.Terminal.route) {
            terminalCommand = null
        }
        val targetScreen = when (route) {
            Screen.Dashboard.route -> Screen.Dashboard
            Screen.Projects.route -> Screen.Projects
            Screen.CreateProject.route -> Screen.CreateProject
            Screen.Terminal.route -> Screen.Terminal
            Screen.Settings.route -> Screen.Settings
            else -> Screen.Dashboard
        }
        currentScreen = targetScreen
    }

    val bottomNavItemsLocalized = bottomNavItems.map {
        com.hinohara.aurastudio.ui.components.BottomNavItem(
            label = stringResource(it.labelRes),
            selectedIcon = it.selectedIcon,
            unselectedIcon = it.unselectedIcon,
            route = it.route,
            isCreateButton = it.isCreateButton
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AuraStudioTopBar()
        },
        bottomBar = {
            ModernBottomNavBar(
                items = bottomNavItemsLocalized,
                currentRoute = currentScreen.route,
                onItemClick = { navigateToTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (currentScreen) {
                is Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        scaffoldPadding = innerPadding,
                        onNavigateToTerminal = { cmd ->
                            terminalCommand = cmd
                            navigateToTab(Screen.Terminal.route)
                        },
                        onNavigateToEditor = {},
                        onOpenProject = { /* TODO */ }
                    )
                }
                is Screen.Projects -> {
                    ProjectsScreen(
                        scaffoldPadding = innerPadding,
                        onOpenProject = { /* TODO */ }
                    )
                }
                is Screen.CreateProject -> {
                    CreateProjectScreen(
                        scaffoldPadding = innerPadding,
                        onCreateProject = { name, type, path ->
                            navigateToTab(Screen.Projects.route)
                        }
                    )
                }
                is Screen.Terminal -> {
                    TerminalScreen(
                        initialCommand = terminalCommand
                    )
                }
                is Screen.Settings -> {
                    com.hinohara.aurastudio.ui.screens.editor.SettingsScreen(
                        scaffoldPadding = innerPadding,
                        themeMode = themeMode,
                        onThemeChange = onThemeChange
                    )
                }
            }
        }
    }
}
