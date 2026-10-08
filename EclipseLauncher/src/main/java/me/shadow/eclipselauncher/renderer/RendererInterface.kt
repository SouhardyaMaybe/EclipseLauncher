package me.shadow.eclipselauncher.renderer

/**
 * Launcher renderer implementation
 */
interface RendererInterface {
    /**
     * Get the renderer's ID
     */
    fun getRendererId(): String

    /**
     * Get the renderer's unique identifier
     */
    fun getUniqueIdentifier(): String

    /**
     * Get the renderer's name
     */
    fun getRendererName(): String

    /**
     * Get the renderer's environment variables
     */
    fun getRendererEnv(): Lazy<Map<String, String>>

    /**
     * Get the libraries that need to be dlopened
     */
    fun getDlopenLibrary(): Lazy<List<String>>

    /**
     * Get the renderer's libraries
     */
    fun getRendererLibrary(): String

    /**
     * Get the EGL name
     */
    fun getRendererEGL(): String? = null
}