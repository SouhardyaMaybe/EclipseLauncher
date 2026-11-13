/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Shared form pieces for the instance editor. They are built from the
 * standard Material 3 controls: an outlined button for a read-only picker,
 * and ExposedDropdownMenuBox + OutlinedTextField for an anchored selector.
 */
package com.eclipselauncher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A labelled read-only field that opens a picker on click. */
@Composable
fun SelectField(
    label: String,
    value: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        SpinnerField(value = value.ifBlank { hint }, onClick = onClick)
    }
}

/**
 * A read-only picker row: an outlined Material 3 button showing [value] (or
 * [hint] when blank), capped at two lines.
 */
@Composable
fun SpinnerField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.startsWith("(")) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The pick-one control the instance editor uses for control maps, runtime
 * and renderer: a Material 3 exposed dropdown anchored to an outlined text
 * field, with a check on the active option.
 */
@Composable
fun DropdownSelectField(
    label: String,
    value: String,
    hint: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value.ifBlank { hint },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = {
                        if (index == selectedIndex) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                )
            }
        }
    }
}
