package com.android.git.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

object FileUtils {
    
    /**
     * Robust URI parsing for High-Performance / All-Files-Access scenarios.
     * Optimized for Internal Storage (primary) and standard paths.
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        try {
            // 1. Direct File Scheme
            if (uri.scheme == "file") {
                return File(uri.path!!)
            }

            // 2. Handle DocumentProvider (SAF)
            if (DocumentsContract.isTreeUri(uri)) {
                val docId = DocumentsContract.getTreeDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]
                
                if (split.size > 1) {
                    val path = split[1]
                    
                    // Handle Primary Storage (Internal Memory)
                    if (type.equals("primary", ignoreCase = true)) {
                        return File(Environment.getExternalStorageDirectory(), path)
                    }
                    
                    // Handle SD Cards (Crude but effective for personal projects with Full Access)
                    // Usually mounted at /storage/UUID/
                    val externalStorage = File("/storage/$type")
                    if (externalStorage.exists()) {
                        return File(externalStorage, path)
                    }
                }
            }
            
            // 3. Fallback: Try to guess based on path string (for some older file managers)
            val path = uri.path
            if (path != null && path.contains("/storage/")) {
                return File(path)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}