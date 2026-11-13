/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * MANAGE CONTENT: unified screen for both installed content and browsing.
 * Segmented control for Installed | Browse. Sticky content-type bar with
 * icons + badges. Single provider selector. Auto-load on browse load.
 * Content icons loaded via Coil with caching.
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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

/** Content types for installed/browse tabs. Icons only in the sticky bar; text labels
 *  appear as small badges under each icon when selected. */
private enum class ContentType(
    val labelRes: Int,
    val glyph: String
) {
    MODS(R.string.content_type_mods, "●"),
    RESOURCE_PACKS(R.string.content_type_resource_packs, "■"),
    DATA_PACKS(R.string.content_type_data_packs, "▲"),
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

/** Custom segmented control: two buttons (Installed | Browse) with a selected indicator.
 *  Supports tap and horizontal drag to switch views seamlessly. */
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
                .weight(1f, fill = false)
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
                .weight(1f, fill = false)
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

/** Floating page navigation control: sticks to right side below search field. */
@Composable
private fun PageNavigationControl(
    currentPage: Int,
    totalPages: Int,
    collapsed: Boolean,
    onCollapseToggle: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(if (collapsed) 36.dp else 150.dp)
            .height(40.dp)
            .background(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            )
            .clickable(onClick = onCollapseToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (collapsed) {
            Text(
                text = "▲",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(
                    onClick = onPreviousPage,
                    enabled = currentPage > 1,
                ) {
                    Text(
                        text = "‹",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            if (currentPage > 1) 1f else 0.3f,
                        ),
                    )
                }
                Text(
                    text = "$currentPage / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onNextPage,
                    enabled = currentPage < totalPages,
                ) {
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            if (currentPage < totalPages) 1f else 0.3f,
                        ),
                    )
                }
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
        if (url.isNotBlank()) {
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

/** Installed content row with update, toggle, delete actions. */
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
                    Text(text = "⟳", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(text = "🗑", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
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
                fallbackGlyph = "●",
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
                Text(text = "⬇", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatDownloads(downloads: Long): String {
    return when {
        downloads >= 1_000_000 -> "${(downloads / 1_000_000.0).toInt()}M"
        downloads >= 1_000 -> "${(downloads / 1_000.0).toInt()}K"
        else -> downloads.toString()
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
    var pageNavCollapsed by remember { mutableStateOf(false) }

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
                when (val result = client.searchMods(effectiveQuery, projectType = browseContentType.modrinthProjectType)) {
                    is MetaResult.Success -> {
                        browseResults = result.value
                        browseLoading = false
                    }
                    is MetaResult.Error -> {
                        browseError = result.message.ifBlank { marketErrorStr }
                        browseLoading = false
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(manageContentTitle) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text(text = "◀", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            actions = {
                IconButton(onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) }) {
                    Text(text = "📦", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            TextButton(onClick = { optionsCollapsed = !optionsCollapsed }) {
                Text(
                    text = if (optionsCollapsed) "▼" else "▲",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

        // Search field + floating page navigation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = browseQuery,
                    onValueChange = { browseQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.manage_content_search_hint)) },
                    singleLine = true,
                    leadingIcon = { Text(text = "🔍", style = MaterialTheme.typography.bodyMedium) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    scope.launch {
                        browseLoading = true
                        browseError = null
                        when (val result = client.searchMods(browseQuery, projectType = browseContentType.modrinthProjectType)) {
                            is MetaResult.Success -> {
                                browseResults = result.value
                                browseLoading = false
                            }
                            is MetaResult.Error -> {
                                browseError = result.message.ifBlank { marketErrorStr }
                                browseLoading = false
                            }
                        }
                    }
                }) {
                    Text(text = "🔍", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }

            // Floating page navigation control
            PageNavigationControl(
                currentPage = browsePage,
                totalPages = browseTotalPages,
                collapsed = pageNavCollapsed,
                onCollapseToggle = { pageNavCollapsed = !pageNavCollapsed },
                onPreviousPage = {
                    if (browsePage > 1) browsePage = browsePage - 1
                },
                onNextPage = {
                    if (browsePage < browseTotalPages) browsePage = browsePage + 1
                },
            )
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
