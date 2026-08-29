package com.aurastudio.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val name: String,
    val path: String,
    val type: ProjectType
)

@Serializable
enum class ProjectType {
    NATIVE_CPP,
    NDK_SHARED_LIB,
    GRADLE_JAVA,
    GRADLE_KOTLIN
}

data class InstalledComponent(
    val name: String,
    val version: String?,
    val isInstalled: Boolean
)

data class EnvironmentStatus(
    val java: InstalledComponent,
    val gradle: InstalledComponent,
    val aapt2: InstalledComponent,
    val cmdlineTools: InstalledComponent,
    val platforms: List<String>,
    val buildTools: List<String>,
    val ndk: List<String>,
    val cmake: List<String>,
    val healthScore: Int
) {
    companion object {
        fun fromStatusJson(json: StatusJson): EnvironmentStatus {
            val javaOk = json.tools["java"] == true
            val gradleOk = json.tools["gradle"] == true
            val aapt2Ok = json.tools["aria2"] == true

            return EnvironmentStatus(
                java = InstalledComponent("Java OpenJDK", null, javaOk),
                gradle = InstalledComponent("Gradle", null, gradleOk),
                aapt2 = InstalledComponent("AAPT2", null, aapt2Ok),
                cmdlineTools = InstalledComponent("cmdline-tools", null, json.paths.sdk_dir.isNotBlank()),
                platforms = json.installed.sdk_platforms,
                buildTools = emptyList(),
                ndk = json.installed.ndk,
                cmake = json.installed.cmake,
                healthScore = calculateHealthScore(json)
            )
        }

        private fun calculateHealthScore(json: StatusJson): Int {
            var score = 0
            if (json.tools["java"] == true) score += 25
            if (json.tools["gradle"] == true) score += 20
            if (json.installed.sdk_platforms.isNotEmpty()) score += 20
            if (json.installed.ndk.isNotEmpty()) score += 15
            if (json.installed.cmake.isNotEmpty()) score += 10
            if (json.paths.sdk_dir.isNotBlank()) score += 10
            return score.coerceIn(0, 100)
        }
    }
}

@Serializable
data class StatusJson(
    val cli_version: String = "",
    val timestamp: String = "",
    val tools: Map<String, Boolean> = emptyMap(),
    val paths: StatusPaths = StatusPaths(),
    val installed: StatusInstalled = StatusInstalled()
)

@Serializable
data class StatusPaths(
    val sdk_dir: String = "",
    val ndk_dir: String = "",
    val cmake_dir: String = ""
)

@Serializable
data class StatusInstalled(
    val ndk: List<String> = emptyList(),
    val cmake: List<String> = emptyList(),
    val sdk_platforms: List<String> = emptyList()
)


