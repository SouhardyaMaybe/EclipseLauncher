/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Forward kinematics skeleton for the Minecraft player model.
 * Each joint has a pivot, local rotation angles (yaw, pitch, roll), and rotation limits.
 * Pure Kotlin — no Android deps — fully JVM testable.
 * Angles (yaw, pitch, roll) are the source of truth; quaternions are derived on demand.
 * This avoids quaternion→Euler→quaternion round-trip issues at singularities (hypothesis c).
 */
package me.shadow.eclipselauncher.ui.character.pose

import me.shadow.eclipselauncher.ui.character.pose.SkeletonConstants.DEG2RAD
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

/** Per-joint state: local rotation angles in radians (yaw, pitch, roll).
 *  Order: yaw (Y), pitch (X), roll (Z) — YXZ Euler order.
 *  These are the source of truth; quaternions are derived on demand. */
data class JointAngles(
    val yaw: Float = 0f,    // rotation around Y (vertical)
    val pitch: Float = 0f,  // rotation around X (horizontal forward/back)
    val roll: Float = 0f,   // rotation around Z (horizontal side)
) {
    fun withYaw(y: Float) = copy(yaw = y)
    fun withPitch(p: Float) = copy(pitch = p)
    fun withRoll(r: Float) = copy(roll = r)

    /** Returns quaternion derived from these angles (YXZ order). */
    fun toQuaternion(): Quaternion = Quaternion.fromEuler(yaw, pitch, roll)
}

/** Complete skeleton pose: all joint angles (source of truth). */
data class SkeletonPose(
    val body: JointAngles = JointAngles(),
    val head: JointAngles = JointAngles(),
    val armLeft: JointAngles = JointAngles(),
    val armRight: JointAngles = JointAngles(),
    val legLeft: JointAngles = JointAngles(),
    val legRight: JointAngles = JointAngles(),
) {
    /** Gets joint angles by enum. */
    fun get(joint: Joint): JointAngles = when (joint) {
        Joint.BODY -> body
        Joint.HEAD -> head
        Joint.ARM_LEFT -> armLeft
        Joint.ARM_RIGHT -> armRight
        Joint.LEG_LEFT -> legLeft
        Joint.LEG_RIGHT -> legRight
    }

    /** Sets joint angles, returns new pose. */
    fun set(joint: Joint, angles: JointAngles): SkeletonPose = when (joint) {
        Joint.BODY -> copy(body = angles)
        Joint.HEAD -> copy(head = angles)
        Joint.ARM_LEFT -> copy(armLeft = angles)
        Joint.ARM_RIGHT -> copy(armRight = angles)
        Joint.LEG_LEFT -> copy(legLeft = angles)
        Joint.LEG_RIGHT -> copy(legRight = angles)
    }

    /** Returns quaternion for a joint (derived from angles). */
    fun getQuaternion(joint: Joint): Quaternion = get(joint).toQuaternion()

    /** Slerps toward target pose by factor t ∈ [0,1] (on angles, not quaternions). */
    fun lerp(target: SkeletonPose, t: Float): SkeletonPose = SkeletonPose(
        body = lerpAngles(body, target.body, t),
        head = lerpAngles(head, target.head, t),
        armLeft = lerpAngles(armLeft, target.armLeft, t),
        armRight = lerpAngles(armRight, target.armRight, t),
        legLeft = lerpAngles(legLeft, target.legLeft, t),
        legRight = lerpAngles(legRight, target.legRight, t),
    )

    private fun lerpAngles(a: JointAngles, b: JointAngles, t: Float): JointAngles =
        JointAngles(
            yaw = a.yaw + (b.yaw - a.yaw) * t,
            pitch = a.pitch + (b.pitch - a.pitch) * t,
            roll = a.roll + (b.roll - a.roll) * t,
        )

    companion object {
        /** Standing identity pose. */
        val IDENTITY = SkeletonPose()

        /** Preset poses (angles per joint). */
        val PRESETS = mapOf(
            UIConstants.POSE_STANDING to IDENTITY,
            UIConstants.POSE_WALKING to SkeletonPose(
                body = JointAngles(0f, 0f, 0f),
                head = JointAngles(0.2f, 0f, 0f),
                armLeft = JointAngles(0f, -0.8f, 0f),
                armRight = JointAngles(0f, 0.8f, 0f),
                legLeft = JointAngles(0f, 0.8f, 0f),
                legRight = JointAngles(0f, -0.8f, 0f),
            ),
            UIConstants.POSE_WAVING to SkeletonPose(
                body = JointAngles(0f, 0f, 0f),
                head = JointAngles(0f, -0.2f, 0f),
                armLeft = JointAngles(0f, -0.3f, 0f),
                armRight = JointAngles(0f, -1.5f, 0.5f),
                legLeft = JointAngles(0f, 0.1f, 0f),
                legRight = JointAngles(0f, -0.1f, 0f),
            ),
            UIConstants.POSE_SITTING to SkeletonPose(
                body = JointAngles(0f, 0f, 0f),
                head = JointAngles(0f, 0.2f, 0f),
                armLeft = JointAngles(0f, -0.5f, 0f),
                armRight = JointAngles(0f, -0.5f, 0f),
                legLeft = JointAngles(0f, PI.toFloat() / 2, 0f),
                legRight = JointAngles(0f, PI.toFloat() / 2, 0f),
            ),
            UIConstants.POSE_ARMS_UP to SkeletonPose(
                body = JointAngles(0f, 0f, 0f),
                head = JointAngles(0f, 0f, 0f),
                armLeft = JointAngles(0f, -PI.toFloat() / 2, 0f),
                armRight = JointAngles(0f, -PI.toFloat() / 2, 0f),
                legLeft = JointAngles(0f, 0f, 0f),
                legRight = JointAngles(0f, 0f, 0f),
            ),
            UIConstants.POSE_T_POSE to SkeletonPose(
                body = JointAngles(0f, 0f, 0f),
                head = JointAngles(0f, 0f, 0f),
                armLeft = JointAngles(0f, 0f, -PI.toFloat() / 2),
                armRight = JointAngles(0f, 0f, PI.toFloat() / 2),
                legLeft = JointAngles(0f, 0f, 0f),
                legRight = JointAngles(0f, 0f, 0f),
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

    /** Computes world transform (Matrix4) for each joint given a pose.
     * Returns MODEL-TO-WORLD transforms (maps model-space points to world-space).
     * Uses the convention: local = T(-pivot) * R * T(pivot), world = parent * local.
     * This rotates around the pivot in MODEL space. */
    fun computeWorldTransforms(pose: SkeletonPose): Map<Joint, Matrix4> {
        val transforms = mutableMapOf<Joint, Matrix4>()

        // Process in topological order (parents before children)
        val order = listOf(Joint.BODY, Joint.HEAD, Joint.ARM_LEFT, Joint.ARM_RIGHT, Joint.LEG_LEFT, Joint.LEG_RIGHT)

        for (joint in order) {
            val parent = parents[joint]
            val pivot = pivots[joint]!!
            val localRot = pose.getQuaternion(joint)

            // Local transform: T(-pivot) * R * T(pivot)
            // For row vectors v' = v * M: translate to origin, rotate, translate back.
            val localTransform = Matrix4.translate(-pivot.x, -pivot.y, -pivot.z)
                .times(localRot.toMatrix())
                .times(Matrix4.translate(pivot.x, pivot.y, pivot.z))

            val worldTransform = if (parent != null) {
                // For MODEL-TO-WORLD: world = parent * local (apply parent then local)
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
            val localRot = pose.getQuaternion(joint)
            obbs[joint] = OBB(worldCenter, worldExtent, localRot)
        }

        return obbs
    }

    /** Clamps a joint's angles to its limits (angles are the source of truth). */
    fun clampAngles(joint: Joint, angles: JointAngles): JointAngles {
        val (yawLimits, pitchLimits, rollLimits) = limits[joint]!!

        val clampedYaw = angles.yaw.coerceIn(yawLimits.first, yawLimits.second)
        val clampedPitch = angles.pitch.coerceIn(pitchLimits.first, pitchLimits.second)
        val clampedRoll = angles.roll.coerceIn(rollLimits.first, rollLimits.second)

        return JointAngles(clampedYaw, clampedPitch, clampedRoll)
    }

    /** Applies a delta rotation to a joint in its local space (on angles), then clamps. */
    fun applyLocalDelta(pose: SkeletonPose, joint: Joint, deltaYaw: Float, deltaPitch: Float, deltaRoll: Float): SkeletonPose {
        val current = pose.get(joint)
        val newAngles = JointAngles(
            yaw = current.yaw + deltaYaw,
            pitch = current.pitch + deltaPitch,
            roll = current.roll + deltaRoll,
        )
        val clamped = clampAngles(joint, newAngles)
        return pose.set(joint, clamped)
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
        val invJointRot = pose.getQuaternion(joint).inverted()
        return invJointRot.rotate(worldRot)
    }
}