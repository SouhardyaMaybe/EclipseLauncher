/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The play gate: pressed with no account, HOME shows this dialog naming
 * the rule — an account is compulsory for that action — and a single
 * button that continues into ACCOUNTS, where both add-account paths
 * (Microsoft sign-in, local account) live.
 */
package com.eclipselauncher.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eclipselauncher.R

@Composable
fun AddAccountGate(
    onConnect: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    AlertDialog(
        // Play-gating can be dismissed: the dialog only states the rule and
        // points at ACCOUNTS, where the account actually gets added.
        onDismissRequest = { onDismiss?.invoke() },
        text = { Text(stringResource(R.string.account_gate_message)) },
        confirmButton = {
            TextButton(onClick = onConnect) {
                Text(stringResource(R.string.action_connect_account))
            }
        },
    )
}
