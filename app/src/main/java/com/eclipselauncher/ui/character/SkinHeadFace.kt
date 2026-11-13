/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * HOME's static skin head face: the front of the account's head rendered
 * as a still image — base face (the 8x8 pixel region at 8,8 in the standard
 * skin layout) with the hat/overlay layer (the matching 8x8 region at 40,8)
 * drawn on top, composed the way Zalith 2 composes its head avatars: the
 * base sits inset by ~1/18 of the tile (an 8/9-scale face) and the hat
 * layer is drawn full-bleed over it, so the overlay sits proud of the base
 * by the same 8:9 ratio the 3D model's inflated hat box uses. Skins with a
 * hat, hair or glasses therefore read with real thickness instead of a
 * flat crop. Nearest-neighbour, no rig, no pose, no frame clock. Only the
 * face square is painted; the nametag plate and the empty-account state
 * live in HomeScreen, which also owns the button behaviour around this
 * face.
 */
package com.eclipselauncher.ui.character

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.eclipselauncher.data.MediaStorage

/**
 * Renders the skin's head front face — base plus overlay hat layer — as a
 * static, hard-pixelated square. A null [skinFileName] (or an unreadable
 * one) uses the bundled default Steve. Purely presentational: taps are
 * handled by the caller.
 */
@Composable
fun SkinHeadFace(
    skinFileName: String?,
    media: MediaStorage,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap: Bitmap? by produceState<Bitmap?>(initialValue = null, key1 = skinFileName) {
        value = withContext(Dispatchers.IO) {
            if (skinFileName == null) {
                defaultSkin(context)
            } else {
                media.resolve(skinFileName)?.let(::decodeSkin) ?: defaultSkin(context)
            }
        }
    }
    val image = remember(bitmap) { bitmap?.asImageBitmap() }

    Canvas(modifier = modifier) {
        val skin = image ?: return@Canvas

        // Square face, centred in the canvas.
        val face = minOf(size.width, size.height)
        val left = (size.width - face) / 2f
        val top = (size.height - face) / 2f
        val dst = IntSize(face.roundToInt(), face.roundToInt())
        val at = IntOffset(left.roundToInt(), top.roundToInt())

        // Zalith 2's head-avatar composition: the base face is drawn inset
        // by 1/18 of the tile on every side (an 8/9-scale face)...
        val inset = (face / 18f).roundToInt()
        drawImage(
            image = skin,
            srcOffset = IntOffset(8, 8),
            srcSize = IntSize(8, 8),
            dstOffset = IntOffset(at.x + inset, at.y + inset),
            dstSize = IntSize(face.roundToInt() - 2 * inset, face.roundToInt() - 2 * inset),
            filterQuality = FilterQuality.None,
        )
        // ...and the hat/overlay layer (the 8x8 region at (40,8)) is drawn
        // full-bleed over it, so the overlay sits proud of the base by
        // exactly the 8:9 ratio the 3D model's inflated hat box uses.
        // Skins without a hat have fully transparent pixels here, so this
        // is a no-op for them.
        drawImage(
            image = skin,
            srcOffset = IntOffset(40, 8),
            srcSize = IntSize(8, 8),
            dstOffset = at,
            dstSize = dst,
            filterQuality = FilterQuality.None,
        )
    }
}
