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

        // All UV faces should be 3px wide for SLIM (Uv properties are Int)
        assertEquals(3, slimArmRight.uvFront.w)
        assertEquals(3, slimArmRight.uvBack.w)
        assertEquals(3, slimArmRight.uvRight.w)
        assertEquals(3, slimArmRight.uvLeft.w)
        assertEquals(3, slimArmRight.uvTop.w)
        assertEquals(3, slimArmRight.uvBottom.w)
    }

    @Test
    fun slimArmUVs_useCorrectTextureRegions() {
        // SLIM arms use different texture coordinates than CLASSIC
        // Right arm SLIM: front at 44,20 (3x12), back at 51,20 (3x12), etc.
        val slimRig = PlayerRig(hasCape = false, capeUvScale = 1, hasOverlay = false, slim = true)

        val slimArmRight = slimRig.boxes.first { it.kind == PartKind.ARM_RIGHT && it.inflate == 0f }

        // Verify UV coordinates match SLIM skin template
        // Front: 44,20 (3x12)
        assertEquals(44, slimArmRight.uvFront.u)
        assertEquals(20, slimArmRight.uvFront.v)

        // Back: 51,20 (3x12) - in SLIM layout, back is at 51
        assertEquals(51, slimArmRight.uvBack.u)

        // Right side: 40,20 (3x12)
        assertEquals(40, slimArmRight.uvRight.u)

        // Left side: 47,20 (3x12) - 44+3=47
        assertEquals(47, slimArmRight.uvLeft.u)

        // Top: 44,16 (3x4)
        assertEquals(44, slimArmRight.uvTop.u)
        assertEquals(16, slimArmRight.uvTop.v)

        // Bottom: 47,16 (3x4) - 44+3=47
        assertEquals(47, slimArmRight.uvBottom.u)
        assertEquals(16, slimArmRight.uvBottom.v)
    }

    @Test
    fun leftArmSLIM_usesCorrectUVs() {
        val slimRig = PlayerRig(hasCape = false, capeUvScale = 1, hasOverlay = false, slim = true)

        val slimArmLeft = slimRig.boxes.first { it.kind == PartKind.ARM_LEFT && it.inflate == 0f }

        // Left arm SLIM texture coordinates
        // Front: 36,52 (3x12) - different from CLASSIC
        assertEquals(36, slimArmLeft.uvFront.u)
        assertEquals(52, slimArmLeft.uvFront.v)

        // Back: 43,52 (3x12) - 36+3+4=43?
        assertEquals(43, slimArmLeft.uvBack.u)

        // Right side: 32,52 (3x12)
        assertEquals(32, slimArmLeft.uvRight.u)

        // Left side: 39,52 (3x12) - 36+3=39
        assertEquals(39, slimArmLeft.uvLeft.u)

        // Top: 36,48 (3x4)
        assertEquals(36, slimArmLeft.uvTop.u)
        assertEquals(48, slimArmLeft.uvTop.v)

        // Bottom: 39,48 (3x4) - 36+3=39
        assertEquals(39, slimArmLeft.uvBottom.u)
        assertEquals(48, slimArmLeft.uvBottom.v)
    }
}