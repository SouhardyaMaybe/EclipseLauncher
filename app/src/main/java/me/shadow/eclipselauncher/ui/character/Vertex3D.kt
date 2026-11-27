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
        return Float.compare(x, other.x) == 0 &&
            Float.compare(y, other.y) == 0 &&
            Float.compare(z, other.z) == 0 &&
            Float.compare(u, other.u) == 0 &&
            Float.compare(v, other.v) == 0
    }

    override fun hashCode(): Int {
        var result = Float.floatToIntBits(x)
        result = result * 31 + Float.floatToIntBits(y)
        result = result * 31 + Float.floatToIntBits(z)
        result = result * 31 + Float.floatToIntBits(u)
        result = result * 31 + Float.floatToIntBits(v)
        return result
    }

    override fun toString(): String {
        return "Vertex3D(x=$x, y=$y, z=$z, u=$u, v=$v)"
    }
}
