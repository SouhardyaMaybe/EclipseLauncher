package me.shadow.eclipselauncher.core.value

import androidx.annotation.Keep

@Keep
open class MinecraftClientInfo {
    @JvmField
    var sha1: String? = null
    @JvmField
    var size: Int = 0
    @JvmField
    var url: String? = null
}
