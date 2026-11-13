/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package com.eclipselauncher.data

import kotlinx.serialization.Serializable

@Serializable
enum class AccountKind {
    MICROSOFT,
    LOCAL,
}

@Serializable
data class Account(
    val id: String,
    val kind: AccountKind,
    val username: String,
    val uuid: String = "",
    /** Xbox user id (Microsoft accounts only). */
    val xuid: String = "",
    /** OAuth refresh token (Microsoft accounts only, persisted locally). */
    val refreshToken: String? = null,
    /** Epoch seconds when the Microsoft access token behind this entry expires. */
    val expiresAtEpochSeconds: Long = 0,
    /** Media file names relative to the media root; null means the default. */
    val skinFileName: String? = null,
    val capeFileName: String? = null,
)

@Serializable
enum class LoaderKind {
    VANILLA,
    FABRIC,
    FORGE,
    NEOFORGE,
    QUILT,
    OPTIFINE,
}

/**
 * One Minecraft or mod-loader version, as offered by the version selector.
 * [id] is what gets stored on the instance; [label] is what the list shows
 * (identical to [id] except for the "latest release/snapshot" entries).
 */
data class VersionOption(
    val id: String,
    val label: String,
)

/** A collapsible group inside the version selector dialog. */
data class VersionGroup(
    val title: String,
    val options: List<VersionOption>,
)

/**
 * One published Minecraft version for the create flow: the manifest [id],
 * its [type] (release / snapshot / old_beta / old_alpha) so the filter chips
 * can narrow the list, and [releaseTime] in epoch millis so the list can run
 * newest-first — the whole create screen is one date-ordered list.
 */
data class McVersion(
    val id: String,
    val type: String,
    val releaseTime: Long,
)

@Serializable
data class Instance(
    val id: String,
    val name: String,
    val minecraftVersion: String,
    val loader: LoaderKind,
    /** Loader version for mod loaders; blank means "latest". */
    val loaderVersion: String = "",
    val iconFileName: String? = null,
    /** File name of an imported modpack archive kept with the instance. */
    val importFileName: String? = null,
    /** Per-instance JVM arguments; blank falls back to the global setting. */
    val jvmArgs: String = "",
    /** Renderer id from [com.eclipselauncher.data.Renderers]; blank = default. */
    val renderer: String = "",
    /** Per-instance Java runtime; blank means the bundled default. */
    val runtime: String = "",
    /** Per-instance control map path; blank means the global default. */
    val controlmap: String = "",
    /** Store game data in a shared directory instead of the instance one. */
    val sharedData: Boolean = false,
)

/** UI state for the account list; persisted through Room, not JSON. */
data class AccountState(
    val accounts: List<Account> = emptyList(),
    val selectedId: String? = null,
) {
    val selected: Account?
        get() = accounts.firstOrNull { it.id == selectedId } ?: accounts.firstOrNull()

    val isEmpty: Boolean get() = accounts.isEmpty()
}

/** Lifecycle of a tracked background task. */
@Serializable
enum class TaskStatus { RUNNING, PAUSED, DONE, FAILED, CANCELLED }

/**
 * One background task. Progress is 0f..1f, or -1f while indeterminate.
 * Finished tasks stay in the store until cleared so the tasks dialog can show
 * what happened.
 */
@Serializable
data class LauncherTask(
    val id: String,
    val title: String,
    val detail: String = "",
    val progress: Float = -1f,
    val status: TaskStatus = TaskStatus.RUNNING,
    val createdAt: Long,
    val error: String? = null,
) {
    val isFinished: Boolean
        get() = status == TaskStatus.DONE ||
            status == TaskStatus.FAILED ||
            status == TaskStatus.CANCELLED
}
