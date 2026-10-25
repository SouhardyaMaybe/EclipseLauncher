/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** The schema version of `instance.json`, `manifest.json` and `plan.json`. */
const val INSTANCE_SCHEMA = 1

/**
 * `instance.json`: the small, human-readable record of what the instance is.
 * It deliberately contains no account token and no absolute path.
 */
@Serializable
data class InstanceRecord(
    val schema: Int = INSTANCE_SCHEMA,
    val id: String,
    val name: String,
    val minecraftVersion: String,
    val loader: String,
    val createdAtEpochMillis: Long,
    val planHash: String,
)

/**
 * `manifest.json`: every managed object the instance depends on. The objects
 * live in the shared store, so the manifest records their store-relative path
 * rather than copying bytes into the instance tree.
 */
@Serializable
data class InstanceManifest(
    val schema: Int = INSTANCE_SCHEMA,
    val instanceId: String,
    val versionId: String,
    val createdAtEpochMillis: Long,
    val planHash: String,
    val files: List<ManagedFile>,
)

@Serializable
data class ManagedFile(
    val path: String,
    val sha1: String,
    val size: Long,
    val kind: DownloadKind,
    val origin: String,
)

/**
 * Pretty JSON for files a user may open and a support bundle may include.
 * Unknown fields are ignored so a newer launcher can add metadata without
 * breaking an older reader.
 */
val InstallJson: Json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Readers for the two documents the app reopens after a restart. They live
 * here so the Android module does not need its own serialization setup, and a
 * malformed file surfaces as one exception the caller can treat as absent.
 */
fun decodeInstanceManifest(text: String): InstanceManifest =
    InstallJson.decodeFromString(text)

fun decodeInstallPlan(text: String): InstallPlan =
    InstallJson.decodeFromString(text)
