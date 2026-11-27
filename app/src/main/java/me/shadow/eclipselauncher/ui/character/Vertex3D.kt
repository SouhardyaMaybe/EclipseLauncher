/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.Vertex3D
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.jvm.internal.Intrinsics

class Vertex3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val u: Float,
    val v: Float
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vertex3D) return false
        return java.lang.Float.floatToIntBits(x) == java.lang.Float.floatToIntBits(other.x) &&
            java.lang.Float.floatToIntBits(y) == java.lang.Float.floatToIntBits(other.y) &&
            java.lang.Float.floatToIntBits(z) == java.lang.Float.floatToIntBits(other.z) &&
            java.lang.Float.floatToIntBits(u) == java.lang.Float.floatToIntBits(other.u) &&
            java.lang.Float.floatToIntBits(v) == java.lang.Float.floatToIntBits(other.v)
    }

    override fun hashCode(): Int {
        var result = java.lang.Float.floatToIntBits(x)
        result = result * 31 + java.lang.Float.floatToIntBits(y)
        result = result * 31 + java.lang.Float.floatToIntBits(z)
        result = result * 31 + java.lang.Float.floatToIntBits(u)
        result = result * 31 + java.lang.Float.floatToIntBits(v)
        return result
    }

    override fun toString(): String {
        return "Vertex3D(x=$x, y=$y, z=$z, u=$u, v=$v)"
    }
}
