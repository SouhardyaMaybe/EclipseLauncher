/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Software rasterizer with per-pixel z-buffer, matching ZL2 PetLib's approach.
 * Pure Kotlin/JVM — no Android types in core logic, only Bitmap at the boundary.
 */
package me.shadow.eclipselauncher.ui.character

import android.graphics.Bitmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Per-pixel depth buffer software rasterizer matching ZL2 PetLib's technique.
 * - Triangle rasterization with barycentric coordinates
 * - 256× fixed-point edge bias for exact edge sharing
 * - Pre-multiplied RGBA with alpha cutoff (16)
 * - Box-filter downsample to target size
 */
class SoftwareRasterizer(
    private val width: Int,
    private val height: Int,
) {
    private val depthBuffer = FloatArray(width * height) { Float.NEGATIVE_INFINITY }
    private val pixelBuffer = IntArray(width * height) { 0 } // Pre-multiplied RGBA as 0xAARRGGBB

    /** Renders a list of projected parts into the internal buffers. */
    fun render(parts: List<ProjectedPart>, light: Lighting = Lighting.FLAT): Bitmap {
        clear()
        // Sort: base layer (inflate == 0) first, then outer layer by camera-space depth (far -> near)
        val sorted = parts.sortedWith(
            compareBy<ProjectedPart> { it.inflate }
                .thenByDescending { it.cameraDepth }
        )

        for (part in sorted) {
            drawPart(part, light)
        }

        return toBitmap()
    }

    private fun clear() {
        depthBuffer.fill(Float.NEGATIVE_INFINITY)
        pixelBuffer.fill(0)
    }

    private fun drawPart(part: ProjectedPart, light: Lighting) {
        val tex = part.texture
        val sz = part.size.plus(Vec3(part.inflate))
        val inflate = part.inflate

        // 6 faces of the box, UV regions per ZL2's strip order: right/front/left/back then top/bottom
        // Face order: +X, -X, +Z, -Z, +Y, -Y (ZL2 render.rs regions array)
        val regions = arrayOf(
            UVRegion(part.uv.x + part.depth + part.width, part.uv.y + part.depth, part.depth, part.height), // +X (right)
            UVRegion(part.uv.x, part.uv.y + part.depth, part.depth, part.height),                         // -X (left)
            UVRegion(part.uv.x + part.depth, part.uv.y, part.width, part.depth),                          // +Z (front)
            UVRegion(part.uv.x + part.depth + part.width, part.uv.y, part.width, part.depth),             // -Z (back)
            UVRegion(part.uv.x + part.depth, part.uv.y + part.depth, part.width, part.height),            // +Y (top)
            UVRegion(part.uv.x + 2 * part.depth + part.width, part.uv.y + part.depth, part.width, part.height), // -Y (bottom)
        )

        val planes = arrayOf(
            Plane(2, 1, 0, -1f, -1f, sz.z, sz.y, sz.x),   // +X
            Plane(2, 1, 0,  1f, -1f, sz.z, sz.y, -sz.x),  // -X
            Plane(0, 2, 1,  1f,  1f, sz.x, sz.z, sz.y),   // +Z
            Plane(0, 2, 1,  1f, -1f, sz.x, sz.z, -sz.y),  // -Z
            Plane(0, 1, 2,  1f, -1f, sz.x, sz.y, sz.z),   // +Y
            Plane(0, 1, 2, -1f, -1f, sz.x, sz.y, -sz.z),  // -Y
        )

        val normals = arrayOf(
            Vec3(1f, 0f, 0f), Vec3(-1f, 0f, 0f),
            Vec3(0f, 1f, 0f), Vec3(0f, -1f, 0f),
            Vec3(0f, 0f, 1f), Vec3(0f, 0f, -1f),
        )

        for (f in 0..5) {
            val (uax, vax, wax, ud, vd, ww, hh, dd) = planes[f].toTuple()
            val normal = normals[f]
            val litNormal = part.transform.transformVector3(normal).normalize()
            val shade = light.shade(litNormal)
            val lut = light.lut(shade)

            val (u, v, tw, th) = regions[f].toTuple()
            val pos = Vec3Array(4)
            val uv = Vec2Array(4)

            for (iy in 0..1) {
                for (ix in 0..1) {
                    val i = iy * 2 + ix
                    var a = Vec3.ZERO
                    a = a.setComponent(uax, (ix * ww - ww / 2f) * ud)
                    a = a.setComponent(vax, (iy * hh - hh / 2f) * vd)
                    a = a.setComponent(wax, dd / 2f)
                    if (part.mirror) a.x = -a.x

                    pos.set(i, project(part.transform.transformPoint3(a)))
                    uv.set(i, Vec2(
                        u + ix * tw,
                        v + (if (f == 3) (1 - iy) * th else iy * th)
                    ))
                }
            }

            // Two triangles per quad, winding depends on face orientation
            for (ids in listOf(intArrayOf(0, 2, 1), intArrayOf(2, 3, 1))) {
                val p0 = pos.get(ids[0])
                val p1 = pos.get(ids[1])
                val p2 = pos.get(ids[2])
                val area = edge(p0, p1, p2)
                if (inflate == 0f && ((area >= 0f) != part.mirror)) continue

                drawTriangle(ids, pos, uv, part.texture, lut)
            }
        }
    }

    private fun drawTriangle(
        ids: IntArray,
        pos: Vec3Array,
        uv: Vec2Array,
        tex: Texture,
        lut: Array<UByteArray>
    ) {
        val p0 = pos.get(ids[0])
        val p1 = pos.get(ids[1])
        val p2 = pos.get(ids[2])
        val area = edge(p0, p1, p2)
        if (abs(area) < 0.0001f) return

        val invArea = 1f / area
        val minX = max(0, floor(min(min(p0.x, p1.x), p2.x)).toInt())
        val maxX = min(width - 1, ceil(max(max(p0.x, p1.x), p2.x)).toInt())
        val minY = max(0, floor(min(min(p0.y, p1.y), p2.y)).toInt())
        val maxY = min(height - 1, ceil(max(max(p0.y, p1.y), p2.y)).toInt())
        if (minX > maxX || minY > maxY) return

        val t0 = uv.get(0).times(tex.scale)
        val t1 = uv.get(1).times(tex.scale)
        val t2 = uv.get(2).times(tex.scale)
        val ta = t0.minus(t1)
        val tc = t2.minus(t1)

        // 256× fixed-point edge bias for exact edge sharing
        val points = arrayOf(
            longArrayOf((p0.x * 256f).roundToLong(), (p0.y * 256f).roundToLong()),
            longArrayOf((p1.x * 256f).roundToLong(), (p1.y * 256f).roundToLong()),
            longArrayOf((p2.x * 256f).roundToLong(), (p2.y * 256f).roundToLong())
        )
        val edges = arrayOf(
            edge256(points[ids[0]], points[ids[1]]),
            edge256(points[ids[1]], points[ids[2]]),
            edge256(points[ids[2]], points[ids[0]])
        )
        val stepY256 = longArrayOf(edges[0][1] * 256L, edges[1][1] * 256L, edges[2][1] * 256L)
        val texW = tex.width
        val invArea = 1f / edge(p0, p1, p2)

        // Barycentric coefficients
        val ax = -(p2.y - p1.y) * invArea
        val ay =  (p2.x - p1.x) * invArea
        val cx = -(p1.y - p0.y) * invArea
        val cy =  (p1.x - p0.x) * invArea

        val origin = Vec3(minX.toFloat() + 0.5f, minY.toFloat() + 0.5f, 0f)
        val a0 = edge(uv.get(ids[1]), uv.get(ids[2]), origin) * invArea
        val c0 = edge(uv.get(ids[0]), uv.get(ids[1]), origin) * invArea
        val za = pos.get(ids[0]).z - pos.get(ids[1]).z
        val zc = pos.get(ids[2]).z - pos.get(ids[1]).z

        for (y in minY..maxY) {
            val row = y * width
            val stepY = (y - minY) * 256L
            var coverage = longArrayOf(
                edges[0][2] + edges[0][1] * stepY,
                edges[1][2] + edges[1][1] * stepY,
                edges[2][2] + edges[2][1] * stepY
            )
            val steps = longArrayOf(stepY256[0], stepY256[1], stepY256[2])

            val rowA = a0 + (y - minY) * ay
            val rowC = c0 + (y - minY) * cy

            var wrote = false
            var rowLo = 0
            var rowHi = 0

            for (x in minX..maxX) {
                val i = x - minX
                val covered = coverage[0] >= 0 && coverage[1] >= 0 && coverage[2] >= 0
                coverage = longArrayOf(
                    coverage[0] - steps[0],
                    coverage[1] - steps[1],
                    coverage[2] - steps[2]
                )
                if (!covered) continue

                val a = rowA + i * ax
                val c = rowC + i * cx
                val z = p1.z + za * a + zc * c
                val idx = row + x
                if (z >= depthBuffer[idx]) continue

                val tx = t1.x + ta.x * a + tc.x * c
                val ty = t1.y + ta.y * a + tc.y * c
                val xx = min(max((tx * tex.scale).toInt(), 0), tex.width - 1)
                val yy = min(max((ty * tex.scale).toInt(), 0), tex.height - 1)
                val base = tex.pixels[yy * texW + xx]
                if (base[3] < 16) continue

                val alpha = base[3].toInt()
                val pixelIdx = row + x
                if (alpha == 255) {
                    pixelBuffer[pixelIdx] = ((255 shl 24) |
                        (lut[0][base[0].toInt()] shl 16) |
                        (lut[1][base[1].toInt()] shl 8) |
                        lut[2][base[2].toInt()])
                } else {
                    val inv = 255 - alpha
                    val old = pixelBuffer[pixelIdx]
                    val oldR = (old shr 16) and 0xFF
                    val oldG = (old shr 8) and 0xFF
                    val oldB = old and 0xFF
                    val oldA = (old shr 24) and 0xFF
                    val newA = (alpha + ((oldA * inv + 127) / 255)).toInt()
                    val newR = ((lut[0][base[0].toInt()] * alpha + oldR * inv + 127) / 255).toInt()
                    val newG = ((lut[1][base[1].toInt()] * alpha + oldG * inv + 127) / 255).toInt()
                    val newB = ((lut[2][base[2].toInt()] * alpha + oldB * inv + 127) / 255).toInt()
                    pixelBuffer[pixelIdx] = (newA shl 24) | (newR shl 16) | (newG shl 8) | newB
                }
                depthBuffer[pixelIdx] = z
                if (!wrote) { rowLo = i; wrote = true }
                rowHi = i
            }
        }
    }

    private fun project(v: Vec3): Vec3 {
        // Match ZL2's weak perspective projection
        val forward = Vec3(30f, 23f, 70f).normalize()
        val right = Vec3(0f, 1f, 0f).cross(forward).normalize()
        val up = forward.cross(right)
        val v2 = v.minus(Vec3(0f, 16f, 0f))
        val scale = width / 44f
        return Vec3(
            width / 2f + v2.dot(right) * scale,
            height / 2f - v2.dot(up) * scale,
            -v2.dot(forward)
        )
    }

    private fun toBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        for (i in pixelBuffer.indices) {
            pixels[i] = pixelBuffer[i]
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}

// --- Data classes ---

data class Vec3(
    var x: Float = 0f, var y: Float = 0f, var z: Float = 0f
) {
    companion object {
        val ZERO = Vec3()
        val X = Vec3(1f, 0f, 0f)
        val Y = Vec3(0f, 1f, 0f)
        val Z = Vec3(0f, 0f, 1f)
    }
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    operator fun div(s: Float) = Vec3(x / s, y / s, z / s)
    fun dot(other: Vec3) = x * other.x + y * other.y + z * other.z
    fun cross(other: Vec3) = Vec3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)
    fun length() = sqrt(dot(this))
    fun normalize() = this.div(length())
    fun setComponent(index: Int, value: Float): Vec3 = when (index) {
        0 -> Vec3(value, y, z)
        1 -> Vec3(x, value, z)
        2 -> Vec3(x, y, value)
        else -> throw IndexOutOfBoundsException()
    }
    fun transformPoint3(m: Mat4): Vec3 {
        val w = m.m03 * x + m.m13 * y + m.m23 * z + m.m33
        return Vec3(
            (m.m00 * x + m.m10 * y + m.m20 * z + m.m30) / w,
            (m.m01 * x + m.m11 * y + m.m21 * z + m.m31) / w,
            (m.m02 * x + m.m12 * y + m.m22 * z + m.m32) / w
        )
    }
    fun transformVector3(m: Mat4): Vec3 = Vec3(
        m.m00 * x + m.m10 * y + m.m20 * z,
        m.m01 * x + m.m11 * y + m.m21 * z,
        m.m02 * x + m.m12 * y + m.m22 * z
    )
    operator fun unaryMinus() = Vec3(-x, -y, -z)
}

data class Vec2(var x: Float = 0f, var y: Float = 0f) {
    companion object { val ZERO = Vec2() }
    operator fun times(s: Float) = Vec2(x * s, y * s)
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
}

class Mat4(
    var m00: Float = 1f, var m01: Float = 0f, var m02: Float = 0f, var m03: Float = 0f,
    var m10: Float = 0f, var m11: Float = 1f, var m12: Float = 0f, var m13: Float = 0f,
    var m20: Float = 0f, var m21: Float = 0f, var m22: Float = 1f, var m23: Float = 0f,
    var m30: Float = 0f, var m31: Float = 0f, var m32: Float = 0f, var m33: Float = 1f
) {
    companion object {
        fun fromTranslation(v: Vec3) = Mat4().apply {
            m03 = v.x; m13 = v.y; m23 = v.z
        }
        fun fromScale(v: Vec3) = Mat4().apply {
            m00 = v.x; m11 = v.y; m22 = v.z
        }
        fun fromRotationY(angle: Float) = Mat4().apply {
            val c = kotlin.math.cos(angle); val s = kotlin.math.sin(angle)
            m00 = c; m02 = s; m20 = -s; m22 = c
        }
        fun fromQuat(q: Quat) = Mat4().apply {
            val xx = q.x * q.x; val yy = q.y * q.y; val zz = q.z * q.z
            val xy = q.x * q.y; val xz = q.x * q.z; val yz = q.y * q.z
            val wx = q.w * q.x; val wy = q.w * q.y; val wz = q.w * q.z
            m00 = 1 - 2 * (yy + zz); m01 = 2 * (xy - wz); m02 = 2 * (xz + wy)
            m10 = 2 * (xy + wz); m11 = 1 - 2 * (xx + zz); m12 = 2 * (yz - wx)
            m20 = 2 * (xz - wy); m21 = 2 * (yz + wx); m22 = 1 - 2 * (xx + yy)
        }
        operator fun times(other: Mat4): Mat4 = Mat4().apply {
            for (i in 0..3) for (j in 0..3) {
                this[i * 4 + j] = (0..3).sumOf { k -> this@times[i * 4 + k] * other[k * 4 + j] }
            }
        }
        fun transformPoint3(v: Vec3): Vec3 {
            val w = m03 * v.x + m13 * v.y + m23 * v.z + m33
            return Vec3(
                (m00 * v.x + m10 * v.y + m20 * v.z + m30) / w,
                (m01 * v.x + m11 * v.y + m21 * v.z + m31) / w,
                (m02 * v.x + m12 * v.y + m22 * v.z + m32) / w
            )
        }
        fun transformVector3(v: Vec3): Vec3 = Vec3(
            m00 * v.x + m10 * v.y + m20 * v.z,
            m01 * v.x + m11 * v.y + m21 * v.z,
            m02 * v.x + m12 * v.y + m22 * v.z
        )
        operator fun get(index: Int): Float = when (index) {
            0 -> m00; 1 -> m01; 2 -> m02; 3 -> m03
            4 -> m10; 5 -> m11; 6 -> m12; 7 -> m13
            8 -> m20; 9 -> m21; 10 -> m22; 11 -> m23
            12 -> m30; 13 -> m31; 14 -> m32; 15 -> m33
            else -> throw IndexOutOfBoundsException()
        }
    }
    fun transformPoint3(v: Vec3): Vec3 = Mat4.fromTranslation(Vec3.ZERO).times(this).transformPoint3(v)
    fun transformVector3(v: Vec3): Vec3 = Mat4.fromTranslation(Vec3.ZERO).times(this).transformVector3(v)
}

data class Quat(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {
    companion object {
        val IDENTITY = Quat(0f, 0f, 0f, 1f)
        fun fromEuler(x: Float, y: Float, z: Float): Quat {
            val cx = kotlin.math.cos(x / 2); val sx = kotlin.math.sin(x / 2)
            val cy = kotlin.math.cos(y / 2); val sy = kotlin.math.sin(y / 2)
            val cz = kotlin.math.cos(z / 2); val sz = kotlin.math.sin(z / 2)
            return Quat(
                sx * cy * cz - cx * sy * sz,
                cx * sy * cz + sx * cy * sz,
                cx * cy * sz - sx * sy * cz,
                cx * cy * cz + sx * sy * sz
            )
        }
        fun fromRotationY(angle: Float) = fromEuler(0f, angle, 0f)
        fun slerp(a: Quat, b: Quat, t: Float): Quat {
            var dot = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w
            var bx = b.x; var by = b.y; var bz = b.z; var bw = b.w
            if (dot < 0) { dot = -dot; bx = -bx; by = -by; bz = -bz; bw = -bw }
            val theta = kotlin.math.acos(dot.clamp(-1f, 1f))
            val sinTheta = kotlin.math.sin(theta)
            if (sinTheta < 1e-6f) return a
            val ratioA = kotlin.math.sin((1 - t) * theta) / sinTheta
            val ratioB = kotlin.math.sin(t * theta) / sinTheta
            return Quat(
                a.x * ratioA + b.x * ratioB,
                a.y * ratioA + b.y * ratioB,
                a.z * ratioA + b.z * ratioB,
                a.w * ratioA + b.w * ratioB
            ).normalize()
        }
    }
    fun normalize() = this.div(sqrt(x * x + y * y + z * z + w * w))
    fun absDiffEq(other: Quat, eps: Float) = (x - other.x).absoluteValue < eps && (y - other.y).absoluteValue < eps && (z - other.z).absoluteValue < eps && (w - other.w).absoluteValue < eps
    operator fun div(s: Float) = Quat(x / s, y / s, z / s, w / s)
    fun normalize(): Quat = div(sqrt(x * x + y * y + z * z + w * w))
}

data class UVRegion(var x: Float, var y: Float, var w: Float, var h: Float) {
    fun toTuple(): Triple<Float, Float, Triple<Float, Float, Float>> = Triple(x, y, Triple(w, h, 0f))
}

data class Plane(var uax: Int, var vax: Int, var wax: Int, var ud: Float, var vd: Float, var ww: Float, var hh: Float, var dd: Float) {
    fun toTuple() = arrayOf(uax, vax, wax, ud, vd, ww, hh, dd)
}

class Texture(val width: Int, val height: Int, val pixels: Array<UByteArray>, val scale: Float = 1f) {
    val texW = width
    val texH = height
}

class Vec3Array(val size: Int) {
    val array = Array(size) { Vec3() }
    operator fun get(i: Int) = array[i]
    fun set(i: Int, v: Vec3) { array[i] = v }
}

class Vec2Array(val size: Int) {
    val array = Array(size) { Vec2() }
    operator fun get(i: Int) = array[i]
    fun set(i: Int, v: Vec2) { array[i] = v }
}

data class ProjectedPart(
    val texture: Texture,
    val transform: Mat4,
    val size: Vec3,
    val uv: Vec2,
    val depth: Float,
    val width: Float,
    val height: Float,
    val inflate: Float,
    val mirror: Boolean,
    val cameraDepth: Float,
)

interface Lighting {
    fun shade(normal: Vec3): Float
    fun lut(shade: Float): Array<UByteArray>
    companion object {
        val FLAT = object : Lighting {
            override fun shade(normal: Vec3) = 1f
            override fun lut(shade: Float) = Array(3) { UByteArray(256) { 255 } }
        }
    }
}

private fun edge(a: Vec3, b: Vec3, p: Vec3): Float = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
private fun edge256(a: LongArray, b: LongArray): LongArray {
    val dx = b[0] - a[0]
    val dy = b[1] - a[1]
    val bias = if (dy < 0 || (dy == 0 && dx > 0)) 0L else -1L
    val start = dx * a[1] - dy * a[0] + bias
    return longArrayOf(dx, dy, start)
}

private fun UByteArray(size: Int, init: (Int) -> Byte): UByteArray = UByteArray(size).also { arr -> for (i in 0 until size) arr[i] = init(i) }