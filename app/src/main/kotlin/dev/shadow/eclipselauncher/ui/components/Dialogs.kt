/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.core.model.InstanceSummary
import dev.shadow.eclipselauncher.data.LauncherTask
import dev.shadow.eclipselauncher.data.TaskCenter
import dev.shadow.eclipselauncher.data.TaskState
import dev.shadow.eclipselauncher.designsystem.component.eclipseTextFieldColors
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette
import java.io.File

@Composable
fun MessageDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        shape = RectangleShape,
        containerColor = EclipsePalette.Panel,
        titleContentColor = EclipsePalette.Text,
        textContentColor = EclipsePalette.Muted,
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = EclipsePalette.Danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RectangleShape,
        containerColor = EclipsePalette.Panel,
        titleContentColor = EclipsePalette.Text,
        textContentColor = EclipsePalette.Muted,
    )
}

@Composable
fun TasksDialog(
    tasks: TaskCenter,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_tasks)) },
        text = {
            if (tasks.tasks.isEmpty()) {
                Column {
                    Text(
                        text = stringResource(R.string.home_tasks_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = EclipsePalette.Text,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.home_tasks_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = EclipsePalette.Muted,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    tasks.tasks.forEach { task ->
                        TaskRow(task = task, tasks = tasks)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        shape = RectangleShape,
        containerColor = EclipsePalette.Panel,
        titleContentColor = EclipsePalette.Text,
        textContentColor = EclipsePalette.Muted,
    )
}

@Composable
private fun TaskRow(task: LauncherTask, tasks: TaskCenter) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.labelLarge,
            color = EclipsePalette.Text,
        )
        Text(
            text = task.detail,
            style = MaterialTheme.typography.bodySmall,
            color = EclipsePalette.Muted,
        )
        Row {
            if (task.state == TaskState.RUNNING) {
                TextButton(onClick = { tasks.pause(task.id) }) {
                    Text(stringResource(R.string.home_tasks_pause))
                }
            } else {
                TextButton(onClick = { tasks.resume(task.id) }) {
                    Text(stringResource(R.string.home_tasks_resume))
                }
            }
            TextButton(onClick = { tasks.cancel(task.id) }) {
                Text(stringResource(R.string.home_tasks_cancel))
            }
        }
    }
}

@Composable
fun InstanceEditorDialog(
    instance: InstanceSummary,
    onDismiss: () -> Unit,
    onSave: (name: String, version: String) -> Unit,
) {
    var name by remember(instance.id) { mutableStateOf(instance.name) }
    var version by remember(instance.id) { mutableStateOf(instance.minecraftVersion) }
    val valid = name.isNotBlank() && version.isNotBlank()
    val loaderName = instance.loader.displayName()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_instance_editor_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.create_instance_name)) },
                    singleLine = true,
                    colors = eclipseTextFieldColors(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.create_instance_version)) },
                    singleLine = true,
                    colors = eclipseTextFieldColors(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${stringResource(R.string.create_instance_loader)}: $loaderName",
                    style = MaterialTheme.typography.labelSmall,
                    color = EclipsePalette.Muted,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), version.trim()) },
                enabled = valid,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RectangleShape,
        containerColor = EclipsePalette.Panel,
        titleContentColor = EclipsePalette.Text,
        textContentColor = EclipsePalette.Muted,
    )
}

@Composable
fun NamePromptDialog(
    title: String,
    label: String,
    initial: String,
    confirmLabel: String,
    invalidHint: String,
    isNameTaken: (String) -> Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(initial) { mutableStateOf(initial) }
    var touched by remember { mutableStateOf(false) }
    val trimmed = value.trim()
    val invalid = trimmed.isBlank() ||
        trimmed.contains('/') ||
        trimmed.contains('\\') ||
        isNameTaken(trimmed)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    touched = true
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                singleLine = true,
                isError = touched && invalid,
                supportingText = {
                    if (touched && invalid) {
                        Text(invalidHint)
                    }
                },
                colors = eclipseTextFieldColors(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed) },
                enabled = !invalid,
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RectangleShape,
        containerColor = EclipsePalette.Panel,
        titleContentColor = EclipsePalette.Text,
        textContentColor = EclipsePalette.Muted,
    )
}

@Composable
fun FileInfoDialog(
    file: File,
    path: String,
    detail: String,
    onOpen: () -> Unit,
    onOpenFolder: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = file.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = path,
                    style = MaterialTheme.typography.labelSmall,
                    color = EclipsePalette.Muted,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = EclipsePalette.Muted,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    if (file.isDirectory) {
                        TextButton(onClick = onOpenFolder) {
                            Text(stringResource(R.string.action_open_folder))
                        }
                    } else {
                        TextButton(onClick = onOpen) {
                            Text(stringResource(R.string.action_open))
                        }
                        TextButton(onClick = onShare) {
                            Text(stringResource(R.string.action_share))
                        }
                    }
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = EclipsePalette.Danger,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        shape = RectangleShape,
        containerColor = EclipsePalette.Panel,
        titleContentColor = EclipsePalette.Text,
        textContentColor = EclipsePalette.Muted,
    )
}
