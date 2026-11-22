/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Minecraft player rig: rest-pose pivots, joint limits and forward kinematics.
 * Pivots are in skin pixels and never contain renderer/mesh offsets. Angles are
 * the source of truth; quaternions and matrices are derived on demand.
 */
package me.shadow.eclipselauncher.pose

import kotlin.math.PI

/** The six movable parts of the player rig. */
enum class Joint {
    BODY,
    HEAD,
    ARM_LEFT,
    ARM_RIGHT,
    LEG_LEFT,
    LEG_RIGHT,
}

/**
 * Local joint rotation in radians. Order is yaw (Y), pitch (X), roll (Z):
 * a column vector is rotated by yaw first, then pitch, then roll.
 */
data class JointAngles(
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
) {
    fun toQuaternion(): Quaternion = Quaternion.fromEuler(yaw, pitch, roll)
}

/** Complete pose. Every joint angle is stored explicitly. */
data class SkeletonPose(
    val body: JointAngles = JointAngles(),
    val head: JointAngles = JointAngles(),
    val armLeft: JointAngles = JointAngles(),
    val armRight: JointAngles = JointAngles(),
    val legLeft: JointAngles = JointAngles(),
    val legRight: JointAngles = JointAngles(),
) {
    fun get(joint: Joint): JointAngles = when (joint) {
        Joint.BODY -> body
        Joint.HEAD -> head
        Joint.ARM_LEFT -> armLeft
        Joint.ARM_RIGHT -> armRight
        Joint.LEG_LEFT -> legLeft
        Joint.LEG_RIGHT -> legRight
    }

    fun set(joint: Joint, angles: JointAngles): SkeletonPose = when (joint) {
        Joint.BODY -> copy(body = angles)
        Joint.HEAD -> copy(head = angles)
        Joint.ARM_LEFT -> copy(armLeft = angles)
        Joint.ARM_RIGHT -> copy(armRight = angles)
        Joint.LEG_LEFT -> copy(legLeft = angles)
        Joint.LEG_RIGHT -> copy(legRight = angles)
    }

    companion object {
        val IDENTITY = SkeletonPose()
    }
}

/** Inclusive range for one joint angle in radians. */
data class AngleLimit(val min: Float, val max: Float) {
    init {
        require(min <= max) { "Angle limit min ($min) must not exceed max ($max)" }
    }

    fun clamp(value: Float): Float = value.coerceIn(min, max)
}

/** Limits for yaw, pitch and roll of one joint. */
data class JointLimits(
    val yaw: AngleLimit,
    val pitch: AngleLimit,
    val roll: AngleLimit,
) {
    fun clamp(angles: JointAngles): JointAngles = JointAngles(
        yaw = yaw.clamp(angles.yaw),
        pitch = pitch.clamp(angles.pitch),
        roll = roll.clamp(angles.roll),
    )
}

/**
 * Rig definition and forward kinematics. Stateless; [MeshMapping] carries the
 * slim/classic geometry differences.
 */
class Skeleton {
    val pivots: Map<Joint, Vec3> = PIVOTS
    val parents: Map<Joint, Joint?> = PARENTS
    val limits: Map<Joint, JointLimits> = LIMITS

    /**
     * Model-to-world transform for every joint, in parent-before-child order.
     *
     * `M_root = T(p) * R * T(-p)` and
     * `M_child = M_parent * T(p_child) * R_child * T(-p_child)`.
     */
    fun computeWorldTransforms(pose: SkeletonPose): Map<Joint, Mat4> {
        val world = LinkedHashMap<Joint, Mat4>(ORDER.size)
        for (joint in ORDER) {
            val pivot = pivots.getValue(joint)
            val local = Mat4.aroundPivot(pose.get(joint).toQuaternion().toMatrix(), pivot)
            val parent = parents.getValue(joint)
            world[joint] = if (parent == null) local else world.getValue(parent) * local
        }
        return world
    }

    /** Clamps stored angles; no quaternion round-trip is involved. */
    fun clamp(joint: Joint, angles: JointAngles): JointAngles =
        limits.getValue(joint).clamp(angles)

    /** Clamps every joint of [pose]. */
    fun clampPose(pose: SkeletonPose): SkeletonPose {
        var result = pose
        for (joint in Joint.entries) {
            result = result.set(joint, clamp(joint, result.get(joint)))
        }
        return result
    }

    /** Adds a delta to one joint in local angle space, then clamps it. */
    fun applyLocalDelta(
        pose: SkeletonPose,
        joint: Joint,
        deltaYaw: Float,
        deltaPitch: Float,
        deltaRoll: Float,
    ): SkeletonPose {
        val current = pose.get(joint)
        val next = JointAngles(
            yaw = current.yaw + deltaYaw,
            pitch = current.pitch + deltaPitch,
            roll = current.roll + deltaRoll,
        )
        return pose.set(joint, clamp(joint, next))
    }

    /**
     * World-space oriented boxes for picking/highlighting. Box centers and
     * half extents come from the separate [MeshMapping]; pivots stay untouched.
     */
    fun computePartBoxes(pose: SkeletonPose, mapping: MeshMapping): Map<Joint, OrientedBox> {
        val world = computeWorldTransforms(pose)
        return Joint.entries.associateWith { joint ->
            val transform = world.getValue(joint)
            val meshBox = mapping.box(joint)
            OrientedBox(
                center = transform.transformPoint(pivots.getValue(joint) + meshBox.offset),
                halfExtents = meshBox.halfExtents,
                axisX = transform.transformVector(Vec3.X).normalized(),
                axisY = transform.transformVector(Vec3.Y).normalized(),
                axisZ = transform.transformVector(Vec3.Z).normalized(),
            )
        }
    }

    companion object {
        val DEG2RAD = (PI / 180.0).toFloat()

        /** Rest-pose model-space pivots, in skin pixels. */
        val BODY_PIVOT = Vec3(0f, 12f, 0f)
        val HEAD_PIVOT = Vec3(0f, 24f, 0f)
        val ARM_LEFT_PIVOT = Vec3(5f, 22f, 0f)
        val ARM_RIGHT_PIVOT = Vec3(-5f, 22f, 0f)
        val LEG_LEFT_PIVOT = Vec3(2f, 12f, 0f)
        val LEG_RIGHT_PIVOT = Vec3(-2f, 12f, 0f)

        private val ORDER = listOf(
            Joint.BODY,
            Joint.HEAD,
            Joint.ARM_LEFT,
            Joint.ARM_RIGHT,
            Joint.LEG_LEFT,
            Joint.LEG_RIGHT,
        )

        private val PIVOTS = mapOf(
            Joint.BODY to BODY_PIVOT,
            Joint.HEAD to HEAD_PIVOT,
            Joint.ARM_LEFT to ARM_LEFT_PIVOT,
            Joint.ARM_RIGHT to ARM_RIGHT_PIVOT,
            Joint.LEG_LEFT to LEG_LEFT_PIVOT,
            Joint.LEG_RIGHT to LEG_RIGHT_PIVOT,
        )

        private val PARENTS = mapOf(
            Joint.BODY to null,
            Joint.HEAD to Joint.BODY,
            Joint.ARM_LEFT to Joint.BODY,
            Joint.ARM_RIGHT to Joint.BODY,
            Joint.LEG_LEFT to Joint.BODY,
            Joint.LEG_RIGHT to Joint.BODY,
        )

        private fun degrees(min: Float, max: Float) =
            AngleLimit(min * DEG2RAD, max * DEG2RAD)

        private val LIMITS = mapOf(
            Joint.BODY to JointLimits(
                yaw = degrees(-180f, 180f),
                pitch = degrees(-90f, 90f),
                roll = degrees(-30f, 30f),
            ),
            Joint.HEAD to JointLimits(
                yaw = degrees(-90f, 90f),
                pitch = degrees(-75f, 75f),
                roll = degrees(-30f, 30f),
            ),
            Joint.ARM_LEFT to JointLimits(
                yaw = degrees(-90f, 90f),
                pitch = degrees(-180f, 80f),
                roll = degrees(0f, 90f),
            ),
            Joint.ARM_RIGHT to JointLimits(
                yaw = degrees(-90f, 90f),
                pitch = degrees(-180f, 80f),
                roll = degrees(-90f, 0f),
            ),
            Joint.LEG_LEFT to JointLimits(
                yaw = degrees(-30f, 30f),
                pitch = degrees(-120f, 60f),
                roll = degrees(0f, 0f),
            ),
            Joint.LEG_RIGHT to JointLimits(
                yaw = degrees(-30f, 30f),
                pitch = degrees(-120f, 60f),
                roll = degrees(0f, 0f),
            ),
        )
    }
}
