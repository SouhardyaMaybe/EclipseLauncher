/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Forward kinematics skeleton for the Minecraft player model.
 * Each joint has a pivot, local rotation quaternion, and rotation limits.
 * Pure Kotlin — no Android deps — fully JVM testable.
 */
package me.shadow.eclipselauncher.ui.character.pose

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Joint identifier matching the 6 movable parts + body root. */
enum class Joint {
    BODY,       // Root — whole model transform
    HEAD,
    ARM_LEFT,
    ARM_RIGHT,
    LEG_LEFT,
    LEG_RIGHT,
}

/** Per-joint state: local rotation quaternion. */
data class JointState(
    val rotation: Quaternion = Quaternion.IDENTITY,
) {
    fun withRotation(q: Quaternion) = copy(rotation = q)
}

/** Complete skeleton pose: all joint rotations. */
data class SkeletonPose(
    val body: JointState = JointState(),
    val head: JointState = JointState(),
    val armLeft: JointState = JointState(),
    val armRight: JointState = JointState(),
    val legLeft: JointState = JointState(),
    val legRight: JointState = JointState(),
) {
    /** Gets joint state by enum. */
    fun get(joint: Joint): JointState = when (joint) {
        Joint.BODY -> body
        Joint.HEAD -> head
        Joint.ARM_LEFT -> armLeft
        Joint.ARM_RIGHT -> armRight
        Joint.LEG_LEFT -> legLeft
        Joint.LEG_RIGHT -> legRight
    }

    /** Sets joint state, returns new pose. */
    fun set(joint: Joint, state: JointState): SkeletonPose = when (joint) {
        Joint.BODY -> copy(body = state)
        Joint.HEAD -> copy(head = state)
        Joint.ARM_LEFT -> copy(armLeft = state)
        Joint.ARM_RIGHT -> copy(armRight = state)
        Joint.LEG_LEFT -> copy(legLeft = state)
        Joint.LEG_RIGHT -> copy(legRight = state)
    }

    /** Slerps toward target pose by factor t ∈ [0,1]. */
    fun slerp(target: SkeletonPose, t: Float): SkeletonPose = SkeletonPose(
        body = body.withRotation(body.rotation.slerp(target.body.rotation, t)),
        head = head.withRotation(head.rotation.slerp(target.head.rotation, t)),
        armLeft = armLeft.withRotation(armLeft.rotation.slerp(target.armLeft.rotation, t)),
        armRight = armRight.withRotation(armRight.rotation.slerp(target.armRight.rotation, t)),
        legLeft = legLeft.withRotation(legLeft.rotation.slerp(target.legLeft.rotation, t)),
        legRight = legRight.withRotation(legRight.rotation.slerp(target.legRight.rotation, t)),
    )

    companion object {
        /** Standing identity pose. */
        val IDENTITY = SkeletonPose()

        /** Preset poses (quaternions per joint). */
        val PRESETS = mapOf(
            UIConstants.POSE_STANDING to IDENTITY,
            UIConstants.POSE_WALKING to SkeletonPose(
                body = JointState(Quaternion.fromEuler(0f, 0f, 0f)),
                head = JointState(Quaternion.fromEuler(0.2f, 0f, 0f)), // slight yaw
                armLeft = JointState(Quaternion.fromEuler(0f, -0.8f, 0f)), // forward
                armRight = JointState(Quaternion.fromEuler(0f, 0.8f, 0f)), // back
                legLeft = JointState(Quaternion.fromEuler(0f, 0.8f, 0f)), // back
                legRight = JointState(Quaternion.fromEuler(0f, -0.8f, 0f)), // forward
            ),
            UIConstants.POSE_WAVING to SkeletonPose(
                body = JointState(Quaternion.IDENTITY),
                head = JointState(Quaternion.fromEuler(0f, -0.2f, 0f)),
                armLeft = JointState(Quaternion.fromEuler(0f, -0.3f, 0f)),
                armRight = JointState(Quaternion.fromEuler(0f, -1.5f, 0.5f)), // raised, bent
                legLeft = JointState(Quaternion.fromEuler(0f, 0.1f, 0f)),
                legRight = JointState(Quaternion.fromEuler(0f, -0.1f, 0f)),
            ),
            UIConstants.POSE_SITTING to SkeletonPose(
                body = JointState(Quaternion.fromEuler(0f, 0f, 0f)),
                head = JointState(Quaternion.fromEuler(0f, 0.2f, 0f)),
                armLeft = JointState(Quaternion.fromEuler(0f, -0.5f, 0f)),
                armRight = JointState(Quaternion.fromEuler(0f, -0.5f, 0f)),
                legLeft = JointState(Quaternion.fromEuler(0f, PI.toFloat() / 2, 0f)), // 90°
                legRight = JointState(Quaternion.fromEuler(0f, PI.toFloat() / 2, 0f)),
            ),
            UIConstants.POSE_ARMS_UP to SkeletonPose(
                body = JointState(Quaternion.IDENTITY),
                head = JointState(Quaternion.IDENTITY),
                armLeft = JointState(Quaternion.fromEuler(0f, -PI.toFloat() / 2, 0f)),
                armRight = JointState(Quaternion.fromEuler(0f, -PI.toFloat() / 2, 0f)),
                legLeft = JointState(Quaternion.IDENTITY),
                legRight = JointState(Quaternion.IDENTITY),
            ),
            UIConstants.POSE_T_POSE to SkeletonPose(
                body = JointState(Quaternion.IDENTITY),
                head = JointState(Quaternion.IDENTITY),
                armLeft = JointState(Quaternion.fromEuler(0f, 0f, -PI.toFloat() / 2)),
                armRight = JointState(Quaternion.fromEuler(0f, 0f, PI.toFloat() / 2)),
                legLeft = JointState(Quaternion.IDENTITY),
                legRight = JointState(Quaternion.IDENTITY),
            ),
        )
    }
}

/** Skeleton with forward kinematics: computes world transforms for each part. */
class Skeleton(
    private val slim: Boolean = false,
) {

    /** Pivot points in model space (origin at feet, +Y up, faces +Z). */
    val pivots = mapOf(
        Joint.BODY to Vec3(SkeletonConstants.BODY_PIVOT_X, SkeletonConstants.BODY_PIVOT_Y, SkeletonConstants.BODY_PIVOT_Z),
        Joint.HEAD to Vec3(SkeletonConstants.HEAD_PIVOT_X, SkeletonConstants.HEAD_PIVOT_Y, SkeletonConstants.HEAD_PIVIT_Z),
        Joint.ARM_LEFT to Vec3(
            SkeletonConstants.ARM_LEFT_PIVOT_X - (if (slim) SkeletonConstants.SLIM_ARM_PIVOT_X_SHIFT else 0f),
            SkeletonConstants.ARM_LEFT_PIVOT_Y,
            SkeletonConstants.ARM_LEFT_PIVOT_Z,
        ),
        Joint.ARM_RIGHT to Vec3(
            SkeletonConstants.ARM_RIGHT_PIVOT_X + (if (slim) SkeletonConstants.SLIM_ARM_PIVOT_X_SHIFT else 0f),
            SkeletonConstants.ARM_RIGHT_PIVOT_Y,
            SkeletonConstants.ARM_RIGHT_PIVOT_Z,
        ),
        Joint.LEG_LEFT to Vec3(SkeletonConstants.LEG_LEFT_PIVOT_X, SkeletonConstants.LEG_LEFT_PIVOT_Y, SkeletonConstants.LEG_LEFT_PIVOT_Z),
        Joint.LEG_RIGHT to Vec3(SkeletonConstants.LEG_RIGHT_PIVOT_X, SkeletonConstants.LEG_RIGHT_PIVOT_Y, SkeletonConstants.LEG_RIGHT_PIVOT_Z),
    )

    /** Half-extents for each part's OBB (in local space, before rotation). */
    val halfExtents = mapOf(
        Joint.HEAD to Vec3(SkeletonConstants.HEAD_HALF_X, SkeletonConstants.HEAD_HALF_Y, SkeletonConstants.HEAD_HALF_Z),
        Joint.BODY to Vec3(SkeletonConstants.BODY_HALF_X, SkeletonConstants.BODY_HALF_Y, SkeletonConstants.BODY_HALF_Z),
        Joint.ARM_LEFT to Vec3(
            if (slim) SkeletonConstants.ARM_HALF_X_ALEX else SkeletonConstants.ARM_HALF_X_STEVE,
            SkeletonConstants.ARM_HALF_Y, SkeletonConstants.ARM_HALF_Z,
        ),
        Joint.ARM_RIGHT to Vec3(
            if (slim) SkeletonConstants.ARM_HALF_X_ALEX else SkeletonConstants.ARM_HALF_X_STEVE,
            SkeletonConstants.ARM_HALF_Y, SkeletonConstants.ARM_HALF_Z,
        ),
        Joint.LEG_LEFT to Vec3(SkeletonConstants.LEG_HALF_X, SkeletonConstants.LEG_HALF_Y, SkeletonConstants.LEG_HALF_Z),
        Joint.LEG_RIGHT to Vec3(SkeletonConstants.LEG_HALF_X, SkeletonConstants.LEG_HALF_Y, SkeletonConstants.LEG_HALF_Z),
    )

    /** Parent joint for FK chain. */
    val parents = mapOf(
        Joint.BODY to null,
        Joint.HEAD to Joint.BODY,
        Joint.ARM_LEFT to Joint.BODY,
        Joint.ARM_RIGHT to Joint.BODY,
        Joint.LEG_LEFT to Joint.BODY,
        Joint.LEG_RIGHT to Joint.BODY,
    )

    /** Joint rotation limits (radians, Float). */
    val limits = mapOf(
        Joint.BODY to Triple(
            (SkeletonConstants.BODY_YAW_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.BODY_YAW_MAX_DEG * DEG2RAD).toFloat(),
            (SkeletonConstants.BODY_PITCH_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.BODY_PITCH_MAX_DEG * DEG2RAD).toFloat(),
            (SkeletonConstants.BODY_ROLL_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.BODY_ROLL_MAX_DEG * DEG2RAD).toFloat(),
        ),
        Joint.HEAD to Triple(
            (SkeletonConstants.HEAD_YAW_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.HEAD_YAW_MAX_DEG * DEG2RAD).toFloat(),
            (SkeletonConstants.HEAD_PITCH_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.HEAD_PITCH_MAX_DEG * DEG2RAD).toFloat(),
            (SkeletonConstants.HEAD_ROLL_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.HEAD_ROLL_MAX_DEG * DEG2RAD).toFloat(),
        ),
        Joint.ARM_LEFT to Triple(
            (SkeletonConstants.ARM_SWING_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.ARM_SWING_MAX_DEG * DEG2RAD).toFloat(),
            (SkeletonConstants.ARM_TWIST_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.ARM_TWIST_MAX_DEG * DEG2RAD).toFloat(),
            (SkeletonConstants.ARM_ABDUCT_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.ARM_ABDUCT_MAX_DEG * DEG2RAD).toFloat(),
        ),
        Joint.ARM_RIGHT to Triple(
            (SkeletonConstants.ARM_SWING_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.ARM_SWING_MAX_DEG * DEG2RAD).toFloat(),
            (SkeletonConstants.ARM_TWIST_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.ARM_TWIST_MAX_DEG * DEG2RAD).toFloat(),
            (SkeletonConstants.ARM_ABDUCT_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.ARM_ABDUCT_MAX_DEG * DEG2RAD).toFloat(),
        ),
        Joint.LEG_LEFT to Triple(
            (SkeletonConstants.LEG_SWING_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.LEG_SWING_MAX_DEG * DEG2RAD).toFloat(),
            (SkeletonConstants.LEG_TWIST_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.LEG_TWIST_MAX_DEG * DEG2RAD).toFloat(),
            0f to 0f, // no abduction
        ),
        Joint.LEG_RIGHT to Triple(
            (SkeletonConstants.LEG_SWING_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.LEG_SWING_MAX_DEG * DEG2RAD).toFloat(),
            (SkeletonConstants.LEG_TWIST_MIN_DEG * DEG2RAD).toFloat() to (SkeletonConstants.LEG_TWIST_MAX_DEG * DEG2RAD).toFloat(),
            0f to 0f,
        ),
    )

    /** Computes world transform (Matrix4) for each joint given a pose. */
    fun computeWorldTransforms(pose: SkeletonPose): Map<Joint, Matrix4> {
        val transforms = mutableMapOf<Joint, Matrix4>()

        // Process in topological order (parents before children)
        val order = listOf(Joint.BODY, Joint.HEAD, Joint.ARM_LEFT, Joint.ARM_RIGHT, Joint.LEG_LEFT, Joint.LEG_RIGHT)

        for (joint in order) {
            val parent = parents[joint]
            val pivot = pivots[joint]!!
            val localRot = pose.get(joint).rotation

            // Local transform: translate to pivot, rotate, translate back
            val localTransform = Matrix4.translate(pivot.x, pivot.y, pivot.z)
                .times(localRot.toMatrix())
                .times(Matrix4.translate(-pivot.x, -pivot.y, -pivot.z))

            val worldTransform = if (parent != null) {
                transforms[parent]!!.times(localTransform)
            } else {
                localTransform
            }
            transforms[joint] = worldTransform
        }

        return transforms
    }

    /** Computes world-space OBB for each part given a pose. */
    fun computeWorldOBBs(pose: SkeletonPose): Map<Joint, OBB> {
        val transforms = computeWorldTransforms(pose)
        val obbs = mutableMapOf<Joint, OBB>()

        for (joint in Joint.values()) {
            val pivot = pivots[joint]!!
            val extent = halfExtents[joint]!!
            val transform = transforms[joint]!!

            // Transform pivot to world space
            val worldCenter = transform.transformPoint(pivot)
            // Transform half-extents by rotation only (no translation) — approximate with abs of basis vectors
            val worldExtent = Vec3(
                extent.x * abs(transform.m00) + extent.y * abs(transform.m01) + extent.z * abs(transform.m02),
                extent.x * abs(transform.m10) + extent.y * abs(transform.m11) + extent.z * abs(transform.m12),
                extent.x * abs(transform.m20) + extent.y * abs(transform.m21) + extent.z * abs(transform.m22),
            )
            // Use the joint's local rotation as OBB orientation
            val localRot = pose.get(joint).rotation
            obbs[joint] = OBB(worldCenter, worldExtent, localRot)
        }

        return obbs
    }

    /** Clamps a joint's rotation to its limits. */
    fun clampRotation(joint: Joint, rotation: Quaternion): Quaternion {
        val (yawLimits, pitchLimits, rollLimits) = limits[joint]!!
        val (yaw, pitch, roll) = rotation.toEuler()

        val clampedYaw = yaw.coerceIn(yawLimits.first.toFloat(), yawLimits.second.toFloat())
        val clampedPitch = pitch.coerceIn(pitchLimits.first.toFloat(), pitchLimits.second.toFloat())
        val clampedRoll = roll.coerceIn(rollLimits.first.toFloat(), rollLimits.second.toFloat())

        return Quaternion.fromEuler(clampedYaw, clampedPitch, clampedRoll)
    }

    /** Applies a delta rotation to a joint in its local space, then clamps. */
    fun applyLocalDelta(pose: SkeletonPose, joint: Joint, delta: Quaternion): SkeletonPose {
        val current = pose.get(joint).rotation
        val combined = current.times(delta) // local space: delta applied after current
        val clamped = clampRotation(joint, combined)
        return pose.set(joint, JointState(clamped))
    }

    /** Converts a world-space rotation axis to joint-local space. */
    fun worldAxisToLocal(joint: Joint, worldAxis: Vec3, pose: SkeletonPose): Vec3 {
        val transforms = computeWorldTransforms(pose)
        val parent = parents[joint]
        val worldRot = if (parent != null) {
            transforms[parent]!!.transformVector(worldAxis).normalized()
        } else {
            worldAxis
        }
        // Transform by inverse of joint's current rotation to get local axis
        val invJointRot = pose.get(joint).rotation.inverted()
        return invJointRot.rotate(worldRot)
    }
}