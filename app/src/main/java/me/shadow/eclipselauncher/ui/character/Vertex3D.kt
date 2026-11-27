/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * A single vertex in model space (32-unit coordinates, +Y up, +Z front).
 * UV coordinates are in texture pixels (0–64), converted to 0..1 at draw time.
 */
package me.shadow.eclipselauncher.ui.character

data class Vertex3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val u: Float,
    val v: Float
) {
    companion object {
        fun create(x: Float, y: Float, z: Float, u: Float, v: Float): Vertex3D {
            return Vertex3D(x, y, z, u, v)
        }
    }
}