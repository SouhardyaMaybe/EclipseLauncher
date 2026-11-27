/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The Compose Canvas 2D renderer for the 3D player model.
 * Uses a custom Mat4, painter's algorithm, orthographic projection, and procedural animations.
 * No OpenGL — all CPU-side vertex transform, depth sort, per-face lighting, and ground shadow.
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.PlayerModel3DKt
 */
package me.shadow.eclipselauncher.ui.character

import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Parts whose rotation pivots at the shoulder rather than the part origin. */
private val ARM_PARTS = setOf("right_arm", "left_arm", "right_sleeve", "left_sleeve")

/** Model is 32 units tall; the reference frames it at 72% of the draw node. */
private const val MODEL_HEIGHT_UNITS = 32f
private const val MODEL_FRAME_FRACTION = 0.72f

/** Reference perspective divisor and clamp. */
private const val PERSPECTIVE_DISTANCE = 80f
private const val PERSPECTIVE_MIN = 0.5f
private const val PERSPECTIVE_MAX = 2.0f

/** Reference lighting constants. */
private const val SUN_TO_X = -0.62f
private const val SUN_TO_Y = -0.38f
private const val SUN_TO_Z = 0.69f
private const val SHADOW_DIR_X = 0.45f
private const val LIGHT_R = 1.2f
private const val LIGHT_G = 0.75f
private const val LIGHT_B = 0.38f
private const val SKY_G = 0.58f
private const val SKY_B = 0.92f
private const val AMBIENT_R = 0.35f
private const val AMBIENT_G = 0.28f
private const val AMBIENT_B = 0.2f

/**
 * The main 3D player model Composable.
 * Draws the animated skin onto a Compose Canvas using a custom CPU renderer.
 * @param bitmap The skin bitmap (64x64 or 64x32, auto-expanded).
 * @param modifier Compose modifier for layout.
 * @param slim True for Alex (3px arms), false for Steve (4px arms).
 * @param animation Current emote animation clip.
 * @param lookTarget Point in model space for the head to look at.
 * @param capeBitmap Optional cape bitmap.
 * @param onDraw Callback invoked after the model is drawn (for nametag anchor).
 * @param playerMotion Shared pose state (bodyYaw, headYaw, headPitch, armSwing, breath).
 * @param dragZoom User pinch zoom factor.
 */
@Composable
fun Minecraft3DPlayer(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    slim: Boolean = false,
    animation: Player3DAnimation = Player3DAnimation.IDLE,
    lookTarget: PlayerLookTarget,
    capeBitmap: Bitmap? = null,
    onDraw: () -> Unit = {},
    playerMotion: PlayerMotion,
    dragZoom: Float = 1f,
) {
    // Animation time state (drives idle choreography and emote motion)
    var animTime by mutableFloatStateOf(0f)

    // Load model once (cached by loader) - in Composable context
    val context = LocalContext.current
    val model = remember(slim) {
        Minecraft3DModelLoader.getPlayerModel(context, slim)
    }

    // The actual drawing happens in a Canvas
    Canvas(
        modifier
            .fillMaxSize()
            .testTag(SKIN_MODEL_CANVAS_TAG),
    ) {
        val width = size.width
        val height = size.height

        if (width <= 0f || height <= 0f) return@Canvas

        // Animation time: advance by ~1/60s per frame
        val timeSeconds = animTime
        animTime = (timeSeconds + (1f / 60f)) % 10000f

        // Advance idle choreography (body sway, head glances, arm swing, breath)
        playerMotion.updateIdle(timeSeconds)

        // Reference framing: 72% of the node height, centered on the node.
        val scale = (height * MODEL_FRAME_FRACTION / MODEL_HEIGHT_UNITS) * dragZoom
        val cx = width / 2f
        val cy = height / 2f

        // Project all parts and draw.
        // PlayerMotion holds the complete pose (idle + drag + look target via gesture handler).
        // Renderer uses it directly — no additional blending here.
        val faces = projectModel(
            root = model,
            timeSeconds = timeSeconds,
            animation = animation,
            cx = cx,
            cy = cy,
            scale = scale,
            bodyYaw = playerMotion.bodyYaw,
            headYaw = playerMotion.headYaw,
            headPitch = playerMotion.headPitch,
        )
        drawFaces(drawContext.canvas.nativeCanvas, faces, bitmap, capeBitmap)

        // Call onDraw callback for nametag anchor
        onDraw()
    }
}

/**
 * Projects the entire model hierarchy into screen space.
 * [cx]/[cy] are the screen centre and [scale] converts model units to pixels.
 * [bodyYaw] is the whole-body yaw rotation (applied at root).
 * [headYaw]/[headPitch] are the head's look direction (added to head's animation transform).
 */
fun projectModel(
    root: ModelPart3D,
    timeSeconds: Float,
    animation: Player3DAnimation,
    cx: Float,
    cy: Float,
    scale: Float,
    bodyYaw: Float,
    headYaw: Float,
    headPitch: Float,
): List<ProjectedQuad> {
    val faces = mutableListOf<ProjectedQuad>()
    val mat = Mat4()

    // Overall model rotation: body yaw around Y (applied at root level).
    // Reference calls rotateEulerZYX(rz, ry, rx) = (0, bodyYaw, 0).
    mat.rotateEulerZYX(0f, bodyYaw, 0f)

    renderPartRecursive(root, mat, timeSeconds, animation, scale, cx, cy, faces, headYaw, headPitch)

    // Sort by depth (far to near) for painter's algorithm
    return faces.sortedByDescending { it.depth }
}

private fun renderPartRecursive(
    part: ModelPart3D,
    parentMat: Mat4,
    timeSeconds: Float,
    animation: Player3DAnimation,
    scale: Float,
    cx: Float,
    cy: Float,
    faces: MutableList<ProjectedQuad>,
    headYaw: Float,
    headPitch: Float,
) {
    val mat = parentMat.clone()

    // Get animation transform for this part
    val animTransform = getPartAnimationTransform(part.name, timeSeconds, animation)

    // Translate to pivot + animation translation (reference order: translate then rotate).
    // The reference applies animation translation (tx,ty,tz) ALONG WITH pivot, BEFORE rotation.
    // Root gets additional -8 Y offset to center the 32-unit model.
    val rootOffsetY = if (part.name == "root") -8f else 0f
    mat.translate(
        part.pivotX + animTransform.tx,
        part.pivotY + animTransform.ty + rootOffsetY,
        part.pivotZ + animTransform.tz,
    )

    // Head look is added directly to the head's Euler angles (matches reference exactly).
    // For head: add headYaw to ry, headPitch to rx.
    // For other parts: use animation transform only.
    val isHead = part.name == "head"
    val rx = animTransform.rx + if (isHead) headPitch else 0f
    val ry = animTransform.ry + if (isHead) headYaw else 0f
    val rz = animTransform.rz

    // Apply rotation. IMPORTANT: Reference calls rotateEulerZYX(rz, ry, rx) parameter order.
    if (rx != 0f || ry != 0f || rz != 0f) {
        if (part.name in ARM_PARTS) {
            // Move the arm rotation centre up to the shoulder (y=-2 from pivot).
            mat.translate(0f, -2f, 0f)
            mat.rotateEulerZYX(rz, ry, rx)
            mat.translate(0f, 2f, 0f)
        } else {
            mat.rotateEulerZYX(rz, ry, rx)
        }
    }

    // Render this part's quads
    for (quad in part.quads) {
        val face = projectQuad(quad, mat, scale, cx, cy)
        if (face.frontFacing) {
            faces.add(face)
        }
    }

    // Recurse into children
    for (child in part.children) {
        renderPartRecursive(child, mat, timeSeconds, animation, scale, cx, cy, faces, headYaw, headPitch)
    }
}

/** Projects a single quad into screen space, using the reference's perspective and winding rules. */
private fun projectQuad(quad: Quad3D, mat: Mat4, scale: Float, cx: Float, cy: Float): ProjectedQuad {
    val scratch = FloatArray(3)
    val vertices = quad.vertices()
    val world = Array(4) { FloatArray(3) }
    val pts = FloatArray(8)
    var depth = 0f

    for (i in 0 until 4) {
        val v = vertices[i]
        mat.transformPoint(v.x, v.y, v.z, scratch)
        world[i][0] = scratch[0]
        world[i][1] = scratch[1]
        world[i][2] = scratch[2]

        // Reference perspective: nearer vertices scale up, far vertices scale down.
        val perspective = (PERSPECTIVE_DISTANCE / (scratch[2] + PERSPECTIVE_DISTANCE))
            .coerceIn(PERSPECTIVE_MIN, PERSPECTIVE_MAX)
        pts[i * 2] = scratch[0] * scale * perspective + cx
        pts[i * 2 + 1] = scratch[1] * scale * perspective + cy
        depth += scratch[2]
    }
    depth /= 4f

    // Overlay layers are nudged toward the camera to avoid z-fighting.
    if (quad.overlay) depth -= 0.08f

    // Screen-space winding test (y-down). Matches the reference exactly.
    val x0 = pts[0]; val y0 = pts[1]
    val x1 = pts[2]; val y1 = pts[3]
    val x2 = pts[4]; val y2 = pts[5]
    val x3 = pts[6]; val y3 = pts[7]
    val frontFacing = ((y2 - y0) * (x1 - x0) - (x2 - x0) * (y1 - y0)) < 0f

    // UVs in 0..1 range
    val u0 = quad.v0.u / 64f; val v0 = quad.v0.v / 64f
    val u1 = quad.v1.u / 64f; val v1 = quad.v1.v / 64f
    val u2 = quad.v2.u / 64f; val v2 = quad.v2.v / 64f
    val u3 = quad.v3.u / 64f; val v3 = quad.v3.v / 64f

    return ProjectedQuad(
        x0, y0, x1, y1, x2, y2, x3, y3,
        u0, v0, u1, v1, u2, v2, u3, v3,
        depth, frontFacing, quad.overlay, computeLighting(world),
    )
}

/** Computes per-face lighting from the transformed 3D normal, matching the reference constants. */
private fun computeLighting(world: Array<FloatArray>): Int {
    val ax = world[1][0] - world[0][0]
    val ay = world[1][1] - world[0][1]
    val az = world[1][2] - world[0][2]
    val bx = world[2][0] - world[0][0]
    val by = world[2][1] - world[0][1]
    val bz = world[2][2] - world[0][2]

    var nx = ay * bz - az * by
    var ny = az * bx - ax * bz
    var nz = ax * by - ay * bx
    val len = sqrt(nx * nx + ny * ny + nz * nz)
    if (len > 1.0e-4f) {
        nx /= len
        ny /= len
        nz /= len
    }

    val sun = max(0f, SUN_TO_X * nx + SUN_TO_Y * ny + SUN_TO_Z * nz)
    val sky = max(0f, nx * 0.18f - ny * 0.55f - nz * 0.82f) * SHADOW_DIR_X + 0.25f
    val r = (LIGHT_R * sun * 0.8f + 0.62f * sky + AMBIENT_R).coerceIn(0f, 1f)
    val g = (LIGHT_G * sun * 0.8f + SKY_G * sky + AMBIENT_G).coerceIn(0f, 1f)
    val b = (LIGHT_B * sun * 0.8f + SKY_B * sky + AMBIENT_B).coerceIn(0f, 1f)
    return (0xFF shl 24) or
        ((r * 255f).toInt() shl 16) or
        ((g * 255f).toInt() shl 8) or
        (b * 255f).toInt()
}

/** Draws all faces to the native canvas. */
private fun drawFaces(
    canvas: NativeCanvas,
    faces: List<ProjectedQuad>,
    skinBitmap: Bitmap,
    capeBitmap: Bitmap?,
) {
    val paint = Paint().apply {
        isFilterBitmap = false
        isDither = false
        isAntiAlias = false
    }
    val filters = HashMap<Int, PorterDuffColorFilter>()

    for (face in faces) {
        val bmp = if (face.isCape()) capeBitmap else skinBitmap
        if (bmp == null) continue
        paint.colorFilter = filters.getOrPut(face.lightColor) {
            PorterDuffColorFilter(face.lightColor, PorterDuff.Mode.MULTIPLY)
        }
        face.draw(canvas, bmp, paint)
    }
}