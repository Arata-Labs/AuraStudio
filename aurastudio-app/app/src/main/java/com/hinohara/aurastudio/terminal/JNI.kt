package com.hinohara.aurastudio.terminal

object JNI {
    init {
        System.loadLibrary("termux")
    }

    external fun createSubprocess(
        cmd: String,
        cwd: String,
        args: Array<String>?,
        envVars: Array<String>?,
        processIdArray: IntArray,
        rows: Int,
        columns: Int
    ): Int

    external fun setPtyWindowSize(fd: Int, rows: Int, cols: Int)

    external fun waitFor(pid: Int): Int

    external fun close(fd: Int)
}
