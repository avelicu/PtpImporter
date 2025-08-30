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
import java.io.File

class FileCopyManager(private val context: Context) {
    private var copyJob: Job? = null
    private val _progress = MutableStateFlow<CopyProgress?>(null)
    val progress: StateFlow<CopyProgress?> = _progress
    
    fun startCopying(sourcePath: String, destPath: String) {
        copyJob?.cancel()
        copyJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FileCopyManager", "Using URI-based copying")
                copyJpgFilesFromUris(sourcePath, destPath)
            } catch (e: Exception) {
                Log.e("FileCopyManager", "Error in startCopying", e)
                _progress.value = CopyProgress.error("Failed to start copying: ${e.message}")
            }
        }
    }
    
    private fun CoroutineScope.copyJpgFilesFromUris(sourceUri: String, destUri: String) {
        try {
            // Test directory access first
            testDirectoryAccess(sourceUri, destUri)
            ensureActive()
            
            val sourceDir = DocumentFile.fromTreeUri(context, Uri.parse(sourceUri))
            val destDir = DocumentFile.fromTreeUri(context, Uri.parse(destUri))
            
            if (sourceDir == null || !sourceDir.exists()) {
                _progress.value = CopyProgress.error("Source directory not found")
                return
            }
            
            if (destDir == null || !destDir.exists()) {
                _progress.value = CopyProgress.error("Destination directory not found")
                return
            }

            // Start scanning phase
            _progress.value = CopyProgress.scanning("Scanning source directory for JPG files...")
            
            val allFiles = getAllFilesRecursively(sourceDir)

            val jpgFiles = allFiles.filter { file ->
                file.name?.lowercase()?.endsWith(".jpg") == true || 
                file.name?.lowercase()?.endsWith(".jpeg") == true
            }
            
            if (jpgFiles.isEmpty()) {
                _progress.value = CopyProgress.error("No JPG files found in source directory")
                return
            }

            // Pre-calculate existing files
            _progress.value = CopyProgress.scanning("Found ${jpgFiles.size} JPG files. Checking destination for existing files...")
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
                ensureActive()
                val fileName = file.name ?: continue

                var newFile: DocumentFile? = null
                try {
                    newFile = destDir.createFile("image/jpeg", fileName)
                    if (newFile != null) {
                        context.contentResolver.openInputStream(file.uri)?.use { input ->
                            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                                input.copyTo(output)
                            }
                        }

                        currentFile++
                        updateProgress(currentFile, totalFiles, fileName, startTime, existingFiles.size, filesToCopy.size)
                    }
                } catch (e: SecurityException) {
                    Log.e("FileCopyManager", "Security exception while copying file: $fileName", e)
                    _progress.value = CopyProgress.error("Permission denied while copying $fileName. This often happens with MTP devices. Try selecting individual files instead of a directory.")
                    newFile?.delete()
                    return
                } catch (e: IOException) {
                    Log.e("FileCopyManager", "IO exception while copying file: $fileName", e)
                    _progress.value = CopyProgress.error("Failed to copy $fileName: ${e.message}")
                    newFile?.delete()
                    return
                }
            }
            
            _progress.value = _progress.value?.copy(isComplete = true)
        } catch (e: SecurityException) {
            Log.e("FileCopyManager", "Security exception during MTP operation", e)
            _progress.value =
                CopyProgress.error("Permission denied accessing MTP device. Try selecting individual files instead of a directory.")
        } catch (e: CancellationException) {
            Log.e("FileCopyManager", "Cancelled")
            _progress.value = CopyProgress.cancelled()
        } catch (e: Exception) {
            Log.e("FileCopyManager", "Unexpected error during copy operation", e)
            _progress.value = CopyProgress.error("Unexpected error: ${e.message}")
        }
    }
    
    private fun CoroutineScope.precalculateExistingFiles(sourceFiles: List<DocumentFile>, destDir: DocumentFile): Pair<List<DocumentFile>, List<DocumentFile>> {
        val filesToCopy = mutableListOf<DocumentFile>()
        val existingFiles = mutableListOf<DocumentFile>()
        
        Log.d("FileCopyManager", "Starting pre-calculation for ${sourceFiles.size} files")
        
        // First, enumerate all files in the destination directory to create a fast lookup set
        _progress.value = CopyProgress.scanning("Scanning destination directory for existing files...")
        val existingFileNames = mutableSetOf<String>()
        
        try {
            val destFiles = destDir.listFiles()
            Log.d("FileCopyManager", "Destination directory contains ${destFiles.size} items")

            for (destFile in destFiles) {
                if (destFile.isFile) {
                    existingFileNames.add(destFile.name ?: "")
                }
            }
            Log.d("FileCopyManager", "Found ${existingFileNames.size} existing files in destination: $existingFileNames")
        } catch (e: Exception) {
            Log.w("FileCopyManager", "Error scanning destination directory: ${e.message}")
            // If we can't scan the destination, assume all files need to be copied
            return Pair(sourceFiles, emptyList())
        }

        // Now do fast in-memory existence checks
        _progress.value = CopyProgress.scanning("Checking which files already exist...")
        
        for ((index, sourceFile) in sourceFiles.withIndex()) {
            // Check for cancellation during pre-calculation
            ensureActive()

            // Update progress every 10 files or for the last file
            if (index % 10 == 0 || index == sourceFiles.size - 1) {
                _progress.value = CopyProgress.scanning("Checking existing files... ${index + 1}/${sourceFiles.size}")
            }
            
            val fileName = sourceFile.name ?: continue
            
            if (existingFileNames.contains(fileName)) {
                existingFiles.add(sourceFile)
            } else {
                filesToCopy.add(sourceFile)
            }
        }
        
        Log.d("FileCopyManager", "Pre-calculation complete: ${sourceFiles.size} total, ${existingFiles.size} exist, ${filesToCopy.size} to copy")
        return Pair(filesToCopy, existingFiles)
    }
    
    private fun CoroutineScope.getAllFilesRecursively(directory: DocumentFile): List<DocumentFile> {
        val allFiles = mutableListOf<DocumentFile>()
        val files = directory.listFiles()
        
        Log.d("FileCopyManager", "Scanning directory: ${directory.name} (${files.size} items)")
        
        for (file in files) {
            // Check for cancellation during recursive search
            ensureActive()
            
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