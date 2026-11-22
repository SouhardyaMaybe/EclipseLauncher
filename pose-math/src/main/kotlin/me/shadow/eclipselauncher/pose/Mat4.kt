/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Column-vector 4x4 matrix. Element mRC is row R, column C. See README.md.
 */
package me.shadow.eclipselauncher.pose

import kotlin.math.cos
import kotlin.math.sin

/** 4x4 transform for column vectors: `p' = M * p`. */
data class Mat4(
    val m00: Float, val m01: Float, val m02: Float, val m03: Float,
    val m10: Float, val m11: Float, val m12: Float, val m13: Float,
    val m20: Float, val m21: Float, val m22: Float, val m23: Float,
    val m30: Float, val m31: Float, val m32: Float, val m33: Float,
) {
    companion object {
        val IDENTITY = Mat4(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        )

        fun translation(x: Float, y: Float, z: Float): Mat4 = Mat4(
            1f, 0f, 0f, x,
            0f, 1f, 0f, y,
            0f, 0f, 1f, z,
            0f, 0f, 0f, 1f,
        )

        fun translation(offset: Vec3): Mat4 = translation(offset.x, offset.y, offset.z)

        /** Right-handed rotation around +X: +Y maps to +Z. */
        fun rotationX(angleRadians: Float): Mat4 {
            val c = cos(angleRadians)
            val s = sin(angleRadians)
            return Mat4(
                1f, 0f, 0f, 0f,
                0f, c, -s, 0f,
                0f, s, c, 0f,
                0f, 0f, 0f, 1f,
            )
        }

        /** Right-handed rotation around +Y: +Z maps to +X. */
        fun rotationY(angleRadians: Float): Mat4 {
            val c = cos(angleRadians)
            val s = sin(angleRadians)
            return Mat4(
                c, 0f, s, 0f,
                0f, 1f, 0f, 0f,
                -s, 0f, c, 0f,
                0f, 0f, 0f, 1f,
            )
        }

        /** Right-handed rotation around +Z: +X maps to +Y. */
        fun rotationZ(angleRadians: Float): Mat4 {
            val c = cos(angleRadians)
            val s = sin(angleRadians)
            return Mat4(
                c, -s, 0f, 0f,
                s, c, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            )
        }

        /** Rotation of [rotation] around [pivot]: `T(pivot) * R * T(-pivot)`. */
        fun aroundPivot(rotation: Mat4, pivot: Vec3): Mat4 =
            translation(pivot) * rotation * translation(-pivot)
    }

    /** Matrix product: applies [other] first, then this. */
    operator fun times(other: Mat4): Mat4 = Mat4(
        m00 * other.m00 + m01 * other.m10 + m02 * other.m20 + m03 * other.m30,
        m00 * other.m01 + m01 * other.m11 + m02 * other.m21 + m03 * other.m31,
        m00 * other.m02 + m01 * other.m12 + m02 * other.m22 + m03 * other.m32,
        m00 * other.m03 + m01 * other.m13 + m02 * other.m23 + m03 * other.m33,

        m10 * other.m00 + m11 * other.m10 + m12 * other.m20 + m13 * other.m30,
        m10 * other.m01 + m11 * other.m11 + m12 * other.m21 + m13 * other.m31,
        m10 * other.m02 + m11 * other.m12 + m12 * other.m22 + m13 * other.m32,
        m10 * other.m03 + m11 * other.m13 + m12 * other.m23 + m13 * other.m33,

        m20 * other.m00 + m21 * other.m10 + m22 * other.m20 + m23 * other.m30,
        m20 * other.m01 + m21 * other.m11 + m22 * other.m21 + m23 * other.m31,
        m20 * other.m02 + m21 * other.m12 + m22 * other.m22 + m23 * other.m32,
        m20 * other.m03 + m21 * other.m13 + m22 * other.m23 + m23 * other.m33,

        m30 * other.m00 + m31 * other.m10 + m32 * other.m20 + m33 * other.m30,
        m30 * other.m01 + m31 * other.m11 + m32 * other.m21 + m33 * other.m31,
        m30 * other.m02 + m31 * other.m12 + m32 * other.m22 + m33 * other.m32,
        m30 * other.m03 + m31 * other.m13 + m32 * other.m23 + m33 * other.m33,
    )

    /** Transforms a point (`w = 1`). */
    fun transformPoint(point: Vec3): Vec3 = Vec3(
        m00 * point.x + m01 * point.y + m02 * point.z + m03,
        m10 * point.x + m11 * point.y + m12 * point.z + m13,
        m20 * point.x + m21 * point.y + m22 * point.z + m23,
    )

    /** Transforms a direction (`w = 0`), ignoring translation. */
    fun transformVector(vector: Vec3): Vec3 = Vec3(
        m00 * vector.x + m01 * vector.y + m02 * vector.z,
        m10 * vector.x + m11 * vector.y + m12 * vector.z,
        m20 * vector.x + m21 * vector.y + m22 * vector.z,
    )
}
