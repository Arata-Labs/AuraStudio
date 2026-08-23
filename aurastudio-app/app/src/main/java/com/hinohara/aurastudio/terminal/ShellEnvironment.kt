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
        val lang = System.getenv("LANG") ?: "en_US.UTF-8"

        return arrayOf(
            "HOME=$homeDir",
            "PREFIX=$prefixDir",
            "SYSROOT=$prefixDir",
            "TMPDIR=$prefixDir/tmp",
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "LANG=$lang",
            "PATH=$prefixDir/bin",
            "PWD=$homeDir",
            "LD_LIBRARY_PATH=$prefixDir/lib",
            "ANDROID_ROOT=$androidRoot",
            "ANDROID_DATA=$androidData",
            "EXTERNAL_STORAGE=$externalStorage",
            "BOOTCLASSPATH=$bootClassPath",
            "PACKAGE_NAME=${context.packageName}",
            "APP_NAME=AuraStudio",
            "BASH_SILENCE_DEPRECATION_WARNING=1",
            "TERMUX_PKG_NO_MIRROR_SELECT=true"
        )
    }

    fun getShellPath(context: Context): String {
        val login = File(BootstrapInstaller.getPrefixDir(context), "bin/login")
        if (login.exists() && login.canExecute()) return login.absolutePath
        val bash = File(BootstrapInstaller.getPrefixDir(context), "bin/bash")
        if (bash.exists() && bash.canExecute()) return bash.absolutePath
        return "/system/bin/sh"
    }

    fun getShellArgs(context: Context): Array<String> {
        val shellPath = getShellPath(context)
        return if (shellPath.endsWith("login")) {
            arrayOf("-l")
        } else {
            arrayOf("--norc", "--noprofile")
        }
    }

    fun getWorkingDirectory(context: Context): String {
        val homeDir = BootstrapInstaller.getHomeDir(context)
        return if (File(homeDir).exists()) homeDir else context.filesDir.absolutePath
    }
}
