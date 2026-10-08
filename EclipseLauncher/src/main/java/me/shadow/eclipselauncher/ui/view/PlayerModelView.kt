package me.shadow.eclipselauncher.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Drag-to-rotate 3D preview of the player model wearing a skin and a cape.
 *
 * The model is a set of textured boxes projected straight onto the regular view
 * canvas with painter's algorithm: no GL context, no render thread and no work
 * done between drags, so the preview stays smooth while you spin it.
 *
 * Model space uses Minecraft's layout: 64 units across one skin row, Y grows
 * downwards and the player faces +Z. The cape box hangs on -Z (behind the
 * shoulders) with its decorated side facing away from the body.
 */
class PlayerModelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * One textured quad: four model-space corners ordered TL, TR, BR, BL of its
     * texture rect, the outward normal, and the rect in texture pixels.
     */
    private class Face(
        val corners: FloatArray,
        val nx: Float,
        val ny: Float,
        val nz: Float,
        val u0: Float,
        val v0: Float,
        val u1: Float,
        val v1: Float,
        val usesCape: Boolean
    )

    companion object {
        /** Radians of rotation per pixel of drag */
        private const val DRAG_SPEED = 0.012f
        /** How far the model may tilt forwards or backwards */
        private const val MAX_PITCH = 0.9f
        /** Camera sits on +Z and looks along -Z, so the face is visible at yaw 0 */
        private const val CAMERA_DISTANCE = 40f
        /** Equal to the camera distance so the projection scale stays near 1x */
        private const val FOCAL_LENGTH = 40f
        /** The model spans 0..32 units vertically */
        private const val MODEL_CENTER_Y = 16f
        /** Bleed past every quad edge so neighbouring quads never leave hairline seams */
        private const val SEAM_FILL = 1.2f

        /** Shared fallback texture for accounts without a custom skin */
        private var defaultSkin: Bitmap? = null
    }

    private var skinBitmap: Bitmap? = null
    private var capeBitmap: Bitmap? = null
    private var effectiveSkin: Bitmap? = null
    private var faces: List<Face> = emptyList()

    /** Yaw starts at 0 so the face points at the camera */
    private var yaw = 0f
    private var pitch = 0f
    private var lastX = 0f
    private var lastY = 0f

    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val clipPath = Path()
    private val matrix = Matrix()
    private val srcPoints = FloatArray(6)
    private val dstPoints = FloatArray(6)

    init {
        rebuild()
    }

    /** Set the skin to preview; null falls back to the bundled default skin */
    fun setSkin(bitmap: Bitmap?) {
        skinBitmap = bitmap
        rebuild()
        invalidate()
    }

    /** Set the cape to preview; null hides the cape */
    fun setCape(bitmap: Bitmap?) {
        capeBitmap = bitmap
        rebuild()
        invalidate()
    }

    private fun rebuild() {
        val skin = skinBitmap ?: loadDefaultSkin()
        effectiveSkin = skin
        faces = if (skin == null) emptyList() else buildFaces(skin, capeBitmap)
    }

    private fun loadDefaultSkin(): Bitmap? {
        defaultSkin?.let { return it }
        return runCatching {
            context.assets.open("steve.png").use { BitmapFactory.decodeStream(it) }?.also {
                it.density = Bitmap.DENSITY_NONE
                defaultSkin = it
            }
        }.getOrNull()
    }

    private fun buildFaces(skin: Bitmap, cape: Bitmap?): List<Face> {
        val result = ArrayList<Face>(78)
        // Legacy 64x32 skins mirror the right limbs and carry no overlay layer
        val legacy = skin.height * 2 <= skin.width
        val overlays = skin.height > skin.width / 2

        fun skinBox(
            x0: Float, y0: Float, z0: Float,
            x1: Float, y1: Float, z1: Float,
            u: Int, v: Int, w: Int, h: Int, d: Int
        ) = addBox(result, skin, false, x0, y0, z0, x1, y1, z1, u, v, w, h, d, false)

        // Head with its hat layer
        skinBox(-4f, 0f, -4f, 4f, 8f, 4f, 0, 0, 8, 8, 8)
        if (overlays) skinBox(-4.5f, -0.5f, -4.5f, 4.5f, 8.5f, 4.5f, 32, 0, 8, 8, 8)
        // Body with its jacket
        skinBox(-4f, 8f, -2f, 4f, 20f, 2f, 16, 16, 8, 12, 4)
        if (overlays) skinBox(-4.25f, 7.75f, -2.25f, 4.25f, 20.25f, 2.25f, 16, 32, 8, 12, 4)
        // Arms with their sleeves; the left arm mirrors the right one on legacy skins
        skinBox(-8f, 8f, -2f, -4f, 20f, 2f, 40, 16, 4, 12, 4)
        skinBox(
            4f, 8f, -2f, 8f, 20f, 2f,
            if (legacy) 40 else 32, if (legacy) 16 else 48, 4, 12, 4
        )
        if (overlays) {
            skinBox(-8.25f, 7.75f, -2.25f, -3.75f, 20.25f, 2.25f, 40, 32, 4, 12, 4)
            skinBox(3.75f, 7.75f, -2.25f, 8.25f, 20.25f, 2.25f, 48, 32, 4, 12, 4)
        }
        // Legs with their trousers; the left leg mirrors the right one on legacy skins
        skinBox(-4f, 20f, -2f, 0f, 32f, 2f, 0, 16, 4, 12, 4)
        skinBox(
            0f, 20f, -2f, 4f, 32f, 2f,
            if (legacy) 0 else 16, if (legacy) 16 else 48, 4, 12, 4
        )
        if (overlays) {
            skinBox(-4.25f, 19.75f, -2.25f, 0.25f, 32.25f, 2.25f, 0, 32, 4, 12, 4)
            skinBox(-0.25f, 19.75f, -2.25f, 4.25f, 32.25f, 2.25f, 0, 48, 4, 12, 4)
        }
        // Cape, hung behind the shoulders; flipped so the decorated side faces outwards
        cape?.let {
            addBox(result, it, true, -5f, 8f, -3f, 5f, 24f, -2f, 0, 0, 10, 16, 1, true)
        }
        return result
    }

    /**
     * Emit the six faces of a box. UV rects follow Minecraft's unwrap layout:
     * the face texture sits at `(u+d, v+d)` with the sides flanking it and the
     * top/bottom rows above. With [flip] the whole box is rotated 180 degrees
     * around Y, which mirrors X and Z in every corner and normal.
     */
    private fun addBox(
        result: MutableList<Face>,
        texture: Bitmap,
        capeTexture: Boolean,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        u: Int, v: Int, w: Int, h: Int, d: Int,
        flip: Boolean
    ) {
        // Scale the 64-unit layout coordinates into texture pixels
        val s = texture.width / 64f
        val U = u * s
        val V = v * s
        val W = w * s
        val H = h * s
        val D = d * s

        fun face(
            model: FloatArray,
            fx: Float, fy: Float, fz: Float,
            ru0: Float, rv0: Float, ru1: Float, rv1: Float
        ) {
            val corners: FloatArray
            var nx = fx
            val ny = fy
            var nz = fz
            if (flip) {
                val mirrored = FloatArray(12)
                for (i in 0 until 4) {
                    mirrored[i * 3] = x0 + x1 - model[i * 3]
                    mirrored[i * 3 + 1] = model[i * 3 + 1]
                    mirrored[i * 3 + 2] = z0 + z1 - model[i * 3 + 2]
                }
                corners = mirrored
                nx = -fx
                nz = -fz
            } else {
                corners = model
            }
            result.add(
                Face(corners, nx, ny, nz, U + ru0, V + rv0, U + ru1, V + rv1, capeTexture)
            )
        }

        // Front (+Z): the side the player looks out of
        face(
            floatArrayOf(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1),
            0f, 0f, 1f, D, D, W + D, D + H
        )
        // Back (-Z)
        face(
            floatArrayOf(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0),
            0f, 0f, -1f, W + 2 * D, D, 2 * W + 2 * D, D + H
        )
        // Right side (+X)
        face(
            floatArrayOf(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0),
            1f, 0f, 0f, W + D, D, W + 2 * D, D + H
        )
        // Left side (-X)
        face(
            floatArrayOf(x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1),
            -1f, 0f, 0f, 0f, D, D, D + H
        )
        // Top (-Y)
        face(
            floatArrayOf(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1),
            0f, -1f, 0f, D, 0f, W + D, D
        )
        // Bottom (+Y)
        face(
            floatArrayOf(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0),
            0f, 1f, 0f, W + D, 0f, 2 * W + D, D
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val skin = effectiveSkin
        if (faces.isEmpty() || skin == null || width == 0 || height == 0) return

        val count = faces.size
        val px = FloatArray(count * 4)
        val py = FloatArray(count * 4)
        val depth = FloatArray(count)
        val visible = IntArray(count)
        var visibleCount = 0

        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosPitch = cos(pitch)
        val sinPitch = sin(pitch)
        val scale = min(width / 24f, height / 40f)
        val centerX = width / 2f
        val centerY = height / 2f

        for (i in 0 until count) {
            val face = faces[i]
            // Rotate the normal; only faces leaning towards the camera are drawn
            val nzYaw = -face.nx * sinYaw + face.nz * cosYaw
            val nzFinal = face.ny * sinPitch + nzYaw * cosPitch
            if (nzFinal <= 0f) continue

            val corners = face.corners
            var sumZ = 0f
            for (c in 0 until 4) {
                val o = c * 3
                // Yaw around the vertical axis through the model
                var x = corners[o] * cosYaw + corners[o + 2] * sinYaw
                var z = -corners[o] * sinYaw + corners[o + 2] * cosYaw
                // Pitch around the horizontal axis through the model's centre
                val yc = corners[o + 1] - MODEL_CENTER_Y
                val yr = MODEL_CENTER_Y + yc * cosPitch - z * sinPitch
                z = yc * sinPitch + z * cosPitch
                // Perspective projection towards the camera on +Z
                val dz = CAMERA_DISTANCE - z
                sumZ += dz
                val k = FOCAL_LENGTH / dz
                px[i * 4 + c] = centerX + x * k * scale
                py[i * 4 + c] = centerY + (yr - MODEL_CENTER_Y) * k * scale
            }
            depth[i] = sumZ * 0.25f
            visible[visibleCount++] = i
        }
        if (visibleCount == 0) return

        // Far quads first so nearer boxes paint over them
        for (i in 1 until visibleCount) {
            val idx = visible[i]
            val d = depth[idx]
            var j = i - 1
            while (j >= 0 && depth[visible[j]] < d) {
                visible[j + 1] = visible[j]
                j--
            }
            visible[j + 1] = idx
        }

        for (i in 0 until visibleCount) {
            val index = visible[i]
            val face = faces[index]
            val texture = if (face.usesCape) capeBitmap else skin
            if (texture == null) continue
            val b = index * 4

            // Bleed each corner away from the quad centre to hide seams
            val ox = (px[b] + px[b + 1] + px[b + 2] + px[b + 3]) * 0.25f
            val oy = (py[b] + py[b + 1] + py[b + 2] + py[b + 3]) * 0.25f
            for (c in 0 until 4) {
                val dx = px[b + c] - ox
                val dy = py[b + c] - oy
                val len = hypot(dx, dy)
                if (len > 0.001f) {
                    px[b + c] = ox + dx / len * (len + SEAM_FILL)
                    py[b + c] = oy + dy / len * (len + SEAM_FILL)
                }
            }

            // Two affine triangles cover the quad exactly
            drawTriangle(
                canvas, texture,
                face.u0, face.v0, face.u1, face.v0, face.u1, face.v1,
                px[b], py[b], px[b + 1], py[b + 1], px[b + 2], py[b + 2]
            )
            drawTriangle(
                canvas, texture,
                face.u0, face.v0, face.u1, face.v1, face.u0, face.v1,
                px[b], py[b], px[b + 2], py[b + 2], px[b + 3], py[b + 3]
            )
        }
    }

    private fun drawTriangle(
        canvas: Canvas,
        texture: Bitmap,
        u0: Float, v0: Float, u1: Float, v1: Float, u2: Float, v2: Float,
        x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float
    ) {
        // Skip quads that are nearly edge-on and collapse to nothing
        val area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
        if (abs(area) < 1f) return

        srcPoints[0] = u0
        srcPoints[1] = v0
        srcPoints[2] = u1
        srcPoints[3] = v1
        srcPoints[4] = u2
        srcPoints[5] = v2
        dstPoints[0] = x0
        dstPoints[1] = y0
        dstPoints[2] = x1
        dstPoints[3] = y1
        dstPoints[4] = x2
        dstPoints[5] = y2
        if (!matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 3)) return

        clipPath.rewind()
        clipPath.moveTo(x0, y0)
        clipPath.lineTo(x1, y1)
        clipPath.lineTo(x2, y2)
        clipPath.close()

        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(texture, matrix, texturePaint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                lastX = event.x
                lastY = event.y
                // Pulling the model sideways spins it under the finger;
                // pulling it down tips the top of the head towards the viewer
                yaw += dx * DRAG_SPEED
                pitch = (pitch - dy * DRAG_SPEED).coerceIn(-MAX_PITCH, MAX_PITCH)
                postInvalidateOnAnimation()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
