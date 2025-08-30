package net.velicu.ptpimporter.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import net.velicu.ptpimporter.utils.DirectoryUtils
import android.net.Uri

@Composable
fun DirectorySelector(
    label: String,
    currentPath: String?,
    onDirectorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isValid: Boolean = true,
    onTakePermission: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    
    val directoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { selectedUri ->
            // Take persistable permission first
            onTakePermission?.invoke(selectedUri)
            
            val path = DirectoryUtils.getPathFromUri(context, selectedUri)
            if (path != null) {
                onDirectorySelected(path)
            }
        }
    }
    
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Validity indicator
            if (currentPath != null) {
                if (isValid) {
                    Text(
                        text = "✅ Valid",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "⚠️ Expired",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 1.dp,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.outlineVariant
                        !isValid -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    },
                    shape = MaterialTheme.shapes.small
                )
                .clickable(enabled = enabled) { directoryLauncher.launch(null) }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = when {
                    currentPath == null -> "Select directory..."
                    !isValid -> "Directory access expired - tap to reselect"
                    else -> currentPath
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    !isValid -> MaterialTheme.colorScheme.error
                    currentPath != null -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Show warning for expired directories
        if (currentPath != null && !isValid) {
            Text(
                text = "This directory's access has expired. Please reselect it to continue.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
} 