/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * A single quad after projection into screen space.
 * All coordinates are in pixels (Compose Canvas space, y-down).
 */
package me.shadow.eclipselauncher.ui.character

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader

/**
 * A single quad after orthographic projection into screen space.
 * Vertices are ordered counter-clockwise in screen space.
 * @param x0..y3 Projected screen coordinates (8 floats: x0,y0,x1,y1,x2,y2,x3,y3).
 * @param u0..v3 Texture UVs in 0..1 range (mapped from 0..64 skin pixels).
 * @param depth Average Z of the four corners (larger = nearer camera).
 * @param frontFacing True if quad winds counter-clockwise in screen space.
 * @param overlay True for hat/jacket/sleeve/pants overlay layer.
 * @param lightColor Pre-multiplied ARGB color for this face's lighting.
 * @param shadow Optional 8-float array for the ground-projected shadow quad.
 */
class ProjectedQuad(
    val x0: Float, val y0: Float,
    val x1: Float, val y1: Float,
    val x2: Float, val y2: Float,
    val x3: Float, val y3: Float,
    val u0: Float, val v0: Float,
    val u1: Float, val v1: Float,
    val u2: Float, val v2: Float,
    val u3: Float, val v3: Float,
    val depth: Float,
    val frontFacing: Boolean,
    val overlay: Boolean,
    val lightColor: Int,
    val shadow: FloatArray? = null, // 8 floats: xs0,ys0,xs1,ys1,xs2,ys2,xs3,ys3
) {
    /** Returns true if this quad is a cape face. */
    fun isCape(): Boolean = false

    /** Draws this quad using the given bitmap and paint. */
    fun draw(canvas: Canvas, bitmap: android.graphics.Bitmap, paint: Paint) {
        if (!frontFacing) return
        if (bitmap == null) return
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        val matrix = android.graphics.Matrix()
        val src = floatArrayOf(
            u0 * width, v0 * height,
            u1 * width, v1 * height,
            u2 * width, v2 * height,
            u3 * width, v3 * height,
        )
        val dst = floatArrayOf(x0, y0, x1, y1, x2, y2, x3, y3)
        if (!matrix.setPolyToPoly(src, 0, dst, 0, 4)) return

        canvas.save()
        // Clip to the quad's shape to avoid bleeding at edges
        val path = android.graphics.Path()
        path.moveTo(x0, y0)
        path.lineTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.close()
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, matrix, paint)
        canvas.restore()
    }

    /** Draws the ground shadow for this quad. */
    fun drawShadow(canvas: Canvas, paint: Paint) {
        shadow?.let { s ->
            canvas.drawVertices(
                android.graphics.Canvas.VertexMode.TRIANGLES,
                8,
                s, 0,
                null, 0,
                intArrayOf(0x99000000, 0x99000000, 0x99000000, 0x99000000),
                0,
                shortArrayOf(0, 1, 2, 0, 2, 3),
                0,
                6,
                paint
            )
        }
    }
}

/** Specialized ProjectedQuad for cape faces (drawn with cape bitmap). */
class CapeProjectedQuad(
    x0: Float, y0: Float,
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    x3: Float, y3: Float,
    u0: Float, v0: Float,
    u1: Float, v1: Float,
    u2: Float, v2: Float,
    u3: Float, v3: Float,
    depth: Float,
    frontFacing: Boolean,
    lightColor: Int,
    shadow: FloatArray? = null,
) : ProjectedQuad(x0, y0, x1, y1, x2, y2, x3, y3, u0, v0, u1, v1, u2, v2, u3, v3, depth, frontFacing, false, lightColor, shadow) {
    override fun isCape(): Boolean = true
}

/** Off-screen buffers for self-shadow rendering (reused each frame). */
class ShadowBuffers {
    private var verts = FloatArray(0)
    private var colors = IntArray(0)
    private val layerPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }
    private val vertexPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }
    private val fadePaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }

    fun getVerts(): FloatArray = verts
    fun setVerts(newVerts: FloatArray) { verts = newVerts }
    fun getColors(): IntArray = colors
    fun setColors(newColors: IntArray) { colors = newColors }
    fun getLayerPaint(): Paint = layerPaint
    fun getVertexPaint(): Paint = vertexPaint
    fun getFadePaint(): Paint = fadePaint
}