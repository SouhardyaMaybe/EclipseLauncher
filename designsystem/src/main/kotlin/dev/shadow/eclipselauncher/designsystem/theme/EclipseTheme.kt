/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The v1 launcher is dark-only. The Ore-inspired palette is fixed so status
 * contrast and the green primary action stay predictable on every device.
 */
@Composable
fun EclipseTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEclipseStatusColors provides EclipseStatus) {
        MaterialTheme(
            colorScheme = EclipseColorScheme,
            typography = EclipseTypography,
            shapes = EclipseShapes,
            content = content,
        )
    }
}
