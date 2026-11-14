/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package me.shadow.eclipselauncher.data

import me.shadow.eclipselauncher.data.room.TaskDao
import me.shadow.eclipselauncher.data.room.TaskEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
class TaskStore @Inject constructor(
    private val dao: TaskDao,
    private val logs: Logs,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state: StateFlow<List<LauncherTask>> = dao.getRecent()
        .map { it.map { entity -> entity.toModel() } }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private fun toEntity(task: LauncherTask): TaskEntity = TaskEntity(
        id = task.id,
        title = task.title,
        detail = task.detail,
        status = task.status.name,
        progress = task.progress,
        error = task.error,
        startedAt = task.createdAt,
        updatedAt = System.currentTimeMillis(),
    )

    /** An interrupted process leaves RUNNING/PAUSED rows behind; fail them. */
    suspend fun load() {
        dao.getActiveList().forEach { entity ->
            dao.update(
                entity.copy(
                    status = TaskStatus.FAILED.name,
                    error = "interrupted",
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun hasActiveTasks(): Boolean = state.value.any { !it.isFinished }

    suspend fun start(title: String, detail: String = ""): String {
        val id = newId()
        dao.insert(
            toEntity(
                LauncherTask(
                    id = id,
                    title = title,
                    detail = detail,
                    createdAt = System.currentTimeMillis(),
                ),
            ),
        )
        logs.log("tasks", "started: $title")
        return id
    }

    suspend fun report(id: String, progress: Float, detail: String? = null) {
        dao.getActiveList().firstOrNull { it.id == id }?.let { entity ->
            dao.update(
                entity.copy(
                    progress = progress.coerceIn(0f, 1f),
                    detail = detail ?: entity.detail,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun step(id: String) {
        while (true) {
            val task = dao.getActiveList().firstOrNull { it.id == id }
                ?: throw CancellationException()
            when (task.status) {
                TaskStatus.CANCELLED.name -> throw CancellationException()
                TaskStatus.PAUSED.name -> delay(250)
                else -> return
            }
        }
    }

    suspend fun pause(id: String) {
        dao.getActiveList().firstOrNull { it.id == id }?.let { entity ->
            dao.update(entity.copy(status = TaskStatus.PAUSED.name, updatedAt = System.currentTimeMillis()))
            logs.log("tasks", "paused: $id")
        }
    }

    suspend fun resume(id: String) {
        dao.getActiveList().firstOrNull { it.id == id }?.let { entity ->
            dao.update(entity.copy(status = TaskStatus.RUNNING.name, updatedAt = System.currentTimeMillis()))
            logs.log("tasks", "resumed: $id")
        }
    }

    suspend fun cancel(id: String) {
        dao.getActiveList().firstOrNull { it.id == id }?.let { entity ->
            if (!entity.isFinished) {
                dao.update(entity.copy(status = TaskStatus.CANCELLED.name, updatedAt = System.currentTimeMillis()))
                logs.log("tasks", "cancelled: $id")
            }
        }
    }

    suspend fun finish(id: String, error: String? = null) {
        dao.getActiveList().firstOrNull { it.id == id }?.let { entity ->
            dao.update(
                entity.copy(
                    status = if (error == null) TaskStatus.DONE.name else TaskStatus.FAILED.name,
                    progress = if (error == null) 1f else entity.progress,
                    error = error,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            logs.log("tasks", if (error == null) "finished: $id" else "failed: $id ($error)")
        }
    }

    suspend fun clearFinished() = dao.clearFinished()
}
