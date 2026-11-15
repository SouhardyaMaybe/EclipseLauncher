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
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sqrt
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.data.CapeTextureLayout
import me.shadow.eclipselauncher.data.MediaStorage
import me.shadow.eclipselauncher.ui.character.loadSkin

/** Radians of yaw/pitch per pixel of drag — a full swipe turns the model. */
private const val DRAG_PER_PX = 0.011f

/** Zoom bounds: closer inspection without losing the model off-frame. */
private const val ZOOM_MIN = 0.65f
private const val ZOOM_MAX = 2.6f

/** Test hooks: the real dialog tests use these to read layout bounds. */
const val SKIN_MODEL_CANVAS_TAG = "skinModelCanvas"
const val FULLSCREEN_BUTTON_TAG = "fullscreenButton"

/** Debug-only, copyable cape diagnostic overlay. */
const val CAPE_DIAGNOSTICS_TAG = "capeDiagnostics"

/**
 * Renders the account's character as a live 3D model the user can turn and
 * zoom. A null [skinFileName] (or an unreadable one) uses the bundled
 * default Steve. Only presentation: gestures are consumed here, taps are
 * the caller's business.
 *
 * Features:
 * - Natural drag rotation (surface follows finger, 360° on both axes)
 * - Full-screen toggle button at top-right; the hosting dialog owns the
 *   full-screen state and switches to a real full-screen window
 * - Pinch to zoom
 */
@Composable
fun SkinModelView(
    skinFileName: String?,
    capeFileName: String? = null,
    media: MediaStorage,
    isFullscreen: Boolean,
    onFullscreenToggle: () -> Unit,
    modifier: Modifier = Modifier,
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

    val context = LocalContext.current
    var skin by remember(skinFileName) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(skinFileName) {
        skin = loadSkin(context, media, skinFileName)
    }

    // Cape loading mirrors the skin: a stored file is decoded off the main
    // thread, and only a usable bitmap builds the cape box. A missing or
    // invalid file therefore shows no cape geometry at all.
    var cape by remember(capeFileName) { mutableStateOf<Bitmap?>(null) }
    var capeStatus by remember(capeFileName) {
        mutableStateOf(if (capeFileName == null) "none" else "not loaded")
    }
    val capeFileExists = remember(capeFileName) {
        capeFileName != null && media.resolve(capeFileName) != null
    }
    LaunchedEffect(capeFileName) {
        if (capeFileName == null) {
            cape = null
            capeStatus = "none"
        } else {
            val decoded = loadCape(media, capeFileName)
            cape = decoded
            capeStatus = if (decoded != null) {
                "ok (${CapeTextureLayout.describe(decoded.width, decoded.height)})"
            } else if (capeFileExists) {
                "failed (not a cape or unreadable)"
            } else {
                "missing file"
            }
        }
    }

    val capeScale = cape?.let { CapeTextureLayout.scaleFor(it.width, it.height) } ?: 1
    val rig = remember(cape) {
        PlayerRig(hasCape = cape != null, capeUvScale = capeScale)
    }
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

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag(SKIN_MODEL_CANVAS_TAG)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        // Horizontal: drag right = rotate model right (yaw increases)
                        yaw += pan.x * DRAG_PER_PX
                        // Vertical: drag down = tilt model down (pitch increases)
                        pitch += pan.y * DRAG_PER_PX
                        zoom = (zoom * zoomChange).coerceIn(ZOOM_MIN, ZOOM_MAX)
                    }
                },
        ) {
            val skinBitmap = skin
            val capeBitmap = cape
            if (skinBitmap == null && capeBitmap == null) return@Canvas
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
                    // The cape quads sample the cape bitmap; every other quad
                    // keeps sampling the skin. Painter order is shared.
                    val bitmap = if (face.part == PartKind.CAPE) capeBitmap else skinBitmap
                    if (bitmap == null || face.w <= 0 || face.h <= 0) continue
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
                        val len = sqrt(dx * dx + dy * dy)
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

        // Debug-only cape diagnostic: copyable text over the model in normal
        // and full-screen dialogs. Kept until the user confirms the fix.
        if (CAPE_DEBUG_BUILD) {
            val diagnostic = remember(
                rig,
                capeFileName,
                capeFileExists,
                cape,
                capeStatus,
                yaw,
                pitch,
            ) {
                capeDiagnosticText(rig, capeFileName, capeFileExists, cape, capeStatus, yaw, pitch)
            }
            SelectionContainer(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            ) {
                Text(
                    text = diagnostic,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.testTag(CAPE_DIAGNOSTICS_TAG),
                )
            }
        }

        // Full-screen toggle at top-right. The icon is a 24dp drawable inside a
        // 48dp tonal button; no inner padding, so the glyph always has draw area.
        // In full screen the button is inset from system bars while the canvas
        // itself continues behind them.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .then(
                    if (isFullscreen) {
                        Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    } else {
                        Modifier
                    },
                ),
        ) {
            FilledTonalIconButton(
                onClick = onFullscreenToggle,
                modifier = Modifier.size(48.dp).testTag(FULLSCREEN_BUTTON_TAG),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(
                    painter = painterResource(
                        if (isFullscreen) {
                            R.drawable.ic_fullscreen_exit_24
                        } else {
                            R.drawable.ic_fullscreen_24
                        },
                    ),
                    contentDescription = stringResource(
                        if (isFullscreen) {
                            R.string.cd_exit_fullscreen
                        } else {
                            R.string.cd_enter_fullscreen
                        },
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * Builds the debug overlay text. Draw order is probed at the user's current
 * yaw/tilt with the idle choreography zeroed, so the text is stable between
 * frames; the label says so.
 */
private fun capeDiagnosticText(
    rig: PlayerRig,
    capeFileName: String?,
    fileExists: Boolean,
    cape: Bitmap?,
    capeStatus: String,
    yaw: Float,
    pitch: Float,
): String {
    val scale = cape?.let { CapeTextureLayout.scaleFor(it.width, it.height) } ?: 1
    val probe = Pose(
        yaw = yaw,
        headYaw = 0f,
        headPitch = 0f,
        armSwing = 0f,
        breath = 0f,
        bodyPitch = pitch,
    )
    val faces = rig.project(probe, 0f, 0f, 1f)
    val capeFaces = faces.filter { it.capeFace != null }
    fun name(face: RenderFace) = face.capeFace?.name?.lowercase() ?: "?"
    val order = capeFaces.joinToString(", ") { name(it) }.ifEmpty { "none" }
    val front = capeFaces.filter { it.isFrontFacing() }
        .joinToString(", ") { name(it) }
        .ifEmpty { "none" }
    val uvs = CapeFace.entries.joinToString(" | ") { face ->
        val uv = CapeGeometry.uv(face, scale)
        "${face.name.lowercase()}=${uv.u},${uv.v},${uv.w},${uv.h}"
    }
    val yawDegrees = (yaw * 180f / PI).toInt()
    val tiltDegrees = (pitch * 180f / PI).toInt()
    return buildString {
        appendLine("CAPE DIAGNOSTICS (debug)")
        appendLine("file: ${capeFileName ?: "none"}")
        appendLine("exists: $fileExists")
        appendLine("bitmap: ${cape?.let { "${it.width}x${it.height}" } ?: "not loaded"}")
        appendLine("decode: $capeStatus")
        appendLine("reached view: ${cape != null}")
        appendLine("cape faces: ${rig.capeFaceCount}")
        appendLine("uv $uvs")
        appendLine("drawn far->near @ yaw ${yawDegrees}deg tilt ${tiltDegrees}deg: $order")
        append("front-facing: $front")
    }
}
