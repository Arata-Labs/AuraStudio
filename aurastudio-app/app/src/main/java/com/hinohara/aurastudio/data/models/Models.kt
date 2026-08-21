package com.hinohara.aurastudio.data.models

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

enum class DashboardQuickAction(val label: String, val icon: String) {
    SETUP("Setup", "rocket"),
    INSTALL_SDK("Install SDK", "download"),
    INSTALL_NDK("Install NDK", "cpu"),
    DOCTOR("Doctor", "stethoscope"),
    STATUS("Status", "bar_chart"),
    CLEAN("Clean", "cleaning_services")
}
