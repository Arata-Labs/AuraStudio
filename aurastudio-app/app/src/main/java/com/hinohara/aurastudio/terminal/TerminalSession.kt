package com.hinohara.aurastudio.terminal

import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

class TerminalSession(
    private val shellPath: String,
    private val cwd: String,
    private val args: Array<String>,
    private val env: Array<String>,
    private val rows: Int = 24,
    private val columns: Int = 80,
    private val onOutput: (String) -> Unit = {},
    private val onProcessExited: (Int) -> Unit = {}
) {
    private var masterFd: Int = -1
    private var processId: Int = -1
    private var isRunning = false
    private var readerThread: Thread? = null
    private var exitThread: Thread? = null

    val pid: Int get() = processId
    val running: Boolean get() = isRunning

    fun start() {
        val processIdArray = IntArray(1)
        masterFd = JNI.createSubprocess(
            shellPath, cwd, args, env, processIdArray, rows, columns
        )
        processId = processIdArray[0]
        isRunning = true

        val fd = masterFd

        readerThread = thread(name = "PTY-Reader") {
            try {
                val inputStream = fdToInputStream(fd)
                val buffer = ByteArray(4096)
                while (isRunning) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    if (bytesRead > 0) {
                        val output = String(buffer, 0, bytesRead, Charsets.UTF_8)
                        onOutput(output)
                    }
                }
            } catch (_: Exception) {
            }
        }

        exitThread = thread(name = "PTY-ExitWaiter") {
            val exitCode = JNI.waitFor(processId)
            isRunning = false
            onProcessExited(exitCode)
        }
    }

    fun write(text: String) {
        if (masterFd < 0 || !isRunning) return
        try {
            val outputStream = fdToOutputStream(masterFd)
            outputStream.write(text.toByteArray(Charsets.UTF_8))
            outputStream.flush()
        } catch (_: Exception) {
        }
    }

    fun resize(rows: Int, columns: Int) {
        if (masterFd < 0) return
        JNI.setPtyWindowSize(masterFd, rows, columns)
    }

    fun finish() {
        if (!isRunning) return
        isRunning = false
        if (masterFd >= 0) {
            try {
                JNI.close(masterFd)
            } catch (_: Exception) {
            }
            masterFd = -1
        }
    }

    private fun fdToInputStream(fd: Int): InputStream {
        return object : InputStream() {
            override fun read(): Int {
                val buf = ByteArray(1)
                val n = NativeIO.read(fd, buf, 1)
                return if (n > 0) buf[0].toInt() and 0xFF else -1
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                return NativeIO.read(fd, b, len)
            }
        }
    }

    private fun fdToOutputStream(fd: Int): OutputStream {
        return object : OutputStream() {
            override fun write(b: Int) {
                val buf = byteArrayOf(b.toByte())
                NativeIO.write(fd, buf, 1)
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                NativeIO.write(fd, b, len)
            }
        }
    }
}
