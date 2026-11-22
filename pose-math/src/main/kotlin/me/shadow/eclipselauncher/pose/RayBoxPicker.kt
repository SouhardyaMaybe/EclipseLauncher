/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ray/oriented-box intersection for skin-part picking. Pure math; the camera
 * builds the ray and the renderer decides what to do with the hit.
 */
package me.shadow.eclipselauncher.pose

import kotlin.math.abs

/**
 * A ray. [direction] need not be unit length; the returned hit parameter is in
 * units of [direction], so callers that want distance should normalize it.
 */
data class Ray(
    val origin: Vec3,
    val direction: Vec3,
) {
    init {
        require(direction.lengthSquared() > 0f) { "Ray direction must be non-zero" }
    }

    fun pointAt(t: Float): Vec3 = origin + direction * t
}

/**
 * Oriented box. Axes must be unit vectors and mutually orthogonal; each axis
 * is one local box axis expressed in world space. The default is axis-aligned.
 */
data class OrientedBox(
    val center: Vec3,
    val halfExtents: Vec3,
    val axisX: Vec3 = Vec3.X,
    val axisY: Vec3 = Vec3.Y,
    val axisZ: Vec3 = Vec3.Z,
) {
    companion object {
        fun axisAligned(center: Vec3, halfExtents: Vec3): OrientedBox =
            OrientedBox(center = center, halfExtents = halfExtents)
    }
}

object RayBoxPicker {
    private const val PARALLEL_EPSILON = 1e-6f

    /**
     * Returns the ray parameter of the first non-negative intersection, or
     * `null` if the ray misses. A ray starting inside the box returns `0`.
     */
    fun intersect(ray: Ray, box: OrientedBox): Float? {
        val relativeOrigin = ray.origin - box.center
        var tMin = -Float.MAX_VALUE
        var tMax = Float.MAX_VALUE

        fun clip(offset: Float, direction: Float, halfExtent: Float): Boolean {
            if (abs(direction) < PARALLEL_EPSILON) {
                return offset >= -halfExtent && offset <= halfExtent
            }
            var t1 = (-halfExtent - offset) / direction
            var t2 = (halfExtent - offset) / direction
            if (t1 > t2) {
                val swap = t1
                t1 = t2
                t2 = swap
            }
            if (t1 > tMin) tMin = t1
            if (t2 < tMax) tMax = t2
            return tMin <= tMax
        }

        if (!clip(relativeOrigin.dot(box.axisX), ray.direction.dot(box.axisX), box.halfExtents.x)) {
            return null
        }
        if (!clip(relativeOrigin.dot(box.axisY), ray.direction.dot(box.axisY), box.halfExtents.y)) {
            return null
        }
        if (!clip(relativeOrigin.dot(box.axisZ), ray.direction.dot(box.axisZ), box.halfExtents.z)) {
            return null
        }

        return if (tMax >= 0f) tMin.coerceAtLeast(0f) else null
    }
}
