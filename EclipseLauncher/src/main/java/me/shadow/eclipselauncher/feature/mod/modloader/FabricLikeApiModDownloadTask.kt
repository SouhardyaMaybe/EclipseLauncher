package me.shadow.eclipselauncher.feature.mod.modloader

import me.shadow.eclipselauncher.mcgui.ProgressLayout
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.download.item.VersionItem
import me.shadow.eclipselauncher.feature.version.install.InstallTask
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.pojav.Tools
import me.shadow.eclipselauncher.pojav.progresskeeper.ProgressKeeper
import me.shadow.eclipselauncher.pojav.utils.DownloadUtils
import java.io.File

class FabricLikeApiModDownloadTask(private val fileName: String, private val versionItem: VersionItem) : InstallTask, Tools.DownloaderFeedback {
    @Throws(Exception::class)
    override fun run(customName: String): File {
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.mod_download_progress, versionItem.fileName)
        val destinationFile = File(PathManager.DIR_CACHE, "$fileName.jar")
        DownloadUtils.downloadFileMonitored(versionItem.fileUrl, destinationFile, ByteArray(8192), this)
        ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
        return destinationFile
    }

    override fun updateProgress(curr: Long, max: Long) {
        val progress100 = ((curr.toFloat() / max.toFloat()) * 100f).toInt()
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, progress100, R.string.mod_download_progress, versionItem.fileName)
    }
}