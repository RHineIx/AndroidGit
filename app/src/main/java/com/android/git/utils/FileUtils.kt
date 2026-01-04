package com.android.git.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

object FileUtils {
    // Converts Android URI (content://...) to Java File Object
    fun getFileFromUri(context: Context, uri: Uri): File? {
        try {
            val path = uri.path ?: return null
            // Handle External Storage URIs (primary:Folder/Subfolder)
            if (path.contains("primary:")) {
                val relativePath = path.substringAfter("primary:")
                return File(Environment.getExternalStorageDirectory(), relativePath)
            }
            // Fallback for direct paths
            if (uri.scheme == "file") {
                return File(uri.path!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
