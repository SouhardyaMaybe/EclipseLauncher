package me.shadow.eclipselauncher.core.modloaders

import me.shadow.eclipselauncher.core.utils.DownloadUtils

object OptiFineUtils {
    @JvmStatic
    @Throws(Exception::class)
    fun downloadOptiFineVersions(force: Boolean): OptiFineVersions {
        return DownloadUtils.downloadStringCached("https://optifine.net/downloads", "of_downloads_page", force, OptiFineScraper())
    }

    class OptiFineVersions {
        var minecraftVersions: MutableList<String> = ArrayList()
        var optifineVersions: MutableList<MutableList<OptiFineVersion>> = ArrayList()
    }

    class OptiFineVersion {
        var minecraftVersion: String? = null
        var versionName: String? = null
        var downloadUrl: String? = null
    }
}
