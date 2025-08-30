package net.velicu.ptpimporter.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import android.util.Log

class FileCopyManager(private val context: Context) {
    private var copyJob: Job? = null
    private val _progress = MutableStateFlow<CopyProgress?>(null)
    val progress: StateFlow<CopyProgress?> = _progress
    
    fun startCopying(sourcePath: String, destPath: String) {
        copyJob?.cancel()
        copyJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if the paths are actually URIs
                if (sourcePath.startsWith("content://") || destPath.startsWith("content://")) {
                    Log.d("FileCopyManager", "Using URI-based copying")
                    copyJpgFilesFromUris(sourcePath, destPath)
                } else {
                    Log.d("FileCopyManager", "Using file path copying")
                    copyJpgFiles(sourcePath, destPath)
                }
            } catch (e: Exception) {
                Log.e("FileCopyManager", "Error in startCopying", e)
                _progress.value = CopyProgress.error("Failed to start copying: ${e.message}")
            }
        }
    }
    
    private suspend fun copyJpgFilesFromUris(sourceUri: String, destUri: String) {
        try {
            Log.d("FileCopyManager", "Starting URI-based copy: source=$sourceUri, dest=$destUri")
            
            // Test directory access first
            if (!testDirectoryAccess(sourceUri, "source") || !testDirectoryAccess(destUri, "destination")) {
                return
            }
            
            val sourceDir = DocumentFile.fromTreeUri(context, Uri.parse(sourceUri))
            val destDir = DocumentFile.fromTreeUri(context, Uri.parse(destUri))
            
            if (sourceDir == null || !sourceDir.exists()) {
                Log.d("FileCopyManager", "Source directory is null or doesn't exist")
                _progress.value = CopyProgress.error("Source directory not found or inaccessible. Please check permissions and try again.")
                return
            }
            
            if (destDir == null || !destDir.exists()) {
                Log.d("FileCopyManager", "Destination directory is null or doesn't exist")
                _progress.value = CopyProgress.error("Destination directory not found or inaccessible. Please check permissions and try again.")
                return
            }
            
            // List all files in source directory recursively
            val allFiles = getAllFilesRecursively(sourceDir)
            val jpgFiles = allFiles.filter { file ->
                file.isFile && file.name?.lowercase()?.endsWith(".jpg") == true || 
                file.name?.lowercase()?.endsWith(".jpeg") == true
            }
            
            if (jpgFiles.isEmpty()) {
                Log.d("FileCopyManager", "No JPG files found in source directory")
                _progress.value = CopyProgress.error("No JPG files found in source directory")
                return
            }
            
            // Pre-calculate existing files to get accurate progress
            val (filesToCopy, existingFiles) = precalculateExistingFiles(jpgFiles, destDir)
            
            if (filesToCopy.isEmpty()) {
                Log.d("FileCopyManager", "All JPG files already exist in destination")
                _progress.value = CopyProgress(
                    currentFile = 0,
                    totalFiles = 0,
                    progress = 1f,
                    currentFileName = "",
                    estimatedTimeRemaining = 0L,
                    isComplete = true,
                    errorMessage = null,
                    hasError = false,
                    cancelled = false,
                    existingFiles = existingFiles.size,
                    filesToCopy = 0
                )
                return
            }
            
            val totalFiles = filesToCopy.size
            Log.d("FileCopyManager", "Found ${jpgFiles.size} total JPG files, ${existingFiles.size} already exist, ${filesToCopy.size} will be copied")
            var currentFile = 0
            val startTime = System.currentTimeMillis()
            
            _progress.value = CopyProgress.initial(totalFiles, existingFiles.size, filesToCopy.size)
            
            for (file in filesToCopy) {
                if (copyJob?.isCancelled == true) {
                    break
                }
                
                val fileName = file.name ?: "unknown.jpg"
                
                try {
                    // Create the destination file
                    val newFile = destDir.createFile("image/jpeg", fileName)
                    if (newFile != null) {
                        // Copy the content using ContentResolver
                        context.contentResolver.openInputStream(file.uri)?.use { input ->
                            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                                input.copyTo(output)
                            }
                        }
                        currentFile++
                        updateProgress(currentFile, totalFiles, fileName, startTime, existingFiles.size, filesToCopy.size)
                    }
                } catch (e: SecurityException) {
                    Log.e("FileCopyManager", "Security exception copying file $fileName", e)
                    _progress.value = CopyProgress.error("Access denied to file: $fileName. This may be due to MTP device restrictions.")
                    return
                } catch (e: IOException) {
                    Log.e("FileCopyManager", "Error copying file $fileName", e)
                    continue
                }
            }
            
            if (copyJob?.isCancelled != true) {
                _progress.value = _progress.value?.copy(isComplete = true)
            }
        } catch (e: SecurityException) {
            Log.e("FileCopyManager", "Permission denied in URI-based copying", e)
            if (e.message?.contains("MtpDocumentsProvider") == true) {
                _progress.value = CopyProgress.error("MTP device access issue. Please ensure the device is properly connected and try selecting the directory again.")
            } else {
                _progress.value = CopyProgress.error("Permission denied. Please grant storage permissions in app settings and try again.")
            }
        } catch (e: Exception) {
            Log.e("FileCopyManager", "Error in URI-based copying", e)
            _progress.value = CopyProgress.error("Error during copying: ${e.message}")
        }
    }
    
    private fun precalculateExistingFiles(sourceFiles: List<DocumentFile>, destDir: DocumentFile): Pair<List<DocumentFile>, List<DocumentFile>> {
        val filesToCopy = mutableListOf<DocumentFile>()
        val existingFiles = mutableListOf<DocumentFile>()
        
        for (sourceFile in sourceFiles) {
            val fileName = sourceFile.name ?: continue
            val destFile = destDir.findFile(fileName)
            
            if (destFile != null && destFile.exists()) {
                existingFiles.add(sourceFile)
            } else {
                filesToCopy.add(sourceFile)
            }
        }
        
        Log.d("FileCopyManager", "Pre-calculation: ${sourceFiles.size} total, ${existingFiles.size} exist, ${filesToCopy.size} to copy")
        return Pair(filesToCopy, existingFiles)
    }
    
    private fun precalculateExistingFiles(sourceFiles: Array<java.io.File>, destDir: java.io.File): Pair<Array<java.io.File>, Array<java.io.File>> {
        val filesToCopy = mutableListOf<java.io.File>()
        val existingFiles = mutableListOf<java.io.File>()
        
        for (sourceFile in sourceFiles) {
            val destFile = java.io.File(destDir, sourceFile.name)
            if (destFile.exists()) {
                existingFiles.add(sourceFile)
            } else {
                filesToCopy.add(sourceFile)
            }
        }
        
        Log.d("FileCopyManager", "Pre-calculation: ${sourceFiles.size} total, ${existingFiles.size} exist, ${filesToCopy.size} to copy")
        return Pair(filesToCopy.toTypedArray(), existingFiles.toTypedArray())
    }
    
    private fun getAllFilesRecursively(directory: DocumentFile): List<DocumentFile> {
        val allFiles = mutableListOf<DocumentFile>()
        val files = directory.listFiles()
        
        Log.d("FileCopyManager", "Scanning directory: ${directory.name} (${files.size} items)")
        
        for (file in files) {
            // Check for cancellation during recursive search
            if (copyJob?.isCancelled == true) {
                Log.d("FileCopyManager", "File discovery cancelled")
                return allFiles
            }
            
            if (file.isFile) {
                allFiles.add(file)
            } else if (file.isDirectory) {
                Log.d("FileCopyManager", "Found subdirectory: ${file.name}, recursing...")
                allFiles.addAll(getAllFilesRecursively(file))
            }
        }
        
        Log.d("FileCopyManager", "Directory ${directory.name} total files: ${allFiles.size}")
        return allFiles
    }
    
    private fun getAllJpgFilesRecursively(directory: java.io.File): Array<java.io.File> {
        val allFiles = mutableListOf<java.io.File>()
        val files = directory.listFiles() ?: emptyArray()
        
        Log.d("FileCopyManager", "Scanning directory: ${directory.name} (${files.size} items)")
        
        for (file in files) {
            // Check for cancellation during recursive search
            if (copyJob?.isCancelled == true) {
                Log.d("FileCopyManager", "File discovery cancelled")
                return allFiles.toTypedArray()
            }
            
            if (file.isFile) {
                if (file.extension.lowercase() in listOf("jpg", "jpeg")) {
                    allFiles.add(file)
                }
            } else if (file.isDirectory) {
                Log.d("FileCopyManager", "Found subdirectory: ${file.name}, recursing...")
                allFiles.addAll(getAllJpgFilesRecursively(file))
            }
        }
        
        Log.d("FileCopyManager", "Directory ${directory.name} total JPG files: ${allFiles.size}")
        return allFiles.toTypedArray()
    }
    
    private fun testDirectoryAccess(uriString: String, directoryType: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            
            if (documentFile == null) {
                Log.e("FileCopyManager", "$directoryType directory is null")
                _progress.value = CopyProgress.error("$directoryType directory is not accessible")
                return false
            }
            
            if (!documentFile.exists()) {
                Log.e("FileCopyManager", "$directoryType directory does not exist")
                _progress.value = CopyProgress.error("$directoryType directory does not exist")
                return false
            }
            
            if (!documentFile.canRead()) {
                Log.e("FileCopyManager", "$directoryType directory is not readable")
                _progress.value = CopyProgress.error("$directoryType directory is not readable")
                return false
            }
            
            // Test if we can list files (this catches many MTP access issues)
            try {
                val testFiles = documentFile.listFiles()
                Log.d("FileCopyManager", "$directoryType directory accessible, contains ${testFiles.size} items")
            } catch (e: SecurityException) {
                Log.e("FileCopyManager", "Security exception accessing $directoryType directory", e)
                if (e.message?.contains("MtpDocumentsProvider") == true) {
                    _progress.value = CopyProgress.error("MTP device access issue with $directoryType directory. Please ensure the device is properly connected and try selecting the directory again.")
                } else {
                    _progress.value = CopyProgress.error("Access denied to $directoryType directory. Please check permissions.")
                }
                return false
            }
            
            true
        } catch (e: Exception) {
            Log.e("FileCopyManager", "Error testing $directoryType directory access", e)
            _progress.value = CopyProgress.error("Error accessing $directoryType directory: ${e.message}")
            false
        }
    }
    
    fun cancelCopying() {
        Log.d("FileCopyManager", "Cancelling copy operation")
        copyJob?.cancel()
        // Clear progress and indicate operation was cancelled
        _progress.value = CopyProgress.cancelled()
    }
    
    private suspend fun CoroutineScope.copyJpgFiles(sourcePath: String, destPath: String) {
        try {
            // For now, we'll use a simplified approach with regular File operations
            // In a real app, you'd need to handle SAF (Storage Access Framework) properly
            android.util.Log.d("FileCopyManager", "Starting copy: source=$sourcePath, dest=$destPath")
            val sourceDir = java.io.File(sourcePath)
            val destDir = java.io.File(destPath)
            
            if (!sourceDir.exists()) {
                android.util.Log.d("FileCopyManager", "Source directory does not exist: $sourcePath")
                _progress.value = CopyProgress.error("Source directory does not exist: $sourcePath")
                return
            }
            
            if (!sourceDir.isDirectory) {
                android.util.Log.d("FileCopyManager", "Source path is not a directory: $sourcePath")
                _progress.value = CopyProgress.error("Source path is not a directory: $sourcePath")
                return
            }
            
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            
            val jpgFiles = getAllJpgFilesRecursively(sourceDir)
            
            if (jpgFiles.isEmpty()) {
                android.util.Log.d("FileCopyManager", "No JPG files found in source directory")
                _progress.value = CopyProgress.error("No JPG files found in source directory")
                return
            }
            
            // Pre-calculate existing files to get accurate progress
            val (filesToCopy, existingFiles) = precalculateExistingFiles(jpgFiles, destDir)
            
            if (filesToCopy.isEmpty()) {
                android.util.Log.d("FileCopyManager", "All JPG files already exist in destination")
                _progress.value = CopyProgress(
                    currentFile = 0,
                    totalFiles = 0,
                    progress = 1f,
                    currentFileName = "",
                    estimatedTimeRemaining = 0L,
                    isComplete = true,
                    errorMessage = null,
                    hasError = false,
                    cancelled = false,
                    existingFiles = existingFiles.size,
                    filesToCopy = 0
                )
                return
            }
            
            val totalFiles = filesToCopy.size
            android.util.Log.d("FileCopyManager", "Found ${jpgFiles.size} total JPG files, ${existingFiles.size} already exist, ${filesToCopy.size} will be copied")
            var currentFile = 0
            val startTime = System.currentTimeMillis()
            
            _progress.value = CopyProgress.initial(totalFiles, existingFiles.size, filesToCopy.size)
            
            for (file in filesToCopy) {
                if (copyJob?.isCancelled == true) {
                    break
                }
                
                try {
                    copyFile(file, java.io.File(destDir, file.name))
                    currentFile++
                    updateProgress(currentFile, totalFiles, file.name, startTime, existingFiles.size, filesToCopy.size)
                } catch (e: IOException) {
                    // Log error but continue with next file
                    continue
                }
            }
            
            _progress.value = _progress.value?.copy(isComplete = true)
        } catch (e: Exception) {
            // Handle any other errors
            _progress.value = CopyProgress.error("Error during copying: ${e.message}")
        }
    }
    
    private fun copyFile(source: java.io.File, dest: java.io.File) {
        java.io.FileInputStream(source).use { input ->
            java.io.FileOutputStream(dest).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
    }
    
    private fun updateProgress(currentFile: Int, totalFiles: Int, currentFileName: String, startTime: Long, existingFiles: Int, filesToCopy: Int) {
        val progress = currentFile.toFloat() / totalFiles
        val elapsedTime = System.currentTimeMillis() - startTime
        val estimatedTimeRemaining = if (currentFile > 0) {
            val avgTimePerFile = elapsedTime / currentFile
            val remainingFiles = totalFiles - currentFile
            (avgTimePerFile * remainingFiles) / 1000 // Convert to seconds
        } else {
            0L
        }
        
        _progress.value = CopyProgress(
            currentFile = currentFile,
            totalFiles = totalFiles,
            progress = progress,
            currentFileName = currentFileName,
            estimatedTimeRemaining = estimatedTimeRemaining,
            existingFiles = existingFiles,
            filesToCopy = filesToCopy
        )
    }
} 