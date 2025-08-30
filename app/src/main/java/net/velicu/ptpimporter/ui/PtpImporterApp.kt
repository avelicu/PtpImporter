package net.velicu.ptpimporter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.safeDrawingPadding
import net.velicu.ptpimporter.data.CopyProgress
import net.velicu.ptpimporter.data.DirectoryManager
import net.velicu.ptpimporter.data.FileCopyManager
import net.velicu.ptpimporter.data.PermissionManager
import net.velicu.ptpimporter.ui.FileTypeSelector
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

@Composable
fun PtpImporterApp(
    onRequestPermissions: (() -> Unit) -> Unit,
    initialSourceUri: String? = null
) {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    val directoryManager = remember { DirectoryManager(context) }
    val fileCopyManager = remember { FileCopyManager(context) }
    
    var hasPermissions by remember { mutableStateOf(permissionManager.hasRequiredPermissions()) }
    var sourceDir by remember { mutableStateOf(directoryManager.getSourceDirectory()) }
    var destDir by remember { mutableStateOf(directoryManager.getDestinationDirectory()) }
    var selectedFileTypes by remember { mutableStateOf(directoryManager.getSelectedFileTypes()) }
    var isCopying by remember { mutableStateOf(false) }
    var copyProgress by remember { mutableStateOf<CopyProgress?>(null) }
    
    // State variables to track directory validity
    var sourceDirValid by remember { mutableStateOf(sourceDir != null && directoryManager.isSourceDirectoryValid()) }
    var destDirValid by remember { mutableStateOf(destDir != null && directoryManager.isDestinationDirectoryValid()) }
    
    // Track if source directory was auto-populated from intent
    var sourceAutoPopulated by remember { mutableStateOf(false) }
    
    // Function to refresh permissions and URI validity
    fun refreshPermissionsAndAccess() {
        hasPermissions = permissionManager.hasRequiredPermissions()
        directoryManager.refreshUriValidity()
        
        // Update directory validity states
        sourceDirValid = sourceDir != null && directoryManager.isSourceDirectoryValid()
        destDirValid = destDir != null && directoryManager.isDestinationDirectoryValid()
    }
    
    // Function to get parent directory URI from a file URI
    fun getParentDirectoryUri(fileUri: String): String? {
        return try {
            val uri = Uri.parse(fileUri)
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            
            if (documentFile != null && documentFile.exists()) {
                val parent = documentFile.parentFile
                if (parent != null && parent.exists()) {
                    // Return the tree URI of the parent directory
                    parent.uri.toString()
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("PtpImporterApp", "Error getting parent directory URI", e)
            null
        }
    }
    
    // Check permissions when the app starts
    LaunchedEffect(Unit) {
        hasPermissions = permissionManager.hasRequiredPermissions()
    }
    
    // Handle initial source URI if provided
    LaunchedEffect(initialSourceUri) {
        initialSourceUri?.let { uri ->
            if (hasPermissions) {
                android.util.Log.d("PtpImporterApp", "Setting initial source URI: $uri")
                
                // Try to get the parent directory of the selected file
                val parentUri = getParentDirectoryUri(uri)
                if (parentUri != null) {
                    sourceDir = parentUri
                    directoryManager.setSourceDirectory(parentUri)
                    sourceDirValid = directoryManager.isSourceDirectoryValid()
                    sourceAutoPopulated = true
                    android.util.Log.d("PtpImporterApp", "Set source directory to parent: $parentUri")
                } else {
                    // If we can't get parent, use the file URI directly
                    sourceDir = uri
                    directoryManager.setSourceDirectory(uri)
                    sourceDirValid = directoryManager.isSourceDirectoryValid()
                    sourceAutoPopulated = true
                    android.util.Log.d("PtpImporterApp", "Set source directory to file: $uri")
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        fileCopyManager.progress.collect { progress ->
            copyProgress = progress
            progress?.let { safeProgress ->
                if (safeProgress.isComplete || safeProgress.hasError || safeProgress.cancelled) {
                    isCopying = false
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "PTP Importer",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Permission Check
        if (!hasPermissions) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Storage Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This app needs access to your photos to copy image and video files. Please grant the required permissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onRequestPermissions {
                                // Update permission status after request
                                hasPermissions = permissionManager.hasRequiredPermissions()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Grant Permissions")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "After granting permissions, you'll be able to select source and destination directories for copying your selected file types.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Permission Status (when granted)
        if (hasPermissions) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✅",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Storage permissions granted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            // Refresh both permissions and URI validity
                            refreshPermissionsAndAccess()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Refresh Permissions & Access", fontSize = 12.sp)
                    }
                }
            }
        }
        
        // Auto-populated source directory notification
        if (sourceAutoPopulated && sourceDir != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📱",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Source directory auto-populated",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Connected device directory automatically selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { sourceAutoPopulated = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Dismiss", fontSize = 12.sp)
                    }
                }
            }
        }
        
        // Source Directory Selection
        DirectorySelector(
            label = "Source Directory",
            currentPath = sourceDir,
            onDirectorySelected = { path ->
                sourceDir = path
                directoryManager.setSourceDirectory(path)
                sourceDirValid = directoryManager.isSourceDirectoryValid()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = hasPermissions,
            isValid = sourceDirValid,
            onTakePermission = { uri ->
                directoryManager.takePersistableUriPermission(uri, true)
                // Refresh validity after taking permission
                sourceDirValid = directoryManager.isSourceDirectoryValid()
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // File Type Selection
        FileTypeSelector(
            selectedFileTypes = selectedFileTypes,
            onFileTypesChanged = { fileTypes ->
                selectedFileTypes = fileTypes
                directoryManager.setSelectedFileTypes(fileTypes)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // Destination Directory Selection
        DirectorySelector(
            label = "Destination Directory",
            currentPath = destDir,
            onDirectorySelected = { path ->
                destDir = path
                directoryManager.setDestinationDirectory(path)
                destDirValid = directoryManager.isDestinationDirectoryValid()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = hasPermissions,
            isValid = destDirValid,
            onTakePermission = { uri ->
                directoryManager.takePersistableUriPermission(uri, false)
                // Refresh validity after taking permission
                destDirValid = directoryManager.isDestinationDirectoryValid()
            }
        )
        
        // Helpful tips for directory selection
        if (hasPermissions) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "💡 Directory Selection Tips",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• For MTP devices (phones, cameras): Select the device's DCIM or Pictures folder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• For local storage: Select any folder on your device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• The app will search recursively through all subfolders for your selected file types",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🔒 Persistent Access: Selected directories will remain accessible even after app restarts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Copy Button
        Button(
            onClick = {
                if (sourceDir != null && destDir != null) {
                    android.util.Log.d("PtpImporterApp", "Starting copy: source=$sourceDir, dest=$destDir, types=$selectedFileTypes")
                    isCopying = true
                    fileCopyManager.startCopying(sourceDir!!, destDir!!, selectedFileTypes)
                }
            },
            enabled = hasPermissions && 
                     sourceDir != null && 
                     destDir != null && 
                     sourceDirValid && 
                     destDirValid && 
                     !isCopying &&
                     selectedFileTypes.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            val fileTypeText = if (selectedFileTypes.size == 1) {
                "Start Copying ${selectedFileTypes.first().uppercase().removePrefix(".")} Files"
            } else {
                "Start Copying ${selectedFileTypes.size} File Types"
            }
            Text(fileTypeText)
        }
        
        // Help text for disabled copy button
        if (hasPermissions && sourceDir != null && destDir != null) {
            if (!sourceDirValid || !destDirValid) {
                Text(
                    text = "⚠️ One or more directories have expired access. Please reselect them to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (selectedFileTypes.isEmpty()) {
                Text(
                    text = "⚠️ Please select at least one file type to import.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        // Error Display
        copyProgress?.let { progress ->
            if (progress.hasError) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = progress.errorMessage ?: "An error occurred",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        // Show permission guidance for permission-related errors
                        if (progress.errorMessage?.contains("permission", ignoreCase = true) == true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 Tip: Make sure you've granted storage permissions above and try selecting the directories again.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        // Show MTP-specific guidance
                        if (progress.errorMessage?.contains("MTP", ignoreCase = true) == true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 MTP Device Tip: Ensure your device is properly connected, try disconnecting and reconnecting, or select a different directory.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else if (progress.cancelled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏹️",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Operation cancelled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            } else if (progress.isComplete && progress.currentFile == 0 && progress.totalFiles == 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ℹ️",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No files found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The selected source directory doesn't contain any files of the selected types (${selectedFileTypes.joinToString(", ") { it.uppercase() }}). Try selecting a different directory or check if the files are in subdirectories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Display
        copyProgress?.let { progress ->
            Spacer(modifier = Modifier.height(16.dp))
            ProgressDisplay(
                progress = progress,
                onCancel = {
                    fileCopyManager.cancelCopying()
                    isCopying = false
                }
            )
            
            // Show scanning state even when not in copying mode
            if (progress.isScanning) {
                isCopying = true
            }
        }
    }
} 