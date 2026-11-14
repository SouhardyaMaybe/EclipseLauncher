/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The version selector dialog: collapsible groups (Latest / Release /
 * Snapshot / Old-beta / Old-alpha) loaded from the piston-meta manifest,
 * with a loading state and an error dialog on failure. Adapted from the
 * old EclipseLauncher's MojoVersionSelectorDialog to Material 3.
 */
package me.shadow.eclipselauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.data.VersionGroup
import me.shadow.eclipselauncher.data.VersionOption

/**
 * Shows [groups] in a modal list of expandable headers; the first group
 * starts expanded. [onPick] receives the chosen version option.
 */
@Composable
fun VersionSelectorDialog(
    title: String,
    groups: List<VersionGroup>,
    loading: Boolean,
    onPick: (VersionOption) -> Unit,
    onDismiss: () -> Unit,
) {
    var expanded by remember(groups) {
        mutableStateOf(groups.firstOrNull()?.title?.let { setOf(it) } ?: emptySet())
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                )
                when {
                    loading -> LoadingRow()
                    groups.all { it.options.isEmpty() } -> EmptyRow()
                    else -> GroupList(
                        groups = groups,
                        expanded = expanded,
                        onToggle = { name ->
                            expanded = if (name in expanded) expanded - name else expanded + name
                        },
                        onPick = onPick,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.version_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyRow() {
    Text(
        text = stringResource(R.string.version_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(24.dp),
    )
}

@Composable
private fun GroupList(
    groups: List<VersionGroup>,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    onPick: (VersionOption) -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 420.dp)) {
        groups.forEach { group ->
            if (group.options.isEmpty()) return@forEach
            val isOpen = group.title in expanded
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(group.title) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = group.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = if (isOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = isOpen,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    group.options.forEach { option ->
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (option.label == option.id) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(option) }
                                .padding(start = 36.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

/** A simple error dialog for version-list failures. */
@Composable
fun VersionErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.version_error_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
    )
}
