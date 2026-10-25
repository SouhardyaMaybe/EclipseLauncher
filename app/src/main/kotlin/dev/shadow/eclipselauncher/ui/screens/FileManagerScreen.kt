/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.data.FileClipboard
import dev.shadow.eclipselauncher.data.FileOps
import dev.shadow.eclipselauncher.designsystem.component.EclipseEmptyState
import dev.shadow.eclipselauncher.designsystem.component.EclipseTopBar
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette
import dev.shadow.eclipselauncher.ui.components.ConfirmDialog
import dev.shadow.eclipselauncher.ui.components.FileRow
import dev.shadow.eclipselauncher.ui.components.NamePromptDialog
import dev.shadow.eclipselauncher.ui.components.PathBar
import dev.shadow.eclipselauncher.ui.components.SelectionActionBar
import java.io.File

@Composable
fun FileManagerScreen(
    root: File,
    initialDir: File?,
    clipboard: FileClipboard,
    onBack: () -> Unit,
    onOpenFile: (File) -> Unit,
) {
    val context = LocalContext.current
    val startDir = remember(root, initialDir) {
        initialDir
            ?.takeIf { it.isDirectory && it.absolutePath.startsWith(root.absolutePath) }
            ?: root
    }
    var currentDir by remember(root, initialDir) { mutableStateOf(startDir) }
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var refresh by remember { mutableIntStateOf(0) }
    var newFolder by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var deleteTargets by remember { mutableStateOf<List<File>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }

    val entries = remember(currentDir, refresh) { FileOps.list(currentDir) }
    val selectedFiles = remember(selected, entries) {
        entries.filter { it.absolutePath in selected }
    }
    val rootLabel = stringResource(R.string.files_root)
    val path = if (currentDir == root) {
        rootLabel
    } else {
        "$rootLabel/${FileOps.relativePath(root, currentDir)}"
    }

    fun finishSelection() {
        selectionMode = false
        selected = emptySet()
    }

    fun toggle(file: File) {
        selected = if (file.absolutePath in selected) {
            selected - file.absolutePath
        } else {
            selected + file.absolutePath
        }
    }

    fun goUp(): Boolean {
        if (currentDir == root) return false
        currentDir = currentDir.parentFile ?: root
        finishSelection()
        return true
    }

    BackHandler(enabled = true) {
        if (selectionMode) finishSelection() else if (!goUp()) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        EclipseTopBar(
            title = stringResource(R.string.files_title),
            onBack = {
                if (selectionMode) finishSelection() else if (!goUp()) onBack()
            },
        )

        PathBar(path = path)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { goUp() },
                enabled = currentDir != root,
            ) {
                Text(stringResource(R.string.action_up))
            }
            TextButton(onClick = { newFolder = true }) {
                Text(stringResource(R.string.action_new_folder))
            }
            if (!selectionMode) {
                TextButton(onClick = { selectionMode = true }) {
                    Text(stringResource(R.string.action_select))
                }
            }
        }

        if (selectionMode) {
            SelectionActionBar(
                selectedText = stringResource(R.string.files_selected, selectedFiles.size),
                canRename = selectedFiles.size == 1,
                canPaste = !clipboard.isEmpty,
                canShare = selectedFiles.isNotEmpty() && selectedFiles.all { it.isFile },
                onDone = { finishSelection() },
                onCopy = {
                    clipboard.copy(selectedFiles)
                    finishSelection()
                    status = context.getString(R.string.logs_copied)
                },
                onCut = {
                    clipboard.cut(selectedFiles)
                    finishSelection()
                    status = context.getString(R.string.logs_cut)
                },
                onPaste = {
                    FileOps.paste(clipboard, currentDir)
                        .onSuccess {
                            refresh++
                            status = context.getString(R.string.logs_pasted)
                        }
                        .onFailure {
                            status = context.getString(R.string.logs_operation_failed)
                        }
                },
                onRename = {
                    renameTarget = selectedFiles.singleOrNull()
                },
                onDelete = {
                    deleteTargets = selectedFiles
                },
                onShare = {
                    FileOps.share(context, selectedFiles)
                    finishSelection()
                },
            )
        }

        status?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = EclipsePalette.Muted,
            )
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                EclipseEmptyState(
                    title = stringResource(R.string.files_empty_title),
                    body = stringResource(R.string.files_empty_body),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(entries, key = { it.absolutePath }) { file ->
                    FileRow(
                        name = file.name,
                        detail = if (file.isDirectory) {
                            stringResource(R.string.files_folder)
                        } else {
                            FileOps.formatSize(context, file)
                        },
                        directory = file.isDirectory,
                        selected = file.absolutePath in selected,
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) {
                                toggle(file)
                            } else if (file.isDirectory) {
                                currentDir = file
                                finishSelection()
                            } else {
                                onOpenFile(file)
                            }
                        },
                    )
                }
            }
        }
    }

    if (newFolder) {
        NamePromptDialog(
            title = stringResource(R.string.files_new_folder_title),
            label = stringResource(R.string.files_new_folder_label),
            initial = "",
            confirmLabel = stringResource(R.string.action_new_folder),
            invalidHint = stringResource(R.string.files_invalid_name),
            isNameTaken = { name -> File(currentDir, name).exists() },
            onConfirm = { name ->
                FileOps.createDirectory(currentDir, name)
                    .onSuccess {
                        refresh++
                        status = context.getString(R.string.files_folder_created)
                    }
                    .onFailure {
                        status = context.getString(R.string.logs_operation_failed)
                    }
                newFolder = false
            },
            onDismiss = { newFolder = false },
        )
    }

    renameTarget?.let { target ->
        NamePromptDialog(
            title = stringResource(R.string.logs_rename_title),
            label = stringResource(R.string.logs_rename_label),
            initial = target.name,
            confirmLabel = stringResource(R.string.action_rename),
            invalidHint = stringResource(R.string.files_invalid_name),
            isNameTaken = { name ->
                name != target.name && File(target.parentFile ?: currentDir, name).exists()
            },
            onConfirm = { name ->
                FileOps.rename(target, name)
                    .onSuccess {
                        refresh++
                        status = context.getString(R.string.logs_renamed)
                    }
                    .onFailure {
                        status = context.getString(R.string.logs_operation_failed)
                    }
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    if (deleteTargets.isNotEmpty()) {
        ConfirmDialog(
            title = stringResource(R.string.files_delete_title),
            body = stringResource(R.string.files_delete_body, deleteTargets.size),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                FileOps.delete(deleteTargets)
                    .onSuccess {
                        refresh++
                        status = context.getString(R.string.logs_deleted)
                    }
                    .onFailure {
                        status = context.getString(R.string.logs_operation_failed)
                    }
                deleteTargets = emptyList()
                finishSelection()
            },
            onDismiss = { deleteTargets = emptyList() },
        )
    }
}
