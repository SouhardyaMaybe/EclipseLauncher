/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * UTILS: a hub of launcher tools, each opening as an internal page — the
 * mod market, a file manager over the app's private storage, a content
 * manager over the media library, the log viewer, executing a .jar (honest
 * stop: no runtime is wired yet) and a deep link into the CONTROL settings
 * page for editing controls.
 */
package com.eclipselauncher.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Javascript
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.eclipselauncher.R
import com.eclipselauncher.data.Logs
import com.eclipselauncher.data.MediaStorage
import com.eclipselauncher.ui.components.PreferenceRow
import com.eclipselauncher.ui.components.SectionHeader

/** Pages inside the UTILS hub. */
private enum class UtilsPage { MARKET, FILES, CONTENT, LOGS }

@Composable
fun UtilsScreen(
    logs: Logs,
    media: MediaStorage,
    onBack: () -> Unit,
    onEditControls: () -> Unit,
) {
    var page by remember { mutableStateOf<UtilsPage?>(null) }
    val context = LocalContext.current
    val notWired = stringResource(R.string.utils_jar_not_wired)

    // Hardware/gesture back steps out of a sub-page first, then leaves UTILS.
    androidx.activity.compose.BackHandler(enabled = page != null) { page = null }

    // System picker for a .jar to execute. No runtime exists yet, so the
    // result stops at an honest toast (and a log line).
    val jarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            logs.log("utils", "picked jar to execute: $uri")
            Toast.makeText(context, notWired, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        when (page) {
            null -> {
                // Back to HOME, then the hub entries.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBack)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                UtilsRoot(
                    onOpenMarket = { page = UtilsPage.MARKET },
                    onOpenFiles = { page = UtilsPage.FILES },
                    onOpenContent = { page = UtilsPage.CONTENT },
                    onOpenLogs = { page = UtilsPage.LOGS },
                    onExecuteJar = { jarLauncher.launch(arrayOf("application/java-archive", "*/*")) },
                    onEditControls = onEditControls,
                )
            }

            UtilsPage.MARKET -> UtilsPageShell(
                title = stringResource(R.string.market_title),
                onBack = { page = null },
            ) {
                MarketPage()
            }

            UtilsPage.FILES -> UtilsPageShell(
                title = stringResource(R.string.utils_file_manager),
                onBack = { page = null },
            ) {
                FileBrowserPage(root = context.filesDir, logs = logs)
            }

            UtilsPage.CONTENT -> UtilsPageShell(
                title = stringResource(R.string.utils_manage_content),
                onBack = { page = null },
            ) {
                FileBrowserPage(root = media.rootDir(), logs = logs)
            }

            UtilsPage.LOGS -> UtilsPageShell(
                title = stringResource(R.string.utils_manage_logs),
                onBack = { page = null },
            ) {
                LogsPage(logs = logs)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun UtilsRoot(
    onOpenMarket: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenContent: () -> Unit,
    onOpenLogs: () -> Unit,
    onExecuteJar: () -> Unit,
    onEditControls: () -> Unit,
) {
    SectionHeader(text = stringResource(R.string.utils_content_header))
    PreferenceRow(
        icon = Icons.Outlined.Map,
        title = stringResource(R.string.market_title),
        summary = stringResource(R.string.utils_market_summary),
        onClick = onOpenMarket,
    )
    PreferenceRow(
        icon = Icons.Outlined.Folder,
        title = stringResource(R.string.utils_file_manager),
        summary = stringResource(R.string.utils_file_manager_summary),
        onClick = onOpenFiles,
    )
    PreferenceRow(
        icon = Icons.Outlined.Inventory2,
        title = stringResource(R.string.utils_manage_content),
        summary = stringResource(R.string.utils_manage_content_summary),
        onClick = onOpenContent,
    )
    PreferenceRow(
        icon = Icons.Outlined.Article,
        title = stringResource(R.string.utils_manage_logs),
        summary = stringResource(R.string.utils_manage_logs_summary),
        onClick = onOpenLogs,
    )
    PreferenceRow(
        icon = Icons.Outlined.Javascript,
        title = stringResource(R.string.utils_execute_jar),
        summary = stringResource(R.string.utils_execute_jar_summary),
        onClick = onExecuteJar,
    )
    PreferenceRow(
        icon = Icons.Outlined.Keyboard,
        title = stringResource(R.string.utils_edit_controls),
        summary = stringResource(R.string.utils_edit_controls_summary),
        onClick = onEditControls,
    )
}

/** Shared chrome for a UTILS sub-page: title, in-hub back, scrolling body. */
@Composable
private fun UtilsPageShell(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
    content()
}

/**
 * A file browser over a private-storage directory: enter folders, see file
 * sizes, delete entries. IO happens on Dispatchers.IO; only names and sizes
 * cross into composition.
 */
@Composable
private fun FileBrowserPage(
    root: File,
    logs: Logs,
) {
    var current by remember { mutableStateOf(root) }
    var entries by remember { mutableStateOf(emptyList<Entry>()) }
    var pendingDelete by remember { mutableStateOf<Entry?>(null) }
    val scope = rememberCoroutineScope()

    // Refresh whenever the directory (or its contents, after a delete) changes.
    LaunchedEffect(current, entries.size) {
        entries = withContext(Dispatchers.IO) {
            val children = current.listFiles().orEmpty()
            children
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .map { file ->
                    Entry(
                        name = file.name,
                        isDirectory = file.isDirectory,
                        sizeBytes = if (file.isFile) file.length() else 0L,
                        file = file,
                    )
                }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
    ) {
        // Breadcrumb: current path relative to the root, tappable segments.
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable(enabled = current != root) { current = current.parentFile ?: root }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "/ " + relativePath(root, current),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        if (entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.utils_files_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(entries, key = { it.name }) { entry ->
            FileRow(
                entry = entry,
                onOpen = { if (entry.isDirectory) current = entry.file },
                onDelete = { pendingDelete = entry },
            )
        }
    }

    pendingDelete?.let { entry ->
        Dialog(onDismissRequest = { pendingDelete = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.utils_delete_title, entry.name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.utils_delete_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = { pendingDelete = null }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        androidx.compose.material3.TextButton(onClick = {
                            val target = entry.file
                            scope.launch(Dispatchers.IO) {
                                runCatching { target.deleteRecursively() }
                            }
                            logs.log("utils", "deleted ${entry.file.absolutePath}")
                            pendingDelete = null
                        }) {
                            Text(
                                text = stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Entry(
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val file: File,
)

@Composable
private fun FileRow(
    entry: Entry,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(enabled = entry.isDirectory, onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (entry.isDirectory) Icons.Outlined.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            tint = if (entry.isDirectory) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (!entry.isDirectory) {
                Text(
                    text = formatFileSize(entry.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Lists daily log files with a viewer dialog and a clear action. */
@Composable
private fun LogsPage(logs: Logs) {
    var files by remember { mutableStateOf(emptyList<File>()) }
    var viewing by remember { mutableStateOf<File?>(null) }
    var viewText by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        files = withContext(Dispatchers.IO) { logs.files() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.utils_logs_count, files.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.material3.TextButton(onClick = {
                    logs.clear()
                    refresh++
                }) {
                    Text(
                        text = stringResource(R.string.utils_logs_clear),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        if (files.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.utils_logs_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        items(files, key = { it.name }) { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable {
                        viewing = file
                        viewText = logs.read(file)
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    viewing?.let { file ->
        Dialog(onDismissRequest = { viewing = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                    ) {
                        item {
                            Text(
                                text = viewText.ifEmpty { " " },
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.TextButton(onClick = { viewing = null }) {
                            Text(stringResource(R.string.action_close))
                        }
                    }
                }
            }
        }
    }
}

/** "a/b/c" relative path display; empty string for the root itself. */
private fun relativePath(root: File, current: File): String {
    val rootPath = root.absolutePath.trimEnd('/')
    val currentPath = current.absolutePath.trimEnd('/')
    return if (currentPath.length <= rootPath.length) "" else currentPath.removePrefix(rootPath).trimStart('/')
}

/** Human-readable file size: B / KB / MB / GB. */
internal fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> String.format("%.2f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> String.format("%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
