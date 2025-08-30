package net.velicu.ptpimporter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

data class FileTypeCategory(
    val name: String,
    val description: String,
    val extensions: List<String>,
    val icon: String,
    val isEnabled: Boolean = true
)

@Composable
fun FileTypeSelector(
    selectedFileTypes: Set<String>,
    onFileTypesChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val fileTypeCategories = remember {
        listOf(
            FileTypeCategory(
                name = "Normal Images",
                description = "Common image formats (JPG, PNG, GIF, BMP)",
                extensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff", ".tif"),
                icon = "🖼️"
            ),
            FileTypeCategory(
                name = "Raw Images",
                description = "Professional camera raw formats (CR2, NEF, ARW, etc.)",
                extensions = listOf(".cr2", ".nef", ".arw", ".dng", ".raf", ".orf", ".rw2", ".pef", ".srw"),
                icon = "📷"
            ),
            FileTypeCategory(
                name = "Videos",
                description = "Video formats (MP4, MOV, AVI, MKV, etc.)",
                extensions = listOf(".mp4", ".mov", ".avi", ".mkv", ".wmv", ".flv", ".webm", ".m4v", ".3gp"),
                icon = "🎥"
            )
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with expand/collapse button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📁 File Types to Import",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = if (isExpanded) {
                            androidx.compose.material.icons.Icons.Default.KeyboardArrowUp
                        } else {
                            androidx.compose.material.icons.Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isExpanded) {
                // Collapsed view - show summary
                FileTypeSummary(
                    selectedFileTypes = selectedFileTypes,
                    fileTypeCategories = fileTypeCategories,
                    onFileTypesChanged = onFileTypesChanged,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                // Expanded view - show full selector
                Spacer(modifier = Modifier.height(16.dp))
                
                fileTypeCategories.forEach { category ->
                    FileTypeCategoryItem(
                        category = category,
                        selectedExtensions = selectedFileTypes,
                        onExtensionsChanged = { extensions ->
                            val newSelectedTypes = selectedFileTypes.toMutableSet()
                            // Remove all extensions from this category first
                            newSelectedTypes.removeAll(category.extensions)
                            // Then add back the selected ones
                            newSelectedTypes.addAll(extensions)
                            onFileTypesChanged(newSelectedTypes)
                        }
                    )
                    
                    if (category != fileTypeCategories.last()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "💡 Tip: Select the file types you want to import. The app will search for all selected formats in the source directory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💾 Your selections are automatically saved and will be restored when you reopen the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun FileTypeSummary(
    selectedFileTypes: Set<String>,
    fileTypeCategories: List<FileTypeCategory>,
    onFileTypesChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fileTypeCategories.forEach { category ->
            val categorySelected = category.extensions.count { it in selectedFileTypes }
            val isFullySelected = category.extensions.all { it in selectedFileTypes }
            val isPartiallySelected = categorySelected > 0 && !isFullySelected
            
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isFullySelected -> MaterialTheme.colorScheme.primaryContainer
                            isPartiallySelected -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = when {
                        isFullySelected -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        isPartiallySelected -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        else -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    },
                    modifier = Modifier
                        .height(40.dp)
                        .clickable {
                            val newSelectedTypes = selectedFileTypes.toMutableSet()
                            if (isFullySelected) {
                                // If fully selected, deselect all
                                newSelectedTypes.removeAll(category.extensions)
                            } else {
                                // If not fully selected, select all
                                newSelectedTypes.addAll(category.extensions)
                            }
                            onFileTypesChanged(newSelectedTypes)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.icon,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    isFullySelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isPartiallySelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = "$categorySelected/${category.extensions.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    isFullySelected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    isPartiallySelected -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTypeCategoryItem(
    category: FileTypeCategory,
    selectedExtensions: Set<String>,
    onExtensionsChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCategorySelected = category.extensions.any { it in selectedExtensions }
    val selectedCount = category.extensions.count { it in selectedExtensions }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCategorySelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isCategorySelected) 
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else 
            null
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.icon,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isCategorySelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCategorySelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Checkbox(
                    checked = isCategorySelected,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onExtensionsChanged(category.extensions)
                        } else {
                            onExtensionsChanged(emptyList())
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }

            if (isCategorySelected) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Show selected extensions
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(category.extensions.size) { index ->
                        val extension = category.extensions[index]
                        val isSelected = extension in selectedExtensions
                        
                        FilterChip(
                            onClick = {
                                val newExtensions = if (isSelected) {
                                    selectedExtensions - extension
                                } else {
                                    selectedExtensions + extension
                                }
                                onExtensionsChanged(newExtensions.toList())
                            },
                            label = {
                                Text(
                                    text = extension.uppercase(),
                                    fontSize = 10.sp
                                )
                            },
                            selected = isSelected,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                
                Text(
                    text = "$selectedCount of ${category.extensions.size} formats selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
