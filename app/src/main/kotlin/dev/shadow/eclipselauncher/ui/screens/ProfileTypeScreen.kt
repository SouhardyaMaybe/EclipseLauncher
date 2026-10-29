/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.core.model.LoaderKind
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoTheme
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import dev.shadow.eclipselauncher.ui.components.MojoDivider
import dev.shadow.eclipselauncher.ui.components.MojoMineButton

/**
 * `fragment_profile_type.xml`: two centred category titles, a divider under
 * each one, and the same full-width MineButton profile choices in order.
 */
@Composable
fun ProfileTypeScreen(
    onPick: (LoaderKind) -> Unit,
) {
    MojoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
        ) {
            ProfileHeading("Vanilla-like versions", topPadding = 16.dp)
            ProfileButton("Create vanilla instance") {
                onPick(LoaderKind.VANILLA)
            }
            ProfileButton("Install OptiFine") {
                onPick(LoaderKind.OPTIFINE)
            }

            ProfileHeading("Modded versions", topPadding = 32.dp)
            ProfileButton("Create Fabric instance") {
                onPick(LoaderKind.FABRIC)
            }
            ProfileButton("Create Quilt instance") {
                onPick(LoaderKind.QUILT)
            }
            ProfileButton("Create Legacy Fabric instance") {
                onPick(LoaderKind.LEGACY_FABRIC)
            }
            ProfileButton("Create Forge instance") {
                onPick(LoaderKind.FORGE)
            }
            ProfileButton("Create NeoForge instance") {
                onPick(LoaderKind.NEOFORGE)
            }
            ProfileButton("Create modpack instance") {
                onPick(LoaderKind.MODPACK)
            }
            ProfileButton("Create \"Better than Adventure!\" instance") {
                onPick(LoaderKind.BTA)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileHeading(text: String, topPadding: Dp) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding),
        style = MojoType.title,
        color = MojoPalette.PrimaryText,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    MojoDivider()
}

@Composable
private fun ProfileButton(text: String, onClick: () -> Unit) {
    Spacer(Modifier.height(16.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MojoMineButton(text = text, onClick = onClick)
    }
}
