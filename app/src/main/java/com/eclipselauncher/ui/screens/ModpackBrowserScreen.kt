/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * MODPACK BROWSER — the Create screen's Modpack destination.
 * Modrinth is live (project_type:modpack) so a pack can be searched and
 * opened; CurseForge still needs an API key the launcher does not have, so
 * it stays a labelled "coming soon" chip. Local .zip import sits in the top
 * bar. Picking a downloaded pack falls through to the existing import path
 * (the store decides the version when unpacking); this screen only finds it.
 *
 * Layout rule: ONE LazyColumn below the pinned controls — no list inside a
 * list, no list inside a scrolling Column.
 *
 * Filters: Loader / Version / Platform below search; each opens a modal dialog.
 * Version picker: tapping a modpack opens a full-screen dialog listing all
 * versions from Modrinth (like ManageContent's VersionInstallDialog).
 */
package com.eclipselauncher.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.eclipselauncher.R
import com.eclipselauncher.data.MetaResult
import com.eclipselauncher.data.ModrinthClient
import com.eclipselauncher.data.ModrinthHit
import com.eclipselauncher.data.ModrinthVersion
import com.eclipselauncher.data.ModrinthVersionFile
import com.eclipselauncher.data.VersionRepository
import com.eclipselauncher.ui.components.FilterOption
import com.eclipselauncher.ui.components.LoaderIcon
import com.eclipselauncher.ui.components.ModpackFilter
import com.eclipselauncher.ui.components.loaderDisplayName
import com.eclipselauncher.ui.components.loaderName
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

/** Compact filter header; tapping opens a modal chooser. */
@Composable
private fun FilterHeader(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** One radio row inside a filter dialog. */
@Composable
private fun FilterOptionRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    detail: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        detail?.let { detailText ->
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = detailText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Modal chooser for one filter; uses LazyColumn for large lists. */
@Composable
private fun FilterDialog(
    title: String,
    options: List<FilterOption>,
    selectedValue: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
            ) {
                item(key = "__any__") {
                    FilterOptionRow(
                        label = stringResource(R.string.manage_content_filter_any),
                        selected = selectedValue == null,
                        enabled = true,
                        detail = null,
                        onClick = { onSelect(null) },
                    )
                }
                items(options, key = { it.value ?: it.label }) { option ->
                    FilterOptionRow(
                        label = option.label,
                        selected = option.value == selectedValue,
                        enabled = option.enabled,
                        detail = option.detail,
                        onClick = { onSelect(option.value) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

/** Full-screen version chooser for one Modrinth modpack project. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModpackVersionDialog(
    projectId: String,
    projectSlug: String,
    projectTitle: String,
    projectAuthor: String,
    projectIconUrl: String,
    client: ModrinthClient,
    onDismiss: () -> Unit,
    onInstall: (ModrinthVersion, ModrinthVersionFile) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var versions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableStateOf(0) }
    var stableOnly by remember { mutableStateOf(true) }
    var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var installError by remember { mutableStateOf<String?>(null) }
    val busy = downloadingId != null

    LaunchedEffect(projectId, projectSlug, reloadToken) {
        loading = true
        failed = false
        expandedGroups = emptySet()
        val result = client.projectVersions(projectId.ifBlank { projectSlug })
        versions = result.versions
        failed = result.failed
        loading = false
    }

    // Group by game_versions, newest first
    val groups = remember(versions) {
        val map = mutableMapOf<String, MutableList<ModrinthVersion>>()
        for (v in versions) {
            if (v.game_versions.isNotEmpty()) {
                for (gv in v.game_versions) {
                    map.getOrPut(gv) { mutableListOf() }.add(v)
                }
            } else {
                map.getOrPut("any") { mutableListOf() }.add(v)
            }
        }
        map.values.forEach { it.sortByDescending { it.date_published } }
        map.toList().sortedByDescending { it.first }
    }

    val visibleGroups = remember(groups, stableOnly) {
        if (stableOnly) groups.filter { it.first != "any" } else groups
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
                                text = projectTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = projectAuthor,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )

                if (loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }
                } else if (failed) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.market_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = { reloadToken++ }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Stable only toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.end,
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = stableOnly,
                                onCheckedChange = { stableOnly = it },
                            )
                            Text(
                                text = stringResource(R.string.manage_content_stable_only),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(visibleGroups, key = { it.first }) { (mcVer, versions) ->
                                VersionGroupCard(
                                    mcVersion = mcVer,
                                    versions = versions,
                                    expanded = expandedGroups.contains(mcVer),
                                    onToggle = {
                                        if (expandedGroups.contains(mcVer)) {
                                            expandedGroups = expandedGroups - mcVer
                                        } else {
                                            expandedGroups = expandedGroups + mcVer
                                        }
                                    },
                                    onInstall = { version, file ->
                                        onInstall(version, file)
                                    },
                                    downloadingId = downloadingId,
                                    downloadProgress = downloadProgress,
                                    projectIconUrl = projectIconUrl,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One expanded group card for a Minecraft version. */
@Composable
private fun VersionGroupCard(
    mcVersion: String,
    versions: List<ModrinthVersion>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onInstall: (ModrinthVersion, ModrinthVersionFile) -> Unit,
    downloadingId: String?,
    downloadProgress: Float,
    projectIconUrl: String,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) -180f else 0f,
        animationSpec = tween(350),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(all = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mcVersion,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(if (expanded) R.string.cd_collapse else R.string.cd_expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer { rotationZ = rotation },
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(350)) + fadeIn(tween(350)),
                exit = shrinkVertically(tween(350)) + fadeOut(tween(350)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .padding(start = 4.dp, top = 0.dp, end = 4.dp, bottom = 8.dp),
                ) {
                    items(versions, key = { it.id }) { version ->
                        VersionRow(
                            version = version,
                            projectIconUrl = projectIconUrl,
                            downloadingId = downloadingId,
                            downloadProgress = downloadProgress,
                            onInstall = { file -> onInstall(version, file) },
                        )
                    }
                }
            }
        }
    }
}

/** One version row inside a group card. */
@Composable
private fun VersionRow(
    version: ModrinthVersion,
    projectIconUrl: String,
    downloadingId: String?,
    downloadProgress: Float,
    onInstall: (ModrinthVersionFile) -> Unit,
) {
    val isDownloading = downloadingId == version.id
    val file = version.files.firstOrNull { it.primary } ?: version.files.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (projectIconUrl.isNotBlank()) {
                    AsyncImage(
                        model = projectIconUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = version.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = version.version_number,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                )
                if (version.game_versions.isNotEmpty()) {
                    Text(
                        text = version.game_versions.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }

            if (isDownloading) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = downloadProgress,
                    modifier = Modifier.width(80.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                )
            } else {
                Button(
                    onClick = { file?.let(onInstall) },
                    enabled = file != null,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.manage_content_install),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun ModpackBrowserScreen(
    client: ModrinthClient,
    versionRepository: VersionRepository,
    onBack: () -> Unit,
    onPickModpack: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf(0) } // 0 = Modrinth
    var results by remember { mutableStateOf<List<ModrinthHit>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var reloadToken by remember { mutableStateOf(0) }

    // Filter state
    var selectedLoader by remember { mutableStateOf<String?>(null) }
    var selectedGameVersion by remember { mutableStateOf<String?>(null) }
    var filterDialog by remember { mutableStateOf<ModpackFilter?>(null) }

    // Version picker dialog
    var versionDialogTarget by remember { mutableStateOf<ModrinthHit?>(null) }

    // Minecraft versions for version filter
    var mcVersions by remember { mutableStateOf(versionRepository.getCachedVersions()) }
    LaunchedEffect(versionRepository) {
        when (val result = versionRepository.minecraftVersions()) {
            is MetaResult.Success -> mcVersions = result.value
            is MetaResult.Error -> Unit
        }
    }

    val networkError = stringResource(R.string.market_error)
    val curseforgeSoon = stringResource(R.string.market_coming_soon)

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let(onPickModpack)
    }

    fun runSearch(term: String, pageNum: Int = 1) {
        loading = true
        error = null
        page = pageNum
        scope.launch {
            val loader = selectedLoader.takeIf { it.isNotBlank() }
            val gameVersion = selectedGameVersion.takeIf { it.isNotBlank() }
            val result = client.searchMods(
                query = term.trim(),
                limit = 25,
                offset = (pageNum - 1) * 25,
                projectType = "modpack",
                loader = loader,
                gameVersion = gameVersion,
            )
            if (result.hits.isNotEmpty()) {
                results = result.hits
                totalPages = result.totalPages.coerceAtLeast(1)
            } else {
                results = emptyList()
                totalPages = 1
            }
            if (result.failed) error = networkError
            loading = false
        }
    }

    LaunchedEffect(Unit) { runSearch("") }

    // Reload when filters change
    LaunchedEffect(selectedLoader, selectedGameVersion) {
        runSearch(query, 1)
    }

    // Filter options
    val anyLabel = stringResource(R.string.manage_content_filter_any)
    val loaderFilterTitle = stringResource(R.string.manage_content_filter_loader)
    val versionFilterTitle = stringResource(R.string.manage_content_filter_version)
    val platformFilterTitle = stringResource(R.string.manage_content_filter_platform)

    val loaderOptions = remember {
        listOf(
            "fabric", "forge", "neoforge", "quilt"
        ).map { FilterOption(it, loaderDisplayName(it)) }
    }
    val versionOptions = remember(mcVersions) {
        mcVersions.map { mc ->
            FilterOption(mc.id, mc.id, detail = mc.type?.takeIf { it != "release" })
        }
    }
    val platformOptions = listOf(
        FilterOption("MODRINTH", stringResource(R.string.provider_modrinth)),
        FilterOption("CURSEFORGE", stringResource(R.string.provider_curseforge), enabled = false, detail = curseforgeSoon),
    )

    val loaderLabel = selectedLoader?.let(::loaderDisplayName) ?: anyLabel
    val versionLabel = selectedGameVersion ?: anyLabel
    val platformLabel = when (selectedProvider) {
        0 -> stringResource(R.string.provider_modrinth)
        else -> stringResource(R.string.provider_curseforge)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.modpack_browser_title)) },
            navigationIcon = {
                androidx.compose.material3.IconButton(onClick = onBack) {
                    Icon(
                        ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
            },
            actions = {
                androidx.compose.material3.IconButton(
                    onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = stringResource(R.string.modpack_tab_import),
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

        // Search — pinned control.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.modpack_search_hint)) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(R.string.cd_search),
                    )
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { runSearch(query) }) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = stringResource(R.string.action_search),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        // Filter headers: Loader / Version / Platform
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterHeader(
                title = loaderFilterTitle,
                value = loaderLabel,
                onClick = { filterDialog = ModpackFilter.LOADER },
                modifier = Modifier.weight(1f),
            )
            FilterHeader(
                title = versionFilterTitle,
                value = versionLabel,
                onClick = { filterDialog = ModpackFilter.VERSION },
                modifier = Modifier.weight(1f),
            )
            FilterHeader(
                title = platformFilterTitle,
                value = platformLabel,
                onClick = { filterDialog = ModpackFilter.PLATFORM },
                modifier = Modifier.weight(1f),
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading && results.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }

                error != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = requireNotNull(error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                results.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.modpack_no_results),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(results, key = { it.project_id }) { hit ->
                            ModpackRow(
                                hit = hit,
                                onClick = { versionDialogTarget = hit },
                            )
                        }
                    }

                    // Pagination
                    if (totalPages > 1) {
                        PaginationPill(
                            currentPage = page,
                            totalPages = totalPages,
                            loading = loading,
                            onPrev = {
                                if (!loading && page > 1) {
                                    runSearch(query, page - 1)
                                }
                            },
                            onNext = {
                                if (!loading && page < totalPages) {
                                    runSearch(query, page + 1)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    // Filter dialogs (Browse-only)
    when (filterDialog) {
        ModpackFilter.LOADER -> FilterDialog(
            title = loaderFilterTitle,
            options = loaderOptions,
            selectedValue = selectedLoader,
            onSelect = { value ->
                selectedLoader = value
                filterDialog = null
            },
            onDismiss = { filterDialog = null },
        )

        ModpackFilter.VERSION -> FilterDialog(
            title = versionFilterTitle,
            options = versionOptions,
            selectedValue = selectedGameVersion,
            onSelect = { value ->
                selectedGameVersion = value
                filterDialog = null
            },
            onDismiss = { filterDialog = null },
        )

        ModpackFilter.PLATFORM -> FilterDialog(
            title = platformFilterTitle,
            options = platformOptions,
            selectedValue = when (selectedProvider) { 0 -> "MODRINTH"; else -> "CURSEFORGE" },
            onSelect = { value ->
                value?.let { name ->
                    if (name == "MODRINTH") selectedProvider = 0
                    else if (name == "CURSEFORGE") selectedProvider = 1
                }
                filterDialog = null
            },
            onDismiss = { filterDialog = null },
        )

        null -> Unit
    }

    // Version picker dialog
    versionDialogTarget?.let { hit ->
        ModpackVersionDialog(
            projectId = hit.project_id,
            projectSlug = hit.slug,
            projectTitle = hit.title,
            projectAuthor = hit.author,
            projectIconUrl = hit.icon_url,
            client = client,
            onDismiss = { versionDialogTarget = null },
            onInstall = { version, file ->
                // TODO: Download and import modpack
                Toast.makeText(context, "Install modpack: ${version.name}", Toast.LENGTH_SHORT).show()
                versionDialogTarget = null
            },
        )
    }
}

@Composable
private fun ModpackRow(
    hit: ModrinthHit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModpackIcon(url = hit.icon_url, size = 56.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hit.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = hit.author + " · " + formatModpackDownloads(hit.downloads),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (hit.description.isNotBlank()) {
                    Text(
                        text = hit.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                if (hit.categories.isNotEmpty() || hit.versions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = (
                            hit.categories.take(3) + hit.versions.take(3)
                            ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.modpack_use_button),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** Loads the project icon through Coil; the tile shows until it arrives. */
@Composable
private fun ModpackIcon(url: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size.value * 0.2f))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudDownload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size((size.value * 0.5f).dp),
        )
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
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
    val indicator = "$currentPage / $totalPages"
    val indicatorWidth = (indicator.length * 8 + 16).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrev,
                    enabled = !loading && currentPage > 1,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = stringResource(R.string.cd_prev),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = indicator,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .width(indicatorWidth)
                        .padding(top = 2.dp),
                )
                IconButton(
                    onClick = onNext,
                    enabled = !loading && currentPage < totalPages,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = stringResource(R.string.cd_next),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .size(24.dp),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

/** Human-readable download count. */
private fun formatModpackDownloads(count: Long): String =
    when {
        count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0)
        count >= 1_000 -> "%.1fK".format(count / 1_000.0)
        else -> count.toString()
    }
