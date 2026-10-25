/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.BuildConfig
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.data.LauncherState
import dev.shadow.eclipselauncher.designsystem.component.EclipseBevelButton
import dev.shadow.eclipselauncher.designsystem.component.EclipseCenteredHeading
import dev.shadow.eclipselauncher.designsystem.component.EclipseStatusChip
import dev.shadow.eclipselauncher.designsystem.component.EclipseTopBar
import dev.shadow.eclipselauncher.designsystem.component.eclipseTextFieldColors
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette

@Composable
fun AccountsScreen(
    state: LauncherState,
    onBack: () -> Unit,
) {
    var name by remember(state.accountName) { mutableStateOf(state.accountName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        EclipseTopBar(
            title = stringResource(R.string.account_title),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            EclipseCenteredHeading(
                text = stringResource(R.string.account_local),
                topPadding = 24.dp,
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                EclipseStatusChip(
                    text = stringResource(R.string.account_local_chip),
                    color = EclipsePalette.PrimaryTop,
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.account_local_name)) },
                singleLine = true,
                colors = eclipseTextFieldColors(),
            )
            Spacer(Modifier.height(12.dp))
            EclipseBevelButton(
                text = stringResource(R.string.account_local_save),
                onClick = { state.setAccountName(name) },
                primary = true,
            )

            Spacer(Modifier.height(32.dp))
            EclipseCenteredHeading(
                text = stringResource(R.string.account_microsoft),
                topPadding = 8.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (BuildConfig.MS_CLIENT_ID.isBlank()) {
                    stringResource(R.string.account_microsoft_not_configured)
                } else {
                    stringResource(R.string.account_microsoft_configured)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = EclipsePalette.Text,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.account_microsoft_body),
                style = MaterialTheme.typography.bodySmall,
                color = EclipsePalette.Muted,
            )
            Spacer(Modifier.height(16.dp))
            EclipseBevelButton(
                text = stringResource(R.string.account_microsoft_action),
                onClick = {},
                enabled = false,
                primary = false,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
