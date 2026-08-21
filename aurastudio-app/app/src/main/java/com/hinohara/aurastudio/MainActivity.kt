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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hinohara.aurastudio.data.viewmodel.DashboardViewModel
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentRoute == item.route
                    val label = stringResource(item.labelRes)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (item.route == Screen.Terminal.route) {
                                terminalCommand = null
                            }
                            navController.navigate(item.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = label
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToTerminal = { cmd ->
                        terminalCommand = cmd
                        navController.navigate(Screen.Terminal.route)
                    },
                    onNavigateToEditor = {},
                    onOpenProject = { /* TODO: open project */ }
                )
            }
            composable(Screen.Projects.route) {
                ProjectsScreen(
                    onOpenProject = { /* TODO: open project */ }
                )
            }
            composable(Screen.CreateProject.route) {
                CreateProjectScreen(
                    onCreateProject = { name, type, path ->
                        // TODO: create project via aurastudio init
                        navController.navigate(Screen.Projects.route)
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
