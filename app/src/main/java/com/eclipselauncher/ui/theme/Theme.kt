/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
@file:OptIn(ExperimentalTextApi::class)

package com.eclipselauncher.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.eclipselauncher.R
import com.eclipselauncher.data.SettingsStore

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
 * The Eclipse dark palette: pure black canvas, panel surfaces, and a deep blue
 * primary (replacing the old green). Blue reads cleanly against black and is
 * neither violet nor purple.
 */
private val EclipseDarkColorScheme = darkColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF64B5F6),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF121212),
    onSecondaryContainer = Color(0xFFBBBBBB),
    tertiary = Color(0xFF64B5F6),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF121212),
    onTertiaryContainer = Color(0xFF64B5F6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFBBBBBB),
    surfaceTint = Color(0xFF1565C0),
    outline = Color(0xFF2C2C2C),
    outlineVariant = Color(0xFF3A3A3A),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFFC93636),
    onErrorContainer = Color(0xFFFFFFFF),
    scrim = Color.Black,
)

/**
 * The Eclipse light palette: white canvas with the same deep blue primary.
 * Mirrors the dark scheme so switching modes stays consistent.
 */
private val EclipseLightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF003366),
    secondary = Color(0xFF64B5F6),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF64B5F6),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFFE3F2FD),
    onTertiaryContainer = Color(0xFF000000),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF424242),
    surfaceTint = Color(0xFF1565C0),
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFCFD8DC),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFFB3261E),
    scrim = Color.Black,
)

/** Returns true when the system is currently in dark mode. */
private fun isSystemDarkMode(context: Context): Boolean {
    val uiMode = context.resources.configuration.uiMode
    return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}

/**
 * Resolves the active color scheme for [themeMode].
 *
 * - DYNAMIC: Material You wallpaper-derived colors on Android 12+, falling back
 *   to the matching static scheme on older versions.
 * - DARK / LIGHT: the fixed Eclipse scheme for that mode.
 */
private fun eclipseColorScheme(context: Context, themeMode: SettingsStore.ThemeMode): androidx.compose.material3.ColorScheme =
    when (themeMode) {
        SettingsStore.ThemeMode.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isSystemDarkMode(context)) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else if (isSystemDarkMode(context)) EclipseDarkColorScheme
            else EclipseLightColorScheme
        }
        SettingsStore.ThemeMode.DARK -> EclipseDarkColorScheme
        SettingsStore.ThemeMode.LIGHT -> EclipseLightColorScheme
    }

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
fun EclipseTheme(
    themeMode: SettingsStore.ThemeMode = SettingsStore.ThemeMode.DYNAMIC,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = eclipseColorScheme(context, themeMode)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = EclipseTypography,
        shapes = EclipseShapes,
        content = content,
    )
}
