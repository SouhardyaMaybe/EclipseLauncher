/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Shared "create local account" dialog: used by the first-launch account
 * gate on HOME and by the ACCOUNTS screen's add row.
 */
package com.eclipselauncher.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.eclipselauncher.R

/** Minecraft usernames: 3-16 letters, digits or underscore. */
internal fun isValidMinecraftName(name: String): Boolean =
    name.matches(Regex("[A-Za-z0-9_]{3,16}"))

@Composable
fun LocalAccountDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.local_account_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(16) },
                label = { Text(stringResource(R.string.local_account_hint)) },
                singleLine = true,
                isError = name.isNotEmpty() && !isValidMinecraftName(name),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = isValidMinecraftName(name.trim()),
            ) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
