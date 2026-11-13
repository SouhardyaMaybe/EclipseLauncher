/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The tasks overlay: a dialog (not a screen) listing only the ONGOING
 * tasks in real time — this is a live manager, not a history; finished
 * work leaves the list automatically (EclipseApp sweeps finished tasks
 * when the dialog opens). Each row shows status, progress and per-task
 * controls: pause / resume, cancel, and an expandable detail view. The
 * dialog springs open with a bounce and is sucked back into the top-right
 * corner (where its button lives) on minimize.
 */
package com.eclipselauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eclipselauncher.R
import com.eclipselauncher.data.LauncherTask
import com.eclipselauncher.data.TaskStatus
import com.eclipselauncher.ui.animation.AnimatedDialog

/**
 * The ongoing-tasks dialog. [visible] drives the spring-open / corner-suck
 * animation in [AnimatedDialog]; only tasks that have not finished are
 * listed — there is no history and no clear button.
 */
@Composable
fun TasksDialog(
    visible: Boolean,
    tasks: List<LauncherTask>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    // Ongoing only: finished work is swept by the caller and never shown.
    val ongoing = tasks.filter { !it.isFinished }

    AnimatedDialog(
        visible = visible,
        // Sucked back toward the top-right corner on minimize, where the
        // tasks button that opened it lives.
        suckOrigin = TransformOrigin(1f, 0f),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.tasks_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.size(8.dp))

                if (ongoing.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.tasks_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ongoing.forEach { task ->
                            TaskRow(
                                task = task,
                                isExpanded = expanded[task.id] == true,
                                onToggleExpand = {
                                    expanded[task.id] = expanded[task.id] != true
                                },
                                onPause = { onPause(task.id) },
                                onResume = { onResume(task.id) },
                                onCancel = { onCancel(task.id) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            // This is a live manager, not a history: the
                            // button hides the dialog, it does not close it.
                            text = stringResource(R.string.tasks_minimize),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: LauncherTask,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp),
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(status = task.status)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (task.detail.isNotBlank()) {
                    Text(
                        text = task.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            when (task.status) {
                TaskStatus.RUNNING -> IconButton(onClick = onPause) {
                    Icon(
                        imageVector = Icons.Outlined.Pause,
                        contentDescription = stringResource(R.string.tasks_pause),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }

                TaskStatus.PAUSED -> IconButton(onClick = onResume) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(R.string.tasks_resume),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }

                else -> Unit
            }
            if (!task.isFinished) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Outlined.Cancel,
                        contentDescription = stringResource(R.string.tasks_cancel),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            IconButton(onClick = onToggleExpand) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(R.string.tasks_inspect),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (task.progress >= 0f && !task.isFinished) {
            Spacer(modifier = Modifier.size(8.dp))
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                Spacer(modifier = Modifier.size(8.dp))
                DetailLine(
                    label = stringResource(R.string.tasks_detail_status),
                    value = statusLabel(task.status),
                )
                if (task.progress >= 0f) {
                    DetailLine(
                        label = stringResource(R.string.tasks_detail_progress),
                        value = (task.progress * 100).toInt().toString() + "%",
                    )
                }
                DetailLine(
                    label = stringResource(R.string.tasks_detail_started),
                    value = formatTime(task.createdAt),
                )
                if (task.error != null) {
                    DetailLine(
                        label = stringResource(R.string.tasks_detail_error),
                        value = task.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDot(status: TaskStatus) {
    val color = when (status) {
        TaskStatus.RUNNING -> Color(0xFF8FE875)
        TaskStatus.PAUSED -> Color(0xFFF0C674)
        TaskStatus.DONE -> Color(0xFF6BCf4F)
        TaskStatus.FAILED -> Color(0xFFFF8A80)
        TaskStatus.CANCELLED -> Color(0xFF9E9E9E)
    }
    Box(
        modifier = Modifier
            .size(9.dp)
            .background(color, CircleShape),
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.padding(top = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun statusLabel(status: TaskStatus): String = when (status) {
    TaskStatus.RUNNING -> stringResource(R.string.tasks_status_running)
    TaskStatus.PAUSED -> stringResource(R.string.tasks_status_paused)
    TaskStatus.DONE -> stringResource(R.string.tasks_status_done)
    TaskStatus.FAILED -> stringResource(R.string.tasks_status_failed)
    TaskStatus.CANCELLED -> stringResource(R.string.tasks_status_cancelled)
}

private fun formatTime(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
    return sdf.format(java.util.Date(millis))
}
