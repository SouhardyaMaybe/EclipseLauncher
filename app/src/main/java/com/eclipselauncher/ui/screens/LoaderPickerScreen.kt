/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * LOADER PICKER — step 2 of instance creation, for the version chosen on the
 * previous screen. Six loader buttons, one per loader: Vanilla, OptiFine,
 * Fabric, Forge, NeoForge and Quilt. A loader with no build for this
 * Minecraft version reads "Unavailable" and cannot be tapped; the rest read
 * "Available" and open their own dropdown — a fixed-height, scrollable radio
 * list of that loader's versions that snaps to whatever is selected. Vanilla
 * and OptiFine have no version list of their own (the Minecraft version
 * decides), so they are buttons without a dropdown. The top bar carries the
 * Install action, which records the instance and hands back to HOME.
 *
 * Layout rule: ONE LazyColumn. Each open dropdown is an inner list bounded
 * by heightIn first — that bound is what keeps the infinite-height crash
 * from ever coming back.
 */
package com.eclipselauncher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eclipselauncher.R
import com.eclipselauncher.data.LoaderKind
import com.eclipselauncher.data.LoaderPickerData
import com.eclipselauncher.data.MetaResult
import com.eclipselauncher.data.VersionOption
import com.eclipselauncher.data.VersionRepository
import com.eclipselauncher.ui.components.LoaderIcon
import com.eclipselauncher.ui.components.loaderName

/** The six loaders, in the order the picker shows them. */
private val PickerLoaders = listOf(
    LoaderKind.VANILLA,
    LoaderKind.OPTIFINE,
    LoaderKind.FABRIC,
    LoaderKind.FORGE,
    LoaderKind.NEOFORGE,
    LoaderKind.QUILT,
)

/** Loaders that ship a version list of their own behind the dropdown. */
private fun LoaderKind.hasVersionList(): Boolean = when (this) {
    LoaderKind.FABRIC, LoaderKind.QUILT, LoaderKind.FORGE, LoaderKind.NEOFORGE, LoaderKind.OPTIFINE -> true
    else -> false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoaderPickerScreen(
    mcVersion: String,
    versions: VersionRepository,
    onBack: () -> Unit,
    onInstall: (LoaderKind, String) -> Unit,
) {
    var data by remember { mutableStateOf<LoaderPickerData?>(null) }
    var selected by remember { mutableStateOf(LoaderKind.VANILLA) }
    // Each loader maintains its own selected version (blank = latest)
    var loaderVersions by remember { mutableStateOf<Map<LoaderKind, String>>(PickerLoaders.associateWith { "" }) }
    var expanded by remember { mutableStateOf<LoaderKind?>(null) }

    LaunchedEffect(mcVersion) {
        data = versions.loaderPickerData(mcVersion)
    }

    // Install records what has actually been chosen: the Minecraft version
    // from the previous screen, this loader, and its version (blank = latest).
    val canInstall = data != null

    // Get the version for the currently selected loader
    val selectedLoaderVersion = loaderVersions[selected] ?: ""

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = mcVersion,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
            },
            actions = {
                Button(
                    onClick = { onInstall(selected, selectedLoaderVersion) },
                    enabled = canInstall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canInstall) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        },
                        contentColor = if (canInstall) {
                            MaterialTheme.colorScheme.onSecondary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    ),
                ) {
                    Text(stringResource(R.string.action_install))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val pickerData = data) {
                null -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = stringResource(R.string.loader_checking),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    item(key = "heading") {
                        Text(
                            text = stringResource(R.string.field_loader),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }

                    items(PickerLoaders, key = { it.name }) { loader ->
                        LoaderPickerRow(
                            loader = loader,
                            available = pickerData.available[loader] != false,
                            versionList = pickerData.versions[loader]
                                ?: MetaResult.Success(emptyList()),
                            selected = selected == loader,
                            expanded = expanded == loader,
                            loaderVersion = loaderVersions[loader] ?: "",
                            onToggle = {
                                // One dropdown at a time: tapping a loader
                                // selects it and opens it, tapping the open
                                // one again just closes it. Loaders without a
                                // version list (Vanilla) select
                                // without ever holding an open dropdown.
                                val opens = loader.hasVersionList()
                                when {
                                    expanded == loader -> expanded = null
                                    selected == loader -> expanded = if (opens) loader else null
                                    else -> {
                                        selected = loader
                                        // Keep the previously selected version for this loader
                                        expanded = if (opens) loader else null
                                    }
                                }
                            },
                            onPickVersion = { version ->
                                loaderVersions = loaderVersions + (loader to version)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * One loader button. The header carries the glyph, the name, the summary
 * line (Unavailable / Available / the chosen loader version) and, when the
 * loader both qualifies and is available, a chevron that half-turns as the
 * dropdown opens. Under it, an inline-expanding radio list bounded to a
 * fixed height that scrolls and snaps to the selection.
 */
@Composable
private fun LoaderPickerRow(
    loader: LoaderKind,
    available: Boolean,
    versionList: MetaResult<List<VersionOption>>,
    selected: Boolean,
    expanded: Boolean,
    loaderVersion: String,
    onToggle: () -> Unit,
    onPickVersion: (String) -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) -180f else 0f,
        animationSpec = tween(350),
        label = "chevron",
    )

    val name = loaderName(loader)
    val unavailableLabel = stringResource(R.string.loader_unavailable)
    val availableLabel = stringResource(R.string.loader_available)
    val summary = when {
        !available -> unavailableLabel
        selected && loaderVersion.isNotBlank() -> loaderVersion
        selected -> "" // Selected but no version picked yet
        else -> availableLabel // Not selected but available
    }

    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content,
        ),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (available) 1f else 0.45f)
                    .clickable(enabled = available, onClick = onToggle)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                LoaderIcon(loader = loader, size = 34.dp)
                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (selected && loaderVersion.isNotBlank()) {
                    IconButton(
                        onClick = { onPickVersion("") },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.cd_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                if (available && loader.hasVersionList()) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(
                            if (expanded) R.string.cd_collapse else R.string.cd_expand,
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(34.dp)
                            .graphicsLayer { rotationZ = rotation },
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            if (available && loader.hasVersionList()) {
                LoaderVersionDropdown(
                    expanded = expanded,
                    versionList = versionList,
                    selectedId = loaderVersion,
                    onPickVersion = onPickVersion,
                )
            }
        }
    }
}

/**
 * The dropdown body: a radio list of this loader's versions, bounded to a
 * fixed height so it scrolls instead of growing, and snapped to the selected
 * row the moment it opens.
 */
@Composable
private fun LoaderVersionDropdown(
    expanded: Boolean,
    versionList: MetaResult<List<VersionOption>>,
    selectedId: String,
    onPickVersion: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(tween(350)) + fadeIn(tween(350)),
        exit = shrinkVertically(tween(350)) + fadeOut(tween(350)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        when (versionList) {
            is MetaResult.Error -> Text(
                text = versionList.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            is MetaResult.Success<*> -> {
                val options = versionList.value as @Suppress("UNCHECKED_CAST") List<VersionOption>
                val listState = rememberLazyListState()

                // Snap the open list onto the selected row. The body starts
                // at zero height and grows over the animation, so wait for a
                // real viewport before asking the list to position itself.
                LaunchedEffect(expanded, selectedId, options) {
                    if (!expanded) return@LaunchedEffect
                    val selectedIndex = options
                        .indexOfFirst { it.id == selectedId }
                        .let { if (it < 0) 0 else it }
                    repeat(30) {
                        withFrameNanos { }
                        if (listState.layoutInfo.viewportSize.height > 0) {
                            listState.scrollToItem(selectedIndex)
                            return@LaunchedEffect
                        }
                    }
                    listState.scrollToItem(selectedIndex)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        // heightIn BEFORE the scroll bounds the list even
                        // when its item arrives with infinite height — the
                        // whole trick behind the no-nested-scroll crash.
                        .heightIn(max = 200.dp)
                        .padding(vertical = 4.dp),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    items(options, key = { "loader/${it.id}" }) { option ->
                        LoaderRadioRow(
                            label = option.label,
                            selected = option.id == selectedId,
                            onClick = { onPickVersion(option.id) },
                        )
                    }
                }
            }
        }
    }
}

/** ZL2's addon row: clipped 12dp-radius target, radio, labelMedium text. */
@Composable
private fun LoaderRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(all = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.secondary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
