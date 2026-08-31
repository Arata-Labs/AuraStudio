package com.aurastudio.filetree.actions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * File-tree context actions (acs `actions/filetree/{CopyPath,Delete,NewFile,NewFolder,OpenWith,Rename}`).
 * Each returns a success flag so the caller can refresh the tree.
 */
internal object FileTreeActions {

    fun copyPath(context: Context, file: File) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("path", file.absolutePath))
    }

    fun openWith(context: Context, file: File, onError: (String) -> Unit) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.providers.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "*/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, null))
        }.onFailure { e -> onError(e.message ?: "Error") }
    }

    fun newFile(dir: File, name: String): Boolean =
        runCatching { File(dir, name).writeText("") }.isSuccess

    fun newFolder(dir: File, name: String): Boolean =
        runCatching { File(dir, name).mkdirs() }.isSuccess

    fun rename(file: File, name: String): Boolean {
        val parent = file.parentFile ?: return false
        return runCatching { file.renameTo(File(parent, name)) }.isSuccess
    }

    fun delete(file: File): Boolean =
        runCatching { file.deleteRecursively() }.isSuccess
}