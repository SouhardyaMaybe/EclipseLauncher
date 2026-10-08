package me.shadow.eclipselauncher.feature.version.install

import android.app.Activity
import java.io.File

/**
 * A wrapper around InstallTask that records more detailed information
 * @see InstallTask
 */
class InstallTaskItem(
    val selectedVersion: String,
    val isMod: Boolean,
    val task: InstallTask,
    val endTask: EndTask?
) {
    override fun toString(): String {
        return "InstallTaskItem{selectedVersion='$selectedVersion', isMod='$isMod'}"
    }

    fun interface EndTask {
        /**
         * Use this task to perform the ModLoader installation
         * @param activity the current Activity, used to show the JRE selection dialog and switch to the Java GUI screen
         * @param file the file produced by the previous task after it finished
         */
        @Throws(Throwable::class)
        fun endTask(activity: Activity, file: File)
    }
}