/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
@file:OptIn(ExperimentalTextApi::class)

package com.eclipselauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.eclipselauncher.R

/**
 * Geist — Vercel's minimal grotesque (OFL 1.1) — is the whole UI's face. It
 * ships as a single variable font with a weight axis, so each weight the type
 * scale asks for is a real instance on that axis rather than the platform's
 * synthetic bold.
 */
private val EclipseFontFamily: FontFamily by lazy {
    fun variation(weight: Int) = FontVariation.Settings(FontVariation.weight(weight))
    FontFamily(
        Font(R.font.geist, weight = FontWeight.Normal, variationSettings = variation(400)),
        Font(R.font.geist, weight = FontWeight.Medium, variationSettings = variation(500)),
        Font(R.font.geist, weight = FontWeight.SemiBold, variationSettings = variation(600)),
        Font(R.font.geist, weight = FontWeight.Bold, variationSettings = variation(700)),
    )
}

/**
 * The Eclipse palette: dark canvas, panel surfaces, green primary actions and
 * the light-green accent for text and highlights on dark surfaces. Dark-only —
 * the launcher's status contrast is defined against these values.
 */
private val EclipseColorScheme = darkColorScheme(
    primary = Color(0xFF3B8526),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2F6B1E),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF9CD78C),
    onSecondary = Color(0xFF1C1D1D),
    secondaryContainer = Color(0xFF2A2B2C),
    onSecondaryContainer = Color(0xFFB0B0B0),
    tertiary = Color(0xFF9CD78C),
    onTertiary = Color(0xFF1C1D1D),
    tertiaryContainer = Color(0xFF2A2B2C),
    onTertiaryContainer = Color(0xFF9CD78C),
    background = Color(0xFF1C1D1D),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF2A2B2C),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF39393B),
    onSurfaceVariant = Color(0xFFB0B0B0),
    surfaceTint = Color(0xFF3B8526),
    outline = Color(0xFF39393B),
    outlineVariant = Color(0xFF47484A),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF1C1D1D),
    errorContainer = Color(0xFFC93636),
    onErrorContainer = Color(0xFFFFFFFF),
    scrim = Color.Black,
)

/**
 * Material 3's default type scale with Geist applied to every text style.
 * Geist is a tight, precise grotesque with a small x-height relative to the
 * old pixel face, so M3's stock sizes read correctly without adjustment.
 */
private val EclipseTypography: Typography by lazy {
    val defaults = Typography()
    fun withFont(style: TextStyle): TextStyle =
        style.copy(fontFamily = EclipseFontFamily)
    Typography(
        displayLarge = withFont(defaults.displayLarge),
        displayMedium = withFont(defaults.displayMedium),
        displaySmall = withFont(defaults.displaySmall),
        headlineLarge = withFont(defaults.headlineLarge),
        headlineMedium = withFont(defaults.headlineMedium),
        headlineSmall = withFont(defaults.headlineSmall),
        titleLarge = withFont(defaults.titleLarge),
        titleMedium = withFont(defaults.titleMedium),
        titleSmall = withFont(defaults.titleSmall),
        bodyLarge = withFont(defaults.bodyLarge),
        bodyMedium = withFont(defaults.bodyMedium),
        bodySmall = withFont(defaults.bodySmall),
        labelLarge = withFont(defaults.labelLarge),
        labelMedium = withFont(defaults.labelMedium),
        labelSmall = withFont(defaults.labelSmall),
    )
}

@Composable
fun EclipseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EclipseColorScheme,
        typography = EclipseTypography,
        shapes = EclipseShapes,
        content = content,
    )
}
