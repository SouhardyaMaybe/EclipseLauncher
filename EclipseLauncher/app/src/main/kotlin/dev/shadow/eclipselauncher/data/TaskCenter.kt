/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import androidx.compose.runtime.mutableStateListOf
import dev.shadow.eclipselauncher.core.common.Failure

enum class TaskState { RUNNING, PAUSED, FAILED }

/** The visible stage of an install. Progress is only shown once it is real. */
enum class TaskPhase { PREPARING, DOWNLOADING, COMMITTING }

/**
 * One entry in the Tasks dialog. The callbacks are the real controls: the
 * coordinator owns the worker, so a button press changes the worker's state
 * rather than a local UI flag.
 *
 * [fraction] is null until the plan is known. The dialog must not draw a
 * determinate bar before that, because a made-up percentage is worse than no
 * percentage.
 */
data class LauncherTask(
    val id: String,
    val title: String,
    val state: TaskState = TaskState.RUNNING,
    val phase: TaskPhase = TaskPhase.PREPARING,
    val completedItems: Int = 0,
    val totalItems: Int = 0,
    val completedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val fraction: Float? = null,
    val failure: Failure? = null,
    val onPause: (() -> Unit)? = null,
    val onResume: (() -> Unit)? = null,
    val onCancel: (() -> Unit)? = null,
    val onRetry: (() -> Unit)? = null,
)

/**
 * The registry behind the Tasks dialog and its badge. Mutations happen on the
 * main thread: the install coordinator posts progress there, and Compose reads
 * the same observable list.
 */
class TaskCenter {
    val tasks = mutableStateListOf<LauncherTask>()

    fun activeCount(): Int = tasks.size

    fun add(task: LauncherTask) {
        tasks.removeAll { it.id == task.id }
        tasks.add(task)
    }

    fun setPhase(id: String, phase: TaskPhase) = update(id) { it.copy(phase = phase) }

    fun markRunning(id: String) = update(id) {
        it.copy(state = TaskState.RUNNING, failure = null)
    }

    fun markPaused(id: String) = update(id) { it.copy(state = TaskState.PAUSED) }

    fun progress(
        id: String,
        completedItems: Int,
        totalItems: Int,
        completedBytes: Long,
        totalBytes: Long,
        fraction: Float,
    ) = update(id) {
        it.copy(
            completedItems = completedItems,
            totalItems = totalItems,
            completedBytes = completedBytes,
            totalBytes = totalBytes,
            fraction = fraction.coerceIn(0f, 1f),
        )
    }

    fun pause(id: String) {
        val task = tasks.firstOrNull { it.id == id } ?: return
        if (task.state != TaskState.RUNNING) return
        task.onPause?.invoke()
        update(id) { it.copy(state = TaskState.PAUSED) }
    }

    fun resume(id: String) {
        val task = tasks.firstOrNull { it.id == id } ?: return
        if (task.state != TaskState.PAUSED) return
        task.onResume?.invoke()
    }

    fun retry(id: String) {
        val task = tasks.firstOrNull { it.id == id } ?: return
        if (task.state != TaskState.FAILED) return
        task.onRetry?.invoke()
    }

    fun fail(id: String, failure: Failure) = update(id) {
        it.copy(state = TaskState.FAILED, failure = failure)
    }

    fun cancel(id: String) {
        val task = tasks.firstOrNull { it.id == id } ?: return
        task.onCancel?.invoke()
        tasks.removeAll { it.id == id }
    }

    fun finish(id: String) {
        tasks.removeAll { it.id == id }
    }

    private fun update(id: String, block: (LauncherTask) -> LauncherTask) {
        val index = tasks.indexOfFirst { it.id == id }
        if (index >= 0) tasks[index] = block(tasks[index])
    }
}
