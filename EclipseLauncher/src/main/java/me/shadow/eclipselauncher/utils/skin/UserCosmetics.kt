package me.shadow.eclipselauncher.utils.skin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import me.shadow.eclipselauncher.pojav.value.MinecraftAccount
import me.shadow.eclipselauncher.utils.path.PathManager
import java.io.File

/**
 * Stores the custom skins and capes picked for an account next to the avatars
 * the launcher already downloads: `<uuid>.png` holds the skin and
 * `<uuid>_cape.png` holds the cape, both inside [PathManager.DIR_USER_SKIN].
 */
object UserCosmetics {
    fun skinFile(account: MinecraftAccount): File =
        File(PathManager.DIR_USER_SKIN, account.uniqueUUID + ".png")

    fun capeFile(account: MinecraftAccount): File =
        File(PathManager.DIR_USER_SKIN, account.uniqueUUID + "_cape.png")

    /**
     * Minecraft textures are multiples of 8 pixels wide and either square
     * (modern 64x64 skins) or twice as wide as they are tall (64x32 skins and capes)
     */
    fun isValidTexture(width: Int, height: Int): Boolean =
        width >= 32 && width % 8 == 0 && (width == height || width == 2 * height)

    /**
     * Decode the image behind [uri] as a skin or cape texture.
     * Returns null when the image cannot be read or its dimensions are not a
     * Minecraft texture layout. The bounds are checked first so huge images are
     * never decoded into memory just to be rejected.
     */
    fun read(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        }.getOrElse { return null }
        if (!isValidTexture(bounds.outWidth, bounds.outHeight)) return null

        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull() ?: return null
        if (!isValidTexture(bitmap.width, bitmap.height)) {
            bitmap.recycle()
            return null
        }
        // Decoded images must never be rescaled by canvas density handling
        bitmap.density = Bitmap.DENSITY_NONE
        return bitmap
    }

    /** Persist [bitmap] as a PNG at [file], creating parent directories as needed */
    fun write(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Read a stored texture back, or null when it is missing or corrupted */
    fun load(file: File): Bitmap? {
        if (!file.exists()) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)?.also { it.density = Bitmap.DENSITY_NONE }
        }.getOrNull()
    }
}
