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
    private val microsoftAuth: MicrosoftAuth,
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

    /**
     * Refreshes Microsoft account profiles from the Minecraft services API.
     * For each Microsoft account with a refresh token, fetches the latest profile
     * and updates the stored skin/cape URLs and variant if they changed.
     * Call on app start and when the access token expires.
     */
    suspend fun refreshMicrosoftProfiles(media: MediaStorage) {
        val accounts = dao.getAll().first()
        val microsoftAccounts = accounts.filter { it.kind == AccountKind.MICROSOFT && it.refreshToken != null }
        for (entity in microsoftAccounts) {
            val profile = microsoftAuth.fetchProfile(entity.refreshToken!!)
                ?: continue
            val activeSkin = profile.activeSkin
            val activeCape = profile.activeCape
            val newSkinUrl = activeSkin?.url
            val newCapeUrl = activeCape?.url
            val newVariant = activeSkin?.variant
            val skinChanged = newSkinUrl != null && newSkinUrl != entity.skinFileName
            val capeChanged = newCapeUrl != null && newCapeUrl != entity.capeFileName
            val variantChanged = newVariant != null && newVariant != entity.skinVariant
            if (skinChanged || capeChanged || variantChanged) {
                var skinFileName = entity.skinFileName
                var capeFileName = entity.capeFileName
                if (skinChanged) {
                    skinFileName = media.importImageFromUrl(
                        newSkinUrl,
                        ImportKind.SKIN
                    ) ?: entity.skinFileName
                }
                if (capeChanged) {
                    capeFileName = media.importImageFromUrl(
                        newCapeUrl,
                        ImportKind.CAPE
                    ) ?: entity.capeFileName
                }
                dao.update(entity.copy(
                    skinFileName = skinFileName,
                    capeFileName = capeFileName,
                    skinVariant = newVariant ?: entity.skinVariant,
                ))
            }
        }
    }
}
