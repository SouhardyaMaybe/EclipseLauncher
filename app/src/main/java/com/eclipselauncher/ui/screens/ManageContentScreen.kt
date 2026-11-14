/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * MANAGE CONTENT: unified screen for both installed content and browsing.
 * Segmented control for Installed | Browse. Sticky content-type bar with
 * glyph chips + count badges. Content icons loaded via Coil with caching.
 */
package com.eclipselauncher.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.eclipselauncher.R
import com.eclipselauncher.data.MetaResult
import com.eclipselauncher.data.ModrinthClient
import com.eclipselauncher.data.ModrinthHit

/** Content types for installed/browse tabs. Glyph shown in the sticky bar chip. */
private enum class ContentType(
    val labelRes: Int,
    val glyph: String
) {
    MODS(R.string.content_type_mods, "◆"),
    RESOURCE_PACKS(R.string.content_type_resource_packs, "◆"),
    DATA_PACKS(R.string.content_type_data_packs, "◆"),
    SHADERS(R.string.content_type_shaders, "◆"),
    WORLDS(R.string.content_type_worlds, "◆"),
}

/** Helper for ContentType project type mapping. */
private val ContentType.modrinthProjectType: String
    get() = when (this) {
        ContentType.MODS -> "mod"
        ContentType.RESOURCE_PACKS -> "resourcepack"
        ContentType.DATA_PACKS -> "datapack"
        ContentType.SHADERS -> "shader"
        ContentType.WORLDS -> "world"
    }

/** Provider for browsing. */
private enum class Provider(val labelRes: Int) {
    MODRINTH(R.string.provider_modrinth),
    CURSEFORGE(R.string.provider_curseforge),
}

/** Represents installed content item. */
private data class InstalledContent(
    val id: String,
    val name: String,
    val version: String,
    val type: ContentType,
    val iconUrl: String?,
    val enabled: Boolean = true,
)

/** Custom segmented control: Installed | Browse with tap + drag gesture. */
@Composable
private fun ContentSegmentedControl(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val labels = listOf(
        stringResource(R.string.manage_content_installed),
        stringResource(R.string.manage_content_browse),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            ),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = interactionSource,
                    onClick = { onTabSelected(0) },
                )
                .background(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedIndex == 0)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        Color.Transparent,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = labels[0],
                style = MaterialTheme.typography.labelLarge,
                color = if (selectedIndex == 0)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = interactionSource,
                    onClick = { onTabSelected(1) },
                )
                .background(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedIndex == 1)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        Color.Transparent,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = labels[1],
                style = MaterialTheme.typography.labelLarge,
                color = if (selectedIndex == 1)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Content type chip: glyph only with small count badge. */
@Composable
private fun ContentTypeChip(
    type: ContentType,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    val badgeColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(48.dp)
            .background(shape = RoundedCornerShape(10.dp), color = containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = type.glyph,
                style = MaterialTheme.typography.bodyMedium,
                color = badgeColor,
            )
            if (count > 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = badgeColor,
                )
            }
        }
    }
}

/** Loads and caches a content icon via Coil. Falls back to a text glyph. */
@Composable
private fun ContentIcon(url: String?, fallbackGlyph: String, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size.value * 0.25f))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null && url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Text(
                text = fallbackGlyph,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

/** Installed content row: icon card, name + "version · type", refresh ⟳, toggle, 🗑 delete. */
@Composable
private fun InstalledContentRow(
    content: InstalledContent,
    onUpdate: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Content icon via Coil
            ContentIcon(
                url = content.iconUrl,
                fallbackGlyph = content.type.glyph,
                size = 48.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Content info
            Column(modifier = Modifier.weight(1f)) {
                Text(text = content.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "v${content.version} · ${stringResource(content.type.labelRes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Actions: Update/Degrade, Toggle, Delete
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onUpdate) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.manage_content_update), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
                Switch(
                    checked = content.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.secondary,
                        checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/** Browse content row with Coil-loaded icon. */
@Composable
private fun BrowseContentRow(
    hit: ModrinthHit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mod icon via Coil
            ContentIcon(
                url = hit.icon_url,
                fallbackGlyph = "◆",
                size = 48.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = hit.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = hit.author + " · " + formatDownloads(hit.downloads), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                if (hit.description.isNotBlank()) {
                    Text(text = hit.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { /* open version dropdown */ }) {
                Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.manage_content_install), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** Collapse/Expand toggle button for options panel. */
@Composable
private fun ToggleButton(
    collapsed: Boolean,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageContentScreen(
    client: ModrinthClient,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mainTab by remember { mutableStateOf(0) } // 0 = Installed, 1 = Browse
    var installedContentType by remember { mutableStateOf(ContentType.MODS) }
    var browseContentType by remember { mutableStateOf(ContentType.MODS) }
    var browseProvider by remember { mutableStateOf(Provider.MODRINTH) }
    var browseQuery by remember { mutableStateOf("") }
    var browseResults by remember { mutableStateOf<List<ModrinthHit>>(emptyList()) }
    var browseLoading by remember { mutableStateOf(false) }
    var browseError by remember { mutableStateOf<String?>(null) }
    var browsePage by remember { mutableStateOf(1) }
    var browseTotalPages by remember { mutableStateOf(1) }

    var optionsCollapsed by remember { mutableStateOf(false) }

    // Mock installed content
    val installedContent = remember {
        listOf(
            InstalledContent("1", "Sodium", "0.5.8", ContentType.MODS, null, true),
            InstalledContent("2", "Iris Shaders", "1.7.4", ContentType.MODS, null, true),
            InstalledContent("3", "Fabric API", "0.96.0", ContentType.MODS, null, true),
            InstalledContent("4", "Vanilla Resource Pack", "1.21", ContentType.RESOURCE_PACKS, null, true),
            InstalledContent("5", "BSL Shaders", "8.2.01", ContentType.SHADERS, null, true),
            InstalledContent("6", "My World", "1.0", ContentType.WORLDS, null, true),
        )
    }

    // Pre-load strings for use in non-composable callbacks
    val manageContentTitle = stringResource(R.string.manage_content_title)
    val manageContentImport = stringResource(R.string.manage_content_import)
    val manageContentImportStarted = stringResource(R.string.manage_content_import_started)
    val providerComingSoonStr = stringResource(R.string.provider_coming_soon)
    val marketErrorStr = stringResource(R.string.market_error)

    // Import .zip launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(context, manageContentImportStarted, Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-load browse content on initial load and when content type changes
    LaunchedEffect(browseContentType, browseProvider, browsePage, browseQuery) {
        if (mainTab == 1) {
            scope.launch {
                browseLoading = true
                browseError = null
                val effectiveQuery = if (browseQuery.isNotBlank()) browseQuery else ""
                val offset = (browsePage - 1) * 25
                val result = client.searchMods(
                    query = effectiveQuery,
                    limit = 25,
                    offset = offset,
                    projectType = browseContentType.modrinthProjectType,
                )
                if (result.totalPages > 0 && result.hits.isNotEmpty()) {
                    browseResults = result.hits
                    browseTotalPages = result.totalPages
                    browsePage = result.pageNumber
                    browseLoading = false
                } else {
                    browseError = "Failed to load results"
                    browseLoading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(manageContentTitle) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            },
            actions = {
                IconButton(onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = stringResource(R.string.manage_content_import), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        // Segmented control: Installed | Browse
        ContentSegmentedControl(
            selectedIndex = mainTab,
            onTabSelected = { mainTab = it },
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))

        // Sticky content type bar (glyphs + badges)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val activeContentType = if (mainTab == 0) installedContentType else browseContentType
            ContentType.values().forEach { type ->
                val count = if (mainTab == 0) {
                    installedContent.count { it.type == type }
                } else {
                    0
                }
                ContentTypeChip(
                    type = type,
                    selected = activeContentType == type,
                    count = count,
                    onClick = {
                        if (mainTab == 0) installedContentType = type
                        else browseContentType = type
                    },
                )
            }
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))

        // Collapse/Expand button for options
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            ToggleButton(
                collapsed = optionsCollapsed,
                onToggle = { optionsCollapsed = !optionsCollapsed },
            )
        }

        // Options section (collapsible): provider selector
        AnimatedVisibility(
            visible = !optionsCollapsed,
            enter = expandIn(tween(150)) + fadeIn(tween(150)),
            exit = shrinkOut(tween(150)) + fadeOut(tween(150)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Provider.values().forEach { p ->
                    val isEnabled = p != Provider.CURSEFORGE
                    FilterChip(
                        selected = browseProvider == p,
                        onClick = {
                            if (p == Provider.CURSEFORGE) {
                                Toast.makeText(context, providerComingSoonStr, Toast.LENGTH_SHORT).show()
                            } else {
                                browseProvider = p
                            }
                        },
                        label = { Text(text = stringResource(p.labelRes), style = MaterialTheme.typography.labelLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = isEnabled, selected = browseProvider == p),
                    )
                }
            }
        }

        // Search field
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = browseQuery,
                onValueChange = { browseQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.manage_content_search_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.cd_search), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp)) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = {
                scope.launch {
                    browseLoading = true
                    browseError = null
                    val offset = (browsePage - 1) * 25
                    val result = client.searchMods(
                        query = browseQuery,
                        limit = 25,
                        offset = offset,
                        projectType = browseContentType.modrinthProjectType,
                    )
                    if (result.totalPages > 0 && result.hits.isNotEmpty()) {
                        browseResults = result.hits
                        browseTotalPages = result.totalPages
                        browsePage = result.pageNumber
                        browseLoading = false
                    } else {
                        browseError = "Failed to load results"
                        browseLoading = false
                    }
                }
            }) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.action_search), tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
            }
        }

        // Floating pagination control (only in Browse tab)
        AnimatedVisibility(
            visible = mainTab == 1 && browseTotalPages > 1,
        ) {
            var pageNavCollapsed by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .align(Alignment.Horizontal.End)
                    .padding(end = 16.dp, top = 8.dp),
            ) {
                if (pageNavCollapsed) {
                    IconButton(onClick = { pageNavCollapsed = false }) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = stringResource(R.string.cd_expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp).padding(12.dp),
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Previous page
                        IconButton(
                            onClick = {
                                if (browsePage > 1) {
                                    browsePage--
                                }
                            },
                            enabled = browsePage > 1,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowLeft,
                                contentDescription = stringResource(R.string.cd_nav_previous),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (browsePage > 1) 1f else 0.3f),
                                modifier = Modifier.size(24.dp).padding(12.dp),
                            )
                        }
                        // Page indicator
                        Text(
                            text = "$browsePage / $browseTotalPages",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // Next page
                        IconButton(
                            onClick = {
                                if (browsePage < browseTotalPages) {
                                    browsePage++
                                }
                            },
                            enabled = browsePage < browseTotalPages,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowRight,
                                contentDescription = stringResource(R.string.cd_nav_next),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (browsePage < browseTotalPages) 1f else 0.3f),
                                modifier = Modifier.size(24.dp).padding(12.dp),
                            )
                        }
                        // Collapse/Expand toggle
                        IconButton(onClick = { pageNavCollapsed = true }) {
                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                contentDescription = stringResource(R.string.cd_collapse),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp).padding(12.dp),
                            )
                        }
                    }
                }
            }
        }

        // Content list
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (mainTab) {
                0 -> {
                    val filteredContent = installedContent.filter { it.type == installedContentType }
                    when {
                        filteredContent.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.manage_content_empty),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredContent, key = { it.id }) { content ->
                                InstalledContentRow(
                                    content = content,
                                    onUpdate = { /* open version dialog */ },
                                    onToggle = { /* toggle enabled */ },
                                    onDelete = { /* delete with confirmation */ },
                                )
                            }
                        }
                    }
                }
                1 -> {
                    when {
                        browseLoading && browseResults.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary) }

                        browseError != null -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { Text(text = browseError ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }

                        browseResults.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { Text(text = stringResource(R.string.manage_content_empty), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }

                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(browseResults, key = { it.project_id }) { hit ->
                                BrowseContentRow(hit = hit, onClick = { /* open mod detail */ })
                            }
                        }
                    }
                }
            }
        }
    }
}
