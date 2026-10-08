package me.shadow.eclipselauncher.plugins

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import me.shadow.eclipselauncher.plugins.driver.DriverPluginManager
import me.shadow.eclipselauncher.plugins.renderer.RendererPlugin
import me.shadow.eclipselauncher.plugins.renderer.RendererPluginManager
import me.shadow.eclipselauncher.renderer.RendererInterface
import me.shadow.eclipselauncher.renderer.Renderers
import me.shadow.eclipselauncher.utils.path.PathManager
import org.apache.commons.io.FileUtils

/**
 * Centralizes plugin loading, ensuring the app list is fetched only once
 */
object PluginLoader {
    private var isInitialized: Boolean = false
    private const val PACKAGE_FLAGS =
        PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES

    @JvmStatic
    @SuppressLint("QueryPermissionsNeeded")
    fun loadAllPlugins(context: Context, force: Boolean = false) {
        if (isInitialized && !force) return
        isInitialized = true

        DriverPluginManager.initDriver(context, force)
        if (force) RendererPluginManager.clearPlugin()

        val queryIntentActivities =
            context.packageManager.queryIntentActivities(
                Intent("android.intent.action.MAIN"),
                PACKAGE_FLAGS
            )
        queryIntentActivities.forEach {
            val applicationInfo = it.activityInfo.applicationInfo
            DriverPluginManager.parsePlugin(applicationInfo)
            RendererPluginManager.parseApkPlugin(context, applicationInfo)
        }

        //Try to parse local renderer plugins
        PathManager.DIR_INSTALLED_RENDERER_PLUGIN.listFiles()?.let { files ->
            files.forEach { file ->
                if (!(file.isDirectory && RendererPluginManager.parseLocalPlugin(context, file))) {
                    //Renderer plugins that do not meet the requirements will be deleted!
                    FileUtils.deleteQuietly(file)
                }
            }
        }

        if (RendererPluginManager.isAvailable()) {
            val failedToLoadList: MutableList<RendererPlugin> = mutableListOf()
            RendererPluginManager.getRendererList().forEach { rendererPlugin ->
                val isSuccess = Renderers.addRenderer(
                    object : RendererInterface {
                        override fun getRendererId(): String = rendererPlugin.id

                        override fun getUniqueIdentifier(): String = rendererPlugin.uniqueIdentifier

                        override fun getRendererName(): String = rendererPlugin.displayName

                        override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { rendererPlugin.env }

                        override fun getDlopenLibrary(): Lazy<List<String>> = lazy { rendererPlugin.dlopen }

                        override fun getRendererLibrary(): String = rendererPlugin.glName

                        override fun getRendererEGL(): String = rendererPlugin.eglName
                    }
                )
                if (!isSuccess) failedToLoadList.add(rendererPlugin)
            }
            if (failedToLoadList.isNotEmpty()) RendererPluginManager.removeRenderer(failedToLoadList)
        }
    }
}