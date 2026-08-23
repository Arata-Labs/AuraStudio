package com.hinohara.aurastudio.terminal

import android.content.Context
import android.os.Environment
import java.io.File

object ShellEnvironment {

    fun getEnvironment(context: Context): Array<String> {
        val prefixDir = BootstrapInstaller.getPrefixDir(context)
        val homeDir = BootstrapInstaller.getHomeDir(context)

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
            "PATH=$prefixDir/bin:$androidRoot/bin:$androidRoot/xbin",
            "PWD=$homeDir",
            "LD_LIBRARY_PATH=",
            "ANDROID_ROOT=$androidRoot",
            "ANDROID_DATA=$androidData",
            "EXTERNAL_STORAGE=$externalStorage",
            "BOOTCLASSPATH=$bootClassPath",
            "PACKAGE_NAME=${context.packageName}",
            "APP_NAME=AuraStudio",
            "BASH_SILENCE_DEPRECATION_WARNING=1"
        )
    }

    fun getShellPath(context: Context): String {
        val bash = "${BootstrapInstaller.getPrefixDir(context)}/bin/bash"
        if (File(bash).exists()) return bash
        return "/system/bin/sh"
    }

    fun getWorkingDirectory(context: Context): String {
        val homeDir = BootstrapInstaller.getHomeDir(context)
        return if (File(homeDir).exists()) homeDir else context.filesDir.absolutePath
    }
}
