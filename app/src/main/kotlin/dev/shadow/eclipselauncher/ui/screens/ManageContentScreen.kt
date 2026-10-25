/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.core.model.InstanceSummary
import dev.shadow.eclipselauncher.core.model.LoaderKind
import dev.shadow.eclipselauncher.data.ContentType
import dev.shadow.eclipselauncher.data.FileOps
import dev.shadow.eclipselauncher.data.LauncherState
import dev.shadow.eclipselauncher.designsystem.component.EclipseDivider
import dev.shadow.eclipselauncher.designsystem.component.EclipseEmptyState
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.component.EclipseSectionLabel
import dev.shadow.eclipselauncher.designsystem.component.EclipseTab
import dev.shadow.eclipselauncher.designsystem.component.EclipseTopBar
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette
import dev.shadow.eclipselauncher.ui.components.ConfirmDialog
import dev.shadow.eclipselauncher.ui.components.FileInfoDialog
import dev.shadow.eclipselauncher.ui.components.FileRow
import dev.shadow.eclipselauncher.ui.components.displayName
import java.io.File

private enum class ContentSource { MODRINTH, CURSEFORGE }

@Composable
fun ManageContentScreen(
    instance: InstanceSummary?,
    state: LauncherState,
    initialInstalled: Boolean,
    initialType: ContentType,
    onBack: () -> Unit,
    onOpenFile: (File) -> Unit,
    onOpenDirectory: (File) -> Unit,
) {
    val context = LocalContext.current
    var installed by remember { mutableStateOf(initialInstalled) }
    var type by remember { mutableStateOf(initialType) }
    var refresh by remember { mutableIntStateOf(0) }
    var infoFile by remember { mutableStateOf<File?>(null) }
    var deleteFile by remember { mutableStateOf<File?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        EclipseTopBar(
            title = stringResource(R.string.content_title),
            onBack = onBack,
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            EclipseTab(
                text = stringResource(R.string.content_installed),
                selected = installed,
                onClick = { installed = true },
                modifier = Modifier.weight(1f),
            )
            EclipseTab(
                text = stringResource(R.string.content_discover),
                selected = !installed,
                onClick = { installed = false },
                modifier = Modifier.weight(1f),
            )
        }
        EclipseDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            ContentType.entries.forEach { item ->
                EclipseTab(
                    text = stringResource(item.labelRes),
                    selected = type == item,
                    onClick = { type = item },
                )
            }
        }
        EclipseDivider()

        if (installed) {
            InstalledContent(
                instance = instance,
                state = state,
                type = type,
                refresh = refresh,
                onInfo = { infoFile = it },
            )
        } else {
            DiscoverContent(
                instance = instance,
                state = state,
                type = type,
            )
        }
    }

    infoFile?.let { file ->
        FileInfoDialog(
            file = file,
            path = ".minecraft/${FileOps.relativePath(state.minecraftDir, file)}",
            detail = if (file.isDirectory) {
                stringResource(R.string.files_folder)
            } else {
                FileOps.formatSize(context, file)
            },
            onOpen = {
                infoFile = null
                onOpenFile(file)
            },
            onOpenFolder = {
                infoFile = null
                onOpenDirectory(file)
            },
            onShare = {
                FileOps.share(context, listOf(file))
                infoFile = null
            },
            onDelete = {
                infoFile = null
                deleteFile = file
            },
            onDismiss = { infoFile = null },
        )
    }

    deleteFile?.let { file ->
        ConfirmDialog(
            title = stringResource(R.string.content_delete_title, file.name),
            body = stringResource(R.string.content_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                FileOps.delete(listOf(file))
                refresh++
                deleteFile = null
            },
            onDismiss = { deleteFile = null },
        )
    }
}

@Composable
private fun ColumnScope.InstalledContent(
    instance: InstanceSummary?,
    state: LauncherState,
    type: ContentType,
    refresh: Int,
    onInfo: (File) -> Unit,
) {
    if (instance == null) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            EclipseEmptyState(
                title = stringResource(R.string.content_no_instance_title),
                body = stringResource(R.string.content_no_instance_body),
            )
        }
        return
    }

    val context = LocalContext.current
    val directory = remember(instance.id, type) { state.contentDir(type, instance) }
    val files = remember(directory, refresh) { FileOps.list(directory) }

    if (files.isEmpty()) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            EclipseEmptyState(
                title = stringResource(R.string.content_empty_title),
                body = if (type == ContentType.WORLDS) {
                    stringResource(R.string.content_worlds_empty_body)
                } else {
                    stringResource(R.string.content_empty_body, stringResource(type.labelRes))
                },
            )
        }
    } else {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(files, key = { it.absolutePath }) { file ->
                FileRow(
                    name = file.name,
                    detail = if (file.isDirectory) {
                        stringResource(R.string.files_folder)
                    } else {
                        FileOps.formatSize(context, file)
                    },
                    directory = file.isDirectory,
                    selected = false,
                    selectionMode = false,
                    onClick = { onInfo(file) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.DiscoverContent(
    instance: InstanceSummary?,
    state: LauncherState,
    type: ContentType,
) {
    if (type == ContentType.WORLDS) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            EclipseEmptyState(
                title = stringResource(R.string.content_worlds_discover_title),
                body = stringResource(R.string.content_worlds_discover_body),
            )
        }
        return
    }

    var gameVersion by remember { mutableStateOf<String?>(null) }
    var loader by remember { mutableStateOf<LoaderKind?>(null) }
    var source by remember { mutableStateOf(ContentSource.MODRINTH) }
    var versionMenu by remember { mutableStateOf(false) }
    var loaderMenu by remember { mutableStateOf(false) }

    val versionChoices = remember(instance?.minecraftVersion) {
        buildList<String?> {
            add(null)
            instance?.minecraftVersion?.let { add(it) }
            if (LauncherState.DEFAULT_MINECRAFT_VERSION != instance?.minecraftVersion) {
                add(LauncherState.DEFAULT_MINECRAFT_VERSION)
            }
        }.distinct()
    }
    val curseForgeEnabled = state.curseForgeKey.isNotBlank()
    val curseForgeLabel = if (curseForgeEnabled) {
        stringResource(R.string.content_curseforge)
    } else {
        "${stringResource(R.string.content_curseforge)} · ${stringResource(R.string.content_coming_soon)}"
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        EclipseSectionLabel(stringResource(R.string.content_filters))

        FilterRow(
            label = stringResource(R.string.content_filter_version),
            value = gameVersion ?: stringResource(R.string.content_any_version),
            onClick = { versionMenu = true },
            menu = {
                DropdownMenu(
                    expanded = versionMenu,
                    onDismissRequest = { versionMenu = false },
                ) {
                    versionChoices.forEach { choice ->
                        DropdownMenuItem(
                            text = {
                                Text(choice ?: stringResource(R.string.content_any_version))
                            },
                            onClick = {
                                gameVersion = choice
                                versionMenu = false
                            },
                        )
                    }
                }
            },
        )
        EclipseDivider()

        FilterRow(
            label = stringResource(R.string.content_filter_loader),
            value = loader?.displayName() ?: stringResource(R.string.content_any_loader),
            onClick = { loaderMenu = true },
            menu = {
                DropdownMenu(
                    expanded = loaderMenu,
                    onDismissRequest = { loaderMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.content_any_loader)) },
                        onClick = {
                            loader = null
                            loaderMenu = false
                        },
                    )
                    LoaderKind.entries.forEach { choice ->
                        DropdownMenuItem(
                            text = { Text(choice.displayName()) },
                            onClick = {
                                loader = choice
                                loaderMenu = false
                            },
                        )
                    }
                }
            },
        )
        EclipseDivider()

        EclipseSectionLabel(stringResource(R.string.content_filter_source))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            EclipseTab(
                text = stringResource(R.string.content_modrinth),
                selected = source == ContentSource.MODRINTH,
                onClick = { source = ContentSource.MODRINTH },
            )
            EclipseTab(
                text = curseForgeLabel,
                selected = source == ContentSource.CURSEFORGE,
                onClick = { source = ContentSource.CURSEFORGE },
                enabled = curseForgeEnabled,
            )
        }
        EclipseDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.content_auto_deps),
                    style = MaterialTheme.typography.labelLarge,
                    color = EclipsePalette.Text,
                )
                Text(
                    text = stringResource(R.string.content_auto_deps_summary),
                    style = MaterialTheme.typography.labelSmall,
                    color = EclipsePalette.Muted,
                )
            }
            Switch(
                checked = state.autoInstallDependencies,
                onCheckedChange = state::updateAutoInstallDependencies,
            )
        }
        EclipseDivider()

        Spacer(Modifier.height(24.dp))

        when {
            source == ContentSource.CURSEFORGE && !curseForgeEnabled -> {
                EclipseEmptyState(
                    title = stringResource(R.string.content_curseforge_empty_title),
                    body = stringResource(R.string.content_curseforge_empty_body),
                )
            }

            source == ContentSource.CURSEFORGE -> {
                EclipseEmptyState(
                    title = stringResource(R.string.content_curseforge_ready_title),
                    body = stringResource(R.string.content_curseforge_ready_body),
                )
            }

            else -> {
                EclipseEmptyState(
                    title = stringResource(R.string.content_discover_empty_title),
                    body = stringResource(R.string.content_discover_empty_body),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FilterRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    menu: @Composable () -> Unit,
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
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
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = EclipsePalette.Muted,
            )
            EclipsePixelIcon(
                kind = EclipsePixelIconKind.CHEVRON_DOWN,
                modifier = Modifier.size(14.dp),
                tint = EclipsePalette.Muted,
            )
        }
        menu()
    }
}
