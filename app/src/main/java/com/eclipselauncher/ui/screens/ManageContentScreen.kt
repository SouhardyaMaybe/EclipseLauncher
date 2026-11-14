/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * MANAGE CONTENT: unified screen for both installed content and browsing.
 * Segmented control for Installed | Browse. Sticky content-type bar with
 * glyph chips + count badges. Content icons loaded via Coil with caching.
 */
package com.eclipselauncher.ui.screens

import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.eclipselauncher.R
import com.eclipselauncher.data.Instance
import com.eclipselauncher.data.LoaderKind
import com.eclipselauncher.data.McVersion
import com.eclipselauncher.data.MediaStorage
import com.eclipselauncher.data.MetaResult
import com.eclipselauncher.data.ModrinthClient
import com.eclipselauncher.data.ModrinthDependency
import com.eclipselauncher.data.ModrinthHit
import com.eclipselauncher.data.ModrinthVersion
import com.eclipselauncher.data.ModrinthVersionFile
import com.eclipselauncher.data.VersionRepository
import com.eclipselauncher.ui.components.InstanceIcon
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Content types for installed/browse tabs. Icon + label for the sticky bar chip. */
private enum class ContentType(
    val labelRes: Int,
    val icon: ImageVector,
) {
    MODS(R.string.content_type_mods, Icons.Filled.Layers),
    RESOURCE_PACKS(R.string.content_type_resource_packs, Icons.Filled.Image),
    DATA_PACKS(R.string.content_type_data_packs, Icons.Filled.Folder),
    SHADERS(R.string.content_type_shaders, Icons.Filled.WbSunny),
    WORLDS(R.string.content_type_worlds, Icons.Filled.Public),
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

/** Suffix used to disable a content file without deleting it. */
private const val DISABLED_SUFFIX = ".disabled"

/** Sidecar written next to content installed through this screen. */
private const val CONTENT_META_SUFFIX = ".eclipse.json"

/** Platform values carried by the installed-content metadata. */
private const val PLATFORM_MODRINTH = "modrinth"
private const val PLATFORM_LOCAL = "local"

/** One file or folder inside the selected instance's content directories. */
private data class InstalledContent(
    val file: File,
    val name: String,
    val version: String,
    val type: ContentType,
    val iconUrl: String?,
    val enabled: Boolean,
    val projectId: String? = null,
    val author: String? = null,
    val loaders: List<String> = emptyList(),
    val platform: String = PLATFORM_LOCAL,
) {
    /** Stable across the `.disabled` rename so LazyColumn keys survive toggles. */
    val id: String get() = "${type.name}:${file.name.removeSuffix(DISABLED_SUFFIX)}"
}

/** Metadata stored in a sidecar, so installed rows survive relaunch. */
private data class ContentMetadata(
    val projectId: String = "",
    val title: String = "",
    val author: String = "",
    val iconUrl: String = "",
    val version: String = "",
    val loaders: List<String> = emptyList(),
    val platform: String = "",
)

/** What the install sheet targets, including a file to replace on update. */
private data class ContentInstallTarget(
    val projectId: String,
    val slug: String = "",
    val title: String,
    val author: String = "",
    val iconUrl: String = "",
    val replace: InstalledContent? = null,
)

/** A required dependency with its resolved download and checkbox state. */
private data class ResolvedDependency(
    val projectId: String,
    val title: String,
    val iconUrl: String,
    val versionNumber: String,
    val version: ModrinthVersion?,
    val contentType: ContentType,
    val checked: Boolean,
)

/** The three filter headings shown under search. */
private enum class ContentFilter { LOADER, VERSION, PLATFORM }

/** One selectable row inside an expanded filter panel. */
private data class FilterOption(
    val value: String?,
    val label: String,
    val enabled: Boolean = true,
    val detail: String? = null,
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
                .height(44.dp)
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
                .height(44.dp)
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

/** Content type chip: labeled FilterChip with icon + text, count badge on Installed tab. */
@Composable
private fun ContentTypeChip(
    type: ContentType,
    selected: Boolean,
    count: Int,
    showBadge: Boolean,
    onClick: () -> Unit,
) {
    val labelText = stringResource(type.labelRes)
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        labelColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )

    // Custom chip with badge using Box overlay
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .height(36.dp),
    ) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            colors = chipColors,
            leadingIcon = {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            label = { Text(text = labelText, style = MaterialTheme.typography.labelLarge) },
        )

        // Badge for count on Installed tab (top-right corner)
        if (showBadge && count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = -4.dp)
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface,
                )
            }
        }
    }
}

/** Loads and caches a content icon via Coil. Falls back to a Material icon. */
@Composable
private fun ContentIcon(url: String?, fallbackIcon: ImageVector, size: Dp = 40.dp) {
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
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}

/** Installed content row: icon, name + "version · type", update, toggle, delete. */
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
            // Content icon via Coil (real icon from jar metadata/hash lookup, fallback to type icon)
            ContentIcon(
                url = content.iconUrl,
                fallbackIcon = content.type.icon,
                size = 56.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Content info
            Column(modifier = Modifier.weight(1f)) {
                Text(text = content.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val typeLabel = stringResource(content.type.labelRes)
                val details = if (content.version.isBlank()) typeLabel else "v${content.version} · $typeLabel"
                Text(text = details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Actions: Update/Degrade, Toggle, Delete
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onUpdate) {
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
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/** Categories the Modrinth search result also uses for mod loaders. */
private val LoaderCategories = setOf(
    "fabric",
    "forge",
    "neoforge",
    "quilt",
    "liteloader",
    "modloader",
    "rift",
)

private fun ModrinthHit.loaderNames(): List<String> =
    categories.filter { it.lowercase() in LoaderCategories }.distinct()

private fun ModrinthHit.displayCategories(): List<String> =
    categories.filterNot { it.lowercase() in LoaderCategories }.distinct()

/** Loaders offered in the Browse filter; installed content builds its own list. */
private val BrowseLoaderIds = listOf(
    "fabric",
    "forge",
    "neoforge",
    "quilt",
    "liteloader",
    "rift",
)

private fun loaderDisplayName(id: String): String = when (id.lowercase()) {
    "fabric" -> "Fabric"
    "forge" -> "Forge"
    "neoforge" -> "NeoForge"
    "quilt" -> "Quilt"
    "liteloader" -> "LiteLoader"
    "rift" -> "Rift"
    "modloader" -> "ModLoader"
    else -> id.replaceFirstChar { it.uppercase() }
}

/** Browse content row: name + author, description, loaders | categories, install icon. */
@Composable
private fun BrowseContentRow(
    hit: ModrinthHit,
    onClick: () -> Unit,
    onDownload: () -> Unit,
) {
    val loaders = remember(hit.categories) { hit.loaderNames().take(4) }
    val categories = remember(hit.categories) { hit.displayCategories().take(3) }
    val meta = buildList {
        if (loaders.isNotEmpty()) add(loaders.joinToString(" • "))
        if (categories.isNotEmpty()) add(categories.joinToString(" • "))
    }.joinToString(" | ")

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ContentIcon(
                url = hit.icon_url,
                fallbackIcon = Icons.Filled.Layers,
                size = 56.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hit.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hit.author.isNotBlank()) {
                    Text(
                        text = "by ${hit.author}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (hit.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = hit.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (meta.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Filled-tonal install button (opens version dialog)
            IconButton(
                onClick = onDownload,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = stringResource(R.string.manage_content_install),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** Filter panel: always expanded, content-type chips scroll horizontally. */
@Composable
private fun ContentTypeFilterPanel(
    contentType: ContentType,
    onContentTypeClick: (ContentType) -> Unit,
    installedContent: List<InstalledContent>,
    isInstalledTab: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ContentType.entries.forEach { type ->
                val count = if (isInstalledTab) {
                    installedContent.count { it.type == type }
                } else 0
                ContentTypeChip(
                    type = type,
                    selected = contentType == type,
                    count = count,
                    showBadge = isInstalledTab,
                    onClick = { onContentTypeClick(type) },
                )
            }
        }
    }
}

/** Search field: full-width, leading search icon, clear button when non-empty, keyboard search action. */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.manage_content_search_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.cd_search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        // Keyboard search action handled by onSearch callback from UI
        // colors = androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors(...) // API not available in this version
    )
}

/** Compact filter heading; tapping expands the shared option panel below. */
@Composable
private fun FilterHeader(
    title: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (expanded) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** One expanded filter's option list; scrolls so the content list stays usable. */
@Composable
private fun FilterOptionsPanel(
    options: List<FilterOption>,
    selectedValue: String?,
    onSelect: (FilterOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            options.forEachIndexed { index, option ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = option.enabled) { onSelect(option) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = option.value == selectedValue,
                        onClick = null,
                        enabled = option.enabled,
                    )
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (option.enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    option.detail?.let { detail ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Floating page pill, bottom-center: [prev] [page/total] [next]. */
@Composable
private fun PaginationPill(
    currentPage: Int,
    totalPages: Int,
    loading: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val canGoPrev = currentPage > 1 && !loading
    val canGoNext = currentPage < totalPages && !loading
    val buttonColors = IconButtonDefaults.iconButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrev,
                    enabled = canGoPrev,
                    colors = buttonColors,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = stringResource(R.string.cd_nav_previous),
                    )
                }
                // Fixed width stops the buttons from shifting between 1- and 2-digit pages.
                Box(modifier = Modifier.width(84.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$currentPage / $totalPages",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onNext,
                    enabled = canGoNext,
                    colors = buttonColors,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = stringResource(R.string.cd_nav_next),
                    )
                }
            }
        }
    }
}

/** Full-list loading state: M3 expressive indicator over the result area. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator(
            modifier = Modifier.size(56.dp),
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

private const val BROWSE_PAGE_SIZE = 25

@Composable
fun ManageContentScreen(
    client: ModrinthClient,
    instance: Instance?,
    media: MediaStorage,
    versionRepository: VersionRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

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
    var reloadToken by remember { mutableStateOf(0) }
    var versionTarget by remember { mutableStateOf<ContentInstallTarget?>(null) }
    var installedContent by remember(instance?.id) {
        mutableStateOf<List<InstalledContent>>(emptyList())
    }
    var installedReloadToken by remember(instance?.id) { mutableStateOf(0) }
    var pendingDelete by remember { mutableStateOf<InstalledContent?>(null) }

    // Search and filter state is kept separately per tab.
    var installedQuery by remember { mutableStateOf("") }
    var installedLoader by remember { mutableStateOf<String?>(null) }
    var installedVersion by remember { mutableStateOf<String?>(null) }
    var installedPlatform by remember { mutableStateOf<String?>(null) }
    var browseLoader by remember { mutableStateOf<String?>(null) }
    var browseGameVersion by remember { mutableStateOf<String?>(null) }
    var expandedFilter by remember { mutableStateOf<ContentFilter?>(null) }

    // Minecraft versions for the Browse version filter (same manifest cache as
    // the install sheet; the async fetch only fills it when it was empty).
    var mcVersions by remember { mutableStateOf(versionRepository.getCachedVersions()) }
    LaunchedEffect(versionRepository) {
        when (val result = versionRepository.minecraftVersions()) {
            is MetaResult.Success -> mcVersions = result.value
            is MetaResult.Error -> Unit
        }
    }

    // The Installed tab is a real scan of the selected instance's folders.
    LaunchedEffect(instance?.id, installedReloadToken, mainTab) {
        if (mainTab == 0) {
            installedContent = withContext(Dispatchers.IO) {
                scanInstalledContent(context.filesDir, instance)
            }
        }
    }

    // Pre-load strings for use in non-composable callbacks.
    val manageContentTitle = stringResource(R.string.manage_content_title)
    val manageContentImportStarted = stringResource(R.string.manage_content_import_started)
    val providerComingSoonStr = stringResource(R.string.provider_coming_soon)
    val marketErrorStr = stringResource(R.string.market_error)
    val toggleFailedStr = stringResource(R.string.manage_content_toggle_failed)
    val deleteFailedStr = stringResource(R.string.manage_content_delete_failed)
    val updateUnavailableStr = stringResource(R.string.manage_content_update_unavailable)
    val createInstanceFirstStr = stringResource(R.string.manage_content_no_instance)

    fun openInstallTarget(target: ContentInstallTarget) {
        if (instance == null) {
            Toast.makeText(context, createInstanceFirstStr, Toast.LENGTH_SHORT).show()
            return
        }
        versionTarget = target
    }

    fun toggleContent(content: InstalledContent) {
        scope.launch {
            val renamed = withContext(Dispatchers.IO) { toggleInstalledContent(content) }
            if (renamed) {
                installedReloadToken++
            } else {
                Toast.makeText(context, toggleFailedStr, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteContent(content: InstalledContent) {
        scope.launch {
            val deleted = withContext(Dispatchers.IO) { deleteInstalledContentFiles(content) }
            if (deleted) {
                installedReloadToken++
            } else {
                Toast.makeText(context, deleteFailedStr, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateContent(content: InstalledContent) {
        val projectId = content.projectId
        if (projectId.isNullOrBlank()) {
            Toast.makeText(context, updateUnavailableStr, Toast.LENGTH_SHORT).show()
        } else {
            versionTarget = ContentInstallTarget(
                projectId = projectId,
                title = content.name,
                author = content.author.orEmpty(),
                iconUrl = content.iconUrl.orEmpty(),
                replace = content,
            )
        }
    }

    // Import .zip launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(context, manageContentImportStarted, Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-load: first Browse open, tab switch back, source/type/query/filter
    // change, page change, or explicit retry. Empty query is the default listing.
    LaunchedEffect(
        mainTab,
        browseContentType,
        browseProvider,
        browseQuery,
        browseLoader,
        browseGameVersion,
        browsePage,
        reloadToken,
    ) {
        if (mainTab != 1) return@LaunchedEffect

        val requestedPage = browsePage
        browseLoading = true
        browseError = null
        val result = client.searchMods(
            query = browseQuery.trim(),
            limit = BROWSE_PAGE_SIZE,
            offset = (requestedPage - 1) * BROWSE_PAGE_SIZE,
            projectType = browseContentType.modrinthProjectType,
            loader = browseLoader.takeIf { browseContentType == ContentType.MODS },
            gameVersion = browseGameVersion,
        )

        // A newer request already replaced this one; never apply stale results.
        if (browsePage != requestedPage) return@LaunchedEffect

        if (result.failed) {
            browseResults = emptyList()
            browseTotalPages = 1
            browseError = marketErrorStr
        } else {
            browseResults = result.hits
            browseTotalPages = result.totalPages.coerceAtLeast(1)
        }
        browseLoading = false
    }

    // Every page change starts at the top of the list.
    LaunchedEffect(browsePage) {
        if (mainTab == 1) lazyListState.animateScrollToItem(0)
    }

    // ---- Search + filter model -------------------------------------------
    val anyLabel = stringResource(R.string.manage_content_filter_any)
    val loaderFilterTitle = stringResource(R.string.manage_content_filter_loader)
    val versionFilterTitle = stringResource(R.string.manage_content_filter_version)
    val platformFilterTitle = stringResource(R.string.manage_content_filter_platform)
    val modrinthLabel = stringResource(R.string.provider_modrinth)
    val localLabel = stringResource(R.string.platform_local)
    val providerLabels = Provider.entries.associateWith { stringResource(it.labelRes) }

    val installedOfType = installedContent.filter { it.type == installedContentType }
    val showLoaderFilter = if (mainTab == 0) {
        installedContentType == ContentType.MODS
    } else {
        browseContentType == ContentType.MODS
    }

    val loaderIds = if (mainTab == 0) {
        installedOfType.flatMap { it.loaders }.distinct().sortedBy { it.lowercase() }
    } else {
        BrowseLoaderIds
    }
    val loaderOptions = buildList {
        add(FilterOption(null, anyLabel))
        loaderIds.forEach { add(FilterOption(it, loaderDisplayName(it))) }
    }

    val installedVersionOptions = buildList {
        add(FilterOption(null, anyLabel))
        installedOfType
            .map { it.version }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedByDescending { it }
            .forEach { add(FilterOption(it, it)) }
    }
    val browseVersionOptions = buildList {
        add(FilterOption(null, anyLabel))
        val ids = LinkedHashSet<String>()
        instance?.minecraftVersion?.takeIf { it.isNotBlank() }?.let(ids::add)
        mcVersions.forEach { ids.add(it.id) }
        ids.forEach { id ->
            val kind = mcVersions.firstOrNull { it.id == id }?.type
            add(FilterOption(id, id, detail = kind?.takeIf { it != "release" }))
        }
    }

    val installedPlatformOptions = buildList {
        add(FilterOption(null, anyLabel))
        val present = installedOfType.map { it.platform }.toSet()
        if (PLATFORM_MODRINTH in present) {
            add(FilterOption(PLATFORM_MODRINTH, modrinthLabel))
        }
        if (PLATFORM_LOCAL in present) {
            add(FilterOption(PLATFORM_LOCAL, localLabel))
        }
    }
    val browsePlatformOptions = Provider.entries.map { provider ->
        FilterOption(
            value = provider.name,
            label = providerLabels[provider].orEmpty(),
            enabled = provider == Provider.MODRINTH,
            detail = if (provider != Provider.MODRINTH) providerComingSoonStr else null,
        )
    }

    val loaderFilterValue = if (mainTab == 0) installedLoader else browseLoader
    val versionFilterValue = if (mainTab == 0) installedVersion else browseGameVersion
    val platformFilterValue = if (mainTab == 0) installedPlatform else browseProvider.name
    val loaderFilterLabel = loaderFilterValue?.let(::loaderDisplayName) ?: anyLabel
    val versionFilterLabel = versionFilterValue ?: anyLabel
    val platformFilterLabel = when {
        mainTab == 0 -> when (installedPlatform) {
            PLATFORM_MODRINTH -> modrinthLabel
            PLATFORM_LOCAL -> localLabel
            else -> anyLabel
        }
        else -> providerLabels[browseProvider].orEmpty().ifBlank { anyLabel }
    }

    val installedQueryTrimmed = installedQuery.trim()
    val filteredInstalled = installedOfType.filter { content ->
        (
            installedQueryTrimmed.isBlank() ||
                content.name.contains(installedQueryTrimmed, ignoreCase = true) ||
                content.version.contains(installedQueryTrimmed, ignoreCase = true) ||
                content.author?.contains(installedQueryTrimmed, ignoreCase = true) == true
            ) &&
            (installedLoader == null ||
                content.loaders.any { it.equals(installedLoader, ignoreCase = true) }) &&
            (installedVersion == null || content.version == installedVersion) &&
            (installedPlatform == null || content.platform == installedPlatform)
    }
    val hasInstalledFilters = installedQuery.isNotBlank() ||
        installedLoader != null ||
        installedVersion != null ||
        installedPlatform != null
    val hasBrowseFilters = browseQuery.isNotBlank() ||
        browseContentType != ContentType.MODS ||
        browseLoader != null ||
        browseGameVersion != null

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
            onTabSelected = { selected ->
                if (selected != mainTab) {
                    mainTab = selected
                    expandedFilter = null
                }
            },
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))

        // Search field: directly below the tabs on both tabs.
        SearchField(
            query = if (mainTab == 0) installedQuery else browseQuery,
            onQueryChange = { value ->
                if (mainTab == 0) {
                    installedQuery = value
                } else {
                    browseQuery = value
                    browsePage = 1
                }
            },
            onSearch = {
                if (mainTab == 1) {
                    browsePage = 1
                    reloadToken++
                }
            },
            onClear = {
                if (mainTab == 0) {
                    installedQuery = ""
                } else {
                    browseQuery = ""
                    browsePage = 1
                    reloadToken++
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // Content-type chips are kept just above the list; selecting one resets
        // the type-specific filters so no impossible combination lingers.
        val activeContentType = if (mainTab == 0) installedContentType else browseContentType
        val onTypeClick: (ContentType) -> Unit = if (mainTab == 0) {
            { type ->
                installedContentType = type
                installedLoader = null
                installedVersion = null
                installedPlatform = null
                expandedFilter = null
            }
        } else {
            { type ->
                browseContentType = type
                if (type != ContentType.MODS) browseLoader = null
                browsePage = 1
                expandedFilter = null
            }
        }

        // Filter headings: loader only where the content type has loaders.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showLoaderFilter) {
                FilterHeader(
                    title = loaderFilterTitle,
                    value = loaderFilterLabel,
                    expanded = expandedFilter == ContentFilter.LOADER,
                    onClick = {
                        expandedFilter = if (expandedFilter == ContentFilter.LOADER) {
                            null
                        } else {
                            ContentFilter.LOADER
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            FilterHeader(
                title = versionFilterTitle,
                value = versionFilterLabel,
                expanded = expandedFilter == ContentFilter.VERSION,
                onClick = {
                    expandedFilter = if (expandedFilter == ContentFilter.VERSION) {
                        null
                    } else {
                        ContentFilter.VERSION
                    }
                },
                modifier = Modifier.weight(1f),
            )
            FilterHeader(
                title = platformFilterTitle,
                value = platformFilterLabel,
                expanded = expandedFilter == ContentFilter.PLATFORM,
                onClick = {
                    expandedFilter = if (expandedFilter == ContentFilter.PLATFORM) {
                        null
                    } else {
                        ContentFilter.PLATFORM
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }

        when (expandedFilter) {
            ContentFilter.LOADER -> if (showLoaderFilter) {
                FilterOptionsPanel(
                    options = loaderOptions,
                    selectedValue = loaderFilterValue,
                    onSelect = { option ->
                        if (mainTab == 0) {
                            installedLoader = option.value
                        } else {
                            browseLoader = option.value
                            browsePage = 1
                        }
                        expandedFilter = null
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            ContentFilter.VERSION -> FilterOptionsPanel(
                options = if (mainTab == 0) installedVersionOptions else browseVersionOptions,
                selectedValue = versionFilterValue,
                onSelect = { option ->
                    if (mainTab == 0) {
                        installedVersion = option.value
                    } else {
                        browseGameVersion = option.value
                        browsePage = 1
                    }
                    expandedFilter = null
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            ContentFilter.PLATFORM -> FilterOptionsPanel(
                options = if (mainTab == 0) installedPlatformOptions else browsePlatformOptions,
                selectedValue = platformFilterValue,
                onSelect = { option ->
                    if (mainTab == 0) {
                        installedPlatform = option.value
                    } else {
                        option.value?.let { name ->
                            runCatching { Provider.valueOf(name) }.getOrNull()?.let { provider ->
                                browseProvider = provider
                                browsePage = 1
                            }
                        }
                    }
                    expandedFilter = null
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            null -> Unit
        }

        ContentTypeFilterPanel(
            contentType = activeContentType,
            onContentTypeClick = onTypeClick,
            installedContent = installedContent,
            isInstalledTab = mainTab == 0,
        )

        // Content list (pagination floats above it, never covering the last row).
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (mainTab) {
                0 -> {
                    if (filteredInstalled.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(
                                        if (hasInstalledFilters) {
                                            R.string.manage_content_no_matches
                                        } else {
                                            R.string.manage_content_empty
                                        },
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (hasInstalledFilters) {
                                    TextButton(onClick = {
                                        installedQuery = ""
                                        installedLoader = null
                                        installedVersion = null
                                        installedPlatform = null
                                    }) {
                                        Text(stringResource(R.string.manage_content_clear_filters))
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredInstalled, key = { it.id }) { content ->
                                InstalledContentRow(
                                    content = content,
                                    onUpdate = { updateContent(content) },
                                    onToggle = { toggleContent(content) },
                                    onDelete = { pendingDelete = content },
                                )
                            }
                        }
                    }
                }
                1 -> {
                    when {
                        // Full-list indicator below covers the initial fetch.
                        browseLoading && browseResults.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                        )

                        browseError != null -> Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = marketErrorStr,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                            TextButton(onClick = { reloadToken++ }) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }

                        browseResults.isEmpty() -> Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.manage_content_no_results),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (hasBrowseFilters) {
                                TextButton(onClick = {
                                    browseQuery = ""
                                    browseContentType = ContentType.MODS
                                    browseLoader = null
                                    browseGameVersion = null
                                    browsePage = 1
                                    expandedFilter = null
                                    reloadToken++
                                }) {
                                    Text(stringResource(R.string.manage_content_clear_filters))
                                }
                            }
                        }

                        else -> LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = if (browseTotalPages > 1) 96.dp else 16.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(browseResults, key = { it.project_id.ifBlank { it.slug } }) { hit ->
                                BrowseContentRow(
                                    hit = hit,
                                    onClick = { openInstallTarget(hit.toInstallTarget()) },
                                    onDownload = { openInstallTarget(hit.toInstallTarget()) },
                                )
                            }
                        }
                    }
                }
            }

            // Full-list progress: replaces the small spinner that used to live
            // in the pagination pill.
            if (mainTab == 1 && browseLoading) {
                ListLoadingOverlay()
            }

            // Floating pagination pill (Browse only, when multiple pages).
            if (mainTab == 1 && browseTotalPages > 1) {
                PaginationPill(
                    currentPage = browsePage,
                    totalPages = browseTotalPages,
                    loading = browseLoading,
                    onPrev = {
                        if (!browseLoading && browsePage > 1) {
                            browseLoading = true
                            browsePage--
                        }
                    },
                    onNext = {
                        if (!browseLoading && browsePage < browseTotalPages) {
                            browseLoading = true
                            browsePage++
                        }
                    },
                )
            }
        }
    }

    versionTarget?.let { target ->
        val dialogContentType = target.replace?.type ?: browseContentType
        VersionInstallDialog(
            target = target,
            contentType = dialogContentType,
            instance = instance,
            media = media,
            versionRepository = versionRepository,
            client = client,
            // Updates/downgrades replace one file; dependencies are already set up.
            showDependencies = target.replace == null,
            onDismiss = { versionTarget = null },
            onInstalled = { _, _ ->
                installedReloadToken++
                if (target.replace == null) {
                    installedContentType = dialogContentType
                    mainTab = 0
                }
            },
        )
    }

    pendingDelete?.let { content ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(stringResource(R.string.manage_content_delete_title, content.name))
            },
            text = { Text(stringResource(R.string.manage_content_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    deleteContent(content)
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** Full-screen version chooser + installer for one Modrinth project. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VersionInstallDialog(
    target: ContentInstallTarget,
    contentType: ContentType,
    instance: Instance?,
    media: MediaStorage,
    versionRepository: VersionRepository,
    client: ModrinthClient,
    showDependencies: Boolean = true,
    onDismiss: () -> Unit,
    onInstalled: (ModrinthVersion, Instance) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var versions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableStateOf(0) }
    var mcVersions by remember { mutableStateOf(versionRepository.getCachedVersions()) }
    var stableOnly by remember { mutableStateOf(true) }
    var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var checkingDependencies by remember { mutableStateOf(false) }
    var pendingVersion by remember { mutableStateOf<ModrinthVersion?>(null) }
    var pendingDependencies by remember { mutableStateOf<List<ResolvedDependency>?>(null) }
    var installError by remember { mutableStateOf<String?>(null) }
    val busy = downloadingId != null || checkingDependencies

    LaunchedEffect(target.projectId, target.slug, reloadToken) {
        loading = true
        failed = false
        expandedGroups = emptySet()
        val result = client.projectVersions(target.projectId.ifBlank { target.slug })
        versions = result.versions
        failed = result.failed
        loading = false
    }

    // MC-version types decide which groups are stable and colour their badges.
    LaunchedEffect(versionRepository) {
        when (val result = versionRepository.minecraftVersions()) {
            is MetaResult.Success -> mcVersions = result.value
            is MetaResult.Error -> Unit
        }
    }

    val groups = remember(versions, mcVersions) {
        buildVersionGroups(versions, mcVersions)
    }
    val stableAvailable = mcVersions.isNotEmpty()
    val visibleGroups = remember(groups, stableOnly, stableAvailable) {
        if (stableOnly && stableAvailable) groups.filter { it.type == "release" } else groups
    }
    val visibleVersionCount = visibleGroups.sumOf { it.versions.size }

    /** Downloads the main file (and checked dependencies), then writes metadata. */
    fun startInstall(version: ModrinthVersion, dependencies: List<ResolvedDependency>) {
        val targetInstance = instance ?: return
        val file = version.files.firstOrNull { it.primary } ?: version.files.firstOrNull()
        if (file == null) {
            installError = context.getString(R.string.manage_content_install_failed)
            return
        }
        installError = null
        pendingVersion = null
        pendingDependencies = null
        downloadingId = version.id
        downloadProgress = 0f
        scope.launch {
            val result = installVersionWithDependencies(
                context = context,
                client = client,
                instance = targetInstance,
                contentType = contentType,
                version = version,
                file = file,
                target = target,
                dependencies = dependencies,
            ) { progress ->
                downloadProgress = progress
            }
            downloadingId = null
            result.fold(
                onSuccess = {
                    Toast.makeText(
                        context,
                        context.getString(R.string.manage_content_installed_toast, targetInstance.name),
                        Toast.LENGTH_SHORT,
                    ).show()
                    onInstalled(version, targetInstance)
                    onDismiss()
                },
                onFailure = {
                    installError = context.getString(R.string.manage_content_install_failed)
                },
            )
        }
    }

    /** Resolves required dependencies before showing the prompt. */
    fun requestInstall(version: ModrinthVersion) {
        val targetInstance = instance ?: return
        installError = null
        // Update/downgrade only swaps the main file and never prompts for or
        // installs dependencies; a fresh install does the full resolution.
        if (!showDependencies) {
            startInstall(version, emptyList())
            return
        }
        val required = version.dependencies.filter { it.dependency_type == "required" }
        if (required.isEmpty()) {
            startInstall(version, emptyList())
            return
        }
        pendingVersion = version
        pendingDependencies = null
        checkingDependencies = true
        scope.launch {
            val resolved = resolveDependencies(client, targetInstance, version)
            checkingDependencies = false
            if (pendingVersion?.id == version.id) {
                pendingDependencies = resolved
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = target.title.ifBlank { target.slug },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.manage_content_versions_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))

                // Exactly one instance is targeted: the one selected on HOME.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.manage_content_selected_instance),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    if (instance == null) {
                        Text(
                            text = stringResource(R.string.manage_content_no_instance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        InstanceIcon(instance = instance, media = media, size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = instance.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = stableOnly,
                        onClick = { stableOnly = !stableOnly },
                        enabled = !busy,
                        label = { Text(stringResource(R.string.manage_content_stable_only)) },
                        leadingIcon = if (stableOnly) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else {
                            null
                        },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(
                            R.string.manage_content_versions_count,
                            visibleVersionCount,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (downloadingId != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.manage_content_installing,
                                (downloadProgress * 100).toInt(),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                installError?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                if (checkingDependencies) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.manage_content_checking_dependencies),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        loading -> LoadingIndicator(
                            modifier = Modifier.align(Alignment.Center).size(56.dp),
                            color = MaterialTheme.colorScheme.secondary,
                        )

                        failed -> Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.manage_content_versions_error),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                            TextButton(onClick = { reloadToken++ }) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }

                        versions.isEmpty() -> CenteredDialogMessage(
                            text = stringResource(R.string.manage_content_no_versions),
                        )

                        visibleGroups.isEmpty() -> Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(
                                    if (stableOnly && stableAvailable) {
                                        R.string.manage_content_no_stable_versions
                                    } else {
                                        R.string.manage_content_no_versions
                                    },
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (stableOnly && stableAvailable) {
                                TextButton(onClick = { stableOnly = false }) {
                                    Text(stringResource(R.string.manage_content_show_all_versions))
                                }
                            }
                        }

                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 24.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(visibleGroups, key = { it.mcVersion }) { group ->
                                VersionGroupCard(
                                    group = group,
                                    highlight = group.mcVersion == instance?.minecraftVersion,
                                    expanded = group.mcVersion in expandedGroups,
                                    onToggle = {
                                        expandedGroups = if (group.mcVersion in expandedGroups) {
                                            expandedGroups - group.mcVersion
                                        } else {
                                            expandedGroups + group.mcVersion
                                        }
                                    },
                                    installingId = downloadingId,
                                    progress = downloadProgress,
                                    installEnabled = instance != null && !busy,
                                    onInstall = ::requestInstall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val dependencyList = pendingDependencies
    val versionToInstall = pendingVersion
    if (dependencyList != null && versionToInstall != null) {
        DependencyDialog(
            contentTitle = target.title.ifBlank { target.slug },
            dependencies = dependencyList,
            onToggle = { index ->
                pendingDependencies = dependencyList.mapIndexed { i, dependency ->
                    if (i == index && dependency.version != null) {
                        dependency.copy(checked = !dependency.checked)
                    } else {
                        dependency
                    }
                }
            },
            onIgnore = { startInstall(versionToInstall, emptyList()) },
            onInstall = {
                startInstall(
                    versionToInstall,
                    dependencyList.filter { it.checked && it.version != null },
                )
            },
        )
    }
}

/** Required-dependency prompt: tap a row to include or exclude it. */
@Composable
private fun DependencyDialog(
    contentTitle: String,
    dependencies: List<ResolvedDependency>,
    onToggle: (Int) -> Unit,
    onIgnore: () -> Unit,
    onInstall: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onIgnore,
        title = { Text(stringResource(R.string.manage_content_dependencies_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(
                        R.string.manage_content_dependencies_body,
                        contentTitle,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                dependencies.forEachIndexed { index, dependency ->
                    val available = dependency.version != null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = available) { onToggle(index) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = dependency.checked,
                            onCheckedChange = { onToggle(index) },
                            enabled = available,
                        )
                        ContentIcon(
                            url = dependency.iconUrl.ifBlank { null },
                            fallbackIcon = dependency.contentType.icon,
                            size = 36.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dependency.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (available) {
                                    dependency.versionNumber
                                } else {
                                    stringResource(R.string.manage_content_dependency_unavailable)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onInstall,
                enabled = dependencies.any { it.checked && it.version != null },
            ) {
                Text(stringResource(R.string.manage_content_dependencies_install))
            }
        },
        dismissButton = {
            TextButton(onClick = onIgnore) {
                Text(stringResource(R.string.manage_content_dependencies_ignore))
            }
        },
    )
}

/** One stable-MC group: heading + badge, expanding into the versions inside it. */
@Composable
private fun VersionGroupCard(
    group: VersionGroup,
    highlight: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    installingId: String?,
    progress: Float,
    installEnabled: Boolean,
    onInstall: (ModrinthVersion) -> Unit,
) {
    val matchGreen = Color(0xFF4CAF50)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = if (highlight) BorderStroke(1.5.dp, matchGreen) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) {
                matchGreen.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            },
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = group.mcVersion,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = mcVersionTypeColor(group.type),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Text(
                        text = mcVersionTypeLabel(group.type),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    group.versions.forEachIndexed { index, version ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            )
                        }
                        VersionEntryRow(
                            version = version,
                            installing = installingId == version.id,
                            progress = progress,
                            installEnabled = installEnabled && version.files.isNotEmpty(),
                            onInstall = { onInstall(version) },
                        )
                    }
                }
            }
        }
    }
}

/** Expanded content version: name, version, updated-ago, downloads, install icon. */
@Composable
private fun VersionEntryRow(
    version: ModrinthVersion,
    installing: Boolean,
    progress: Float,
    installEnabled: Boolean,
    onInstall: () -> Unit,
) {
    val updated = remember(version.date_published) {
        relativeUpdatedTime(version.date_published)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = version.name.ifBlank { version.version_number },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (version.version_number.isNotBlank() &&
                    version.version_number != version.name
                ) {
                    Text(
                        text = version.version_number,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (updated.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = updated,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                    }
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDownloads(version.downloads),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = onInstall,
                enabled = installEnabled,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.manage_content_install),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (installing) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.manage_content_installing,
                    (progress * 100).toInt(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One Minecraft version as a group, in manifest (newest-first) order. */
private data class VersionGroup(
    val mcVersion: String,
    val type: String,
    val versions: List<ModrinthVersion>,
)

/**
 * Groups content versions under every Minecraft version they list. The
 * manifest supplies the group order and the release/snapshot type used by
 * the stable filter and the badge; groups missing from the manifest fall
 * back to "release" and after the known ones.
 */
private fun buildVersionGroups(
    versions: List<ModrinthVersion>,
    manifest: List<McVersion>,
): List<VersionGroup> {
    val typeById = manifest.associate { it.id to it.type }
    val orderById = manifest.mapIndexed { index, version -> version.id to index }.toMap()
    val byGameVersion = LinkedHashMap<String, MutableList<ModrinthVersion>>()
    versions.forEach { version ->
        version.game_versions.distinct().filter { it.isNotBlank() }.forEach { gameVersion ->
            byGameVersion.getOrPut(gameVersion) { mutableListOf() }.add(version)
        }
    }
    val groups = byGameVersion.map { (gameVersion, groupVersions) ->
        VersionGroup(
            mcVersion = gameVersion,
            type = typeById[gameVersion] ?: "release",
            versions = groupVersions.sortedByDescending { it.date_published },
        )
    }
    if (manifest.isEmpty()) return groups
    return groups.sortedWith(
        compareBy<VersionGroup> { orderById[it.mcVersion] ?: Int.MAX_VALUE }
            .thenByDescending { it.mcVersion },
    )
}

private fun mcVersionTypeColor(type: String): Color = when (type) {
    "snapshot" -> Color(0xFFFFD600)
    "old_beta" -> Color(0xFFFF9800)
    "old_alpha" -> Color(0xFFF44336)
    else -> Color(0xFF4CAF50)
}

@Composable
private fun mcVersionTypeLabel(type: String): String = when (type) {
    "snapshot" -> stringResource(R.string.version_tab_snapshots)
    "old_beta" -> stringResource(R.string.version_tab_old_betas)
    "old_alpha" -> stringResource(R.string.version_tab_old_alphas)
    else -> stringResource(R.string.version_tab_releases)
}

/** "4 days ago" / "1 month ago" / "2 years ago" from Modrinth's ISO date. */
private fun relativeUpdatedTime(datePublished: String): String {
    if (datePublished.isBlank()) return ""
    val epochMillis = runCatching { Instant.parse(datePublished).toEpochMilli() }.getOrNull()
        ?: return datePublished.take(10)
    return DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}

@Composable
private fun CenteredDialogMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** MODS -> mods, RESOURCE_PACKS -> resourcepacks, etc. */
private val ContentType.installDirectory: String
    get() = when (this) {
        ContentType.MODS -> "mods"
        ContentType.RESOURCE_PACKS -> "resourcepacks"
        ContentType.DATA_PACKS -> "datapacks"
        ContentType.SHADERS -> "shaderpacks"
        ContentType.WORLDS -> "saves"
    }

/** Browse hit -> install sheet target. */
private fun ModrinthHit.toInstallTarget(): ContentInstallTarget = ContentInstallTarget(
    projectId = project_id,
    slug = slug,
    title = title,
    author = author,
    iconUrl = icon_url,
)

private fun instanceContentDir(filesDir: File, instance: Instance, type: ContentType): File =
    File(filesDir, "minecraft/instances/${instance.id}/${type.installDirectory}")

/** Real installed files, with metadata from their sidecar when present. */
private fun scanInstalledContent(filesDir: File, instance: Instance?): List<InstalledContent> {
    if (instance == null) return emptyList()
    return ContentType.entries.flatMap { type ->
        val dir = instanceContentDir(filesDir, instance, type)
        dir.listFiles().orEmpty().mapNotNull { file ->
            if (file.name.endsWith(CONTENT_META_SUFFIX) || file.name.endsWith(".part")) {
                return@mapNotNull null
            }
            if (type != ContentType.WORLDS && !file.isFile) return@mapNotNull null
            val enabled = !file.name.endsWith(DISABLED_SUFFIX)
            val baseName = file.name.removeSuffix(DISABLED_SUFFIX)
            val metadata = readContentMetadata(file)
            val fallbackName = if (file.isDirectory) {
                baseName
            } else {
                baseName.substringBeforeLast('.', baseName)
            }
            // Legacy mods have no loaders in their sidecar; a mod living in a
            // loader instance is at least tagged with that instance's loader.
            val loaders = metadata.loaders.ifEmpty {
                if (type == ContentType.MODS) {
                    listOfNotNull(loaderName(instance.loader))
                } else {
                    emptyList()
                }
            }
            InstalledContent(
                file = file,
                name = metadata.title.ifBlank { fallbackName },
                version = metadata.version,
                type = type,
                iconUrl = metadata.iconUrl.ifBlank { null },
                enabled = enabled,
                projectId = metadata.projectId.ifBlank { null },
                author = metadata.author.ifBlank { null },
                loaders = loaders,
                // Legacy sidecars have no platform; a Modrinth project id is
                // the only reliable hint, everything else is a local file.
                platform = metadata.platform.ifBlank {
                    if (metadata.projectId.isNotBlank()) PLATFORM_MODRINTH else PLATFORM_LOCAL
                },
            )
        }
    }.sortedBy { it.name.lowercase() }
}

private fun metadataFile(file: File): File =
    File(file.parentFile, file.name + CONTENT_META_SUFFIX)

private fun readContentMetadata(file: File): ContentMetadata {
    val current = metadataFile(file)
    val base = File(
        file.parentFile,
        file.name.removeSuffix(DISABLED_SUFFIX) + CONTENT_META_SUFFIX,
    )
    val source = when {
        current.exists() -> current
        base.exists() -> base
        else -> return ContentMetadata()
    }
    return runCatching {
        val json = JSONObject(source.readText())
        val loaders = json.optJSONArray("loaders")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).takeIf { it.isNotBlank() }
            }
        }.orEmpty()
        ContentMetadata(
            projectId = json.optString("project_id"),
            title = json.optString("title"),
            author = json.optString("author"),
            iconUrl = json.optString("icon_url"),
            version = json.optString("version"),
            loaders = loaders,
            platform = json.optString("platform"),
        )
    }.getOrDefault(ContentMetadata())
}

private fun writeContentMetadata(file: File, metadata: ContentMetadata): Boolean = runCatching {
    val json = JSONObject()
        .put("project_id", metadata.projectId)
        .put("title", metadata.title)
        .put("author", metadata.author)
        .put("icon_url", metadata.iconUrl)
        .put("version", metadata.version)
        .put("loaders", JSONArray(metadata.loaders))
        .put("platform", metadata.platform)
    metadataFile(file).writeText(json.toString())
}.isSuccess

/** Renames between `name` and `name.disabled`, carrying the sidecar along. */
private fun toggleInstalledContent(content: InstalledContent): Boolean {
    val file = content.file
    val target = if (content.enabled) {
        File(file.parentFile, file.name + DISABLED_SUFFIX)
    } else {
        File(file.parentFile, file.name.removeSuffix(DISABLED_SUFFIX))
    }
    if (target.exists()) return false
    if (!file.renameTo(target)) return false
    val oldMetadata = metadataFile(file)
    if (oldMetadata.exists()) {
        oldMetadata.renameTo(metadataFile(target))
    }
    return true
}

private fun deleteInstalledContentFiles(content: InstalledContent): Boolean {
    val file = content.file
    val deleted = !file.exists() || file.deleteRecursively()
    metadataFile(file).delete()
    File(
        file.parentFile,
        file.name.removeSuffix(DISABLED_SUFFIX) + CONTENT_META_SUFFIX,
    ).delete()
    return deleted
}

/** Required dependencies only, resolved against the instance's MC version. */
private suspend fun resolveDependencies(
    client: ModrinthClient,
    instance: Instance,
    version: ModrinthVersion,
): List<ResolvedDependency> = coroutineScope {
    version.dependencies
        .filter { it.dependency_type == "required" }
        .map { dependency -> async { resolveDependency(client, instance, dependency) } }
        .awaitAll()
        .distinctBy { it.projectId.ifBlank { it.title.lowercase() } }
}

private suspend fun resolveDependency(
    client: ModrinthClient,
    instance: Instance,
    dependency: ModrinthDependency,
): ResolvedDependency {
    val projectId = dependency.project_id.orEmpty()
    val project = projectId.takeIf { it.isNotBlank() }?.let { client.projectById(it) }
    val contentType = dependencyContentType(project?.project_type)
    val versionId = dependency.version_id
    val resolvedVersion = when {
        !versionId.isNullOrBlank() -> client.versionById(versionId)
        projectId.isNotBlank() -> client.projectVersions(projectId).versions
            .firstOrNull { isCompatibleVersion(it, instance, contentType) }
        else -> null
    }
    val title = project?.title?.takeIf { it.isNotBlank() }
        ?: dependency.file_name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
        ?: projectId.ifBlank { instance.name }
    return ResolvedDependency(
        projectId = projectId,
        title = title,
        iconUrl = project?.icon_url.orEmpty(),
        versionNumber = resolvedVersion?.version_number.orEmpty(),
        version = resolvedVersion,
        contentType = contentType,
        checked = resolvedVersion != null,
    )
}

private fun dependencyContentType(projectType: String?): ContentType =
    when (projectType) {
        "resourcepack" -> ContentType.RESOURCE_PACKS
        "datapack" -> ContentType.DATA_PACKS
        "shader" -> ContentType.SHADERS
        "world" -> ContentType.WORLDS
        else -> ContentType.MODS
    }

private fun loaderName(loader: LoaderKind): String? = when (loader) {
    LoaderKind.FABRIC -> "fabric"
    LoaderKind.FORGE -> "forge"
    LoaderKind.NEOFORGE -> "neoforge"
    LoaderKind.QUILT -> "quilt"
    else -> null
}

private fun isCompatibleVersion(
    version: ModrinthVersion,
    instance: Instance,
    contentType: ContentType,
): Boolean {
    if (version.game_versions.isNotEmpty() &&
        instance.minecraftVersion !in version.game_versions
    ) {
        return false
    }
    if (contentType != ContentType.MODS) return true
    val loader = loaderName(instance.loader) ?: return true
    return version.loaders.isEmpty() ||
        version.loaders.any { it.equals(loader, ignoreCase = true) }
}

/** Installs selected dependencies first, then the main file and its metadata. */
private suspend fun installVersionWithDependencies(
    context: Context,
    client: ModrinthClient,
    instance: Instance,
    contentType: ContentType,
    version: ModrinthVersion,
    file: ModrinthVersionFile,
    target: ContentInstallTarget,
    dependencies: List<ResolvedDependency>,
    onProgress: (Float) -> Unit,
): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        dependencies.forEach { dependency ->
            val dependencyVersion = dependency.version ?: return@forEach
            val dependencyFile = dependencyVersion.files.firstOrNull { it.primary }
                ?: dependencyVersion.files.firstOrNull()
                ?: return@forEach
            val installed = installContentFile(
                context = context,
                client = client,
                instance = instance,
                type = dependency.contentType,
                file = dependencyFile,
                skipExisting = true,
            ).getOrThrow()
            writeContentMetadata(
                installed,
                ContentMetadata(
                    projectId = dependency.projectId,
                    title = dependency.title,
                    iconUrl = dependency.iconUrl,
                    version = dependencyVersion.version_number,
                    loaders = dependencyVersion.loaders,
                    platform = PLATFORM_MODRINTH,
                ),
            )
        }

        // Never overwrite an existing main file; updates use a new filename.
        val installed = installContentFile(
            context = context,
            client = client,
            instance = instance,
            type = contentType,
            file = file,
            onProgress = onProgress,
        ).getOrThrow()
        if (!writeContentMetadata(
                installed,
                ContentMetadata(
                    projectId = target.projectId,
                    title = target.title,
                    author = target.author,
                    iconUrl = target.iconUrl,
                    version = version.version_number,
                    loaders = version.loaders,
                    platform = PLATFORM_MODRINTH,
                ),
            )
        ) {
            error("cannot write metadata")
        }

        target.replace?.let { deleteInstalledContentFiles(it) }
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}

/** Streams a Modrinth file into the selected instance's content folder. */
private suspend fun installContentFile(
    context: Context,
    client: ModrinthClient,
    instance: Instance,
    type: ContentType,
    file: ModrinthVersionFile,
    skipExisting: Boolean = false,
    onProgress: (Float) -> Unit = {},
): Result<File> = withContext(Dispatchers.IO) {
    var partFile: File? = null
    try {
        if (file.url.isBlank()) error("missing file url")
        val dir = instanceContentDir(context.filesDir, instance, type)
        if (!dir.exists() && !dir.mkdirs()) error("cannot create ${dir.path}")
        val safeName = file.filename
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .ifBlank { "download" }
        if (skipExisting) {
            val existing = File(dir, safeName)
            if (existing.exists()) return@withContext Result.success(existing)
            val disabled = File(dir, safeName + DISABLED_SUFFIX)
            if (disabled.exists()) return@withContext Result.success(disabled)
        }
        val target = uniqueFile(dir, safeName)
        val part = File(dir, "${target.name}.part")
        partFile = part
        part.delete()
        if (!client.downloadTo(file.url, part, onProgress)) {
            error("download failed")
        }
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }
        partFile = null
        Result.success(target)
    } catch (cancelled: CancellationException) {
        partFile?.delete()
        throw cancelled
    } catch (error: Exception) {
        partFile?.delete()
        Result.failure(error)
    }
}

/** Never overwrite an existing file; add " (1)", " (2)" before the extension. */
private fun uniqueFile(dir: File, filename: String): File {
    val first = File(dir, filename)
    if (!first.exists()) return first
    val dot = filename.lastIndexOf('.')
    val base = if (dot > 0) filename.substring(0, dot) else filename
    val extension = if (dot > 0) filename.substring(dot) else ""
    var index = 1
    var candidate = first
    while (candidate.exists()) {
        candidate = File(dir, "$base ($index)$extension")
        index++
    }
    return candidate
}
