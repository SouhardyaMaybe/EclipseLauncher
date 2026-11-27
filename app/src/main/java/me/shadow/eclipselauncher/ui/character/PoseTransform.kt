/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Per-part animation transform for a single frame.
 * Applied in order: translate to pivot → rotate → translate from pivot.
 * All values are in model-space units.
 */
package me.shadow.eclipselauncher.ui.character

data class PoseTransform(
    val tx: Float = 0f,  // translation X after rotation
    val ty: Float = 0f,  // translation Y after rotation
    val tz: Float = 0f,  // translation Z after rotation
    val rx: Float = 0f,  // rotation X (radians)
    val ry: Float = 0f,  // rotation Y (radians)
    val rz: Float = 0f   // rotation Z (radians)
) {
    companion object {
        fun identity(): PoseTransform = PoseTransform()
    }
}