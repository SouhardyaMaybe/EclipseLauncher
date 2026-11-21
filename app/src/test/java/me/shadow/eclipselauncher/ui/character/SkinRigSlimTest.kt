/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Real JVM tests for the SLIM skin variant (Alex model):
 * - Arm boxes have correct width (3px vs 4px)
 * - Arm UV coordinates are correct for SLIM (3px wide textures)
 * - Back and bottom faces of arms use correct texture regions
 */
package me.shadow.eclipselauncher.ui.character

import org.junit.Test
import org.junit.Assert.*

class SkinRigSlimTest {

    @Test
    fun slimArmWidth_is3px() {
        // The right arm box for SLIM should be 3 pixels wide (x1 - x0 = 3)
        // CLASSIC is 4 pixels wide (x1 - x0 = 4)
        val slimRig = PlayerRig(hasCape = false, capeUvScale = 1, hasOverlay = false, slim = true)
        val classicRig = PlayerRig(hasCape = false, capeUvScale = 1, hasOverlay = false, slim = false)

        // Check arm right box geometry
        val slimArmRight = slimRig.boxes.first { it.kind == PartKind.ARM_RIGHT && it.inflate == 0f }
        val classicArmRight = classicRig.boxes.first { it.kind == PartKind.ARM_RIGHT && it.inflate == 0f }

        val slimWidth = slimArmRight.x1 - slimArmRight.x0
        val classicWidth = classicArmRight.x1 - classicArmRight.x0

        assertEquals(3f, slimWidth, 0.001f, "SLIM arm should be 3px wide")
        assertEquals(4f, classicWidth, 0.001f, "CLASSIC arm should be 4px wide")
    }

    @Test
    fun slimArmUVs_are3pxWide() {
        // The UV coordinates for SLIM arms should be 3px wide, not 4px
        val slimRig = PlayerRig(hasCape = false, capeUvScale = 1, hasOverlay = false, slim = true)

        val slimArmRight = slimRig.boxes.first { it.kind == PartKind.ARM_RIGHT && it.inflate == 0f }

        // All UV faces should be 3px wide for SLIM
        assertEquals(3f, slimArmRight.front.w, 0.001f, "SLIM front UV should be 3px wide")
        assertEquals(3f, slimArmRight.back.w, 0.001f, "SLIM back UV should be 3px wide")
        assertEquals(3f, slimArmRight.right.w, 0.001f, "SLIM right UV should be 3px wide")
        assertEquals(3f, slimArmRight.left.w, 0.001f, "SLIM left UV should be 3px wide")
        assertEquals(3f, slimArmRight.top.w, 0.001f, "SLIM top UV should be 3px wide")
        assertEquals(3f, slimArmRight.bottom.w, 0.001f, "SLIM bottom UV should be 3px wide")
    }

    @Test
    fun slimArmUVs_useCorrectTextureRegions() {
        // SLIM arms use different texture coordinates than CLASSIC
        // Right arm SLIM: front at 44,20 (3x12), back at 51,20 (3x12), etc.
        val slimRig = PlayerRig(hasCape = false, capeUvScale = 1, hasOverlay = false, slim = true)

        val slimArmRight = slimRig.boxes.first { it.kind == PartKind.ARM_RIGHT && it.inflate == 0f }

        // Verify UV coordinates match SLIM skin template
        // Front: 44,20 (3x12)
        assertEquals(44f, slimArmRight.front.u, 0.001f)
        assertEquals(20f, slimArmRight.front.v, 0.001f)

        // Back: 51,20 (3x12) - 44+3+4=51? Actually in SLIM layout, back is at 51
        assertEquals(51f, slimArmRight.back.u, 0.001f)

        // Right side: 40,20 (3x12)
        assertEquals(40f, slimArmRight.right.u, 0.001f)

        // Left side: 47,20 (3x12) - 44+3=47
        assertEquals(47f, slimArmRight.left.u, 0.001f)

        // Top: 44,16 (3x4)
        assertEquals(44f, slimArmRight.top.u, 0.001f)
        assertEquals(16f, slimArmRight.top.v, 0.001f)

        // Bottom: 47,16 (3x4) - 44+3=47
        assertEquals(47f, slimArmRight.bottom.u, 0.001f)
        assertEquals(16f, slimArmRight.bottom.v, 0.001f)
    }

    @Test
    fun leftArmSLIM_usesCorrectUVs() {
        val slimRig = PlayerRig(hasCape = false, capeUvScale = 1, hasOverlay = false, slim = true)

        val slimArmLeft = slimRig.boxes.first { it.kind == PartKind.ARM_LEFT && it.inflate == 0f }

        // Left arm SLIM texture coordinates
        // Front: 36,52 (3x12) - different from CLASSIC
        assertEquals(36f, slimArmLeft.front.u, 0.001f)
        assertEquals(52f, slimArmLeft.front.v, 0.001f)

        // Back: 43,52 (3x12) - 36+3+4=43?
        assertEquals(43f, slimArmLeft.back.u, 0.001f)

        // Right side: 32,52 (3x12)
        assertEquals(32f, slimArmLeft.right.u, 0.001f)

        // Left side: 39,52 (3x12) - 36+3=39
        assertEquals(39f, slimArmLeft.left.u, 0.001f)

        // Top: 36,48 (3x4)
        assertEquals(36f, slimArmLeft.top.u, 0.001f)
        assertEquals(48f, slimArmLeft.top.v, 0.001f)

        // Bottom: 39,48 (3x4) - 36+3=39
        assertEquals(39f, slimArmLeft.bottom.u, 0.001f)
        assertEquals(48f, slimArmLeft.bottom.v, 0.001f)
    }
}