package me.shadow.eclipselauncher.utils.skin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.utils.path.PathManager

import me.shadow.eclipselauncher.core.value.MinecraftAccount

import java.io.File
import java.io.IOException
import java.nio.file.Files

object SkinLoader {
    @JvmStatic
    @Throws(Exception::class)
    fun getAvatarDrawable(context: Context, account: MinecraftAccount, size: Int): Drawable {
        val skin = File(PathManager.DIR_USER_SKIN, account.uniqueUUID + ".png")
        if (skin.exists()) {
            try {
                Files.newInputStream(skin.toPath()).use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                        ?: throw IOException("Failed to read the skin picture and try to parse it to a bitmap")
                    return BitmapDrawable(context.resources, getAvatar(bitmap, size))
                }
            } catch (e: Exception) {
                //Local skin loading failed, log it and try loading the default "steve" avatar later
                Logging.e("SkinLoader", "Failed to load avatar from locally!", e)
            }
        }
        return getDefaultAvatar(context, size)
    }

    @Throws(Exception::class)
    private fun getDefaultAvatar(context: Context, size: Int): Drawable {
        val stream = context.assets.open("steve.png")
        val bitmap = BitmapFactory.decodeStream(stream)!!
        return BitmapDrawable(context.resources, getAvatar(bitmap, size))
    }

    @JvmStatic
    fun getAvatar(skin: Bitmap, size: Int): Bitmap {
        val faceOffset = Math.round(size / 18.0).toFloat()
        val scaleFactor = skin.width / 64.0f
        val faceSize = Math.round(8 * scaleFactor)
        val faceBitmap = Bitmap.createBitmap(skin, faceSize, faceSize, faceSize, faceSize, null, false)
        val hatBitmap = Bitmap.createBitmap(skin, Math.round(40 * scaleFactor), faceSize, faceSize, faceSize, null, false)
        val avatar = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(avatar)
        val faceScale = (size - 2 * faceOffset) / faceSize
        val hatScale = size.toFloat() / faceSize
        var matrix = Matrix()
        matrix.postScale(faceScale, faceScale)
        val newFaceBitmap = Bitmap.createBitmap(faceBitmap, 0, 0, faceSize, faceSize, matrix, false)
        matrix = Matrix()
        matrix.postScale(hatScale, hatScale)
        val newHatBitmap = Bitmap.createBitmap(hatBitmap, 0, 0, faceSize, faceSize, matrix, false)
        canvas.drawBitmap(newFaceBitmap, faceOffset, faceOffset, Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.drawBitmap(newHatBitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        return avatar
    }
}
