package me.shadow.eclipselauncher.plugins.renderer

data class RendererConfig(
    val pluginVersion: Int = 0,
    var rendererId: String = "",
    var rendererDisplayName: String = "",
    var glName: String = "",
    var eglName: String = "",
    val boatEnv: Map<String, String> = emptyMap(),
    val pojavEnv: Map<String, String> = emptyMap(),
    val dlopenList: List<String>? = null
)