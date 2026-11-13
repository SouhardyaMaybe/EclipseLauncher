package com.eclipselauncher.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun FileManagerScreen(
    onNavigate: (String) -> Unit
) {
    var currentPath by remember { mutableStateOf("/") }
    var entries by remember { mutableStateOf(mockEntries("/")) }

    EclipseLauncherTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EclipseColors.DarkBackground
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                // Status bar spacer
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                )

                // Bare back arrow + title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = EclipseColors.DarkPrimary,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onNavigate("tools") }
                        )
                        Text(
                            text = "File manager",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EclipseColors.DarkPrimary
                        )
                    }
                }

                // Breadcrumb bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = EclipseColors.DarkSurfaceVariant,
                    onClick = {
                        if (currentPath != "/") {
                            val parts = currentPath.split("/").filter { it.isNotBlank() }.toMutableList()
                            if (parts.size > 1) {
                                parts.removeAt(parts.lastIndex)
                                currentPath = "/" + parts.joinToString("/")
                            } else {
                                currentPath = "/"
                            }
                            entries = mockEntries(currentPath)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPath,
                            fontSize = 14.sp,
                            color = EclipseColors.DarkOnSurface,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // File list
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = "This folder is empty",
                            fontSize = 16.sp,
                            color = EclipseColors.DarkOnSurfaceVariant,
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        entries.forEach { entry ->
                            FileEntryRow(
                                entry = entry,
                                onClick = { 
                                    if (entry.isDirectory) {
                                        currentPath = if (currentPath == "/") "/${entry.name}" else "$currentPath/${entry.name}"
                                        entries = mockEntries(currentPath)
                                    }
                                },
                                onDeleteClick = { /* Delete confirmation */ }
                            )
                        }
                    }
                }
            }
        }
    }

}

private fun mockEntries(path: String): List<FileEntry> {
    return when (path) {
        "/" -> listOf(
            FileEntry("minecraft", true, 0),
            FileEntry("logs", true, 0),
            FileEntry("media", true, 0),
            FileEntry("modpacks", true, 0),
            FileEntry("settings.json", false, 2150),
            FileEntry("last-run.txt", false, 640)
        )
        "/skins" -> listOf(
            FileEntry("capes", true, 0),
            FileEntry("classic", true, 0),
            FileEntry("steve.png", false, 12697),
            FileEntry("alex.png", false, 8908)
        )
        else -> listOf()
    }
}

data class FileEntry(
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long
)

@Composable
fun FileEntryRow(
    entry: FileEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val icon = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.Description
    val iconColor = if (entry.isDirectory) EclipseColors.DarkPrimary else EclipseColors.DarkOnSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = EclipseColors.DarkSurface,
        onClick = { if (entry.isDirectory) onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (entry.isDirectory) "Folder" else "File",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = entry.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = EclipseColors.DarkOnSurface
                    )
                    if (!entry.isDirectory) {
                        Text(
                            text = formatSize(entry.sizeBytes),
                            fontSize = 11.sp,
                            color = EclipseColors.DarkOnSurfaceVariant
                        )
                    }
                }
            }
            
            // Delete button
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = EclipseColors.DarkError,
                modifier = Modifier
                    .size(24.dp)
                    .padding(8.dp)
                    .clickable { onDeleteClick() }
            )
        }
    }
}

fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "${(bytes / (1024.0 * 1024)).toString().take(4)} MB"
        bytes >= 1024 -> "${(bytes / 1024.0).toString().take(4)} KB"
        else -> "$bytes B"
    }
}