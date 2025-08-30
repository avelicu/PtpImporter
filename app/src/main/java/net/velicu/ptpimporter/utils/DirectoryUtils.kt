package net.velicu.ptpimporter.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import android.util.Log

object DirectoryUtils {
    fun getPathFromUri(context: Context, uri: Uri): String? {
        return try {
            // Return the URI string directly - don't try to convert to file path
            // This allows the FileCopyManager to handle SAF URIs properly
            uri.toString()
        } catch (e: Exception) {
            Log.e("DirectoryUtils", "Error processing URI", e)
            null
        }
    }
    
    fun isUriValid(context: Context, uri: Uri): Boolean {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            documentFile?.exists() == true
        } catch (e: Exception) {
            false
        }
    }
} 