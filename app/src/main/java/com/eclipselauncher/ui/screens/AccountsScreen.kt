/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * ACCOUNTS: pick between Microsoft and local accounts, manage existing
 * ones. The two "add account" actions are pinned under the app bar and
 * never scroll away; only the account list below them scrolls. Each row
 * shows the account's skin head — the default Steve (base + hat overlay)
 * when no custom skin is set, never a letter tile — with a dress icon that
 * opens the skin dialog (3D model, pick/clear skin and cape) and a delete
 * icon that arms a removal this screen then confirms in a dialog before it
 * happens. Selecting a row makes it the active account and returns to HOME.
 */
package com.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eclipselauncher.R
import com.eclipselauncher.data.Account
import com.eclipselauncher.data.AccountKind
import com.eclipselauncher.data.AccountState
import com.eclipselauncher.data.MediaStorage
import com.eclipselauncher.ui.character.SkinHeadFace
import com.eclipselauncher.ui.components.LocalAccountDialog
import com.eclipselauncher.ui.components.SkinCustomizationDialog
import com.eclipselauncher.ui.theme.EclipseRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accountState: AccountState,
    media: MediaStorage,
    onBack: () -> Unit,
    onAddMicrosoft: () -> Unit,
    onSelect: (String) -> Unit,
    onCreateLocal: (String) -> Unit,
    onUpdate: (Account) -> Unit,
    onRemove: (String) -> Unit,
) {
    var showLocalDialog by remember { mutableStateOf(false) }
    // Which account's skin dialog is open (null = none).
    var customizingId by remember { mutableStateOf<String?>(null) }
    val customizing = accountState.accounts.firstOrNull { it.id == customizingId }
    // Which account's deletion awaits confirmation (null = none). The row's
    // trash icon only stages the delete; this dialog performs it.
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    val pendingDelete = accountState.accounts.firstOrNull { it.id == pendingDeleteId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.accounts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Fixed header: the two add-account actions, always visible and
            // never part of the list's scroll. Full width each, so long
            // labels never clip on narrow screens.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onAddMicrosoft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(EclipseRadius.control),
                ) {
                    Text(
                        text = stringResource(R.string.action_sign_in_microsoft),
                        maxLines = 1,
                    )
                }
                Button(
                    onClick = { showLocalDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(EclipseRadius.control),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_create_local),
                        maxLines = 1,
                    )
                }
            }

            // Scrollable account list; the header above stays put.
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(accountState.accounts, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        isSelected = account.id == accountState.selectedId,
                        media = media,
                        onClick = {
                            onSelect(account.id)
                            onBack()
                        },
                        onCustomize = { customizingId = account.id },
                        onDelete = { pendingDeleteId = account.id },
                    )
                }

                if (accountState.accounts.isEmpty()) {
                    item {
                        // Just the title now: the two add-account buttons
                        // above already explain how to add one.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.no_account_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLocalDialog) {
        LocalAccountDialog(
            onConfirm = { name ->
                onCreateLocal(name)
                showLocalDialog = false
            },
            onDismiss = { showLocalDialog = false },
        )
    }

    if (customizing != null) {
        SkinCustomizationDialog(
            account = customizing,
            media = media,
            onUpdate = onUpdate,
            onDismiss = { customizingId = null },
        )
    }

    // Deletion is destructive and irreversible from the launcher's side, so
    // the trash icon only arms it — this dialog names the account and asks
    // before anything is removed.
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = {
                Text(stringResource(R.string.delete_account_message, pendingDelete.username))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteId = null
                        onRemove(pendingDelete.id)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/**
 * One account: its skin head (default Steve with the hat overlay when the
 * account has no custom skin), username and kind, then the dress and delete
 * actions. The whole row selects; the icons stop the click from bubbling.
 */
@Composable
private fun AccountRow(
    account: Account,
    isSelected: Boolean,
    media: MediaStorage,
    onClick: () -> Unit,
    onCustomize: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EclipseRadius.tile))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Skin head: base face + hat overlay, so local accounts without a
        // custom skin show Steve's textured head instead of a letter tile.
        SkinHeadFace(
            skinFileName = account.skinFileName,
            media = media,
            modifier = Modifier.size(44.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.username,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    when (account.kind) {
                        AccountKind.MICROSOFT -> R.string.account_kind_microsoft
                        AccountKind.LOCAL -> R.string.account_kind_local
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (isSelected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.cd_selected_account),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
        // Dress: opens the skin dialog for this account.
        IconButton(onClick = onCustomize) {
            Icon(
                Icons.Filled.Checkroom,
                contentDescription = stringResource(R.string.cd_customize_skin),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp),
            )
        }
        // Delete: arms the removal; the screen's dialog confirms it first.
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.action_delete_account),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
