package com.hinohara.aurastudio.terminal

import android.content.Context
import android.os.Environment
import java.io.File

object ShellEnvironment {

    private val TERMUX_PACKAGES = listOf(
        "com.termux",
        "com.termux.api",
        "com.termux.boot",
        "com.termux.widget",
        "com.termux.x11"
    )

    private fun detectTermuxPrefix(): String? {
        for (pkg in TERMUX_PACKAGES) {
            val prefix = "/data/data/$pkg/files/usr"
            if (File(prefix, "bin/bash").exists()) return prefix
        }
        return null
    }

    fun getEnvironment(context: Context): Array<String> {
        val filesDir = context.filesDir.absolutePath
        val homeDir = "$filesDir/home"
        val prefixDir = "$filesDir/usr"

        File(homeDir).mkdirs()
        File(prefixDir).mkdirs()
        File("$prefixDir/bin").mkdirs()
        File("$prefixDir/tmp").mkdirs()

        val termuxPrefix = detectTermuxPrefix()
        val appBinDir = "$prefixDir/bin"

        val path = if (termuxPrefix != null) {
            "$appBinDir:$termuxPrefix/bin:/system/bin:/system/xbin"
        } else {
            "$appBinDir:/system/bin:/system/xbin"
        }

        val androidRoot = System.getenv("ANDROID_ROOT") ?: "/system"
        val androidData = System.getenv("ANDROID_DATA") ?: "/data"
        val externalStorage = try {
            Environment.getExternalStorageDirectory().absolutePath
        } catch (_: Exception) {
            "/storage/emulated/0"
        }

        val bootClassPath = System.getenv("BOOTCLASSPATH") ?: ""

        return arrayOf(
            "HOME=$homeDir",
            "PREFIX=$prefixDir",
            "TMPDIR=$prefixDir/tmp",
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "LANG=en_US.UTF-8",
            "PATH=$path",
            "PWD=$homeDir",
            "LD_LIBRARY_PATH=",
            "ANDROID_ROOT=$androidRoot",
            "ANDROID_DATA=$androidData",
            "EXTERNAL_STORAGE=$externalStorage",
            "BOOTCLASSPATH=$bootClassPath",
            "PACKAGE_NAME=${context.packageName}",
            "APP_NAME=AuraStudio",
            "PS1=\\[\\033[1;38;2;130;170;255m\\]aurastudio\\[\\033[0m\\] \\[\\033[38;2;180;180;200m\\]\\w\\[\\033[0m\\] \\[\\033[1;38;2;130;255;170m\\]\\$\\[\\033[0m\\] ",
            "BASH_SILENCE_DEPRECATION_WARNING=1"
        )
    }

    fun getShellPath(): String {
        val termuxPrefix = detectTermuxPrefix()
        if (termuxPrefix != null) {
            val bash = "$termuxPrefix/bin/bash"
            if (File(bash).exists()) return bash
        }
        return "/system/bin/sh"
    }

    fun getWorkingDirectory(context: Context): String {
        val homeDir = "${context.filesDir.absolutePath}/home"
        return if (File(homeDir).exists()) homeDir else context.filesDir.absolutePath
    }
}
