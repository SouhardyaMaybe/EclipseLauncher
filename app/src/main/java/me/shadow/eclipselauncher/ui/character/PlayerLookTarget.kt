/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The point in model space that the player's head should look at.
 * Updated by drag gestures; the head smoothly tracks this target.
 */
package me.shadow.eclipselauncher.ui.character

import androidx.compose.ui.geometry.Offset

/**
 * A target point in model space for the head to look at.
 * Coordinates are in model-space units (feet at 0, head at ~24, waist at 16).
 */
class PlayerLookTarget {
    /** X in model space (left/right). */
    var x: Float = 0f
    /** Y in model space (up/down). */
    var y: Float = 24f
    /** Z in model space (front/back). */
    var z: Float = 0f

    /** Resets the target to the default (straight ahead at eye level). */
    fun reset() {
        x = 0f
        y = 24f
        z = 0f
    }

    /** Sets the target to a point in model space. */
    fun set(tx: Float, ty: Float, tz: Float) {
        x = tx
        y = ty
        z = tz
    }

    /** Returns the target as a model-space vector. */
    fun toVector(): FloatArray = floatArrayOf(x, y, z)
}