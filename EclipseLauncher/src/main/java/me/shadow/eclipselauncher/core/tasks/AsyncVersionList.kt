package me.shadow.eclipselauncher.core.tasks

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.shadow.eclipselauncher.core.JMinecraftVersionList
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.utils.DownloadUtils
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.json.JsonIOException
import me.shadow.eclipselauncher.json.JsonSyntaxException
import me.shadow.eclipselauncher.utils.EclipseTools
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.path.UrlManager
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException

/** Class getting the version list, and that's all really */
class AsyncVersionList {
    // Runs where the old Task.runTask did: on a background pool shared by every caller
    private val coroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("AsyncVersionList"))

    /**
     * Load the Minecraft version list, refreshing the cached copy when it is older than 24 hours.
     * The callback is invoked from a background thread, like before.
     *
     * @param listener one-shot callback receiving the parsed list (or null on failure)
     * @param secondPass internal flag marking the retry after a corrupt cached file
     */
    fun getVersionList(listener: VersionDoneListener?, secondPass: Boolean) {
        coroutineScope.launch {
            // Any unexpected throwable aborts this attempt without invoking the listener,
            // exactly like the failed Task body did before the coroutine rewrite.
            runCatching { loadVersionList(listener, secondPass) }
        }
    }

    private fun loadVersionList(listener: VersionDoneListener?, secondPass: Boolean) {
        val versionFile = File(PathManager.FILE_VERSION_LIST)
        var versionList: JMinecraftVersionList? = null
        try {
            if (!versionFile.exists() || EclipseTools.getCurrentTimeMillis() > versionFile.lastModified() + 86400000) {
                versionList = downloadVersionList(UrlManager.URL_MINECRAFT_VERSION_REPOS)
            }
        } catch (e: Exception) {
            Logging.e("AsyncVersionList", "Refreshing version list failed :$e")
            Logging.e("GetVersionList", Tools.printToString(e))
        }

        // Fallback when no network or not needed
        if (versionList == null) {
            try {
                versionList = Tools.JSON_CODEC.fromJson(FileReader(versionFile), JMinecraftVersionList::class.java)
            } catch (e: FileNotFoundException) {
                Logging.e("File Not Found", Tools.printToString(e))
            } catch (e: JsonIOException) {
                handleBrokenVersionListCache(e, versionFile, listener, secondPass)
            } catch (e: JsonSyntaxException) {
                handleBrokenVersionListCache(e, versionFile, listener, secondPass)
            }
        }

        listener?.onVersionDone(versionList)
    }

    /**
     * A corrupt cached version list is deleted and (on the first pass) the whole load is retried
     * in a fresh coroutine - like before, the current pass still reports its null result first.
     */
    private fun handleBrokenVersionListCache(e: Exception, versionFile: File, listener: VersionDoneListener?, secondPass: Boolean) {
        Logging.e("AsyncVersionList", Tools.printToString(e))
        versionFile.delete()
        if (!secondPass) getVersionList(listener, true)
    }

    @Suppress("SameParameterValue")
    private fun downloadVersionList(mirror: String): JMinecraftVersionList? {
        var list: JMinecraftVersionList? = null
        try {
            Logging.i("ExtVL", "Syncing to external: $mirror")
            val jsonString = DownloadUtils.downloadString(mirror)
            val parsedList: JMinecraftVersionList = Tools.JSON_CODEC.fromJson(jsonString, JMinecraftVersionList::class.java)
            list = parsedList
            Logging.i("ExtVL", "Downloaded the version list, len=${parsedList.versions.size}")

            // Then save the version list
            //TODO make it not save at times ?
            val fos = FileOutputStream(PathManager.FILE_VERSION_LIST)
            fos.write(jsonString.toByteArray())
            fos.close()
        } catch (e: IOException) {
            Logging.e("AsyncVersionList", Tools.printToString(e))
        }
        return list
    }

    /** Basic listener, acting as a callback */
    fun interface VersionDoneListener {
        fun onVersionDone(versions: JMinecraftVersionList?)
    }
}
