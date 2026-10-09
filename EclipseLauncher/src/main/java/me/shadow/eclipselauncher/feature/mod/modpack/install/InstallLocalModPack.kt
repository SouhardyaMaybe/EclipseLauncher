package me.shadow.eclipselauncher.feature.mod.modpack.install

import android.content.Context
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.download.item.ModLoaderWrapper
import me.shadow.eclipselauncher.feature.download.platform.curseforge.CurseForgeModPackInstallHelper
import me.shadow.eclipselauncher.feature.download.platform.modrinth.ModrinthModPackInstallHelper
import me.shadow.eclipselauncher.feature.download.utils.PlatformUtils
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.mod.modpack.install.ModPackUtils.ModPackEnum
import me.shadow.eclipselauncher.feature.version.VersionConfig
import me.shadow.eclipselauncher.feature.version.VersionsManager
import me.shadow.eclipselauncher.task.TaskExecutors
import me.shadow.eclipselauncher.ui.dialog.TipDialog
import me.shadow.eclipselauncher.utils.io.FileUtils
import java.io.File
import java.util.zip.ZipFile

class InstallLocalModPack {
    companion object {
        @JvmStatic
        @Throws(Exception::class)
        fun installModPack(
            context: Context,
            type: ModPackEnum?,
            zipFile: File,
            customVersionName: String
        ): ModLoaderWrapper? {
            try {
                runCatching {
                    ZipFile(zipFile)
                }.getOrElse {
                    Logging.e("Install local ModPack", "This file doesn't seem to be a proper archive", it)
                    TaskExecutors.runInUIThread {
                        showUnSupportDialog(context)
                    }
                    return null
                }.use { modpackZipFile ->
                    val modLoader: ModLoaderWrapper?
                    val versionPath = VersionsManager.getVersionPath(customVersionName)

                    when (type) {
                        ModPackEnum.CURSEFORGE -> {
                            modLoader = curseforgeModPack(zipFile, versionPath) ?: return null
                            VersionConfig.createIsolation(versionPath).save()

                            return modLoader
                        }

                        ModPackEnum.MODRINTH -> {
                            modLoader = modrinthModPack(zipFile, versionPath) ?: return null
                            VersionConfig.createIsolation(versionPath).save()

                            return modLoader
                        }

                        else -> {
                            TaskExecutors.runInUIThread {
                                showUnSupportDialog(context)
                            }
                            return null
                        }
                    }
                }
            } finally {
                FileUtils.deleteQuietly(zipFile) // Delete the file (although such files are usually not very large)
            }
        }

        @JvmStatic
        fun showUnSupportDialog(context: Context) {
            TipDialog.Builder(context)
                .setTitle(R.string.generic_warning)
                .setMessage(R.string.select_modpack_local_not_supported) //Show a dialog reminder
                .setWarning()
                .setShowCancel(true)
                .setShowConfirm(false)
                .showDialog()
        }

        @Throws(Exception::class)
        private fun curseforgeModPack(
            zipFile: File,
            versionPath: File
        ): ModLoaderWrapper? {
            return CurseForgeModPackInstallHelper.installZip(
                PlatformUtils.createCurseForgeApi(),
                zipFile,
                versionPath
            )
        }

        @Throws(Exception::class)
        private fun modrinthModPack(
            zipFile: File,
            versionPath: File
        ): ModLoaderWrapper? {
            return ModrinthModPackInstallHelper.installZip(
                zipFile,
                versionPath
            )
        }
    }
}
