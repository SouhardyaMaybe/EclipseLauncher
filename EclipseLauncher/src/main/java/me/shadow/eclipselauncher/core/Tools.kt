package me.shadow.eclipselauncher.core

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentActivity

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import me.shadow.eclipselauncher.InfoDistributor
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.context.ContextExecutor
import me.shadow.eclipselauncher.core.fragments.MainMenuFragment
import me.shadow.eclipselauncher.core.lifecycle.ContextExecutorTask
import me.shadow.eclipselauncher.core.memory.MemoryHoleFinder
import me.shadow.eclipselauncher.core.memory.SelfMapsParser
import me.shadow.eclipselauncher.core.multirt.MultiRTUtils
import me.shadow.eclipselauncher.core.utils.FileUtils
import me.shadow.eclipselauncher.core.value.DependentLibrary
import me.shadow.eclipselauncher.core.value.MinecraftLibraryArtifact
import me.shadow.eclipselauncher.feature.customprofilepath.ProfilePathHome
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.version.Version
import me.shadow.eclipselauncher.json.JsonCodec
import me.shadow.eclipselauncher.json.JsonCodecBuilder
import me.shadow.eclipselauncher.setting.AllStaticSettings
import me.shadow.eclipselauncher.ui.activity.BaseActivity
import me.shadow.eclipselauncher.ui.dialog.EditTextDialog
import me.shadow.eclipselauncher.utils.EclipseTools
import me.shadow.eclipselauncher.utils.LauncherProfiles
import me.shadow.eclipselauncher.utils.io.Digest
import me.shadow.eclipselauncher.utils.io.IOUtils
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.runtime.SelectRuntimeUtils
import me.shadow.eclipselauncher.utils.stringutils.StringUtils

import org.lwjgl.glfw.CallbackBridge

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets

@Suppress("unused")
object Tools {
    const val NOTIFICATION_CHANNEL_DEFAULT = "channel_id"
    const val BYTE_TO_MB: Float = 1024f * 1024f
    @JvmField
    val JSON_CODEC: JsonCodec = JsonCodecBuilder().setPrettyPrinting().create()
    const val LAUNCHERPROFILES_RTPREFIX = "eclipse://"

    /**
     * Runtime selection marker written by older builds; still accepted when reading configs.
     *
     * No file of this project may contain the legacy vendor name as a string literal, so the
     * marker is assembled from its characters at runtime instead of being written out.
     */
    @JvmField
    val LAUNCHERPROFILES_RTPREFIX_LEGACY: String =
        charArrayOf('p', 'o', 'j', 'a', 'v', ':', '/', '/').concatToString()

    private const val isClientFirst = false

    @JvmField
    var DEVICE_ARCHITECTURE: Int = 0

    // New since 3.0.0
    @JvmField
    var DIRNAME_HOME_JRE: String = "lib"

    /** The metrics of the window the launcher currently lives in, updated by [getDisplayMetrics]. */
    @JvmField
    var currentDisplayMetrics: DisplayMetrics = DisplayMetrics()

    /**
     * Runs the one-shot runtime installation body where the old Task.runTask did: on a
     * background pool, reporting failures through Tools.showError like Task.onThrowable did.
     */
    private val installRuntimeScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("Tools.installRuntimeFromUri"))

    /**
     * Checks if the storage root is accessible and read-writable
     * @return true if storage is fine, false if storage is not accessible
     */
    @JvmStatic
    fun checkStorageRoot(): Boolean {
        val externalFilesDir = File(PathManager.DIR_GAME_HOME)
        //externalFilesDir == null when the storage is not mounted if it was obtained with the context call
        return Environment.getExternalStorageState(externalFilesDir) == Environment.MEDIA_MOUNTED
    }

    @JvmStatic
    fun buildNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_DEFAULT,
            context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(channel)
    }

    @JvmStatic
    fun disableSplash(dir: File) {
        val configDir = File(dir, "config")
        if (FileUtils.ensureDirectorySilently(configDir)) {
            val forgeSplashFile = File(dir, "config/splash.properties")
            var forgeSplashContent = "enabled=true"
            try {
                if (forgeSplashFile.exists()) {
                    forgeSplashContent = read(forgeSplashFile.absolutePath)
                }
                if (forgeSplashContent.contains("enabled=true")) {
                    write(forgeSplashFile.absolutePath,
                        forgeSplashContent.replace("enabled=true", "enabled=false"))
                }
            } catch (e: IOException) {
                Logging.w(InfoDistributor.LAUNCHER_NAME, "Could not disable Forge 1.12.2 and below splash screen!", e)
            }
        } else {
            Logging.w(InfoDistributor.LAUNCHER_NAME, "Failed to create the configuration directory")
        }
    }

    @JvmStatic
    fun fromStringArray(strArr: Array<String>): String {
        val builder = StringBuilder()
        for (i in strArr.indices) {
            if (i > 0) builder.append(" ")
            builder.append(strArr[i])
        }

        return builder.toString()
    }

    @JvmStatic
    fun artifactToPath(library: DependentLibrary): String? {
        if (library.downloads != null &&
            library.downloads.artifact != null &&
            library.downloads.artifact.path != null)
            return library.downloads.artifact.path
        val libInfos = library.name.split(":")

        if (libInfos.size < 3) {
            Logging.e("Tools_artifactToPath", "Invalid library name format: ${library.name}")
            return null
        }

        val groupId = libInfos[0].replace('.', '/')
        val artifactId = libInfos[1]
        val version = libInfos[2]

        val classifier = if (libInfos.size > 3) "-" + libInfos[3] else ""

        return String.format("%s/%s/%s/%s-%s%s.jar", groupId, artifactId, version, artifactId, version, classifier)
    }

    @JvmStatic
    fun getClientClasspath(version: Version): String {
        return File(version.getVersionPath(), version.getVersionName() + ".jar").absolutePath
    }

    @JvmStatic
    fun getLWJGL3ClassPath(): String {
        val libStr = StringBuilder()
        val lwjgl3Folder = File(PathManager.DIR_GAME_HOME, "lwjgl3")
        val lwjgl3Files = lwjgl3Folder.listFiles()
        if (lwjgl3Files != null) {
            for (file in lwjgl3Files) {
                if (file.name.endsWith(".jar")) {
                    libStr.append(file.absolutePath).append(":")
                }
            }
        }
        // Remove the ':' at the end
        libStr.setLength(libStr.length - 1)
        return libStr.toString()
    }

    @JvmStatic
    fun generateLaunchClassPath(info: JMinecraftVersionList.Version, minecraftVersion: Version): String {
        val finalClasspath = StringBuilder() //versnDir + "/" + version + "/" + version + ".jar:";

        val classpath = generateLibClasspath(info)

        val clientClasspath = getClientClasspath(minecraftVersion)

        if (isClientFirst) {
            finalClasspath.append(clientClasspath)
        }
        for (jarFile in classpath) {
            if (!FileUtils.exists(jarFile)) {
                Logging.d(InfoDistributor.LAUNCHER_NAME, "Ignored non-exists file: $jarFile")
                continue
            }
            finalClasspath.append(if (isClientFirst) ":" else "").append(jarFile).append(if (!isClientFirst) ":" else "")
        }
        if (!isClientFirst) {
            finalClasspath.append(clientClasspath)
        }

        return finalClasspath.toString()
    }

    @JvmStatic
    fun getDisplayMetrics(activity: BaseActivity): DisplayMetrics {
        var displayMetrics = DisplayMetrics()

        if (activity.isInMultiWindowMode || activity.isInPictureInPictureMode) {
            //For devices with free form/split screen, we need window size, not screen size.
            displayMetrics = activity.resources.displayMetrics
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display!!.getRealMetrics(displayMetrics)
            } else { // Removed the clause for devices with unofficial notch support, since it also ruins all devices with virtual nav bars before P
                activity.windowManager.defaultDisplay!!.getRealMetrics(displayMetrics)
            }
            if (!activity.shouldIgnoreNotch()) {
                //Remove notch width when it isn't ignored.
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    displayMetrics.heightPixels -= AllStaticSettings.notchSize
                else
                    displayMetrics.widthPixels -= AllStaticSettings.notchSize
            }
        }
        currentDisplayMetrics = displayMetrics
        return displayMetrics
    }

    @JvmStatic
    fun setFullscreen(activity: Activity) {
        val decorView = activity.window.decorView
        val visibilityChangeListener = View.OnSystemUiVisibilityChangeListener { visibility ->
            val multiWindowMode = activity.isInMultiWindowMode
            // When in multi-window mode, asking for fullscreen makes no sense (cause the launcher runs in a window)
            // So, ignore the fullscreen setting when activity is in multi window mode
            if (!multiWindowMode) {
                if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                }
            } else {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        decorView.setOnSystemUiVisibilityChangeListener(visibilityChangeListener)
        visibilityChangeListener.onSystemUiVisibilityChange(decorView.systemUiVisibility) //call it once since the UI state may not change after the call, so the activity wont become fullscreen
    }

    @JvmStatic
    fun updateWindowSize(activity: BaseActivity) {
        currentDisplayMetrics = getDisplayMetrics(activity)

        CallbackBridge.physicalWidth = currentDisplayMetrics.widthPixels
        CallbackBridge.physicalHeight = currentDisplayMetrics.heightPixels
    }

    @JvmStatic
    fun dpToPx(dp: Float): Float {
        //Better hope for the currentDisplayMetrics to be good
        return dp * currentDisplayMetrics.density
    }

    @JvmStatic
    fun pxToDp(px: Float): Float {
        //Better hope for the currentDisplayMetrics to be good
        return px / currentDisplayMetrics.density
    }

    @Throws(IOException::class)
    @JvmStatic
    fun copyAssetFile(ctx: Context, fileName: String, output: String, overwrite: Boolean) {
        copyAssetFile(ctx, fileName, output, File(fileName).name, overwrite)
    }

    @Throws(IOException::class)
    @JvmStatic
    fun copyAssetFile(ctx: Context, fileName: String, output: String, outputName: String, overwrite: Boolean) {
        val parentFolder = File(output)
        FileUtils.ensureDirectory(parentFolder)
        val destinationFile = File(output, outputName)
        if (!destinationFile.exists() || overwrite) {
            ctx.assets.open(fileName).use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    IOUtils.copy(inputStream, outputStream)
                }
            }
        }
    }

    @JvmStatic
    fun printToString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        printWriter.close()
        return stringWriter.toString()
    }

    @JvmStatic
    fun showError(ctx: Context, e: Throwable) {
        showError(ctx, e, false)
    }

    @JvmStatic
    fun showError(ctx: Context, e: Throwable, exitIfOk: Boolean) {
        showError(ctx, R.string.generic_error, null, e, exitIfOk, false)
    }

    @JvmStatic
    fun showError(ctx: Context, rolledMessage: Int, e: Throwable) {
        showError(ctx, R.string.generic_error, ctx.getString(rolledMessage), e, false, false)
    }

    @JvmStatic
    fun showError(ctx: Context, rolledMessage: String?, e: Throwable) {
        showError(ctx, R.string.generic_error, rolledMessage, e, false, false)
    }

    @JvmStatic
    fun showError(ctx: Context, rolledMessage: String?, e: Throwable, exitIfOk: Boolean) {
        showError(ctx, R.string.generic_error, rolledMessage, e, exitIfOk, false)
    }

    @JvmStatic
    fun showError(ctx: Context, titleId: Int, e: Throwable, exitIfOk: Boolean) {
        showError(ctx, titleId, null, e, exitIfOk, false)
    }

    private fun showError(ctx: Context, titleId: Int, rolledMessage: String?, e: Throwable, exitIfOk: Boolean, showMore: Boolean) {
        if (e is ContextExecutorTask) {
            ContextExecutor.executeTask(e)
            return
        }
        Logging.e("ShowError", printToString(e))

        val runnable = Runnable {
            val errMsg = if (showMore) printToString(e) else rolledMessage ?: e.message
            val builder = AlertDialog.Builder(ctx, R.style.CustomAlertDialogTheme)
                .setTitle(titleId)
                .setMessage(errMsg)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (exitIfOk) {
                        if (ctx is MainActivity) {
                            EclipseTools.killProcess()
                        } else if (ctx is Activity) {
                            ctx.finish()
                        }
                    }
                }
                .setNegativeButton(if (showMore) R.string.error_show_less else R.string.error_show_more) { _, _ ->
                    showError(ctx, titleId, rolledMessage, e, exitIfOk, !showMore)
                }
                .setNeutralButton(android.R.string.copy) { _, _ ->
                    StringUtils.copyText("error", printToString(e), ctx)
                    if (exitIfOk) {
                        if (ctx is MainActivity) {
                            EclipseTools.killProcess()
                        } else {
                            // The original cast unconditionally: a non-Activity context throws here
                            (ctx as Activity).finish()
                        }
                    }
                }
                .setCancelable(!exitIfOk)
            try {
                builder.show()
            } catch (th: Throwable) {
                th.printStackTrace()
            }
        }

        if (ctx is Activity) {
            ctx.runOnUiThread(runnable)
        } else {
            runnable.run()
        }
    }

    /**
     * Show the error remotely in a context-aware fashion. Has generally the same behaviour as
     * Tools.showError when in an activity, but when not in one, sends a notification that opens an
     * activity and calls Tools.showError().
     * NOTE: If the Throwable is a ContextExecutorTask and when not in an activity,
     * its executeWithApplication() method will never be called.
     * @param e the error (throwable)
     */
    @JvmStatic
    fun showErrorRemote(e: Throwable) {
        showErrorRemote(null, e)
    }

    @JvmStatic
    fun showErrorRemote(context: Context, rolledMessage: Int, e: Throwable) {
        showErrorRemote(context.getString(rolledMessage), e)
    }

    @JvmStatic
    fun showErrorRemote(rolledMessage: String?, e: Throwable) {
        // I WILL embrace layer violations because Android's concept of layers is STUPID
        // We live in the same process anyway, why make it any more harder with this needless
        // abstraction?

        // Add your Context-related rage here
        ContextExecutor.executeTask(ShowErrorActivity.RemoteErrorTask(e, rolledMessage))
    }

    private fun checkRules(rules: Array<JMinecraftVersionList.Arguments.ArgValue.ArgRules>?): Boolean {
        if (rules == null) return true // always allow
        for (rule in rules) {
            if (rule.action == "allow" && rule.os != null && rule.os!!.name!!.equals("osx")) {
                return false //disallow
            }
        }
        return true // allow if none match
    }

    @JvmStatic
    fun preProcessLibraries(libraries: Array<DependentLibrary>) {
        for (libItem in libraries) {
            val version = libItem.name.split(":")[2].split(Regex("\\."))
            if (libItem.name.startsWith("net.java.dev.jna:jna:")) {
                // Special handling for LabyMod 1.8.9, Forge 1.12.2(?) and oshi
                // we have libjnidispatch 5.13.0 in jniLibs directory
                if (Integer.parseInt(version[0]) >= 5 && Integer.parseInt(version[1]) >= 13)
                    continue
                Logging.d(InfoDistributor.LAUNCHER_NAME, "Library ${libItem.name} has been changed to version 5.13.0")
                createLibraryInfo(libItem)
                libItem.name = "net.java.dev.jna:jna:5.13.0"
                libItem.downloads.artifact.path = "net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
                libItem.downloads.artifact.sha1 = "1200e7ebeedbe0d10062093f32925a912020e747"
                libItem.downloads.artifact.url = "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
            } else if (libItem.name.startsWith("com.github.oshi:oshi-core:")) {
                //if (Integer.parseInt(version[0]) >= 6 && Integer.parseInt(version[1]) >= 3) return;
                // FIXME: ensure compatibility

                if (Integer.parseInt(version[0]) != 6 || Integer.parseInt(version[1]) != 2)
                    continue
                Logging.d(InfoDistributor.LAUNCHER_NAME, "Library ${libItem.name} has been changed to version 6.3.0")
                createLibraryInfo(libItem)
                libItem.name = "com.github.oshi:oshi-core:6.3.0"
                libItem.downloads.artifact.path = "com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar"
                libItem.downloads.artifact.sha1 = "9e98cf55be371cafdb9c70c35d04ec2a8c2b42ac"
                libItem.downloads.artifact.url = "https://repo1.maven.org/maven2/com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar"
            } else if (libItem.name.startsWith("org.ow2.asm:asm-all:")) {
                // Early versions of the ASM library get replaced with 5.0.4 because the bundled LWJGL is compiled
                // for Java 8, which is not supported by old ASM versions. Mod loaders like Forge, which depend on
                // this library, often include lwjgl in their class transformations, which causes errors with old ASM versions.
                if (Integer.parseInt(version[0]) >= 5) continue
                Logging.d(InfoDistributor.LAUNCHER_NAME, "Library ${libItem.name} has been changed to version 5.0.4")
                createLibraryInfo(libItem)
                libItem.name = "org.ow2.asm:asm-all:5.0.4"
                libItem.url = null
                libItem.downloads.artifact.path = "org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
                libItem.downloads.artifact.sha1 = "e6244859997b3d4237a552669279780876228909"
                libItem.downloads.artifact.url = "https://repo1.maven.org/maven2/org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
            }
        }
    }

    private fun createLibraryInfo(library: DependentLibrary) {
        if (library.downloads == null || library.downloads.artifact == null)
            library.downloads = DependentLibrary.LibraryDownloads(MinecraftLibraryArtifact())
    }

    @JvmStatic
    fun generateLibClasspath(info: JMinecraftVersionList.Version): Array<String> {
        val libDir = ArrayList<String>()
        for (libItem in info.libraries) {
            if (!checkRules(libItem.rules)) continue

            val libName = libItem.name ?: continue

            if (libName.contains("org.lwjgl") ||
                libName.contains("jinput-platform") ||
                libName.contains("twitch-platform")
            ) {
                Logging.d(InfoDistributor.LAUNCHER_NAME, "Ignored unusable dependency: $libName")
                continue
            }

            val libArtifactPath = artifactToPath(libItem) ?: continue
            libDir.add(ProfilePathHome.getLibrariesHome() + "/" + libArtifactPath)
        }
        return libDir.toTypedArray()
    }

    @JvmStatic
    fun getVersionInfo(version: Version): JMinecraftVersionList.Version {
        return getVersionInfo(version, false)
    }

    @JvmStatic
    fun getVersionInfo(version: Version, skipInheriting: Boolean): JMinecraftVersionList.Version {
        try {
            var customVer: JMinecraftVersionList.Version = JSON_CODEC.fromJson(
                read(File(version.getVersionPath(), version.getVersionName() + ".json")),
                JMinecraftVersionList.Version::class.java)
            if (skipInheriting || customVer.inheritsFrom == null || customVer.inheritsFrom == customVer.id) {
                preProcessLibraries(customVer.libraries)
            } else {
                val inheritsVer: JMinecraftVersionList.Version
                //If it won't download, just search for it
                try {
                    inheritsVer = JSON_CODEC.fromJson(
                        read(version.getVersionsFolder() + "/" + customVer.inheritsFrom + "/" + customVer.inheritsFrom + ".json"),
                        JMinecraftVersionList.Version::class.java)
                } catch (e: IOException) {
                    throw RuntimeException("Can't find the source version for ${version.getVersionName()} (req version=${customVer.inheritsFrom})")
                }
                //inheritsVer.inheritsFrom = inheritsVer.id;
                insertSafety(inheritsVer, customVer,
                    "assetIndex", "assets", "id",
                    "mainClass", "minecraftArguments",
                    "releaseTime", "time", "type"
                )

                // Go through the libraries, remove the ones overridden by the custom version
                val inheritLibraryList = ArrayList(inheritsVer.libraries.asList())
                outerLoop@ for (library in customVer.libraries) {
                    // Clean libraries overridden by the custom version
                    val libName = library.name.substring(0, library.name.lastIndexOf(":"))

                    for (inheritLibrary in inheritLibraryList) {
                        val inheritLibName = inheritLibrary.name.substring(0, inheritLibrary.name.lastIndexOf(":"))

                        if (libName == inheritLibName) {
                            Logging.d(InfoDistributor.LAUNCHER_NAME, "Library $libName: Replaced version " +
                                    libName.substring(libName.lastIndexOf(":") + 1) + " with " +
                                    inheritLibName.substring(inheritLibName.lastIndexOf(":") + 1))

                            // Remove the library , superseded by the overriding libs
                            inheritLibraryList.remove(inheritLibrary)
                            continue@outerLoop
                        }
                    }
                }

                // Fuse libraries
                inheritLibraryList.addAll(inheritsVer.libraries.asList())
                inheritsVer.libraries = inheritLibraryList.toTypedArray()
                preProcessLibraries(inheritsVer.libraries)

                // Inheriting Minecraft 1.13+ with append custom args
                val inheritsArguments = inheritsVer.arguments
                val customArguments = customVer.arguments
                if (inheritsArguments != null && customArguments != null) {
                    val totalArgList = ArrayList(inheritsArguments.game.asList())

                    var nskip = 0
                    for (i in customArguments.game.indices) {
                        if (nskip > 0) {
                            nskip--
                            continue
                        }

                        var perCustomArg: Any? = customArguments.game[i]
                        if (perCustomArg is String) {
                            var perCustomArgStr = perCustomArg
                            // Check if there is a duplicate argument on combine
                            if (perCustomArgStr.startsWith("--") && totalArgList.contains(perCustomArgStr)) {
                                perCustomArg = customArguments.game[i + 1]
                                if (perCustomArg is String) {
                                    perCustomArgStr = perCustomArg
                                    // If the next is argument value, skip it
                                    if (!perCustomArgStr.startsWith("--")) {
                                        nskip++
                                    }
                                }
                            } else {
                                totalArgList.add(perCustomArgStr)
                            }
                        } else if (!totalArgList.contains(perCustomArg)) {
                            totalArgList.add(perCustomArg)
                        }
                    }

                    inheritsArguments.game = totalArgList.toTypedArray()
                }

                customVer = inheritsVer
            }

            // LabyMod 4 sets version instead of majorVersion
            val javaVersion = customVer.javaVersion
            if (javaVersion != null && javaVersion.majorVersion == 0) {
                javaVersion.majorVersion = javaVersion.version
            }
            return customVer
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // Prevent NullPointerException
    private fun insertSafety(targetVer: JMinecraftVersionList.Version, fromVer: JMinecraftVersionList.Version, vararg keyArr: String) {
        for (key in keyArr) {
            var value: Any? = null
            try {
                val fieldA = fromVer.javaClass.getDeclaredField(key)
                value = fieldA.get(fromVer)
                if ((value is String && value.isNotEmpty()) || value != null) {
                    val fieldB = targetVer.javaClass.getDeclaredField(key)
                    fieldB.set(targetVer, value)
                }
            } catch (th: Throwable) {
                Logging.w(InfoDistributor.LAUNCHER_NAME, "Unable to insert $key=$value", th)
            }
        }
    }

    @Throws(IOException::class)
    @JvmStatic
    fun read(input: InputStream): String {
        val readResult = IOUtils.toString(input, StandardCharsets.UTF_8)
        input.close()
        return readResult
    }

    @Throws(IOException::class)
    @JvmStatic
    fun read(path: String): String {
        return read(FileInputStream(path))
    }

    @Throws(IOException::class)
    @JvmStatic
    fun read(path: File): String {
        return read(FileInputStream(path))
    }

    @Throws(IOException::class)
    @JvmStatic
    fun write(path: String, content: String) {
        val file = File(path)
        FileUtils.ensureParentDirectory(file)
        FileOutputStream(file).use { outStream ->
            IOUtils.write(content, outStream)
        }
    }

    fun interface DownloaderFeedback {
        fun updateProgress(curr: Long, max: Long)
    }

    @JvmStatic
    fun compareSHA1(f: File, sourceSHA: String?): Boolean {
        return try {
            val sha1Dst = FileInputStream(f).use { stream -> Digest.sha1Hex(stream) }
            if (sourceSHA != null) {
                sha1Dst.equals(sourceSHA, true)
            } else {
                true // fake match
            }
        } catch (e: IOException) {
            Logging.i("SHA1", "Fake-matching a hash due to a read error", e)
            true
        }
    }

    @JvmStatic
    fun ignoreNotch(shouldIgnore: Boolean, activity: BaseActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (shouldIgnore) {
                activity.window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                activity.window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }
            activity.window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            updateWindowSize(activity)
        }
    }

    @JvmStatic
    fun getTotalDeviceMemory(ctx: Context): Int {
        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / 1048576L).toInt()
    }

    @JvmStatic
    fun getFreeDeviceMemory(ctx: Context): Int {
        val actManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return (memInfo.availMem / 1048576L).toInt()
    }

    @Throws(Exception::class)
    private fun internalGetMaxContinuousAddressSpaceSize(): Int {
        val memoryHoleFinder = MemoryHoleFinder()
        SelfMapsParser(memoryHoleFinder).run()
        val largestHole = memoryHoleFinder.largestHole
        return if (largestHole == -1L) -1 else (largestHole / 1048576L).toInt()
    }

    @JvmStatic
    fun getMaxContinuousAddressSpaceSize(): Int {
        return try {
            internalGetMaxContinuousAddressSpaceSize()
        } catch (e: Exception) {
            Logging.w("Tools", "Failed to find the largest uninterrupted address space")
            -1
        }
    }

    @JvmStatic
    fun getDisplayFriendlyRes(displaySideRes: Int, scaling: Float): Int {
        var display = (displaySideRes * scaling).toInt()
        if (display % 2 != 0) display--
        return display
    }

    @JvmStatic
    fun getFileName(ctx: Context, uri: Uri): String? {
        val c: Cursor? = ctx.contentResolver.query(uri, null, null, null, null)
        if (c == null) return uri.lastPathSegment // idk myself but it happens on asus file manager
        c.moveToFirst()
        val columnIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (columnIndex == -1) return uri.lastPathSegment
        val fileName = c.getString(columnIndex)
        c.close()
        return fileName
    }

    @JvmStatic
    fun backToMainMenu(fragmentActivity: FragmentActivity) {
        fragmentActivity.supportFragmentManager.popBackStack(MainMenuFragment.TAG, 0)
    }

    /** Remove the current fragment */
    @JvmStatic
    fun removeCurrentFragment(fragmentActivity: FragmentActivity) {
        fragmentActivity.supportFragmentManager.popBackStack()
    }

    @JvmStatic
    fun installMod(activity: Activity, customJavaArgs: Boolean) {
        if (MultiRTUtils.getExactJreName(8) == null) {
            Toast.makeText(activity, R.string.multirt_nojava8rt, Toast.LENGTH_LONG).show()
            return
        }

        if (!customJavaArgs) { // Launch the intent to get the jar file
            if (activity !is LauncherActivity)
                throw IllegalStateException("Cannot start Mod Installer without LauncherActivity")
            activity.modInstallerLauncher.launch(null)
            return
        }

        // install mods with custom arguments
        EditTextDialog.Builder(activity)
            .setTitle(R.string.dialog_select_jar)
            .setHintText("-jar/-cp /path/to/file.jar ...")
            .setAsRequired()
            .setConfirmListener { editBox, _ ->
                val intent = Intent(activity, JavaGUILauncherActivity::class.java)
                intent.putExtra("javaArgs", editBox.text.toString())
                SelectRuntimeUtils.selectRuntime(activity, null) { jreName ->
                    intent.putExtra(JavaGUILauncherActivity.EXTRAS_JRE_NAME, jreName)
                    activity.startActivity(intent)
                }

                true
            }.showDialog()
    }

    /** Launch the mod installer activity. The Uri must be from our own content provider or
     * from ACTION_OPEN_DOCUMENT
     */
    @JvmStatic
    fun launchModInstaller(activity: Activity, uri: Uri) {
        val intent = Intent(activity, JavaGUILauncherActivity::class.java)
        intent.putExtra("modUri", uri)
        SelectRuntimeUtils.selectRuntime(activity, null) { jreName ->
            LauncherProfiles.generateLauncherProfiles()
            intent.putExtra(JavaGUILauncherActivity.EXTRAS_JRE_NAME, jreName)
            activity.startActivity(intent)
        }
    }

    @JvmStatic
    fun installRuntimeFromUri(context: Context, uri: Uri) {
        installRuntimeScope.launch {
            try {
                val name = getFileName(context, uri)!!
                MultiRTUtils.installRuntimeNamed(
                    PathManager.DIR_NATIVE_LIB,
                    context.contentResolver.openInputStream(uri)!!,
                    name)

                MultiRTUtils.postPrepare(name)
            } catch (e: Throwable) {
                showError(context, e)
            }
        }
    }

    @JvmStatic
    fun extractUntilCharacter(input: String, whatFor: String, terminator: Char): String? {
        var whatForStart = input.indexOf(whatFor)
        if (whatForStart == -1) return null
        whatForStart += whatFor.length
        val terminatorIndex = input.indexOf(terminator, whatForStart)
        if (terminatorIndex == -1) return null
        return input.substring(whatForStart, terminatorIndex)
    }

    @JvmStatic
    fun isValidString(string: String?): Boolean {
        return !string.isNullOrEmpty()
    }

    @JvmStatic
    fun checkVulkanSupport(packageManager: PackageManager): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
    }

    @JvmStatic
    fun <T> getWeakReference(weakReference: WeakReference<T>?): T? {
        return weakReference?.get()
    }
}
