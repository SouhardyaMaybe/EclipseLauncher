/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.components

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.core.model.InstanceSummary
import dev.shadow.eclipselauncher.data.LauncherTask
import dev.shadow.eclipselauncher.data.TaskCenter
import dev.shadow.eclipselauncher.data.TaskPhase
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
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.labelLarge,
            color = EclipsePalette.Text,
        )
        Spacer(Modifier.height(4.dp))

        when (task.state) {
            TaskState.RUNNING -> {
                Text(
                    text = phaseLabel(task.phase),
                    style = MaterialTheme.typography.bodySmall,
                    color = EclipsePalette.Muted,
                )
                TaskProgress(task, context)
                Row {
                    TaskAction(stringResource(R.string.home_tasks_pause)) {
                        tasks.pause(task.id)
                    }
                    TaskAction(stringResource(R.string.home_tasks_cancel)) {
                        tasks.cancel(task.id)
                    }
                }
            }

            TaskState.PAUSED -> {
                Text(
                    text = stringResource(R.string.home_tasks_paused),
                    style = MaterialTheme.typography.bodySmall,
                    color = EclipsePalette.Warning,
                )
                TaskProgress(task, context)
                Row {
                    TaskAction(stringResource(R.string.home_tasks_resume)) {
                        tasks.resume(task.id)
                    }
                    TaskAction(stringResource(R.string.home_tasks_cancel)) {
                        tasks.cancel(task.id)
                    }
                }
            }

            TaskState.FAILED -> {
                Text(
                    text = task.failure?.userMessage
                        ?: stringResource(R.string.home_tasks_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = EclipsePalette.Danger,
                )
                task.failure?.let { failure ->
                    Text(
                        text = failure.code,
                        style = MaterialTheme.typography.labelSmall,
                        color = EclipsePalette.Muted,
                    )
                }
                Row {
                    if (task.failure?.retryable == true) {
                        TaskAction(stringResource(R.string.home_tasks_retry)) {
                            tasks.retry(task.id)
                        }
                    }
                    TaskAction(stringResource(R.string.action_close)) {
                        tasks.cancel(task.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskProgress(task: LauncherTask, context: Context) {
    val fraction = task.fraction
    if (fraction != null) {
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = EclipsePalette.Primary,
            trackColor = EclipsePalette.Border,
        )
        if (task.totalItems > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.home_tasks_progress_files,
                    task.completedItems,
                    task.totalItems,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = EclipsePalette.Muted,
            )
        }
        if (task.totalBytes > 0L) {
            Text(
                text = stringResource(
                    R.string.home_tasks_progress_bytes,
                    Formatter.formatShortFileSize(context, task.completedBytes),
                    Formatter.formatShortFileSize(context, task.totalBytes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = EclipsePalette.Muted,
            )
        }
    }
}

@Composable
private fun TaskAction(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun phaseLabel(phase: TaskPhase): String = stringResource(
    when (phase) {
        TaskPhase.PREPARING -> R.string.home_tasks_phase_preparing
        TaskPhase.DOWNLOADING -> R.string.home_tasks_phase_downloading
        TaskPhase.COMMITTING -> R.string.home_tasks_phase_committing
    },
)

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
