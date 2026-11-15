/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Pure JVM tests for the cape mesh: vanilla size and position, the 22x17
 * unwrap regions, the outward tilt, no-cape behavior, and which side is
 * visible plus the painter order from front, back, side and upside down.
 */
package me.shadow.eclipselauncher.ui.character

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapeModelTest {

    private data class FaceInfo(
        val capeFace: CapeFace?,
        val part: PartKind,
        val slot: Int,
        val front: Boolean,
        val depth: Float,
        val u: Int,
        val v: Int,
        val w: Int,
        val h: Int,
        val xs: List<Float>,
        val ys: List<Float>,
    )

    private class Snapshot(faces: Array<RenderFace>) {
        val order: List<FaceInfo> = faces.map { face ->
            FaceInfo(
                capeFace = face.capeFace,
                part = face.part,
                slot = face.slot,
                front = face.isFrontFacing(),
                depth = face.depth,
                u = face.u,
                v = face.v,
                w = face.w,
                h = face.h,
                xs = (0 until 4).map { face.pts[it * 2] },
                ys = (0 until 4).map { face.pts[it * 2 + 1] },
            )
        }
        val cape: List<FaceInfo> = order.filter { it.capeFace != null }
        val frontCape: Set<CapeFace> = cape.filter { it.front }.mapNotNull { it.capeFace }.toSet()

        fun indexOf(face: CapeFace): Int = order.indexOfFirst { it.capeFace == face }

        fun indicesOf(part: PartKind, slot: Int): List<Int> =
            order.withIndex()
                .filter { it.value.part == part && it.value.slot == slot }
                .map { it.index }
    }

    private fun pose(yaw: Float = 0f, pitch: Float = 0f) = Pose(
        yaw = yaw,
        headYaw = 0f,
        headPitch = 0f,
        armSwing = 0f,
        breath = 0f,
        bodyPitch = pitch,
    )

    private fun snapshot(rig: PlayerRig, pose: Pose): Snapshot =
        Snapshot(rig.project(pose, 0f, 0f, 1f))

    @Test
    fun noCapeBuildsNoCapeFaces() {
        val rig = PlayerRig()
        assertEquals(0, rig.capeFaceCount)
        assertTrue(snapshot(rig, pose()).cape.isEmpty())
    }

    @Test
    fun capeHasSixFacesAtVanillaWidthAndShoulderHeight() {
        val rig = PlayerRig(hasCape = true)
        assertEquals(6, rig.capeFaceCount)
        assertEquals(10f, CapeGeometry.WIDTH, 0f)
        assertEquals(16f, CapeGeometry.HEIGHT, 0f)
        assertEquals(1f, CapeGeometry.THICKNESS, 0f)

        val snap = snapshot(rig, pose())
        assertEquals(6, snap.cape.size)
        assertEquals(
            CapeFace.entries.toSet(),
            snap.cape.mapNotNull { it.capeFace }.toSet(),
        )

        val xs = snap.cape.flatMap { it.xs }
        assertEquals(-5f, xs.min(), 0.001f)
        assertEquals(5f, xs.max(), 0.001f)

        val sinTilt = sin(CapeGeometry.TILT_RADIANS.toDouble()).toFloat()
        val cosTilt = cos(CapeGeometry.TILT_RADIANS.toDouble()).toFloat()
        val topOuterScreenY = 16f - (CapeGeometry.TOP_Y + sinTilt)
        val bottomInnerScreenY = 16f - (CapeGeometry.TOP_Y - CapeGeometry.HEIGHT * cosTilt)
        val ys = snap.cape.flatMap { it.ys }
        assertEquals("cape top hangs at the shoulder line", topOuterScreenY, ys.min(), 0.001f)
        assertEquals("cape hem stops at 16 px", bottomInnerScreenY, ys.max(), 0.001f)
        assertEquals(
            "tilt shortens the height by cos(6 degrees) and lifts the outer top by sin",
            CapeGeometry.HEIGHT * cosTilt + sinTilt,
            ys.max() - ys.min(),
            0.01f,
        )
    }

    @Test
    fun capeFacesUseTheVanilla22x17Unwrap() {
        val expected = mapOf(
            CapeFace.OUTER to intArrayOf(1, 1, 10, 16),
            CapeFace.INNER to intArrayOf(12, 1, 10, 16),
            CapeFace.LEFT to intArrayOf(0, 1, 1, 16),
            CapeFace.RIGHT to intArrayOf(11, 1, 1, 16),
            CapeFace.TOP to intArrayOf(1, 0, 10, 1),
            CapeFace.BOTTOM to intArrayOf(11, 0, 10, 1),
        )
        val snap = snapshot(PlayerRig(hasCape = true), pose())
        for ((face, rect) in expected) {
            val actual = snap.cape.single { it.capeFace == face }
            assertEquals("$face u", rect[0], actual.u)
            assertEquals("$face v", rect[1], actual.v)
            assertEquals("$face w", rect[2], actual.w)
            assertEquals("$face h", rect[3], actual.h)
        }
    }

    @Test
    fun hdCapeMultipliesEveryUvRectangle() {
        val snap = snapshot(PlayerRig(hasCape = true, capeUvScale = 2), pose())
        val outer = snap.cape.single { it.capeFace == CapeFace.OUTER }
        val inner = snap.cape.single { it.capeFace == CapeFace.INNER }
        val left = snap.cape.single { it.capeFace == CapeFace.LEFT }
        assertEquals(listOf(2, 2, 20, 32), listOf(outer.u, outer.v, outer.w, outer.h))
        assertEquals(listOf(24, 2, 20, 32), listOf(inner.u, inner.v, inner.w, inner.h))
        assertEquals(listOf(0, 2, 2, 32), listOf(left.u, left.v, left.w, left.h))
    }

    @Test
    fun capeSitsBehindTheJacketAndTiltsItsHemAway() {
        val rig = PlayerRig(hasCape = true)
        val snap = snapshot(rig, pose())
        val outer = snap.cape.single { it.capeFace == CapeFace.OUTER }
        val inner = snap.cape.single { it.capeFace == CapeFace.INNER }

        assertTrue("outer face must be farther than the inner face", outer.depth < inner.depth)
        assertTrue(
            "cape must sit behind the jacket overlay at z=-2.25",
            inner.depth < -2.25f,
        )
        assertEquals(CapeGeometry.INNER_Z, CapeGeometry.OUTER_Z + CapeGeometry.THICKNESS, 0.0001f)
        assertEquals(CapeGeometry.TOP_Y, CapeGeometry.tiltedY(0f), 0.0001f)
        assertEquals(CapeGeometry.INNER_Z, CapeGeometry.tiltedZ(0f), 0.0001f)
        assertTrue(
            "hem must move more than 1 px away from the back",
            CapeGeometry.tiltedZ(CapeGeometry.HEIGHT) < CapeGeometry.INNER_Z - 1f,
        )
        assertTrue(CapeGeometry.tiltedY(CapeGeometry.HEIGHT) in 8f..8.2f)
    }

    @Test
    fun positiveProjectedWindingIsTheFrontFacingSide() {
        val snap = snapshot(PlayerRig(), pose())
        val bodyFront = snap.order.first { it.part == PartKind.BODY && it.slot == 0 }
        val bodyBack = snap.order.first { it.part == PartKind.BODY && it.slot == 1 }
        assertTrue("model's +Z face is front-facing at yaw 0", bodyFront.front)
        assertFalse("model's -Z face is back-facing at yaw 0", bodyBack.front)
    }

    @Test
    fun frontViewHidesTheOuterCapeFaceBehindTheTorso() {
        val snap = snapshot(PlayerRig(hasCape = true), pose(yaw = 0f))
        assertFalse(CapeFace.OUTER in snap.frontCape)
        assertTrue(CapeFace.INNER in snap.frontCape)
        assertTrue(snap.indexOf(CapeFace.OUTER) < snap.indexOf(CapeFace.INNER))
        val torsoFront = snap.indicesOf(PartKind.BODY, 0)
        assertTrue(torsoFront.isNotEmpty())
        assertTrue(torsoFront.all { snap.indexOf(CapeFace.OUTER) < it })
    }

    @Test
    fun backViewShowsTheOuterCapeFaceLast() {
        val snap = snapshot(PlayerRig(hasCape = true), pose(yaw = PI.toFloat()))
        assertTrue(CapeFace.OUTER in snap.frontCape)
        assertFalse(CapeFace.INNER in snap.frontCape)
        assertTrue(snap.indexOf(CapeFace.OUTER) > snap.indexOf(CapeFace.INNER))
        val torsoBack = snap.indicesOf(PartKind.BODY, 1)
        assertTrue(torsoBack.isNotEmpty())
        assertTrue(torsoBack.all { snap.indexOf(CapeFace.OUTER) > it })
    }

    @Test
    fun sideViewShowsTheNearCapeStrip() {
        val snap = snapshot(PlayerRig(hasCape = true), pose(yaw = (PI / 2).toFloat()))
        assertTrue(CapeFace.RIGHT in snap.frontCape)
        assertFalse(CapeFace.LEFT in snap.frontCape)
        assertTrue(snap.indexOf(CapeFace.RIGHT) > snap.indexOf(CapeFace.LEFT))
    }

    @Test
    fun upsideDownViewShowsTheOuterCapeFace() {
        val snap = snapshot(PlayerRig(hasCape = true), pose(pitch = PI.toFloat()))
        assertTrue(CapeFace.OUTER in snap.frontCape)
        assertTrue(snap.indexOf(CapeFace.OUTER) > snap.indexOf(CapeFace.INNER))
        // Flipped, the 1 px hem strip is nearest among the cape faces, with
        // the outer face right behind it; both draw after the inner face.
        val capeOrder = snap.cape.mapNotNull { it.capeFace }
        assertEquals(CapeFace.BOTTOM, capeOrder.last())
        assertEquals(CapeFace.OUTER, capeOrder[capeOrder.size - 2])
    }
}
