package net.velicu.ptpimporter.data

data class CopyProgress(
    val currentFile: Int,
    val totalFiles: Int,
    val progress: Float,
    val currentFileName: String,
    val estimatedTimeRemaining: Long,
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
    val hasError: Boolean = false,
    val cancelled: Boolean = false,
    val existingFiles: Int = 0,
    val filesToCopy: Int = 0,
    val isScanning: Boolean = false,
    val scanProgress: String = ""
) {
    companion object {
        fun initial(totalFiles: Int, existingFiles: Int = 0, filesToCopy: Int = 0) = CopyProgress(
            currentFile = 0, totalFiles = totalFiles, progress = 0f, currentFileName = "",
            estimatedTimeRemaining = 0L, existingFiles = existingFiles, filesToCopy = filesToCopy
        )
        fun scanning(message: String) = CopyProgress(
            currentFile = 0, totalFiles = 0, progress = 0f, currentFileName = "",
            estimatedTimeRemaining = 0L, isScanning = true, scanProgress = message
        )
        fun error(message: String) = CopyProgress(
            currentFile = 0, totalFiles = 0, progress = 0f, currentFileName = "",
            estimatedTimeRemaining = 0L, errorMessage = message, hasError = true
        )
        fun cancelled() = CopyProgress(
            currentFile = 0, totalFiles = 0, progress = 0f, currentFileName = "",
            estimatedTimeRemaining = 0L, errorMessage = "Operation cancelled by user", cancelled = true
        )
    }
} 