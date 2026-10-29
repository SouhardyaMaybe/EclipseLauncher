/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.data.LauncherState
import dev.shadow.eclipselauncher.data.MojangMetadataClient
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoTheme
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import dev.shadow.eclipselauncher.ui.components.MojoButtonRow
import dev.shadow.eclipselauncher.ui.components.MojoConfirmDialog
import dev.shadow.eclipselauncher.ui.components.MojoDivider
import dev.shadow.eclipselauncher.ui.components.MojoErrorDialog
import dev.shadow.eclipselauncher.ui.components.MojoFileSelectorDialog
import dev.shadow.eclipselauncher.ui.components.MojoLabelledField
import dev.shadow.eclipselauncher.ui.components.MojoSelectField
import dev.shadow.eclipselauncher.ui.components.MojoSingleChoiceDialog
import dev.shadow.eclipselauncher.ui.components.MojoTextField
import dev.shadow.eclipselauncher.ui.components.MojoVersionSelectorDialog
import java.io.File

/**
 * `fragment_instance_editor.xml` and `InstanceEditorFragment`.
 *
 * Field order, hints, the shared-data switch copy, both Select buttons, the
 * Java Runtime and Renderer spinners, and Save/Delete match the reference.
 * The runtime list is the real one for this build: no runtimes are installed,
 * so the spinner contains only the reference `<Default>` entry. The renderer
 * list mirrors the reference display names; the value is stored on this
 * device and is not a promise that a game renderer exists.
 */
@Composable
fun InstanceEditorScreen(
    state: LauncherState,
    metadata: MojangMetadataClient,
    onSaved: (
        name: String,
        version: String,
        sharedData: Boolean,
        jvmArgs: String,
        renderer: String,
        runtime: String,
        controlmap: String,
    ) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val instance = state.instance

    var name by remember(instance?.id) { mutableStateOf(instance?.name.orEmpty()) }
    var version by remember(instance?.id) {
        mutableStateOf(instance?.minecraftVersion.orEmpty())
    }
    var controlmap by remember(instance?.id) {
        mutableStateOf(state.instanceControlmap)
    }
    var sharedData by remember(instance?.id) { mutableStateOf(state.instanceSharedData) }
    var jvmArgs by remember(instance?.id) { mutableStateOf(state.instanceJvmArgs) }
    var runtime by remember(instance?.id) {
        mutableStateOf(state.instanceRuntime.ifBlank { DEFAULT_RUNTIME })
    }
    var renderer by remember(instance?.id) {
        mutableStateOf(state.instanceRenderer)
    }

    var showVersions by remember { mutableStateOf(false) }
    var versionError by remember { mutableStateOf<String?>(null) }
    var showControlmaps by remember { mutableStateOf(false) }
    var showRuntimes by remember { mutableStateOf(false) }
    var showRenderers by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val rendererNames = remember {
        listOf(
            "GL4ES (OpenGL ES 2) - 1.21.4 and lower",
            "LTW (OpenGL ES 3) - 1.17+ only",
            "Zink (Vulkan) - all versions",
            "Freedreno (KGSL) - all versions",
            "Mesa (DRM) - all versions",
            "Mesa (DRM, external) - all versions",
            "Legacy Zink (Vulkan) - all versions, low performance",
        )
    }
    val rendererValues = remember {
        listOf(
            "opengles2",
            "opengles3_ltw",
            "vulkan_zink",
            "freedreno_kgsl",
            "mesa_desktop",
            "mesa_desktop_ext",
            "vulkan_legacyzink",
        )
    }

    MojoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(50.dp)
                        .background(MojoPalette.Input)
                        .border(1.dp, MojoPalette.Line),
                    contentAlignment = Alignment.Center,
                ) {
                    EclipsePixelIcon(
                        kind = EclipsePixelIconKind.WORLD,
                        modifier = Modifier.size(32.dp),
                        tint = MojoPalette.Accent,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .background(MojoPalette.StatusBar),
                        contentAlignment = Alignment.Center,
                    ) {
                        EclipsePixelIcon(
                            kind = EclipsePixelIconKind.EDIT,
                            modifier = Modifier.size(10.dp),
                            tint = MojoPalette.PrimaryText,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Name",
                    style = MojoType.body,
                    color = MojoPalette.PrimaryText,
                )
                Spacer(Modifier.height(2.dp))
                MojoTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Unnamed",
                )

                Spacer(Modifier.height(32.dp))
                MojoSelectField(
                    label = "Version",
                    value = versionLabel(version),
                    onClick = { showVersions = true },
                    hint = "Select a version",
                )

                Spacer(Modifier.height(32.dp))
                MojoSelectField(
                    label = "Default controlmap",
                    value = controlmap,
                    onClick = { showControlmaps = true },
                    hint = "(Use global default)",
                )

                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Shared data (click to switch)",
                    style = MojoType.body,
                    color = MojoPalette.PrimaryText,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, MojoPalette.Line)
                        .toggleable(
                            value = sharedData,
                            role = Role.Checkbox,
                            onValueChange = { sharedData = it },
                        )
                        // Reference: `button="@null"` — no check glyph, only the
                        // swapping text on the `background_line` row (11dp top,
                        // 12dp bottom padding).
                        .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (sharedData) {
                            "Store game data in a shared directory"
                        } else {
                            "Store game data in the instance directory"
                        },
                        modifier = Modifier.weight(1f),
                        style = MojoType.body,
                        color = MojoPalette.PrimaryText,
                    )
                }

                Spacer(Modifier.height(32.dp))
                MojoLabelledField(
                    label = "JVM arguments",
                    value = jvmArgs,
                    onValueChange = { jvmArgs = it },
                    placeholder = "(Use global default)",
                )

                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Java Runtime",
                    style = MojoType.body,
                    color = MojoPalette.PrimaryText,
                )
                Spacer(Modifier.height(2.dp))
                SpinnerField(
                    value = runtime,
                    onClick = { showRuntimes = true },
                )

                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Renderer",
                    style = MojoType.body,
                    color = MojoPalette.PrimaryText,
                )
                Spacer(Modifier.height(2.dp))
                SpinnerField(
                    value = rendererDisplay(renderer, rendererValues, rendererNames),
                    onClick = { showRenderers = true },
                )
                Spacer(Modifier.height(24.dp))
            }

            MojoDivider()
            MojoButtonRow(
                startLabel = "Save",
                onStart = {
                    onSaved(
                        name,
                        version,
                        sharedData,
                        jvmArgs,
                        renderer,
                        runtime,
                        controlmap,
                    )
                },
                endLabel = "Delete",
                onEnd = { showDelete = true },
                horizontalPadding = 10.dp,
            )
        }
    }

    if (showVersions) {
        MojoVersionSelectorDialog(
            versionsDir = File(state.minecraftDir, "versions"),
            client = metadata,
            onSelect = { id ->
                version = id
                showVersions = false
            },
            onDismiss = { showVersions = false },
            onLoadError = { message ->
                showVersions = false
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

    if (showControlmaps) {
        MojoFileSelectorDialog(
            root = state.controlmapDir,
            selectFolder = false,
            onSelect = { file ->
                controlmap = file.absolutePath.replace(
                    state.controlmapDir.absolutePath,
                    ".",
                )
                showControlmaps = false
            },
            onDismiss = { showControlmaps = false },
        )
    }

    if (showRuntimes) {
        MojoSingleChoiceDialog(
            title = "Java Runtime",
            options = listOf(DEFAULT_RUNTIME),
            selectedIndex = 0,
            onSelect = {
                runtime = DEFAULT_RUNTIME
                showRuntimes = false
            },
            onDismiss = { showRuntimes = false },
        )
    }

    if (showRenderers) {
        val options = rendererNames + "Default"
        val selected = if (renderer.isBlank()) {
            options.lastIndex
        } else {
            rendererValues.indexOf(renderer).takeIf { it >= 0 } ?: options.lastIndex
        }
        MojoSingleChoiceDialog(
            title = "Renderer",
            options = options,
            selectedIndex = selected,
            onSelect = { index ->
                renderer = if (index == options.lastIndex) "" else rendererValues[index]
                showRenderers = false
            },
            onDismiss = { showRenderers = false },
        )
    }

    if (showDelete) {
        MojoConfirmDialog(
            title = "Delete instance",
            message = "Are you sure you want to delete the instance? This will permanently delete all worlds, mods, resources and any other data in the instance directory. This action cannot be undone!",
            confirmLabel = "Delete",
            dismissLabel = "No",
            onConfirm = {
                showDelete = false
                onDeleted()
            },
            onDismiss = { showDelete = false },
        )
    }

    LaunchedEffect(instance) {
        if (instance == null) onBack()
    }
}

@Composable
private fun SpinnerField(value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, MojoPalette.Line)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MojoType.body,
            color = MojoPalette.PrimaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // Reference spinners carry no dropdown arrow: the `<Spinner>` overrides
        // the AppCompat arrow background with `background_line`.
    }
}

private fun versionLabel(value: String): String = when (value) {
    "latest_release" -> "Latest release"
    "latest_snapshot" -> "Latest snapshot"
    else -> value
}

private fun rendererDisplay(
    value: String,
    ids: List<String>,
    names: List<String>,
): String {
    if (value.isBlank()) return "Default"
    val index = ids.indexOf(value)
    return if (index >= 0) names[index] else "Default"
}

private const val DEFAULT_RUNTIME = "<Default>"
