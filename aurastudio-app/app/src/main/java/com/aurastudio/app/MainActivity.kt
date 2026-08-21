package com.aurastudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurastudio.app.data.models.EditorTab
import com.aurastudio.app.data.viewmodel.MainViewModel
import com.aurastudio.app.ui.components.AuraStudioBottomBar
import com.aurastudio.app.ui.components.AuraStudioTopBar
import com.aurastudio.app.ui.screens.editor.EditorScreen
import com.aurastudio.app.ui.screens.editor.FileExplorerScreen
import com.aurastudio.app.ui.screens.terminal.TerminalScreen
import com.aurastudio.app.ui.theme.AuraStudioTheme
import com.aurastudio.app.ui.theme.DarkBackground

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

@Composable
fun AuraStudioApp(viewModel: MainViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsState()
    val terminalState by viewModel.terminalState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            AuraStudioTopBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        },
        bottomBar = {
            AuraStudioBottomBar(
                isRunning = terminalState.isRunning,
                onAuraStudioCommand = { viewModel.runAuraStudio(it) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentTab) {
                EditorTab.TERMINAL -> TerminalScreen(viewModel)
                EditorTab.EDITOR -> EditorScreen(viewModel)
                EditorTab.FILES -> FileExplorerScreen(viewModel)
            }
        }
    }
}
