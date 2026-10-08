package me.shadow.eclipselauncher.feature.mod.modpack.install

import android.content.Context
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.download.item.ModLoaderWrapper
import me.shadow.eclipselauncher.feature.download.platform.curseforge.CurseForgeModPackInstallHelper
import me.shadow.eclipselauncher.feature.download.platform.modrinth.ModrinthModPackInstallHelper
import me.shadow.eclipselauncher.feature.download.utils.PlatformUtils
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.mod.models.MCBBSPackMeta
import me.shadow.eclipselauncher.feature.mod.modpack.MCBBSModPack
import me.shadow.eclipselauncher.feature.mod.modpack.install.ModPackUtils.ModPackEnum
import me.shadow.eclipselauncher.feature.version.VersionConfig
import me.shadow.eclipselauncher.feature.version.VersionsManager
import me.shadow.eclipselauncher.task.TaskExecutors
import me.shadow.eclipselauncher.ui.dialog.TipDialog
import me.shadow.eclipselauncher.utils.stringutils.StringUtils
import me.shadow.eclipselauncher.pojav.Tools
import org.apache.commons.io.FileUtils
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

                        ModPackEnum.MCBBS -> {
                            val mcbbsEntry = modpackZipFile.getEntry("mcbbs.packmeta")

                            val mcbbsPackMeta = Tools.GLOBAL_GSON.fromJson(
                                Tools.read(
                                    modpackZipFile.getInputStream(mcbbsEntry)
                                ), MCBBSPackMeta::class.java
                            )

                            modLoader = mcbbsModPack(context, zipFile, versionPath) ?: return null
                            VersionConfig.createIsolation(versionPath).apply {
                                setJavaArgs(StringUtils.insertSpace(null, *mcbbsPackMeta.launchInfo.javaArgument))
                            }.save()

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
                FileUtils.deleteQuietly(zipFile) // 删除文件（虽然文件通常来说并不会很大）
            }
        }

        @JvmStatic
        fun showUnSupportDialog(context: Context) {
            TipDialog.Builder(context)
                .setTitle(R.string.generic_warning)
                .setMessage(R.string.select_modpack_local_not_supported) //弹窗提醒
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

        @Throws(Exception::class)
        private fun mcbbsModPack(context: Context, zipFile: File, versionPath: File): ModLoaderWrapper? {
            val mcbbsModPack = MCBBSModPack(context, zipFile)
            return mcbbsModPack.install(versionPath)
        }
    }
}
