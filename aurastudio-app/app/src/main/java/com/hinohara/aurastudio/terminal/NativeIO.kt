package com.hinohara.aurastudio.terminal

import android.system.Os
import java.io.FileDescriptor

object NativeIO {
    fun read(fd: Int, buffer: ByteArray, length: Int): Int {
        val fileDescriptor = fdToFileDescriptor(fd)
        return Os.read(fileDescriptor, buffer, 0, length)
    }

    fun write(fd: Int, buffer: ByteArray, length: Int): Int {
        val fileDescriptor = fdToFileDescriptor(fd)
        return Os.write(fileDescriptor, buffer, 0, length)
    }

    private fun fdToFileDescriptor(fd: Int): FileDescriptor {
        val fileDescriptor = FileDescriptor()
        setFdInt(fileDescriptor, fd)
        return fileDescriptor
    }

    private fun setFdInt(fd: FileDescriptor, fdInt: Int) {
        try {
            val field = FileDescriptor::class.java.getDeclaredField("descriptor")
            field.isAccessible = true
            field.setInt(fd, fdInt)
        } catch (_: Exception) {
        }
    }
}
