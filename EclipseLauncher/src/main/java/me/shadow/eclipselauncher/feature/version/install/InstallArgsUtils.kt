package me.shadow.eclipselauncher.feature.version.install

import android.content.Intent
import com.google.gson.JsonParser
import me.shadow.eclipselauncher.mcgui.ProgressLayout
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.customprofilepath.ProfilePathHome
import me.shadow.eclipselauncher.utils.path.LibPath
import me.shadow.eclipselauncher.pojav.JavaGUILauncherActivity
import me.shadow.eclipselauncher.pojav.progresskeeper.ProgressKeeper
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class InstallArgsUtils(private val mcVersion: String, private val loaderVersion: String) {
    fun setFabric(intent: Intent, jarFile: File, customName: String) {
        val args = "-DprofileName=\"$customName\" -javaagent:${LibPath.MIO_FABRIC_AGENT.absolutePath}" +
                " -jar ${jarFile.absolutePath} client -mcversion \"$mcVersion\" -loader \"$loaderVersion\" -dir \"${ProfilePathHome.getGameHome()}\""
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
    }

    @Deprecated("JRE 8 is not supported for installation; with newer JRE environments the installer does not exit automatically, so this function is temporarily unused")
    fun setQuilt(intent: Intent, jarFile: File) {
        val args = "-jar ${jarFile.absolutePath} install client \"$mcVersion\" \"$loaderVersion\" --install-dir=\"${ProfilePathHome.getGameHome()}\""
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
    }

    @Throws(Throwable::class)
    fun setForge(intent: Intent, jarFile: File, customName: String) {
        forgeLikeCustomVersionName(jarFile, customName)

        val args = "-javaagent:${LibPath.FORGE_INSTALLER.absolutePath}=\"$loaderVersion\" -jar ${jarFile.absolutePath}"
        intent.putExtra("javaArgs", args)
    }

    @Throws(Throwable::class)
    fun setNeoForge(intent: Intent, jarFile: File, customName: String) {
        forgeLikeCustomVersionName(jarFile, customName)

        val args = "-jar ${jarFile.absolutePath} --installClient \"${ProfilePathHome.getGameHome()}\""
        intent.putExtra("javaArgs", args)
        intent.putExtra(JavaGUILauncherActivity.SUBSCRIBE_JVM_EXIT_EVENT, true)
        intent.putExtra(JavaGUILauncherActivity.FORCE_SHOW_LOG, true)
        intent.putExtra("disableSecurityManager", true)
    }

    fun setOptiFine(intent: Intent, jarFile: File, customName: String) {
        val args = "-javaagent:${LibPath.FORGE_INSTALLER.absolutePath}=OFNPS " +
                "-javaagent:${LibPath.OPTIFINE_RENAMER.absolutePath}=\"$customName\" " +
                "-jar ${jarFile.absolutePath}"
        intent.putExtra("javaArgs", args)
    }

    /**
     * Change the version key in the install_profile.json of the Forge or NeoForge installer to customName
     * The Forge installer uses this version value to create the corresponding version folder
     * This is done so the installation location of the version json can be customized
     */
    @Throws(Throwable::class)
    private fun forgeLikeCustomVersionName(jarFile: File, customName: String) {
        val tempJarFile = File(jarFile.parentFile, "${jarFile.nameWithoutExtension}_temp.jar")
        val profileJson = File(jarFile.parentFile, "install_profile.json")
        try {
            updateProgress(0)

            if (tempJarFile.exists()) tempJarFile.delete()
            extractInstallProfile(jarFile, profileJson)
            updateProgress(50)

            modifyJsonFile(profileJson, customName)
            writeTempJarFile(jarFile, tempJarFile, profileJson)
            updateProgress(100)

            if (!jarFile.delete()) throw IOException("Failed to delete original Installer file!")
            if (!tempJarFile.renameTo(jarFile)) throw IOException("Failed to rename temp Installer file to original!")
            profileJson.delete()
        } catch (e: Exception) {
            throw RuntimeException(e)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE)
        }
    }

    private fun updateProgress(progress: Int) {
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, progress, R.string.mod_forge_custom_version)
    }

    /**
     * Extract install_profile.json
     */
    @Throws(Throwable::class)
    private fun extractInstallProfile(jarFile: File, profileJson: File) {
        val zipFile = ZipFile(jarFile)
        val entry = zipFile.getEntry("install_profile.json")
            ?: throw IOException("File \"install_profile.json\" not found in the Installer")
        profileJson.outputStream().use { outputStream ->
            zipFile.getInputStream(entry).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    /**
     * Achieve a custom version name by modifying values in the install_profile.json file
     */
    @Throws(Throwable::class)
    private fun modifyJsonFile(profileJson: File, customName: String) {
        val jsonObject = JsonParser.parseString(profileJson.readText()).asJsonObject
        //Check for the spec key to decide whether this is a new-format installer
        if (jsonObject.has("spec")) { //New-format installer
            if (!jsonObject.has("version")) throw IOException("Unable to find version key!")
            //In install_profile.json, change the version value to customName to complete the custom version name
            jsonObject.addProperty("version", customName)
        } else { //Legacy installer
            if (!jsonObject.has("install")) throw IOException("Unable to find install key!")
            val install = jsonObject.get("install").asJsonObject
            if (!install.has("target")) throw IOException("Unable to find install-target key!")
            //Change the target value to customName to complete the custom version name in the legacy format
            install.addProperty("target", customName)
            jsonObject.add("install", install)
        }
        profileJson.writeText(jsonObject.toString())
    }

    @Throws(Throwable::class)
    private fun writeTempJarFile(jarFile: File, tempJarFile: File, profileJson: File) {
        //Skip only files ending in .SF or .RSA under META-INF, so that verification does not detect that install_profile.json was modified
        fun needSkip(entryName: String) = entryName.startsWith("META-INF/") && (entryName.endsWith(".SF") || entryName.endsWith(".RSA"))

        ZipFile(jarFile).use { zipFile ->
            ZipOutputStream(tempJarFile.outputStream()).use { zos ->
                zipFile.entries().asSequence().forEach { originalEntry ->
                    zos.putNextEntry(ZipEntry(originalEntry.name))
                    if (originalEntry.name == "install_profile.json") {
                        profileJson.inputStream().use { fis -> fis.copyTo(zos) }
                    } else {
                        if (!originalEntry.isDirectory && !needSkip(originalEntry.name)) {
                            //Write the original file
                            zipFile.getInputStream(originalEntry).use { it.copyTo(zos) }
                        }
                    }
                    zos.closeEntry()
                }
            }
        }
    }
}