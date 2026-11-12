package com.eclipselauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EclipseColors.DarkPrimary,
    onPrimary = EclipseColors.DarkOnPrimary,
    primaryContainer = EclipseColors.DarkPrimaryContainer,
    onPrimaryContainer = EclipseColors.DarkOnPrimaryContainer,
    secondary = EclipseColors.DarkSecondary,
    onSecondary = EclipseColors.DarkOnSecondary,
    secondaryContainer = EclipseColors.DarkSecondaryContainer,
    onSecondaryContainer = EclipseColors.DarkOnSecondaryContainer,
    tertiary = EclipseColors.DarkSecondary,
    onTertiary = EclipseColors.DarkOnSecondary,
    tertiaryContainer = EclipseColors.DarkSecondaryContainer,
    onTertiaryContainer = EclipseColors.DarkOnSecondaryContainer,
    error = EclipseColors.DarkError,
    onError = EclipseColors.DarkOnError,
    errorContainer = EclipseColors.DarkErrorContainer,
    onErrorContainer = EclipseColors.DarkOnErrorContainer,
    background = EclipseColors.DarkBackground,
    onBackground = EclipseColors.DarkOnBackground,
    surface = EclipseColors.DarkSurface,
    onSurface = EclipseColors.DarkOnSurface,
    surfaceVariant = EclipseColors.DarkSurfaceVariant,
    onSurfaceVariant = EclipseColors.DarkOnSurfaceVariant,
    outline = EclipseColors.DarkOutlineVariant,
    outlineVariant = EclipseColors.DarkOutlineVariant,
    shadow = Color.Black,
    scrim = EclipseColors.DarkScrim,
    inverseSurface = EclipseColors.LightSurface,
    inverseOnSurface = EclipseColors.LightOnSurface,
    inversePrimary = EclipseColors.DarkPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = EclipseColors.LightPrimary,
    onPrimary = EclipseColors.LightOnPrimary,
    primaryContainer = EclipseColors.LightPrimaryContainer,
    onPrimaryContainer = EclipseColors.LightOnPrimaryContainer,
    secondary = EclipseColors.LightSecondary,
    onSecondary = EclipseColors.LightOnSecondary,
    secondaryContainer = EclipseColors.LightSecondaryContainer,
    onSecondaryContainer = EclipseColors.LightOnSecondaryContainer,
    tertiary = EclipseColors.LightSecondary,
    onTertiary = EclipseColors.LightOnTertiary,
    tertiaryContainer = EclipseColors.LightSecondaryContainer,
    onTertiaryContainer = EclipseColors.LightOnTertiaryContainer,
    error = EclipseColors.LightError,
    onError = EclipseColors.LightOnError,
    errorContainer = EclipseColors.LightErrorContainer,
    onErrorContainer = EclipseColors.LightOnErrorContainer,
    background = EclipseColors.LightBackground,
    onBackground = EclipseColors.LightOnBackground,
    surface = EclipseColors.LightSurface,
    onSurface = EclipseColors.LightOnSurface,
    surfaceVariant = EclipseColors.LightSurfaceVariant,
    onSurfaceVariant = EclipseColors.LightOnSurfaceVariant,
    outline = EclipseColors.LightOutlineVariant,
    outlineVariant = EclipseColors.LightOutlineVariant,
    shadow = Color.Black,
    scrim = EclipseColors.LightScrim,
    inverseSurface = EclipseColors.DarkSurface,
    inverseOnSurface = EclipseColors.DarkOnSurface,
    inversePrimary = EclipseColors.LightPrimary
)

@Composable
fun EclipseLauncherTheme(
    darkTheme: Boolean = androidx.compose.material3.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Extension for easy access to Eclipse-specific colors
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

val MaterialTheme.eclipseColors: EclipseColors
    get() = EclipseColors