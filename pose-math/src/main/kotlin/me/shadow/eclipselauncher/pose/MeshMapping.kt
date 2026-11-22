/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Skeleton-to-mesh mapping. Offsets are measured from the rest-pose joint
 * pivot to the box center, in skin pixels. This is the only place where slim
 * (Alex) and classic (Steve) geometry differ; pivots never change.
 */
package me.shadow.eclipselauncher.pose

/** Axis-aligned box in the joint's local rest frame. */
data class MeshBox(
    /** Box center relative to the joint pivot, in rest pose. */
    val offset: Vec3,
    /** Half extents in skin pixels. */
    val halfExtents: Vec3,
)

/** Player box geometry for classic (Steve) or slim (Alex) skins. */
class MeshMapping(val slim: Boolean = false) {
    fun box(joint: Joint): MeshBox = when (joint) {
        Joint.HEAD -> MeshBox(
            offset = Vec3(0f, 4f, 0f),
            halfExtents = Vec3(4f, 4f, 4f),
        )

        Joint.BODY -> MeshBox(
            offset = Vec3(0f, 6f, 0f),
            halfExtents = Vec3(4f, 6f, 2f),
        )

        Joint.ARM_LEFT -> armBox(side = 1f)
        Joint.ARM_RIGHT -> armBox(side = -1f)

        Joint.LEG_LEFT, Joint.LEG_RIGHT -> MeshBox(
            offset = Vec3(0f, -6f, 0f),
            halfExtents = Vec3(2f, 6f, 2f),
        )
    }

    fun boxes(): Map<Joint, MeshBox> = Joint.entries.associateWith { box(it) }

    private fun armBox(side: Float): MeshBox {
        val centerX = if (slim) 0.5f else 1f
        val halfWidth = if (slim) 1.5f else 2f
        return MeshBox(
            offset = Vec3(side * centerX, -4f, 0f),
            halfExtents = Vec3(halfWidth, 6f, 2f),
        )
    }
}
