package com.aurastudio.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class InstallEvent(
    val isFinished: Boolean = false,
    val isSuccess: Boolean = true,
    val line: String = "",
    val error: String = ""
)

/**
 * Runs install/uninstall commands inside the app's embedded Termux prefix.
 * [java]/[aapt2]/[gradle] install via apt from the hosted repo; SDK/NDK/CMake
 * components install via the aurastudio CLI which downloads from the same
 * sources the CLI uses.
 */
class PackageInstaller(private val context: Context) {

    private val filesDir: String get() = context.filesDir.absolutePath
    private val prefix: String get() = "$filesDir/usr"
    private val home: String get() = "$filesDir/home"

    private fun buildCommand(command: String): Process {
        val bash = "$prefix/bin/bash"
        val pb = ProcessBuilder(bash, "-lc", command)
        pb.redirectErrorStream(true)
        val env = pb.environment()
        env["PREFIX"] = prefix
        env["TERMUX_PREFIX"] = prefix
        env["HOME"] = home
        env["PATH"] = "$prefix/bin:$prefix/bin/applets"
        env["LD_LIBRARY_PATH"] = "$prefix/lib"
        env["TMPDIR"] = "$prefix/tmp"
        env["TERMUX_APP_PACKAGE"] = "com.aurastudio"
        env["TERMUX_PREFIX"] = prefix
        return pb.start()
    }

    /**
     * Run [command] and stream its stdout/stderr lines via the returned flow.
     * The flow emits [InstallEvent] per line, then a final event with
     * [InstallEvent.isFinished] = true and [InstallEvent.isSuccess].
     * Terminates process on flow collector cancellation.
     */
    fun run(command: String): Flow<InstallEvent> = flow {
        if (!File("$prefix/bin/bash").exists()) {
            emit(InstallEvent(line = "Bootstrap not installed yet."))
            emit(InstallEvent(isFinished = true, isSuccess = false, error = "Bootstrap not installed"))
            return@flow
        }

        val process = buildCommand(command)
        try {
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.useLines { lines ->
                for (line in lines) {
                    emit(InstallEvent(line = line))
                }
            }
            val exitCode = process.waitFor()
            emit(InstallEvent(isFinished = true, isSuccess = exitCode == 0, error = if (exitCode == 0) "" else "Command exited with code $exitCode"))
        } catch (e: Exception) {
            emit(InstallEvent(isFinished = true, isSuccess = false, error = e.message ?: "Install failed"))
        } finally {
            process.destroy()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Map a component key + version to the shell command that installs it.
     * `aurastudio install sdk` is used only for sdkmanager-managed components
     * (platforms, build-tools); everything else installs via pkg or the CLI.
     */
    fun installCommand(componentKey: String, version: String): String = when (componentKey) {
        "java" -> if (version.contains("21")) "pkg install -y openjdk-21 2>&1 || exit 1" else "pkg install -y openjdk-17 2>&1 || exit 1"
        "gradle" -> "pkg install -y gradle 2>&1 || exit 1"
        "aapt2" -> "pkg install -y aapt2 2>&1 || exit 1"
        "cmdline_tools" -> "aurastudio setup --cmdtools-only 2>&1 || exit 1"
        "platforms" -> "aurastudio install sdk platform $version 2>&1 || exit 1"
        "build_tools" -> "aurastudio install sdk buildtools $version 2>&1 || exit 1"
        "ndk" -> "aurastudio install ndk $version 2>&1 || exit 1"
        "cmake" -> "aurastudio install cmake $version 2>&1 || exit 1"
        else -> "echo \"Unknown component: $componentKey\""
    }

    /** Map a component key + version to the shell command that uninstalls it. */
    fun uninstallCommand(componentKey: String, version: String): String = when (componentKey) {
        "java" -> "pkg remove -y openjdk-21 openjdk-17 2>&1"
        "gradle" -> "pkg remove -y gradle 2>&1"
        "aapt2" -> "pkg remove -y aapt2 2>&1"
        "cmdline_tools" -> "rm -rf $home/android-sdk/cmdline-tools 2>&1"
        "platforms" -> "rm -rf $home/android-sdk/platforms/android-$version 2>&1"
        "build_tools" -> "rm -rf $home/android-sdk/build-tools/$version 2>&1"
        "ndk" -> "rm -rf $home/android-sdk/ndk/$version 2>&1"
        "cmake" -> "rm -rf $home/android-sdk/cmake/$version 2>&1"
        else -> "echo \"Unknown component: $componentKey\""
    }

    fun switchJavaCommand(version: String): String = when {
        version == "21" -> "aurastudio use java 21 2>&1 || exit 1"
        version == "17" -> "aurastudio use java 17 2>&1 || exit 1"
        else -> "echo \"Invalid Java version: $version\""
    }
}