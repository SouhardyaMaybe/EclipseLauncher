/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package me.shadow.eclipselauncher.data

import me.shadow.eclipselauncher.data.room.InstanceDao
import me.shadow.eclipselauncher.data.room.InstanceEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
class InstanceStore @Inject constructor(
    private val dao: InstanceDao,
    private val media: MediaStorage,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state: StateFlow<List<Instance>> = dao.getAll()
        .map { it.map { entity -> entity.toModel() } }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** The Room flow is collected eagerly; retained for the application warm-up. */
    suspend fun load() = Unit

    suspend fun create(instance: Instance): Instance {
        val resolved = if (instance.id.isBlank()) instance.copy(id = newId()) else instance
        dao.insert(InstanceEntity.fromModel(resolved))
        return resolved
    }

    suspend fun update(instance: Instance) = dao.update(InstanceEntity.fromModel(instance))

    suspend fun duplicate(id: String): Instance? = dao.getById(id)?.let { entity ->
        val copy = entity.toModel().copy(
            id = newId(),
            name = entity.name + " (copy)",
            iconFileName = entity.iconFileName?.let(media::copyForNew),
            importFileName = entity.importFileName?.let(media::copyForNew),
        )
        dao.insert(InstanceEntity.fromModel(copy))
        copy
    }

    suspend fun remove(id: String) {
        dao.getById(id)?.let { entity ->
            entity.iconFileName?.let(media::delete)
            entity.importFileName?.let(media::delete)
            dao.delete(id)
        }
    }
}
