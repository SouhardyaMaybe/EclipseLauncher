/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Runtime motion state for the interactive 3D model.
 * Holds the current pose channels (yaw, pitch, head, arms, breath) and
 * provides the idle choreography driven by elapsed time.
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * The current animated state of the player model.
 * Updated each frame by the idle choreography or by an active emote.
 */
class PlayerMotion {
    var bodyYaw: Float = 0f
    var headYaw: Float = 0f
    var headPitch: Float = 0f
    var armSwing: Float = 0f
    var breath: Float = 0f

    /**
     * Advances the idle choreography to the given time.
     * This is called every frame when no emote is playing.
     */
    fun updateIdle(timeSeconds: Float) {
        // Whole-body weight shift: a very slow left/right sway.
        bodyYaw = sin(timeSeconds * 0.32f) * 0.35f

        // Glances: eased head targets that change every few seconds,
        // driven by a hash so the motion looks random but stays reproducible.
        val segmentLength = 5.5f
        val seg = floor(timeSeconds / segmentLength)
        val within = timeSeconds - seg * segmentLength
        val k = smoothstep((within / 1.3f).coerceIn(0f, 1f))
        val curYaw = hash01(seg) * 1.1f - 0.55f
        val prevYaw = hash01(seg - 1f) * 1.1f - 0.55f
        val curPitch = hash01(seg + 17f) * 0.28f - 0.14f
        val prevPitch = hash01(seg + 16f) * 0.28f - 0.14f
        headYaw = prevYaw + (curYaw - prevYaw) * k
        headPitch = prevPitch + (curPitch - prevPitch) * k

        // Arms hang and drift with the sway at idle frequency — far
        // below the ~1.3Hz rhythm a walk cycle would use.
        armSwing = sin(timeSeconds * 0.5f) * 0.03f
        breath = sin(timeSeconds * 1.1f) * 0.012f
    }

    /**
     * Applies an emote pose, overriding the idle channels.
     * Called when an emote is triggered.
     */
    fun applyEmote(pose: PoseTransform, partName: String) {
        when (partName) {
            "root" -> {
                // Root transform handled by caller
            }
            "head" -> {
                headYaw = pose.ry
                headPitch = pose.rx
            }
            "right_arm", "left_arm" -> {
                armSwing = pose.rz // repurposed for emote arm angle
            }
            else -> {}
        }
    }

    /** Returns the current pose for the given part (used by renderer). */
    fun getCurrentPose(partName: String): PoseTransform {
        return when (partName) {
            "head" -> PoseTransform(rx = headPitch, ry = headYaw)
            "right_arm", "left_arm" -> PoseTransform(rz = armSwing)
            "root" -> PoseTransform(rz = bodyYaw) // body yaw applied at root
            else -> PoseTransform()
        }
    }

    private fun smoothstep(x: Float): Float {
        val c = x.coerceIn(0f, 1f)
        return c * c * (3f - 2f * c)
    }

    private fun hash01(x: Float): Float {
        val s = sin(x * 127.1f) * 43758.5453f
        return s - floor(s)
    }
}