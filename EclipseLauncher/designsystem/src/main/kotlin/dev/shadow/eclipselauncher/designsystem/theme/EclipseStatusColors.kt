/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic state colors. Every use pairs color with a label or icon; color is
 * never the only signal.
 */
@Immutable
data class EclipseStatusColors(
    val ready: Color,
    val attention: Color,
    val broken: Color,
    val busy: Color,
    val scrim: Color,
    val gameOverlay: Color,
)

internal val EclipseStatus = EclipseStatusColors(
    ready = EclipsePalette.Success,
    attention = EclipsePalette.Warning,
    broken = EclipsePalette.Danger,
    busy = EclipsePalette.Link,
    scrim = Color(0x99000000),
    gameOverlay = Color(0x47000000),
)

val LocalEclipseStatusColors = staticCompositionLocalOf { EclipseStatus }
