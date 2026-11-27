/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * 4x4 matrix in row-major order (float[16]): M[row * 4 + col].
 * Used for the Compose Canvas 2D player renderer.
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.Mat4
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.math.cos
import kotlin.math.sin

/**
 * 4x4 matrix in row-major order (float[16]): M[row * 4 + col].
 * Used for the Compose Canvas 2D player renderer.
 */
class Mat4 {
    val m = FloatArray(16)

    /** Identity matrix. */
    init {
        identity()
    }

    /** Returns a deep copy of this matrix. */
    fun clone(): Mat4 {
        val c = Mat4()
        c.m[0] = m[0]; c.m[1] = m[1]; c.m[2] = m[2]; c.m[3] = m[3]
        c.m[4] = m[4]; c.m[5] = m[5]; c.m[6] = m[6]; c.m[7] = m[7]
        c.m[8] = m[8]; c.m[9] = m[9]; c.m[10] = m[10]; c.m[11] = m[11]
        c.m[12] = m[12]; c.m[13] = m[13]; c.m[14] = m[14]; c.m[15] = m[15]
        return c
    }

    /** Post-multiplies by a translation. */
    fun translate(tx: Float, ty: Float, tz: Float) {
        m[12] += m[0] * tx + m[4] * ty + m[8] * tz
        m[13] += m[1] * tx + m[5] * ty + m[9] * tz
        m[14] += m[2] * tx + m[6] * ty + m[10] * tz
        m[15] += m[3] * tx + m[7] * ty + m[11] * tz
    }

    /**
     * Post-multiplies by a rotation Rz(rz) * Ry(ry) * Rx(rx).
     * Parameters match the reference: rotateEulerZYX(rz, ry, rx).
     * This matches the reference's call convention: rotateEulerZYX(rz, ry, rx).
     */
    fun rotateEulerZYX(rz: Float, ry: Float, rx: Float) {
        val cosRz = cos(rz); val sinRz = sin(rz)
        val cosRy = cos(ry); val sinRy = sin(ry)
        val cosRx = cos(rx); val sinRx = sin(rx)

        // Reference rotation matrix for Rz(rz) * Ry(ry) * Rx(rx) (row-major).
        // Computed from reference: f=rz, f2=ry, f3=rx
        val r00 = cosRz * cosRy
        val r01 = sinRz * cosRy
        val r02 = -sinRy

        val cosRzSinRy = cosRz * sinRy
        val r10 = cosRzSinRy * sinRx - sinRz * cosRx
        val r11 = cosRz * cosRx + sinRy * sinRz * sinRx
        val r12 = cosRy * sinRx

        val r20 = sinRz * sinRx + cosRzSinRy * cosRx
        val r21 = sinRy * sinRz * cosRx - cosRz * sinRx
        val r22 = cosRy * cosRx

        // Multiply current matrix by R on the right: M' = M * R
        for (row in 0..3) {
            val m0 = m[row * 4 + 0]
            val m1 = m[row * 4 + 1]
            val m2 = m[row * 4 + 2]
            m[row * 4 + 0] = m0 * r00 + m1 * r10 + m2 * r20
            m[row * 4 + 1] = m0 * r01 + m1 * r11 + m2 * r21
            m[row * 4 + 2] = m0 * r02 + m1 * r12 + m2 * r22
            // m[row*4+3] unchanged (always 0 except m[15]=1)
        }
    }

    /** Transforms a point (x,y,z,1) by this matrix; writes into out[0..2]. */
    fun transformPoint(x: Float, y: Float, z: Float, out: FloatArray, outOffset: Int = 0) {
        out[outOffset] = m[0] * x + m[4] * y + m[8] * z + m[12]
        out[outOffset + 1] = m[1] * x + m[5] * y + m[9] * z + m[13]
        out[outOffset + 2] = m[2] * x + m[6] * y + m[10] * z + m[14]
        // w = m[3]*x + m[7]*y + m[11]*z + m[15] — always 1 for affine
    }

    /** In-place transform of a point (x,y,z,1) by this matrix. */
    fun transformPointInPlace(v: FloatArray) {
        val x = v[0]; val y = v[1]; val z = v[2]
        v[0] = m[0] * x + m[4] * y + m[8] * z + m[12]
        v[1] = m[1] * x + m[5] * y + m[9] * z + m[13]
        v[2] = m[2] * x + m[6] * y + m[10] * z + m[14]
    }

    private fun identity() {
        m[0] = 1f; m[1] = 0f; m[2] = 0f; m[3] = 0f
        m[4] = 0f; m[5] = 1f; m[6] = 0f; m[7] = 0f
        m[8] = 0f; m[9] = 0f; m[10] = 1f; m[11] = 0f
        m[12] = 0f; m[13] = 0f; m[14] = 0f; m[15] = 1f
    }
}