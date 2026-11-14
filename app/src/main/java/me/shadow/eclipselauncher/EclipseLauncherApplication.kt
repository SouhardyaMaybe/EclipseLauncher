/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package me.shadow.eclipselauncher

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import me.shadow.eclipselauncher.data.AccountStore
import me.shadow.eclipselauncher.data.InstanceStore
import me.shadow.eclipselauncher.data.Logs
import me.shadow.eclipselauncher.data.MediaStorage
import me.shadow.eclipselauncher.data.MicrosoftAuth
import me.shadow.eclipselauncher.data.ModrinthClient
import me.shadow.eclipselauncher.data.NetworkClient
import me.shadow.eclipselauncher.data.SettingsStore
import me.shadow.eclipselauncher.data.TaskStore
import me.shadow.eclipselauncher.data.VersionRepository
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
