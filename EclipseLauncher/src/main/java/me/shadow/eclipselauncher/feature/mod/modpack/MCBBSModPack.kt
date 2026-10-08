package me.shadow.eclipselauncher.feature.mod.modpack

import android.content.Context
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.download.enums.ModLoader
import me.shadow.eclipselauncher.feature.download.item.ModLoaderWrapper
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.mod.models.MCBBSPackMeta
import me.shadow.eclipselauncher.feature.mod.models.MCBBSPackMeta.MCBBSAddons
import me.shadow.eclipselauncher.feature.mod.modpack.install.ModPackUtils
import me.shadow.eclipselauncher.task.TaskExecutors
import me.shadow.eclipselauncher.ui.dialog.ProgressDialog
import me.shadow.eclipselauncher.utils.file.FileTools
import me.shadow.eclipselauncher.pojav.Tools
import me.shadow.eclipselauncher.pojav.utils.FileUtils
import me.shadow.eclipselauncher.pojav.utils.ZipUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile

class MCBBSModPack(private val context: Context, private val zipFile: File?) {
    private var installDialog: ProgressDialog? = null
    private var isCanceled = false

    @Throws(IOException::class)
    fun install(versionFolder: File): ModLoaderWrapper? {
        zipFile?.let {
            ZipFile(this.zipFile).use { modpackZipFile ->
                val mcbbsPackMeta = Tools.GLOBAL_GSON.fromJson(
                    Tools.read(ZipUtils.getEntryStream(modpackZipFile, "mcbbs.packmeta")),
                    MCBBSPackMeta::class.java
                )
                if (!ModPackUtils.verifyMCBBSPackMeta(mcbbsPackMeta)) {
                    Logging.i("MCBBSModPack", "manifest verification failed")
                    return null
                }

                initDialog()

                val overridesDir = "overrides" + File.separatorChar
                val dirNameLen = overridesDir.length

                val fileCounters = AtomicInteger() //File counter
                val length = mcbbsPackMeta.files.size

                for (file in mcbbsPackMeta.files) {
                    if (isCanceled) {
                        cancel(versionFolder)
                        return null
                    }

                    val entry = modpackZipFile.getEntry(overridesDir + file.path)
                    if (entry != null) {
                        val entryName = entry.name
                        val zipDestination = File(versionFolder, entryName.substring(dirNameLen))
                        if (zipDestination.exists() && !file.force) continue

                        val fileHash = FileTools.calculateFileHash(modpackZipFile.getInputStream(entry), "SHA-1")
                        val equals = file.hash == fileHash

                        if (equals) {
                            //If the hashes match, copy the file (if it already exists, the "force" setting decides whether to overwrite it)
                            FileUtils.ensureParentDirectory(zipDestination)

                            modpackZipFile.getInputStream(entry).use { entryInputStream ->
                                Files.newOutputStream(zipDestination.toPath())
                                    .use { outputStream ->
                                        IOUtils.copy(entryInputStream, outputStream)
                                    }
                            }
                            val fileCount = fileCounters.getAndIncrement()
                            TaskExecutors.runInUIThread {
                                installDialog?.updateText(
                                    context.getString(
                                        R.string.select_modpack_local_installing_files,
                                        fileCount,
                                        length
                                    )
                                )
                                installDialog?.updateProgress(
                                    fileCount.toDouble(),
                                    length.toDouble()
                                )
                            }
                        }
                    }
                }

                closeDialog()
                return createInfo(mcbbsPackMeta.addons)
            }
        }
        return null
    }

    private fun initDialog() {
        TaskExecutors.runInUIThread {
            installDialog = ProgressDialog(context) {
                isCanceled = true
                true
            }
            installDialog?.show()
        }
    }

    private fun closeDialog() {
        TaskExecutors.runInUIThread { installDialog?.dismiss() }
    }

    private fun cancel(instanceDestination: File) {
        org.apache.commons.io.FileUtils.deleteQuietly(instanceDestination)
    }

    private fun createInfo(addons: Array<MCBBSAddons?>): ModLoaderWrapper? {
        var version = ""
        var modLoader = ""
        var modLoaderVersion = ""
        for (i in 0..addons.size) {
            if (addons[i]!!.id == "game") {
                version = addons[i]!!.version
                continue
            }
            if (addons[i] != null) {
                modLoader = addons[i]!!.id
                modLoaderVersion = addons[i]!!.version
                break
            }
        }
        val modloader = when (modLoader) {
            "forge" -> ModLoader.FORGE
            "neoforge" -> ModLoader.NEOFORGE
            "fabric" -> ModLoader.FABRIC
            else -> return null
        }
        return ModLoaderWrapper(modloader, modLoaderVersion, version)
    }
}
