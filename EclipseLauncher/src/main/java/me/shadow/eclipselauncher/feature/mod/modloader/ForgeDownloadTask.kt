package me.shadow.eclipselauncher.feature.mod.modloader

import kotlinx.coroutines.runInterruptible
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.modloaders.ForgeUtils
import me.shadow.eclipselauncher.core.progresskeeper.ProgressKeeper
import me.shadow.eclipselauncher.core.utils.DownloadUtils
import me.shadow.eclipselauncher.feature.version.install.InstallTask
import me.shadow.eclipselauncher.mcgui.ProgressLayout
import me.shadow.eclipselauncher.utils.path.PathManager
import java.io.File
import java.io.IOException

class ForgeDownloadTask : InstallTask, Tools.DownloaderFeedback {
    private var mDownloadUrl: String? = null
    private var mFullVersion: String? = null
    private var mLoaderVersion: String? = null
    private var mGameVersion: String? = null

    constructor(forgeVersion: String) {
        this.mDownloadUrl = ForgeUtils.getInstallerUrl(forgeVersion)
        this.mFullVersion = forgeVersion
    }

    constructor(gameVersion: String, loaderVersion: String) {
        this.mLoaderVersion = loaderVersion
        this.mGameVersion = gameVersion
    }

    @Throws(Exception::class)
    override suspend fun run(customName: String): File? {
        var outputFile: File? = null
        if (runInterruptible { determineDownloadUrl() }) {
            outputFile = downloadForge()
        }
        ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
        return outputFile
    }

    override fun updateProgress(curr: Long, max: Long) {
        val progress100 = ((curr.toFloat() / max.toFloat()) * 100f).toInt()
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, progress100, R.string.mod_download_progress, mFullVersion)
    }

    private suspend fun downloadForge(): File {
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.mod_download_progress, mFullVersion)
        val destinationFile = File(PathManager.DIR_CACHE, "forge-installer.jar")
        val buffer = ByteArray(8192)
        runInterruptible { DownloadUtils.downloadFileMonitored(mDownloadUrl, destinationFile, buffer, this) }
        return destinationFile
    }

    @Throws(Exception::class)
    fun determineDownloadUrl(): Boolean {
        if (mDownloadUrl != null && mFullVersion != null) return true
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.mod_forge_searching)
        if (!findVersion()) {
            throw IOException("Version not found")
        }
        return true
    }

    @Throws(Exception::class)
    fun findVersion(): Boolean {
        val forgeVersions: List<String>? = ForgeUtils.downloadForgeVersions(false)
        if (forgeVersions == null) return false
        val versionStart = "$mGameVersion-$mLoaderVersion"
        for (versionName in forgeVersions) {
            if (!versionName.startsWith(versionStart)) continue
            mFullVersion = versionName
            mDownloadUrl = ForgeUtils.getInstallerUrl(mFullVersion)
            return true
        }
        return false
    }
}
