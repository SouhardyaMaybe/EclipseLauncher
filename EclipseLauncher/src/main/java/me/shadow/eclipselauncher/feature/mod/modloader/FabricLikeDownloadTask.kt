package me.shadow.eclipselauncher.feature.mod.modloader

import kotlinx.coroutines.runInterruptible
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.progresskeeper.ProgressKeeper
import me.shadow.eclipselauncher.core.utils.DownloadUtils
import me.shadow.eclipselauncher.core.utils.FileUtils
import me.shadow.eclipselauncher.feature.customprofilepath.ProfilePathHome
import me.shadow.eclipselauncher.feature.version.install.InstallTask
import me.shadow.eclipselauncher.mcgui.ProgressLayout
import me.shadow.eclipselauncher.utils.path.PathManager
import java.io.File

class FabricLikeDownloadTask(
    private val mUtils: FabricLikeUtils,
    private var mGameVersion: String? = null,
    private var mLoaderVersion: String? = null
) : InstallTask, Tools.DownloaderFeedback {

    @Throws(Exception::class)
    override suspend fun run(customName: String): File? {
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.mod_download_progress, mUtils.name)
        val outputFile: File?
        if (mGameVersion == null && mLoaderVersion == null) {
            outputFile = downloadInstaller()
        } else {
            legacyInstall(customName)
            outputFile = null
        }
        ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
        return outputFile
    }

    private suspend fun downloadInstaller(): File {
        val outputFile = File(PathManager.DIR_CACHE, "fabric-installer.jar")

        val installerDownloadUrl = runInterruptible { mUtils.installerDownloadUrl }
        val buffer = ByteArray(8192)
        runInterruptible { DownloadUtils.downloadFileMonitored(installerDownloadUrl, outputFile, buffer, this) }

        return outputFile
    }

    //Because Quilt needs to run with JRE 17, and the JVM does not exit automatically when it finishes
    //This is how it is handled for now, to keep the process automated
    private suspend fun legacyInstall(customName: String) {
        val jsonString = runInterruptible {
            DownloadUtils.downloadString(mUtils.createJsonDownloadUrl(mGameVersion, mLoaderVersion))
        }

        val versionJsonDir = File(ProfilePathHome.getVersionsHome(), customName)
        val versionJsonFile = File(versionJsonDir, customName + ".json")
        runInterruptible {
            FileUtils.ensureDirectory(versionJsonDir)
            Tools.write(versionJsonFile.absolutePath, jsonString)
        }
    }

    override fun updateProgress(curr: Long, max: Long) {
        val progress100 = ((curr.toFloat() / max.toFloat()) * 100f).toInt()
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, progress100, R.string.mod_download_progress, mUtils.name)
    }
}
