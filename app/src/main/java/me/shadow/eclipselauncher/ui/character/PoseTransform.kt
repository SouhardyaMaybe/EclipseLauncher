/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Per-part animation transform for a single frame.
 * Applied in order: translate to pivot → rotate → translate from pivot.
 * All values are in model-space units.
 *
 * The per-part fields deliberately follow the reference constructor order:
 * rx, ry, rz, tx, ty, tz (rotations first, translations second).
 * Also used as the overall pose state for the whole model (yaw, headYaw, etc.).
 */
package me.shadow.eclipselauncher.ui.character

data class PoseTransform(
    // Per-part transform (used by getPartAnimationTransform)
    val rx: Float = 0f,  // rotation X (radians)
    val ry: Float = 0f,  // rotation Y (radians)
    val rz: Float = 0f,  // rotation Z (radians)
    val tx: Float = 0f,  // translation X after rotation
    val ty: Float = 0f,  // translation Y after rotation
    val tz: Float = 0f,  // translation Z after rotation

    // Overall model pose (used by renderer)
    val yaw: Float = 0f,       // body yaw
    val headYaw: Float = 0f,   // head yaw
    val headPitch: Float = 0f, // head pitch
    val armSwing: Float = 0f,  // arm swing angle
    val breath: Float = 0f,    // breathing offset
    val bodyPitch: Float = 0f, // body pitch
) {
    companion object {
        fun identity(): PoseTransform = PoseTransform()
    }
}
