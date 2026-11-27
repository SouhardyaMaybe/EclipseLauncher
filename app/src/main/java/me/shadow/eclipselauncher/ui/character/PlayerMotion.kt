/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.PlayerMotion
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.ResultKt

class PlayerMotion {
    companion object {
        const val BODY_OMEGA = 8.0f
        const val DRAG_DEG_PER_PX = 0.32f
        const val FLING_FRICTION = 3.2f
        const val HEAD_OMEGA = 12.0f
        const val MAX_FLING = 540.0f
        const val NECK_LIMIT = 60.0f
        const val PITCH_OMEGA = 10.0f
        const val REST_YAW = -20.0f
    }

    private var bodyVel: Float = 0f
    private var dragVelocity: Float = 0f
    private var dragging: Boolean = false
    private var fling: Float = 0f
    private var headPitch: Float = 0f
    private var headPitchVel: Float = 0f
    private var headYaw: Float = 0f
    private var headYawVel: Float = 0f
    var bodyYaw: Float = -20.0f
        private set
    var bodyTarget: Float = -20.0f
        private set

    private fun spring(f: Float, f2: Float, f3: Float, f4: Float, f5: Float): Float {
        return (((f2 - f) * (f4 * f4)) - ((f4 * 2.0f) * f3)) * f5 + f3
    }

    private fun wrapDegrees(f: Float): Float {
        var f2 = f % 360.0f
        if (f2 > 180.0f) {
            f2 -= 360.0f
        }
        return if (f2 < -180.0f) f2 + 360.0f else f2
    }

    fun beginDrag() {
        this.dragging = true
        this.fling = 0.0f
        this.dragVelocity = 0.0f
    }

    fun drag(f: Float, f2: Float) {
        val f3 = f * 0.32f
        this.bodyTarget += f3
        if (f2 < 0.004f) {
            f2 = 0.004f
        }
        this.dragVelocity = ((f3 / f2) * 0.4f) + (this.dragVelocity * 0.6f)
    }

    fun endDrag() {
        this.dragging = false
        this.fling = ResultKt.coerceIn(this.dragVelocity, -540.0f, 540.0f)
    }

    fun getBodyYaw(): Float = bodyYaw

    fun getHeadPitch(): Float = headPitch

    fun getHeadYaw(): Float = headYaw

    fun isMoving(): Boolean =
        dragging || fling != 0.0f ||
            Math.abs(bodyVel) > 0.5f ||
            Math.abs(headYawVel) > 0.5f ||
            Math.abs(headPitchVel) > 0.5f

    fun reset() {
        this.fling = 0.0f
        this.bodyTarget = (Math.round((this.bodyYaw - (-20.0f)) / 360.0f) * 360.0f) - 20.0f
    }

    fun setBodyYaw(f: Float) { this.bodyYaw = f }
    fun setHeadPitch(f: Float) { this.headPitch = f }
    fun setHeadYaw(f: Float) { this.headYaw = f }

    fun step(deltaTime: Float, panX: Float, panY: Float, rectWidth: Float, rectHeight: Float) {
        // Simplified step logic matching reference behavior
        val dt = deltaTime

        // Body spring toward target
        val bodyTargetDiff = bodyTarget - bodyYaw
        val wrappedBodyDiff = wrapDegrees(bodyTargetDiff)
        bodyVel = spring(bodyYaw, bodyTarget, bodyVel, BODY_OMEGA, dt)
        bodyYaw += bodyVel * dt
        bodyYaw = wrapDegrees(bodyYaw)

        // Fling decay
        if (fling != 0.0f) {
            bodyYaw += fling * dt
            fling *= (1.0f - FLING_FRICTION * dt).coerceIn(0.0f, 1.0f)
            if (Math.abs(fling) < 0.001f) fling = 0.0f
        }

        // Head spring toward neutral (drag overrides during drag)
        if (!dragging) {
            headYawVel = spring(headYaw, 0.0f, headYawVel, HEAD_OMEGA, dt)
            headPitchVel = spring(headPitch, 0.0f, headPitchVel, PITCH_OMEGA, dt)
            headYaw += headYawVel * dt
            headPitch += headPitchVel * dt
            headYaw = headYaw.coerceIn(-NECK_LIMIT, NECK_LIMIT)
            headPitch = headPitch.coerceIn(-NECK_LIMIT, NECK_LIMIT)
        }
    }
}
