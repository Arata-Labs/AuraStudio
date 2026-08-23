package com.hinohara.aurastudio.terminal

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

object BootstrapInstaller {

    private const val TAG = "BootstrapInstaller"
    private const val BOOTSTRAP_VERSION = "2026.02.12-r1"
    private const val BOOTSTRAP_VARIANT = "apt.android-7"
    private const val BOOTSTRAP_BASE_URL =
        "https://github.com/termux/termux-packages/releases/download"

    enum class State {
        NOT_INSTALLED,
        DOWNLOADING,
        EXTRACTING,
        SETTING_UP,
        READY,
        ERROR
    }

    data class Progress(
        val state: State = State.NOT_INSTALLED,
        val progress: Float = 0f,
        val message: String = "",
        val error: String? = null
    )

    fun getPrefixDir(context: Context): String {
        return "${context.filesDir.absolutePath}/usr"
    }

    fun getHomeDir(context: Context): String {
        return "${context.filesDir.absolutePath}/home"
    }

    fun isInstalled(context: Context): Boolean {
        val bash = File(getPrefixDir(context), "bin/bash")
        return bash.exists() && bash.length() > 0
    }

    fun getArchName(): String {
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when (primaryAbi) {
            "arm64-v8a" -> "aarch64"
            "armeabi-v7a" -> "arm"
            "x86" -> "i686"
            "x86_64" -> "x86_64"
            else -> "aarch64"
        }
    }

    private fun getBootstrapUrl(): String {
        val arch = getArchName()
        val encodedVariant = BOOTSTRAP_VARIANT.replace("+", "%2B")
        return "$BOOTSTRAP_BASE_URL/bootstrap-$BOOTSTRAP_VERSION%2B$encodedVariant/bootstrap-$arch.zip"
    }

    fun install(
        context: Context,
        onProgress: (Progress) -> Unit
    ): Boolean {
        if (isInstalled(context)) {
            onProgress(Progress(state = State.READY))
            return true
        }

        val prefixDir = File(getPrefixDir(context))
        val tmpDir = File(context.filesDir, "usr_tmp")

        try {
            onProgress(Progress(state = State.DOWNLOADING, message = "Downloading bootstrap..."))

            val zipFile = File(context.cacheDir, "bootstrap-${getArchName()}.zip")
            downloadBootstrap(zipFile, onProgress)

            onProgress(Progress(state = State.EXTRACTING, message = "Extracting packages..."))

            tmpDir.mkdirs()
            extractZip(zipFile, tmpDir, onProgress)

            onProgress(Progress(state = State.SETTING_UP, message = "Setting up environment..."))

            setupEnvironment(tmpDir, context)

            if (prefixDir.exists()) prefixDir.deleteRecursively()
            tmpDir.renameTo(prefixDir)

            createHomeDir(context)

            onProgress(Progress(state = State.READY, message = "Ready!"))
            zipFile.delete()
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap installation failed", e)
            tmpDir.deleteRecursively()
            onProgress(Progress(
                state = State.ERROR,
                error = "Installation failed: ${e.message}"
            ))
            return false
        }
    }

    private fun downloadBootstrap(
        outputFile: File,
        onProgress: (Progress) -> Unit
    ) {
        val url = URL(getBootstrapUrl())
        Log.d(TAG, "Downloading: $url")

        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        conn.connect()

        val totalSize = conn.contentLength.toLong()
        var downloaded = 0L

        conn.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalSize > 0) {
                        val pct = (downloaded.toFloat() / totalSize * 100).toInt()
                        onProgress(Progress(
                            state = State.DOWNLOADING,
                            progress = downloaded.toFloat() / totalSize,
                            message = "Downloading... $pct%"
                        ))
                    }
                }
            }
        }
    }

    private fun extractZip(
        zipFile: File,
        targetDir: File,
        onProgress: (Progress) -> Unit
    ) {
        val zipIn = ZipInputStream(zipFile.inputStream().buffered())
        var entryCount = 0

        zipIn.use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount % 100 == 0) {
                    onProgress(Progress(
                        state = State.EXTRACTING,
                        message = "Extracting... ($entryCount files)"
                    ))
                }

                val outFile = File(targetDir, entry.name)

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out ->
                        zip.copyTo(out)
                    }
                }

                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun setupEnvironment(prefixDir: File, context: Context) {
        val binDirs = listOf(
            File(prefixDir, "bin"),
            File(prefixDir, "libexec/apt"),
            File(prefixDir, "lib/apt"),
            File(prefixDir, "lib"),
            File(prefixDir, "share"),
        )

        for (dir in binDirs) {
            if (!dir.exists()) continue
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.setExecutable(true, false)
                    file.setReadable(true, false)
                }
            }
        }

        val tmpDir = File(prefixDir, "tmp")
        tmpDir.mkdirs()

        val varDir = File(prefixDir, "var")
        varDir.mkdirs()
        File(prefixDir, "var/tmp").mkdirs()
        File(prefixDir, "var/lib").mkdirs()
        File(prefixDir, "var/lib/dpkg").mkdirs()
        File(prefixDir, "var/cache").mkdirs()
        File(prefixDir, "var/log").mkdirs()
        File(prefixDir, "etc/apt").mkdirs()

        val bashrc = File(prefixDir, "etc/bash.bashrc")
        if (!bashrc.exists()) {
            bashrc.writeText("""
# AuraStudio bash configuration
export HOME="${getHomeDir(context)}"
export TMPDIR="${prefixDir.absolutePath}/tmp"
export LANG="en_US.UTF-8"
export TERM="xterm-256color"
export COLORTERM="truecolor"
export PATH="${prefixDir.absolutePath}/bin:/system/bin:/system/xbin"
export PREFIX="${prefixDir.absolutePath}"
export BASH_SILENCE_DEPRECATION_WARNING=1
PS1='\[\033[1;38;2;130;170;255m\]aurastudio\[\033[0m\] \[\033[38;2;180;180;200m\]\w\[\033[0m\] \[\033[1;38;2;130;255;170m\]\$\[\033[0m\] '
alias ll='ls -la'
alias la='ls -A'
alias l='ls -CF'
""".trimIndent())
        }
    }

    private fun createHomeDir(context: Context) {
        val homeDir = File(getHomeDir(context))
        homeDir.mkdirs()

        val profile = File(homeDir, ".profile")
        if (!profile.exists()) {
            profile.writeText("""
export HOME="${homeDir.absolutePath}"
export PATH="${getPrefixDir(context)}/bin:/system/bin:/system/xbin"
""".trimIndent())
        }
    }
}
