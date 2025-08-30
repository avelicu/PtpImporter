package net.velicu.ptpimporter.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import android.util.Log

class DirectoryManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ptp_importer_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_SOURCE_URI = "source_uri"
        private const val KEY_DEST_URI = "destination_uri"
    }
    
    fun setSourceDirectory(uri: String) {
        prefs.edit().putString(KEY_SOURCE_URI, uri).apply()
    }
    
    fun getSourceDirectory(): String? {
        return prefs.getString(KEY_SOURCE_URI, null)
    }
    
    fun setDestinationDirectory(uri: String) {
        prefs.edit().putString(KEY_DEST_URI, uri).apply()
    }
    
    fun getDestinationDirectory(): String? {
        return prefs.getString(KEY_DEST_URI, null)
    }
    
    fun isSourceDirectoryValid(): Boolean {
        val uri = getSourceDirectory() ?: return false
        return isUriValid(uri)
    }
    
    fun isDestinationDirectoryValid(): Boolean {
        val uri = getDestinationDirectory() ?: return false
        return isUriValid(uri)
    }
    
    private fun isUriValid(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            documentFile?.exists() == true && documentFile.canRead()
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Error checking URI validity: $uriString", e)
            false
        }
    }
    
    fun clearDirectories() {
        prefs.edit().remove(KEY_SOURCE_URI).remove(KEY_DEST_URI).apply()
    }
} 