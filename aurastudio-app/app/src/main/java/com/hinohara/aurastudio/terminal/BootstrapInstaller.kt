package com.hinohara.aurastudio.terminal

import android.content.Context
import android.os.Build
import android.system.Os
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
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
        return bash.exists() && bash.length() > 0 && bash.canExecute()
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
        val stagingDir = File(context.filesDir, "usr_staging")
        val tmpDir = File(context.filesDir, "usr_tmp")

        if (prefixDir.exists() && !isInstalled(context)) {
            prefixDir.deleteRecursively()
        }
        stagingDir.deleteRecursively()

        try {
            onProgress(Progress(state = State.DOWNLOADING, message = "Downloading bootstrap..."))

            val zipFile = File(context.cacheDir, "bootstrap-${getArchName()}.zip")
            downloadBootstrap(zipFile, onProgress)

            onProgress(Progress(state = State.EXTRACTING, message = "Extracting packages..."))

            stagingDir.mkdirs()
            extractZip(zipFile, stagingDir, onProgress)

            onProgress(Progress(state = State.SETTING_UP, message = "Setting up environment..."))

            setupEnvironment(stagingDir, context)

            if (prefixDir.exists()) prefixDir.deleteRecursively()
            if (!stagingDir.renameTo(prefixDir)) {
                throw RuntimeException("Failed to rename staging dir to prefix dir")
            }

            createHomeDir(context)

            onProgress(Progress(state = State.READY, message = "Ready!"))
            zipFile.delete()
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap installation failed", e)
            stagingDir.deleteRecursively()
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
        val symlinks = mutableListOf<Pair<String, String>>()

        zipIn.use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val entryName = entry.name

                if (entryName == "SYMLINKS.txt") {
                    val reader = BufferedReader(InputStreamReader(zip))
                    var line = reader.readLine()
                    while (line != null) {
                        val parts = line.split("\u2190")
                        if (parts.size == 2) {
                            val oldPath = parts[0].trim()
                            val newPath = "${targetDir.absolutePath}/${parts[1].trim()}"
                            symlinks.add(Pair(oldPath, newPath))
                            File(newPath).parentFile?.mkdirs()
                        }
                        line = reader.readLine()
                    }
                } else {
                    entryCount++
                    if (entryCount % 200 == 0) {
                        onProgress(Progress(
                            state = State.EXTRACTING,
                            message = "Extracting... ($entryCount files)"
                        ))
                    }

                    val outFile = File(targetDir, entryName)

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { out ->
                            zip.copyTo(out)
                        }

                        if (entryName.startsWith("bin/") ||
                            entryName.startsWith("libexec") ||
                            entryName.startsWith("lib/apt/apt-helper") ||
                            entryName.startsWith("lib/apt/methods")
                        ) {
                            try {
                                Os.chmod(outFile.absolutePath, 448)
                            } catch (e: Exception) {
                                Log.w(TAG, "chmod failed for $entryName: ${e.message}")
                                outFile.setExecutable(true, false)
                            }
                        }
                    }
                }

                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        if (symlinks.isEmpty()) {
            throw RuntimeException("No SYMLINKS.txt found in bootstrap zip")
        }

        Log.d(TAG, "Creating ${symlinks.size} symlinks...")
        for ((oldPath, newPath) in symlinks) {
            try {
                val newFile = File(newPath)
                if (newFile.exists() || newFile.createNewFile()) {
                    newFile.delete()
                }
                Os.symlink(oldPath, newPath)
            } catch (e: Exception) {
                Log.w(TAG, "Symlink failed: $oldPath -> $newPath: ${e.message}")
            }
        }
    }

    private fun setupEnvironment(prefixDir: File, context: Context) {
        File(prefixDir, "tmp").mkdirs()
        File(prefixDir, "var").mkdirs()
        File(prefixDir, "var/tmp").mkdirs()
        File(prefixDir, "var/lib").mkdirs()
        File(prefixDir, "var/lib/dpkg").mkdirs()
        File(prefixDir, "var/cache").mkdirs()
        File(prefixDir, "var/log").mkdirs()
        File(prefixDir, "etc/apt").mkdirs()

        val bashrc = File(prefixDir, "etc/bash.bashrc")
        if (!bashrc.exists()) {
            bashrc.writeText("""
export HOME="${getHomeDir(context)}"
export TMPDIR="${prefixDir.absolutePath}/tmp"
export LANG="en_US.UTF-8"
export TERM="xterm-256color"
export COLORTERM="truecolor"
export PREFIX="${prefixDir.absolutePath}"
export PATH="${prefixDir.absolutePath}/bin:/system/bin:/system/xbin"
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
