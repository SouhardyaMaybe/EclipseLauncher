package me.shadow.eclipselauncher.feature.download.utils

import me.shadow.eclipselauncher.InfoDistributor
import me.shadow.eclipselauncher.pojav.modloaders.modpacks.api.ApiHandler

class PlatformUtils {
    companion object {
        fun createCurseForgeApi() = ApiHandler(
            "https://api.curseforge.com/v1",
            InfoDistributor.CURSEFORGE_API_KEY
        )

        inline fun <T> ApiHandler.safeRun(block: ApiHandler.() -> T): T? =
            runCatching(block).getOrNull()
    }
}