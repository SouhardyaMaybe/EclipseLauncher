/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette

/**
 * Layout primitives shared by the pushed screens. They follow the reference
 * launcher's preference rows, content cards, search row and two-button bottom
 * bar, while keeping the Ore palette and original pixel glyphs.
 */

@Composable
fun EclipseBackRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 52.dp)
            .eclipseFocusRing()
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EclipsePixelIcon(
            kind = EclipsePixelIconKind.ARROW_LEFT,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = EclipsePalette.Text,
        )
    }
}

@Composable
fun EclipsePreferenceRow(
    icon: EclipsePixelIconKind,
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .eclipseFocusRing(enabled)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(start = 22.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EclipsePixelIcon(
            kind = icon,
            modifier = Modifier.size(30.dp),
            tint = if (enabled) EclipsePalette.Text else EclipsePalette.Disabled,
        )
        Spacer(Modifier.width(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) EclipsePalette.Text else EclipsePalette.Disabled,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary.isNotBlank()) {
                Spacer(Modifier.size(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) EclipsePalette.Muted else EclipsePalette.Disabled,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        EclipsePixelIcon(
            kind = EclipsePixelIconKind.CHEVRON_RIGHT,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) EclipsePalette.Muted else EclipsePalette.Disabled,
        )
    }
}

@Composable
fun EclipseContentCard(
    icon: EclipsePixelIconKind,
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(EclipsePalette.Panel)
            .border(2.dp, EclipsePalette.Border)
            .eclipseFocusRing()
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .padding(2.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(65.dp)
                .background(EclipsePalette.Canvas)
                .border(1.dp, EclipsePalette.Border),
            contentAlignment = Alignment.Center,
        ) {
            EclipsePixelIcon(
                kind = icon,
                modifier = Modifier.size(34.dp),
                tint = EclipsePalette.Primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = EclipsePalette.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = EclipsePalette.Primary,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = EclipsePalette.Muted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun EclipseSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    filterDescription: String = "",
    onFilterClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            singleLine = true,
            shape = RectangleShape,
            colors = eclipseTextFieldColors(),
        )
        if (onFilterClick != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .eclipseFocusRing()
                    .clickable(
                        role = Role.Button,
                        onClick = onFilterClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                EclipsePixelIcon(
                    kind = EclipsePixelIconKind.FILTER,
                    modifier = Modifier.size(22.dp),
                    description = filterDescription,
                )
            }
        }
    }
}

@Composable
fun EclipseBottomActionRow(
    startLabel: String,
    onStart: () -> Unit,
    endLabel: String,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
    startEnabled: Boolean = true,
    endEnabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EclipsePalette.BottomBar)
            .navigationBarsPadding(),
    ) {
        EclipseDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EclipseBevelButton(
                text = startLabel,
                onClick = onStart,
                modifier = Modifier.weight(1f),
                enabled = startEnabled,
                primary = false,
                height = 52.dp,
            )
            EclipseBevelButton(
                text = endLabel,
                onClick = onEnd,
                modifier = Modifier.weight(1f),
                enabled = endEnabled,
                primary = true,
                height = 52.dp,
            )
        }
    }
}

@Composable
fun EclipseValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = EclipsePalette.Text,
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = EclipsePalette.Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
fun EclipseSmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 96.dp,
    enabled: Boolean = true,
) {
    Box(modifier = modifier.width(width)) {
        EclipseBevelButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
            primary = false,
            height = 48.dp,
        )
    }
}

/**
 * The single full-width action at the bottom of a list screen. It mirrors the
 * reference launcher's "Import local modpack" footer without inventing a
 * second button when the flow only has one action.
 */
@Composable
fun EclipseBottomButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supporting: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EclipsePalette.BottomBar)
            .navigationBarsPadding(),
    ) {
        EclipseDivider()
        if (supporting != null) {
            Text(
                text = supporting,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
                style = MaterialTheme.typography.labelSmall,
                color = EclipsePalette.Muted,
            )
        }
        EclipseBevelButton(
            text = label,
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            enabled = enabled,
            primary = true,
            height = 52.dp,
        )
    }
}

@Composable
fun EclipseSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .eclipseFocusRing()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = EclipsePalette.Text,
            )
            if (summary.isNotBlank()) {
                Spacer(Modifier.size(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = EclipsePalette.Muted,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@Composable
fun EclipseSelectRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .eclipseFocusRing()
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = EclipsePalette.Text,
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = EclipsePalette.Muted,
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.width(10.dp))
        EclipsePixelIcon(
            kind = EclipsePixelIconKind.CHEVRON_DOWN,
            modifier = Modifier.size(14.dp),
            tint = EclipsePalette.Muted,
        )
    }
}
