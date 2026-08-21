package com.hinohara.aurastudio.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hinohara.aurastudio.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _status = MutableStateFlow(
        EnvironmentStatus(
            java = InstalledComponent("Java OpenJDK", null, false),
            gradle = InstalledComponent("Gradle", null, false),
            aapt2 = InstalledComponent("AAPT2", null, false),
            cmdlineTools = InstalledComponent("cmdline-tools", null, false),
            platforms = emptyList(),
            buildTools = emptyList(),
            ndk = emptyList(),
            cmake = emptyList(),
            healthScore = 0
        )
    )
    val status: StateFlow<EnvironmentStatus> = _status.asStateFlow()

    private val _recentProjects = MutableStateFlow<List<Project>>(emptyList())
    val recentProjects: StateFlow<List<Project>> = _recentProjects.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Simulate loading - in real app, execute aurastudio status --json
            kotlinx.coroutines.delay(500)
            _status.value = EnvironmentStatus(
                java = InstalledComponent("Java OpenJDK", "21.0.12", true),
                gradle = InstalledComponent("Gradle", "9.7.0", true),
                aapt2 = InstalledComponent("AAPT2", "2.20", true),
                cmdlineTools = InstalledComponent("cmdline-tools", "12.0", true),
                platforms = listOf("API 36", "API 36.1"),
                buildTools = listOf("36.0.0", "37.0.0"),
                ndk = emptyList(),
                cmake = emptyList(),
                healthScore = 75
            )
            _recentProjects.value = listOf(
                Project("MyAuraApp", "/data/data/com.termux/files/home/MyAuraApp", ProjectType.GRADLE_KOTLIN),
                Project("NativeLib", "/data/data/com.termux/files/home/NativeLib", ProjectType.NDK_SHARED_LIB),
            )
            _isRefreshing.value = false
        }
    }
}
