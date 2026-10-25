/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.BuildConfig
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.data.LauncherState
import dev.shadow.eclipselauncher.designsystem.component.EclipseBackRow
import dev.shadow.eclipselauncher.designsystem.component.EclipseBevelButton
import dev.shadow.eclipselauncher.designsystem.component.EclipseDivider
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.component.EclipsePreferenceRow
import dev.shadow.eclipselauncher.designsystem.component.EclipseSectionLabel
import dev.shadow.eclipselauncher.designsystem.component.eclipseTextFieldColors
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette

private enum class AccountsPane { LIST, METHODS, LOCAL }

private val LocalName = Regex("^[A-Za-z0-9_]{1,16}$")

/**
 * Accounts follows the reference launcher's add-account flow: a list entry,
 * a centred card of authentication methods, then a local username card. The
 * local account stays warning-free; the field only explains its character
 * rule when the typed name is invalid.
 */
@Composable
fun AccountsScreen(
    state: LauncherState,
    onBack: () -> Unit,
) {
    var pane by rememberSaveable { mutableStateOf(AccountsPane.LIST) }
    var name by remember(state.accountName) { mutableStateOf(state.accountName) }

    BackHandler(enabled = pane != AccountsPane.LIST) {
        pane = when (pane) {
            AccountsPane.LOCAL -> AccountsPane.METHODS
            else -> AccountsPane.LIST
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        when (pane) {
            AccountsPane.LIST -> AccountList(
                state = state,
                onBack = onBack,
                onAdd = { pane = AccountsPane.METHODS },
                onEditLocal = {
                    name = state.accountName
                    pane = AccountsPane.LOCAL
                },
            )

            AccountsPane.METHODS -> AuthMethods(
                onBack = { pane = AccountsPane.LIST },
                onLocal = {
                    name = state.accountName
                    pane = AccountsPane.LOCAL
                },
            )

            AccountsPane.LOCAL -> LocalLogin(
                name = name,
                onNameChange = { name = it },
                onBack = { pane = AccountsPane.METHODS },
                onSave = {
                    state.updateAccountName(name)
                    pane = AccountsPane.LIST
                },
            )
        }
    }
}

@Composable
private fun AccountList(
    state: LauncherState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEditLocal: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        EclipseBackRow(
            label = stringResource(R.string.action_back),
            onClick = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            EclipseSectionLabel(stringResource(R.string.account_signed_in))
            EclipsePreferenceRow(
                icon = EclipsePixelIconKind.ACCOUNT,
                title = state.accountName,
                summary = stringResource(R.string.account_local),
                onClick = onEditLocal,
            )
            EclipseDivider()

            EclipseBevelButton(
                text = stringResource(R.string.account_add),
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                primary = true,
                height = 52.dp,
            )

            Text(
                text = stringResource(R.string.account_list_note),
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = EclipsePalette.Muted,
            )
        }
    }
}

@Composable
private fun AuthMethods(
    onBack: () -> Unit,
    onLocal: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        EclipseBackRow(
            label = stringResource(R.string.action_back),
            onClick = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EclipsePalette.Panel)
                    .border(2.dp, EclipsePalette.Border)
                    .padding(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.account_add),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    color = EclipsePalette.Text,
                )
                Spacer(Modifier.height(20.dp))

                EclipseBevelButton(
                    text = stringResource(R.string.account_microsoft_action),
                    onClick = {},
                    enabled = false,
                    primary = false,
                    height = 52.dp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (BuildConfig.MS_CLIENT_ID.isBlank()) {
                        stringResource(R.string.account_microsoft_not_configured)
                    } else {
                        stringResource(R.string.account_microsoft_configured)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = EclipsePalette.Muted,
                )
                Spacer(Modifier.height(16.dp))

                EclipseBevelButton(
                    text = stringResource(R.string.account_local),
                    onClick = onLocal,
                    primary = true,
                    height = 52.dp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.account_local_summary),
                    style = MaterialTheme.typography.labelSmall,
                    color = EclipsePalette.Muted,
                )
                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.account_microsoft_pending),
                    style = MaterialTheme.typography.bodySmall,
                    color = EclipsePalette.Muted,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LocalLogin(
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val valid = LocalName.matches(name)

    Column(modifier = Modifier.fillMaxSize()) {
        EclipseBackRow(
            label = stringResource(R.string.action_back),
            onClick = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EclipsePalette.Panel)
                    .border(2.dp, EclipsePalette.Border)
                    .padding(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.account_local),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    color = EclipsePalette.Text,
                )
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.account_local_name)) },
                    isError = name.isNotEmpty() && !valid,
                    supportingText = {
                        if (name.isNotEmpty() && !valid) {
                            Text(stringResource(R.string.account_local_invalid))
                        }
                    },
                    singleLine = true,
                    colors = eclipseTextFieldColors(),
                )
                Spacer(Modifier.height(16.dp))

                EclipseBevelButton(
                    text = stringResource(R.string.account_local_save),
                    onClick = onSave,
                    enabled = valid,
                    primary = true,
                    height = 52.dp,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
