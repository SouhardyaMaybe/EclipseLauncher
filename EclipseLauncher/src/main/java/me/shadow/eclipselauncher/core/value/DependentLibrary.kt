package me.shadow.eclipselauncher.core.value

import androidx.annotation.Keep

import me.shadow.eclipselauncher.core.JMinecraftVersionList.Arguments.ArgValue.ArgRules

@Keep
class DependentLibrary {
    @JvmField
    var rules: Array<ArgRules>? = null
    @JvmField
    var name: String = ""
    @JvmField
    var downloads: LibraryDownloads? = null
    @JvmField
    var url: String? = null

    @Keep
    class LibraryDownloads @JvmOverloads constructor(
        @JvmField
        val artifact: MinecraftLibraryArtifact? = null
    )
}
