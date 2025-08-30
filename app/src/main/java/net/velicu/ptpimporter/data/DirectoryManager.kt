package net.velicu.ptpimporter.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import android.util.Log
import android.content.Intent

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
            
            // Check if the document exists and is readable
            if (documentFile?.exists() != true || !documentFile.canRead()) {
                return false
            }
            
            // Check if we have persistable permission for this URI
            if (!hasPersistablePermission(uri)) {
                Log.w("DirectoryManager", "No persistable permission for URI: $uriString")
                return false
            }
            
            true
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Error checking URI validity: $uriString", e)
            false
        }
    }
    
    fun clearDirectories() {
        prefs.edit().remove(KEY_SOURCE_URI).remove(KEY_DEST_URI).apply()
    }
    
    fun takePersistableUriPermission(uri: Uri, isSource: Boolean) {
        try {
            // Take persistent permission for the URI
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            
            Log.d("DirectoryManager", "Persistent permission granted for ${if (isSource) "source" else "destination"} URI")
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Failed to take persistent URI permission", e)
        }
    }
    
    fun hasPersistablePermission(uri: Uri): Boolean {
        return try {
            val permissions = context.contentResolver.getPersistedUriPermissions()
            permissions.any { it.uri == uri }
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Error checking persistable permissions", e)
            false
        }
    }
    
    fun releasePersistablePermission(uri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(uri, 
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            
            Log.d("DirectoryManager", "Persistent permission released for URI")
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Error releasing persistable permission", e)
        }
    }
    
    fun validateStoredUris(): Boolean {
        val sourceUri = getSourceDirectory()
        val destUri = getDestinationDirectory()
        
        var allValid = true
        
        if (sourceUri != null) {
            if (!isUriValid(sourceUri)) {
                Log.w("DirectoryManager", "Source URI is no longer valid: $sourceUri")
                allValid = false
            }
        }
        
        if (destUri != null) {
            if (!isUriValid(destUri)) {
                Log.w("DirectoryManager", "Destination URI is no longer valid: $destUri")
                allValid = false
            }
        }
        
        return allValid
    }
    
    fun getInvalidUris(): List<String> {
        val invalidUris = mutableListOf<String>()
        
        val sourceUri = getSourceDirectory()
        val destUri = getDestinationDirectory()
        
        if (sourceUri != null && !isUriValid(sourceUri)) {
            invalidUris.add(sourceUri)
        }
        
        if (destUri != null && !isUriValid(destUri)) {
            invalidUris.add(destUri)
        }
        
        return invalidUris
    }
} 