package me.shadow.eclipselauncher.core

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat

import me.shadow.eclipselauncher.InfoDistributor
import me.shadow.eclipselauncher.context.ContextExecutor
import me.shadow.eclipselauncher.context.LocaleHelper
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.ui.activity.ErrorActivity
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.EclipseTools

import me.shadow.eclipselauncher.core.utils.FileUtils

import java.io.File
import java.io.PrintStream
import java.text.DateFormat
import java.util.Date
import java.util.Objects

class EclipseApplication : Application() {

    override fun onCreate() {
        ContextExecutor.setApplication(this)

        // Two fixed themes only: the light and dark Eclipse violet palettes are
        // baked into the DayNight resource sets, wallpaper colors are never adopted.

        // The uncaught-exception hook is the JVM's crash-reporting entry point and has no
        // coroutine equivalent, so it stays a plain Thread handler, exactly like before.
        Thread.setDefaultUncaughtExceptionHandler { thread, th ->
            val storagePermAllowed = (Build.VERSION.SDK_INT >= 29 || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) && Tools.checkStorageRoot()
            val crashFile = File(if (storagePermAllowed) PathManager.DIR_LAUNCHER_LOG else PathManager.DIR_DATA, "latestcrash.txt")
            try {
                // Write to file, since some devices may not able to show error
                FileUtils.ensureParentDirectory(crashFile)
                val crashStream = PrintStream(crashFile)
                crashStream.append(InfoDistributor.APP_NAME + " crash report\n")
                crashStream.append(" - Time: ").append(DateFormat.getDateTimeInstance().format(Date())).append("\n")
                crashStream.append(" - Device: ").append(Build.PRODUCT).append(" ").append(Build.MODEL).append("\n")
                crashStream.append(" - Android version: ").append(Build.VERSION.RELEASE).append("\n")
                crashStream.append(" - Launcher version: ").append(EclipseTools.getVersionName()).append(" (").append(EclipseTools.getVersionCode().toString()).append(")").append("\n")
                crashStream.append(" - Crash stack trace:\n")
                crashStream.append(Log.getStackTraceString(th))
                crashStream.close()
            } catch (throwable: Throwable) {
                Logging.e(CRASH_REPORT_TAG, " - Exception attempt saving crash stack trace:", throwable)
                Logging.e(CRASH_REPORT_TAG, " - The crash stack trace was:", th)
            }

            ErrorActivity.showLauncherCrash(this, crashFile.absolutePath, th)
            EclipseTools.killProcess()
        }

        try {
            super.onCreate()
            PathManager.DIR_DATA = getDir("files", Context.MODE_PRIVATE).parent
            PathManager.DIR_CACHE = cacheDir
            PathManager.DIR_ACCOUNT_NEW = PathManager.DIR_DATA + "/accounts"
            Tools.DEVICE_ARCHITECTURE = Architecture.getDeviceArchitecture()
            //Force x86 lib directory for Asus x86 based zenfones
            if (Architecture.isx86Device() && Architecture.is32BitsDevice()) {
                val originalJNIDirectory = applicationInfo.nativeLibraryDir!!
                applicationInfo.nativeLibraryDir = originalJNIDirectory.substring(0,
                    originalJNIDirectory.lastIndexOf("/")) + "/x86"
            }
        } catch (throwable: Throwable) {
            val ferrorIntent = Intent(this, ErrorActivity::class.java)
            ferrorIntent.putExtra("throwable", throwable)
            ferrorIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(ferrorIntent)
        }

        //Set the theme
        val launcherTheme = AllSettings.launcherTheme.getValue()
        if (!Objects.equals(launcherTheme, "system")) {
            when (launcherTheme) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        ContextExecutor.clearApplication()
    }

    override fun attachBaseContext(base: Context) {
        ContextExecutor.setApplication(this)
        super.attachBaseContext(LocaleHelper.Companion.setLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ContextExecutor.setApplication(this)
        LocaleHelper.Companion.setLocale(this)
    }

    companion object {
        const val CRASH_REPORT_TAG = "EclipseCrashReport"
    }
}
