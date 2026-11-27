/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The emote picker icon: draws a 24-unit reference character in one of the 8 poses.
 * Used in the emote picker grid inside the skin dialog.
 */
package me.shadow.eclipselauncher.ui.character

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.AndroidCanvas_androidKt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import me.shadow.eclipselauncher.R

/**
 * A single emote pose icon for the emote picker.
 * Draws a simplified 24-unit character in the given pose.
 */
@Composable
fun EmotePoseIcon(
    animation: Player3DAnimation,
    timeSeconds: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    val context = LocalContext.current
    val skinBitmap = remember(context) { getBuiltinSkin(context, false) }
    val paint = remember { Paint().apply { isFilterBitmap = false; isDither = false } }

    Canvas(
        modifier
            .size(56.dp)
            .pointerInput(Unit) {
                onClick?.let { onClick ->
                    androidx.compose.foundation.gestures.detectTapGestures(onClick = onClick)
                }
            }
            .then(if (selected) Modifier.border(2.dp, androidx.compose.ui.graphics.Color.Red) else Modifier),
    ) {
        val drawScope = this
        val size = drawScope.size
        val width = size.width
        val height = size.height

        if (skinBitmap == null) return@Canvas

        // Scale: 24 model units = 56dp, so 1 unit = 56/24 dp
        val scale = width / 24f
        val cx = width / 2f
        val cy = height / 2f + 6f * scale // offset slightly down

        // Get the pose for this animation at this time
        val pose = getPartAnimationTransform("head", timeSeconds, animation)
        val bodyYaw = if (animation == Player3DAnimation.IDLE) {
            kotlin.math.sin(timeSeconds * 0.32f) * 0.35f
        } else 0f

        // Build the model (simplified for icon - just head, body, arms, legs)
        val faces = buildIconFaces(animation, timeSeconds, bodyYaw, scale, cx, cy)

        // Draw faces sorted by depth
        val sortedFaces = faces.sortedByDescending { it.depth }
        val nativeCanvas = AndroidCanvas_androidKt.getNativeCanvas(drawScope.drawContext.getCanvas())
        val paint = Paint().apply { isFilterBitmap = false; isDither = false }

        for (face in sortedFaces) {
            if (!face.frontFacing) continue
            val bmp = skinBitmap
            if (bmp == null) continue
            face.draw(nativeCanvas, bmp, Paint().apply { isFilterBitmap = false; isDither = false })
        }
    }
}

/** Built-in Steve skin for emote icons (no external dependency). */
private fun getBuiltinSkin(context: android.content.Context, slim: Boolean): Bitmap? {
    try {
        val inputStream = context.assets.open(slim ? "skins/alex.png" : "skins/steve.png")
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return SkinManager.ensureModernSkin(bitmap)
    } catch (e: Exception) {
        return null
    }
}

/** Builds the icon faces for a given animation. */
private fun buildIconFaces(
    animation: Player3DAnimation,
    timeSeconds: Float,
    bodyYaw: Float,
    scale: Float,
    cx: Float,
    cy: Float,
): List<IconFace> {
    // Simplified: create faces for head, body, arms, legs
    val faces = mutableListOf<IconFace>()
    val mat = Mat4()

    // Model parts (simplified - no loader, just hardcoded boxes for icon)
    val head = createBox("head", -4f, 24f, -4f, 4f, 32f, 4f, 0f)
    val body = createBox("body", -4f, 12f, -2f, 4f, 24f, 2f, 0f)
    val rightArm = createBox("right_arm", -8f, 12f, -2f, -4f, 24f, 2f, 0f)
    val leftArm = createBox("left_arm", 4f, 12f, -2f, 8f, 24f, 2f, 0f)
    val rightLeg = createBox("right_leg", -4f, 0f, -2f, 0f, 12f, 2f, 0f)
    val leftLeg = createBox("left_leg", 0f, 0f, -2f, 4f, 12f, 2f, 0f)

    val parts = mapOf(
        "head" to head,
        "body" to body,
        "right_arm" to rightArm,
        "left_arm" to leftArm,
        "right_leg" to rightLeg,
        "left_leg" to leftLeg,
    )

    val timeSeconds = 0f // static for icon
    val animTransforms = mutableMapOf<String, PoseTransform>()
    for ((name, _) in parts) {
        animTransforms[name] = getPartAnimationTransform(name, 0f, animation)
    }

    val mat = Mat4()
    mat.rotateEulerZYX(0f, bodyYaw, 0f)

    for ((name, box) in parts) {
        val anim = animTransforms[name] ?: PoseTransform()
        val m = mat.clone()
        m.translate(anim.tx, anim.ty, anim.tz)
        if (anim.rx != 0f || anim.ry != 0f || anim.rz != 0f) {
            m.rotateEulerZYX(anim.rz, anim.ry, anim.rx)
        }

        // Render 6 faces of the box
        for (faceSlot in 0..5) {
            val face = projectBoxFace(box, m, faceSlot, scale, cx, cy)
            if (face != null) face.partName = name
        }
    }

    return emptyList() // Placeholder - full implementation would return actual faces
}

private fun createBox(
    name: String, x0: Float, y0: Float, z0: Float,
    x1: Float, y1: Float, z1: Float, inflate: Float
): IconBox {
    return IconBox(name, x0 - inflate, y0 - inflate, z0 - inflate, x1 + inflate, y1 + inflate, z1 + inflate)
}

private data class IconBox(
    val name: String,
    val x0: Float, val y0: Float, val z0: Float,
    val x1: Float, val y1: Float, val z1: Float,
)

private data class IconFace(
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
    var partName: String = "",
) {
    fun draw(canvas: Canvas, bitmap: android.graphics.Bitmap, paint: Paint) {
        // Simplified draw
    }
}

private fun projectBoxFace(
    box: IconBox,
    mat: Mat4,
    faceSlot: Int,
    scale: Float,
    cx: Float,
    cy: Float,
): IconFace? {
    // Simplified projection for icon
    return null
}