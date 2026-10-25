/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.designsystem.component.EclipseDivider
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.component.EclipseSectionLabel
import dev.shadow.eclipselauncher.designsystem.component.EclipseValueRow
import dev.shadow.eclipselauncher.designsystem.component.eclipseFocusRing
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette

/**
 * The controls editor shell. It shows the full-screen layout and the
 * right-hand drawer from the reference launcher, but keeps every control
 * inert until the game process exists. No fake editing or fake saving.
 */
@Composable
fun ControlsScreen(onBack: () -> Unit) {
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val previewDescription = stringResource(R.string.controls_preview)
    val drawerDescription = stringResource(R.string.controls_open_drawer)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        ControlPreview(
            description = previewDescription,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .size(48.dp)
                .eclipseFocusRing()
                .clickable(
                    role = Role.Button,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            EclipsePixelIcon(
                kind = EclipsePixelIconKind.ARROW_LEFT,
                modifier = Modifier.size(20.dp),
                description = stringResource(R.string.action_back),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 48.dp)
                    .eclipseFocusRing()
                    .clickable(
                        role = Role.Button,
                        onClick = { drawerOpen = true },
                    )
                    .semantics {
                        contentDescription = drawerDescription
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .width(40.dp)
                        .height(20.dp)
                        .background(EclipsePalette.Control)
                        .border(1.dp, EclipsePalette.Border),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 1.dp)
                                .width(20.dp)
                                .height(2.dp)
                                .background(EclipsePalette.Muted),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.controls_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                style = MaterialTheme.typography.labelSmall,
                color = EclipsePalette.Muted,
                textAlign = TextAlign.Center,
            )
        }

        if (drawerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .pointerInput(Unit) {
                        detectTapGestures { drawerOpen = false }
                    },
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(320.dp)
                    .background(EclipsePalette.Panel)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.controls_drawer_title),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                        color = EclipsePalette.Text,
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .eclipseFocusRing()
                            .clickable(
                                role = Role.Button,
                                onClick = { drawerOpen = false },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        EclipsePixelIcon(
                            kind = EclipsePixelIconKind.CLOSE,
                            modifier = Modifier.size(18.dp),
                            description = stringResource(R.string.action_close),
                        )
                    }
                }
                EclipseDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EclipseSectionLabel(stringResource(R.string.controls_layout))
                    EclipseValueRow(
                        label = stringResource(R.string.controls_active_layout),
                        value = stringResource(R.string.controls_default),
                    )
                    EclipseDivider()

                    EclipseSectionLabel(stringResource(R.string.controls_buttons))
                    ControlGroupRow(stringResource(R.string.controls_movement))
                    EclipseDivider()
                    ControlGroupRow(stringResource(R.string.controls_actions))
                    EclipseDivider()
                    ControlGroupRow(stringResource(R.string.controls_interface))
                    EclipseDivider()
                    ControlGroupRow(stringResource(R.string.controls_mouse))
                    EclipseDivider()

                    Text(
                        text = stringResource(R.string.controls_empty_body),
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = EclipsePalette.Muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlGroupRow(label: String) {
    EclipseValueRow(
        label = label,
        value = stringResource(R.string.controls_preview_only),
    )
}

@Composable
private fun ControlPreview(
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 96.dp)
            .semantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .alpha(0.65f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PreviewButton(stringResource(R.string.action_up), Modifier.size(44.dp))
            Row {
                PreviewButton(stringResource(R.string.controls_left), Modifier.size(44.dp))
                Spacer(Modifier.size(4.dp))
                PreviewButton(stringResource(R.string.controls_right), Modifier.size(44.dp))
            }
            PreviewButton(stringResource(R.string.controls_down), Modifier.size(44.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .alpha(0.65f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewButton(stringResource(R.string.controls_inventory), Modifier.size(48.dp))
                PreviewButton(stringResource(R.string.controls_chat), Modifier.size(48.dp))
            }
            PreviewButton(stringResource(R.string.controls_jump), Modifier.size(64.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .alpha(0.65f),
        ) {
            PreviewButton(stringResource(R.string.controls_mouse_button), Modifier.size(width = 96.dp, height = 44.dp))
        }
    }
}

@Composable
private fun PreviewButton(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(EclipsePalette.Control.copy(alpha = 0.8f))
            .border(1.dp, EclipsePalette.Border),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = EclipsePalette.Text,
            textAlign = TextAlign.Center,
        )
    }
}
