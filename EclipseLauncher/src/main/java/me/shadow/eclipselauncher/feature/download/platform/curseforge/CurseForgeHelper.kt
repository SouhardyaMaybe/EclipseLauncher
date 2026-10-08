package me.shadow.eclipselauncher.feature.download.platform.curseforge

import android.widget.Toast
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.context.ContextExecutor
import me.shadow.eclipselauncher.feature.download.Filters
import me.shadow.eclipselauncher.feature.download.enums.Classify
import me.shadow.eclipselauncher.feature.download.install.InstallHelper
import me.shadow.eclipselauncher.feature.download.install.UnpackWorldZipHelper
import me.shadow.eclipselauncher.feature.download.item.InfoItem
import me.shadow.eclipselauncher.feature.download.item.ModLoaderWrapper
import me.shadow.eclipselauncher.feature.download.item.ScreenshotItem
import me.shadow.eclipselauncher.feature.download.item.SearchResult
import me.shadow.eclipselauncher.feature.download.item.VersionItem
import me.shadow.eclipselauncher.feature.download.platform.AbstractPlatformHelper
import me.shadow.eclipselauncher.feature.download.platform.curseforge.CurseForgeCommonUtils.Companion.CURSEFORGE_MODPACK_CLASS_ID
import me.shadow.eclipselauncher.feature.download.platform.curseforge.CurseForgeCommonUtils.Companion.CURSEFORGE_MOD_CLASS_ID
import me.shadow.eclipselauncher.feature.download.utils.PlatformUtils
import java.io.File

class CurseForgeHelper : AbstractPlatformHelper(PlatformUtils.createCurseForgeApi()) {
    override fun copy(): AbstractPlatformHelper {
        return CurseForgeHelper()
    }

    //Switch to building the link with the slug
    override fun getWebUrl(infoItem: InfoItem): String? {
        return "https://www.curseforge.com/minecraft/${
            when (infoItem.classify) {
                Classify.ALL -> return null
                Classify.MOD -> "mc-mods"
                Classify.MODPACK -> "modpacks"
                Classify.RESOURCE_PACK -> "texture-packs"
                Classify.WORLD -> "worlds"
                Classify.SHADER_PACK -> "shaders"
            }
        }/${infoItem.slug}"
    }

    override fun getScreenshots(projectId: String): List<ScreenshotItem> {
        return CurseForgeCommonUtils.getScreenshots(api, projectId)
    }

    @Throws(Throwable::class)
    override fun searchMod(filters: Filters, lastResult: SearchResult): SearchResult? {
        return CurseForgeModHelper.modLikeSearch(api, lastResult, filters, CURSEFORGE_MOD_CLASS_ID, Classify.MOD)
    }

    @Throws(Throwable::class)
    override fun searchModPack(filters: Filters, lastResult: SearchResult): SearchResult? {
        return CurseForgeModHelper.modLikeSearch(api, lastResult, filters, CURSEFORGE_MODPACK_CLASS_ID, Classify.MODPACK)
    }

    @Throws(Throwable::class)
    override fun searchResourcePack(filters: Filters, lastResult: SearchResult): SearchResult? {
        return CurseForgeCommonUtils.getResults(api, lastResult, filters, 12, Classify.RESOURCE_PACK)
    }

    @Throws(Throwable::class)
    override fun searchWorld(filters: Filters, lastResult: SearchResult): SearchResult? {
        return CurseForgeCommonUtils.getResults(api, lastResult, filters, 17, Classify.WORLD)
    }

    @Throws(Throwable::class)
    override fun searchShaderPack(filters: Filters, lastResult: SearchResult): SearchResult? {
        return CurseForgeCommonUtils.getResults(api, lastResult, filters, 6552, Classify.SHADER_PACK)
    }

    @Throws(Throwable::class)
    override fun getModVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return CurseForgeModHelper.getModVersions(api, infoItem, force)
    }

    @Throws(Throwable::class)
    override fun getModPackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return CurseForgeModHelper.getModPackVersions(api, infoItem, force)
    }

    @Throws(Throwable::class)
    override fun getResourcePackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return CurseForgeCommonUtils.getVersions(api, infoItem, force)
    }

    @Throws(Throwable::class)
    override fun getWorldVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return CurseForgeCommonUtils.getVersions(api, infoItem, force)
    }

    @Throws(Throwable::class)
    override fun getShaderPackVersions(infoItem: InfoItem, force: Boolean): List<VersionItem>? {
        return CurseForgeCommonUtils.getVersions(api, infoItem, force)
    }

    @Throws(Throwable::class)
    override fun installMod(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        InstallHelper.downloadFile(version, targetPath, progressKey)
    }

    @Throws(Throwable::class)
    override fun installModPack(version: VersionItem, customName: String): ModLoaderWrapper? {
        return CurseForgeModPackInstallHelper.startInstall(api, version, customName)
    }

    @Throws(Throwable::class)
    override fun installResourcePack(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        InstallHelper.downloadFile(version, targetPath, progressKey)
    }

    @Throws(Throwable::class)
    override fun installWorld(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        InstallHelper.downloadFile(version, targetPath, progressKey) { file ->
            targetPath.parentFile?.let {
                runCatching {
                    UnpackWorldZipHelper.unpackFile(file, it)
                }.getOrElse {
                    ContextExecutor.showToast(R.string.download_install_unpack_world_error, Toast.LENGTH_SHORT)
                }
            }
        }
    }

    @Throws(Throwable::class)
    override fun installShaderPack(infoItem: InfoItem, version: VersionItem, targetPath: File, progressKey: String) {
        InstallHelper.downloadFile(version, targetPath, progressKey)
    }
}