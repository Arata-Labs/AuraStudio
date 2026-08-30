package com.aurastudio.data.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aurastudio.R
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
    val isUninstall: Boolean = false,
    val error: String = ""
)

private val aptNoise = Regex(
    "^(" +
        "Reading package lists|" +
        "Building dependency tree|" +
        "Reading state information|" +
        "Need to get|" +
        "After this operation|" +
        "The following .* packages|" +
        "Selecting previously unselected|" +
        "Fetched |" +
        "Get:|Hit:|Ign:|" +
        "WARNING: apt does not have a stable CLI interface" +
        ")",
    RegexOption.IGNORE_CASE
)

private val sdkmanagerNoise = Regex(
    "^(" +
        "Loading (local|SDK) repository|" +
        "\\[?\\.Info\\.?\\s*\\]?:|" +
        "Warning: (File|Unable|No such)" +
        ")",
    RegexOption.IGNORE_CASE
)

/** Progress meter updates (sdkmanager/apt write these with \r, no newline). */
private val progressMeter = Regex(
    "^\\s*(\\[[=\\s]*\\]\\s*\\d+%|\\d+%)\\s*(.*)$"
)

/** True when the line is a live progress-marker, not a stable milestone. */
private fun isProgressMarker(line: String): Boolean =
    progressMeter.matches(line) || line.matches(Regex("^[=\\-\\s]+>?\\s*$"))

/** True when a raw line must always survive filtering (real problems). */
private fun isCriticalLine(line: String): Boolean =
    line.contains("error", ignoreCase = true) ||
        line.startsWith("E:") ||
        line.contains("failed", ignoreCase = true) ||
        line.contains("exception", ignoreCase = true)

/** Reduce raw tool output into a tidy progress log. Returns null to drop the line. */
private fun cleanInstallLine(raw: String): String? {
    val line = raw.trim()
    if (line.isEmpty()) return null
    if (isCriticalLine(line)) return line
    if (aptNoise.containsMatchIn(line)) return null
    if (sdkmanagerNoise.containsMatchIn(line)) return null
    if (line.matches(Regex("^\\d+%.*"))) return null
    if (line.matches(Regex("^[\\d\\s\\u0008\\r%-]+$"))) return null
    if (line.matches(Regex("^[=\\-]+$"))) return null
    return line
}

/**
 * Build a compact status line from a \r progress meter chunk, e.g.
 * "[==                  ] 45% Computing updates..." -> "45% Computing updates…".
 */
private fun progressText(raw: String): String? {
    val match = progressMeter.find(raw)
    if (match == null) {
        return if (isProgressMarker(raw)) raw.trim().take(60) else null
    }
    val pct = match.groupValues[1].trim()
    val trail = match.groupValues[2].trim()
    return (if (trail.isEmpty()) pct else "$pct $trail").take(60)
}

/**
 * Append a clean line to the log, dropping consecutive duplicates and capping size.
 * Live progress markers ([=== ] 45%) update / replace the previous marker instead
 * of being appended, so installs read as flowing status without spam.
 */
private fun appendCleanLine(log: List<String>, raw: String): List<String> {
    val progress = progressText(raw)
    if (progress != null) {
        return if (log.isNotEmpty() && isProgressMarker(log.last())) {
            log.dropLast(1) + progress
        } else {
            (log + progress).takeLast(300)
        }
    }
    val clean = cleanInstallLine(raw)
    if (clean == null) return log
    if (log.isNotEmpty() && log.last() == clean) return log
    return (log + clean).takeLast(300)
}

class DashboardViewModel(private val context: Context) : ViewModel() {

    private val repository = DashboardRepository(context)
    private val installer = PackageInstaller(context)

    private val _status = MutableStateFlow(
        EnvironmentStatus(
            java = InstalledComponent("Java OpenJDK", null, false),
            gradle = InstalledComponent("Gradle", null, false),
            aapt2 = InstalledComponent("AAPT2", null, false),
            cmdlineTools = InstalledComponent("cmdline-tools", null, false),
            platformTools = InstalledComponent("platform-tools", null, false),
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
        // Components managed by sdkmanager require both Java and cmdline-tools first.
        val needsSdkmanager = componentKey == "platforms" || componentKey == "build_tools" || componentKey == "platform_tools"
        if (needsSdkmanager) {
            val statusNow = _status.value
            if (!statusNow.java.isInstalled) {
                _installState.value = InstallState(
                    componentName = componentName,
                    version = version,
                    log = listOf(
                        context.getString(R.string.env_dialog_needs_java)
                    ),
                    isInstalling = false,
                    isFinished = true,
                    isSuccess = false,
                    error = context.getString(R.string.env_dialog_err_java_required, componentName)
                )
                return
            }
            if (!statusNow.cmdlineTools.isInstalled) {
                _installState.value = InstallState(
                    componentName = componentName,
                    version = version,
                    log = listOf(
                        context.getString(R.string.env_dialog_needs_cmdline)
                    ),
                    isInstalling = false,
                    isFinished = true,
                    isSuccess = false,
                    error = context.getString(R.string.env_dialog_err_cmdline_required, componentName)
                )
                return
            }
        }
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
                    current.copy(log = appendCleanLine(current.log, event.line))
                }
            }
            refreshStatus()
        }
    }

    fun startUninstall(componentKey: String, version: String, componentName: String) {
        installJob?.cancel()
        _installState.value = InstallState(
            componentName = componentName,
            version = version,
            log = emptyList(),
            isUninstall = true
        )
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
                    current.copy(log = appendCleanLine(current.log, event.line))
                }
            }
            refreshStatus()
        }
    }

    fun switchJavaVersion(version: String) {
        installJob?.cancel()
        _installState.value = InstallState(
            componentName = context.getString(R.string.env_component_java_version, version),
            version = version,
            log = emptyList()
        )
        installJob = viewModelScope.launch {
            val command = installer.switchJavaCommand(version)
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
                    current.copy(log = appendCleanLine(current.log, event.line))
                }
            }
            refreshStatus()
        }
    }

    fun dismissInstallDialog() {
        installJob?.cancel()
        _installState.value = null
        refreshStatus()
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
