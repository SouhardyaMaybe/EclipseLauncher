package me.shadow.eclipselauncher.feature.version

import android.os.Parcel
import android.os.Parcelable
import me.shadow.eclipselauncher.feature.customprofilepath.ProfilePathHome
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.pojav.Tools
import java.io.File

/**
 * A Minecraft version, identified by its version name
 * @param versionsFolder the versions folder that contains this version
 * @param versionPath the path of the version
 * @param versionConfig the configuration of the individual version
 * @param isValid whether the version is valid
 */
class Version(
    private val versionsFolder: String,
    private val versionPath: String,
    private val versionConfig: VersionConfig,
    private val isValid: Boolean
) :Parcelable {
    /**
     * Controls whether the game is launched treating the current account as an offline account
     */
    var offlineAccountLogin: Boolean = false

    /**
     * @return the versions folder that contains this version
     */
    fun getVersionsFolder(): String = versionsFolder

    /**
     * @return the version folder
     */
    fun getVersionPath(): File = File(versionPath)

    /**
     * @return the version name
     */
    fun getVersionName(): String = getVersionPath().name

    /**
     * @return the version isolation configuration
     */
    fun getVersionConfig() = versionConfig

    /**
     * @return whether the version is valid: whether the version JSON file exists and whether the version folder exists
     */
    fun isValid() = isValid && getVersionPath().exists()

    /**
     * @return whether version isolation is enabled
     */
    fun isIsolation() = versionConfig.isIsolation()

    /**
     * @return the game folder path of the version (the version folder itself when version isolation is enabled)
     */
    fun getGameDir(): File {
        return if (versionConfig.isIsolation()) versionConfig.getVersionPath()
        //When version isolation is disabled a custom path may be used; if the custom path is empty (i.e. not set), fall back to the default game path (.minecraft/)
        else if (versionConfig.getCustomPath().isNotEmpty()) File(versionConfig.getCustomPath())
        else File(ProfilePathHome.getGameHome())
    }

    private fun String.getValueOrDefault(default: String): String = this.takeIf { it.isNotEmpty() } ?: default

    fun getRenderer(): String = versionConfig.getRenderer().getValueOrDefault(AllSettings.renderer.getValue())

    fun getDriver(): String = versionConfig.getDriver().getValueOrDefault(AllSettings.driver.getValue())

    fun getJavaDir(): String = versionConfig.getJavaDir().getValueOrDefault(AllSettings.defaultRuntime.getValue())

    fun getJavaArgs(): String = versionConfig.getJavaArgs().getValueOrDefault(AllSettings.javaArgs.getValue())

    fun getControl(): String {
        val configControl = versionConfig.getControl().removeSuffix("./")
        return if (configControl.isNotEmpty()) File(PathManager.DIR_CTRLMAP_PATH, configControl).absolutePath
        else File(AllSettings.defaultCtrl.getValue()).absolutePath
    }

    fun getCustomInfo(): String = versionConfig.getCustomInfo().getValueOrDefault(AllSettings.versionCustomInfo.getValue())
        .replace("[zl_version]", ZHTools.getVersionName())

    fun getVersionInfo(): VersionInfo? {
        return runCatching {
            val infoFile = File(VersionsManager.getEclipseVersionPath(this), "VersionInfo.json")
            Tools.GLOBAL_GSON.fromJson(Tools.read(infoFile), VersionInfo::class.java)
        }.getOrElse { null }
    }

    private fun Boolean.getInt(): Int = if (this) 1 else 0

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeStringList(listOf(versionsFolder, versionPath))
        dest.writeParcelable(versionConfig, flags)
        dest.writeInt(isValid.getInt())
        dest.writeInt(offlineAccountLogin.getInt())
    }

    companion object CREATOR : Parcelable.Creator<Version> {
        private fun Int.toBoolean(): Boolean = this != 0

        override fun createFromParcel(parcel: Parcel): Version {
            val stringList = ArrayList<String>()
            parcel.readStringList(stringList)
            val versionConfig = parcel.readParcelable<VersionConfig>(VersionConfig::class.java.classLoader)!!
            val isValid = parcel.readInt().toBoolean()
            val offlineAccount = parcel.readInt().toBoolean()
            return Version(stringList[0], stringList[1], versionConfig, isValid).apply {
                offlineAccountLogin = offlineAccount
            }
        }

        override fun newArray(size: Int): Array<Version?> {
            return arrayOfNulls(size)
        }
    }
}