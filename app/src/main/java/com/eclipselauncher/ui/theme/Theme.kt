/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
@file:OptIn(ExperimentalTextApi::class)

package com.eclipselauncher.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
 * Eclipse dark palette — pure black canvas with deep blue primary. Neither
 * violet nor purple.
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
 * Eclipse light palette — white canvas with deep blue primary.
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

/**
 * Urban theme — copied from Zalith Launcher 2's urbanAsh palette.
 * Dark variant uses dark gray surfaces; light variant uses light gray surfaces.
 */
private val UrbanDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC7C6C6),
    onPrimary = Color(0xFF2F3131),
    primaryContainer = Color(0xFF9B9B9B),
    onPrimaryContainer = Color(0xFF0B0C0D),
    secondary = Color(0xFFC8C6C6),
    onSecondary = Color(0xFF303030),
    secondaryContainer = Color(0xFF3D3D3D),
    onSecondaryContainer = Color(0xFFD3D0D0),
    tertiary = Color(0xFFCBC5C7),
    onTertiary = Color(0xFF323031),
    tertiaryContainer = Color(0xFFA09B9D),
    onTertiaryContainer = Color(0xFF0F0D0F),
    background = Color(0xFF141313),
    onBackground = Color(0xFFE5E2E1),
    surface = Color(0xFF141313),
    onSurface = Color(0xFFE5E2E1),
    surfaceVariant = Color(0xFF444748),
    onSurfaceVariant = Color(0xFFC4C7C7),
    surfaceTint = Color(0xFFC7C6C6),
    outline = Color(0xFF8E9192),
    outlineVariant = Color(0xFF444748),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black,
)

private val UrbanLightColorScheme = lightColorScheme(
    primary = Color(0xFF5E5E5F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFAEAEAE),
    onPrimaryContainer = Color(0xFF222324),
    secondary = Color(0xFF5F5E5E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E6E5),
    onSecondaryContainer = Color(0xFF4A4A4A),
    tertiary = Color(0xFF615D5F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB2ADAF),
    onTertiaryContainer = Color(0xFF252324),
    background = Color(0xFFFCF8F8),
    onBackground = Color(0xFF1C1B1B),
    surface = Color(0xFFFCF8F8),
    onSurface = Color(0xFF1C1B1B),
    surfaceVariant = Color(0xFFE0E3E3),
    onSurfaceVariant = Color(0xFF444748),
    surfaceTint = Color(0xFF5E5E5F),
    outline = Color(0xFF747878),
    outlineVariant = Color(0xFFC4C7C7),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
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
 * - URBAN: the fixed dark/gray palette copied from Zalith Launcher 2.
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
        SettingsStore.ThemeMode.URBAN -> {
            if (isSystemDarkMode(context)) UrbanDarkColorScheme
            else UrbanLightColorScheme
        }
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

    // Crossfade smoothly between color schemes when the user switches themes.
    Crossfade(
        targetState = themeMode,
        animationSpec = tween(250),
    ) { mode ->
        MaterialTheme(
            colorScheme = eclipseColorScheme(context, mode),
            typography = EclipseTypography,
            shapes = EclipseShapes,
            content = content,
        )
    }
}
