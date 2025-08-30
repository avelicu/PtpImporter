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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import net.velicu.ptpimporter.utils.DirectoryUtils
import android.net.Uri
import android.content.Context

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
            onTakePermission?.invoke(selectedUri) // Call persistable permission callback
            val path = DirectoryUtils.getPathFromUri(selectedUri)
            if (path != null) {
                onDirectorySelected(path)
            }
        }
    }
    
    // Function to get display text for the current path
    fun getDisplayText(path: String?): String {
        if (path == null) return "Select directory..."
        if (!isValid) return "Directory access expired - tap to reselect"
        
        return try {
            val uri = Uri.parse(path)
            DirectoryUtils.getFriendlyName(context, uri)
        } catch (e: Exception) {
            // Fallback to showing part of the URI
            if (path.length > 50) {
                "..." + path.takeLast(47)
            } else {
                path
            }
        }
    }
    
    // Function to get additional context about the directory
    fun getDirectoryContext(path: String): String {
        return try {
            val uri = Uri.parse(path)
            when {
                uri.scheme == "content" -> {
                    when {
                        uri.host?.contains("mtp", ignoreCase = true) == true -> "MTP Device"
                        uri.host?.contains("external", ignoreCase = true) == true -> "External Storage"
                        uri.host?.contains("primary", ignoreCase = true) == true -> "Internal Storage"
                        else -> "Cloud/Network Storage"
                    }
                }
                uri.scheme == "file" -> "Local Storage"
                else -> "Unknown Storage"
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
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
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = getDisplayText(currentPath),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        !isValid -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Show additional context if available
                if (currentPath != null && isValid) {
                    val contextInfo = getDirectoryContext(currentPath)
                    if (contextInfo.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = contextInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        
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