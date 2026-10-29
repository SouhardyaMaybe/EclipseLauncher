/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shadow.eclipselauncher.data.MojangMetadataClient
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoTheme
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import dev.shadow.eclipselauncher.ui.components.MojoDialog
import dev.shadow.eclipselauncher.ui.components.MojoErrorDialog
import dev.shadow.eclipselauncher.ui.components.MojoFieldHeight
import dev.shadow.eclipselauncher.ui.components.MojoMineButton
import dev.shadow.eclipselauncher.ui.components.MojoProgressBar
import dev.shadow.eclipselauncher.ui.components.MojoVersionSelectorDialog
import kotlinx.coroutines.delay
import java.io.File

private enum class ModpackSearchState { LOADING, FAILED }

/**
 * `fragment_mod_search.xml` and `SearchModFragment`.
 *
 * The reference searches CurseForge on entry. This build has no modpack API,
 * so the screen goes straight to the real internal-error text
 * `Failed to find modpacks`; local import ends at the existing
 * `Error` + `Failed to download modpack metadata` dialog.
 */
@Composable
fun ModpackSearchScreen(
    metadata: MojangMetadataClient,
    versionsDir: File,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var version by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf(ModpackSearchState.LOADING) }
    var attempt by remember { mutableStateOf(0) }
    var filtersOpen by remember { mutableStateOf(false) }
    var versionPickerOpen by remember { mutableStateOf(false) }
    var versionError by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importError = true
    }

    LaunchedEffect(attempt) {
        state = ModpackSearchState.LOADING
        delay(400)
        state = ModpackSearchState.FAILED
    }

    MojoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background)
                .padding(horizontal = 10.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, top = 10.dp)
                        .background(MojoPalette.Overlay)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(MojoFieldHeight())
                            .background(MojoPalette.Input)
                            .border(2.dp, MojoPalette.Line)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search for modpacks",
                                style = MojoType.body.copy(fontSize = 13.sp),
                                color = MojoPalette.SecondaryText,
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MojoType.body.copy(
                                fontSize = 13.sp,
                                color = MojoPalette.PrimaryText,
                            ),
                            cursorBrush = SolidColor(MojoPalette.Accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { attempt++ },
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            // Reference: 38sdp wide, height matched to the search field.
                            .size(width = 38.dp, height = MojoFieldHeight())
                            .clickable { filtersOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        EclipsePixelIcon(
                            kind = EclipsePixelIconKind.FILTER,
                            modifier = Modifier.size(22.dp),
                            tint = MojoPalette.PrimaryText,
                        )
                    }
                }

                if (state == ModpackSearchState.LOADING) {
                    MojoProgressBar()
                }

                Spacer(Modifier.weight(1f))
                if (state == ModpackSearchState.FAILED) {
                    Text(
                        text = "Failed to find modpacks",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        style = MojoType.body,
                        color = MojoPalette.Error,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.weight(1f))

                MojoMineButton(
                    text = "Import local modpack",
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    enabled = state != ModpackSearchState.LOADING,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }

    if (filtersOpen) {
        AlertDialog(
            onDismissRequest = { filtersOpen = false },
            shape = RectangleShape,
            containerColor = MojoPalette.StatusBar,
            titleContentColor = MojoPalette.PrimaryText,
            textContentColor = MojoPalette.PrimaryText,
            confirmButton = {},
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Version",
                        style = MojoType.body,
                        color = MojoPalette.PrimaryText,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(MojoFieldHeight())
                                .background(MojoPalette.Input)
                                .border(2.dp, MojoPalette.Line)
                                .clickable { versionPickerOpen = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = version.ifEmpty { "Select a version" },
                                style = MojoType.body.copy(fontSize = 13.sp),
                                color = if (version.isEmpty()) {
                                    MojoPalette.SecondaryText
                                } else {
                                    MojoPalette.PrimaryText
                                },
                            )
                        }
                        Spacer(Modifier.size(4.dp))
                        MojoMineButton(
                            text = "Select",
                            onClick = { versionPickerOpen = true },
                            modifier = Modifier.size(
                                width = 88.dp,
                                height = MojoFieldHeight(),
                            ),
                            primary = false,
                            height = MojoFieldHeight(),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    MojoMineButton(
                        text = "Apply",
                        onClick = {
                            filtersOpen = false
                            attempt++
                        },
                    )
                }
            },
        )
    }

    if (versionPickerOpen) {
        MojoVersionSelectorDialog(
            versionsDir = versionsDir,
            client = metadata,
            onSelect = { selected ->
                version = selected
                versionPickerOpen = false
            },
            onDismiss = { versionPickerOpen = false },
            hideCustomVersions = true,
            onLoadError = { message ->
                versionPickerOpen = false
                versionError = message
            },
        )
    }

    versionError?.let { message ->
        MojoErrorDialog(
            message = message,
            onDismiss = { versionError = null },
        )
    }

    if (importError) {
        MojoDialog(
            onDismiss = { importError = false },
            confirmLabel = "OK",
            onConfirm = { importError = false },
            title = "Error",
            message = "Failed to download modpack metadata",
        )
    }
}
