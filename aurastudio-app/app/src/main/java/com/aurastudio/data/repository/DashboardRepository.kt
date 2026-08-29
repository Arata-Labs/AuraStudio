package com.aurastudio.data.repository

import android.content.Context
import com.aurastudio.data.models.EnvironmentStatus
import com.aurastudio.data.models.StatusJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

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

    suspend fun getEnvironmentStatus(): Result<EnvironmentStatus> = withContext(Dispatchers.IO) {
        try {
            val bashPath = "$prefix/bin/bash"
            if (!File(bashPath).exists()) {
                return@withContext Result.failure(IllegalStateException("Bootstrap not installed: bash not found at $bashPath"))
            }

            val process = ProcessBuilder(
                bashPath,
                "-c",
                "aurastudio status --json 2>/dev/null"
            ).apply {
                val env = environment()
                env["PREFIX"] = prefix
                env["TERMUX_PREFIX"] = prefix
                env["HOME"] = home
                env["PATH"] = "$prefix/bin:$prefix/bin/applets"
                env["LD_LIBRARY_PATH"] = "$prefix/lib"
                env["TMPDIR"] = "$prefix/tmp"
            }.start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            if (exitCode == 0 && output.isNotBlank()) {
                // Find start of JSON object { in case of terminal escapes
                val jsonStart = output.indexOf('{')
                val jsonEnd = output.lastIndexOf('}')
                if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                    val cleanJson = output.substring(jsonStart, jsonEnd + 1)
                    val statusJson = jsonParser.decodeFromString<StatusJson>(cleanJson)
                    val rawStatus = EnvironmentStatus.fromStatusJson(statusJson)
                    val envStatus = rawStatus.copy(
                        aapt2 = com.aurastudio.data.models.InstalledComponent(
                            "AAPT2",
                            null,
                            File("$prefix/bin/aapt2").exists()
                        ),
                        cmdlineTools = com.aurastudio.data.models.InstalledComponent(
                            "cmdline-tools",
                            null,
                            File("$sdkDir/cmdline-tools/latest").isDirectory
                        ),
                        buildTools = listDir("$sdkDir/build-tools"),
                        platforms = listDir("$sdkDir/platforms")
                    )
                    val finalStatus = envStatus.copy(
                        healthScore = EnvironmentStatus.calculateHealthScore(envStatus)
                    )
                    Result.success(finalStatus)
                } else {
                    Result.failure(IllegalStateException("No valid JSON found in output"))
                }
            } else {
                Result.failure(IllegalStateException("Command failed with exit code $exitCode: $output"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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