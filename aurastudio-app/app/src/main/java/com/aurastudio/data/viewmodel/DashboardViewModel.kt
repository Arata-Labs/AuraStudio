package com.aurastudio.data.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aurastudio.data.models.*
import com.aurastudio.data.repository.DashboardRepository
import com.aurastudio.data.repository.InstallEvent
import com.aurastudio.data.repository.PackageInstaller
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InstallState(
    val componentName: String,
    val version: String,
    val log: List<String>,
    val isInstalling: Boolean = true,
    val isFinished: Boolean = false,
    val isSuccess: Boolean = true,
    val error: String = ""
)

class DashboardViewModel(private val context: Context) : ViewModel() {

    private val repository = DashboardRepository(context)
    private val installer = PackageInstaller(context)

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

    private val _installState = MutableStateFlow<InstallState?>(null)
    val installState: StateFlow<InstallState?> = _installState.asStateFlow()

    private var installJob: Job? = null

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.getEnvironmentStatus().fold(
                onSuccess = { _status.value = it },
                onFailure = { /* keep current */ }
            )
            _isRefreshing.value = false
        }
    }

    fun startInstall(componentKey: String, version: String, componentName: String) {
        installJob?.cancel()
        _installState.value = InstallState(componentName = componentName, version = version, log = emptyList())
        installJob = viewModelScope.launch {
            val command = installer.installCommand(componentKey, version)
            installer.run(command).collect { event ->
                val current = _installState.value ?: return@collect
                _installState.value = if (event.isFinished) {
                    current.copy(
                        isInstalling = false,
                        isFinished = true,
                        isSuccess = event.isSuccess,
                        error = event.error
                    )
                } else {
                    current.copy(log = current.log + event.line)
                }
            }
            refreshStatus()
        }
    }

    fun startUninstall(componentKey: String, version: String, componentName: String) {
        installJob?.cancel()
        _installState.value = InstallState(componentName = componentName, version = version, log = emptyList())
        installJob = viewModelScope.launch {
            val command = installer.uninstallCommand(componentKey, version)
            installer.run(command).collect { event ->
                val current = _installState.value ?: return@collect
                _installState.value = if (event.isFinished) {
                    current.copy(
                        isInstalling = false,
                        isFinished = true,
                        isSuccess = event.isSuccess,
                        error = event.error
                    )
                } else {
                    current.copy(log = current.log + event.line)
                }
            }
            refreshStatus()
        }
    }

    fun dismissInstallDialog() {
        installJob?.cancel()
        _installState.value = null
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(context) as T
            }
        }
    }
}
