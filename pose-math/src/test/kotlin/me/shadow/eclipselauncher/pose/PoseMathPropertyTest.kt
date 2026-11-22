/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Property tests for the pose math primitives and the FK chain. These run
 * before the hand-calculated examples and use a fixed seed.
 */
package me.shadow.eclipselauncher.pose

import kotlin.math.PI
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoseMathPropertyTest {
    private val random = Random(0x5EED)
    private val skeleton = Skeleton()

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

    private fun assertVec(expected: Vec3, actual: Vec3, epsilon: Float = 1e-3f) {
        assertEquals("x", expected.x, actual.x, epsilon)
        assertEquals("y", expected.y, actual.y, epsilon)
        assertEquals("z", expected.z, actual.z, epsilon)
    }

    @Test
    fun quaternionRotationPreservesLength() {
        repeat(200) {
            val rotation = Quaternion.fromAxisAngle(randomUnitVector(), randomAngle())
            val point = randomVector()
            assertEquals(point.length(), rotation.rotate(point).length(), 1e-3f)
        }
    }

    @Test
    fun axisAngleLeavesItsAxisFixed() {
        repeat(200) {
            val axis = randomUnitVector()
            val rotated = Quaternion.fromAxisAngle(axis, randomAngle()).rotate(axis)
            assertVec(axis, rotated)
        }
    }

    @Test
    fun quaternionRotationMatchesItsMatrix() {
        repeat(200) {
            val rotation = Quaternion.fromAxisAngle(randomUnitVector(), randomAngle())
            val point = randomVector()
            assertVec(rotation.rotate(point), rotation.toMatrix().transformVector(point))
        }
    }

    @Test
    fun matrixProductAppliesTheRightOperandFirst() {
        repeat(200) {
            val first = Mat4.translation(randomVector(scale = 5f)) *
                Mat4.rotationX(randomAngle()) *
                Mat4.rotationY(randomAngle())
            val second = Mat4.translation(randomVector(scale = 5f)) *
                Mat4.rotationZ(randomAngle()) *
                Mat4.rotationX(randomAngle())
            val point = randomVector()

            assertVec(
                first.transformPoint(second.transformPoint(point)),
                (first * second).transformPoint(point),
            )
        }
    }

    @Test
    fun pivotRotationKeepsThePivotFixed() {
        repeat(200) {
            val pivot = randomVector(scale = 20f)
            val rotation = Quaternion.fromAxisAngle(randomUnitVector(), randomAngle()).toMatrix()
            val transform = Mat4.aroundPivot(rotation, pivot)

            assertVec(pivot, transform.transformPoint(pivot))
        }
    }

    @Test
    fun pivotRotationOnlyRotatesTheOffsetFromThePivot() {
        repeat(200) {
            val pivot = randomVector(scale = 20f)
            val point = randomVector(scale = 20f)
            val rotation = Quaternion.fromAxisAngle(randomUnitVector(), randomAngle()).toMatrix()

            assertVec(
                pivot + rotation.transformVector(point - pivot),
                Mat4.aroundPivot(rotation, pivot).transformPoint(point),
            )
        }
    }

    @Test
    fun childPivotsFollowTheParentAndStayFixedInTheirOwnFrame() {
        val children = listOf(
            Joint.HEAD,
            Joint.ARM_LEFT,
            Joint.ARM_RIGHT,
            Joint.LEG_LEFT,
            Joint.LEG_RIGHT,
        )

        repeat(100) {
            val pose = SkeletonPose(
                body = JointAngles(randomAngle(), randomAngle(), randomAngle()),
                head = JointAngles(randomAngle(), randomAngle(), randomAngle()),
                armLeft = JointAngles(randomAngle(), randomAngle(), randomAngle()),
                armRight = JointAngles(randomAngle(), randomAngle(), randomAngle()),
                legLeft = JointAngles(randomAngle(), randomAngle(), randomAngle()),
                legRight = JointAngles(randomAngle(), randomAngle(), randomAngle()),
            )
            val world = skeleton.computeWorldTransforms(pose)

            assertVec(
                Skeleton.BODY_PIVOT,
                world.getValue(Joint.BODY).transformPoint(Skeleton.BODY_PIVOT),
            )
            for (joint in children) {
                val pivot = skeleton.pivots.getValue(joint)
                val parent = skeleton.parents.getValue(joint)!!
                assertVec(
                    world.getValue(parent).transformPoint(pivot),
                    world.getValue(joint).transformPoint(pivot),
                )
            }
        }
    }

    @Test
    fun clampingIsIdempotentAndStaysInsideEveryLimit() {
        for (joint in Joint.entries) {
            val limits = skeleton.limits.getValue(joint)
            repeat(100) {
                val angles = JointAngles(
                    yaw = randomAngle() * 3f,
                    pitch = randomAngle() * 3f,
                    roll = randomAngle() * 3f,
                )
                val clamped = skeleton.clamp(joint, angles)

                assertTrue(clamped.yaw >= limits.yaw.min && clamped.yaw <= limits.yaw.max)
                assertTrue(clamped.pitch >= limits.pitch.min && clamped.pitch <= limits.pitch.max)
                assertTrue(clamped.roll >= limits.roll.min && clamped.roll <= limits.roll.max)
                assertEquals(clamped, skeleton.clamp(joint, clamped))
            }
        }
    }

    @Test
    fun localDeltasEqualAddingThenClamping() {
        for (joint in Joint.entries) {
            repeat(50) {
                val start = JointAngles(randomAngle(), randomAngle(), randomAngle())
                val pose = SkeletonPose.IDENTITY.set(joint, start)
                val deltaYaw = randomAngle()
                val deltaPitch = randomAngle()
                val deltaRoll = randomAngle()

                val expected = skeleton.clamp(
                    joint,
                    JointAngles(
                        yaw = start.yaw + deltaYaw,
                        pitch = start.pitch + deltaPitch,
                        roll = start.roll + deltaRoll,
                    ),
                )
                val actual = skeleton.applyLocalDelta(
                    pose,
                    joint,
                    deltaYaw,
                    deltaPitch,
                    deltaRoll,
                ).get(joint)

                assertEquals(expected.yaw, actual.yaw, 1e-5f)
                assertEquals(expected.pitch, actual.pitch, 1e-5f)
                assertEquals(expected.roll, actual.roll, 1e-5f)
            }
        }
    }
}
