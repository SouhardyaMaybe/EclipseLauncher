/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Pure Kotlin quaternion math for forward kinematics and pose interpolation.
 * No Android dependencies — fully JVM testable.
 * Based on standard formulas (Skinseed J0/d.java, Foley/van Dam).
 */
package me.shadow.eclipselauncher.ui.character.pose

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.PI

/** Quaternion with (w, x, y, z) component order. */
data class Quaternion(
    val w: Float = 1.0f,
    val x: Float = 0.0f,
    val y: Float = 0.0f,
    val z: Float = 0.0f,
) {

    companion object {
        /** Identity quaternion. */
        val IDENTITY = Quaternion(1.0f, 0.0f, 0.0f, 0.0f)

        /** Creates a quaternion from axis-angle (radians). */
        fun fromAxisAngle(axisX: Float, axisY: Float, axisZ: Float, angleRad: Float): Quaternion {
            val halfAngle = angleRad * 0.5f
            val s = sin(halfAngle)
            return Quaternion(
                w = cos(halfAngle),
                x = axisX * s,
                y = axisY * s,
                z = axisZ * s,
            ).normalized()
        }

        /** Creates a quaternion from Euler angles (yaw, pitch, roll in radians, YXZ order).
     * Yaw = rotation around Y (vertical), Pitch = rotation around X (horizontal forward/back),
     * Roll = rotation around Z (horizontal side). Order of application: Yaw → Pitch → Roll.
     * Formula derived from q = q_roll * q_pitch * q_yaw:
     * q_yaw (Y) = (cos(y/2), 0, sin(y/2), 0)
     * q_pitch (X) = (cos(p/2), sin(p/2), 0, 0)
     * q_roll (Z) = (cos(r/2), 0, 0, sin(r/2))
     * q = q_roll * q_pitch * q_yaw
     */
        fun fromEuler(yaw: Float, pitch: Float, roll: Float): Quaternion {
            val cy = cos(yaw * 0.5f)
            val sy = sin(yaw * 0.5f)
            val cp = cos(pitch * 0.5f)
            val sp = sin(pitch * 0.5f)
            val cr = cos(roll * 0.5f)
            val sr = sin(roll * 0.5f)

            // q = q_roll * q_pitch * q_yaw
            // w = cr*cp*cy - sr*sp*sy
            // x = cr*sp*cy - sr*cp*sy
            // y = cr*cp*sy + sr*sp*cy
            // z = cr*sp*sy + sr*cp*cy
            return Quaternion(
                w = cr * cp * cy - sr * sp * sy,
                x = cr * sp * cy - sr * cp * sy,
                y = cr * cp * sy + sr * sp * cy,
                z = cr * sp * sy + sr * cp * cy,
            ).normalized()
        }

        /** Creates a quaternion rotating from vector `from` to vector `to`. */
        fun fromTo(from: Vec3, to: Vec3): Quaternion {
            val dot = from.dot(to)
            if (dot < -0.999999f) {
                // 180-degree rotation — pick any orthogonal axis
                val ortho = if (abs(from.x) > abs(from.z))
                    Vec3(-from.y, from.x, 0f) else Vec3(0f, -from.z, from.y)
                return fromAxisAngle(ortho.x, ortho.y, ortho.z, PI.toFloat())
            }
            val cross = from.cross(to)
            val w = 1.0f + dot
            return Quaternion(w, cross.x, cross.y, cross.z).normalized()
        }
    }

    /** Returns the squared length. */
    fun lengthSquared(): Float = w * w + x * x + y * y + z * z

    /** Returns the length. */
    fun length(): Float = sqrt(lengthSquared())

    /** Returns a normalized copy. */
    fun normalized(): Quaternion {
        val len = length()
        if (len == 0f) return IDENTITY
        val inv = 1.0f / len
        return Quaternion(w * inv, x * inv, y * inv, z * inv)
    }

    /** Returns the conjugate. */
    fun conjugated(): Quaternion = Quaternion(w, -x, -y, -z)

    /** Returns the inverse (conjugate for unit quaternions). */
    fun inverted(): Quaternion = conjugated()

    /** Multiplies this * other (applies other then this). */
    fun times(other: Quaternion): Quaternion = Quaternion(
        w = w * other.w - x * other.x - y * other.y - z * other.z,
        x = w * other.x + x * other.w + y * other.z - z * other.y,
        y = w * other.y - x * other.z + y * other.w + z * other.x,
        z = w * other.z + x * other.y - y * other.x + z * other.w,
    )

    /** Rotates a vector by this quaternion. */
    fun rotate(v: Vec3): Vec3 {
        // v' = q * v * q^-1  (v as pure quaternion)
        val qv = Quaternion(0f, v.x, v.y, v.z)
        val result = this.times(qv).times(inverted())
        return Vec3(result.x, result.y, result.z)
    }

    /** Returns the angle (radians) of this rotation. */
    fun angle(): Float = 2.0f * acos(w.coerceIn(-1f, 1f))

    /** Returns the axis (normalized) of this rotation. */
    fun axis(): Vec3 {
        val s = sqrt(1.0f - w * w)
        if (s < 1e-6f) return Vec3(1f, 0f, 0f) // arbitrary
        return Vec3(x / s, y / s, z / s)
    }

    /** Spherical linear interpolation (slerp) toward `target` by factor `t ∈ [0,1]`.
     *  Handles sign flip for shortest path (Skinseed J0/d.java:100-120). */
    fun slerp(target: Quaternion, t: Float): Quaternion {
        var dot = w * target.w + x * target.x + y * target.y + z * target.z

        // Choose shortest path
        var targetW = target.w
        var targetX = target.x
        var targetY = target.y
        var targetZ = target.z
        if (dot < 0f) {
            dot = -dot
            targetW = -targetW
            targetX = -targetX
            targetY = -targetY
            targetZ = -targetZ
        }

        // If very close, use linear interpolation (nlerp) to avoid numerical issues
        if (dot > 1.0f - AnimationConstants.SLERP_EPSILON) {
            val result = Quaternion(
                w = w + (targetW - w) * t,
                x = x + (targetX - x) * t,
                y = y + (targetY - y) * t,
                z = z + (targetZ - z) * t,
            )
            return result.normalized()
        }

        val theta = acos(dot.coerceIn(-1f, 1f))
        val sinTheta = sin(theta)
        val scale0 = sin((1.0f - t) * theta) / sinTheta
        val scale1 = sin(t * theta) / sinTheta

        return Quaternion(
            w = w * scale0 + targetW * scale1,
            x = x * scale0 + targetX * scale1,
            y = y * scale0 + targetY * scale1,
            z = z * scale0 + targetZ * scale1,
        )
    }

    /** Returns Euler angles (yaw, pitch, roll) in radians (YXZ order).
     * Yaw = rotation around Y, Pitch = rotation around X, Roll = rotation around Z.
     * Extraction from quaternion for YXZ order:
     * yaw = atan2(2(x*z + w*y), w² - x² - y² + z²)
     * pitch = asin(2(w*x - y*z))
     * roll = atan2(2(x*y + w*z), w² - x² + y² - z²)
     */
    fun toEuler(): Triple<Float, Float, Float> {
        // Yaw (Y) = atan2(2(x*z + w*y), w² - x² - y² + z²)
        val yaw = atan2(2f * (x * z + w * y), w * w - x * x - y * y + z * z).toFloat()

        // Pitch (X) = asin(2(w*x - y*z))
        val sinp = 2f * (w * x - y * z)
        val pitch = if (sinp >= 1f) PI.toFloat() / 2f else if (sinp <= -1f) -PI.toFloat() / 2f else asin(sinp).toFloat()

        // Roll (Z) = atan2(2(x*y + w*z), w² - x² + y² - z²)
        val roll = atan2(2f * (x * y + w * z), w * w - x * x + y * y - z * z).toFloat()

        return Triple(yaw, pitch, roll)
    }

    /** Converts to a 4x4 rotation matrix (row-major, for row-vector transformPoint). */
    fun toMatrix(): Matrix4 = Matrix4(
        1f - 2f * (y * y + z * z), 2f * (x * y - w * z), 2f * (x * z + w * y), 0f,
        2f * (x * y + w * z), 1f - 2f * (x * x + z * z), 2f * (y * z - w * x), 0f,
        2f * (x * z - w * y), 2f * (y * z + w * x), 1f - 2f * (x * x + y * y), 0f,
        0f, 0f, 0f, 1f
    )
}

/** 3D vector. */
data class Vec3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    operator fun div(s: Float) = Vec3(x / s, y / s, z / s)
    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z
    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )
    fun lengthSquared(): Float = dot(this)
    fun length(): Float = sqrt(lengthSquared())
    fun normalized(): Vec3 = this / length()
}

/** 4x4 matrix in row-major layout for row-vector convention (v' = v * M).
 *  Layout (row-major):
 *  [ m00 m01 m02 m03 ]
 *  [ m10 m11 m12 m13 ]
 *  [ m20 m21 m22 m23 ]
 *  [ m30 m31 m32 m33 ]
 *  where last row (m30,m31,m32) is translation for row vectors.
 */
data class Matrix4(
    val m00: Float, val m01: Float, val m02: Float, val m03: Float,
    val m10: Float, val m11: Float, val m12: Float, val m13: Float,
    val m20: Float, val m21: Float, val m22: Float, val m23: Float,
    val m30: Float, val m31: Float, val m32: Float, val m33: Float,
) {
    companion object {
        val IDENTITY = Matrix4(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )

        /** Perspective projection matrix (OpenGL style, row-major). */
        fun perspective(fovYRad: Float, aspect: Float, near: Float, far: Float): Matrix4 {
            val f = 1.0f / tan(fovYRad * 0.5f)
            val rangeInv = 1.0f / (near - far)
            return Matrix4(
                f / aspect, 0f, 0f, 0f,
                0f, f, 0f, 0f,
                0f, 0f, (near + far) * rangeInv, -1f,
                0f, 0f, near * far * rangeInv * 2f, 0f
            )
        }

        /** Look-at view matrix (row-major). */
        fun lookAt(eyeX: Float, eyeY: Float, eyeZ: Float, centerX: Float, centerY: Float, centerZ: Float, upX: Float, upY: Float, upZ: Float): Matrix4 {
            val fx = centerX - eyeX
            val fy = centerY - eyeY
            val fz = centerZ - eyeZ
            val flen = sqrt(fx * fx + fy * fy + fz * fz)
            val fxl = fx / flen
            val fyl = fy / flen
            val fzl = fz / flen

            val sx = fyl * upZ - fzl * upY
            val sy = fzl * upX - fxl * upZ
            val sz = fxl * upY - fyl * upX
            val slen = sqrt(sx * sx + sy * sy + sz * sz)
            val sxl = sx / slen
            val syl = sy / slen
            val szl = sz / slen

            val ux = syl * fzl - szl * fyl
            val uy = szl * fxl - sxl * fzl
            val uz = sxl * fyl - syl * fxl

            return Matrix4(
                sxl, syl, szl, 0f,
                ux, uy, uz, 0f,
                -fxl, -fyl, -fzl, 0f,
                0f, 0f, 0f, 1f
            ).translate(-eyeX, -eyeY, -eyeZ)
        }

        /** Translation matrix (row-major: translation in last row). */
        fun translate(tx: Float, ty: Float, tz: Float): Matrix4 = Matrix4(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            tx, ty, tz, 1f
        )
    }

    /** Returns a copy with translation applied (post-multiply). */
    fun translate(tx: Float, ty: Float, tz: Float): Matrix4 = this.times(Matrix4(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        tx, ty, tz, 1f
    ))

    /** Multiplies this * other (row-major: this * other means apply other then this). */
    fun times(other: Matrix4): Matrix4 = Matrix4(
        m00 * other.m00 + m01 * other.m10 + m02 * other.m20 + m03 * other.m30,
        m00 * other.m01 + m01 * other.m11 + m02 * other.m21 + m03 * other.m31,
        m00 * other.m02 + m01 * other.m12 + m02 * other.m22 + m03 * other.m32,
        m00 * other.m03 + m01 * other.m13 + m02 * other.m23 + m03 * other.m33,

        m10 * other.m00 + m11 * other.m10 + m12 * other.m20 + m13 * other.m30,
        m10 * other.m01 + m11 * other.m11 + m12 * other.m11 + m13 * other.m31,
        m10 * other.m02 + m11 * other.m12 + m12 * other.m22 + m13 * other.m32,
        m10 * other.m03 + m11 * other.m13 + m12 * other.m23 + m13 * other.m33,

        m20 * other.m00 + m21 * other.m10 + m22 * other.m20 + m23 * other.m30,
        m20 * other.m01 + m21 * other.m11 + m22 * other.m21 + m23 * other.m31,
        m20 * other.m02 + m21 * other.m12 + m22 * other.m22 + m23 * other.m32,
        m20 * other.m03 + m21 * other.m13 + m22 * other.m23 + m23 * other.m33,

        m30 * other.m00 + m31 * other.m10 + m32 * other.m20 + m33 * other.m30,
        m30 * other.m01 + m31 * other.m11 + m32 * other.m21 + m33 * other.m31,
        m30 * other.m02 + m31 * other.m12 + m32 * other.m22 + m33 * other.m32,
        m30 * other.m03 + m31 * other.m13 + m32 * other.m33 + m33 * other.m33,
    )

    /** Transforms a Vec3 point (w=1) using row-vector convention: v' = v * M. */
    fun transformPoint(v: Vec3): Vec3 = Vec3(
        m00 * v.x + m10 * v.y + m20 * v.z + m30,
        m01 * v.x + m11 * v.y + m21 * v.z + m31,
        m02 * v.x + m12 * v.y + m22 * v.z + m32,
    )

    /** Transforms a Vec3 vector (w=0) using row-vector convention. */
    fun transformVector(v: Vec3): Vec3 = Vec3(
        m00 * v.x + m10 * v.y + m20 * v.z,
        m01 * v.x + m11 * v.y + m21 * v.z,
        m02 * v.x + m12 * v.y + m22 * v.z,
    )
}

/** Oriented bounding box for ray-box intersection. */
data class OBB(
    val center: Vec3,
    val halfExtents: Vec3,
    val rotation: Quaternion, // orientation of local axes
) {
    /** Tests ray (origin, direction) against this OBB.
     *  Returns `t` (distance along ray) of first hit, or null if miss. */
    fun rayIntersect(rayOrigin: Vec3, rayDir: Vec3): Float? {
        // Transform ray into OBB local space
        val invRot = rotation.inverted()
        val localOrigin = invRot.rotate(rayOrigin - center)
        val localDir = invRot.rotate(rayDir)

        // Slab test in local space (AABB)
        var tMin = -Float.MAX_VALUE
        var tMax = Float.MAX_VALUE

        // X slab
        if (abs(localDir.x) < 1e-6f) {
            if (localOrigin.x < -halfExtents.x || localOrigin.x > halfExtents.x) return null
        } else {
            val invD = 1.0f / localDir.x
            var t1 = (-halfExtents.x - localOrigin.x) * invD
            var t2 = (halfExtents.x - localOrigin.x) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            if (t1 > tMin) tMin = t1
            if (t2 < tMax) tMax = t2
            if (tMin > tMax) return null
        }
        // Y slab
        if (abs(localDir.y) < 1e-6f) {
            if (localOrigin.y < -halfExtents.y || localOrigin.y > halfExtents.y) return null
        } else {
            val invD = 1.0f / localDir.y
            var t1 = (-halfExtents.y - localOrigin.y) * invD
            var t2 = (halfExtents.y - localOrigin.y) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            if (t1 > tMin) tMin = t1
            if (t2 < tMax) tMax = t2
            if (tMin > tMax) return null
        }
        // Z slab
        if (abs(localDir.z) < 1e-6f) {
            if (localOrigin.z < -halfExtents.z || localOrigin.z > halfExtents.z) return null
        } else {
            val invD = 1.0f / localDir.z
            var t1 = (-halfExtents.z - localOrigin.z) * invD
            var t2 = (halfExtents.z - localOrigin.z) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            if (t1 > tMin) tMin = t1
            if (t2 < tMax) tMax = t2
            if (tMin > tMax) return null
        }

        return if (tMax >= 0f) tMin.coerceAtLeast(0f) else null
    }
}