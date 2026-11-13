/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package com.eclipselauncher.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM accounts ORDER BY username ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Query("UPDATE accounts SET selected = 0")
    suspend fun clearSelection()

    @Query("UPDATE accounts SET selected = 1 WHERE id = :id")
    suspend fun setSelected(id: String)

    @Transaction
    suspend fun select(id: String) {
        clearSelection()
        setSelected(id)
    }

    @Transaction
    suspend fun addAndSelect(account: AccountEntity) {
        insert(account)
        select(account.id)
    }
}

@Dao
interface InstanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(instance: InstanceEntity)

    @Update
    suspend fun update(instance: InstanceEntity)

    @Query("DELETE FROM instances WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM instances ORDER BY name ASC")
    fun getAll(): Flow<List<InstanceEntity>>

    @Query("SELECT * FROM instances WHERE id = :id")
    suspend fun getById(id: String): InstanceEntity?
}

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM tasks WHERE status IN ('RUNNING', 'PAUSED') ORDER BY startedAt DESC")
    fun getActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status IN ('RUNNING', 'PAUSED')")
    suspend fun getActiveList(): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY rowid ASC")
    fun getRecent(): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks WHERE status IN ('DONE', 'FAILED', 'CANCELLED')")
    suspend fun clearFinished()
}
