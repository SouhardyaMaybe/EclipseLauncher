/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The nametag's account switcher: a fixed-size dropdown listing every added
 * account as a radio row (Steve head with its hat overlay + username + kind)
 * that switches the selection directly on tap. The account list owns the
 * scrolling and snaps to the selected account on every open; "+ add
 * account" sits below the divider as a pinned footer, so the action never
 * scrolls out of reach no matter how many accounts exist.
 * Presented in the standard Material 3 basic dialog.
 */
package com.eclipselauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eclipselauncher.R
import com.eclipselauncher.data.AccountKind
import com.eclipselauncher.data.AccountState
import com.eclipselauncher.data.MediaStorage
import androidx.compose.material3.BasicAlertDialog
import com.eclipselauncher.ui.character.SkinHeadFace
import com.eclipselauncher.ui.theme.EclipseRadius

/** Fixed dropdown dimensions: the list flexes, the frame never does. */
private val SwitcherWidth = 300.dp
private val SwitcherHeight = 340.dp

/**
 * A radio list of the added accounts; tapping a row selects it directly.
 * The pinned footer row opens ACCOUNTS to add or manage accounts. The
 * frame only fits a couple of rows, so every open snaps the list to the
 * currently selected account — the one the nametag is showing must be
 * visible without hunting for it.
 */
@Composable
fun AccountSwitcherDialog(
    visible: Boolean,
    accountState: AccountState,
    media: MediaStorage,
    onSelect: (String) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    // Opening the dialog scrolls the list to the selected account. Keying
    // the effect on `visible` re-snaps on every open.
    val listState = rememberLazyListState()
    LaunchedEffect(visible) {
        if (visible) {
            val index = accountState.accounts.indexOfFirst { it.id == accountState.selectedId }
            if (index >= 0) {
                listState.scrollToItem(index)
            }
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(EclipseRadius.dialog),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            // Fixed frame: title, the account list taking the remaining
            // height (it snaps to the selection on open — only a couple
            // of rows fit), then the pinned add-account footer.
            Column(
                modifier = Modifier
                    .width(SwitcherWidth)
                    .height(SwitcherHeight)
                    .padding(horizontal = 12.dp, vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.accounts_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(modifier = Modifier.size(8.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(accountState.accounts, key = { it.id }) { account ->
                        val selected = account.id == accountState.selectedId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Direct switch, then close.
                                    onSelect(account.id)
                                    onDismiss()
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            SkinHeadFace(
                                skinFileName = account.skinFileName,
                                media = media,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.username,
                                    style = MaterialTheme.typography.bodyLarge,
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
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))

                // Pinned footer: leaves for the ACCOUNTS screen.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(EclipseRadius.tile))
                        .clickable {
                            onDismiss()
                            onAddAccount()
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Same footprint as a head thumb so the rows align.
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.action_add_account),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
