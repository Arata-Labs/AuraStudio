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
    GRADLE_KOTLIN,
    MATERIAL_YOU,
    COMPOSE,
    ANDROID_LIBRARY,
    JAVA_LIBRARY,
    KOTLIN_LIBRARY
}

data class InstalledComponent(
    val name: String,
    val version: String?,
    val isInstalled: Boolean,
    val availableVersions: List<String> = emptyList(),
    val installedVersions: List<String> = emptyList()
)

data class EnvironmentStatus(
    val java: InstalledComponent,
    val gradle: InstalledComponent,
    val aapt2: InstalledComponent,
    val cmdlineTools: InstalledComponent,
    val platformTools: InstalledComponent,
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

            return EnvironmentStatus(
                java = InstalledComponent("Java OpenJDK", null, javaOk),
                gradle = InstalledComponent("Gradle", null, gradleOk),
                aapt2 = InstalledComponent("AAPT2", null, false),
                cmdlineTools = InstalledComponent("cmdline-tools", null, false),
                platformTools = InstalledComponent("platform-tools", null, false),
                platforms = json.installed.sdk_platforms,
                buildTools = emptyList(),
                ndk = json.installed.ndk,
                cmake = json.installed.cmake,
                healthScore = 0
            )
        }

        fun calculateHealthScore(status: EnvironmentStatus): Int {
            var score = 0
            if (status.java.isInstalled) score += 20
            if (status.gradle.isInstalled) score += 20
            if (status.aapt2.isInstalled) score += 15
            if (status.cmdlineTools.isInstalled) score += 15
            if (status.platformTools.isInstalled) score += 10
            if (status.platforms.isNotEmpty()) score += 10
            if (status.buildTools.isNotEmpty()) score += 10
            if (status.ndk.isNotEmpty()) score += 5
            if (status.cmake.isNotEmpty()) score += 5
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


