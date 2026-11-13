/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package com.eclipselauncher.data.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eclipselauncher.data.Account
import com.eclipselauncher.data.AccountKind
import com.eclipselauncher.data.Instance
import com.eclipselauncher.data.LauncherTask
import com.eclipselauncher.data.LoaderKind
import com.eclipselauncher.data.TaskStatus

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["selected"])],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val kind: AccountKind,
    val username: String,
    val uuid: String = "",
    val xuid: String = "",
    val refreshToken: String? = null,
    val expiresAtEpochSeconds: Long = 0,
    val skinFileName: String? = null,
    val capeFileName: String? = null,
    val selected: Boolean = false,
) {
    fun toModel(): Account = Account(
        id = id,
        kind = kind,
        username = username,
        uuid = uuid,
        xuid = xuid,
        refreshToken = refreshToken,
        expiresAtEpochSeconds = expiresAtEpochSeconds,
        skinFileName = skinFileName,
        capeFileName = capeFileName,
    )

    companion object {
        fun fromModel(account: Account, selected: Boolean): AccountEntity = AccountEntity(
            id = account.id,
            kind = account.kind,
            username = account.username,
            uuid = account.uuid,
            xuid = account.xuid,
            refreshToken = account.refreshToken,
            expiresAtEpochSeconds = account.expiresAtEpochSeconds,
            skinFileName = account.skinFileName,
            capeFileName = account.capeFileName,
            selected = selected,
        )
    }
}

@Entity(tableName = "instances")
data class InstanceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val minecraftVersion: String,
    val loader: LoaderKind,
    val loaderVersion: String = "",
    val iconFileName: String? = null,
    val importFileName: String? = null,
    val jvmArgs: String = "",
    val renderer: String = "",
    val runtime: String = "",
    val controlmap: String = "",
    val sharedData: Boolean = false,
) {
    fun toModel(): Instance = Instance(
        id = id,
        name = name,
        minecraftVersion = minecraftVersion,
        loader = loader,
        loaderVersion = loaderVersion,
        iconFileName = iconFileName,
        importFileName = importFileName,
        jvmArgs = jvmArgs,
        renderer = renderer,
        runtime = runtime,
        controlmap = controlmap,
        sharedData = sharedData,
    )

    companion object {
        fun fromModel(instance: Instance): InstanceEntity = InstanceEntity(
            id = instance.id,
            name = instance.name,
            minecraftVersion = instance.minecraftVersion,
            loader = instance.loader,
            loaderVersion = instance.loaderVersion,
            iconFileName = instance.iconFileName,
            importFileName = instance.importFileName,
            jvmArgs = instance.jvmArgs,
            renderer = instance.renderer,
            runtime = instance.runtime,
            controlmap = instance.controlmap,
            sharedData = instance.sharedData,
        )
    }
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val detail: String = "",
    val status: String,
    val progress: Float = 0f,
    val error: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isFinished: Boolean
        get() = status == TaskStatus.DONE.name ||
            status == TaskStatus.FAILED.name ||
            status == TaskStatus.CANCELLED.name

    fun toModel(): LauncherTask = LauncherTask(
        id = id,
        title = title,
        detail = detail,
        progress = progress,
        status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.FAILED),
        createdAt = startedAt,
        error = error,
    )
}
