/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * CREATE INSTANCE — step 1: pick the Minecraft version.
 * One date-ordered list of every published version, newest at the top and
 * oldest at the bottom, with a row of filter chips above it (All / Releases /
 * Snapshots / Old Betas / Old Alphas). Tapping a version moves on to the
 * loader picker for exactly that version; the Modpack entry in the top bar
 * leaves for the pack browser instead.
 *
 * The row language follows ZalithLauncher 2's version items, reimplemented
 * with Eclipse's colors: a flat large-radius card that settles in from 95%
 * scale, the version id over its release date, and a bright type tag while
 * the unfiltered "All" view is up.
 *
 * Layout rule: ONE LazyColumn. The chips are a pinned header outside it, so
 * they never scroll away and never nest a second scrolling list.
 */
package com.eclipselauncher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.eclipselauncher.R
import com.eclipselauncher.data.McVersion
import com.eclipselauncher.data.MetaResult
import com.eclipselauncher.data.VersionRepository

/** Filter chips: Releases / Snapshots / Old Betas / Old Alphas. */
private data class VersionTab(
    val titleRes: Int,
    val type: String,
    val badgeColor: androidx.compose.ui.graphics.Color,
)

private val VersionTabs = listOf(
    VersionTab(R.string.version_tab_releases, "release", androidx.compose.ui.graphics.Color(0xFF4CAF50)),      // Green
    VersionTab(R.string.version_tab_snapshots, "snapshot", androidx.compose.ui.graphics.Color(0xFFFFD600)),   // Yellow
    VersionTab(R.string.version_tab_old_betas, "old_beta", androidx.compose.ui.graphics.Color(0xFFFF9800)),  // Orange
    VersionTab(R.string.version_tab_old_alphas, "old_alpha", androidx.compose.ui.graphics.Color(0xFFF44336)), // Red
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInstanceScreen(
    versions: VersionRepository,
    onBack: () -> Unit,
    onPickVersion: (String) -> Unit,
    onModpack: () -> Unit,
) {
    // Show cached versions instantly for zero-lag first paint
    val cachedVersions = remember { versions.getCachedVersions() }
    var allVersions by remember { mutableStateOf(cachedVersions) }
    var loading by remember { mutableStateOf(cachedVersions.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableIntStateOf(0) }

    // Load fresh data in background
    LaunchedEffect(Unit) {
        if (cachedVersions.isEmpty()) loading = true
        launch(Dispatchers.IO) {
            when (val result = versions.minecraftVersions()) {
                is MetaResult.Success<*> -> {
                    allVersions = result.value as List<McVersion>
                    loading = false
                }
                is MetaResult.Error -> {
                    if (allVersions.isEmpty()) error = result.message
                    loading = false
                }
            }
        }
    }

    val activeType = VersionTabs[activeTab].type
    val displayVersions = remember(allVersions, activeType) {
        allVersions.filter { it.type == activeType }
    }

    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.create_title),
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
                IconButton(onClick = onModpack) {
                    Icon(
                        imageVector = Icons.Outlined.FolderZip,
                        contentDescription = stringResource(R.string.modpack_browser_title),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        // Pinned filter row: it labels the list underneath without ever
        // becoming a scrolling item itself.
        VersionFilterRow(
            activeTab = activeTab,
            onTabClick = { activeTab = it },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading && allVersions.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }

                error != null && allVersions.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = requireNotNull(error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                displayVersions.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.version_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    items(displayVersions, key = { it.id }) { version ->
                        VersionRow(
                            version = version,
                            dateFormat = dateFormat,
                            onClick = { onPickVersion(version.id) },
                        )
                    }
                    // Show skeleton placeholders while fresh data loads
                    if (loading) {
                        items(5) { _ ->
                            VersionSkeleton()
                        }
                    }
                }
            }
        }
    }
}

/**
 * ZL2's filter chips: M3 FilterChip with an animated leading check that
 * expands from the start when a tab is picked, spaced 12dp in a horizontal
 * scroll row over a hairline divider.
 */
@Composable
private fun VersionFilterRow(
    activeTab: Int,
    onTabClick: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VersionTabs.forEachIndexed { index, tab ->
                FilterChip(
                    selected = activeTab == index,
                    onClick = { onTabClick(index) },
                    label = {
                        Text(
                            text = stringResource(tab.titleRes),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    leadingIcon = {
                        AnimatedVisibility(
                            visible = activeTab == index,
                            enter = expandIn(tween(150), expandFrom = Alignment.CenterStart) +
                                fadeIn(tween(150)),
                            exit = shrinkOut(tween(150), shrinkTowards = Alignment.CenterStart) +
                                fadeOut(tween(150)),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = activeTab == index,
                    ),
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        )
    }
}

/**
 * One Minecraft version as a ZL2 card: the id over its release date,
 * a type badge colored by version type, and a trailing chevron.
 * Every card settles in from 95% scale.
 */
@Composable
private fun VersionRow(
    version: McVersion,
    dateFormat: DateFormat,
    onClick: () -> Unit,
) {
    val scale = remember { Animatable(0.95f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, tween(350)) }

    val dateLabel = remember(version.releaseTime) {
        if (version.releaseTime > 0L) {
            dateFormat.format(Date(version.releaseTime))
        } else {
            ""
        }
    }

    // Get badge color for this version type
    val badgeColor = VersionTabs.firstOrNull { it.type == version.type }?.badgeColor
        ?: MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = version.id,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dateLabel.isNotEmpty()) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }

            // Type badge with version-specific color
            Surface(
                shape = MaterialTheme.shapes.large,
                color = badgeColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    text = typeLabel(version.type),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Manifest type → the short tag shown on a row. */
@Composable
private fun typeLabel(type: String): String = when (type) {
    "snapshot" -> stringResource(R.string.version_tab_snapshots)
    "old_beta" -> stringResource(R.string.version_tab_old_betas)
    "old_alpha" -> stringResource(R.string.version_tab_old_alphas)
    else -> stringResource(R.string.version_tab_releases)
}

/** Skeleton placeholder for version row while loading. */
@Composable
private fun VersionSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                )
            }
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            )
        }
    }
}
