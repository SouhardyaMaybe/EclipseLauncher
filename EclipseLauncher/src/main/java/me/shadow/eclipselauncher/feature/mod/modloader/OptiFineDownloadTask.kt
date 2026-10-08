package me.shadow.eclipselauncher.feature.mod.modloader

import me.shadow.eclipselauncher.mcgui.ProgressLayout
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.version.install.InstallTask
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.pojav.Tools.DownloaderFeedback
import me.shadow.eclipselauncher.pojav.modloaders.OFDownloadPageScraper
import me.shadow.eclipselauncher.pojav.modloaders.OptiFineUtils.OptiFineVersion
import me.shadow.eclipselauncher.pojav.progresskeeper.ProgressKeeper
import me.shadow.eclipselauncher.pojav.utils.DownloadUtils
import java.io.File
import java.io.IOException

class OptiFineDownloadTask(
    private val mOptiFineVersion: OptiFineVersion
) : InstallTask, DownloaderFeedback {
    private val mDestinationFile = File(PathManager.DIR_CACHE, "optifine-installer.jar")

    @Throws(IOException::class)
    override fun run(customName: String): File? {
        ProgressKeeper.submitProgress(
            ProgressLayout.INSTALL_RESOURCE,
            0,
            R.string.mod_download_progress,
            mOptiFineVersion.versionName
        )
        val downloadUrl = OFDownloadPageScraper.run(mOptiFineVersion.downloadUrl) ?: return null
        DownloadUtils.downloadFileMonitored(
            downloadUrl, mDestinationFile, ByteArray(8192),
            this
        )
        ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)

        return mDestinationFile
    }

    override fun updateProgress(curr: Long, max: Long) {
        val progress100 = ((curr.toFloat() / max.toFloat()) * 100f).toInt()
        ProgressKeeper.submitProgress(
            ProgressLayout.INSTALL_RESOURCE,
            progress100,
            R.string.mod_optifine_progress,
            mOptiFineVersion.versionName
        )
    }
}
