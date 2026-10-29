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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import java.io.File

@Composable
fun MessageDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    MojoDialog(
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.action_close),
        onConfirm = onDismiss,
        title = title,
        message = body,
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
    MojoDialog(
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.action_cancel),
        onDismissButton = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        title = title,
        message = body,
    )
}

@Composable
fun TasksDialog(
    tasks: TaskCenter,
    onDismiss: () -> Unit,
) {
    MojoDialog(
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.action_close),
        onConfirm = onDismiss,
        title = stringResource(R.string.home_tasks),
        content = {
            if (tasks.tasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_tasks_empty_title),
                    style = MojoType.preferenceTitle,
                    color = MojoPalette.PrimaryText,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.home_tasks_empty_body),
                    style = MojoType.body,
                    color = MojoPalette.SecondaryText,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    tasks.tasks.forEach { task ->
                        TaskRow(task = task, tasks = tasks)
                        MojoDivider(Modifier.padding(vertical = 10.dp))
                    }
                }
            }
        },
    )
}

@Composable
private fun TaskRow(task: LauncherTask, tasks: TaskCenter) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = task.title,
            style = MojoType.preferenceTitle,
            color = MojoPalette.PrimaryText,
        )
        Spacer(Modifier.height(4.dp))

        when (task.state) {
            TaskState.RUNNING -> {
                Text(
                    text = phaseLabel(task.phase),
                    style = MojoType.bodySmall,
                    color = MojoPalette.SecondaryText,
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
                    style = MojoType.bodySmall,
                    color = MojoPalette.Accent,
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
                    style = MojoType.bodySmall,
                    color = MojoPalette.Error,
                )
                task.failure?.let { failure ->
                    Text(
                        text = failure.code,
                        style = MojoType.secondary,
                        color = MojoPalette.SecondaryText,
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
            color = MojoPalette.Accent,
            trackColor = MojoPalette.SecondaryButton,
        )
        if (task.totalItems > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.home_tasks_progress_files,
                    task.completedItems,
                    task.totalItems,
                ),
                style = MojoType.bodySmall,
                color = MojoPalette.SecondaryText,
            )
        }
        if (task.totalBytes > 0L) {
            Text(
                text = stringResource(
                    R.string.home_tasks_progress_bytes,
                    Formatter.formatShortFileSize(context, task.completedBytes),
                    Formatter.formatShortFileSize(context, task.totalBytes),
                ),
                style = MojoType.bodySmall,
                color = MojoPalette.SecondaryText,
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
        Text(
            text = text,
            style = MojoType.body,
            color = MojoPalette.Accent,
        )
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

    MojoDialog(
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.action_cancel),
        onDismissButton = onDismiss,
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = { onSave(name.trim(), version.trim()) },
        confirmEnabled = valid,
        title = stringResource(R.string.create_instance_editor_title),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.create_instance_name),
                    style = MojoType.body,
                    color = MojoPalette.SecondaryText,
                )
                Spacer(Modifier.height(6.dp))
                MojoTextField(
                    value = name,
                    onValueChange = { name = it },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.create_instance_version),
                    style = MojoType.body,
                    color = MojoPalette.SecondaryText,
                )
                Spacer(Modifier.height(6.dp))
                MojoTextField(
                    value = version,
                    onValueChange = { version = it },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${stringResource(R.string.create_instance_loader)}: $loaderName",
                    style = MojoType.secondary,
                    color = MojoPalette.SecondaryText,
                )
            }
        },
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

    MojoDialog(
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.action_cancel),
        onDismissButton = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = { onConfirm(trimmed) },
        confirmEnabled = !invalid,
        title = title,
        content = {
            MojoTextField(
                value = value,
                onValueChange = {
                    value = it
                    touched = true
                },
                placeholder = label,
                errorText = if (touched && invalid) invalidHint else null,
            )
        },
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
    MojoDialog(
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.action_close),
        onConfirm = onDismiss,
        title = file.name,
        content = {
            Text(
                text = path,
                style = MojoType.secondary,
                color = MojoPalette.SecondaryText,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = detail,
                style = MojoType.bodySmall,
                color = MojoPalette.SecondaryText,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                if (file.isDirectory) {
                    FileAction(stringResource(R.string.action_open_folder), onOpenFolder)
                } else {
                    FileAction(stringResource(R.string.action_open), onOpen)
                    FileAction(stringResource(R.string.action_share), onShare)
                }
                FileAction(
                    text = stringResource(R.string.action_delete),
                    onClick = onDelete,
                    color = MojoPalette.Error,
                )
            }
        },
    )
}

@Composable
private fun FileAction(
    text: String,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = MojoPalette.Accent,
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            style = MojoType.body,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
