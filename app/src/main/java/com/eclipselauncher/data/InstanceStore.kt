package com.eclipselauncher.data

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer

/**
 * JSON-backed instance store with the same surface as the reference app's
 * Room-backed store.
 */
class InstanceStore(
    file: File,
    private val media: MediaStorage,
) {
    private val mutex = Mutex()
    private val store = JsonStore(file, ListSerializer(Instance.serializer())) { emptyList() }
    private val _state = MutableStateFlow<List<Instance>>(emptyList())

    val state: StateFlow<List<Instance>> = _state.asStateFlow()

    suspend fun load() = mutex.withLock {
        _state.value = store.read()
    }

    suspend fun create(instance: Instance): Instance = mutex.withLock {
        val resolved = if (instance.id.isBlank()) instance.copy(id = newId()) else instance
        val instances = _state.value + resolved
        _state.value = instances
        store.write(instances)
        resolved
    }

    suspend fun update(instance: Instance) = mutex.withLock {
        val instances = _state.value.map { if (it.id == instance.id) instance else it }
        _state.value = instances
        store.write(instances)
    }

    suspend fun duplicate(id: String): Instance? = mutex.withLock {
        val source = _state.value.firstOrNull { it.id == id } ?: return@withLock null
        val copy = source.copy(
            id = newId(),
            name = source.name + " (copy)",
            iconFileName = source.iconFileName?.let(media::copyForNew),
            importFileName = source.importFileName?.let(media::copyForNew),
        )
        val instances = _state.value + copy
        _state.value = instances
        store.write(instances)
        copy
    }

    suspend fun remove(id: String) = mutex.withLock {
        val target = _state.value.firstOrNull { it.id == id } ?: return@withLock
        target.iconFileName?.let(media::delete)
        target.importFileName?.let(media::delete)
        val instances = _state.value.filterNot { it.id == id }
        _state.value = instances
        store.write(instances)
    }
}
