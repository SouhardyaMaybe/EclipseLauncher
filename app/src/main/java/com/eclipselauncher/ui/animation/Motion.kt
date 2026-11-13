/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Motion — the launcher's small in-house animation library. One place for
 * the interaction feel used across the UI:
 *
 *  - pressSqueeze      press: the shape squashes (taller squeeze than wide),
 *                      release: springs back with a soft bounce.
 *  - pressPushCorner   press: one corner pushes in while the opposite
 *                      corner stays pinned to its point (transform origin),
 *                      release: bounces back.
 *  - caretRotation     a caret/arrow that flips 180° when its panel opens
 *                      or collapses, reacting with a light bounce.
 *  - AnimatedDialog    dialog window that springs open with an overshoot
 *                      (bounce) and gets sucked back into a corner on
 *                      close — scale + fade around a chosen origin.
 */
package com.eclipselauncher.ui.animation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

private const val DIALOG_EXIT_MS = 170L

/**
 * A clickable that squeezes on press — the shape scales down slightly more
 * vertically than horizontally, and springs back with a bounce on release
 * (spring overshoot gives the rebound). No ripple: the squeeze is the
 * feedback.
 */
fun Modifier.pressSqueeze(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squeezeX by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "squeezeX",
    )
    val squeezeY by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "squeezeY",
    )
    this
        .graphicsLayer {
            scaleX = squeezeX
            scaleY = squeezeY
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * A clickable that pushes one corner on press while the other corners stay
 * pinned to their points: the whole shape scales around a fixed transform
 * origin, so the pressed side travels and the origin corner does not move.
 * Springs back on release.
 */
fun Modifier.pressPushCorner(
    pushOrigin: TransformOrigin = TransformOrigin(0f, 1f),
    pressScale: Float = 0.955f,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pushScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = pushOrigin
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

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

/**
 * A dialog whose content springs open with an overshoot (bounce) and is
 * sucked back into [suckOrigin] on close — it scales and fades around that
 * origin, so closing looks like the window is pulled into the corner. The
 * underlying Dialog window stays mounted until the exit finishes, then
 * unmounts (fixed exit budget), so dismissing animates instead of popping.
 */
@OptIn(ExperimentalTransitionApi::class)
@Composable
fun AnimatedDialog(
    visible: Boolean,
    suckOrigin: TransformOrigin = TransformOrigin(0.5f, 0f),
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    var mounted by remember { mutableStateOf(false) }
    val motion = remember { MutableTransitionState(false) }

    LaunchedEffect(visible) {
        if (visible) {
            mounted = true
            motion.targetState = true
        } else if (mounted) {
            motion.targetState = false
            delay(DIALOG_EXIT_MS)
            mounted = false
        }
    }

    if (mounted) {
        Dialog(onDismissRequest = onDismissRequest) {
            val transition = rememberTransition(motion, label = "dialogMotion")
            val progress by transition.animateFloat(
                transitionSpec = {
                    if (targetState) {
                        spring(
                            dampingRatio = 0.55f,
                            stiffness = Spring.StiffnessMedium,
                        )
                    } else {
                        tween(durationMillis = DIALOG_EXIT_MS.toInt(), easing = FastOutLinearInEasing)
                    }
                },
                label = "dialogProgress",
            ) { open -> if (open) 1f else 0f }
            Box(
                modifier = Modifier.graphicsLayer {
                    // Starts small and settled overshoots past 1 = bounce.
                    scaleX = 0.72f + 0.28f * progress
                    scaleY = 0.72f + 0.28f * progress
                    alpha = progress.coerceIn(0f, 1f)
                    transformOrigin = suckOrigin
                },
            ) {
                content()
            }
        }
    }
}
