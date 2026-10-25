/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.designsystem.component.EclipseEmptyState
import dev.shadow.eclipselauncher.designsystem.component.EclipseTopBar
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette

@Composable
fun ControlsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        EclipseTopBar(
            title = stringResource(R.string.controls_title),
            onBack = onBack,
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            EclipseEmptyState(
                title = stringResource(R.string.controls_empty_title),
                body = stringResource(R.string.controls_empty_body),
            )
        }
    }
}
