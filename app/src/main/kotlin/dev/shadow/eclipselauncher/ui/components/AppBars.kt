/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.core.model.InstanceSummary
import dev.shadow.eclipselauncher.core.model.LoaderKind
import dev.shadow.eclipselauncher.designsystem.component.EclipseBevelButton
import dev.shadow.eclipselauncher.designsystem.component.EclipseDivider
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.component.eclipseFocusRing
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette

@Composable
fun AccountBar(
    accountName: String,
    taskCount: Int,
    onAccountClick: () -> Unit,
    onTasksClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EclipsePalette.AccountBar)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .eclipseFocusRing()
                .clickable(
                    role = Role.Button,
                    onClick = onAccountClick,
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EclipsePixelIcon(
                kind = EclipsePixelIconKind.ACCOUNT,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = accountName,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    color = EclipsePalette.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.account_local),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = EclipsePalette.Muted,
                    maxLines = 1,
                )
            }
        }

        EclipseBarIconButton(
            kind = EclipsePixelIconKind.TASKS,
            description = if (taskCount > 0) {
                stringResource(R.string.cd_tasks_active, taskCount)
            } else {
                stringResource(R.string.cd_tasks)
            },
            onClick = onTasksClick,
            badge = taskCount,
        )
        EclipseBarIconButton(
            kind = EclipsePixelIconKind.SETTINGS,
            description = stringResource(R.string.cd_settings),
            onClick = onSettingsClick,
        )
    }
}

@Composable
private fun EclipseBarIconButton(
    kind: EclipsePixelIconKind,
    description: String,
    onClick: () -> Unit,
    badge: Int = 0,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .eclipseFocusRing()
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        EclipsePixelIcon(
            kind = kind,
            modifier = Modifier.size(22.dp),
        )
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .background(EclipsePalette.DangerFill, CircleShape)
                    .widthIn(min = 16.dp)
                    .heightIn(min = 16.dp)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge.toString(),
                    modifier = Modifier.clearAndSetSemantics { },
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = EclipsePalette.Text,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun BottomInstanceBar(
    instance: InstanceSummary?,
    onEditInstance: () -> Unit,
    onCreateInstance: () -> Unit,
    onPlayClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val editInstanceDescription = if (instance == null) {
        stringResource(R.string.cd_create_instance)
    } else {
        stringResource(R.string.cd_edit_instance)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EclipsePalette.BottomBar)
            .navigationBarsPadding(),
    ) {
        EclipseDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .eclipseFocusRing()
                        .clickable(
                            role = Role.Button,
                            onClick = { menuOpen = true },
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = instance?.name ?: stringResource(R.string.home_no_instance),
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = EclipsePalette.Text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = instance?.let {
                                "${it.minecraftVersion} · ${it.loader.displayName()}"
                            } ?: stringResource(R.string.home_create_instance),
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = EclipsePalette.Muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    EclipsePixelIcon(
                        kind = EclipsePixelIconKind.CHEVRON_UP,
                        modifier = Modifier.size(16.dp),
                        tint = EclipsePalette.Muted,
                    )
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    if (instance != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.home_edit_instance)) },
                            onClick = {
                                menuOpen = false
                                onEditInstance()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_new_instance)) },
                        onClick = {
                            menuOpen = false
                            onCreateInstance()
                        },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .eclipseFocusRing()
                    .clickable(role = Role.Button) {
                        if (instance == null) onCreateInstance() else onEditInstance()
                    }
                    .semantics { contentDescription = editInstanceDescription },
                contentAlignment = Alignment.Center,
            ) {
                EclipsePixelIcon(
                    kind = EclipsePixelIconKind.EDIT,
                    modifier = Modifier.size(22.dp),
                )
            }

            Box(modifier = Modifier.width(116.dp)) {
                EclipseBevelButton(
                    text = stringResource(R.string.home_play),
                    onClick = onPlayClick,
                    enabled = instance != null,
                    primary = true,
                    height = 52.dp,
                )
            }
        }
    }
}

@Composable
fun LoaderKind.displayName(): String = stringResource(
    when (this) {
        LoaderKind.VANILLA -> R.string.create_instance_vanilla
        LoaderKind.FABRIC -> R.string.create_instance_fabric
        LoaderKind.QUILT -> R.string.create_instance_quilt
        LoaderKind.LEGACY_FABRIC -> R.string.mojo_modloader_dl_install_legacy_fabric_instance
        LoaderKind.FORGE -> R.string.create_instance_forge
        LoaderKind.NEOFORGE -> R.string.create_instance_neoforge
        LoaderKind.OPTIFINE -> R.string.create_instance_optifine
        LoaderKind.MODPACK -> R.string.create_instance_modpack
        LoaderKind.BTA -> R.string.mojo_create_bta_instance
    },
)
