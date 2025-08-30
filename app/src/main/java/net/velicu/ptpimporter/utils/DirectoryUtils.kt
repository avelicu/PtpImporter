package net.velicu.ptpimporter.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import android.util.Log

object DirectoryUtils {
    fun getPathFromUri(uri: Uri): String? {
        return try {
            // Return the URI string directly - don't try to convert to file path
            // This allows the FileCopyManager to handle SAF URIs properly
            uri.toString()
        } catch (e: Exception) {
            Log.e("DirectoryUtils", "Error processing URI", e)
            null
        }
    }
    
    fun getFriendlyName(context: Context, uri: Uri): String {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            if (documentFile != null && documentFile.exists()) {
                val name = documentFile.name
                if (!name.isNullOrEmpty()) {
                    return name
                }
            }
            
            // Try to get name from content URI
            val displayName = getDisplayNameFromUri(context, uri)
            if (!displayName.isNullOrEmpty()) {
                return displayName
            }
            
            // Fallback to URI-based naming
            getFallbackName(uri)
        } catch (e: Exception) {
            Log.e("DirectoryUtils", "Error getting friendly name", e)
            getFallbackName(uri)
        }
    }
    
    private fun getDisplayNameFromUri(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (displayNameIndex >= 0) {
                        return it.getString(displayNameIndex)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("DirectoryUtils", "Error querying display name", e)
            null
        }
    }
    
    private fun getFallbackName(uri: Uri): String {
        return when {
            uri.scheme == "content" -> {
                when {
                    uri.host?.contains("mtp", ignoreCase = true) == true -> {
                        // MTP device
                        val path = uri.path
                        when {
                            path?.contains("DCIM", ignoreCase = true) == true -> "Camera DCIM"
                            path?.contains("Pictures", ignoreCase = true) == true -> "Camera Pictures"
                            path?.contains("Download", ignoreCase = true) == true -> "Camera Downloads"
                            else -> "MTP Device"
                        }
                    }
                    uri.host?.contains("external", ignoreCase = true) == true -> {
                        // External storage
                        val path = uri.path
                        when {
                            path?.contains("DCIM", ignoreCase = true) == true -> "DCIM"
                            path?.contains("Pictures", ignoreCase = true) == true -> "Pictures"
                            path?.contains("Download", ignoreCase = true) == true -> "Downloads"
                            path?.contains("Documents", ignoreCase = true) == true -> "Documents"
                            else -> "External Storage"
                        }
                    }
                    uri.host?.contains("primary", ignoreCase = true) == true -> {
                        // Primary storage
                        val path = uri.path
                        when {
                            path?.contains("DCIM", ignoreCase = true) == true -> "DCIM"
                            path?.contains("Pictures", ignoreCase = true) == true -> "Pictures"
                            path?.contains("Download", ignoreCase = true) == true -> "Downloads"
                            path?.contains("Documents", ignoreCase = true) == true -> "Documents"
                            else -> "Internal Storage"
                        }
                    }
                    else -> {
                        // Other content providers
                        val path = uri.path
                        if (!path.isNullOrEmpty()) {
                            val segments = path.split("/").filter { it.isNotEmpty() }
                            if (segments.isNotEmpty()) {
                                segments.last()
                            } else {
                                "Unknown Location"
                            }
                        } else {
                            "Unknown Location"
                        }
                    }
                }
            }
            uri.scheme == "file" -> {
                // File URI
                val path = uri.path
                if (!path.isNullOrEmpty()) {
                    val segments = path.split("/").filter { it.isNotEmpty() }
                    if (segments.isNotEmpty()) {
                        segments.last()
                    } else {
                        "Local Folder"
                    }
                } else {
                    "Local Folder"
                }
            }
            else -> "Unknown Location"
        }
    }
    
    fun getFullPath(context: Context, uri: Uri): String {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            if (documentFile != null && documentFile.exists()) {
                val name = documentFile.name
                if (!name.isNullOrEmpty()) {
                    return name
                }
            }
            
            // Try to build a readable path
            val displayName = getDisplayNameFromUri(context, uri)
            if (!displayName.isNullOrEmpty()) {
                return displayName
            }
            
            getFallbackName(uri)
        } catch (e: Exception) {
            Log.e("DirectoryUtils", "Error getting full path", e)
            getFallbackName(uri)
        }
    }
} 