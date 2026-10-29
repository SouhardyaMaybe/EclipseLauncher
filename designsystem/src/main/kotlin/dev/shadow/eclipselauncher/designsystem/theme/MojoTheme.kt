/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Screen geometry and copy come from the reference launcher, but every color
 * token carries Eclipse's own Ore-inspired palette ([EclipsePalette]) so the
 * rebuilt (non-home) surfaces match the approved home page. Values that were
 * already Eclipse's (account strip, bottom bar, overlay, text) are kept
 * verbatim.
 */
object MojoPalette {
    val Background = Color(0xFF1C1D1D) // Eclipse Canvas
    val StatusBar = Color(0xFF242424) // Eclipse AccountBar
    val BottomBar = Color(0xFF232323) // Eclipse BottomBar
    val Overlay = Color(0xFF464646) // Eclipse Overlay
    val Accent = Color(0xFF9CD78C) // Eclipse Focus/Success accent green
    val SecondaryButton = Color(0xFF2A2B2C) // Eclipse Panel
    val PrimaryText = Color(0xFFFFFFFF) // Eclipse Text
    val SecondaryText = Color(0xFFB0B0B0) // Eclipse Muted
    val Error = Color(0xFFFF8A80) // Eclipse Danger
    val Input = Color(0x00000000)
    val Line = Color(0xFF39393B) // Eclipse Border
    val Control = Color(0xFF39393B) // Eclipse Control
    val ControlPressed = Color(0xFF2A2B2C) // Eclipse ControlPressed
    val LoggerHeader = Color(0xFF47484A) // Eclipse ControlHover
    val Scrim = Color(0xA6000000)
    val Drawer = Color(0xFF47484A) // Eclipse ControlHover (raised surface)
    val ActionButton = Color(0xFF39393B) // Eclipse Control
    val Divider = Overlay
}

/**
 * Mojo uses Noto Sans (regular and bold) for launcher chrome. The referenced
 * font files are not part of the approved asset set, so the system sans face
 * is used with the same sizes and weights.
 */
object MojoType {
    private val Sans = FontFamily.SansSerif
    private val Bold = FontFamily.SansSerif

    val body = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
    val bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val secondary = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = MojoPalette.SecondaryText,
    )
    val title = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = MojoPalette.PrimaryText,
    )
    val dialogTitle = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = MojoPalette.PrimaryText,
    )
    val button = TextStyle(
        fontFamily = Bold,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = MojoPalette.PrimaryText,
    )
    val preferenceTitle = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = MojoPalette.PrimaryText,
    )
    val preferenceSummary = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = MojoPalette.SecondaryText,
    )
}

internal val MojoColorScheme = darkColorScheme(
    primary = MojoPalette.Accent,
    onPrimary = MojoPalette.Background,
    secondary = MojoPalette.SecondaryButton,
    onSecondary = MojoPalette.PrimaryText,
    background = MojoPalette.Background,
    onBackground = MojoPalette.PrimaryText,
    surface = MojoPalette.StatusBar,
    onSurface = MojoPalette.PrimaryText,
    surfaceVariant = MojoPalette.SecondaryButton,
    onSurfaceVariant = MojoPalette.SecondaryText,
    error = MojoPalette.Error,
    onError = MojoPalette.PrimaryText,
    outline = MojoPalette.Line,
    scrim = MojoPalette.Scrim,
)

@Composable
fun MojoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MojoColorScheme,
        typography = androidx.compose.material3.Typography(
            bodyMedium = MojoType.body,
            bodySmall = MojoType.bodySmall,
            titleLarge = MojoType.title,
            labelLarge = MojoType.button,
        ),
        shapes = EclipseShapes,
        content = content,
    )
}
