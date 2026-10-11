package me.shadow.eclipselauncher.core.value

import androidx.annotation.Keep

@Keep
class MinecraftLibraryArtifact : MinecraftClientInfo() {
    @JvmField
    var path: String? = null
}
