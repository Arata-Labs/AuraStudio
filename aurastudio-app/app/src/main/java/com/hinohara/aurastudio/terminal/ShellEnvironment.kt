package com.hinohara.aurastudio.terminal

import android.content.Context
import android.os.Environment
import java.io.File

object ShellEnvironment {
    fun getEnvironment(context: Context): Array<String> {
        val filesDir = context.filesDir.absolutePath
        val homeDir = "$filesDir/home"
        val prefixDir = "$filesDir/usr"

        // Ensure directories exist
        File(homeDir).mkdirs()
        File(prefixDir).mkdirs()
        File("$prefixDir/bin").mkdirs()
        File("$prefixDir/tmp").mkdirs()

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
            "PATH=/data/data/com.termux/files/usr/bin:$androidRoot/bin:$androidRoot/xbin",
            "PWD=$homeDir",
            "LD_LIBRARY_PATH=",
            "ANDROID_ROOT=$androidRoot",
            "ANDROID_DATA=$androidData",
            "EXTERNAL_STORAGE=$externalStorage",
            "BOOTCLASSPATH=$bootClassPath",
            "PACKAGE_NAME=${context.packageName}",
            "LANG=en_US.UTF-8",
            "TMP=$prefixDir/tmp"
        )
    }

    fun getShellPath(): String {
        // Prefer Termux bash if available, fallback to Android sh
        val termuxBash = "/data/data/com.termux/files/usr/bin/bash"
        if (File(termuxBash).exists()) return termuxBash
        return "/system/bin/sh"
    }

    fun getWorkingDirectory(context: Context): String {
        val homeDir = "${context.filesDir.absolutePath}/home"
        return if (File(homeDir).exists()) homeDir else context.filesDir.absolutePath
    }
}
