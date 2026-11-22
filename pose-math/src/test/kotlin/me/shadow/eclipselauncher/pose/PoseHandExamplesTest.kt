/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Hand-calculated examples for the agreed convention, the arm/leg/body-yaw
 * poses, joint limits, the skeleton-to-mesh mapping and picking.
 */
package me.shadow.eclipselauncher.pose

import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PoseHandExamplesTest {
    private val skeleton = Skeleton()
    private val degrees = Skeleton.DEG2RAD
    private val quarterTurn = PI.toFloat() / 2f

    private fun assertVec(expected: Vec3, actual: Vec3, epsilon: Float = 1e-3f) {
        assertEquals("x", expected.x, actual.x, epsilon)
        assertEquals("y", expected.y, actual.y, epsilon)
        assertEquals("z", expected.z, actual.z, epsilon)
    }

    @Test
    fun positiveRotationsFollowTheAgreedRightHandedConvention() {
        // Rx(+90) maps y -> z and z -> -y.
        assertVec(Vec3(0f, 0f, 1f), Mat4.rotationX(quarterTurn).transformPoint(Vec3(0f, 1f, 0f)))
        assertVec(Vec3(0f, -1f, 0f), Mat4.rotationX(quarterTurn).transformPoint(Vec3(0f, 0f, 1f)))

        // Ry(+90) maps z -> x and x -> -z.
        assertVec(Vec3(1f, 0f, 0f), Mat4.rotationY(quarterTurn).transformPoint(Vec3(0f, 0f, 1f)))
        assertVec(Vec3(0f, 0f, -1f), Mat4.rotationY(quarterTurn).transformPoint(Vec3(1f, 0f, 0f)))

        // Rz(+90) maps x -> y and y -> -x.
        assertVec(Vec3(0f, 1f, 0f), Mat4.rotationZ(quarterTurn).transformPoint(Vec3(1f, 0f, 0f)))
        assertVec(Vec3(-1f, 0f, 0f), Mat4.rotationZ(quarterTurn).transformPoint(Vec3(0f, 1f, 0f)))
    }

    @Test
    fun pivotFormulaTurnsTheArmPivotAroundTheBodyPivot() {
        val pivot = Skeleton.BODY_PIVOT
        val point = Skeleton.ARM_LEFT_PIVOT
        val rotation = Mat4.rotationY(quarterTurn)

        val transformed = Mat4.aroundPivot(rotation, pivot).transformPoint(point)

        assertVec(pivot + rotation.transformVector(point - pivot), transformed)
        assertVec(Vec3(0f, 22f, -5f), transformed)
    }

    @Test
    fun leftArmPitchPlusNinetyMovesTheTipToY22ZMinus12() {
        val pose = SkeletonPose(armLeft = JointAngles(pitch = quarterTurn))
        val arm = skeleton.computeWorldTransforms(pose).getValue(Joint.ARM_LEFT)

        assertVec(Vec3(5f, 22f, -12f), arm.transformPoint(Vec3(5f, 10f, 0f)))
        assertVec(Skeleton.ARM_LEFT_PIVOT, arm.transformPoint(Skeleton.ARM_LEFT_PIVOT))
    }

    @Test
    fun leftArmPitchMinusNinetyMovesTheTipForward() {
        val pose = SkeletonPose(armLeft = JointAngles(pitch = -quarterTurn))
        val arm = skeleton.computeWorldTransforms(pose).getValue(Joint.ARM_LEFT)

        assertVec(Vec3(5f, 22f, 12f), arm.transformPoint(Vec3(5f, 10f, 0f)))
    }

    @Test
    fun leftLegPitchPlusNinetySendsTheFootToY12ZMinus12() {
        val pose = SkeletonPose(legLeft = JointAngles(pitch = quarterTurn))
        val leg = skeleton.computeWorldTransforms(pose).getValue(Joint.LEG_LEFT)

        assertVec(Vec3(2f, 12f, -12f), leg.transformPoint(Vec3(2f, 0f, 0f)))
        assertVec(Skeleton.LEG_LEFT_PIVOT, leg.transformPoint(Skeleton.LEG_LEFT_PIVOT))
    }

    @Test
    fun rightLegPitchMinusNinetySendsTheFootForward() {
        val pose = SkeletonPose(legRight = JointAngles(pitch = -quarterTurn))
        val leg = skeleton.computeWorldTransforms(pose).getValue(Joint.LEG_RIGHT)

        assertVec(Vec3(-2f, 12f, 12f), leg.transformPoint(Vec3(-2f, 0f, 0f)))
    }

    @Test
    fun bodyYawNinetySwingsArmsAcrossZAndKeepsCentreJointsOnAxis() {
        val pose = SkeletonPose(body = JointAngles(yaw = quarterTurn))
        val world = skeleton.computeWorldTransforms(pose)

        // Left arm pivot (5, 22, 0) rotates around (0, 12, 0) to (0, 22, -5).
        assertVec(
            Vec3(0f, 22f, -5f),
            world.getValue(Joint.ARM_LEFT).transformPoint(Skeleton.ARM_LEFT_PIVOT),
        )
        // Right arm pivot (-5, 22, 0) goes to (0, 22, 5).
        assertVec(
            Vec3(0f, 22f, 5f),
            world.getValue(Joint.ARM_RIGHT).transformPoint(Skeleton.ARM_RIGHT_PIVOT),
        )
        // Head and body pivots lie on the yaw axis and do not move.
        assertVec(
            Skeleton.HEAD_PIVOT,
            world.getValue(Joint.HEAD).transformPoint(Skeleton.HEAD_PIVOT),
        )
        assertVec(
            Skeleton.BODY_PIVOT,
            world.getValue(Joint.BODY).transformPoint(Skeleton.BODY_PIVOT),
        )
    }

    @Test
    fun slimSkinsChangeTheArmMeshButNeverThePivots() {
        val classic = MeshMapping(slim = false)
        val slim = MeshMapping(slim = true)

        // The skeleton has one set of pivots, shared by both skin types.
        assertVec(Vec3(5f, 22f, 0f), skeleton.pivots.getValue(Joint.ARM_LEFT))
        assertVec(Vec3(-5f, 22f, 0f), skeleton.pivots.getValue(Joint.ARM_RIGHT))

        assertEquals(Vec3(1f, -4f, 0f), classic.box(Joint.ARM_LEFT).offset)
        assertEquals(Vec3(0.5f, -4f, 0f), slim.box(Joint.ARM_LEFT).offset)
        assertEquals(Vec3(2f, 6f, 2f), classic.box(Joint.ARM_LEFT).halfExtents)
        assertEquals(Vec3(1.5f, 6f, 2f), slim.box(Joint.ARM_LEFT).halfExtents)

        assertEquals(Vec3(-1f, -4f, 0f), classic.box(Joint.ARM_RIGHT).offset)
        assertEquals(Vec3(-0.5f, -4f, 0f), slim.box(Joint.ARM_RIGHT).offset)
    }

    @Test
    fun meshCentresMatchTheSkinPixelLayoutAtRest() {
        val classic = skeleton.computePartBoxes(SkeletonPose.IDENTITY, MeshMapping(slim = false))
        val slim = skeleton.computePartBoxes(SkeletonPose.IDENTITY, MeshMapping(slim = true))

        assertVec(Vec3(0f, 28f, 0f), classic.getValue(Joint.HEAD).center)
        assertVec(Vec3(0f, 18f, 0f), classic.getValue(Joint.BODY).center)
        assertVec(Vec3(2f, 6f, 0f), classic.getValue(Joint.LEG_LEFT).center)
        assertVec(Vec3(-2f, 6f, 0f), classic.getValue(Joint.LEG_RIGHT).center)

        assertVec(Vec3(6f, 18f, 0f), classic.getValue(Joint.ARM_LEFT).center)
        assertVec(Vec3(5.5f, 18f, 0f), slim.getValue(Joint.ARM_LEFT).center)
        assertVec(Vec3(-6f, 18f, 0f), classic.getValue(Joint.ARM_RIGHT).center)
        assertVec(Vec3(-5.5f, 18f, 0f), slim.getValue(Joint.ARM_RIGHT).center)
    }

    @Test
    fun bodyYawClampsWithoutWrappingPastOneHundredEighty() {
        val clamped = skeleton.clamp(Joint.BODY, JointAngles(yaw = 270f * degrees))

        assertEquals(180f * degrees, clamped.yaw, 1e-4f)
        assertEquals(0f, clamped.pitch, 0f)
        assertEquals(0f, clamped.roll, 0f)
    }

    @Test
    fun armPitchClampsAtBothShoulderLimits() {
        val backward = skeleton.clamp(Joint.ARM_LEFT, JointAngles(pitch = 100f * degrees))
        val forward = skeleton.clamp(Joint.ARM_LEFT, JointAngles(pitch = -200f * degrees))

        assertEquals(80f * degrees, backward.pitch, 1e-4f)
        assertEquals(-180f * degrees, forward.pitch, 1e-4f)
    }

    @Test
    fun localDeltasAccumulateThenClamp() {
        var pose = SkeletonPose.IDENTITY

        pose = skeleton.applyLocalDelta(pose, Joint.ARM_LEFT, 0f, 45f * degrees, 0f)
        assertEquals(45f * degrees, pose.armLeft.pitch, 1e-4f)

        pose = skeleton.applyLocalDelta(pose, Joint.ARM_LEFT, 0f, 45f * degrees, 0f)
        assertEquals(80f * degrees, pose.armLeft.pitch, 1e-4f)

        pose = skeleton.applyLocalDelta(pose, Joint.ARM_LEFT, 0f, -90f * degrees, 0f)
        assertEquals(-10f * degrees, pose.armLeft.pitch, 1e-4f)
    }

    @Test
    fun axisAlignedBoxRayHitsTheNearFaceAtTFour() {
        val box = OrientedBox.axisAligned(Vec3.ZERO, Vec3(1f, 1f, 1f))
        val ray = Ray(Vec3(0f, 0f, -5f), Vec3.Z)
        val hit = RayBoxPicker.intersect(ray, box)

        assertNotNull(hit)
        assertEquals(4f, hit!!, 1e-4f)
        assertVec(Vec3(0f, 0f, -1f), ray.pointAt(hit))
    }

    @Test
    fun rotatedBoxUsesItsOwnAxesForPicking() {
        val rotation = Mat4.rotationX(quarterTurn)
        val box = OrientedBox(
            center = Vec3.ZERO,
            halfExtents = Vec3(1f, 2f, 0.5f),
            axisX = rotation.transformVector(Vec3.X),
            axisY = rotation.transformVector(Vec3.Y),
            axisZ = rotation.transformVector(Vec3.Z),
        )

        // Local +Y is world +Z with half extent 2, so the near face is z = -2.
        assertEquals(
            3f,
            RayBoxPicker.intersect(Ray(Vec3(0f, 0f, -5f), Vec3.Z), box)!!,
            1e-4f,
        )
    }

    @Test
    fun raysThatMissOrPointAwayReturnNull() {
        val box = OrientedBox.axisAligned(Vec3.ZERO, Vec3(1f, 1f, 1f))

        assertNull(RayBoxPicker.intersect(Ray(Vec3(2f, 0f, -5f), Vec3.Z), box))
        assertNull(RayBoxPicker.intersect(Ray(Vec3(0f, 0f, -5f), -Vec3.Z), box))
        assertEquals(0f, RayBoxPicker.intersect(Ray(Vec3.ZERO, Vec3.Z), box)!!, 0f)
    }
}
