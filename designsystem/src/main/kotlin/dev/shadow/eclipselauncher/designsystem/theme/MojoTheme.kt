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
 * Tokens taken from the reference launcher's own resource values. They are
 * applied only to the rebuilt (non-home) surfaces; the approved home page
 * keeps [EclipsePalette].
 */
object MojoPalette {
    val Background = Color(0xFF181818)
    val StatusBar = Color(0xFF242424)
    val BottomBar = Color(0xFF232323)
    val Overlay = Color(0xFF464646)
    val Accent = Color(0xFF91DFFB)
    val SecondaryButton = Color(0xFF2A2A2A)
    val PrimaryText = Color(0xFFFFFFFF)
    val SecondaryText = Color(0xFFB2B2B2)
    val Error = Color(0xFFFF0000)
    val Input = Color(0x00000000)
    val Line = Color(0xFF909090)
    val Control = Color(0x4CC4C4C4)
    val ControlPressed = Color(0x4D000000)
    val LoggerHeader = Color(0xFF555555)
    val Scrim = Color(0xA6000000)
    val Drawer = Color(0xFF424242)
    val ActionButton = Color(0xFF424242)
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
