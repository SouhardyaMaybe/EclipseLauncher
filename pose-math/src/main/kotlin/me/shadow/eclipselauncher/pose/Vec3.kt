/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Pure Kotlin 3D vector. Coordinates are skin pixels; see README.md.
 */
package me.shadow.eclipselauncher.pose

import kotlin.math.sqrt

/** A point or direction in the pose-math coordinate system. */
data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)

    operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)

    operator fun times(scale: Float): Vec3 = Vec3(x * scale, y * scale, z * scale)

    operator fun div(scale: Float): Vec3 = Vec3(x / scale, y / scale, z / scale)

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    fun lengthSquared(): Float = dot(this)

    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Vec3 {
        val length = length()
        require(length > 0f) { "Cannot normalize a zero vector" }
        return this / length
    }

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val X = Vec3(1f, 0f, 0f)
        val Y = Vec3(0f, 1f, 0f)
        val Z = Vec3(0f, 0f, 1f)
    }
}
