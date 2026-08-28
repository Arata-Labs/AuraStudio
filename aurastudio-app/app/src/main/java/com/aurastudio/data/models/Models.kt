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
)


