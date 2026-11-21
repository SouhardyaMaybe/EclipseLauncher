/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Pure JVM tests for forward kinematics: known angles, pivot transforms,
 * joint limit clamping, and FK chain correctness.
 */
package me.shadow.eclipselauncher.ui.character.pose

import me.shadow.eclipselauncher.ui.character.pose.SkeletonConstants
import me.shadow.eclipselauncher.ui.character.pose.PoseConstants.DEG2RAD
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwardKinematicsTest {

    private val skeleton = Skeleton(slim = false)
    private val skeletonSlim = Skeleton(slim = true)

    @Test
    fun identityPoseHasCorrectPivotsInWorldSpace() {
        val transforms = skeleton.computeWorldTransforms(SkeletonPose.IDENTITY)

        // Body pivot at (0, 0.4, 0) in model space = world space when identity
        val bodyTransform = transforms[Joint.BODY]!!
        val bodyPivot = bodyTransform.transformPoint(Vec3(SkeletonConstants.BODY_PIVOT_X, SkeletonConstants.BODY_PIVOT_Y, SkeletonConstants.BODY_PIVOT_Z))
        assertEquals(0f, bodyPivot.x, 0.0001f)
        assertEquals(SkeletonConstants.BODY_PIVOT_Y, bodyPivot.y, 0.0001f)
        assertEquals(0f, bodyPivot.z, 0.0001f)

        // Head pivot at (0, 1.6, 0)
        val headTransform = transforms[Joint.HEAD]!!
        val headPivot = headTransform.transformPoint(Vec3(SkeletonConstants.HEAD_PIVOT_X, SkeletonConstants.HEAD_PIVOT_Y, SkeletonConstants.HEAD_PIVIT_Z))
        assertEquals(0f, headPivot.x, 0.0001f)
        assertEquals(SkeletonConstants.HEAD_PIVOT_Y, headPivot.y, 0.0001f)
        assertEquals(0f, headPivot.z, 0.0001f)

        // Left arm pivot at (-1.2, 1.2, 0)
        val armLTransform = transforms[Joint.ARM_LEFT]!!
        val armLPivot = armLTransform.transformPoint(Vec3(SkeletonConstants.ARM_LEFT_PIVOT_X, SkeletonConstants.ARM_LEFT_PIVOT_Y, SkeletonConstants.ARM_LEFT_PIVOT_Z))
        assertEquals(SkeletonConstants.ARM_LEFT_PIVOT_X, armLPivot.x, 0.0001f)
        assertEquals(SkeletonConstants.ARM_LEFT_PIVOT_Y, armLPivot.y, 0.0001f)
        assertEquals(0f, armLPivot.z, 0.0001f)

        // Right arm pivot at (1.2, 1.2, 0)
        val armRTransform = transforms[Joint.ARM_RIGHT]!!
        val armRPivot = armRTransform.transformPoint(Vec3(SkeletonConstants.ARM_RIGHT_PIVOT_X, SkeletonConstants.ARM_RIGHT_PIVOT_Y, SkeletonConstants.ARM_RIGHT_PIVOT_Z))
        assertEquals(SkeletonConstants.ARM_RIGHT_PIVOT_X, armRPivot.x, 0.0001f)
        assertEquals(SkeletonConstants.ARM_RIGHT_PIVOT_Y, armRPivot.y, 0.0001f)
        assertEquals(0f, armRPivot.z, 0.0001f)

        // Left leg pivot at (0.4, -0.8, 0)
        val legLTransform = transforms[Joint.LEG_LEFT]!!
        val legLPivot = legLTransform.transformPoint(Vec3(SkeletonConstants.LEG_LEFT_PIVOT_X, SkeletonConstants.LEG_LEFT_PIVOT_Y, SkeletonConstants.LEG_LEFT_PIVOT_Z))
        assertEquals(SkeletonConstants.LEG_LEFT_PIVOT_X, legLPivot.x, 0.0001f)
        assertEquals(SkeletonConstants.LEG_LEFT_PIVOT_Y, legLPivot.y, 0.0001f)
        assertEquals(0f, legLPivot.z, 0.0001f)

        // Right leg pivot at (-0.4, -0.8, 0)
        val legRTransform = transforms[Joint.LEG_RIGHT]!!
        val legRPivot = legRTransform.transformPoint(Vec3(SkeletonConstants.LEG_RIGHT_PIVOT_X, SkeletonConstants.LEG_RIGHT_PIVOT_Y, SkeletonConstants.LEG_RIGHT_PIVOT_Z))
        assertEquals(SkeletonConstants.LEG_RIGHT_PIVOT_X, legRPivot.x, 0.0001f)
        assertEquals(SkeletonConstants.LEG_RIGHT_PIVOT_Y, legRPivot.y, 0.0001f)
        assertEquals(0f, legRPivot.z, 0.0001f)
    }

    @Test
    fun bodyYawRotatesEntireModel() {
        // 90° yaw around Y
        val pose = SkeletonPose.IDENTITY.set(Joint.BODY, JointState(Quaternion.fromEuler(PI.toFloat() / 2, 0f, 0f)))
        val transforms = skeleton.computeWorldTransforms(pose)

        // Head pivot originally at (0, 1.6, 0) → after 90° yaw: (-1.6, 1.6, 0)
        val headPivot = transforms[Joint.HEAD]!!.transformPoint(Vec3(SkeletonConstants.HEAD_PIVOT_X, SkeletonConstants.HEAD_PIVOT_Y, SkeletonConstants.HEAD_PIVIT_Z))
        assertEquals(-SkeletonConstants.HEAD_PIVOT_Y, headPivot.x, 0.001f)
        assertEquals(SkeletonConstants.HEAD_PIVOT_Y, headPivot.y, 0.001f)
        assertEquals(0f, headPivot.z, 0.001f)

        // Left arm pivot (-1.2, 1.2, 0) → (-1.2, 1.2, 1.2) ? Wait: yaw rotates around Y
        // x' = x*cos - z*sin = -1.2*0 - 0*1 = 0
        // z' = x*sin + z*cos = -1.2*1 + 0*0 = -1.2
        val armLPivot = transforms[Joint.ARM_LEFT]!!.transformPoint(Vec3(SkeletonConstants.ARM_LEFT_PIVOT_X, SkeletonConstants.ARM_LEFT_PIVOT_Y, SkeletonConstants.ARM_LEFT_PIVOT_Z))
        assertEquals(0f, armLPivot.x, 0.001f)
        assertEquals(SkeletonConstants.ARM_LEFT_PIVOT_Y, armLPivot.y, 0.001f)
        assertEquals(SkeletonConstants.ARM_LEFT_PIVOT_X, armLPivot.z, 0.001f) // -1.2
    }

    @Test
    fun headYawIsRelativeToBody() {
        // Body yaw 90°, head yaw 90° → total 180° from model forward
        val pose = SkeletonPose.IDENTITY
            .set(Joint.BODY, JointState(Quaternion.fromEuler(PI.toFloat() / 2, 0f, 0f)))
            .set(Joint.HEAD, JointState(Quaternion.fromEuler(PI.toFloat() / 2, 0f, 0f)))
        val transforms = skeleton.computeWorldTransforms(pose)

        // Head pivot should be at body-rotated position, then head-rotated
        // Body rotates head pivot to (-1.6, 1.6, 0), then head rotates around that pivot
        // Since head pivot is at the rotation center, it stays at (-1.6, 1.6, 0)
        val headPivot = transforms[Joint.HEAD]!!.transformPoint(Vec3(SkeletonConstants.HEAD_PIVOT_X, SkeletonConstants.HEAD_PIVOT_Y, SkeletonConstants.HEAD_PIVIT_Z))
        assertEquals(-SkeletonConstants.HEAD_PIVOT_Y, headPivot.x, 0.001f)
        assertEquals(SkeletonConstants.HEAD_PIVOT_Y, headPivot.y, 0.001f)
        assertEquals(0f, headPivot.z, 0.001f)
    }

    @Test
    fun armSwingRotatesAroundShoulderPivot() {
        // Left arm swing -90° (forward)
        val pose = SkeletonPose.IDENTITY.set(
            Joint.ARM_LEFT,
            JointState(Quaternion.fromEuler(0f, -PI.toFloat() / 2, 0f))
        )
        val transforms = skeleton.computeWorldTransforms(pose)

        // Arm pivot at (-1.2, 1.2, 0) — this is the shoulder, center of rotation
        val armLPivot = transforms[Joint.ARM_LEFT]!!.transformPoint(Vec3(SkeletonConstants.ARM_LEFT_PIVOT_X, SkeletonConstants.ARM_LEFT_PIVOT_Y, SkeletonConstants.ARM_LEFT_PIVOT_Z))
        assertEquals(SkeletonConstants.ARM_LEFT_PIVOT_X, armLPivot.x, 0.001f)
        assertEquals(SkeletonConstants.ARM_LEFT_PIVOT_Y, armLPivot.y, 0.001f)
        assertEquals(0f, armLPivot.z, 0.001f)

        // A point at the hand (arm extends down -Y in local space: y from 1.2 to -10.8)
        // Hand tip in local space: (-1.2, -10.8, 0) relative to origin
        // Actually, arm box is from y=12 to y=24 in skin pixels (1.2 units), so hand at y=0?
        // Model: arm box y=12..24px (y=2.4..4.8 units) with pivot at y=1.2 (shoulder)
        // Local hand tip: (0, -1.2, 0) from pivot
        val handTipLocal = Vec3(SkeletonConstants.ARM_LEFT_PIVOT_X, SkeletonConstants.ARM_LEFT_PIVOT_Y - 2.4f, SkeletonConstants.ARM_LEFT_PIVOT_Z)
        val handTipWorld = transforms[Joint.ARM_LEFT]!!.transformPoint(handTipLocal)
        // After -90° pitch around X: y' = y*cos - z*sin = -1.2*0 - 0*(-1) = 0
        // z' = y*sin + z*cos = -1.2*(-1) + 0*0 = 1.2
        // x unchanged
        assertEquals(SkeletonConstants.ARM_LEFT_PIVOT_X, handTipWorld.x, 0.001f)
        assertEquals(SkeletonConstants.ARM_LEFT_PIVOT_Y, handTipWorld.y, 0.001f) // rotates to same y as pivot
        assertEquals(SkeletonConstants.ARM_LEFT_PIVOT_Z + 2.4f, handTipWorld.z, 0.001f) // extends forward (+Z)
    }

    @Test
    fun legSwingRotatesAroundHipPivot() {
        // Right leg swing 90° (forward)
        val pose = SkeletonPose.IDENTITY.set(
            Joint.LEG_RIGHT,
            JointState(Quaternion.fromEuler(0f, -PI.toFloat() / 2, 0f))
        )
        val transforms = skeleton.computeWorldTransforms(pose)

        // Leg pivot at (-0.4, -0.8, 0) — hip
        val legRPivot = transforms[Joint.LEG_RIGHT]!!.transformPoint(Vec3(SkeletonConstants.LEG_RIGHT_PIVOT_X, SkeletonConstants.LEG_RIGHT_PIVOT_Y, SkeletonConstants.LEG_RIGHT_PIVOT_Z))
        assertEquals(SkeletonConstants.LEG_RIGHT_PIVOT_X, legRPivot.x, 0.001f)
        assertEquals(SkeletonConstants.LEG_RIGHT_PIVOT_Y, legRPivot.y, 0.001f)
        assertEquals(0f, legRPivot.z, 0.001f)

        // Foot tip local: (0, -1.2, 0) from hip (leg is 12px = 2.4 units, half = 1.2)
        val footTipLocal = Vec3(SkeletonConstants.LEG_RIGHT_PIVOT_X, SkeletonConstants.LEG_RIGHT_PIVOT_Y - 2.4f, SkeletonConstants.LEG_RIGHT_PIVOT_Z)
        val footTipWorld = transforms[Joint.LEG_RIGHT]!!.transformPoint(footTipLocal)
        // After -90° pitch: extends forward (+Z)
        assertEquals(SkeletonConstants.LEG_RIGHT_PIVOT_X, footTipWorld.x, 0.001f)
        assertEquals(SkeletonConstants.LEG_RIGHT_PIVOT_Y, footTipWorld.y, 0.001f)
        assertEquals(SkeletonConstants.LEG_RIGHT_PIVOT_Z + 2.4f, footTipWorld.z, 0.001f)
    }

    @Test
    fun slimArmPivotShiftedInward() {
        val transforms = skeletonSlim.computeWorldTransforms(SkeletonPose.IDENTITY)

        // Slim left arm pivot X = -1.2 + 0.1 = -1.1
        val armLPivot = transforms[Joint.ARM_LEFT]!!.transformPoint(
            Vec3(SkeletonConstants.ARM_LEFT_PIVOT_X + SkeletonConstants.SLIM_ARM_PIVOT_X_SHIFT, SkeletonConstants.ARM_LEFT_PIVOT_Y, SkeletonConstants.ARM_LEFT_PIVOT_Z)
        )
        assertEquals(SkeletonConstants.ARM_LEFT_PIVOT_X + SkeletonConstants.SLIM_ARM_PIVOT_X_SHIFT, armLPivot.x, 0.0001f)

        // Slim right arm pivot X = 1.2 - 0.1 = 1.1
        val armRPivot = transforms[Joint.ARM_RIGHT]!!.transformPoint(
            Vec3(SkeletonConstants.ARM_RIGHT_PIVOT_X - SkeletonConstants.SLIM_ARM_PIVOT_X_SHIFT, SkeletonConstants.ARM_RIGHT_PIVOT_Y, SkeletonConstants.ARM_RIGHT_PIVOT_Z)
        )
        assertEquals(SkeletonConstants.ARM_RIGHT_PIVOT_X - SkeletonConstants.SLIM_ARM_PIVOT_X_SHIFT, armRPivot.x, 0.0001f)
    }

    @Test
    fun jointLimitsClampBodyYaw() {
        val unlimited = SkeletonPose.IDENTITY.set(
            Joint.BODY,
            JointState(Quaternion.fromEuler(PI.toFloat(), 0f, 0f)) // 180°
        )
        val clamped = skeleton.clampRotation(Joint.BODY, unlimited.get(Joint.BODY).rotation)
        val (yaw, _, _) = clamped.toEuler()
        assertEquals(PI.toFloat(), yaw, 0.001f) // 180° is within [-180, 180]

        val over = SkeletonPose.IDENTITY.set(
            Joint.BODY,
            JointState(Quaternion.fromEuler(PI.toFloat() * 1.5f, 0f, 0f)) // 270°
        )
        val clampedOver = skeleton.clampRotation(Joint.BODY, over.get(Joint.BODY).rotation)
        val (yaw2, _, _) = clampedOver.toEuler()
        // 270° = -90° in [-180, 180] range
        assertEquals(-PI.toFloat() / 2, yaw2, 0.001f)
    }

    @Test
    fun jointLimitsClampArmSwing() {
        // Arm swing limit: -180° to +80°
        val forward = SkeletonPose.IDENTITY.set(
            Joint.ARM_LEFT,
            JointState(Quaternion.fromEuler(0f, -PI.toFloat(), 0f)) // -180°
        )
        val clamped = skeleton.clampRotation(Joint.ARM_LEFT, forward.get(Joint.ARM_LEFT).rotation)
        val (_, pitch, _) = clamped.toEuler()
        assertEquals(-PI.toFloat(), pitch, 0.001f)

        // Beyond limit: -200° → clamped to -180°
        val over = SkeletonPose.IDENTITY.set(
            Joint.ARM_LEFT,
            JointState(Quaternion.fromEuler(0f, -PI.toFloat() * 1.11f, 0f)) // -200°
        )
        val clampedOver = skeleton.clampRotation(Joint.ARM_LEFT, over.get(Joint.ARM_LEFT).rotation)
        val (_, pitch2, _) = clampedOver.toEuler()
        assertEquals(-PI.toFloat(), pitch2, 0.001f)

        // Backward beyond +80°
        val back = SkeletonPose.IDENTITY.set(
            Joint.ARM_LEFT,
            JointState(Quaternion.fromEuler(0f, 100f * SkeletonConstants.DEG2RAD, 0f))
        )
        val clampedBack = skeleton.clampRotation(Joint.ARM_LEFT, back.get(Joint.ARM_LEFT).rotation)
        val (_, pitch3, _) = clampedBack.toEuler()
        assertEquals(80f * SkeletonConstants.DEG2RAD, pitch3, 0.001f)
    }

    @Test
    fun applyLocalDeltaAccumulatesAndClamps() {
        var pose = SkeletonPose.IDENTITY

        // Apply 45° swing
        val delta45 = Quaternion.fromEuler(0f, 45f * SkeletonConstants.DEG2RAD, 0f)
        pose = skeleton.applyLocalDelta(pose, Joint.ARM_LEFT, delta45)
        var (_, pitch, _) = pose.get(Joint.ARM_LEFT).rotation.toEuler()
        assertEquals(45f * SkeletonConstants.DEG2RAD, pitch, 0.001f)

        // Apply another 45° → 90° (clamped to 80°)
        pose = skeleton.applyLocalDelta(pose, Joint.ARM_LEFT, delta45)
        (_, pitch, _) = pose.get(Joint.ARM_LEFT).rotation.toEuler()
        assertEquals(80f * SkeletonConstants.DEG2RAD, pitch, 0.001f)

        // Apply -90° → should go to -10° (80 - 90 = -10)
        val deltaNeg90 = Quaternion.fromEuler(0f, -90f * SkeletonConstants.DEG2RAD, 0f)
        pose = skeleton.applyLocalDelta(pose, Joint.ARM_LEFT, deltaNeg90)
        (_, pitch, _) = pose.get(Joint.ARM_LEFT).rotation.toEuler()
        assertEquals(-10f * SkeletonConstants.DEG2RAD, pitch, 0.001f)
    }

    @Test
    fun slerpPoseInterpolatesCorrectly() {
        val start = SkeletonPose.IDENTITY
        val end = SkeletonPose.IDENTITY.set(
            Joint.ARM_RIGHT,
            JointState(Quaternion.fromEuler(0f, -PI.toFloat() / 2, 0f))
        )

        // t=0 → start
        val at0 = start.slerp(end, 0f)
        assertEquals(Quaternion.IDENTITY, at0.get(Joint.ARM_RIGHT).rotation)

        // t=1 → end
        val at1 = start.slerp(end, 1f)
        assertEquals(end.get(Joint.ARM_RIGHT).rotation, at1.get(Joint.ARM_RIGHT).rotation)

        // t=0.5 → halfway (slerp on quaternion)
        val atHalf = start.slerp(end, 0.5f)
        val (_, pitch, _) = atHalf.get(Joint.ARM_RIGHT).rotation.toEuler()
        assertEquals(-PI.toFloat() / 4, pitch, 0.01f) // -45°
    }
}