package com.eclipselauncher.data

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/** Persisted account list: the entries plus the selected account id. */
@Serializable
data class AccountsFile(
    val accounts: List<Account> = emptyList(),
    val selectedId: String? = null,
)

/**
 * JSON-backed account store. The reference app used Room here; this project
 * keeps its pinned stack, so the same public surface is implemented with a
 * StateFlow over a small file store.
 */
class AccountStore(file: File) {
    private val mutex = Mutex()
    private val store = JsonStore(file, AccountsFile.serializer()) { AccountsFile() }
    private val _state = MutableStateFlow(AccountState())

    val state: StateFlow<AccountState> = _state.asStateFlow()

    suspend fun load() = mutex.withLock {
        val loaded = store.read()
        val selected = loaded.selectedId ?: loaded.accounts.firstOrNull()?.id
        _state.value = AccountState(loaded.accounts, selected)
    }

    suspend fun add(account: Account, select: Boolean = true): Account = mutex.withLock {
        val resolved = if (account.id.isBlank()) account.copy(id = newId()) else account
        val current = _state.value
        val accounts = current.accounts + resolved
        val selectedId = if (select) resolved.id else current.selectedId
        commit(accounts, selectedId)
        resolved
    }

    suspend fun update(account: Account) = mutex.withLock {
        val accounts = _state.value.accounts.map { if (it.id == account.id) account else it }
        commit(accounts, _state.value.selectedId)
    }

    suspend fun select(id: String) = mutex.withLock {
        if (_state.value.accounts.any { it.id == id }) {
            commit(_state.value.accounts, id)
        }
    }

    suspend fun remove(id: String) = mutex.withLock {
        val accounts = _state.value.accounts.filterNot { it.id == id }
        val selectedId = _state.value.selectedId
            ?.takeIf { candidate -> accounts.any { it.id == candidate } }
            ?: accounts.firstOrNull()?.id
        commit(accounts, selectedId)
    }

    suspend fun setMedia(id: String, skin: String?, cape: String?) = mutex.withLock {
        val accounts = _state.value.accounts.map {
            if (it.id == id) it.copy(skinFileName = skin, capeFileName = cape) else it
        }
        commit(accounts, _state.value.selectedId)
    }

    private suspend fun commit(accounts: List<Account>, selectedId: String?) {
        _state.value = AccountState(accounts, selectedId)
        store.write(AccountsFile(accounts, selectedId))
    }
}
