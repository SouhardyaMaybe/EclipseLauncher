package com.eclipselauncher.data

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer

/** JSON-backed task registry; finished tasks stay until cleared. */
class TaskStore(
    file: File,
    private val logs: Logs,
) {
    private val mutex = Mutex()
    private val store = JsonStore(file, ListSerializer(LauncherTask.serializer())) { emptyList() }
    private val _state = MutableStateFlow<List<LauncherTask>>(emptyList())

    val state: StateFlow<List<LauncherTask>> = _state.asStateFlow()

    suspend fun load() = mutex.withLock {
        // An interrupted process leaves RUNNING/PAUSED entries behind; they
        // can never resume, so mark them failed on the next launch.
        val loaded = store.read().map {
            if (it.status == TaskStatus.RUNNING || it.status == TaskStatus.PAUSED) {
                it.copy(status = TaskStatus.FAILED, error = "interrupted")
            } else {
                it
            }
        }
        _state.value = loaded
        store.write(loaded)
    }

    fun hasActiveTasks(): Boolean = state.value.any { !it.isFinished }

    suspend fun start(title: String, detail: String = ""): String = mutex.withLock {
        val id = newId()
        val task = LauncherTask(
            id = id,
            title = title,
            detail = detail,
            createdAt = System.currentTimeMillis(),
        )
        val tasks = listOf(task) + _state.value
        _state.value = tasks
        store.write(tasks)
        logs.log("tasks", "started: $title")
        id
    }

    suspend fun report(id: String, progress: Float, detail: String? = null) = mutex.withLock {
        commit { task ->
            if (task.id == id) {
                task.copy(
                    progress = progress.coerceIn(0f, 1f),
                    detail = detail ?: task.detail,
                )
            } else {
                task
            }
        }
    }

    suspend fun step(id: String) {
        while (true) {
            val task = _state.value.firstOrNull { it.id == id } ?: throw CancellationException()
            when (task.status) {
                TaskStatus.CANCELLED -> throw CancellationException()
                TaskStatus.PAUSED -> delay(250)
                TaskStatus.RUNNING -> return
                else -> return
            }
        }
    }

    suspend fun pause(id: String) = mutex.withLock {
        commit { if (it.id == id) it.copy(status = TaskStatus.PAUSED) else it }
        logs.log("tasks", "paused: $id")
    }

    suspend fun resume(id: String) = mutex.withLock {
        commit { if (it.id == id) it.copy(status = TaskStatus.RUNNING) else it }
        logs.log("tasks", "resumed: $id")
    }

    suspend fun cancel(id: String) = mutex.withLock {
        commit { if (it.id == id && !it.isFinished) it.copy(status = TaskStatus.CANCELLED) else it }
        logs.log("tasks", "cancelled: $id")
    }

    suspend fun finish(id: String, error: String? = null) = mutex.withLock {
        commit {
            if (it.id == id) {
                it.copy(
                    status = if (error == null) TaskStatus.DONE else TaskStatus.FAILED,
                    progress = if (error == null) 1f else it.progress,
                    error = error,
                )
            } else {
                it
            }
        }
        logs.log("tasks", if (error == null) "finished: $id" else "failed: $id ($error)")
    }

    suspend fun clearFinished() = mutex.withLock {
        val tasks = _state.value.filterNot { it.isFinished }
        _state.value = tasks
        store.write(tasks)
    }

    private suspend fun commit(transform: (LauncherTask) -> LauncherTask) {
        val tasks = _state.value.map(transform)
        _state.value = tasks
        store.write(tasks)
    }
}
