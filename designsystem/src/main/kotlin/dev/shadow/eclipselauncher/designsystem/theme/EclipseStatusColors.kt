/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic state colors. These are separate from the Material roles because
 * "ready" and "broken" must keep their meaning even when a user opts into
 * dynamic color. Status is never carried by color alone; every use pairs it
 * with an icon and a label.
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

internal val LightStatusColors = EclipseStatusColors(
    ready = Color(0xFF2E6B4F),
    attention = Color(0xFF8A5A00),
    broken = Color(0xFFA33A2B),
    busy = Color(0xFF35618E),
    scrim = Color(0x66000000),
    gameOverlay = Color(0x47000000),
)

internal val DarkStatusColors = EclipseStatusColors(
    ready = Color(0xFF8FD5B0),
    attention = Color(0xFFF2C66D),
    broken = Color(0xFFFFB4A8),
    busy = Color(0xFFA8C8F0),
    scrim = Color(0x99000000),
    gameOverlay = Color(0x47000000),
)

val LocalEclipseStatusColors = staticCompositionLocalOf { LightStatusColors }
