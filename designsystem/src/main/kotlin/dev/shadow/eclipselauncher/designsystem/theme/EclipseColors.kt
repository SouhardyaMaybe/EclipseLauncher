/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Ore-inspired palette. The launcher is dark-only in v1; a light theme would
 * change the contrast of every status surface for no product gain.
 *
 * These are original token names and values. No game textures are bundled.
 */
object EclipsePalette {
    val Canvas = Color(0xFF1C1D1D)
    val Panel = Color(0xFF2A2B2C)
    val Border = Color(0xFF39393B)
    val Control = Color(0xFF39393B)
    val ControlHover = Color(0xFF47484A)
    val ControlPressed = Color(0xFF2A2B2C)

    val Primary = Color(0xFF3B8526)
    val PrimaryHover = Color(0xFF4B9736)
    val PrimaryPressed = Color(0xFF2F6B1E)
    val PrimaryTop = Color(0xFF5AAE43)
    val PrimaryBottom = Color(0xFF1F4A12)

    val Text = Color(0xFFFFFFFF)
    val Muted = Color(0xFFB0B0B0)
    val Disabled = Color(0xFF737476)
    val Focus = Color(0xFF9CD78C)
    val Danger = Color(0xFFFF8A80)
    val DangerFill = Color(0xFFC93636)
    val Warning = Color(0xFFFDDC69)
    val Success = Color(0xFF9CD78C)
    val Link = Color(0xFF6FA8FF)

    val AccountBar = Color(0xFF242424)
    val BottomBar = Color(0xFF232323)
    val Overlay = Color(0xFF464646)
}

internal val EclipseColorScheme = darkColorScheme(
    primary = EclipsePalette.Primary,
    onPrimary = EclipsePalette.Text,
    primaryContainer = EclipsePalette.PrimaryPressed,
    onPrimaryContainer = EclipsePalette.Text,
    secondary = EclipsePalette.Control,
    onSecondary = EclipsePalette.Text,
    secondaryContainer = EclipsePalette.Panel,
    onSecondaryContainer = EclipsePalette.Text,
    tertiary = EclipsePalette.Warning,
    onTertiary = EclipsePalette.Canvas,
    tertiaryContainer = EclipsePalette.Panel,
    onTertiaryContainer = EclipsePalette.Text,
    error = EclipsePalette.Danger,
    onError = EclipsePalette.Text,
    errorContainer = EclipsePalette.Danger,
    onErrorContainer = EclipsePalette.Text,
    background = EclipsePalette.Canvas,
    onBackground = EclipsePalette.Text,
    surface = EclipsePalette.Panel,
    onSurface = EclipsePalette.Text,
    surfaceVariant = EclipsePalette.Control,
    onSurfaceVariant = EclipsePalette.Muted,
    surfaceTint = EclipsePalette.Primary,
    inverseSurface = EclipsePalette.Text,
    inverseOnSurface = EclipsePalette.Canvas,
    inversePrimary = EclipsePalette.PrimaryPressed,
    outline = EclipsePalette.Border,
    outlineVariant = EclipsePalette.Border,
    scrim = Color.Black,
    surfaceBright = EclipsePalette.ControlHover,
    surfaceDim = EclipsePalette.Canvas,
    surfaceContainerLowest = EclipsePalette.Canvas,
    surfaceContainerLow = EclipsePalette.Panel,
    surfaceContainer = EclipsePalette.Panel,
    surfaceContainerHigh = EclipsePalette.Control,
    surfaceContainerHighest = EclipsePalette.ControlHover,
)
