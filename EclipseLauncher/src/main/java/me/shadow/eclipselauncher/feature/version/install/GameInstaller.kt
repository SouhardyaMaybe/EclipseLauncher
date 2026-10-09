package me.shadow.eclipselauncher.feature.version.install

import android.app.Activity
import me.shadow.eclipselauncher.mcgui.ProgressLayout
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.event.value.InstallGameEvent
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.version.VersionsManager
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.progresskeeper.ProgressKeeper
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import me.shadow.eclipselauncher.core.tasks.AsyncMinecraftDownloader
import me.shadow.eclipselauncher.core.tasks.MinecraftDownloader
import me.shadow.eclipselauncher.utils.io.FileUtils
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class GameInstaller(
    private val activity: Activity,
    installEvent: InstallGameEvent
) {
    private val realVersion: String = installEvent.minecraftVersion
    private val customVersionName: String = installEvent.customVersionName
    private val taskMap: Map<Addon, InstallTaskItem> = installEvent.taskMap
    private val targetVersionFolder = VersionsManager.getVersionPath(customVersionName)
    private val vanillaVersionFolder = VersionsManager.getVersionPath(realVersion)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("GameInstaller"))

    fun installGame() {
        Logging.i("Minecraft Downloader", "Start downloading the version: $realVersion")

        if (taskMap.isNotEmpty()) {
            ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.download_install_download_file, 0, 0, 0)
        }

        val mcVersion = AsyncMinecraftDownloader.getListedVersion(realVersion)
        coroutineScope.launch {
            try {
                MinecraftDownloader().start(mcVersion, realVersion)
            } catch (throwable: Throwable) {
                Tools.showErrorRemote(throwable)
                if (taskMap.isNotEmpty()) {
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
                }
                return@launch
            }

            //The vanilla copy, the mod files and the single ModLoader install run in order inside
            //this coroutine - exactly the body of the old Task.runTask { }.ended { } chain, with
            //the same error mapping (a thrown failure skips the completion handling, like before)
            try {
                val taskResult: Pair<File?, InstallTaskItem>? = if (taskMap.isEmpty()) {
                    //If there are no add-ons, only the vanilla version needs to be installed, so make sure the vanilla .json file exists inside this custom version folder
                    //Check whether the version name was customized: if the real version equals the custom name, the user did not change the name and a plain vanilla install is being performed
                    //If the name was not customized, do not copy the version file - the vanilla file and the target file are the same file anyway!
                    if (realVersion != customVersionName && VersionsManager.isVersionExists(realVersion)) {
                        //Locate the vanilla .json file; it was already downloaded when MinecraftDownloader started
                        val vanillaJsonFile = File(vanillaVersionFolder, "${vanillaVersionFolder.name}.json")
                        if (vanillaJsonFile.exists() && vanillaJsonFile.isFile) {
                            //If the vanilla .json file exists, just copy it over
                            FileUtils.copyFile(vanillaJsonFile, File(targetVersionFolder, "$customVersionName.json"))
                        }
                    }
                    //There is no ModLoader task, so the following pointless ModLoader steps are skipped entirely!
                    null
                } else {
                    //Separate the Mod and ModLoader tasks; the mods should be installed first
                    val modTask: MutableList<InstallTaskItem> = ArrayList()
                    val modloaderTask = AtomicReference<Pair<Addon, InstallTaskItem>>() //For now only one ModLoader can be installed at a time
                    taskMap.forEach { (addon, taskItem) ->
                        if (taskItem.isMod) modTask.add(taskItem)
                        else modloaderTask.set(Pair(addon, taskItem))
                    }

                    //Download the mod files
                    modTask.forEach { task ->
                        Logging.i("Install Version", "Installing Mod: ${task.selectedVersion}")
                        val file = task.task.run(customVersionName)
                        val endTask = task.endTask
                        file?.let { endTask?.endTask(activity, it) }
                        ensureActive() //never triggers today (the scope is not cancelled), but a cancelled install must not start the next mod
                    }

                    modloaderTask.get()?.let { taskPair ->
                        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.mod_download_progress, taskPair.first.addonName)

                        Logging.i("Install Version", "Installing ModLoader: ${taskPair.second.selectedVersion}")
                        val file = taskPair.second.task.run(customVersionName)
                        Pair(file, taskPair.second)
                    }
                }

                taskResult?.let { pair ->
                    pair.first?.let {
                        pair.second.endTask?.endTask(activity, it)
                    }
                }
            } catch (e: Throwable) {
                Tools.showErrorRemote(e)
            }
        }
    }
}