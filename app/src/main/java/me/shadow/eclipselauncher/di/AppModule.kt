/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package me.shadow.eclipselauncher.di

import android.content.Context
import androidx.room.Room
import me.shadow.eclipselauncher.data.Logs
import me.shadow.eclipselauncher.data.MediaStorage
import me.shadow.eclipselauncher.data.NetworkClient
import me.shadow.eclipselauncher.data.SettingsStore
import me.shadow.eclipselauncher.data.SqlCipherPassphrase
import me.shadow.eclipselauncher.data.VersionRepository
import me.shadow.eclipselauncher.data.room.AccountDao
import me.shadow.eclipselauncher.data.room.AppDatabase
import me.shadow.eclipselauncher.data.room.InstanceDao
import me.shadow.eclipselauncher.data.room.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMediaStorage(@ApplicationContext context: Context): MediaStorage =
        MediaStorage(context)

    @Provides
    @Singleton
    fun provideLogs(@ApplicationContext context: Context): Logs =
        Logs(File(context.filesDir, "logs"))

    @Provides
    @Singleton
    fun provideVersionRepository(
        @ApplicationContext context: Context,
        network: NetworkClient,
    ): VersionRepository = VersionRepository(File(context.cacheDir, "meta_cache"), network)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        passphrase: SqlCipherPassphrase,
    ): AppDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(passphrase.getPassphrase())
        return Room.databaseBuilder(context, AppDatabase::class.java, "eclipse.db")
            .openHelperFactory(factory)
            .build()
    }

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideInstanceDao(db: AppDatabase): InstanceDao = db.instanceDao()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore =
        SettingsStore(context)

    @Provides
    @Singleton
    fun provideControlMapsDir(@ApplicationContext context: Context): File =
        File(context.filesDir, "controlmaps").apply { mkdirs() }
}
