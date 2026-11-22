/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit quaternion with (w, x, y, z) component order, for column-vector math.
 * Euler order is yaw (Y), then pitch (X), then roll (Z). See README.md.
 */
package me.shadow.eclipselauncher.pose

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Unit quaternion in `(w, x, y, z)` order. */
data class Quaternion(
    val w: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
) {
    companion object {
        val IDENTITY = Quaternion()

        /**
         * Rotation of [angleRadians] around [axis]. The axis is normalized;
         * a zero axis produces the identity rotation.
         */
        fun fromAxisAngle(axis: Vec3, angleRadians: Float): Quaternion {
            val axisLength = axis.length()
            if (axisLength == 0f) return IDENTITY
            val normalizedAxis = axis / axisLength
            val halfAngle = angleRadians * 0.5f
            val sinHalf = sin(halfAngle)
            return Quaternion(
                w = cos(halfAngle),
                x = normalizedAxis.x * sinHalf,
                y = normalizedAxis.y * sinHalf,
                z = normalizedAxis.z * sinHalf,
            )
        }

        /**
         * Builds `q_roll * q_pitch * q_yaw`, so a column vector is rotated by
         * yaw first, then pitch, then roll. Angles are radians.
         */
        fun fromEuler(yaw: Float, pitch: Float, roll: Float): Quaternion {
            val cy = cos(yaw * 0.5f)
            val sy = sin(yaw * 0.5f)
            val cp = cos(pitch * 0.5f)
            val sp = sin(pitch * 0.5f)
            val cr = cos(roll * 0.5f)
            val sr = sin(roll * 0.5f)

            return Quaternion(
                w = cr * cp * cy - sr * sp * sy,
                x = cr * sp * cy - sr * cp * sy,
                y = cr * cp * sy + sr * sp * cy,
                z = cr * sp * sy + sr * cp * cy,
            )
        }
    }

    fun lengthSquared(): Float = w * w + x * x + y * y + z * z

    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Quaternion {
        val length = length()
        if (length == 0f) return IDENTITY
        val inverseLength = 1f / length
        return Quaternion(w * inverseLength, x * inverseLength, y * inverseLength, z * inverseLength)
    }

    fun conjugated(): Quaternion = Quaternion(w, -x, -y, -z)

    /** Hamilton product: applies [other] first, then this rotation. */
    fun times(other: Quaternion): Quaternion = Quaternion(
        w = w * other.w - x * other.x - y * other.y - z * other.z,
        x = w * other.x + x * other.w + y * other.z - z * other.y,
        y = w * other.y - x * other.z + y * other.w + z * other.x,
        z = w * other.z + x * other.y - y * other.x + z * other.w,
    )

    /** Rotates [vector] with this unit quaternion (column-vector convention). */
    fun rotate(vector: Vec3): Vec3 {
        val unit = normalized()
        val vectorPart = Vec3(unit.x, unit.y, unit.z)
        val twiceCross = vectorPart.cross(vector) * 2f
        return vector + twiceCross * unit.w + vectorPart.cross(twiceCross)
    }

    /** Rotation matrix for column-vector transforms. */
    fun toMatrix(): Mat4 {
        val unit = normalized()
        val w = unit.w
        val x = unit.x
        val y = unit.y
        val z = unit.z
        return Mat4(
            1f - 2f * (y * y + z * z), 2f * (x * y - w * z), 2f * (x * z + w * y), 0f,
            2f * (x * y + w * z), 1f - 2f * (x * x + z * z), 2f * (y * z - w * x), 0f,
            2f * (x * z - w * y), 2f * (y * z + w * x), 1f - 2f * (x * x + y * y), 0f,
            0f, 0f, 0f, 1f,
        )
    }
}
