/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Vanilla cape geometry and the cape texture's box-unwrap regions.
 * Dimensions/tilt: https://minecraft.wiki/w/Cape — 16x10x1 pixels, attached
 * to the top of the back, bottom separated from the back by a few degrees.
 * The 22x17 UV regions were verified against the wiki's 64x32 cape textures
 * and ZL2 PetLib's 10x16x1 box at uv (0,0) with a 180-degree Y rotation.
 * Pure Kotlin so the mesh stays unit-testable.
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.math.cos
import kotlin.math.sin

/** The six quads of the cape box, named from the character's perspective. */
enum class CapeFace {
    /** -Z: away from the body, visible from behind. */
    OUTER,

    /** +Z: toward the body. */
    INNER,

    TOP,
    BOTTOM,

    /** +X: the character's left cheek of the cape. */
    LEFT,

    /** -X: the character's right cheek of the cape. */
    RIGHT,
}

object CapeGeometry {
    const val WIDTH = 10f
    const val HEIGHT = 16f
    const val THICKNESS = 1f

    /** Shoulder line: the top of the cape hangs here. */
    const val TOP_Y = 24f
    const val BOTTOM_Y = TOP_Y - HEIGHT

    /**
     * The torso's back is z=-2 and the jacket overlay reaches z=-2.25; keep a
     * 0.25 px gap behind the overlay so no cape face coincides with it.
     */
    const val INNER_Z = -2.5f
    const val OUTER_Z = INNER_Z - THICKNESS

    /**
     * "A few degrees" per the wiki. Six degrees keeps the bottom ~1.67 px off
     * the back, so the cape clears the torso without looking detached.
     */
    const val TILT_RADIANS = 0.10471976f

    /** Texture-space rectangle for [face] in the 64x32 base layout. */
    fun uv(face: CapeFace, scale: Int = 1): Uv {
        val base = when (face) {
            CapeFace.OUTER -> Uv(1, 1, 10, 16)
            CapeFace.INNER -> Uv(12, 1, 10, 16)
            CapeFace.TOP -> Uv(1, 0, 10, 1)
            CapeFace.BOTTOM -> Uv(11, 0, 10, 1)
            CapeFace.LEFT -> Uv(0, 1, 1, 16)
            CapeFace.RIGHT -> Uv(11, 1, 1, 16)
        }
        return Uv(base.u * scale, base.v * scale, base.w * scale, base.h * scale)
    }

    /** Model-space y of a point [drop] pixels below the shoulder pivot. */
    fun tiltedY(drop: Float): Float = TOP_Y - drop * cos(TILT_RADIANS)

    /** Model-space z of a point [drop] pixels below the shoulder pivot. */
    fun tiltedZ(drop: Float): Float = INNER_Z - drop * sin(TILT_RADIANS)
}
