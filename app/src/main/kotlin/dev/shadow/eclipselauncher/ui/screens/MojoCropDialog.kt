/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import dev.shadow.eclipselauncher.ui.components.MojoFlatButton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * `dialog_cropper.xml` and `CropperUtils.openCropperDialog`.
 *
 * The selection rectangle is fixed in the centre of the checkerboard, its
 * aspect ratio is the control's width/height, and the image can be panned or
 * pinched underneath it. H/V lock constrain panning and Reset restores the
 * reference "fit inside the selection" transform.
 */
@Composable
internal fun MojoCropDialog(
    bitmap: Bitmap,
    aspectRatio: Float,
    targetMaxSide: Int,
    onCancel: () -> Unit,
    onCropped: (Bitmap) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var horizontalLock by remember { mutableStateOf(false) }
    var verticalLock by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density
    val paddingPx = 24f * density

    val selection = if (canvasSize.width > 0 && canvasSize.height > 0) {
        fitSelectionRect(
            width = canvasSize.width.toFloat(),
            height = canvasSize.height.toFloat(),
            aspectRatio = aspectRatio,
            padding = paddingPx,
        )
    } else {
        Rect.Zero
    }
    val baseScale = if (bitmap.width > 0 && bitmap.height > 0 &&
        selection.width > 0f && selection.height > 0f
    ) {
        min(
            selection.width / bitmap.width,
            selection.height / bitmap.height,
        )
    } else {
        1f
    }
    val renderScale = baseScale * zoom
    val drawWidth = bitmap.width * renderScale
    val drawHeight = bitmap.height * renderScale
    val drawLeft = selection.center.x - drawWidth / 2f + offset.x
    val drawTop = selection.center.y - drawHeight / 2f + offset.y

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 680.dp)
                    .background(MojoPalette.StatusBar)
                    .padding(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.mojo_cropper_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
                    style = MojoType.dialogTitle,
                    color = MojoPalette.PrimaryText,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .background(MojoPalette.Background)
                        .onSizeChanged { canvasSize = it },
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(horizontalLock, verticalLock) {
                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    val oldZoom = zoom
                                    zoom = (zoom * gestureZoom).coerceIn(0.2f, 12f)
                                    if (zoom != oldZoom) {
                                        // Reference zoom is anchored to the
                                        // selection centre, which is also the
                                        // image centre at reset.
                                    }
                                    offset += Offset(
                                        if (horizontalLock) 0f else pan.x,
                                        if (verticalLock) 0f else pan.y,
                                    )
                                }
                            },
                    ) {
                        drawCropCheckerboard()
                        drawImage(
                            image = bitmap.asImageBitmap(),
                            dstOffset = IntOffset(
                                (drawLeft + offset.x * 0f).roundToInt(),
                                drawTop.roundToInt(),
                            ),
                            dstSize = IntSize(
                                drawWidth.roundToInt().coerceAtLeast(1),
                                drawHeight.roundToInt().coerceAtLeast(1),
                            ),
                        )
                        drawRect(
                            color = Color.DarkGray,
                            topLeft = Offset(selection.left, selection.top),
                            size = Size(selection.width, selection.height),
                            style = Stroke(width = 3.dp.toPx()),
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CropToggle(
                        label = stringResource(R.string.mojo_cropper_lock_horizontal),
                        checked = horizontalLock,
                        modifier = Modifier.weight(1f),
                    ) { horizontalLock = !horizontalLock }
                    CropToggle(
                        label = stringResource(R.string.mojo_cropper_lock_vertical),
                        checked = verticalLock,
                        modifier = Modifier.weight(1f),
                    ) { verticalLock = !verticalLock }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, MojoPalette.Line)
                            .clickable {
                                zoom = 1f
                                offset = Offset.Zero
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.mojo_cropper_reset),
                            style = MojoType.button,
                            color = MojoPalette.PrimaryText,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MojoFlatButton(
                        text = stringResource(R.string.mojo_cancel),
                        onClick = onCancel,
                    )
                    MojoFlatButton(
                        text = stringResource(R.string.mojo_ok),
                        onClick = {
                            performCrop(
                                bitmap = bitmap,
                                selection = selection,
                                drawLeft = drawLeft,
                                drawTop = drawTop,
                                renderScale = renderScale,
                                targetMaxSide = targetMaxSide,
                                onCropped = onCropped,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CropToggle(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(if (checked) MojoPalette.Accent else Color.Transparent)
            .border(1.dp, MojoPalette.Line)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MojoType.button,
            color = if (checked) MojoPalette.Background else MojoPalette.PrimaryText,
            textAlign = TextAlign.Center,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCropCheckerboard(
    cell: Float = 12.dp.toPx(),
) {
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = 0f
        var column = 0
        while (x < size.width) {
            drawRect(
                color = if ((row + column) % 2 == 0) Color(0xFF3A3A3A) else Color(0xFF2A2A2A),
                topLeft = Offset(x, y),
                size = Size(
                    minOf(cell, size.width - x),
                    minOf(cell, size.height - y),
                ),
            )
            x += cell
            column++
        }
        y += cell
        row++
    }
}

/**
 * Maps the fixed selection back through the preview transform and draws the
 * result at the reference's target size, preserving the selection aspect.
 */
private fun performCrop(
    bitmap: Bitmap,
    selection: Rect,
    drawLeft: Float,
    drawTop: Float,
    renderScale: Float,
    targetMaxSide: Int,
    onCropped: (Bitmap) -> Unit,
) {
    if (bitmap.width <= 0 || bitmap.height <= 0 ||
        selection.width <= 0f || selection.height <= 0f || renderScale <= 0f
    ) {
        return
    }
    val sourceLeft = (selection.left - drawLeft) / renderScale
    val sourceTop = (selection.top - drawTop) / renderScale
    val sourceWidth = selection.width / renderScale
    val sourceHeight = selection.height / renderScale
    if (sourceWidth <= 0f || sourceHeight <= 0f) return

    // `BitmapCropBehaviour.crop`: the target side only constrains the crop
    // when it is smaller than the shortest edge of the selected source rect.
    val targetMinDimension = min(sourceWidth, sourceHeight)
    val ratio = if (targetMaxSide < targetMinDimension) {
        targetMaxSide.toFloat() / targetMinDimension
    } else {
        1f
    }
    val outputWidth = (sourceWidth * ratio).roundToInt().coerceAtLeast(1)
    val outputHeight = (sourceHeight * ratio).roundToInt().coerceAtLeast(1)
    val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    canvas.drawBitmap(
        bitmap,
        // Both source/destination overloads take a `Rect` source region; the
        // float crop rect is rounded to whole source pixels.
        android.graphics.Rect(
            sourceLeft.roundToInt(),
            sourceTop.roundToInt(),
            (sourceLeft + sourceWidth).roundToInt(),
            (sourceTop + sourceHeight).roundToInt(),
        ),
        android.graphics.Rect(0, 0, outputWidth, outputHeight),
        Paint(Paint.FILTER_BITMAP_FLAG),
    )
    onCropped(output)
}
