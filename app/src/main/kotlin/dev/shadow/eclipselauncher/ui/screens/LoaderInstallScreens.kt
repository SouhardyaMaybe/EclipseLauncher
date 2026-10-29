/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shadow.eclipselauncher.core.model.LoaderKind
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoTheme
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import dev.shadow.eclipselauncher.ui.components.MojoDivider
import dev.shadow.eclipselauncher.ui.components.MojoFieldHeight
import dev.shadow.eclipselauncher.ui.components.MojoInlineError
import dev.shadow.eclipselauncher.ui.components.MojoMineButton
import dev.shadow.eclipselauncher.ui.components.MojoProgressBar
import kotlinx.coroutines.delay

/**
 * The three Fabric-like installer screens (`fragment_fabric_install.xml`).
 *
 * The reference loads game and loader versions from the loader's own metadata
 * service. This build has no loader client, so the screen ends at the
 * reference failure state: the two spinners stay empty, the retry row appears,
 * Install is disabled, and the progress bar disappears after the failed load.
 */
@Composable
fun FabricLoaderScreen(
    kind: LoaderKind,
) {
    val name = when (kind) {
        LoaderKind.QUILT -> "Quilt"
        LoaderKind.LEGACY_FABRIC -> "Legacy Fabric"
        else -> "Fabric"
    }
    var attempt by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(attempt) {
        loading = true
        delay(400)
        loading = false
    }

    MojoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background)
                .padding(horizontal = 10.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Select versions",
                modifier = Modifier.fillMaxWidth(),
                style = MojoType.title.copy(fontSize = 20.sp),
                color = MojoPalette.PrimaryText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            MojoDivider()
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Game version",
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
            )
            Spacer(Modifier.height(6.dp))
            EmptySpinner()

            Spacer(Modifier.height(24.dp))
            Text(
                text = "$name loader version",
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
            )
            Spacer(Modifier.height(6.dp))
            EmptySpinner()

            Spacer(Modifier.height(24.dp))
            Text(
                text = "Options",
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
            )
            Spacer(Modifier.height(10.dp))
            var stable by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = stable,
                        role = Role.Checkbox,
                        onValueChange = { stable = it },
                    )
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(2.dp, MojoPalette.Line),
                    contentAlignment = Alignment.Center,
                ) {
                    if (stable) {
                        EclipsePixelIcon(
                            kind = EclipsePixelIconKind.CHECK,
                            modifier = Modifier.size(14.dp),
                            tint = MojoPalette.Accent,
                        )
                    }
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    text = "Show only stable versions",
                    style = MojoType.body,
                    color = MojoPalette.PrimaryText,
                )
            }

            Spacer(Modifier.weight(1f))
            if (!loading) {
                MojoInlineError(
                    message = "Failed to load the version list!",
                    onRetry = { attempt++ },
                )
            }
            MojoMineButton(
                text = "Install",
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
            if (loading) {
                MojoProgressBar(modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

@Composable
private fun EmptySpinner() {
    // Reference spinner: `background_line` overrides the AppCompat arrow
    // background, so the field renders as a plain bordered row.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MojoFieldHeight())
            .background(MojoPalette.Input)
            .border(2.dp, MojoPalette.Line),
        verticalAlignment = Alignment.CenterVertically,
    ) {}
}

/**
 * `fragment_mod_version_list.xml` for Forge, NeoForge, OptiFine and BTA.
 *
 * Every one of these screens loads its list over the network. With no loader
 * or modpack client in this build the list is empty and the reference retry
 * row is shown; there is no Start button in this layout.
 */
@Composable
fun ModVersionLoaderScreen(
    kind: LoaderKind,
) {
    val title = when (kind) {
        LoaderKind.FORGE -> "Select Forge version"
        LoaderKind.NEOFORGE -> "Select NeoForge version"
        LoaderKind.OPTIFINE -> "Select OptiFine version"
        LoaderKind.BTA -> "Select \"Better than Adventure!\" version"
        else -> "Select version"
    }
    var attempt by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(attempt) {
        loading = true
        delay(400)
        loading = false
    }

    MojoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background)
                .padding(horizontal = 10.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = MojoType.title.copy(fontSize = 20.sp),
                color = MojoPalette.PrimaryText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            MojoDivider()

            Spacer(Modifier.weight(1f))
            if (!loading) {
                // Reference retry row from `fragment_mod_version_list.xml`: red
                // message at the start of the row, Retry button at the end.
                MojoInlineError(
                    message = "Failed to load the version list!",
                    onRetry = { attempt++ },
                )
            }

            if (loading) {
                MojoProgressBar(modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}
