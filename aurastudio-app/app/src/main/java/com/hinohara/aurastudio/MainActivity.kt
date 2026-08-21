package com.hinohara.aurastudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraStudioTheme {
                AuraStudioApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraStudioApp(
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var terminalCommand by remember { mutableStateOf<String?>(null) }

    fun navigateToBottomTab(route: String) {
        if (route == Screen.Terminal.route) {
            terminalCommand = null
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
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
                currentRoute = currentRoute,
                onItemClick = { navigateToBottomTab(it) }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(
                top = padding.calculateTopPadding()
            )
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToTerminal = { cmd ->
                        terminalCommand = cmd
                        navigateToBottomTab(Screen.Terminal.route)
                    },
                    onNavigateToEditor = {},
                    onOpenProject = { /* TODO */ }
                )
            }
            composable(Screen.Projects.route) {
                ProjectsScreen(
                    onOpenProject = { /* TODO */ }
                )
            }
            composable(Screen.CreateProject.route) {
                CreateProjectScreen(
                    onCreateProject = { name, type, path ->
                        navigateToBottomTab(Screen.Projects.route)
                    }
                )
            }
            composable(Screen.Terminal.route) {
                TerminalScreen(initialCommand = terminalCommand)
            }
            composable(Screen.Settings.route) {
                com.hinohara.aurastudio.ui.screens.editor.SettingsScreen()
            }
        }
    }
}
