/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.data.MojangMetadataClient
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class VersionGroup(
    val title: String,
    val values: List<String>,
)

/**
 * `VersionSelectorDialog` and `dialog_expendable_list_view.xml`.
 *
 * The reference dialog has no title, no buttons and a fixed 200sdp list. The
 * version table is loaded once by the launcher; if that load failed, the
 * reference shows its own Error dialog and this list stays empty. [onLoadError]
 * reports the same failure to the caller so it can present `MojoErrorDialog`.
 */
@Composable
fun MojoVersionSelectorDialog(
    versionsDir: File,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    client: MojangMetadataClient,
    hideCustomVersions: Boolean = false,
    onLoadError: (String) -> Unit = {},
) {
    var groups by remember { mutableStateOf<List<VersionGroup>>(emptyList()) }
    var expanded by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { client.fetchManifest() }
        when (result) {
            is AppResult.Success -> {
                val manifest = result.value
                val releases = manifest.versions.filter { it.type == "release" }.map { it.id }
                val snapshots = manifest.versions.filter { it.type == "snapshot" }.map { it.id }
                val betas = manifest.versions.filter { it.type == "old_beta" }.map { it.id }
                val alphas = manifest.versions.filter { it.type == "old_alpha" }.map { it.id }
                val installed = if (hideCustomVersions) {
                    emptyList()
                } else {
                    versionsDir.listFiles()
                        ?.filter { it.isDirectory }
                        ?.map { it.name }
                        ?.sorted()
                        .orEmpty()
                }
                groups = buildList {
                    if (installed.isNotEmpty()) add(VersionGroup("Installed", installed))
                    add(VersionGroup("Release", releases))
                    add(VersionGroup("Snapshot", snapshots))
                    add(VersionGroup("Old-beta", betas))
                    add(VersionGroup("Old-alpha", alphas))
                }
            }

            is AppResult.Error -> onLoadError(result.failure.userMessage)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(200.dp)
                .background(MojoPalette.StatusBar, RectangleShape),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                groups.forEach { group ->
                    item(key = "header:${group.title}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    expanded = if (group.title in expanded) {
                                        expanded - group.title
                                    } else {
                                        expanded + group.title
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = group.title,
                                modifier = Modifier.weight(1f),
                                style = MojoType.body.copy(fontWeight = FontWeight.Bold),
                                color = MojoPalette.PrimaryText,
                            )
                            EclipsePixelIcon(
                                kind = if (group.title in expanded) {
                                    EclipsePixelIconKind.CHEVRON_UP
                                } else {
                                    EclipsePixelIconKind.CHEVRON_DOWN
                                },
                                modifier = Modifier.size(14.dp),
                                tint = MojoPalette.PrimaryText,
                            )
                        }
                    }
                    if (group.title in expanded) {
                        items(group.values, key = { "${group.title}:$it" }) { value ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .clickable { onSelect(value) }
                                    .padding(start = 36.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text = value,
                                    style = MojoType.body,
                                    color = MojoPalette.PrimaryText,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
