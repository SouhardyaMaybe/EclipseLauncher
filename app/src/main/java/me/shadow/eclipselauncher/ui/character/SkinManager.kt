/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Skin utilities ported from the reference Obsidian implementation.
 */
package me.shadow.eclipselauncher.ui.character

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint

/** Skin utilities. */
object SkinManager {
    /** Expands a legacy 64x32 skin to 64x64 by mirroring the arm/leg textures. */
    fun ensureModernSkin(bitmap: Bitmap): Bitmap {
        var result: Bitmap
        if (bitmap.width == 64 && bitmap.height == 32) {
            result = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint().apply {
                isFilterBitmap = false
                isAntiAlias = false
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            // Mirror right arm (40,16,16,16) -> (32,48)
            mirrorSkinRect(bitmap, result, 40, 16, 16, 16, 32, 48)
            // Mirror left arm (0,16,16,16) -> (16,48)
            mirrorSkinRect(bitmap, result, 0, 16, 16, 16, 16, 48)
        } else {
            result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        // Fix transparent pixels on slim arms (Alex)
        if (result.width == 64 && result.height == 64 && (result.getPixel(55, 20) shr 24) == 0) {
            for (i in 16 until 32) {
                val pixel = result.getPixel(53, i)
                if ((result.getPixel(54, i) shr 24) == 0) {
                    result.setPixel(54, i, pixel)
                }
                if ((result.getPixel(55, i) shr 24) == 0) {
                    result.setPixel(55, i, pixel)
                }
            }
            for (i in 48 until 64) {
                val pixel = result.getPixel(45, i)
                if ((result.getPixel(46, i) shr 24) == 0) {
                    result.setPixel(46, i, pixel)
                }
                if ((result.getPixel(47, i) shr 24) == 0) {
                    result.setPixel(47, i, pixel)
                }
            }
        }
        return result
    }

    private fun mirrorSkinRect(
        src: Bitmap,
        dst: Bitmap,
        sx: Int, sy: Int, sw: Int, sh: Int,
        dx: Int, dy: Int,
    ) {
        try {
            val cropped = Bitmap.createBitmap(src, sx, sy, sw, sh)
            val matrix = Matrix().apply { preScale(-1f, 1f) }
            val mirrored = Bitmap.createBitmap(cropped, 0, 0, sw, sh, matrix, false)
            val canvas = Canvas(dst)
            val paint = Paint().apply {
                isFilterBitmap = false
                isAntiAlias = false
            }
            canvas.drawBitmap(mirrored, dx.toFloat(), dy.toFloat(), paint)
            cropped.recycle()
            mirrored.recycle()
        } catch (e: Exception) {
            // Ignore
        }
    }
}