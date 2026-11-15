/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Shared skin utilities used by SkinHeadFace and SkinModelView.
 */
package me.shadow.eclipselauncher.ui.character

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shadow.eclipselauncher.data.CapeTextureLayout
import me.shadow.eclipselauncher.data.MediaStorage

/**
 * Decodes a skin file, tolerating the legacy 64x32 layout.
 * Returns null if the file cannot be decoded or has invalid dimensions.
 */
fun decodeSkin(file: File): Bitmap? =
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

/**
 * Resolves a skin file name through MediaStorage, decodes it, and falls back
 * to the default Steve skin on failure.
 */
suspend fun loadSkin(
    context: Context,
    media: MediaStorage,
    skinFileName: String?,
): Bitmap = withContext(Dispatchers.IO) {
    if (skinFileName == null) {
        defaultSkin(context)
    } else {
        media.resolve(skinFileName)?.let(::decodeSkin) ?: defaultSkin(context)
    }
}

/**
 * Decodes a cape file, accepting the vanilla 64x32 layout, integer HD
 * multiples of it, and the legacy 22x17 file. Returns null if the file
 * cannot be decoded or its dimensions are not a cape.
 */
fun decodeCape(file: File): Bitmap? =
    runCatching {
        BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
            if (CapeTextureLayout.scaleFor(bitmap.width, bitmap.height) == null) {
                bitmap.recycle()
                null
            } else {
                bitmap
            }
        }
    }.getOrNull()

/**
 * Resolves a cape file name through MediaStorage and decodes it. Null means
 * the account has no usable cape: no file, decode failure, or wrong size.
 */
suspend fun loadCape(media: MediaStorage, capeFileName: String?): Bitmap? =
    withContext(Dispatchers.IO) {
        capeFileName?.let { name -> media.resolve(name)?.let(::decodeCape) }
    }