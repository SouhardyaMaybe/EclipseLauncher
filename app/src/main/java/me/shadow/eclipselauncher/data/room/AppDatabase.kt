/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package me.shadow.eclipselauncher.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import me.shadow.eclipselauncher.data.AccountKind
import me.shadow.eclipselauncher.data.LoaderKind

@Database(
    entities = [
        AccountEntity::class,
        InstanceEntity::class,
        TaskEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun instanceDao(): InstanceDao
    abstract fun taskDao(): TaskDao
}

class Converters {
    @TypeConverter
    fun fromAccountKind(kind: AccountKind): String = kind.name

    @TypeConverter
    fun toAccountKind(name: String): AccountKind = AccountKind.valueOf(name)

    @TypeConverter
    fun fromLoaderKind(kind: LoaderKind): String = kind.name

    @TypeConverter
    fun toLoaderKind(name: String): LoaderKind = LoaderKind.valueOf(name)
}
