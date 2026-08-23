package com.hinohara.aurastudio.terminal

import android.content.Context
import android.os.Build
import android.os.StatFs
import android.system.Os
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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

    private const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"

    enum class State {
        NOT_INSTALLED, DOWNLOADING, EXTRACTING, SETTING_UP, READY, ERROR
    }

    data class Progress(
        val state: State = State.NOT_INSTALLED,
        val progress: Float = 0f,
        val message: String = "",
        val error: String? = null
    )

    fun getPrefixDir(context: Context): String =
        "${context.filesDir.absolutePath}/usr"

    fun getHomeDir(context: Context): String =
        "${context.filesDir.absolutePath}/home"

    fun isInstalled(context: Context): Boolean {
        val bash = File(getPrefixDir(context), "bin/bash")
        return bash.exists() && bash.length() > 0 && bash.canExecute()
    }

    private fun getArchName(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when (abi) {
            "arm64-v8a" -> "aarch64"
            "armeabi-v7a" -> "arm"
            "x86" -> "i686"
            "x86_64" -> "x86_64"
            else -> "aarch64"
        }
    }

    private fun getBootstrapUrl(): String {
        val arch = getArchName()
        val v = BOOTSTRAP_VERSION.replace("+", "%2B")
        val vr = BOOTSTRAP_VARIANT.replace("+", "%2B")
        return "$BOOTSTRAP_BASE_URL/bootstrap-$v%2B$vr/bootstrap-$arch.zip"
    }

    fun install(context: Context, onProgress: (Progress) -> Unit): Boolean {
        if (isInstalled(context)) {
            onProgress(Progress(state = State.READY))
            return true
        }

        val prefixDir = File(getPrefixDir(context))
        val stagingDir = File(context.filesDir, "usr_staging")
        val downloadDir = File(context.filesDir, "downloads")

        prefixDir.deleteRecursively()
        stagingDir.deleteRecursively()

        try {
            downloadDir.mkdirs()

            onProgress(Progress(state = State.DOWNLOADING, message = "Downloading bootstrap..."))
            val zipFile = File(downloadDir, "bootstrap-${getArchName()}.zip")
            downloadBootstrap(zipFile, onProgress)

            if (!zipFile.exists() || zipFile.length() == 0L) {
                throw RuntimeException("Download failed - file is empty or missing")
            }

            onProgress(Progress(state = State.EXTRACTING, message = "Extracting packages..."))
            stagingDir.mkdirs()
            extractZip(zipFile, stagingDir, onProgress)

            onProgress(Progress(state = State.SETTING_UP, message = "Setting up environment..."))
            setupEnvironment(stagingDir, context)

            if (!stagingDir.renameTo(prefixDir)) {
                throw RuntimeException("Failed to finalize prefix directory")
            }

            createHomeDir(context)
            zipFile.delete()

            onProgress(Progress(state = State.READY, message = "Ready!"))
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap install failed", e)
            stagingDir.deleteRecursively()
            onProgress(Progress(state = State.ERROR, error = "Install failed: ${e.message}"))
            return false
        }
    }

    private fun downloadBootstrap(zipFile: File, onProgress: (Progress) -> Unit) {
        if (zipFile.exists() && zipFile.length() > 1_000_000) {
            Log.d(TAG, "Using cached bootstrap: ${zipFile.length()} bytes")
            return
        }

        val url = URL(getBootstrapUrl())
        Log.d(TAG, "Downloading: $url")

        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 60_000
        conn.readTimeout = 300_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "AuraStudio/1.0")
        conn.connect()

        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            throw RuntimeException("HTTP $code downloading bootstrap")
        }

        val totalSize = conn.contentLength.toLong()
        var downloaded = 0L

        val tmpFile = File(zipFile.parent, "${zipFile.name}.tmp")
        try {
            FileOutputStream(tmpFile).use { output ->
                conn.inputStream.use { input ->
                    copyWithProgress(input, output, totalSize, downloaded) { bytes ->
                        downloaded = bytes
                        if (totalSize > 0) {
                            onProgress(Progress(
                                state = State.DOWNLOADING,
                                progress = downloaded.toFloat() / totalSize,
                                message = "Downloading... ${(downloaded * 100 / totalSize).toInt()}%"
                            ))
                        }
                    }
                }
            }

            if (tmpFile.length() < 1_000_000) {
                throw RuntimeException("Downloaded file too small (${tmpFile.length()} bytes)")
            }

            tmpFile.renameTo(zipFile)
            Log.d(TAG, "Download complete: ${zipFile.length()} bytes")

        } catch (e: Exception) {
            tmpFile.delete()
            throw e
        } finally {
            conn.disconnect()
        }
    }

    private fun copyWithProgress(
        input: InputStream,
        output: FileOutputStream,
        totalSize: Long,
        initial: Long,
        onBytes: (Long) -> Unit
    ) {
        val buffer = ByteArray(16384)
        var total = initial
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
            total += read
            onBytes(total)
        }
    }

    private fun extractZip(
        zipFile: File,
        targetDir: File,
        onProgress: (Progress) -> Unit
    ) {
        val ourPrefix = targetDir.absolutePath
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
                            var oldPath = parts[0].trim()
                            val relNewPath = parts[1].trim()

                            if (oldPath.startsWith(TERMUX_PREFIX)) {
                                oldPath = ourPrefix + oldPath.removePrefix(TERMUX_PREFIX)
                            }

                            val newPath = "$ourPrefix/${relNewPath.removePrefix("./")}"
                            symlinks.add(Pair(oldPath, newPath))
                            File(newPath).parentFile?.mkdirs()
                        }
                        line = reader.readLine()
                    }
                } else {
                    entryCount++
                    if (entryCount % 300 == 0) {
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
                                Log.w(TAG, "chmod failed: $entryName: ${e.message}")
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
            throw RuntimeException("No SYMLINKS.txt in bootstrap")
        }

        Log.d(TAG, "Creating ${symlinks.size} symlinks...")
        for ((oldPath, newPath) in symlinks) {
            try {
                val f = File(newPath)
                if (!f.parentFile?.exists()!!) f.parentFile?.mkdirs()
                if (f.exists()) f.delete()
                f.createNewFile()
                f.delete()
                Os.symlink(oldPath, newPath)
            } catch (e: Exception) {
                Log.w(TAG, "Symlink failed: $oldPath -> $newPath: ${e.message}")
            }
        }
    }

    private fun setupEnvironment(prefixDir: File, context: Context) {
        File(prefixDir, "tmp").mkdirs()
        File(prefixDir, "var/tmp").mkdirs()
        File(prefixDir, "var/lib/dpkg").mkdirs()
        File(prefixDir, "var/cache").mkdirs()
        File(prefixDir, "var/log").mkdirs()
        File(prefixDir, "etc/apt").mkdirs()
    }

    private fun createHomeDir(context: Context) {
        File(getHomeDir(context)).mkdirs()
    }
}
