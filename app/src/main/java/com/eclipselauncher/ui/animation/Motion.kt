/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Small shared motion helpers. Dialogs and press feedback use the standard
 * Material 3 components (BasicAlertDialog, clickable ripple); only the
 * disclosure caret keeps a tiny rotation animation, which Material 3 does
 * not provide as a component.
 */
package com.eclipselauncher.ui.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/** Flips a caret/arrow 180° while [open], with a light bounce on the way. */
fun Modifier.caretRotation(open: Boolean): Modifier = composed {
    val angle by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "caret",
    )
    graphicsLayer(rotationZ = angle)
}
