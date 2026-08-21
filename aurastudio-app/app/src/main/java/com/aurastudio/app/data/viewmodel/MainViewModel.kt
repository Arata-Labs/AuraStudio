package com.aurastudio.app.data.viewmodel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurastudio.app.data.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainViewModel : ViewModel() {

    private val _terminalState = MutableStateFlow(TerminalState())
    val terminalState: StateFlow<TerminalState> = _terminalState.asStateFlow()

    private val _currentTab = MutableStateFlow(EditorTab.TERMINAL)
    val currentTab: StateFlow<EditorTab> = _currentTab.asStateFlow()

    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree: StateFlow<List<FileNode>> = _fileTree.asStateFlow()

    private val _currentFile = MutableStateFlow<String?>(null)
    val currentFile: StateFlow<String?> = _currentFile.asStateFlow()

    private val _fileContent = MutableStateFlow("")
    val fileContent: StateFlow<String> = _fileContent.asStateFlow()

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private var currentDir = File(Environment.getExternalStorageDirectory(), "")
    private var process: Process? = null

    init {
        loadFileTree()
    }

    fun setTab(tab: EditorTab) {
        _currentTab.value = tab
    }

    // ── Terminal ──────────────────────────────────────────────
    fun executeCommand(command: String) {
        val state = _terminalState.value
        _terminalState.value = state.copy(
            lines = state.lines + TerminalLine("$ ${command.trim()}", LineType.INPUT),
            history = state.history + command.trim(),
            isRunning = true
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val shellEnv = buildMap {
                    put("HOME", System.getProperty("user.home") ?: "/data/data/com.termux/files/home")
                    put("PATH", "/data/data/com.termux/files/usr/bin:${System.getenv("PATH") ?: ""}")
                    put("PREFIX", "/data/data/com.termux/files/usr")
                    put("ANDROID_HOME", "/data/data/com.termux/files/home/android-sdk")
                    put("ANDROID_SDK_ROOT", "/data/data/com.termux/files/home/android-sdk")
                }

                val pb = ProcessBuilder("sh", "-c", command.trim())
                pb.directory(currentDir)
                pb.environment().putAll(shellEnv)
                pb.redirectErrorStream(true)

                process = pb.start()
                val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                val output = mutableListOf<String>()

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { output.add(it) }
                }

                val exitCode = process?.waitFor() ?: -1

                withContext(Dispatchers.Main) {
                    val current = _terminalState.value
                    val newLines = current.lines + output.map {
                        TerminalLine(it, LineType.OUTPUT)
                    }
                    if (exitCode != 0 && output.isEmpty()) {
                        _terminalState.value = current.copy(
                            lines = newLines + TerminalLine("Exit code: $exitCode", LineType.ERROR),
                            isRunning = false
                        )
                    } else {
                        _terminalState.value = current.copy(
                            lines = newLines,
                            isRunning = false
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val current = _terminalState.value
                    _terminalState.value = current.copy(
                        lines = current.lines + TerminalLine("Error: ${e.message}", LineType.ERROR),
                        isRunning = false
                    )
                }
            }
        }
    }

    fun cancelCommand() {
        process?.destroy()
        process = null
        _terminalState.value = _terminalState.value.copy(isRunning = false)
    }

    fun clearTerminal() {
        _terminalState.value = TerminalState()
    }

    // ── File Manager ──────────────────────────────────────────
    fun loadFileTree() {
        viewModelScope.launch(Dispatchers.IO) {
            val tree = buildFileNode(currentDir)
            withContext(Dispatchers.Main) {
                _fileTree.value = listOf(tree)
            }
        }
    }

    private fun buildFileNode(dir: File): FileNode {
        val children = dir.listFiles()
            ?.filter { !it.name.startsWith(".") }
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?.take(100)
            ?.map { file ->
                if (file.isDirectory) {
                    buildFileNode(file)
                } else {
                    FileNode(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = false,
                        size = file.length()
                    )
                }
            } ?: emptyList()

        return FileNode(
            name = dir.name.ifEmpty { "/" },
            path = dir.absolutePath,
            isDirectory = true,
            children = children
        )
    }

    fun openFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (file.exists() && file.length() < 512_000) {
                    val content = file.readText()
                    withContext(Dispatchers.Main) {
                        _currentFile.value = path
                        _fileContent.value = content
                        _currentTab.value = EditorTab.EDITOR
                    }
                }
            } catch (e: Exception) {
                // ignore binary files
            }
        }
    }

    fun navigateTo(path: String) {
        currentDir = File(path)
        loadFileTree()
    }

    fun navigateUp() {
        currentDir = currentDir.parentFile ?: currentDir
        loadFileTree()
    }

    // ── AuraStudio Integration ────────────────────────────────
    fun runAuraStudio(command: String) {
        executeCommand("aurastudio $command")
    }
}
