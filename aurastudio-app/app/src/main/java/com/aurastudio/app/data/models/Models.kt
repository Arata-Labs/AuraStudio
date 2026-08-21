package com.aurastudio.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val name: String,
    val path: String,
    val type: ProjectType,
    val lastModified: Long = System.currentTimeMillis()
)

@Serializable
enum class ProjectType {
    NATIVE_CPP,
    NDK_SHARED_LIB,
    GRADLE_JAVA,
    GRADLE_KOTLIN
}

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList(),
    val size: Long = 0
)

data class TerminalState(
    val lines: List<TerminalLine> = emptyList(),
    val currentInput: String = "",
    val isRunning: Boolean = false,
    val history: List<String> = emptyList()
)

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.OUTPUT
)

enum class LineType {
    INPUT,
    OUTPUT,
    ERROR,
    SYSTEM
}

enum class EditorTab(val label: String) {
    EDITOR("Editor"),
    TERMINAL("Terminal"),
    FILES("Files")
}
