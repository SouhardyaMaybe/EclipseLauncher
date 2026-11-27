/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.Player3DAnimation
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class Player3DAnimation {
    IDLE,   // Standing, breathing, idle head glances
    WAVE,   // Right arm up, palm forward
    CLAP,   // Both arms up, alternating
    CHEER,  // Both arms up, bouncing
    DANCE,  // Arms out, hips swaying
    ZOMBIE, // Arms forward, stiff walk
    SNEAK,  // Crouched, arms forward
    WALK;   // Mid-stride, arms/legs opposite

    companion object {
        private const val AMBIENT_R = 0.35f
        private const val SHADOW_DIR_X = 0.45f
        private const val LIGHT_R = 1.2f
    }

    fun getPartAnimationTransform(partName: String, t: Float): PoseTransform {
        return when (this) {
            IDLE -> when (partName) {
                "right_arm" -> PoseTransform(
                    rx = sin(1.34f * t) * 0.04f,
                    rz = cos(t * 1.8f) * 0.04f + 0.06f
                )
                "body" -> PoseTransform(rx = sin(t * 1.6f) * 0.015f)
                "head" -> PoseTransform(
                    rx = 0.03f * cos(t * 0.8f),
                    ry = 0.05f * sin(SHADOW_DIR_X * t)
                )
                "left_arm" -> PoseTransform(
                    rx = -sin(1.34f * t) * 0.04f,
                    rz = -cos(t * 1.8f) * 0.04f - 0.06f
                )
                else -> PoseTransform()
            }

            WAVE -> when (partName) {
                "left_arm" -> PoseTransform(
                    rx = 0.03f * sin(LIGHT_R * t),
                    rz = -0.06f
                )
                "head" -> PoseTransform(rx = -0.04f, rz = -0.08f)
                "right_arm" -> PoseTransform(
                    rx = -1.95f,
                    ry = -0.22f,
                    rz = sin(5.5f * t) * 0.32f - 0.45f
                )
                else -> PoseTransform()
            }

            CLAP -> {
                val fAbs = abs(sin(6.5f * t)) * 0.22f
                when (partName) {
                    "left_arm" -> PoseTransform(
                        rx = -1.15f,
                        ry = SHADOW_DIR_X - fAbs,
                        rz = 0.12f
                    )
                    "head" -> PoseTransform(rx = 0.08f)
                    "right_arm" -> PoseTransform(
                        rx = -1.15f,
                        ry = fAbs - 0.45f,
                        rz = -0.12f
                    )
                    else -> PoseTransform()
                }
            }

            CHEER -> {
                val d = 6.0f * t
                val bounce = -abs(sin(d)) * 1.8f
                when (partName) {
                    "right_arm" -> PoseTransform(
                        rx = -2.5f,
                        rz = sin(d) * 0.14f - 0.32f
                    )
                    "head" -> PoseTransform(rx = -0.2f)
                    "root" -> PoseTransform(ty = bounce)
                    "left_arm" -> PoseTransform(
                        rx = -2.5f,
                        rz = 0.32f - sin(d) * 0.14f
                    )
                    else -> PoseTransform()
                }
            }

            DANCE -> {
                val d2 = 4.8f * t
                val fSin = sin(d2) * 0.12f
                val fSin2 = sin(d2) * 0.18f
                when (partName) {
                    "right_arm" -> PoseTransform(
                        rx = cos(d2) * AMBIENT_R,
                        rz = sin(d2) * 0.18f + AMBIENT_R
                    )
                    "right_leg" -> PoseTransform(rx = fSin2)
                    "head" -> PoseTransform(
                        rx = abs(sin(d2)) * 0.06f,
                        ry = sin(2.4f * t) * 0.14f
                    )
                    "root" -> PoseTransform(
                        rz = fSin * 0.8f,
                        tx = fSin * 1.4f
                    )
                    "left_arm" -> PoseTransform(
                        rx = -cos(d2) * AMBIENT_R,
                        rz = -0.35f - sin(d2) * 0.18f
                    )
                    "left_leg" -> PoseTransform(rx = -fSin2)
                    else -> PoseTransform()
                }
            }

            ZOMBIE -> when (partName) {
                "left_arm" -> PoseTransform(
                    rx = sin(t * 1.6f) * 0.04f - 1.57f,
                    ry = 0.08f
                )
                "head" -> PoseTransform(rx = 0.12f)
                "right_arm" -> PoseTransform(
                    rx = sin(t * 1.6f) * 0.04f - 1.57f,
                    ry = -0.08f
                )
                else -> PoseTransform()
            }

            SNEAK -> when (partName) {
                "right_arm" -> PoseTransform(rx = 0.4f, ty = 2.0f)
                "right_leg" -> PoseTransform(rx = -0.05f, tz = -2.5f)
                "body" -> PoseTransform(rx = 0.5f, ty = 1.0f)
                "head" -> PoseTransform(ty = 2.6f)
                "left_arm" -> PoseTransform(rx = 0.4f, ty = 2.0f)
                "left_leg" -> PoseTransform(rx = -0.05f, tz = -2.5f)
                else -> PoseTransform()
            }

            WALK -> {
                val f3 = 4.8f * t
                when (partName) {
                    "right_arm" -> PoseTransform(rx = cos(f3) * 0.65f)
                    "right_leg" -> PoseTransform(rx = -cos(f3) * 0.7f)
                    "head" -> PoseTransform(ry = sin(f3 * 0.5f) * 0.04f)
                    "root" -> PoseTransform(ty = abs(sin(f3)) * AMBIENT_R)
                    "left_arm" -> PoseTransform(rx = -cos(f3) * 0.65f)
                    "left_leg" -> PoseTransform(rx = cos(f3) * 0.7f)
                    else -> PoseTransform()
                }
            }
        }
    }
}
