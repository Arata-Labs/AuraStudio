package com.hinohara.aurastudio.terminal

import android.content.Context
import android.system.Os
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object BootstrapInstaller {

    private const val TAG = "BootstrapInstaller"
    private const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"

    enum class State {
        NOT_INSTALLED, EXTRACTING, SETTING_UP, READY, ERROR
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

    fun install(context: Context, onProgress: (Progress) -> Unit): Boolean {
        if (isInstalled(context)) {
            onProgress(Progress(state = State.READY))
            return true
        }

        val finalPrefix = getPrefixDir(context)
        val prefixDir = File(finalPrefix)
        val stagingDir = File(context.filesDir, "usr_staging")

        if (prefixDir.exists()) prefixDir.deleteRecursively()
        if (stagingDir.exists()) stagingDir.deleteRecursively()

        if (prefixDir.exists()) {
            throw RuntimeException("Cannot remove existing prefix directory")
        }

        try {
            onProgress(Progress(state = State.EXTRACTING, message = "Extracting bootstrap..."))
            stagingDir.mkdirs()
            extractBootstrap(context, stagingDir, finalPrefix, onProgress)

            onProgress(Progress(state = State.SETTING_UP, message = "Setting up environment..."))
            setupEnvironment(stagingDir)
            makeExecutable(stagingDir)

            if (!stagingDir.renameTo(prefixDir)) {
                if (prefixDir.exists()) {
                    prefixDir.deleteRecursively()
                }
                if (!stagingDir.renameTo(prefixDir)) {
                    throw RuntimeException("Failed to finalize prefix directory")
                }
            }

            makeExecutable(prefixDir)

            File(getHomeDir(context)).mkdirs()

            onProgress(Progress(state = State.READY, message = "Ready!"))
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Bootstrap install failed", e)
            stagingDir.deleteRecursively()
            onProgress(Progress(state = State.ERROR, error = "Install failed: ${e.message}"))
            return false
        }
    }

    private fun extractBootstrap(
        context: Context,
        targetDir: File,
        finalPrefix: String,
        onProgress: (Progress) -> Unit
    ) {
        val arch = getArchName()
        val assetName = "bootstrap-$arch.zip"

        val symlinks = mutableListOf<Pair<String, String>>()
        val buffer = ByteArray(89096)
        var entryCount = 0

        context.assets.open(assetName).use { assetStream ->
            ZipInputStream(assetStream.buffered()).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name

                    if (entryName == "SYMLINKS.txt") {
                        val reader = BufferedReader(InputStreamReader(zipIn))
                        var line = reader.readLine()
                        while (line != null) {
                            val parts = line.split("\u2190")
                            if (parts.size == 2) {
                                var oldPath = parts[0].trim()
                                val relNewPath = parts[1].trim()

                                if (oldPath.startsWith(TERMUX_PREFIX)) {
                                    oldPath = finalPrefix + oldPath.removePrefix(TERMUX_PREFIX)
                                }

                                val newPath = "$finalPrefix/${relNewPath.removePrefix("./")}"
                                symlinks.add(Pair(oldPath, newPath))
                                File(newPath).parentFile?.mkdirs()
                            }
                            line = reader.readLine()
                        }
                    } else {
                        entryCount++
                        if (entryCount % 500 == 0) {
                            onProgress(Progress(
                                state = State.EXTRACTING,
                                progress = entryCount.toFloat() / 5000f,
                                message = "Extracting... ($entryCount files)"
                            ))
                        }

                        val outFile = File(targetDir, entryName)

                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out ->
                                var read: Int
                                while (zipIn.read(buffer).also { read = it } != -1) {
                                    out.write(buffer, 0, read)
                                }
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
                                }
                            }
                        }
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }

        if (symlinks.isEmpty()) {
            throw RuntimeException("No SYMLINKS.txt found in bootstrap")
        }

        Log.d(TAG, "Creating ${symlinks.size} symlinks...")
        for ((oldPath, newPath) in symlinks) {
            try {
                val f = File(newPath)
                f.parentFile?.mkdirs()
                if (f.exists()) f.delete()
                Os.symlink(oldPath, newPath)
            } catch (e: Exception) {
                Log.w(TAG, "Symlink failed: $oldPath -> $newPath: ${e.message}")
            }
        }
    }

    private fun makeExecutable(prefixDir: File) {
        val binDir = File(prefixDir, "bin")
        if (binDir.exists()) {
            binDir.listFiles()?.forEach { file ->
                if (file.isFile && !file.name.contains(".")) {
                    try {
                        file.setExecutable(true, false)
                    } catch (_: Exception) {}
                }
            }
        }
        arrayOf(
            "lib/apt/apt-helper",
            "lib/apt/methods/http",
            "lib/apt/methods/https",
            "lib/apt/methods/ssh"
        ).forEach { path ->
            val f = File(prefixDir, path)
            if (f.exists()) {
                try { f.setExecutable(true, false) } catch (_: Exception) {}
            }
        }
        val bash = File(prefixDir, "bin/bash")
        if (bash.exists()) {
            try {
                Os.chmod(bash.absolutePath, 448)
            } catch (_: Exception) {
                bash.setExecutable(true, false)
            }
        }
    }

    private fun setupEnvironment(prefixDir: File) {
        File(prefixDir, "tmp").mkdirs()
        File(prefixDir, "var/tmp").mkdirs()
        File(prefixDir, "var/lib/dpkg").mkdirs()
        File(prefixDir, "var/cache").mkdirs()
        File(prefixDir, "var/log").mkdirs()
        File(prefixDir, "etc/apt").mkdirs()
    }

    private fun getArchName(): String {
        val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when (abi) {
            "arm64-v8a" -> "aarch64"
            "armeabi-v7a" -> "arm"
            "x86" -> "i686"
            "x86_64" -> "x86_64"
            else -> "aarch64"
        }
    }
}
