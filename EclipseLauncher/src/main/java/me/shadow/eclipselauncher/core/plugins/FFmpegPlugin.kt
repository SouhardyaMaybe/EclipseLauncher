package me.shadow.eclipselauncher.core.plugins

import android.content.Context
import android.content.pm.PackageManager

import me.shadow.eclipselauncher.feature.log.Logging

import java.io.File

object FFmpegPlugin {
    @JvmField
    var isAvailable = false

    @JvmField
    var libraryPath: String? = null

    @JvmField
    var executablePath: String? = null

    @JvmStatic
    fun discover(context: Context) {
        val manager = context.packageManager
        try {
            val ffmpegPluginInfo = manager.getPackageInfo(
                "me.shadow.eclipse.ffmpeg",
                PackageManager.GET_SHARED_LIBRARY_FILES
            )
            libraryPath = ffmpegPluginInfo.applicationInfo!!.nativeLibraryDir
            val ffmpegExecutable = File(libraryPath!!, "libffmpeg.so")
            executablePath = ffmpegExecutable.absolutePath
            // Older plugin versions still have the old executable location
            isAvailable = ffmpegExecutable.exists()
        } catch (e: Exception) {
            Logging.i("FFmpegPlugin", "Failed to discover plugin", e)
        }
    }
}
