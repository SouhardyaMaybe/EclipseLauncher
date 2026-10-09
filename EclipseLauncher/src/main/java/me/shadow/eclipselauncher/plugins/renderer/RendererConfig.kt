package me.shadow.eclipselauncher.plugins.renderer

import me.shadow.eclipselauncher.json.annotations.SerializedName

data class RendererConfig(
    val pluginVersion: Int = 0,
    var rendererId: String = "",
    var rendererDisplayName: String = "",
    var glName: String = "",
    var eglName: String = "",
    val boatEnv: Map<String, String> = emptyMap(),
    // The on-disk renderer plugin config format still uses the legacy key name
    @SerializedName("pojavEnv")
    val eclipseEnv: Map<String, String> = emptyMap(),
    val dlopenList: List<String>? = null
)