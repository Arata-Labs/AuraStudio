package com.aurastudio.data.repository

import android.content.Context
import com.aurastudio.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL

class DashboardRepository(private val context: Context) {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val filesDir: String
        get() = context.filesDir.absolutePath

    private val prefix: String
        get() = "$filesDir/usr"

    private val home: String
        get() = "$filesDir/home"

    private val sdkDir: String
        get() = "$home/android-sdk"

    private val packagesJsonUrl = "https://arata-labs.github.io/aurastudio-termux/packages.json"

    suspend fun getEnvironmentStatus(): Result<EnvironmentStatus> = withContext(Dispatchers.IO) {
        try {
            val packages = fetchPackagesSync()

            // Detect all installed JDKs under $PREFIX/lib/jvm/
            val jvmDir = File("$prefix/lib/jvm")
            val installedJdks = if (jvmDir.isDirectory) {
                jvmDir.listFiles()
                    ?.filter { it.isDirectory && File(it, "bin/java").exists() }
                    ?.map { 
                        val name = it.name
                        when {
                            name.contains("21") -> "21.0.12"
                            name.contains("17") -> "17.0.20"
                            else -> name
                        }
                    } ?: emptyList()
            } else {
                if (File("$prefix/bin/java").exists()) listOf("21.0.12") else emptyList()
            }

            val javaInstalled = installedJdks.isNotEmpty()
            val javaVersion = installedJdks.firstOrNull() ?: findPackageVersion(packages, "openjdk-21") ?: findPackageVersion(packages, "openjdk-17")
            val gradleInstalled = File("$prefix/bin/gradle").exists() || File("$prefix/share/gradle").isDirectory
            val gradleVersion = findPackageVersion(packages, "gradle")
            val aapt2Installed = File("$prefix/bin/aapt2").exists()
            val aapt2Version = findPackageVersion(packages, "aapt2")

            val cmdlineToolsDir = File("$sdkDir/cmdline-tools")
            val sdkToolsInstalled = cmdlineToolsDir.isDirectory && cmdlineToolsDir.listFiles()?.any { it.isDirectory } == true
            val cmdlineToolsVersion = if (sdkToolsInstalled) cmdlineToolsDir.listFiles()?.maxByOrNull { it.name }?.name else null

            val status = EnvironmentStatus(
                java = InstalledComponent(
                    name = "Java OpenJDK",
                    version = if (javaInstalled) javaVersion else null,
                    isInstalled = javaInstalled,
                    availableVersions = listOfNotNull(findPackageVersion(packages, "openjdk-21"), findPackageVersion(packages, "openjdk-17")),
                    installedVersions = installedJdks
                ),
                gradle = InstalledComponent(
                    name = "Gradle",
                    version = if (gradleInstalled) gradleVersion else null,
                    isInstalled = gradleInstalled,
                    availableVersions = listOfNotNull(gradleVersion)
                ),
                aapt2 = InstalledComponent(
                    name = "AAPT2",
                    version = if (aapt2Installed) aapt2Version else null,
                    isInstalled = aapt2Installed,
                    availableVersions = listOfNotNull(aapt2Version)
                ),
                cmdlineTools = InstalledComponent(
                    name = "cmdline-tools",
                    version = cmdlineToolsVersion,
                    isInstalled = sdkToolsInstalled,
                    availableVersions = listOf("12.0")
                ),
                platforms = listDir("$sdkDir/platforms"),
                buildTools = listDir("$sdkDir/build-tools"),
                ndk = listDir("$sdkDir/ndk"),
                cmake = listDir("$sdkDir/cmake"),
                healthScore = 0
            )

            Result.success(status.copy(
                healthScore = EnvironmentStatus.calculateHealthScore(status)
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchPackagesSync(): List<PackageInfo> {
        return try {
            val url = URL(packagesJsonUrl)
            val text = url.readText()
            jsonParser.decodeFromString(text)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun findPackageVersion(packages: List<PackageInfo>, name: String): String? {
        return packages.find { it.name == name }?.version
    }

    private fun listDir(path: String): List<String> {
        val dir = File(path)
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sortedDescending() ?: emptyList()
    }

    fun listJavaVersions(): List<String> {
        val jvmDir = File("$prefix/lib/jvm")
        return if (jvmDir.isDirectory) {
            jvmDir.listFiles()
                ?.filter { it.isDirectory && File(it, "bin/java").exists() }
                ?.map { 
                    val name = it.name
                    when {
                        name.contains("21") -> "21.0.12"
                        name.contains("17") -> "17.0.20"
                        else -> name
                    }
                } ?: emptyList()
        } else {
            emptyList()
        }
    }
}

@kotlinx.serialization.Serializable
data class PackageInfo(
    val name: String = "",
    val version: String = "",
    val desc: String = "",
    val arch: List<String> = emptyList()
)