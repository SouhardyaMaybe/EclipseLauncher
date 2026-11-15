/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package me.shadow.eclipselauncher.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun instanceDao(): InstanceDao
    abstract fun taskDao(): TaskDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add skinVariant column for Microsoft account skin model (CLASSIC/SLIM)
                database.execSQL("ALTER TABLE accounts ADD COLUMN skinVariant TEXT")
            }
        }
    }
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
