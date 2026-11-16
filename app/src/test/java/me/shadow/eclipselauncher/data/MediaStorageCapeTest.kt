/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric tests for cape imports: the picker path and the URL path must
 * accept vanilla/HD/legacy cape layouts and reject anything else.
 */
package me.shadow.eclipselauncher.data

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MediaStorageCapeTest {

    private val context = RuntimeEnvironment.getApplication()
    private val media = MediaStorage(context)
    private var uriCounter = 0

    private fun png(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(30, 120, 210))
        val out = ByteArrayOutputStream()
        assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun contentUri(bytes: ByteArray): Uri {
        val uri = Uri.parse("content://me.shadow.eclipselauncher.test/cape-${uriCounter++}.png")
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(bytes))
        return uri
    }

    @Test
    fun pickerAcceptsVanillaCape() = runBlocking {
        val name = media.importImage("vanilla.png", contentUri(png(64, 32)), ImportKind.CAPE)
        assertEquals("vanilla.png", name)
        assertTrue(media.file("vanilla.png").exists())
    }

    @Test
    fun pickerAcceptsHdCape() = runBlocking {
        val name = media.importImage("hd.png", contentUri(png(128, 64)), ImportKind.CAPE)
        assertEquals("hd.png", name)
    }

    @Test
    fun pickerAcceptsLegacyCape() = runBlocking {
        val name = media.importImage("legacy.png", contentUri(png(22, 17)), ImportKind.CAPE)
        assertEquals("legacy.png", name)
    }

    @Test
    fun pickerRejectsSkinSizedImage() = runBlocking {
        assertNull(media.importImage("not-cape.png", contentUri(png(64, 64)), ImportKind.CAPE))
        assertFalse(media.file("not-cape.png").exists())
    }

    @Test
    fun pickerRejectsImageWithCapeLikeSizeButWrongLayout() = runBlocking {
        assertNull(media.importImage("square.png", contentUri(png(300, 300)), ImportKind.CAPE))
        assertFalse(media.file("square.png").exists())
    }

    @Test
    fun urlImportStoresFetchedCape() = runBlocking {
        val bytes = png(64, 32)
        val name = media.importImageFromUrl(
            "https://textures.minecraft.net/texture/abc123",
            ImportKind.CAPE,
        ) { bytes }
        assertNotNull(name)
        assertTrue(name!!.startsWith("cape_"))
        assertTrue(media.file(name!!).exists())
    }

    @Test
    fun urlImportFailsWhenFetchFails() = runBlocking {
        val name = media.importImageFromUrl(
            "https://textures.minecraft.net/texture/missing",
            ImportKind.CAPE,
        ) { null }
        assertNull(name)
    }

    @Test
    fun urlImportRejectsNonCapePayload() = runBlocking {
        val bytes = png(64, 64)
        val name = media.importImageFromUrl(
            "https://textures.minecraft.net/texture/not-a-cape",
            ImportKind.CAPE,
        ) { bytes }
        assertNull(name)
    }
}
