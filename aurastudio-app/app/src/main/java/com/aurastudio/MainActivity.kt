package com.aurastudio

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurastudio.data.bootstrap.BootstrapCoordinator
import com.aurastudio.data.viewmodel.DashboardViewModel
import com.aurastudio.ui.components.AuraStudioTopBar
import com.aurastudio.ui.components.ModernBottomNavBar
import com.aurastudio.ui.navigation.Screen
import com.aurastudio.ui.navigation.bottomNavItems
import com.aurastudio.ui.screens.bootstrap.BootstrapSetupScreen
import com.aurastudio.ui.screens.dashboard.DashboardScreen
import com.aurastudio.ui.screens.projects.CreateProjectScreen
import com.aurastudio.ui.screens.projects.ProjectsScreen
import com.aurastudio.ui.screens.splash.SplashScreen
import com.aurastudio.ui.screens.terminal.TerminalScreen
import com.aurastudio.ui.theme.*
import com.termux.app.TermuxInstaller

private const val PREFS_NAME = "aurastudio_settings"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_ICON_MODE = "icon_mode"

private val ICON_ALIASES = mapOf(
    ICON_DARK to ComponentName(BuildConfig.APPLICATION_ID, "${BuildConfig.APPLICATION_ID}.IconDark"),
    ICON_LIGHT to ComponentName(BuildConfig.APPLICATION_ID, "${BuildConfig.APPLICATION_ID}.IconLight"),
)

private enum class StartupPhase { Splash, Bootstrap, Main }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedThemeMode = prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM)
        val savedIconMode = prefs.getInt(KEY_ICON_MODE, ICON_DARK)
        applyIcon(this, savedIconMode, resources.configuration.uiMode and 0x20 != 0)

        setContent {
            var themeMode by remember { mutableIntStateOf(savedThemeMode) }
            var iconMode by remember { mutableIntStateOf(savedIconMode) }
            val isSystemDark = isSystemInDarkTheme()

            LaunchedEffect(isSystemDark, iconMode) {
                applyIcon(this@MainActivity, iconMode, isSystemDark)
            }

            AuraStudioTheme(themeMode = themeMode) {
                AuraStudioStartup(
                    iconMode = iconMode,
                    isSystemDark = isSystemDark,
                ) { phase ->
                    AuraStudioApp(
                        themeMode = themeMode,
                        iconMode = iconMode,
                        onThemeChange = { mode ->
                            themeMode = mode
                            prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
                        },
                        onIconChange = { mode ->
                            iconMode = mode
                            prefs.edit().putInt(KEY_ICON_MODE, mode).apply()
                        }
                    )
                }
            }
        }
    }

    private fun applyIcon(context: Context, iconMode: Int, isDark: Boolean) {
        val pm = context.packageManager
        val effectiveMode = when (iconMode) {
            ICON_SYSTEM -> if (isDark) ICON_DARK else ICON_LIGHT
            else -> iconMode
        }
        ICON_ALIASES.forEach { (mode, component) ->
            pm.setComponentEnabledSetting(
                component,
                if (mode == effectiveMode) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}

/**
 * First-run sequence: splash (brand logo matching the active icon mode + permission
 * request) → bootstrap installer (first launch only) → main app.
 */
@Composable
private fun AuraStudioStartup(
    iconMode: Int,
    isSystemDark: Boolean,
    mainContent: @Composable (StartupPhase) -> Unit,
) {
    var phase by remember { mutableStateOf(StartupPhase.Splash) }
    val context = LocalContext.current
    val coordinator = remember { BootstrapCoordinator(context as Activity) }

    when (phase) {
        StartupPhase.Splash -> {
            SplashScreen(
                useDarkLogo = effectiveIconIsDark(iconMode, isSystemDark),
                onFinished = {
                    phase = if (TermuxInstaller.isBootstrapInstalled()) StartupPhase.Main else StartupPhase.Bootstrap
                },
            )
        }

        StartupPhase.Bootstrap -> {
            val state by coordinator.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                coordinator.start(postSetup = { coordinator.finishSetup() })
            }
            BootstrapSetupScreen(
                state = state,
                onDone = { phase = StartupPhase.Main },
                onRetry = { coordinator.retry(postSetup = { coordinator.finishSetup() }) },
                onSkip = { phase = StartupPhase.Main },
            )
        }

        StartupPhase.Main -> mainContent(phase)
    }
}

private fun effectiveIconIsDark(iconMode: Int, isDark: Boolean): Boolean = when (iconMode) {
    ICON_DARK -> true
    ICON_LIGHT -> false
    else -> isDark
}

@Composable
fun AuraStudioApp(
    themeMode: Int,
    iconMode: Int,
    onThemeChange: (Int) -> Unit,
    onIconChange: (Int) -> Unit,
    dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(LocalContext.current.applicationContext)
    )
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var previousScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
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
        if (targetScreen == Screen.CreateProject && currentScreen != Screen.CreateProject) {
            previousScreen = currentScreen
        }
        if (currentScreen == Screen.CreateProject && targetScreen == Screen.CreateProject) {
            currentScreen = previousScreen
        } else {
            currentScreen = targetScreen
        }
    }

    val bottomNavItemsLocalized = bottomNavItems.map {
        com.aurastudio.ui.components.BottomNavItem(
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
                        initialCommand = terminalCommand,
                        scaffoldPadding = innerPadding
                    )
                }
                is Screen.Settings -> {
                    com.aurastudio.ui.screens.settings.SettingsScreen(
                        scaffoldPadding = innerPadding,
                        themeMode = themeMode,
                        iconMode = iconMode,
                        onThemeChange = onThemeChange,
                        onIconChange = onIconChange
                    )
                }
            }
        }
    }
}