package me.shadow.eclipselauncher.plugins.renderer

import android.content.Context
import android.content.pm.ApplicationInfo
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.update.UpdateUtils
import me.shadow.eclipselauncher.renderer.Renderers
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.stringutils.StringUtilsKt
import me.shadow.eclipselauncher.pojav.Architecture
import me.shadow.eclipselauncher.pojav.Tools
import me.shadow.eclipselauncher.pojav.utils.ZipUtils
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile

/**
 * FCL and EclipseLauncher renderer plugins, with support for local renderer plugins as well
 * [FCL Renderer Plugin](https://github.com/FCL-Team/FCLRendererPlugin)
 */
object RendererPluginManager {
    private val rendererPluginList: MutableList<RendererPlugin> = mutableListOf()
    private val apkRendererPluginList: MutableList<ApkRendererPlugin> = mutableListOf()
    private val localRendererPluginList: MutableList<LocalRendererPlugin> = mutableListOf()

    /**
     * Get all renderers loaded by the current renderer plugins
     */
    @JvmStatic
    fun getRendererList() = rendererPluginList

    /**
     * Remove some of the loaded renderers
     */
    @JvmStatic
    fun removeRenderer(rendererPlugins: Collection<RendererPlugin>) {
        rendererPluginList.removeAll(rendererPlugins)
    }

    /**
     * Get all renderers loaded by the current local renderer plugins
     */
    @JvmStatic
    fun getAllLocalRendererList() = localRendererPluginList

    /**
     * @return whether it is usable
     */
    @JvmStatic
    fun isAvailable(): Boolean {
        return rendererPluginList.isNotEmpty()
    }

    /**
     * The renderer loaded by the currently selected renderer plugin
     * Determined by the renderer unique identifier selected by the master renderer manager
     */
    @JvmStatic
    val selectedRendererPlugin: RendererPlugin?
        get() {
            val currentRenderer = runCatching {
                Renderers.getCurrentRenderer().getUniqueIdentifier()
            }.getOrNull()
            return rendererPluginList.find { it.uniqueIdentifier == currentRenderer }
        }

    /**
     * Clear the renderer plugins
     */
    fun clearPlugin() {
        rendererPluginList.clear()
        apkRendererPluginList.clear()
        localRendererPluginList.clear()
    }

    /**
     * Whether the current renderer plugin has configuration options (software plugin, whitelisted package name)
     */
    @JvmStatic
    fun getConfigurablePluginOrNull(rendererUniqueIdentifier: String): RendererPlugin? {
        val renderer = apkRendererPluginList.find { it.uniqueIdentifier == rendererUniqueIdentifier }
        return renderer?.takeIf { it.packageName in setOf(
                "com.bzlzhh.plugin.ngg",
                "com.bzlzhh.plugin.ngg.angleless",
                "com.fcl.plugin.mobileglues"
            ) }
    }

    /**
     * Parse EclipseLauncher and FCL renderer plugins
     */
    fun parseApkPlugin(context: Context, info: ApplicationInfo) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (
                metaData.getBoolean("fclPlugin", false) ||
                metaData.getBoolean("eclipseRendererPlugin", false)
            ) {
                val rendererString = metaData.getString("renderer") ?: return
                val des = metaData.getString("des") ?: return
                val pojavEnvString = metaData.getString("pojavEnv") ?: return
                val nativeLibraryDir = info.nativeLibraryDir
                val renderer = rendererString.split(":")

                var rendererId: String = renderer[0]
                val envList = mutableMapOf<String, String>()
                val dlopenList = mutableListOf<String>()
                pojavEnvString.split(":").forEach { envString ->
                    if (envString.contains("=")) {
                        val stringList = envString.split("=")
                        val key = stringList[0]
                        val value = stringList[1]
                        when (key) {
                            "POJAV_RENDERER" -> rendererId = value
                            "DLOPEN" -> {
                                value.split(",").forEach { lib ->
                                    dlopenList.add(lib)
                                }
                            }
                            "LIB_MESA_NAME", "MESA_LIBRARY" -> envList[key] = "$nativeLibraryDir/$value"
                            else -> envList[key] = value
                        }
                    }
                }

                val packageName = info.packageName

                val plugin = ApkRendererPlugin(
                    rendererId,
                    "$des (${
                        context.getString(
                            R.string.setting_renderer_from_plugins,
                            runCatching {
                                context.packageManager.getApplicationLabel(info)
                            }.getOrElse {
                                context.getString(R.string.generic_unknown)
                            }
                        )
                    })",
                    packageName,
                    renderer[1],
                    renderer[2].progressEglName(nativeLibraryDir),
                    nativeLibraryDir,
                    envList,
                    dlopenList,
                    packageName
                )

                rendererPluginList.add(plugin)
                apkRendererPluginList.add(plugin)
            }
        }
    }

    /**
     * Try to parse renderer plugins from the local `/files/renderer_plugins/` directory
     * @return whether it is a plugin that meets the requirements
     *
     * Renderer folder format
     * renderer_plugins/
     * ----folder name/
     * --------renderer_config.json (config file storing the renderer's specific information)
     * --------libs/ (directory holding the renderer `.so` files)
     * ------------arm64-v8a/ (arm64 architecture)
     * ----------------renderer library .so file
     * ------------armeabi-v7a/ (arm32 architecture)
     * ----------------renderer library .so file
     * ------------x86/ (x86 architecture)
     * ----------------renderer library .so file
     * ------------x86_64/ (x86_64 architecture)
     * ----------------renderer library .so file
     */
    fun parseLocalPlugin(context: Context, directory: File): Boolean {
        val archModel: String = UpdateUtils.getArchModel(Architecture.getDeviceArchitecture()) ?: return false
        val libsDirectory: File = File(directory, "libs/$archModel").takeIf { it.exists() && it.isDirectory } ?: return false
        val rendererConfigFile: File = File(directory, "config").takeIf { it.exists() && it.isFile } ?: return false
        val rendererConfig: RendererConfig = runCatching {
            Tools.GLOBAL_GSON.fromJson(readLocalRendererPluginConfig(rendererConfigFile), RendererConfig::class.java)
        }.getOrElse { e ->
            Logging.e("LocalRendererPlugin", "Failed to parse the configuration file", e)
            return false
        }
        val uniqueIdentifier = directory.name
        rendererConfig.run {
            val libPath = libsDirectory.absolutePath

            val plugin = LocalRendererPlugin(
                rendererId,
                "$rendererDisplayName (${
                    context.getString(
                        R.string.setting_renderer_from_plugins,
                        uniqueIdentifier
                    )
                })",
                uniqueIdentifier,
                glName,
                eglName.progressEglName(libPath),
                libPath,
                pojavEnv.filter { it.key != "POJAV_RENDERER" },
                dlopenList ?: emptyList(),
                directory
            )

            rendererPluginList.add(plugin)
            localRendererPluginList.add(plugin)
        }
        return true
    }

    private fun String.progressEglName(libPath: String): String =
        if (startsWith("/")) "$libPath$this"
        else this

    private fun readLocalRendererPluginConfig(configFile: File): String {
        return FileInputStream(configFile).use { fileInputStream ->
            DataInputStream(fileInputStream).use { dataInputStream ->
                dataInputStream.readUTF()
            }
        }
    }

    /**
     * Import local renderer plugins
     */
    fun importLocalRendererPlugin(pluginFile: File): Boolean {
        if (!pluginFile.exists() || !pluginFile.isFile) {
            Logging.i("importLocalRendererPlugin", "The compressed file does not exist or is not a valid file.")
            return false
        }

        return try {
            ZipFile(pluginFile).use { pluginZip ->
                val configEntry = pluginZip.entries().asSequence().find { it.name == "config" }
                    ?: throw IllegalArgumentException("The plugin package does not meet the requirements!")

                pluginZip.getInputStream(configEntry).use { inputStream ->
                    DataInputStream(inputStream).use { dataInputStream ->
                        val configContent = dataInputStream.readUTF()
                        Tools.GLOBAL_GSON.fromJson(configContent, RendererConfig::class.java)
                    }
                }

                val pluginFolder = File(
                    PathManager.DIR_INSTALLED_RENDERER_PLUGIN,
                    StringUtilsKt.generateUniqueUUID(
                        { string ->
                            string.replace("-", "").substring(0, 8)
                        },
                        { uuid ->
                            File(PathManager.DIR_INSTALLED_RENDERER_PLUGIN, uuid).exists()
                        }
                    )
                )

                ZipUtils.zipExtract(pluginZip, "", pluginFolder)
            }
            true
        } catch (e: Exception) {
            Logging.i("importLocalRendererPlugin", "Error: ${e.message}")
            false
        }
    }
}