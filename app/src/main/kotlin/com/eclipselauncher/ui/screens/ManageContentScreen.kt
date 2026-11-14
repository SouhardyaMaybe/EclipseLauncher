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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
                Text(text = "v${content.version} · ${stringResource(content.type.labelRes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

/** Browse content row with Coil-loaded icon: project icon, title, 2-line description, size/support range, filled-tonal download button. */
@Composable
private fun BrowseContentRow(
    hit: ModrinthHit,
    onClick: () -> Unit,
    onDownload: () -> Unit,
) {
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
            // Project icon via Coil (56dp)
            ContentIcon(
                url = hit.icon_url,
                fallbackIcon = Icons.Filled.Layers,
                size = 56.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Content info
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Text(text = hit.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Author (no download count)
                Text(text = hit.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Description (2 lines)
                if (hit.description.isNotBlank()) {
                    Text(text = hit.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                // Support range: loaders + game versions (placeholder - would come from API)
                Text(
                    text = "Fabric, Forge · 1.21.4, 1.21.3, 1.20.1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Filled-tonal download button (opens version dialog)
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

/** Collapsible filter panel with drag handle. Shows content-type chips. */
@Composable
private fun CollapsibleFilterPanel(
    contentType: ContentType,
    onContentTypeClick: (ContentType) -> Unit,
    installedContent: List<InstalledContent>,
    isInstalledTab: Boolean,
    collapsed: Boolean,
    onCollapseToggle: () -> Unit,
    onDrag: (Offset) -> Unit,
) {
    val allTypes = ContentType.values()
    val selectedType = contentType

    // When collapsed, show only selected type; when expanded, show all
    val visibleTypes = if (collapsed) listOf(selectedType) else allTypes

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
        Column(modifier = Modifier.padding(12.dp)) {
            // Content type chips row (horizontally scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (visibleTypes as List<ContentType>).forEach { type ->
                    val count = if (isInstalledTab) {
                        installedContent.count { it.type == type }
                    } else 0
                    ContentTypeChip(
                        type = type,
                        selected = selectedType == type,
                        count = count,
                        showBadge = isInstalledTab,
                        onClick = { onContentTypeClick(type) },
                    )
                }
            }

            // Drag handle at bottom center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(32.dp)
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount)
                            },
                            onDragEnd = { onCollapseToggle() },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.DragIndicator,
                    contentDescription = stringResource(if (collapsed) R.string.cd_expand else R.string.cd_collapse),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/** Full-width segmented button for provider selection (Modrinth | CurseForge). */
@Composable
private fun ProviderSegmentedControl(
    selectedProvider: Provider,
    onProviderChange: (Provider) -> Unit,
    context: android.content.Context,
    providerComingSoonStr: String,
) {
    val options = listOf(Provider.MODRINTH, Provider.CURSEFORGE)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        options.forEach { provider ->
            val isSelected = selectedProvider == provider
            val enabled = provider != Provider.CURSEFORGE
            val colors = FilterChipDefaults.filterChipColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                labelColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (provider == Provider.CURSEFORGE) {
                        Toast.makeText(context, providerComingSoonStr, Toast.LENGTH_SHORT).show()
                    } else {
                        onProviderChange(provider)
                    }
                },
                label = { Text(text = stringResource(provider.labelRes), style = MaterialTheme.typography.labelLarge) },
                colors = colors,
                modifier = Modifier
                    .weight(1f, fill = true)
                    .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
                enabled = enabled,
            )
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
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.manage_content_search_hint)) },
        singleLine = true,
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

/** Floating pagination pill at bottom: [collapse] [prev] [N/total] [next]. */
@Composable
private fun PaginationPill(
    currentPage: Int,
    totalPages: Int,
    loading: Boolean,
    collapsed: Boolean,
    onCollapseToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPageChange: () -> Unit, // for scroll to top
) {
    val pillHeight = 56.dp

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        if (collapsed) {
            // Collapsed: compact handle at screen edge (chevron right to expand)
            Box(
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .height(pillHeight)
                    .width(pillHeight)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onCollapseToggle) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = stringResource(R.string.cd_expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            // Expanded: full pill with controls
            Box(
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Collapse toggle (chevron left to shrink)
                    IconButton(onClick = onCollapseToggle) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = stringResource(R.string.cd_collapse),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp).padding(4.dp),
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = {
                            onPrev()
                            onPageChange()
                        },
                        enabled = currentPage > 1 && !loading,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = stringResource(R.string.cd_nav_previous),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (currentPage > 1 && !loading) 1f else 0.3f),
                            modifier = Modifier.size(24.dp).padding(4.dp),
                        )
                    }

                    // Page indicator
                    Text(
                        text = "$currentPage / $totalPages",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )

                    // Next
                    IconButton(
                        onClick = {
                            onNext()
                            onPageChange()
                        },
                        enabled = currentPage < totalPages && !loading,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                        contentDescription = stringResource(R.string.cd_nav_next),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (currentPage < totalPages && !loading) 1f else 0.3f),
                        modifier = Modifier.size(24.dp).padding(4.dp),
                    )
                }
            }
        }
    }
}

class ManageContentScreenImpl {
    @Composable
    fun Screen(
        client: ModrinthClient,
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

    // Collapse state remembered per tab
    var installedTypeCollapsed by remember { mutableStateOf(false) }
    var browseTypeCollapsed by remember { mutableStateOf(false) }
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

    // Auto-load browse content on initial load and when content type/page/query changes
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

    // Scroll to top when page changes
    LaunchedEffect(browsePage) {
        if (mainTab == 1 && !browseLoading) {
            lazyListState.animateScrollToItem(0)
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

        // Collapsible filter panel with content-type chips + drag handle
        val activeContentType = if (mainTab == 0) installedContentType else browseContentType
        val activeCollapsed = if (mainTab == 0) installedTypeCollapsed else browseTypeCollapsed
        val onCollapseToggle: () -> Unit = if (mainTab == 0) { { installedTypeCollapsed = !installedTypeCollapsed } } else { { browseTypeCollapsed = !browseTypeCollapsed } }
        val onTypeClick: (ContentType) -> Unit = if (mainTab == 0) { { installedContentType = it } } else { { browseContentType = it } }
        val onDrag = { dragAmount: Offset ->
            // Drag vertically to collapse/expand (threshold-based)
            if (dragAmount.y > 20 && !activeCollapsed) {
                onCollapseToggle()
            } else if (dragAmount.y < -20 && activeCollapsed) {
                onCollapseToggle()
            }
        }

        CollapsibleFilterPanel(
            contentType = activeContentType,
            onContentTypeClick = onTypeClick,
            installedContent = installedContent,
            isInstalledTab = mainTab == 0,
            collapsed = activeCollapsed,
            onCollapseToggle = onCollapseToggle,
            onDrag = onDrag,
        )

        // Search field (full-width, leading icon, clear button, keyboard search action)
        SearchField(
            query = browseQuery,
            onQueryChange = { browseQuery = it },
            onSearch = {
                if (mainTab == 1) {
                    scope.launch {
                        browseLoading = true
                        browseError = null
                        browsePage = 1 // Reset to first page on new search
                        val offset = 0
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
                }
            },
            onClear = { browseQuery = "" },
        )

        // Source selector (Browse only): full-width segmented control below search
        if (mainTab == 1) {
            ProviderSegmentedControl(
                selectedProvider = browseProvider,
                onProviderChange = { browseProvider = it },
                context = context,
                providerComingSoonStr = providerComingSoonStr,
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
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp), // Space for pagination pill (not shown on Installed)
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
                    val listPaddingBottom = if (browseTotalPages > 1) 100.dp else 80.dp // Extra space for pagination pill
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
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = listPaddingBottom),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(browseResults, key = { it.project_id }) { hit ->
                                BrowseContentRow(
                                    hit = hit,
                                    onClick = { /* open mod detail */ },
                                    onDownload = { /* open versions dialog */ },
                                )
                            }
                        }
                    }
                }
            }
            Unit // Explicit Unit to satisfy Box content lambda
        }

        // Floating pagination pill (Browse only, when multiple pages)
        if (mainTab == 1 && browseTotalPages > 1) {
            PaginationPill(
                currentPage = browsePage,
                totalPages = browseTotalPages,
                loading = browseLoading,
                collapsed = pageNavCollapsed,
                onCollapseToggle = { pageNavCollapsed = !pageNavCollapsed },
                onPrev = { if (browsePage > 1) browsePage-- },
                onNext = { if (browsePage < browseTotalPages) browsePage++ },
                onPageChange = { /* triggers LaunchedEffect to scroll to top */ },
            )
        }
    }
}
}
}
