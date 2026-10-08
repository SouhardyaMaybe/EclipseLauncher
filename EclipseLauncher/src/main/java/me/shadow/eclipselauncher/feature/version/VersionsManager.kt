package me.shadow.eclipselauncher.feature.version

import android.content.Context
import me.shadow.eclipselauncher.InfoDistributor
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.event.single.RefreshVersionsEvent
import me.shadow.eclipselauncher.event.single.RefreshVersionsEvent.MODE.END
import me.shadow.eclipselauncher.event.single.RefreshVersionsEvent.MODE.START
import me.shadow.eclipselauncher.feature.customprofilepath.ProfilePathHome
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.version.favorites.FavoritesVersionUtils
import me.shadow.eclipselauncher.feature.version.utils.VersionInfoUtils
import me.shadow.eclipselauncher.task.Task
import me.shadow.eclipselauncher.task.TaskExecutors
import me.shadow.eclipselauncher.ui.dialog.EditTextDialog
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.utils.file.FileTools
import me.shadow.eclipselauncher.utils.stringutils.SortStrings
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.shadow.eclipselauncher.pojav.Tools
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manager for all versions
 * @see Version
 */
object VersionsManager {
    private val versions = CopyOnWriteArrayList<Version>()

    /**
     * @return the current game info
     */
    lateinit var currentGameInfo: CurrentGameInfo
        private set

    private val coroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("VersionsManager"))
    private val refreshMutex = Mutex()
    private var isRefreshing: Boolean = false
    private var lastRefreshTime = 0L

    /**
     * @return whether a refresh is allowed
     */
    @JvmStatic
    fun canRefresh() = !isRefreshing && ZHTools.getCurrentTimeMillis() - lastRefreshTime > 500

    /**
     * @return all version data
     */
    fun getVersions() = versions.toList()

    /**
     * Check whether the version already exists
     */
    fun isVersionExists(versionName: String, checkJson: Boolean = false): Boolean {
        val folder = File(ProfilePathHome.getVersionsHome(), versionName)
        //As well as requiring the version folder to exist, its version json file must exist too
        return if (checkJson) File(folder, "${folder.name}.json").exists()
        else folder.exists()
    }

    /**
     * Asynchronously refresh the current version list; once the refresh completes an event is posted to notify observers, but this event is not delivered on the UI thread
     * @param tag identifies who initiated the version refresh task, to make debugging easier
     * @see me.shadow.eclipselauncher.event.single.RefreshVersionsEvent
     */
    fun refresh(tag: String, refreshVersionInfo: Boolean = false) {
        Logging.i("VersionsManager", "$tag initiated the refresh version task")
        coroutineScope.launch {
            refreshMutex.withLock {
                lastRefreshTime = ZHTools.getCurrentTimeMillis()
                handleRefreshOperation(refreshVersionInfo)
            }
        }
    }

    private fun handleRefreshOperation(refreshVersionInfo: Boolean) {
        isRefreshing = true
        EventBus.getDefault().post(RefreshVersionsEvent(START))

        versions.clear()

        val versionsHome: String = ProfilePathHome.getVersionsHome()
        File(versionsHome).listFiles()?.forEach { versionFile ->
            runCatching {
                processVersionFile(versionsHome, versionFile, refreshVersionInfo)
            }
        }

        versions.sortWith { o1, o2 ->
            var sort = -SortStrings.compareClassVersions(
                o1.getVersionInfo()?.minecraftVersion ?: o1.getVersionName(),
                o2.getVersionInfo()?.minecraftVersion ?: o2.getVersionName()
            )
            if (sort == 0) sort = SortStrings.compareChar(o1.getVersionName(), o2.getVersionName())
            sort
        }

        currentGameInfo = CurrentGameInfo.refreshCurrentInfo()

        //Notify via event that the versions have been refreshed
        EventBus.getDefault().post(RefreshVersionsEvent(END))
        isRefreshing = false
    }

    private fun processVersionFile(versionsHome: String, versionFile: File, refreshVersionInfo: Boolean) {
        if (versionFile.exists() && versionFile.isDirectory) {
            var isVersion = false

            //Determine whether a folder is a version by checking whether its .json file exists
            val jsonFile = File(versionFile, "${versionFile.name}.json")
            if (jsonFile.exists() && jsonFile.isFile) {
                isVersion = true
                val versionInfoFile = File(getEclipseVersionPath(versionFile), "VersionInfo.json")
                if (refreshVersionInfo) FileUtils.deleteQuietly(versionInfoFile)
                if (!versionInfoFile.exists()) {
                    VersionInfoUtils.parseJson(jsonFile)?.save(versionFile)
                }
            }

            val versionConfig = VersionConfig.parseConfig(versionFile)

            val version = Version(
                versionsHome,
                versionFile.absolutePath,
                versionConfig,
                isVersion
            )
            versions.add(version)

            Logging.i("VersionsManager", "Identified and added version: ${version.getVersionName()}, " +
                    "Path: (${version.getVersionPath()}), " +
                    "Info: ${version.getVersionInfo()?.getInfoString()}")
        }
    }

    /**
     * @return the current version
     */
    fun getCurrentVersion(): Version? {
        if (versions.isEmpty()) return null

        fun returnVersionByFirst(): Version? {
            return versions.find { it.isValid() }?.apply {
                //Make sure the version is valid
                saveCurrentVersion(getVersionName())
            }
        }

        return runCatching {
            val versionString = currentGameInfo.version
            getVersion(versionString) ?: run {
                return returnVersionByFirst()
            }
        }.getOrElse { e ->
            Logging.e("Get Current Version", Tools.printToString(e))
            returnVersionByFirst()
        }
    }

    /**
     * @return whether a version with the given name exists
     */
    fun checkVersionExistsByName(versionName: String?) =
        versionName?.let { name -> versions.any { it.getVersionName() == name } } ?: false

    /**
     * @return the Eclipse launcher version marker folder
     */
    fun getEclipseVersionPath(version: Version) = File(version.getVersionPath(), InfoDistributor.LAUNCHER_NAME)

    /**
     * @return the Eclipse launcher version marker folder for the given directory
     */
    fun getEclipseVersionPath(folder: File) = File(folder, InfoDistributor.LAUNCHER_NAME)

    /**
     * @return the Eclipse launcher version marker folder for the given name
     */
    fun getEclipseVersionPath(name: String) = File(getVersionPath(name), InfoDistributor.LAUNCHER_NAME)

    /**
     * @return the icon configured for the version
     */
    fun getVersionIconFile(version: Version) = File(getEclipseVersionPath(version), "VersionIcon.png")

    /**
     * @return the icon configured for the version with the given name
     */
    fun getVersionIconFile(name: String) = File(getEclipseVersionPath(name), "VersionIcon.png")

    /**
     * @return the version folder path for the given name
     */
    fun getVersionPath(name: String) = File(ProfilePathHome.getVersionsHome(), name)

    /**
     * Save the currently selected version
     */
    fun saveCurrentVersion(versionName: String) {
        runCatching {
            currentGameInfo.apply {
                version = versionName
                saveCurrentInfo()
            }
        }.onFailure { e -> Logging.e("Save Current Version", Tools.printToString(e)) }
    }

    private fun validateVersionName(
        context: Context,
        newName: String,
        versionInfo: VersionInfo?
    ): String? {
        return when {
            isVersionExists(newName, true) ->
                context.getString(R.string.version_install_exists)
            versionInfo?.loaderInfo?.takeIf { it.isNotEmpty() }?.let {
                //If this version has ModLoader info, do not allow renaming it to the vanilla Minecraft version name, to avoid conflicts
                newName == versionInfo.minecraftVersion
            } ?: false ->
                context.getString(R.string.version_install_cannot_use_mc_name)
            else -> null
        }
    }

    /**
     * Open the rename-version dialog; must be run on the UI thread
     * @param beforeRename the action to run one step before renaming
     */
    fun openRenameDialog(context: Context, version: Version, beforeRename: (() -> Unit)? = null) {
        EditTextDialog.Builder(context)
            .setTitle(R.string.version_manager_rename)
            .setEditText(version.getVersionName())
            .setAsRequired()
            .setConfirmListener { editText, _ ->
                val string = editText.text.toString()

                //Same as the original name
                if (string == version.getVersionName()) return@setConfirmListener true

                if (FileTools.isFilenameInvalid(editText)) {
                    return@setConfirmListener false
                }

                val error = validateVersionName(context, string, version.getVersionInfo())
                error?.let {
                    editText.error = it
                    return@setConfirmListener false
                }

                beforeRename?.invoke()
                renameVersion(version, string)

                true
            }.showDialog()
    }

    /**
     * Rename the current version; the new name is not validated here
     */
    private fun renameVersion(version: Version, name: String) {
        val currentVersionName = getCurrentVersion()?.getVersionName()
        //If the version being renamed is the current version, update the current version to the new name
        if (version.getVersionName() == currentVersionName) saveCurrentVersion(name)

        //Try to refresh the version name inside the favorites
        FavoritesVersionUtils.renameVersion(version.getVersionName(), name)

        val versionFolder = version.getVersionPath()
        val renameFolder = File(ProfilePathHome.getVersionsHome(), name)

        //Whatever the target folder is after the rename, if that folder exists it must be deleted
        //Otherwise problems will occur
        FileUtils.deleteQuietly(renameFolder)

        val originalName = versionFolder.name

        FileTools.renameFile(versionFolder, renameFolder)

        val versionJsonFile = File(renameFolder, "$originalName.json")
        val versionJarFile = File(renameFolder, "$originalName.jar")
        val renameJsonFile = File(renameFolder, "$name.json")
        val renameJarFile = File(renameFolder, "$name.jar")

        FileTools.renameFile(versionJsonFile, renameJsonFile)
        FileTools.renameFile(versionJarFile, renameJarFile)

        FileUtils.deleteQuietly(versionFolder)

        //Refresh the list after renaming
        refresh("VersionsManager:renameVersion")
    }

    /**
     * Open the name input dialog for copying a version, duplicating the selected version into a new one
     */
    fun openCopyDialog(context: Context, version: Version) {
        val dialog = ZHTools.createTaskRunningDialog(context)
        EditTextDialog.Builder(context)
            .setTitle(R.string.version_manager_copy)
            .setMessage(R.string.version_manager_copy_tip)
            .setCheckBoxText(R.string.version_manager_copy_all)
            .setShowCheckBox(true)
            .setEditText(version.getVersionName())
            .setAsRequired()
            .setConfirmListener { editText, checked ->
                val string = editText.text.toString()

                //Same as the original name
                if (string == version.getVersionName()) return@setConfirmListener true

                if (FileTools.isFilenameInvalid(editText)) {
                    return@setConfirmListener false
                }

                val error = validateVersionName(context, string, version.getVersionInfo())
                error?.let {
                    editText.error = it
                    return@setConfirmListener false
                }

                Task.runTask {
                    copyVersion(version, string, checked)
                }.beforeStart(TaskExecutors.getAndroidUI()) {
                    dialog.show()
                }.onThrowable { e ->
                    Tools.showErrorRemote(e)
                }.finallyTask(TaskExecutors.getAndroidUI()) {
                    dialog.dismiss()
                    refresh("VersionsManager:openCopyDialog")
                }.execute()
                true
            }.showDialog()
    }

    /**
     * Copy the selected version into a new version
     * @param version the selected version
     * @param name the name of the new version
     * @param copyAllFile whether to copy all files
     */
    private fun copyVersion(version: Version, name: String, copyAllFile: Boolean) {
        val versionsFolder = version.getVersionsFolder()
        val newVersion = File(versionsFolder, name)

        val originalName = version.getVersionName()

        //The json and jar files of the new version
        val newJsonFile = File(newVersion, "$name.json")
        val newJarFile = File(newVersion, "$name.jar")

        val originalVersionFolder = version.getVersionPath()
        if (copyAllFile) {
            //When copying all files is enabled, copy the whole original folder into the new version
            FileUtils.copyDirectory(originalVersionFolder, newVersion)
            //Rename the json and jar files
            val jsonFile = File(newVersion, "$originalName.json")
            val jarFile = File(newVersion, "$originalName.jar")
            if (jsonFile.exists()) jsonFile.renameTo(newJsonFile)
            if (jarFile.exists()) jarFile.renameTo(newJarFile)
        } else {
            //When not copying all files, only copy and rename the json and jar files
            val originalJsonFile = File(originalVersionFolder, "$originalName.json")
            val originalJarFile = File(originalVersionFolder, "$originalName.jar")
            newVersion.mkdirs()
            // versions/1.21.3/1.21.3.json -> versions/name/name.json
            if (originalJsonFile.exists()) originalJsonFile.copyTo(newJsonFile)
            // versions/1.21.3/1.21.3.jar -> versions/name/name.jar
            if (originalJarFile.exists()) originalJarFile.copyTo(newJarFile)
        }

        //Save the version config file
        version.getVersionConfig().copy().let { config ->
            config.setVersionPath(newVersion)
            config.setIsolationType(VersionConfig.IsolationType.ENABLE)
            config.saveWithThrowable()
        }
    }

    private fun getVersion(name: String?): Version? {
        name?.let { versionName ->
            return versions.find { it.getVersionName() == versionName }?.takeIf { it.isValid() }
        }
        return null
    }
}