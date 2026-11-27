/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The eight procedural animation clips used by the emote picker.
 * Each pose is defined by per-part keyframe data (translation + rotation).
 * The reference uses a fixed set of 8 poses: IDLE, WAVE, CLAP, CHEER, DANCE, ZOMBIE, SNEAK, WALK.
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The eight built-in animation clips.
 * Each clip is a fixed pose (no time variation — emotes are discrete).
 */
enum class Player3DAnimation {
    IDLE,   // Standing, breathing, idle head glances
    WAVE,   // Right arm up, palm forward
    CLAP,   // Both arms forward, hands meet at chest
    CHEER,  // Both arms up, jumping pose
    DANCE,  // Arms out, hips swaying
    ZOMBIE, // Arms forward, stiff walk
    SNEAK,  // Crouched, arms forward
    WALK,   // Mid-stride, arms/legs opposite
}

/**
 * Returns the [PoseTransform] for a given part at time t for the given animation.
 * Each animation is a fixed pose (no time variation) except IDLE which uses t.
 * For emotes, t is ignored (static pose).
 */
fun getPartAnimationTransform(
    partName: String,
    t: Float,
    animation: Player3DAnimation,
): PoseTransform {
    // Constants used across animations
    val ARM_UP = -1.57f // -90°
    val ARM_FWD = -1.15f // -66°
    val ARM_DOWN = 0.0f
    val ARM_SWING = 0.3f
    val LEG_WALK = 0.7f
    val BODY_LEAN = 0.2f
    val CROUCH = -0.5f

    // Helper: sin/cos with frequency
    fun oscillate(freq: Float, amp: Float): Float = sin(t * freq) * amp

    return when (animation) {
        Player3DAnimation.IDLE -> when (partName) {
            "head" -> PoseTransform(
                rx = 0.03f * cos(t * 0.8f),
                ry = 0.05f * sin(0.45f * t),
            )
            "body" -> PoseTransform(rx = 0.015f * sin(t * 1.6f))
            "right_arm" -> PoseTransform(
                tx = 0.04f * sin(1.34f * t),
                tz = 0.04f * cos(t * 1.8f) + 0.06f,
            )
            "left_arm" -> PoseTransform(
                tx = -0.04f * sin(1.34f * t),
                tz = -0.04f * cos(t * 1.8f) - 0.06f,
            )
            "right_leg", "left_leg" -> PoseTransform()
            else -> PoseTransform()
        }

        Player3DAnimation.WAVE -> when (partName) {
            "head" -> PoseTransform(rx = -0.04f, tz = -0.08f)
            "right_arm" -> PoseTransform(tx = -1.95f, ty = -0.22f, tz = 0.32f * sin(5.5f * t) - 0.45f)
            "left_arm" -> PoseTransform(tx = 0.03f * sin(1.5f * t), tz = -0.06f)
            "right_leg", "left_leg" -> PoseTransform()
            else -> PoseTransform()
        }

        Player3DAnimation.CLAP -> when (partName) {
            "head" -> PoseTransform(rx = -0.04f, tz = -0.08f)
            "right_arm" -> PoseTransform(tx = 0.03f * sin(1.5f * t), tz = -0.06f)
            "left_arm" -> PoseTransform(tx = 0.03f * sin(1.5f * t), tz = 0.06f)
            "right_leg", "left_leg" -> PoseTransform()
            else -> PoseTransform()
        }

        Player3DAnimation.CHEER -> when (partName) {
            "head" -> PoseTransform(rx = 0.08f)
            "right_arm" -> PoseTransform(tx = -1.15f, ty = 0.45f - oscillate(6.5f, 0.22f), tz = -0.12f)
            "left_arm" -> PoseTransform(tx = -1.15f, ty = oscillate(6.5f, 0.22f) - 0.45f, tz = 0.12f)
            "right_leg", "left_leg" -> PoseTransform()
            else -> PoseTransform()
        }

        Player3DAnimation.DANCE -> when (partName) {
            "head" -> PoseTransform(rx = -0.2f)
            "right_arm" -> PoseTransform(tx = -2.5f, tz = 0.14f * sin(6.0f * t) - 0.32f)
            "left_arm" -> PoseTransform(tx = -2.5f, tz = 0.32f - 0.14f * sin(6.0f * t))
            "root" -> PoseTransform(ty = 0.0f, rz = oscillate(6.0f, 0.04f) * 1.8f)
            "right_leg", "left_leg" -> PoseTransform()
            else -> PoseTransform()
        }

        Player3DAnimation.ZOMBIE -> when (partName) {
            "head" -> PoseTransform(rx = 0.08f)
            "right_arm" -> PoseTransform(tx = 0.6f, ty = 0.0f, tz = 0.18f * sin(4.8f * t) - 0.45f)
            "left_arm" -> PoseTransform(tx = 0.6f, ty = -0.0f, tz = -0.18f * sin(4.8f * t) + 0.45f)
            "right_leg", "left_leg" -> PoseTransform()
            else -> PoseTransform()
        }

        Player3DAnimation.SNEAK -> when (partName) {
            "head" -> PoseTransform(rx = 0.12f)
            "right_arm" -> PoseTransform(tx = 0.04f * sin(1.6f * t) - 1.57f, ty = -0.08f)
            "left_arm" -> PoseTransform(tx = 0.04f * sin(1.6f * t) - 1.57f, ty = 0.08f)
            "right_leg", "left_leg" -> PoseTransform()
            "root" -> PoseTransform(ty = -0.08f * abs(sin(t * 1.6f)), rz = oscillate(4.8f, 0.02f))
            else -> PoseTransform()
        }

        Player3DAnimation.WALK -> when (partName) {
            "right_arm" -> PoseTransform(
                tx = 0.65f * cos(4.8f * t),
                rz = 0.0f,
            )
            "left_arm" -> PoseTransform(
                tx = -0.65f * cos(4.8f * t),
                rz = 0.0f,
            )
            "right_leg" -> PoseTransform(tx = 0.7f * sin(4.8f * t))
            "left_leg" -> PoseTransform(tx = -0.7f * sin(4.8f * t))
            "root" -> PoseTransform(
                ty = abs(sin(4.8f * t)) * 0.06f,
                rz = 0.02f * sin(4.8f * t),
            )
            else -> PoseTransform()
        }
    }
}