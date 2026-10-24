/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * The brand palette from docs/09 §3.2. These are deliberate seeds, not values
 * generated from a wallpaper. Dynamic color is available as an opt-in and is
 * applied in EclipseTheme, never here.
 */

private val LightPrimary = Color(0xFF8A4B08)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFFFDDB6)
private val LightOnPrimaryContainer = Color(0xFF2C1600)
private val LightSecondary = Color(0xFF3F665F)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFC1EAE1)
private val LightOnSecondaryContainer = Color(0xFF0B3733)
private val LightTertiary = Color(0xFF5B5D2E)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFE1E3A4)
private val LightOnTertiaryContainer = Color(0xFF1E2000)
private val LightError = Color(0xFFBA1A1A)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFFDAD6)
private val LightOnErrorContainer = Color(0xFF93000A)
private val LightSurface = Color(0xFFFBF8F3)
private val LightOnSurface = Color(0xFF1E1B18)
private val LightSurfaceVariant = Color(0xFFEFEAE1)
private val LightOnSurfaceVariant = Color(0xFF51443A)
private val LightOutline = Color(0xFF857468)
private val LightOutlineVariant = Color(0xFFD8C8BB)

internal val EclipseLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = LightPrimary,
    inverseSurface = Color(0xFF33302C),
    inverseOnSurface = Color(0xFFF6F0E8),
    inversePrimary = Color(0xFFFFB86A),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = Color.Black,
    surfaceBright = Color(0xFFFFFBF6),
    surfaceDim = Color(0xFFDCD7CF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F1E9),
    surfaceContainer = Color(0xFFEFEAE1),
    surfaceContainerHigh = Color(0xFFE9E3D9),
    surfaceContainerHighest = Color(0xFFE4DED4),
)

private val DarkPrimary = Color(0xFFF2A93B)
private val DarkOnPrimary = Color(0xFF2A1700)
private val DarkPrimaryContainer = Color(0xFF6A3A00)
private val DarkOnPrimaryContainer = Color(0xFFFFDDB6)
private val DarkSecondary = Color(0xFFA6CEC5)
private val DarkOnSecondary = Color(0xFF0B3733)
private val DarkSecondaryContainer = Color(0xFF264E48)
private val DarkOnSecondaryContainer = Color(0xFFC1EAE1)
private val DarkTertiary = Color(0xFFC5C78B)
private val DarkOnTertiary = Color(0xFF2F3204)
private val DarkTertiaryContainer = Color(0xFF464A16)
private val DarkOnTertiaryContainer = Color(0xFFE1E3A4)
private val DarkError = Color(0xFFFFB4AB)
private val DarkOnError = Color(0xFF690005)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)
private val DarkSurface = Color(0xFF141210)
private val DarkOnSurface = Color(0xFFEDE7DF)
private val DarkSurfaceVariant = Color(0xFF211E1B)
private val DarkOnSurfaceVariant = Color(0xFFD5C4B7)
private val DarkOutline = Color(0xFF9F8D80)
private val DarkOutlineVariant = Color(0xFF4F443B)

internal val EclipseDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DarkPrimary,
    inverseSurface = Color(0xFFEDE7DF),
    inverseOnSurface = Color(0xFF33302C),
    inversePrimary = Color(0xFF8A4B08),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Color.Black,
    surfaceBright = Color(0xFF3A3632),
    surfaceDim = Color(0xFF141210),
    surfaceContainerLowest = Color(0xFF0E0D0B),
    surfaceContainerLow = Color(0xFF1C1A17),
    surfaceContainer = Color(0xFF211E1B),
    surfaceContainerHigh = Color(0xFF2B2723),
    surfaceContainerHighest = Color(0xFF36312D),
)
