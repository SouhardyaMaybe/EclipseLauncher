/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.Mat4
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.math.cos
import kotlin.math.sin

class Mat4 {
    private val m = FloatArray(16)

    init {
        identity()
    }

    fun clone(): Mat4 {
        return Mat4().copyFrom(this)
    }

    fun copyFrom(mat4: Mat4): Mat4 {
        java.lang.System.arraycopy(mat4.m, 0, this.m, 0, 16)
        return this
    }

    fun getM(): FloatArray {
        return m
    }

    fun identity(): Mat4 {
        for (i in 0 until 16) {
            m[i] = 0.0f
        }
        m[0] = 1.0f
        m[5] = 1.0f
        m[10] = 1.0f
        m[15] = 1.0f
        return this
    }

    fun multiply(mat4: Mat4): Mat4 {
        val fArr = FloatArray(16)
        for (i in 0 until 4) {
            for (i2 in 0 until 4) {
                var f = 0.0f
                for (i3 in 0 until 4) {
                    f += this.m[(i3 * 4) + i2] * mat4.m[(i * 4) + i3]
                }
                fArr[(i * 4) + i2] = f
            }
        }
        java.lang.System.arraycopy(fArr, 0, this.m, 0, 16)
        return this
    }

    fun rotateEulerZYX(f: Float, f2: Float, f3: Float): Mat4 {
        val fCos = kotlin.math.cos(f.toDouble()).toFloat()
        val fSin = kotlin.math.sin(f.toDouble()).toFloat()
        val fCos2 = kotlin.math.cos(f2.toDouble()).toFloat()
        val fSin2 = kotlin.math.sin(f2.toDouble()).toFloat()
        val fCos3 = kotlin.math.cos(f3.toDouble()).toFloat()
        val fSin3 = kotlin.math.sin(f3.toDouble()).toFloat()
        val mat4 = Mat4()
        val fArr = mat4.m
        fArr[0] = fCos * fCos2
        fArr[1] = fSin * fCos2
        fArr[2] = -fSin2
        fArr[3] = 0.0f
        val f4 = fCos * fSin2
        fArr[4] = (f4 * fSin3) - (fSin * fCos3)
        val f5 = fSin2 * fSin
        fArr[5] = (fCos * fCos3) + (f5 * fSin3)
        fArr[6] = fCos2 * fSin3
        fArr[7] = 0.0f
        fArr[8] = (fSin * fSin3) + (f4 * fCos3)
        fArr[9] = (f5 * fCos3) - (fCos * fSin3)
        fArr[10] = fCos2 * fCos3
        fArr[11] = 0.0f
        fArr[12] = 0.0f
        fArr[13] = 0.0f
        fArr[14] = 0.0f
        fArr[15] = 1.0f
        return multiply(mat4)
    }

    fun transformPoint(f: Float, f2: Float, f3: Float, fArr: FloatArray, i: Int = 0) {
        val fArr2 = this.m
        fArr[i] = (fArr2[8] * f3) + (fArr2[4] * f2) + (fArr2[0] * f) + fArr2[12]
        fArr[i + 1] = (fArr2[9] * f3) + (fArr2[5] * f2) + (fArr2[1] * f) + fArr2[13]
        fArr[i + 2] = (fArr2[10] * f3) + (fArr2[6] * f2) + (fArr2[2] * f) + fArr2[14]
    }

    fun translate(f: Float, f2: Float, f3: Float): Mat4 {
        val mat4 = Mat4()
        val fArr = mat4.m
        fArr[12] = f
        fArr[13] = f2
        fArr[14] = f3
        return multiply(mat4)
    }
}
