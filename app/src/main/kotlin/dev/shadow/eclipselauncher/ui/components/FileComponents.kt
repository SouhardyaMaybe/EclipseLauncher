/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.designsystem.component.EclipseDivider
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.component.eclipseFocusRing
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette

@Composable
fun PathBar(
    path: String,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "",
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .eclipseFocusRing()
                    .clickable(
                        role = Role.Button,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                EclipsePixelIcon(
                    kind = EclipsePixelIconKind.ARROW_LEFT,
                    modifier = Modifier.size(20.dp),
                    description = backContentDescription,
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = path,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = EclipsePalette.Muted,
        )
        actions()
    }
}

@Composable
fun FileRow(
    name: String,
    detail: String,
    directory: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
) {
    val interaction = if (selectionMode) {
        Modifier.selectable(
            selected = selected,
            role = Role.Checkbox,
            onClick = onClick,
        )
    } else {
        Modifier.clickable(
            role = Role.Button,
            onClick = onClick,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) EclipsePalette.Control else Color.Transparent)
            .eclipseFocusRing()
            .then(interaction)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(2.dp, if (selected) EclipsePalette.Primary else EclipsePalette.Border),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(EclipsePalette.Primary),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
        }

        EclipsePixelIcon(
            kind = if (directory) EclipsePixelIconKind.FOLDER else EclipsePixelIconKind.FILE,
            modifier = Modifier.size(24.dp),
            tint = if (selected) EclipsePalette.Primary else EclipsePalette.Muted,
        )
        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = EclipsePalette.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = EclipsePalette.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (directory) {
            EclipsePixelIcon(
                kind = EclipsePixelIconKind.CHEVRON_RIGHT,
                modifier = Modifier.size(14.dp),
                tint = EclipsePalette.Muted,
            )
        }
    }
    EclipseDivider(Modifier.padding(start = if (selectionMode) 88.dp else 56.dp))
}

@Composable
fun SelectionActionBar(
    selectedText: String,
    canRename: Boolean,
    canPaste: Boolean,
    canShare: Boolean,
    onDone: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EclipsePalette.Panel)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = EclipsePalette.Text,
            )
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.action_done))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(stringResource(R.string.action_copy), onClick = onCopy)
            ActionButton(stringResource(R.string.action_cut), onClick = onCut)
            ActionButton(stringResource(R.string.action_paste), enabled = canPaste, onClick = onPaste)
            ActionButton(stringResource(R.string.action_rename), enabled = canRename, onClick = onRename)
            ActionButton(stringResource(R.string.action_delete), onClick = onDelete)
            ActionButton(stringResource(R.string.action_share), enabled = canShare, onClick = onShare)
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}
