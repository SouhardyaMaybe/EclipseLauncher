/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.core.model

@JvmInline
value class InstanceId(val value: String) {
    init {
        require(value.isNotBlank()) { "instance id must not be blank" }
    }
}

/**
 * The loader families the launcher can name. Only Vanilla and Fabric are
 * connected to a real install path in v1; the other values exist so the
 * instance record and the placeholder buttons do not invent a second model.
 */
enum class LoaderKind {
    VANILLA,
    FABRIC,
    QUILT,
    FORGE,
    NEOFORGE,
    OPTIFINE,
    MODPACK,
}

/**
 * The small, stable view of an instance the shell needs. The full instance
 * record lives in the database and filesystem; this type is intentionally not
 * an ORM entity.
 */
data class InstanceSummary(
    val id: InstanceId,
    val name: String,
    val minecraftVersion: String,
    val loader: LoaderKind,
    val lastPlayedEpochMillis: Long? = null,
)
