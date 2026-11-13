/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * MANAGE CONTENT: unified screen for both installed content and browsing.
 * Two top-level tabs: Installed | Browse
 * - Installed: sub-tabs for Mods, Resource Packs, Data Packs, Shaders, Worlds
 * - Browse: same sub-tabs but for browsing/installing from Modrinth/CurseForge
 * Provider selector (Modrinth/CurseForge) in browse tab.
 * Layout rule: ONE LazyColumn per tab, no nested scrolling.
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Texture
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.eclipselauncher.R
import com.eclipselauncher.data.MetaResult
import com.eclipselauncher.data.ModrinthClient
import com.eclipselauncher.data.ModrinthHit

/** Content types for installed/browse tabs. */
private enum class ContentType(
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    MODS(R.string.content_type_mods, Icons.Filled.Layers),
    RESOURCE_PACKS(R.string.content_type_resource_packs, Icons.Filled.Texture),
    DATA_PACKS(R.string.content_type_data_packs, Icons.Filled.Description),
    SHADERS(R.string.content_type_shaders, Icons.Filled.Opacity),
    WORLDS(R.string.content_type_worlds, Icons.Filled.Public),
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

/** Helper for ContentType project type mapping. */
private val ContentType.modrinthProjectType: String
    get() = when (this) {
        ContentType.MODS -> "mod"
        ContentType.RESOURCE_PACKS -> "resourcepack"
        ContentType.DATA_PACKS -> "datapack"
        ContentType.SHADERS -> "shader"
        ContentType.WORLDS -> "world"
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
    var browsePage by remember { mutableStateOf(0) }

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
    val cdBackStr = stringResource(R.string.cd_back)
    val actionSearchStr = stringResource(R.string.action_search)
    val cdSearchStr = stringResource(R.string.cd_search)
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

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(manageContentTitle) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = cdBackStr)
                }
            },
            actions = {
                IconButton(onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) }) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = manageContentImport,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        // Main tabs: Installed | Browse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val tabs = listOf(
                stringResource(R.string.manage_content_installed) to 0,
                stringResource(R.string.manage_content_browse) to 1,
            )
            tabs.forEachIndexed { index, (label, _) ->
                FilterChip(
                    selected = mainTab == index,
                    onClick = { mainTab = index },
                    label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
                    leadingIcon = {
                        AnimatedVisibility(
                            visible = mainTab == index,
                            enter = expandIn(tween(150), expandFrom = Alignment.CenterStart) + fadeIn(tween(150)),
                            exit = shrinkOut(tween(150), shrinkTowards = Alignment.CenterStart) + fadeOut(tween(150)),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = mainTab == index),
                )
            }
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (mainTab) {
                0 -> InstalledTab(
                    contentType = installedContentType,
                    onContentTypeChange = { installedContentType = it },
                    installedContent = installedContent,
                )
                1 -> BrowseTab(
                    contentType = browseContentType,
                    onContentTypeChange = { browseContentType = it },
                    provider = browseProvider,
                    onProviderChange = { browseProvider = it },
                    query = browseQuery,
                    onQueryChange = { browseQuery = it },
                    results = browseResults,
                    loading = browseLoading,
                    error = browseError,
                    page = browsePage,
                    onPageChange = { browsePage = it },
                    onSearch = { term ->
                        scope.launch {
                            browseLoading = true
                            browseError = null
                            when (val result = client.searchMods(term, projectType = browseContentType.modrinthProjectType)) {
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
                    },
                    client = client,
                    scope = scope,
                )
            }
        }
    }
}

/** Installed tab with sub-tabs for content types. */
@Composable
private fun InstalledTab(
    contentType: ContentType,
    onContentTypeChange: (ContentType) -> Unit,
    installedContent: List<InstalledContent>,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tabs for content types
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ContentType.values().forEach { type ->
                FilterChip(
                    selected = contentType == type,
                    onClick = { onContentTypeChange(type) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(type.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(type.labelRes), style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = contentType == type),
                )
            }
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))

        // Content list
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val filteredContent = installedContent.filter { it.type == contentType }
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
    }
}

/** Browse tab with provider selector, search, and content list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowseTab(
    contentType: ContentType,
    onContentTypeChange: (ContentType) -> Unit,
    provider: Provider,
    onProviderChange: (Provider) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<ModrinthHit>,
    loading: Boolean,
    error: String?,
    page: Int,
    onPageChange: (Int) -> Unit,
    onSearch: (String) -> Unit,
    client: ModrinthClient,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    var localQuery by remember { mutableStateOf(query) }

    val context = LocalContext.current
    val providerComingSoonStr = stringResource(R.string.provider_coming_soon)

    Column(modifier = Modifier.fillMaxSize()) {
        // Provider filter
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Provider.values().forEach { p ->
                FilterChip(
                    selected = provider == p,
                    onClick = { onProviderChange(p) },
                    label = { Text(text = stringResource(p.labelRes), style = MaterialTheme.typography.labelLarge) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = provider == p),
                )
            }
        }

        // Content type filter
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ContentType.values().forEach { type ->
                FilterChip(
                    selected = contentType == type,
                    onClick = { onContentTypeChange(type) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(type.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(stringResource(type.labelRes), style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = contentType == type),
                )
            }
        }

        // Search field
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = localQuery,
                onValueChange = { localQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.manage_content_search_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.cd_search)) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onSearch(localQuery) }) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.action_search), tint = MaterialTheme.colorScheme.secondary)
            }
        }

        // Provider selector (Modrinth/CurseForge) - CurseForge coming soon
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Provider.values().forEach { p ->
                val isEnabled = p != Provider.CURSEFORGE
                FilterChip(
                    selected = provider == p,
                    onClick = {
                        if (p == Provider.CURSEFORGE) {
                            Toast.makeText(context, providerComingSoonStr, Toast.LENGTH_SHORT).show()
                        } else {
                            onProviderChange(p)
                        }
                    },
                    label = { Text(text = stringResource(p.labelRes), style = MaterialTheme.typography.labelLarge) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    border = FilterChipDefaults.filterChipBorder(enabled = isEnabled, selected = provider == p),
                )
            }
        }

        // Results area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading && results.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary) }

                error != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { Text(text = error.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }

                results.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { Text(text = stringResource(R.string.manage_content_empty), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.project_id }) { hit ->
                        BrowseContentRow(hit = hit, onClick = { /* open mod detail */ })
                    }
                }
            }
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
            // Content icon
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)) {
                Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(32.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Content info
            Column(modifier = Modifier.weight(1f)) {
                Text(text = content.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "v${content.version} · ${stringResource(content.type.labelRes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Actions: Update/Degrade, Toggle, Delete
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Update/Degrade icon
                IconButton(onClick = onUpdate) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.manage_content_update), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
                // Enable/Disable toggle
                Switch(
                    checked = content.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.secondary,
                        checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
                // Delete
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/** Browse content row with dropdown for versions. */
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
            // Mod icon
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)) {
                Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(32.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = hit.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = hit.author + " · " + formatDownloads(hit.downloads), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                if (hit.description.isNotBlank()) {
                    Text(text = hit.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                if (hit.categories.isNotEmpty() || hit.versions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = (hit.categories.take(3) + hit.versions.take(3)).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Install button
            IconButton(onClick = { /* open version dropdown */ }) {
                Icon(Icons.Filled.CloudDownload, contentDescription = stringResource(R.string.manage_content_install), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }
        }
    }
}