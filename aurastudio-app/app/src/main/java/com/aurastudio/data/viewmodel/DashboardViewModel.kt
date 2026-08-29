package com.aurastudio.data.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aurastudio.data.models.*
import com.aurastudio.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val context: Context
) : ViewModel() {

    private val repository: DashboardRepository = DashboardRepository(context)

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
            
            repository.getEnvironmentStatus().fold(
                onSuccess = { envStatus ->
                    _status.value = envStatus
                },
                onFailure = {
                    // Fallback to basic check if command failed
                }
            )

            _isRefreshing.value = false
        }
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
