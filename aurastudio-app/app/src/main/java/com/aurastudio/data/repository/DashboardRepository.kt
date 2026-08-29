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
            // Probe local installations
            val javaInstalled = File("$prefix/bin/java").exists()
            val gradleInstalled = File("$prefix/bin/gradle").exists() || 
                File("$prefix/share/gradle").isDirectory
            val aapt2Installed = File("$prefix/bin/aapt2").exists()
            val sdkToolsInstalled = File("$sdkDir/cmdline-tools/latest").isDirectory
            val buildTools = listDir("$sdkDir/buildTools")
            val platforms = listDir("$sdkDir/platforms")
            val ndk = listDir("$sdkDir/ndk")
            val cmake = listDir("$sdkDir/cmake")

            // Fetch available versions from packages.json
            val packages = fetchPackages()
            val javaVersion = findPackageVersion(packages, "openjdk-21") ?: findPackageVersion(packages, "openjdk-17")
            val aapt2Version = findPackageVersion(packages, "aapt2")
            val gradleVersion = findPackageVersion(packages, "gradle")

            val status = EnvironmentStatus(
                java = InstalledComponent(
                    name = "Java OpenJDK",
                    version = if (javaInstalled) javaVersion else javaVersion,
                    isInstalled = javaInstalled
                ),
                gradle = InstalledComponent(
                    name = "Gradle",
                    version = if (gradleInstalled) gradleVersion else gradleVersion,
                    isInstalled = gradleInstalled
                ),
                aapt2 = InstalledComponent(
                    name = "AAPT2",
                    version = if (aapt2Installed) aapt2Version else aapt2Version,
                    isInstalled = aapt2Installed
                ),
                cmdlineTools = InstalledComponent(
                    name = "cmdline-tools",
                    version = null,
                    isInstalled = sdkToolsInstalled
                ),
                platforms = platforms,
                buildTools = buildTools,
                ndk = ndk,
                cmake = cmake,
                healthScore = 0
            )

            Result.success(status.copy(
                healthScore = EnvironmentStatus.calculateHealthScore(status)
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchPackages(): List<PackageInfo> {
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
}

@kotlinx.serialization.Serializable
data class PackageInfo(
    val name: String = "",
    val version: String = "",
    val desc: String = "",
    val arch: List<String> = emptyList()
)
