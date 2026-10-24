/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * The app theme.
 *
 * Brand color is the default. Dynamic color is an explicit opt-in: a launcher
 * with a distinct identity should not silently become the wallpaper's palette,
 * and the status colors must keep their meaning either way.
 */
@Composable
fun EclipseTheme(
    dynamicColor: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> EclipseDarkColorScheme
        else -> EclipseLightColorScheme
    }
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(LocalEclipseStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EclipseTypography,
            shapes = EclipseShapes,
            content = content,
        )
    }
}
