/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import dev.shadow.eclipselauncher.designsystem.component.EclipseBackRow
import dev.shadow.eclipselauncher.designsystem.component.EclipseBottomButton
import dev.shadow.eclipselauncher.designsystem.component.EclipseContentCard
import dev.shadow.eclipselauncher.designsystem.component.EclipseDivider
import dev.shadow.eclipselauncher.designsystem.component.EclipseEmptyState
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.component.EclipseSearchBar
import dev.shadow.eclipselauncher.designsystem.component.EclipseSelectRow
import dev.shadow.eclipselauncher.designsystem.component.EclipseSwitchRow
import dev.shadow.eclipselauncher.designsystem.component.EclipseTab
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette
import dev.shadow.eclipselauncher.ui.components.ConfirmDialog
import dev.shadow.eclipselauncher.ui.components.FileInfoDialog
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
    onStateChanged: (Boolean, ContentType) -> Unit,
    onOpenFile: (File) -> Unit,
    onOpenDirectory: (File) -> Unit,
) {
    val context = LocalContext.current
    var installed by remember { mutableStateOf(initialInstalled) }
    var type by remember { mutableStateOf(initialType) }
    var query by rememberSaveable { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var infoFile by remember { mutableStateOf<File?>(null) }
    var deleteFile by remember { mutableStateOf<File?>(null) }
    var filtersOpen by remember { mutableStateOf(false) }

    var gameVersion by rememberSaveable { mutableStateOf<String?>(null) }
    var loader by rememberSaveable { mutableStateOf<LoaderKind?>(null) }
    var source by rememberSaveable { mutableStateOf(ContentSource.MODRINTH) }

    LaunchedEffect(installed, type) {
        onStateChanged(installed, type)
    }

    val versionChoices = remember(instance?.minecraftVersion) {
        buildList<String?> {
            add(null)
            instance?.minecraftVersion?.let { add(it) }
            if (LauncherState.DEFAULT_MINECRAFT_VERSION != instance?.minecraftVersion) {
                add(LauncherState.DEFAULT_MINECRAFT_VERSION)
            }
        }.distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        EclipseBackRow(
            label = stringResource(R.string.action_back),
            onClick = onBack,
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
            if (instance != null) {
                EclipseSearchBar(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.content_search_installed),
                )
            }
            InstalledContent(
                instance = instance,
                state = state,
                type = type,
                query = query,
                refresh = refresh,
                onInfo = { infoFile = it },
            )
        } else {
            EclipseSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.content_search_projects),
                filterDescription = stringResource(R.string.content_filters),
                onFilterClick = { filtersOpen = true },
            )
            DiscoverContent(
                type = type,
                gameVersion = gameVersion,
                loader = loader,
                source = source,
                curseForgeEnabled = state.curseForgeKey.isNotBlank(),
            )
            if (type == ContentType.MODPACKS) {
                EclipseBottomButton(
                    label = stringResource(R.string.content_import_modpack),
                    onClick = {},
                    enabled = false,
                    supporting = stringResource(R.string.content_import_modpack_note),
                )
            }
        }
    }

    if (filtersOpen) {
        ContentFilterDialog(
            gameVersion = gameVersion,
            loader = loader,
            source = source,
            curseForgeEnabled = state.curseForgeKey.isNotBlank(),
            autoDependencies = state.autoInstallDependencies,
            versionChoices = versionChoices,
            onApply = { newVersion, newLoader, newSource, autoDependencies ->
                gameVersion = newVersion
                loader = newLoader
                source = newSource
                state.updateAutoInstallDependencies(autoDependencies)
                filtersOpen = false
            },
            onDismiss = { filtersOpen = false },
        )
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
    query: String,
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
    val filtered = remember(files, query) {
        if (query.isBlank()) {
            files
        } else {
            files.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }
    }

    if (filtered.isEmpty()) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            EclipseEmptyState(
                title = if (files.isEmpty()) {
                    stringResource(R.string.content_empty_title)
                } else {
                    stringResource(R.string.content_no_matches_title)
                },
                body = when {
                    files.isNotEmpty() -> stringResource(R.string.content_no_matches_body)
                    type == ContentType.WORLDS -> stringResource(R.string.content_worlds_empty_body)
                    else -> stringResource(R.string.content_empty_body, stringResource(type.labelRes))
                },
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.absolutePath }) { file ->
                EclipseContentCard(
                    icon = if (file.isDirectory) {
                        EclipsePixelIconKind.FOLDER
                    } else {
                        typeIcon(type)
                    },
                    title = file.name,
                    body = if (file.isDirectory) {
                        stringResource(R.string.content_card_folder, stringResource(type.labelRes))
                    } else {
                        stringResource(
                            R.string.content_card_file,
                            stringResource(type.labelRes),
                            FileOps.formatSize(context, file),
                        )
                    },
                    badge = if (file.isDirectory) {
                        stringResource(R.string.files_folder)
                    } else {
                        null
                    },
                    onClick = { onInfo(file) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.DiscoverContent(
    type: ContentType,
    gameVersion: String?,
    loader: LoaderKind?,
    source: ContentSource,
    curseForgeEnabled: Boolean,
) {
    val versionLabel = gameVersion ?: stringResource(R.string.content_any_version)
    val loaderLabel = loader?.displayName() ?: stringResource(R.string.content_any_loader)
    val sourceLabel = if (source == ContentSource.CURSEFORGE) {
        stringResource(R.string.content_curseforge)
    } else {
        stringResource(R.string.content_modrinth)
    }

    Text(
        text = stringResource(
            R.string.content_active_filters,
            versionLabel,
            loaderLabel,
            sourceLabel,
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = EclipsePalette.Muted,
    )

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

    Box(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
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
    }
}

@Composable
private fun ContentFilterDialog(
    gameVersion: String?,
    loader: LoaderKind?,
    source: ContentSource,
    curseForgeEnabled: Boolean,
    autoDependencies: Boolean,
    versionChoices: List<String?>,
    onApply: (String?, LoaderKind?, ContentSource, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var draftVersion by remember { mutableStateOf(gameVersion) }
    var draftLoader by remember { mutableStateOf(loader) }
    var draftSource by remember { mutableStateOf(source) }
    var draftAutoDependencies by remember { mutableStateOf(autoDependencies) }
    var versionMenu by remember { mutableStateOf(false) }
    var loaderMenu by remember { mutableStateOf(false) }
    var sourceMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.content_filters)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Box {
                    EclipseSelectRow(
                        label = stringResource(R.string.content_filter_version),
                        value = draftVersion ?: stringResource(R.string.content_any_version),
                        onClick = { versionMenu = true },
                    )
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
                                    draftVersion = choice
                                    versionMenu = false
                                },
                            )
                        }
                    }
                }
                EclipseDivider()

                Box {
                    EclipseSelectRow(
                        label = stringResource(R.string.content_filter_loader),
                        value = draftLoader?.displayName()
                            ?: stringResource(R.string.content_any_loader),
                        onClick = { loaderMenu = true },
                    )
                    DropdownMenu(
                        expanded = loaderMenu,
                        onDismissRequest = { loaderMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.content_any_loader)) },
                            onClick = {
                                draftLoader = null
                                loaderMenu = false
                            },
                        )
                        LoaderKind.entries.forEach { choice ->
                            DropdownMenuItem(
                                text = { Text(choice.displayName()) },
                                onClick = {
                                    draftLoader = choice
                                    loaderMenu = false
                                },
                            )
                        }
                    }
                }
                EclipseDivider()

                Box {
                    EclipseSelectRow(
                        label = stringResource(R.string.content_filter_source),
                        value = if (draftSource == ContentSource.CURSEFORGE) {
                            stringResource(R.string.content_curseforge)
                        } else {
                            stringResource(R.string.content_modrinth)
                        },
                        onClick = { sourceMenu = true },
                    )
                    DropdownMenu(
                        expanded = sourceMenu,
                        onDismissRequest = { sourceMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.content_modrinth)) },
                            onClick = {
                                draftSource = ContentSource.MODRINTH
                                sourceMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (curseForgeEnabled) {
                                        stringResource(R.string.content_curseforge)
                                    } else {
                                        "${stringResource(R.string.content_curseforge)} · " +
                                            stringResource(R.string.content_coming_soon)
                                    },
                                )
                            },
                            enabled = curseForgeEnabled,
                            onClick = {
                                draftSource = ContentSource.CURSEFORGE
                                sourceMenu = false
                            },
                        )
                    }
                }
                EclipseDivider()

                EclipseSwitchRow(
                    title = stringResource(R.string.content_auto_deps),
                    summary = stringResource(R.string.content_auto_deps_summary),
                    checked = draftAutoDependencies,
                    onCheckedChange = { draftAutoDependencies = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(draftVersion, draftLoader, draftSource, draftAutoDependencies)
                },
            ) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private fun typeIcon(type: ContentType): EclipsePixelIconKind = when (type) {
    ContentType.WORLDS -> EclipsePixelIconKind.WORLD
    else -> EclipsePixelIconKind.CONTENT
}
