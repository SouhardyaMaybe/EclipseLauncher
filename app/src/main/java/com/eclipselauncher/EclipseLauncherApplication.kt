/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package com.eclipselauncher

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.eclipselauncher.data.AccountStore
import com.eclipselauncher.data.InstanceStore
import com.eclipselauncher.data.Logs
import com.eclipselauncher.data.MediaStorage
import com.eclipselauncher.data.MicrosoftAuth
import com.eclipselauncher.data.ModrinthClient
import com.eclipselauncher.data.NetworkClient
import com.eclipselauncher.data.SettingsStore
import com.eclipselauncher.data.TaskStore
import com.eclipselauncher.data.VersionRepository
import com.tencent.mmkv.MMKV
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class EclipseLauncherApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        val container = EntryPoints.get(this, EclipseContainer::class.java)
        // Fail tasks interrupted by the previous process before the first frame.
        runBlocking { container.tasks.load() }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
}

/** Hilt-provided dependency container: one instance for the whole process. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface EclipseContainer {
    val network: NetworkClient
    val media: MediaStorage
    val accounts: AccountStore
    val instances: InstanceStore
    val settings: SettingsStore
    val versions: VersionRepository
    val logs: Logs
    val tasks: TaskStore
    val auth: MicrosoftAuth
    val modrinth: ModrinthClient

    /** Per-instance control maps; the editor picks files from here. */
    val controlmapsDir: java.io.File
}
