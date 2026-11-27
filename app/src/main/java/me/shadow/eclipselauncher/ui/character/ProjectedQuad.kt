/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.ProjectedQuad
 */
package me.shadow.eclipselauncher.ui.character

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

open class ProjectedQuad(
    val x0: Float, val y0: Float,
    val x1: Float, val y1: Float,
    val x2: Float, val y2: Float,
    val x3: Float, val y3: Float,
    val u0: Float, val v0: Float,
    val u1: Float, val v1: Float,
    val u2: Float, val v2: Float,
    val u3: Float, val v3: Float,
    val depth: Float,
    val isFrontFacing: Boolean,
    val isOverlay: Boolean,
    val lightingColor: Int,
    val shadow: FloatArray? = null
) {

    open fun isCape(): Boolean = false

    fun draw(canvas: Canvas, bitmap: android.graphics.Bitmap, paint: Paint) {
        if (!isFrontFacing) return
        if (bitmap == null) return
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        val matrix = android.graphics.Matrix()
        val src = floatArrayOf(
            u0 * width, v0 * height,
            u1 * width, v1 * height,
            u2 * width, v2 * height,
            u3 * width, v3 * height
        )
        val dst = floatArrayOf(x0, y0, x1, y1, x2, y2, x3, y3)
        if (!matrix.setPolyToPoly(src, 0, dst, 0, 4)) return

        canvas.save()
        val path = Path()
        path.moveTo(x0, y0)
        path.lineTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.close()
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, matrix, paint)
        canvas.restore()
    }

    fun drawShadow(canvas: Canvas, paint: Paint) {
        shadow?.let { s ->
            canvas.drawVertices(
                android.graphics.Canvas.VertexMode.TRIANGLES,
                8,
                s, 0,
                null, 0,
                intArrayOf(0x99000000.toInt(), 0x99000000.toInt(), 0x99000000.toInt(), 0x99000000.toInt()),
                0,
                shortArrayOf(0, 1, 2, 0, 2, 3),
                0,
                6,
                paint
            )
        }
    }
}

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
    isFrontFacing: Boolean,
    lightingColor: Int,
    shadow: FloatArray? = null
) : ProjectedQuad(
    x0, y0, x1, y1, x2, y2, x3, y3,
    u0, v0, u1, v1, u2, v2, u3, v3,
    depth, isFrontFacing, false, lightingColor, shadow
) {
    override fun isCape(): Boolean = true
}

class ShadowBuffers {
    private var verts = FloatArray(0)
    private var colors = IntArray(0)
    private val layerPaint = Paint().apply { isAntiAlias = true }
    private val vertexPaint = Paint().apply { isAntiAlias = true }
    private val fadePaint = Paint().apply {
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
    }

    fun getVerts(): FloatArray = verts
    fun getColors(): IntArray = colors
    fun getLayerPaint(): Paint = layerPaint
    fun getVertexPaint(): Paint = vertexPaint
    fun getFadePaint(): Paint = fadePaint
    fun setVerts(fArr: FloatArray) { fArr.getClass(); verts = fArr }
    fun setColors(iArr: IntArray) { iArr.getClass(); colors = iArr }
}
