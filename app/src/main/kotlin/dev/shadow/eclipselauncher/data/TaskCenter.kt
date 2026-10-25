/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import androidx.compose.runtime.mutableStateListOf

enum class TaskState { RUNNING, PAUSED }

data class LauncherTask(
    val id: String,
    val title: String,
    val detail: String,
    val state: TaskState = TaskState.RUNNING,
)

/**
 * A small task registry for the Tasks dialog. It intentionally starts empty:
 * no install or download is connected in this build, so there is no task to
 * invent. When a real transaction starts it will add an entry here.
 */
class TaskCenter {
    val tasks = mutableStateListOf<LauncherTask>()

    fun activeCount(): Int = tasks.size

    fun add(task: LauncherTask) {
        tasks.removeAll { it.id == task.id }
        tasks.add(task)
    }

    fun pause(id: String) = update(id) { it.copy(state = TaskState.PAUSED) }

    fun resume(id: String) = update(id) { it.copy(state = TaskState.RUNNING) }

    fun cancel(id: String) {
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
