/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.core.model.LoaderKind
import dev.shadow.eclipselauncher.designsystem.component.EclipseBevelButton
import dev.shadow.eclipselauncher.designsystem.component.EclipseCenteredHeading
import dev.shadow.eclipselauncher.designsystem.component.EclipseTopBar
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette

/**
 * The loader picker. It follows the reference layout literally: a centered
 * section title, a thick divider, and full-width buttons with generous space.
 * There are no cards, chips, badges or explanatory rows.
 */
@Composable
fun CreateInstanceScreen(
    onBack: () -> Unit,
    onPick: (LoaderKind) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        EclipseTopBar(
            title = stringResource(R.string.create_instance_title),
            backContentDescription = stringResource(R.string.action_back),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            EclipseCenteredHeading(
                text = stringResource(R.string.create_instance_vanilla_like),
                topPadding = 32.dp,
            )
            LoaderButton(
                label = stringResource(R.string.create_instance_vanilla),
                onClick = { onPick(LoaderKind.VANILLA) },
            )
            LoaderButton(
                label = stringResource(R.string.create_instance_optifine),
                onClick = { onPick(LoaderKind.OPTIFINE) },
            )

            Spacer(Modifier.height(24.dp))
            EclipseCenteredHeading(
                text = stringResource(R.string.create_instance_modded),
                topPadding = 32.dp,
            )
            LoaderButton(
                label = stringResource(R.string.create_instance_fabric),
                onClick = { onPick(LoaderKind.FABRIC) },
            )
            LoaderButton(
                label = stringResource(R.string.create_instance_quilt),
                onClick = { onPick(LoaderKind.QUILT) },
            )
            LoaderButton(
                label = stringResource(R.string.create_instance_forge),
                onClick = { onPick(LoaderKind.FORGE) },
            )
            LoaderButton(
                label = stringResource(R.string.create_instance_neoforge),
                onClick = { onPick(LoaderKind.NEOFORGE) },
            )
            LoaderButton(
                label = stringResource(R.string.create_instance_modpack),
                onClick = { onPick(LoaderKind.MODPACK) },
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoaderButton(
    label: String,
    onClick: () -> Unit,
) {
    EclipseBevelButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        primary = false,
        height = 56.dp,
    )
}
