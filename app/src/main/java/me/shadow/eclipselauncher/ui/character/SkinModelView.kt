/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The interactive 3D character canvas used by the skin dialog. A frame
 * clock (withFrameNanos) advances the idle pose and the Canvas draw block
 * re-projects and redraws; because the clock is only read inside the draw
 * lambda, ticking animates the draw phase without recomposing anything
 * every frame. Each face of the rig is an affine-mapped texture quad
 * (clip + transformed bitmap draw), painter-ordered far to near.
 *
 * Unlike a plain portrait this view is directly manipulable: dragging
 * rotates the model (whole-body yaw plus a clamped whole-body tilt) and
 * pinching scales it, layered on top of the idle choreography so the model
 * still breathes and sways while the user handles it.
 */
package me.shadow.eclipselauncher.ui.character

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.align
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.shadow.eclipselauncher.data.MediaStorage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Radians of yaw/pitch per pixel of drag — a full swipe turns the model. */
private const val DRAG_PER_PX = 0.011f

/** Zoom bounds: closer inspection without losing the model off-frame. */
private const val ZOOM_MIN = 0.65f
private const val ZOOM_MAX = 2.6f

/** Full-screen toggle button size. */
private val FULLSCREEN_BUTTON_SIZE = 40.dp

/**
 * Renders the account's character as a live 3D model the user can turn and
 * zoom. A null [skinFileName] (or an unreadable one) uses the bundled
 * default Steve. Only presentation: gestures are consumed here, taps are
 * the caller's business.
 *
 * Features:
 * - Natural drag rotation (surface follows finger, 360° on both axes)
 * - Full-screen toggle button at top-right
 * - Pinch to zoom
 */
@Composable
fun SkinModelView(
    skinFileName: String?,
    media: MediaStorage,
    modifier: Modifier = Modifier,
    onFullscreenChange: ((Boolean) -> Unit)? = null,
) {
    // Draw-phase clock: animateFloatAsState with infiniteRepeatable works
    // with Compose test clock (test-compatible) and real clock (production).
    val timeSeconds by animateFloatAsState(
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skinModelTime",
    )

    // User handling: yaw around the model, tilt (unclamped), and a zoom factor.
    var yaw by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }

    // Full-screen state
    var isFullscreen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var skin by remember(skinFileName) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(skinFileName) {
        skin = withContext(Dispatchers.IO) {
            if (skinFileName == null) {
                defaultSkin(context)
            } else {
                media.resolve(skinFileName)?.let(::decodeSkin) ?: defaultSkin(context)
            }
        }
    }

    val rig = remember { PlayerRig() }
    // Nearest-neighbour sampling: no bilinear filtering, no dithering. Skin
    // texels stay hard-edged at every scale, like pixel blending off in 3D.
    val paint = remember {
        Paint().apply {
            isFilterBitmap = false
            isDither = false
        }
    }
    val matrix = remember { Matrix() }
    val srcPts = remember { FloatArray(8) }
    val path = remember { Path() }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    // Invert yaw: drag right = rotate model left (natural)
                    yaw -= pan.x * DRAG_PER_PX
                    // Remove pitch clamp for full 360° rotation
                    pitch -= pan.y * DRAG_PER_PX
                    zoom = (zoom * zoomChange).coerceIn(ZOOM_MIN, ZOOM_MAX)
                }
            },
    ) {
        val bitmap = skin ?: return@Canvas
        val baseScale = minOf(size.height / 46f, size.width / 30f)
        val scale = baseScale * zoom
        val cx = size.width / 2f
        val cy = size.height * 0.46f

        // The idle choreography, with the user's turn and tilt layered on.
        // Dragging up/down tilts the whole character around the hips — the
        // head keeps only its idle glances, never the user's pitch.
        val idle = Pose.at(timeSeconds)
        val pose = Pose(
            yaw = idle.yaw + yaw,
            headYaw = idle.headYaw,
            headPitch = idle.headPitch,
            armSwing = idle.armSwing,
            breath = idle.breath,
            bodyPitch = pitch,
        )
        val faces = rig.project(pose, cx, cy, scale)

        // Bounded canvas: an invisible rectangle the model lives inside, so
        // nothing can bleed past it when zoomed in — and the rectangle is
        // never itself drawn.
        val boundsLeft = cx - 12f * scale
        val boundsTop = cy - 21f * scale
        val boundsRight = cx + 12f * scale
        val boundsBottom = cy + 19f * scale

        clipRect(
            left = boundsLeft,
            top = boundsTop,
            right = boundsRight,
            bottom = boundsBottom,
        ) {
            val native = drawContext.canvas.nativeCanvas
            for (face in faces) {
                if (face.w <= 0 || face.h <= 0) continue
                srcPts[0] = face.u.toFloat()
                srcPts[1] = face.v.toFloat()
                srcPts[2] = (face.u + face.w).toFloat()
                srcPts[3] = face.v.toFloat()
                srcPts[4] = (face.u + face.w).toFloat()
                srcPts[5] = (face.v + face.h).toFloat()
                srcPts[6] = face.u.toFloat()
                srcPts[7] = (face.v + face.h).toFloat()
                matrix.reset()
                if (!matrix.setPolyToPoly(srcPts, 0, face.pts, 0, 4)) continue

                // Expand the clip a hair past each edge so neighbouring quads
                // don't leave hairline seams between them.
                path.rewind()
                val qx = (face.pts[0] + face.pts[2] + face.pts[4] + face.pts[6]) / 4f
                val qy = (face.pts[1] + face.pts[3] + face.pts[5] + face.pts[7]) / 4f
                for (corner in 0 until 4) {
                    val px = face.pts[corner * 2]
                    val py = face.pts[corner * 2 + 1]
                    val dx = px - qx
                    val dy = py - qy
                    val len = kotlin.math.sqrt(dx * dx + dy * dy)
                    val grow = if (len > 0.001f) 0.6f / len else 0f
                    val ex = px + dx * grow
                    val ey = py + dy * grow
                    if (corner == 0) path.moveTo(ex, ey) else path.lineTo(ex, ey)
                }
                path.close()

                native.save()
                native.clipPath(path)
                native.drawBitmap(bitmap, matrix, paint)
                native.restore()
            }
        }
    }

    // Full-screen toggle button at top-right
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
            .size(40.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isFullscreen = !isFullscreen
                    }
                )
            }
    ) {
        Icon(
            imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
            contentDescription = if (isFullscreen) "Exit full screen" else "Enter full screen",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp).padding(8.dp),
        )
    }

    // Notify parent of fullscreen change
    LaunchedEffect(isFullscreen) {
        onFullscreenChange?.invoke(isFullscreen)
    }
}

/** Decodes a skin file, tolerating the legacy 64x32 layout. */
internal fun decodeSkin(file: File): Bitmap? =
    runCatching {
        BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
            val legacy = bitmap.width == 64 && bitmap.height == 32
            if (!legacy && !(bitmap.width == 64 && bitmap.height == 64)) {
                bitmap.recycle()
                null
            } else {
                bitmap
            }
        }
    }.getOrNull()
