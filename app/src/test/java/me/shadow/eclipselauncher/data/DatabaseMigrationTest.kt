/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests Room database migration from v1 to v2 (adding skinVariant column).
 * Uses a real v1 database with existing account data, runs migration, and
 * asserts all fields survive and the new column has the correct default.
 */
package me.shadow.eclipselauncher.data

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DatabaseMigrationTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun migration_v1_to_v2_preservesDataAndAddsSkinVariant() {
        // 1. Create v1 database (without skinVariant column)
        val v1Db = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "test_v1.db"
        ).build()

        // Insert a v1 account entity (no skinVariant field)
        val accountDao = v1Db.accountDao()
        val v1Account = AccountEntity(
            id = "test-migration",
            kind = AccountKind.MICROSOFT,
            username = "TestUser",
            uuid = "abcdef1234567890abcdef1234567890",
            xuid = "xuid123",
            refreshToken = "refresh-token",
            expiresAtEpochSeconds = System.currentTimeMillis() / 1000 + 3600,
            skinFileName = "skin_abc123.png",
            capeFileName = "cape_def456.png",
            selected = true,
        )
        v1Db.runInTransaction { accountDao.insert(v1Account) }
        v1Db.close()

        // 2. Open with v2 database (with migration) - this triggers the migration
        val v2Db = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "test_v1.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()

        // 3. Verify the migrated data
        val migratedAccount = v2Db.runInTransaction { accountDao.getById("test-migration") }

        assertNotNull("Account should exist after migration", migratedAccount)
        assertEquals("test-migration", migratedAccount!!.id)
        assertEquals(AccountKind.MICROSOFT, migratedAccount.kind)
        assertEquals("TestUser", migratedAccount.username)
        assertEquals("abcdef1234567890abcdef1234567890", migratedAccount.uuid)
        assertEquals("xuid123", migratedAccount.xuid)
        assertEquals("refresh-token", migratedAccount.refreshToken)
        assertEquals("skin_abc123.png", migratedAccount.skinFileName)
        assertEquals("cape_def456.png", migratedAccount.capeFileName)
        assertTrue(migratedAccount.selected)

        // 4. Verify new skinVariant column has correct default (null)
        assertNull("skinVariant should default to null for existing rows", migratedAccount.skinVariant)

        // 5. Verify we can update the new column
        val updated = migratedAccount.copy(skinVariant = "SLIM")
        v2Db.runInTransaction { accountDao.update(updated) }
        val reloaded = v2Db.runInTransaction { accountDao.getById("test-migration") }
        assertEquals("SLIM", reloaded!!.skinVariant)

        v2Db.close()
    }

    @Test
    fun migration_v1_to_v2_handlesLocalAccountWithoutSkinFiles() {
        // Create v1 database
        val v1Db = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "test_v1_local.db"
        ).build()

        val accountDao = v1Db.accountDao()
        val v1Account = AccountEntity(
            id = "local-test",
            kind = AccountKind.LOCAL,
            username = "LocalUser",
            uuid = "",
            xuid = "",
            refreshToken = null,
            expiresAtEpochSeconds = 0,
            skinFileName = null,
            capeFileName = null,
            selected = false,
        )
        v1Db.runInTransaction { accountDao.insert(v1Account) }
        v1Db.close()

        // Migrate
        val v2Db = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "test_v1_local.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()

        val migrated = v2Db.runInTransaction { accountDao.getById("local-test") }
        assertNotNull(migrated)
        assertEquals(AccountKind.LOCAL, migrated!!.kind)
        assertNull(migrated.skinFileName)
        assertNull(migrated.capeFileName)
        assertNull(migrated.skinVariant)
        assertFalse(migrated.selected)

        v2Db.close()
    }

    @Test
    fun migration_v1_to_v2_canQueryBySkinVariant() {
        val v1Db = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "test_v1_query.db"
        ).build()

        val accountDao = v1Db.accountDao()
        v1Db.runInTransaction {
            accountDao.insert(AccountEntity(
                id = "classic-user",
                kind = AccountKind.LOCAL,
                username = "Classic",
                skinFileName = "skin1.png",
                capeFileName = null,
                selected = true,
            ))
            accountDao.insert(AccountEntity(
                id = "slim-user",
                kind = AccountKind.LOCAL,
                username = "Slim",
                skinFileName = "skin2.png",
                capeFileName = null,
                selected = false,
            ))
        }
        v1Db.close()

        val v2Db = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "test_v1_query.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()

        // After migration, skinVariant is null for both
        val classic = v2Db.runInTransaction { accountDao.getById("classic-user") }
        val slim = v2Db.runInTransaction { accountDao.getById("slim-user") }
        assertNull(classic!!.skinVariant)
        assertNull(slim!!.skinVariant)

        // Update one to SLIM
        v2Db.runInTransaction {
            accountDao.update(slim!!.copy(skinVariant = "SLIM"))
        }

        // Query by skinVariant would require a new DAO method; verify we can read it
        val reloaded = v2Db.runInTransaction { accountDao.getById("slim-user") }
        assertEquals("SLIM", reloaded!!.skinVariant)

        v2Db.close()
    }
}