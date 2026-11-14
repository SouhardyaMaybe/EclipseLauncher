/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package me.shadow.eclipselauncher.data

import me.shadow.eclipselauncher.data.room.AccountDao
import me.shadow.eclipselauncher.data.room.AccountEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
class AccountStore @Inject constructor(
    private val dao: AccountDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val state: StateFlow<AccountState> = dao.getAll()
        .map { accounts ->
            val selected = accounts.firstOrNull { it.selected } ?: accounts.firstOrNull()
            AccountState(
                accounts = accounts.map { it.toModel() },
                selectedId = selected?.id,
            )
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, AccountState())

    /** The Room flow is collected eagerly; retained for the application warm-up. */
    suspend fun load() = Unit

    suspend fun add(account: Account, select: Boolean = true): Account {
        val resolved = if (account.id.isBlank()) account.copy(id = newId()) else account
        val entity = AccountEntity.fromModel(resolved, select)
        if (select) dao.addAndSelect(entity) else dao.insert(entity)
        return resolved
    }

    suspend fun update(account: Account) {
        dao.getById(account.id)?.let { existing ->
            dao.update(AccountEntity.fromModel(account, existing.selected))
        }
    }

    suspend fun select(id: String) = dao.select(id)

    suspend fun remove(id: String) = dao.delete(id)

    suspend fun setMedia(id: String, skin: String?, cape: String?) {
        dao.getById(id)?.let { entity ->
            dao.update(entity.copy(skinFileName = skin, capeFileName = cape))
        }
    }
}
