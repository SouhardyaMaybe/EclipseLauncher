/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.core.model.InstanceSummary
import dev.shadow.eclipselauncher.data.FileClipboard
import dev.shadow.eclipselauncher.data.FileOps
import dev.shadow.eclipselauncher.data.LogStore
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
fun ManageLogsScreen(
    instance: InstanceSummary?,
    logsDir: File?,
    clipboard: FileClipboard,
    onBack: () -> Unit,
    onOpenFile: (File) -> Unit,
) {
    val context = LocalContext.current
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var refresh by remember { mutableIntStateOf(0) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var deleteTargets by remember { mutableStateOf<List<File>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }

    val files = remember(logsDir, refresh) {
        logsDir?.let { LogStore.list(it) } ?: emptyList()
    }
    val selectedFiles = remember(selected, files) {
        files.filter { it.absolutePath in selected }
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

    BackHandler(enabled = true) {
        if (selectionMode) finishSelection() else onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        EclipseTopBar(
            title = stringResource(R.string.logs_title),
            backContentDescription = stringResource(R.string.action_back),
            onBack = {
                if (selectionMode) finishSelection() else onBack()
            },
        )

        if (instance == null || logsDir == null) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                EclipseEmptyState(
                    title = stringResource(R.string.logs_no_instance_title),
                    body = stringResource(R.string.logs_no_instance_body),
                )
            }
        } else {
            PathBar(
                path = stringResource(R.string.logs_path, instance.id.value),
            ) {
                if (!selectionMode) {
                    TextButton(onClick = { selectionMode = true }) {
                        Text(stringResource(R.string.action_select))
                    }
                }
            }

            if (selectionMode) {
                SelectionActionBar(
                    selectedText = stringResource(R.string.logs_selected_count, selectedFiles.size),
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
                        FileOps.paste(clipboard, logsDir)
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
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.labelSmall,
                    color = EclipsePalette.Muted,
                )
            }

            if (files.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EclipseEmptyState(
                        title = stringResource(R.string.logs_empty_title),
                        body = stringResource(R.string.logs_empty_body),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(files, key = { it.absolutePath }) { file ->
                        FileRow(
                            name = file.name,
                            detail = FileOps.formatSize(context, file),
                            directory = false,
                            selected = file.absolutePath in selected,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) toggle(file) else onOpenFile(file)
                            },
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        NamePromptDialog(
            title = stringResource(R.string.logs_rename_title),
            label = stringResource(R.string.logs_rename_label),
            initial = target.name,
            confirmLabel = stringResource(R.string.action_rename),
            invalidHint = stringResource(R.string.logs_invalid_name),
            isNameTaken = { name ->
                name != target.name && File(target.parentFile ?: logsDir, name).exists()
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
            title = stringResource(R.string.logs_delete_title),
            body = stringResource(R.string.logs_delete_body, deleteTargets.size),
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
