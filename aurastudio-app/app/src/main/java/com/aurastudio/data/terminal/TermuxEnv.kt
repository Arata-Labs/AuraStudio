package com.aurastudio.data.terminal

import android.content.Context
import java.io.File

/**
 * Builds the environment variables the embedded prefix expects, mirroring the
 * CLI's PREFIX/HOME/PATH conventions. Shared by the in-project terminal
 * session and by the build runner so both see the same toolchain.
 */
object TermuxEnv {

    fun prefix(context: Context): File = File(context.filesDir, "usr")

    fun homeDir(context: Context): File = File(context.filesDir, "home")

    fun toMap(context: Context): Map<String, String> {
        val prefixPath = prefix(context).absolutePath
        val homePath = homeDir(context).absolutePath
        val env = LinkedHashMap<String, String>()
        env["HOME"] = homePath
        env["PREFIX"] = prefixPath
        env["TERMUX_PREFIX"] = prefixPath
        env["PATH"] = "$prefixPath/bin:$prefixPath/bin/applets"
        env["LD_LIBRARY_PATH"] = "$prefixPath/lib"
        env["TMPDIR"] = "$prefixPath/tmp"
        env["TERM"] = "xterm-256color"
        env["SHELL"] = "$prefixPath/bin/bash"
        env["LANG"] = "en_US.UTF-8"
        env["LC_ALL"] = "en_US.UTF-8"
        env["CHARSET"] = "UTF-8"
        env["TERMUX_APP_PACKAGE"] = "com.aurastudio"
        prefix(context).let { p ->
            File(p, "lib/jvm").listFiles()?.firstOrNull {
                File(it, "bin/java").isFile
            }?.let {
                env["JAVA_HOME"] = it.absolutePath
            }
        }
        listOf(
            "ANDROID_DATA",
            "ANDROID_ROOT",
            "ANDROID_ART_ROOT",
            "ANDROID_RUNTIME_ROOT",
            "ANDROID_CPU_ABI",
            "ANDROID_CPU_ABI2"
        ).forEach { key ->
            if (env[key] == null && !System.getenv(key).isNullOrEmpty()) {
                env[key] = System.getenv(key)
            }
        }
        return env
    }

    /** "KEY=VALUE" pairs as accepted by [com.termux.terminal.TerminalSession]. */
    fun toArray(context: Context): Array<String> =
        toMap(context).map { "${it.key}=${it.value}" }.toTypedArray()
}