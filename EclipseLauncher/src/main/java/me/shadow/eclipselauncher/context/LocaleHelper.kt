package me.shadow.eclipselauncher.context

import android.content.Context
import android.content.ContextWrapper
import me.shadow.eclipselauncher.setting.Settings
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.pojav.prefs.LauncherPreferences

class LocaleHelper(context: Context) : ContextWrapper(context) {
    companion object {
        fun setLocale(context: Context): ContextWrapper {
            //Initialize the paths
            PathManager.initContextConstants(context)
            //Reload the launcher settings
            Settings.refreshSettings()

            LauncherPreferences.loadPreferences()
            return LocaleHelper(context)
        }
    }
}