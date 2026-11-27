/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The emote picker icon: draws a label for each emote pose.
 * Used in the emote picker grid inside the skin dialog.
 */
package me.shadow.eclipselauncher.ui.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.shadow.eclipselauncher.ui.theme.EclipseRadius

/**
 * A single emote pose icon for the emote picker.
 * Shows the emote name with a colored background.
 */
@Composable
fun EmotePoseIcon(
    animation: Player3DAnimation,
    timeSeconds: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    val emoteLabel = when (animation) {
        Player3DAnimation.IDLE -> "Idle"
        Player3DAnimation.WAVE -> "Wave"
        Player3DAnimation.CLAP -> "Clap"
        Player3DAnimation.CHEER -> "Cheer"
        Player3DAnimation.DANCE -> "Dance"
        Player3DAnimation.ZOMBIE -> "Zombie"
        Player3DAnimation.SNEAK -> "Sneak"
        Player3DAnimation.WALK -> "Walk"
    }

    val bgColor = if (selected) Color.Red.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.1f)
    val borderColor = if (selected) Color.Red else Color.Transparent

    Box(
        modifier = modifier
            .size(56.dp)
            .background(bgColor, RoundedCornerShape(EclipseRadius.tile))
            .border(if (selected) 2.dp else 0.dp, borderColor, RoundedCornerShape(EclipseRadius.tile))
            .pointerInput(Unit) {
                onClick?.let { click ->
                    detectTapGestures(onTap = { click() })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = emoteLabel,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}