/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.core.model.InstanceSummary
import dev.shadow.eclipselauncher.core.model.LoaderKind
import dev.shadow.eclipselauncher.data.ContentType
import dev.shadow.eclipselauncher.data.FileOps
import dev.shadow.eclipselauncher.data.LauncherState
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoTheme
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import dev.shadow.eclipselauncher.ui.components.ConfirmDialog
import dev.shadow.eclipselauncher.ui.components.FileInfoDialog
import dev.shadow.eclipselauncher.ui.components.MojoBackRow
import dev.shadow.eclipselauncher.ui.components.MojoCard
import dev.shadow.eclipselauncher.ui.components.MojoDialog
import dev.shadow.eclipselauncher.ui.components.MojoDivider
import dev.shadow.eclipselauncher.ui.components.MojoEmptyState
import dev.shadow.eclipselauncher.ui.components.MojoMineButton
import dev.shadow.eclipselauncher.ui.components.MojoPreferenceSwitch
import dev.shadow.eclipselauncher.ui.components.MojoSearchRow
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

    MojoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background),
        ) {
            MojoBackRow(
                label = stringResource(R.string.action_back),
                onClick = onBack,
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                ContentTab(
                    text = stringResource(R.string.content_installed),
                    selected = installed,
                    onClick = { installed = true },
                    modifier = Modifier.weight(1f),
                )
                ContentTab(
                    text = stringResource(R.string.content_discover),
                    selected = !installed,
                    onClick = { installed = false },
                    modifier = Modifier.weight(1f),
                )
            }
            MojoDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ContentType.entries.forEach { item ->
                    TypeChip(
                        text = stringResource(item.labelRes),
                        selected = type == item,
                        onClick = { type = item },
                    )
                }
            }
            MojoDivider()

            if (installed) {
                if (instance != null) {
                    MojoSearchRow(
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
                MojoSearchRow(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.content_search_projects),
                    onFilter = { filtersOpen = true },
                )
                DiscoverContent(
                    type = type,
                    gameVersion = gameVersion,
                    loader = loader,
                    source = source,
                    curseForgeEnabled = state.curseForgeKey.isNotBlank(),
                )
                if (type == ContentType.MODPACKS) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        MojoMineButton(
                            text = stringResource(R.string.content_import_modpack),
                            onClick = {},
                            enabled = false,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.content_import_modpack_note),
                            modifier = Modifier.fillMaxWidth(),
                            style = MojoType.secondary,
                            color = MojoPalette.SecondaryText,
                        )
                    }
                }
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
private fun ContentTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MojoPalette.StatusBar)
            .clickable(role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 13.dp),
            style = MojoType.preferenceTitle,
            color = if (selected) MojoPalette.PrimaryText else MojoPalette.SecondaryText,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) MojoPalette.Accent else MojoPalette.StatusBar),
        )
    }
}

@Composable
private fun TypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(if (selected) MojoPalette.Overlay else MojoPalette.SecondaryButton)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text = text,
            style = MojoType.body,
            color = if (selected) MojoPalette.PrimaryText else MojoPalette.SecondaryText,
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
            MojoEmptyState(
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
            MojoEmptyState(
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
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.absolutePath }) { file ->
                MojoCard(
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
                    trailing = if (file.isDirectory) {
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        style = MojoType.secondary,
        color = MojoPalette.SecondaryText,
    )

    if (type == ContentType.WORLDS) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            MojoEmptyState(
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
                MojoEmptyState(
                    title = stringResource(R.string.content_curseforge_empty_title),
                    body = stringResource(R.string.content_curseforge_empty_body),
                )
            }

            source == ContentSource.CURSEFORGE -> {
                MojoEmptyState(
                    title = stringResource(R.string.content_curseforge_ready_title),
                    body = stringResource(R.string.content_curseforge_ready_body),
                )
            }

            else -> {
                MojoEmptyState(
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

    MojoDialog(
        onDismiss = onDismiss,
        dismissLabel = stringResource(R.string.action_cancel),
        onDismissButton = onDismiss,
        confirmLabel = stringResource(R.string.action_apply),
        onConfirm = {
            onApply(draftVersion, draftLoader, draftSource, draftAutoDependencies)
        },
        title = stringResource(R.string.content_filters),
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FilterSelectRow(
                    label = stringResource(R.string.content_filter_version),
                    value = draftVersion ?: stringResource(R.string.content_any_version),
                    onOpen = { versionMenu = true },
                    open = versionMenu,
                    onDismissMenu = { versionMenu = false },
                ) {
                    versionChoices.forEach { choice ->
                        MojoMenuItem(
                            text = choice ?: stringResource(R.string.content_any_version),
                            onClick = {
                                draftVersion = choice
                                versionMenu = false
                            },
                        )
                    }
                }
                MojoDivider()

                FilterSelectRow(
                    label = stringResource(R.string.content_filter_loader),
                    value = draftLoader?.displayName()
                        ?: stringResource(R.string.content_any_loader),
                    onOpen = { loaderMenu = true },
                    open = loaderMenu,
                    onDismissMenu = { loaderMenu = false },
                ) {
                    MojoMenuItem(
                        text = stringResource(R.string.content_any_loader),
                        onClick = {
                            draftLoader = null
                            loaderMenu = false
                        },
                    )
                    LoaderKind.entries.forEach { choice ->
                        MojoMenuItem(
                            text = choice.displayName(),
                            onClick = {
                                draftLoader = choice
                                loaderMenu = false
                            },
                        )
                    }
                }
                MojoDivider()

                FilterSelectRow(
                    label = stringResource(R.string.content_filter_source),
                    value = if (draftSource == ContentSource.CURSEFORGE) {
                        stringResource(R.string.content_curseforge)
                    } else {
                        stringResource(R.string.content_modrinth)
                    },
                    onOpen = { sourceMenu = true },
                    open = sourceMenu,
                    onDismissMenu = { sourceMenu = false },
                ) {
                    MojoMenuItem(
                        text = stringResource(R.string.content_modrinth),
                        onClick = {
                            draftSource = ContentSource.MODRINTH
                            sourceMenu = false
                        },
                    )
                    MojoMenuItem(
                        text = if (curseForgeEnabled) {
                            stringResource(R.string.content_curseforge)
                        } else {
                            "${stringResource(R.string.content_curseforge)} · " +
                                stringResource(R.string.content_coming_soon)
                        },
                        enabled = curseForgeEnabled,
                        onClick = {
                            draftSource = ContentSource.CURSEFORGE
                            sourceMenu = false
                        },
                    )
                }
                MojoDivider()

                MojoPreferenceSwitch(
                    title = stringResource(R.string.content_auto_deps),
                    summary = stringResource(R.string.content_auto_deps_summary),
                    checked = draftAutoDependencies,
                    onCheckedChange = { draftAutoDependencies = it },
                )
            }
        },
    )
}

@Composable
private fun FilterSelectRow(
    label: String,
    value: String,
    onOpen: () -> Unit,
    open: Boolean,
    onDismissMenu: () -> Unit,
    menu: @Composable ColumnScope.() -> Unit,
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MojoType.preferenceTitle,
                    color = MojoPalette.PrimaryText,
                )
                Text(
                    text = value,
                    style = MojoType.preferenceSummary,
                    color = MojoPalette.SecondaryText,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Select",
                style = MojoType.body,
                color = MojoPalette.Accent,
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = onDismissMenu,
            shape = RectangleShape,
            containerColor = MojoPalette.StatusBar,
            content = menu,
        )
    }
}

@Composable
private fun MojoMenuItem(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MojoType.body,
                color = if (enabled) MojoPalette.PrimaryText else MojoPalette.SecondaryText,
            )
        },
        enabled = enabled,
        onClick = onClick,
    )
}

private fun typeIcon(type: ContentType): EclipsePixelIconKind = when (type) {
    ContentType.WORLDS -> EclipsePixelIconKind.WORLD
    else -> EclipsePixelIconKind.CONTENT
}
