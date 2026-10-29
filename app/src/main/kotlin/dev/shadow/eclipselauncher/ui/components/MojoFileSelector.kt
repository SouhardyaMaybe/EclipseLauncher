/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import java.io.File

/**
 * `fragment_file_selector.xml` + `FileSelectorFragment`.
 *
 * The reference opens this browser as a fragment over the launcher chrome.
 * Here it is a full-window dialog so the editor underneath keeps its values;
 * the path is shown relative to the locked root, folders open on tap, and only
 * files returned by the reference callback can be selected.
 */
@Composable
fun MojoFileSelectorDialog(
    root: File,
    onSelect: (File) -> Unit,
    onDismiss: () -> Unit,
    selectFolder: Boolean = true,
    showFiles: Boolean = true,
    showFolders: Boolean = true,
    title: String? = null,
    pathTitle: Boolean = false,
    cancelLabel: String? = null,
    fileExtension: String? = null,
    showCreateFolder: Boolean = true,
) {
    var current by remember { mutableStateOf(root) }
    var refresh by remember { mutableStateOf(0) }
    var createFolderOpen by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var createFailed by remember { mutableStateOf(false) }

    val entries = remember(current, refresh) {
        val directories = if (showFolders) {
            current.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name.lowercase() }.orEmpty()
        } else {
            emptyList()
        }
        val files = if (showFiles) {
            current.listFiles()
                ?.filter { file ->
                    file.isFile && (
                        fileExtension == null || file.name.endsWith(".$fileExtension")
                        )
                }
                ?.sortedBy { it.name.lowercase() }
                .orEmpty()
        } else {
            emptyList()
        }
        directories to files
    }

    fun relative(path: File): String {
        val rootPath = root.absolutePath.trimEnd(File.separatorChar)
        val full = path.absolutePath
        if (full == rootPath) return "."
        return if (full.startsWith(rootPath + File.separator)) {
            "." + full.removePrefix(rootPath)
        } else {
            full
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background, RectangleShape)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = if (pathTitle) current.absolutePath else title ?: relative(current),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 12.dp),
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                if (current.absolutePath != root.absolutePath) {
                    item(key = "..") {
                        FileRow(
                            label = "..",
                            icon = EclipsePixelIconKind.CHEVRON_UP,
                            onClick = { current = current.parentFile ?: root },
                        )
                    }
                }
                items(entries.first, key = { "d:${it.absolutePath}" }) { directory ->
                    FileRow(
                        label = directory.name,
                        icon = EclipsePixelIconKind.FOLDER,
                        onClick = { current = directory },
                    )
                }
                items(entries.second, key = { "f:${it.absolutePath}" }) { file ->
                    FileRow(
                        label = file.name,
                        icon = EclipsePixelIconKind.FILE,
                        onClick = {
                            if (!selectFolder) onSelect(file)
                        },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showCreateFolder) {
                    MojoMineButton(
                        text = "Create folder",
                        onClick = {
                            newFolderName = ""
                            createFailed = false
                            createFolderOpen = true
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (selectFolder) {
                    MojoMineButton(
                        text = "Select folder",
                        onClick = { onSelect(current) },
                        modifier = Modifier.weight(1f),
                        primary = true,
                    )
                }
                if (cancelLabel != null) {
                    if (!showCreateFolder && !selectFolder) {
                        Spacer(Modifier.weight(1f))
                    }
                    MojoFlatButton(
                        text = cancelLabel,
                        onClick = onDismiss,
                    )
                }
            }
        }
    }

    if (createFolderOpen) {
        MojoDialog(
            onDismiss = { createFolderOpen = false },
            dismissLabel = "Cancel",
            onDismissButton = { createFolderOpen = false },
            confirmLabel = "Create",
            onConfirm = {
                val target = File(current, newFolderName.trim())
                val created = newFolderName.isNotBlank() && target.mkdir()
                if (created) {
                    createFolderOpen = false
                    current = target
                } else {
                    refresh++
                    createFailed = true
                }
            },
            title = "Insert folder name",
            content = {
                MojoTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                )
                if (createFailed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Error",
                        style = MojoType.body,
                        color = MojoPalette.Error,
                    )
                }
            },
        )
    }
}

@Composable
private fun FileRow(
    label: String,
    icon: EclipsePixelIconKind,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EclipsePixelIcon(
            kind = icon,
            modifier = Modifier.size(22.dp),
            tint = MojoPalette.PrimaryText,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MojoType.body.copy(fontSize = 15.sp),
            color = MojoPalette.PrimaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
