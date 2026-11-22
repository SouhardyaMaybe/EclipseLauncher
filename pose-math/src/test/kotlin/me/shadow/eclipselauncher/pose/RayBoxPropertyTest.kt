/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Property tests for ray/oriented-box picking.
 */
package me.shadow.eclipselauncher.pose

import kotlin.math.PI
import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RayBoxPropertyTest {
    private val random = Random(0xB0B)

    private fun randomAngle(): Float = (random.nextFloat() * 2f - 1f) * PI.toFloat()

    private fun randomVector(scale: Float = 10f): Vec3 = Vec3(
        (random.nextFloat() * 2f - 1f) * scale,
        (random.nextFloat() * 2f - 1f) * scale,
        (random.nextFloat() * 2f - 1f) * scale,
    )

    private fun randomUnitVector(): Vec3 {
        while (true) {
            val candidate = randomVector(scale = 1f)
            if (candidate.lengthSquared() > 0.01f) return candidate.normalized()
        }
    }

    private fun randomBox(): OrientedBox {
        val rotation = Quaternion.fromAxisAngle(randomUnitVector(), randomAngle()).toMatrix()
        return OrientedBox(
            center = randomVector(scale = 10f),
            halfExtents = Vec3(
                random.nextFloat() * 4f + 0.5f,
                random.nextFloat() * 4f + 0.5f,
                random.nextFloat() * 4f + 0.5f,
            ),
            axisX = rotation.transformVector(Vec3.X).normalized(),
            axisY = rotation.transformVector(Vec3.Y).normalized(),
            axisZ = rotation.transformVector(Vec3.Z).normalized(),
        )
    }

    @Test
    fun rayThroughTheCentreHitsTheChosenFaceAtTheExpectedParameter() {
        repeat(200) {
            val box = randomBox()
            val face = random.nextInt(3)
            val sign = if (random.nextBoolean()) 1f else -1f

            val axis = when (face) {
                0 -> box.axisX
                1 -> box.axisY
                else -> box.axisZ
            }
            val halfExtent = when (face) {
                0 -> box.halfExtents.x
                1 -> box.halfExtents.y
                else -> box.halfExtents.z
            }
            val distance = random.nextFloat() * 5f + 0.5f
            val ray = Ray(
                origin = box.center + axis * (sign * (halfExtent + distance)),
                direction = axis * -sign,
            )

            val hit = RayBoxPicker.intersect(ray, box)

            assertNotNull(hit)
            assertEquals(distance, hit!!, 1e-2f)
            val local = ray.pointAt(hit) - box.center
            assertEquals(halfExtent, abs(local.dot(axis)), 1e-2f)
        }
    }

    @Test
    fun everyHitPointIsInsideTheBox() {
        repeat(200) {
            val box = randomBox()
            val axis = box.axisX
            val distance = random.nextFloat() * 5f + 0.5f
            val ray = Ray(
                origin = box.center + axis * (box.halfExtents.x + distance),
                direction = -axis,
            )

            val hit = RayBoxPicker.intersect(ray, box)

            assertNotNull(hit)
            val local = ray.pointAt(hit!!) - box.center
            assertTrue(abs(local.dot(box.axisX)) <= box.halfExtents.x + 1e-2f)
            assertTrue(abs(local.dot(box.axisY)) <= box.halfExtents.y + 1e-2f)
            assertTrue(abs(local.dot(box.axisZ)) <= box.halfExtents.z + 1e-2f)
        }
    }

    @Test
    fun aRayOffsetBeyondTheBoxMisses() {
        repeat(200) {
            val box = randomBox()
            val distance = random.nextFloat() * 5f + 0.5f
            val ray = Ray(
                origin = box.center +
                    box.axisX * (box.halfExtents.x + distance) +
                    box.axisY * (box.halfExtents.y + 1f),
                direction = -box.axisX,
            )

            assertNull(RayBoxPicker.intersect(ray, box))
        }
    }

    @Test
    fun aRayPointingAwayFromTheBoxMisses() {
        repeat(200) {
            val box = randomBox()
            val distance = random.nextFloat() * 5f + 0.5f
            val ray = Ray(
                origin = box.center + box.axisX * (box.halfExtents.x + distance),
                direction = box.axisX,
            )

            assertNull(RayBoxPicker.intersect(ray, box))
        }
    }
}
