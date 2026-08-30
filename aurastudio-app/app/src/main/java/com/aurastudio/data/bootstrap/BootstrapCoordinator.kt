package com.aurastudio.data.bootstrap

import android.app.Activity
import com.aurastudio.R
import com.termux.app.TermuxInstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Drives the Termux bootstrap installation from a Composable UI, streaming
 * phase + log lines into a [StateFlow] the UI can collect.
 */
class BootstrapCoordinator(private val activity: Activity) {

    private val _state = MutableStateFlow(BootstrapState())
    val state: StateFlow<BootstrapState> = _state.asStateFlow()

    private var started = false

    fun start(postSetup: () -> Unit = {}) {
        if (started) return
        started = true
        _state.value = BootstrapState(running = true, phase = activity.getString(R.string.bootstrap_status_checking))

        TermuxInstaller.installBootstrap(
            activity,
            {
                _state.update { it.copy(running = false, done = true, phase = activity.getString(R.string.bootstrap_status_complete)) }
                postSetup()
            },
            object : TermuxInstaller.TermuxBootstrapInstallerListener {
                override fun onStatus(message: String) {
                    _state.update { it.copy(phase = message, running = true) }
                }

                override fun onExtractProgress(entryName: String) {
                    _state.update { it.copy(extractedFiles = it.extractedFiles + 1) }
                }

                override fun onTerminalLine(line: String) {
                    _state.update { s ->
                        val log = (s.log + line).takeLast(MAX_LOG_LINES)
                        s.copy(log = log)
                    }
                }

                override fun onError(title: String, message: String) {
                    _state.update { it.copy(running = false, errorTitle = title, errorMessage = message) }
                }
            },
        )
    }

    fun retry(postSetup: () -> Unit = {}) {
        started = false
        start(postSetup)
    }

    fun finishSetup() {
        // Best-effort storage symlinks, mirroring what TermuxActivity normally sets up.
        try {
            TermuxInstaller.setupStorageSymlinks(activity)
        } catch (_: Throwable) {
            // Non-fatal; user can re-trigger from the terminal later.
        }
    }

    private companion object {
        const val MAX_LOG_LINES = 400
    }
}