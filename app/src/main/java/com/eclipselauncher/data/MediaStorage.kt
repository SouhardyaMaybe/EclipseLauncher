/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package com.eclipselauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What kind of image is being imported, which decides validation. */
enum class ImportKind {
    /** A Minecraft skin: must be 64x64, or the legacy 64x32 layout. */
    SKIN,
    CAPE,
    INSTANCE_ICON,
}

/**
 * Owns the `media/` directory: skins, capes, instance icons and imported
 * modpack archives. File names are opaque tokens persisted in the stores.
 */
class MediaStorage(context: Context) {
    private val root = File(context.filesDir, "media").apply { mkdirs() }
    // Captured once: the constructor parameter is not visible to methods, and
    // the resolver is what the import paths need off the main thread.
    private val resolver = context.contentResolver

    fun file(name: String): File = File(root, name)

    /** The media root directory, for the content manager page. */
    fun rootDir(): File = root

    fun resolve(name: String?): File? =
        name?.let { file(it).takeIf(File::exists) }

    fun delete(name: String) {
        file(name).delete()
    }

    /** Copies an existing media file to a fresh unique name (for duplicates). */
    fun copyForNew(sourceName: String): String? {
        val source = file(sourceName)
        if (!source.exists()) return null
        val target = newId() + "_" + sourceName.substringAfterLast('_')
        return runCatching {
            source.copyTo(file(target), overwrite = true)
            target
        }.getOrNull()
    }

    /**
     * Copies the picked image into the store under [targetName].
     * Returns null when the image cannot be decoded or fails validation.
     */
    suspend fun importImage(targetName: String, uri: Uri, kind: ImportKind): String? =
        withContext(Dispatchers.IO) {
            val decoded = runCatching {
                resolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }.getOrNull() ?: return@withContext null

            val valid = when (kind) {
                ImportKind.SKIN ->
                    decoded.width == 64 && (decoded.height == 64 || decoded.height == 32)
                ImportKind.CAPE -> decoded.width in 1..512 && decoded.height in 1..512
                ImportKind.INSTANCE_ICON -> decoded.width in 1..2048 && decoded.height in 1..2048
            }
            if (!valid) {
                decoded.recycle()
                return@withContext null
            }

            val toWrite = if (kind == ImportKind.INSTANCE_ICON) {
                scaleDown(decoded, 256f)
            } else {
                decoded
            }

            runCatching {
                file(targetName).outputStream().use { out ->
                    toWrite.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                targetName
            }.onFailure { toWrite.recycle() }.getOrNull()
        }

    /** Imports a modpack archive from its content URI; returns its file name. */
    suspend fun importArchive(targetName: String, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                resolver.openInputStream(uri)?.use { input ->
                    file(targetName).outputStream().use(input::copyTo)
                    targetName
                }
            }.getOrNull()
        }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Float): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val factor = maxDimension / largest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * factor).toInt().coerceAtLeast(1),
            (bitmap.height * factor).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }
}
