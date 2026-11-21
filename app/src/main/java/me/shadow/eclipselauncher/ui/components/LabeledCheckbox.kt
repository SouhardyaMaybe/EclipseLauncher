/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * A reusable labeled checkbox row where the entire row is clickable
 * and toggles the checkbox. Matches Material 3 CheckboxListItem behavior.
 */
package me.shadow.eclipselauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A labeled checkbox row where the entire row is clickable and toggles the checkbox.
 * This is the shared implementation for all checkbox rows in the app.
 *
 * @param checked Whether the checkbox is checked
 * @param onCheckedChange Callback when the checked state changes
 * @param label The label text to display next to the checkbox
 * @param enabled Whether the checkbox is enabled
 * @param modifier Optional modifier for the row
 * @param labelStyle Text style for the label
 */
@Composable
fun LabeledCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    labelStyle: androidx.compose.ui.text.TextStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = { onCheckedChange(!checked) })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.width(8.dp))
        Text(
            text = label,
            style = labelStyle,
            color = if (enabled) {
                androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            } else {
                androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}