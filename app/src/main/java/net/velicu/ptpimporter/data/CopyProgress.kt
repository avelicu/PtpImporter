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
    val filesToCopy: Int = 0
) {
    companion object {
        fun initial(totalFiles: Int, existingFiles: Int = 0, filesToCopy: Int = 0) = CopyProgress(
            currentFile = 0,
            totalFiles = totalFiles,
            progress = 0f,
            currentFileName = "",
            estimatedTimeRemaining = 0L,
            existingFiles = existingFiles,
            filesToCopy = filesToCopy
        )
        
        fun error(message: String) = CopyProgress(
            currentFile = 0,
            totalFiles = 0,
            progress = 0f,
            currentFileName = "",
            estimatedTimeRemaining = 0L,
            isComplete = true,
            errorMessage = message,
            hasError = true
        )
        
        fun cancelled() = CopyProgress(
            currentFile = 0,
            totalFiles = 0,
            progress = 0f,
            currentFileName = "",
            estimatedTimeRemaining = 0L,
            isComplete = true,
            errorMessage = "Operation cancelled by user",
            hasError = false,
            cancelled = true
        )
    }
} 