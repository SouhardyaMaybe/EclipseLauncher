/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.di

import android.content.Context
import dev.shadow.eclipselauncher.core.common.EpochClock
import dev.shadow.eclipselauncher.core.common.SystemEpochClock
import dev.shadow.eclipselauncher.core.network.HttpDownloadSource
import dev.shadow.eclipselauncher.data.FileClipboard
import dev.shadow.eclipselauncher.data.InstallCoordinator
import dev.shadow.eclipselauncher.data.LauncherState
import dev.shadow.eclipselauncher.data.MojangMetadataClient
import dev.shadow.eclipselauncher.data.MojoSettings
import dev.shadow.eclipselauncher.data.TaskCenter

/**
 * Manual dependency container. Every dependency is visible here, and a module
 * that needs something new has to say so in its constructor.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val clock: EpochClock = SystemEpochClock

    val state = LauncherState(appContext)

    val mojoSettings = MojoSettings(appContext)

    /** One metadata client shared by the version picker and instance editor. */
    val metadata = MojangMetadataClient(HttpDownloadSource())

    val tasks = TaskCenter()

    val fileClipboard = FileClipboard()

    val installer = InstallCoordinator(
        context = appContext,
        minecraftDir = state.minecraftDir,
        tasks = tasks,
    )

    init {
        // A crash during a commit leaves a journal. Recover it before the user
        // can start another install; the transaction repeats this check too.
        installer.recover()
    }
}
