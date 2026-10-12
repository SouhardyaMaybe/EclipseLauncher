package me.shadow.eclipselauncher.core.utils

import android.content.Context
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.util.ArrayMap
import android.util.Log
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import me.shadow.eclipselauncher.InfoDistributor
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.event.value.JvmExitEvent
import me.shadow.eclipselauncher.feature.customprofilepath.ProfilePathHome
import me.shadow.eclipselauncher.feature.customprofilepath.ProfilePathManager
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.version.Version
import me.shadow.eclipselauncher.feature.version.VersionInfo
import me.shadow.eclipselauncher.plugins.driver.DriverPluginManager
import me.shadow.eclipselauncher.plugins.renderer.RendererPluginManager
import me.shadow.eclipselauncher.plugins.renderer.RendererPlugin
import me.shadow.eclipselauncher.renderer.RendererInterface
import me.shadow.eclipselauncher.renderer.Renderers
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.ui.activity.ErrorActivity
import me.shadow.eclipselauncher.utils.EclipseTools
import me.shadow.eclipselauncher.utils.path.LibPath
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.path.PathManager.Companion.DIR_NATIVE_LIB
import com.oracle.dalvik.VMLauncher

import me.shadow.eclipselauncher.core.Architecture
import me.shadow.eclipselauncher.core.Architecture.Companion.ARCH_X86
import me.shadow.eclipselauncher.core.Architecture.Companion.is64BitsDevice
import me.shadow.eclipselauncher.core.Logger
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.Tools.currentDisplayMetrics
import me.shadow.eclipselauncher.core.multirt.MultiRTUtils
import me.shadow.eclipselauncher.core.multirt.Runtime
import me.shadow.eclipselauncher.core.plugins.FFmpegPlugin

import org.greenrobot.eventbus.EventBus
import org.lwjgl.glfw.CallbackBridge

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.ArrayList
import java.util.Arrays
import java.util.TimeZone

import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

object JREUtils {

    @JvmField
    var LD_LIBRARY_PATH: String? = null

    @JvmField
    var jvmLibraryPath: String? = null

    /**
     * The logcat redirector used to outsource the JVM log to the launcher's own log, replacing the
     * old dedicated "jrelog" thread.
     */
    private val logRedirectScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("JREUtils-logcat"))

    @JvmStatic
    fun findInLdLibPath(libName: String?): String? {
        val ldLibraryPathEnv = Os.getenv("LD_LIBRARY_PATH")
        if (ldLibraryPathEnv == null) {
            try {
                LD_LIBRARY_PATH?.let { Os.setenv("LD_LIBRARY_PATH", it, true) }
            } catch (e: ErrnoException) {
                Logging.e("JREUtils", Tools.printToString(e))
            }
            return libName
        }
        for (libPath in ldLibraryPathEnv.split(":")) {
            val f = File(libPath, libName)
            if (f.exists() && f.isFile) {
                return f.absolutePath
            }
        }
        return libName
    }

    @JvmStatic
    fun locateLibs(path: File): ArrayList<File> {
        val returnValue = ArrayList<File>()
        val list = path.listFiles()
        if (list != null) {
            for (f in list) {
                if (f.isFile && f.name.endsWith(".so")) {
                    returnValue.add(f)
                } else if (f.isDirectory) {
                    returnValue.addAll(locateLibs(f))
                }
            }
        }
        return returnValue
    }

    @JvmStatic
    fun initJavaRuntime(jreHome: String) {
        dlopen(findInLdLibPath("libjli.so"))
        if (!dlopen("libjvm.so")) {
            Logging.w("DynamicLoader", "Failed to load with no path, trying with full path")
            dlopen(jvmLibraryPath + "/libjvm.so")
        }
        dlopen(findInLdLibPath("libverify.so"))
        dlopen(findInLdLibPath("libjava.so"))
        // dlopen(findInLdLibPath("libjsig.so"))
        dlopen(findInLdLibPath("libnet.so"))
        dlopen(findInLdLibPath("libnio.so"))
        dlopen(findInLdLibPath("libawt.so"))
        dlopen(findInLdLibPath("libawt_headless.so"))
        dlopen(findInLdLibPath("libfreetype.so"))
        dlopen(findInLdLibPath("libfontmanager.so"))
        for (f in locateLibs(File(jreHome, Tools.DIRNAME_HOME_JRE))) {
            dlopen(f.absolutePath)
        }
    }

    @JvmStatic
    fun redirectAndPrintJRELog() {

        Logging.v("jrelog", "Log starts here")
        // The old implementation spawned a dedicated thread that restarted logcat up to 10 times;
        // this loop keeps the exact same restart/exit semantics on the shared IO dispatcher.
        logRedirectScope.launch {
            var failTime = 0
            var logcatPb: ProcessBuilder? = null
            try {
                while (true) {
                    if (logcatPb == null) {
                        logcatPb = ProcessBuilder().command(
                            "logcat", /* "-G", "1mb", */ "-v", "brief", "-s", "jrelog:I", "LIBGL:I", "NativeInput"
                        ).redirectErrorStream(true)
                    }

                    Logging.i("jrelog-logcat", "Clearing logcat")
                    ProcessBuilder().command("logcat", "-c").redirectErrorStream(true).start()
                    Logging.i("jrelog-logcat", "Starting logcat")
                    val p = logcatPb.start()

                    val buf = ByteArray(1024)
                    while (true) {
                        val len = p.inputStream.read(buf)
                        if (len == -1) break
                        val currStr = String(buf, 0, len)
                        Logger.appendToLog(currStr)
                    }

                    if (p.waitFor() != 0) {
                        Logging.e("jrelog-logcat", "Logcat exited with code " + p.exitValue())
                        failTime++
                        Logging.i(
                            "jrelog-logcat",
                            (if (failTime <= 10) "Restarting logcat" else "Too many restart fails") + " (attempt $failTime/10"
                        )
                        if (failTime > 10) {
                            Logger.appendToLog("ERROR: Unable to get more Logging.")
                            break
                        }
                    } else {
                        // Logcat exited cleanly, nothing to restart
                        break
                    }
                }
            } catch (e: Throwable) {
                Logging.e("jrelog-logcat", "Exception on logging thread", e)
                Logger.appendToLog("Exception on logging thread:\n" + Log.getStackTraceString(e))
            }
        }
        Logging.i("jrelog-logcat", "Logcat thread started")

    }

    @JvmStatic
    fun relocateLibPath(runtime: Runtime, jreHome: String) {
        var jreArchitecture = runtime.arch!!
        if (Architecture.archAsInt(jreArchitecture) == ARCH_X86) {
            jreArchitecture = "i386/i486/i586"
        }

        for (arch in jreArchitecture.split("/")) {
            val f = File(jreHome, "lib/$arch")
            if (f.exists() && f.isDirectory) {
                Tools.DIRNAME_HOME_JRE = "lib/$arch"
            }
        }

        val libName = if (is64BitsDevice()) "lib64" else "lib"
        val ldLibraryPath = StringBuilder()
        if (FFmpegPlugin.isAvailable) {
            ldLibraryPath.append(FFmpegPlugin.libraryPath).append(":")
        }
        val customRenderer = RendererPluginManager.selectedRendererPlugin
        if (customRenderer != null) {
            ldLibraryPath.append(customRenderer.path).append(":")
        }
        ldLibraryPath.append(jreHome)
            .append("/").append(Tools.DIRNAME_HOME_JRE)
            .append("/jli:").append(jreHome).append("/").append(Tools.DIRNAME_HOME_JRE)
            .append(":")
        ldLibraryPath.append("/system/").append(libName).append(":")
        ldLibraryPath.append("/vendor/").append(libName).append(":")
        ldLibraryPath.append("/vendor/").append(libName).append("/hw:")
        val runtimeModDir = PathManager.DIR_RUNTIME_MOD
        if (runtimeModDir != null) {
            ldLibraryPath.append(runtimeModDir.absolutePath).append(":")
        }
        ldLibraryPath.append(DIR_NATIVE_LIB)
        LD_LIBRARY_PATH = ldLibraryPath.toString()
    }

    private fun initLdLibraryPath(jreHome: String) {
        val serverFile = File("$jreHome/${Tools.DIRNAME_HOME_JRE}/server/libjvm.so")
        jvmLibraryPath = "$jreHome/${Tools.DIRNAME_HOME_JRE}/" + (if (serverFile.exists()) "server" else "client")
        Logging.d("DynamicLoader", "Base LD_LIBRARY_PATH: $LD_LIBRARY_PATH")
        Logging.d("DynamicLoader", "Internal LD_LIBRARY_PATH: $jvmLibraryPath:$LD_LIBRARY_PATH")
        setLdLibraryPath("$jvmLibraryPath:$LD_LIBRARY_PATH")
    }

    private fun setJavaEnv(envMap: MutableMap<String, String?>, jreHome: String) {
        envMap["ECLIPSE_NATIVEDIR"] = DIR_NATIVE_LIB
        envMap["DRIVER_PATH"] = DriverPluginManager.getDriver().path
        envMap["JAVA_HOME"] = jreHome
        envMap["HOME"] = PathManager.DIR_GAME_HOME
        envMap["TMPDIR"] = PathManager.DIR_CACHE.absolutePath
        envMap["LD_LIBRARY_PATH"] = LD_LIBRARY_PATH
        envMap["PATH"] = "$jreHome/bin:" + Os.getenv("PATH")
        envMap["FORCE_VSYNC"] = AllSettings.forceVsync.getValue().toString()
        envMap["AWTSTUB_WIDTH"] =
            (if (CallbackBridge.windowWidth > 0) CallbackBridge.windowWidth else CallbackBridge.physicalWidth).toString()
        envMap["AWTSTUB_HEIGHT"] =
            (if (CallbackBridge.windowHeight > 0) CallbackBridge.windowHeight else CallbackBridge.physicalHeight).toString()
        val runtimeModDir = PathManager.DIR_RUNTIME_MOD
        envMap["MOD_ANDROID_RUNTIME"] = if (runtimeModDir != null) runtimeModDir.absolutePath else ""

        if (FFmpegPlugin.isAvailable)
            envMap["ECLIPSE_FFMPEG_PATH"] = FFmpegPlugin.executablePath
    }

    private fun setRendererEnv(envMap: MutableMap<String, String?>) {
        val currentRenderer: RendererInterface = Renderers.getCurrentRenderer()
        val rendererId = currentRenderer.getRendererId()

        if (rendererId.startsWith("opengles2")) {
            envMap["LIBGL_ES"] = "2"
            envMap["LIBGL_MIPMAP"] = "3"
            envMap["LIBGL_NOERROR"] = "1"
            envMap["LIBGL_NOINTOVLHACK"] = "1"
            envMap["LIBGL_NORMALIZE"] = "1"
        }

        envMap.putAll(currentRenderer.getRendererEnv().value)

        val eglName = currentRenderer.getRendererEGL()
        if (eglName != null) envMap["ECLIPSEEXEC_EGL"] = eglName

        envMap["ECLIPSE_RENDERER"] = rendererId

        if (RendererPluginManager.selectedRendererPlugin != null) return

        if (!rendererId.startsWith("opengles")) {
            envMap["MESA_LOADER_DRIVER_OVERRIDE"] = "zink"
            envMap["MESA_GLSL_CACHE_DIR"] = PathManager.DIR_CACHE.absolutePath
            envMap["force_glsl_extensions_warn"] = "true"
            envMap["allow_higher_compat_version"] = "true"
            envMap["allow_glsl_extension_directive_midshader"] = "true"
            envMap["LIB_MESA_NAME"] = loadGraphicsLibrary()
        }

        if (!envMap.containsKey("LIBGL_ES")) {
            val glesMajor = getDetectedVersion()
            Logging.i("glesDetect", "GLES version detected: $glesMajor")

            if (glesMajor < 3) {
                //fallback to 2 since it's the minimum for the entire app
                envMap["LIBGL_ES"] = "2"
            } else if (rendererId.startsWith("opengles")) {
                envMap["LIBGL_ES"] = rendererId.replace("opengles", "").replace("_5", "")
            } else {
                // TODO if can: other backends such as Vulkan.
                // Sure, they should provide GLES 3 support.
                envMap["LIBGL_ES"] = "3"
            }
        }
    }

    private fun setCustomEnv(envMap: MutableMap<String, String?>) {
        val customEnvFile = File(PathManager.DIR_GAME_HOME, "custom_env.txt")
        if (customEnvFile.exists() && customEnvFile.isFile) {
            BufferedReader(FileReader(customEnvFile)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val index = line.indexOf("=")
                    envMap[line.substring(0, index)] = line.substring(index + 1)
                    line = reader.readLine()
                }
            }
        }
    }

    private fun checkAndUsedJSPH(envMap: MutableMap<String, String?>, runtime: Runtime) {
        val onUseJSPH = runtime.javaVersion > 11
        if (!onUseJSPH) return
        val dir = File(DIR_NATIVE_LIB)
        if (!dir.isDirectory) return
        val jsphName = if (runtime.javaVersion == 17) "libjsph17" else "libjsph21"
        val files = dir.listFiles { _, name -> name.startsWith(jsphName) }
        if (files != null && files.isNotEmpty()) {
            val libName = "$DIR_NATIVE_LIB/$jsphName.so"
            envMap["JSP"] = libName
        }
    }

    private fun setEnv(jreHome: String, runtime: Runtime, gameVersion: Version?) {
        val envMap = ArrayMap<String, String?>()

        setJavaEnv(envMap, jreHome)
        setCustomEnv(envMap)

        if (gameVersion != null) {
            checkAndUsedJSPH(envMap, runtime)

            val versionInfo = gameVersion.getVersionInfo()
            if (versionInfo != null && versionInfo.loaderInfo != null) {
                for (loaderInfo in versionInfo.loaderInfo) {
                    val loaderEnvKey = loaderInfo.getLoaderEnvKey()
                    if (loaderEnvKey != null) {
                        envMap[loaderEnvKey] = "1"
                    }
                }
            }

            if (Renderers.isCurrentRendererValid()) {
                setRendererEnv(envMap)
            }

            envMap["ECLIPSE_VERSION_CODE"] = EclipseTools.getVersionCode().toString()
        }

        for ((envKey, envValue) in envMap) {
            Logger.appendToLog("Added custom env: $envKey=$envValue")
            try {
                // The original Java passed the possibly-null value straight to Os.setenv, which
                // fails with a NullPointerException that is logged; `!!` keeps that exact behavior.
                Os.setenv(envKey, envValue!!, true)
            } catch (exception: NullPointerException) {
                Logging.e("JREUtils", exception.toString())
            }
        }
    }

    private fun initGraphicAndSoundEngine(renderer: Boolean) {
        dlopen("$DIR_NATIVE_LIB/libopenal.so")

        if (!renderer) return

        val rendererLib = loadGraphicsLibrary()
        val customRenderer = RendererPluginManager.selectedRendererPlugin

        if (customRenderer != null) {
            customRenderer.dlopen.forEach { lib -> dlopen(customRenderer.path + "/" + lib) }
        }

        if (!dlopen(rendererLib) && !dlopen(findInLdLibPath(rendererLib))) {
            Logging.e("RENDER_LIBRARY", "Failed to load renderer $rendererLib")
        }
    }

    private fun launchJavaVM(
        activity: AppCompatActivity,
        runtimeHome: String,
        gameVersion: Version?,
        JVMArgs: List<String>,
        userArgsString: String
    ) {
        val userArgs = getJavaArgs(runtimeHome, userArgsString)
        //Remove arguments that can interfere with the good working of the launcher
        purgeArg(userArgs, "-Xms")
        purgeArg(userArgs, "-Xmx")
        purgeArg(userArgs, "-d32")
        purgeArg(userArgs, "-d64")
        purgeArg(userArgs, "-Xint")
        purgeArg(userArgs, "-XX:+UseTransparentHugePages")
        purgeArg(userArgs, "-XX:+UseLargePagesInMetaspace")
        purgeArg(userArgs, "-XX:+UseLargePages")
        purgeArg(userArgs, "-Dorg.lwjgl.opengl.libname")
        // Don't let the user specify a custom Freetype library (as the user is unlikely to specify a version compiled for Android)
        purgeArg(userArgs, "-Dorg.lwjgl.freetype.libname")
        // Overridden by us to specify the exact number of cores that the android system has
        purgeArg(userArgs, "-XX:ActiveProcessorCount")

        userArgs.add("-javaagent:" + LibPath.MIO_LIB_PATCHER.absolutePath)

        //Add automatically generated args
        userArgs.add("-Xms" + AllSettings.ramAllocation.value.getValue() + "M")
        userArgs.add("-Xmx" + AllSettings.ramAllocation.value.getValue() + "M")
        if (Renderers.isCurrentRendererValid()) userArgs.add("-Dorg.lwjgl.opengl.libname=" + loadGraphicsLibrary())

        // Force LWJGL to use the Freetype library intended for it, instead of using the one
        // that we ship with Java (since it may be older than what's needed)
        userArgs.add("-Dorg.lwjgl.freetype.libname=" + DIR_NATIVE_LIB + "/libfreetype.so")

        // Some phones are not using the right number of cores, fix that
        userArgs.add("-XX:ActiveProcessorCount=" + java.lang.Runtime.getRuntime().availableProcessors())

        userArgs.addAll(JVMArgs)
        activity.runOnUiThread {
            Toast.makeText(
                activity,
                activity.getString(R.string.autoram_info_msg, AllSettings.ramAllocation.value.getValue()),
                Toast.LENGTH_SHORT
            ).show()
        }
        println(JVMArgs)
        var i = 0
        while (i < userArgs.size) {
            val arg = userArgs[i]
            if (arg.startsWith("--accessToken")) {
                i += 1
            }
            Logger.appendToLog("JVMArg: $arg")
            i++
        }

        setupExitMethod(activity.application)
        initializeGameExitHook()
        chdir(if (gameVersion == null) ProfilePathHome.getGameHome() else gameVersion.getGameDir().absolutePath)
        userArgs.add(0, "java") //argv[0] is the program name according to C standard.

        val exitCode = VMLauncher.launchJVM(userArgs.toTypedArray())
        Logger.appendToLog("Java Exit code: $exitCode")
        if (exitCode != 0) {
            ErrorActivity.showExitMessage(activity, exitCode, false)
        }
        EventBus.getDefault().post(JvmExitEvent(exitCode))
    }

    @JvmStatic
    @Throws(Throwable::class)
    fun launchWithUtils(
        activity: AppCompatActivity,
        runtime: Runtime,
        gameVersion: Version?,
        JVMArgs: List<String>,
        userArgsString: String
    ) {
        val runtimeHome = MultiRTUtils.getRuntimeHome(runtime.name).absolutePath

        relocateLibPath(runtime, runtimeHome)

        initLdLibraryPath(runtimeHome)

        setEnv(runtimeHome, runtime, gameVersion)

        initJavaRuntime(runtimeHome)

        initGraphicAndSoundEngine(gameVersion != null)

        launchJavaVM(activity, runtimeHome, gameVersion, JVMArgs, userArgsString)
    }

    /**
     *  Gives an argument list filled with both the user args
     *  and the auto-generated ones (eg. the window resolution).
     * @return A list filled with args.
     */
    @JvmStatic
    fun getJavaArgs(runtimeHome: String, userArgumentsString: String): MutableList<String> {
        val userArguments = parseJavaArguments(userArgumentsString)
        val resolvFile = File(PathManager.DIR_DATA, "resolv.conf").absolutePath

        val overridableArguments = ArrayList(
            Arrays.asList(
                "-Djava.home=$runtimeHome",
                "-Djava.io.tmpdir=" + PathManager.DIR_CACHE.absolutePath,
                "-Djna.boot.library.path=$DIR_NATIVE_LIB",
                "-Duser.home=" + ProfilePathManager.getCurrentPath(),
                "-Duser.language=" + System.getProperty("user.language"),
                "-Dos.name=Linux",
                "-Dos.version=Android-" + Build.VERSION.RELEASE,
                "-Declipse.path.minecraft=" + ProfilePathHome.getGameHome(),
                "-Declipse.path.private.account=" + PathManager.DIR_ACCOUNT_NEW,
                "-Duser.timezone=" + TimeZone.getDefault().id,

                "-Dorg.lwjgl.vulkan.libname=libvulkan.so",
                //LWJGL 3 DEBUG FLAGS
                //"-Dorg.lwjgl.util.Debug=true",
                //"-Dorg.lwjgl.util.DebugFunctions=true",
                //"-Dorg.lwjgl.util.DebugLoader=true",
                // GLFW Stub width height
                "-Dglfwstub.windowWidth=" + Tools.getDisplayFriendlyRes(
                    currentDisplayMetrics.widthPixels, AllSettings.resolutionRatio.getValue() / 100F
                ),
                "-Dglfwstub.windowHeight=" + Tools.getDisplayFriendlyRes(
                    currentDisplayMetrics.heightPixels, AllSettings.resolutionRatio.getValue() / 100F
                ),
                "-Dglfwstub.initEgl=false",
                "-Dext.net.resolvPath=$resolvFile",
                "-Dlog4j2.formatMsgNoLookups=true", //Log4j RCE mitigation

                "-Dnet.minecraft.clientmodname=" + InfoDistributor.LAUNCHER_NAME,
                "-Dfml.earlyprogresswindow=false", //Forge 1.14+ workaround
                "-Dloader.disable_forked_guis=true",
                // Default is POSIX_SPAWN which requires starting jspawnhelper, which doesn't work on Android
                "-Djdk.lang.Process.launchMechanism=FORK",
                "-Dsodium.checks.issue2561=false"
            )
        )

        val additionalArguments = ArrayList<String>()
        for (arg in overridableArguments) {
            val strippedArg = arg.substring(0, arg.indexOf('='))
            var add = true
            for (uarg in userArguments) {
                if (uarg.startsWith(strippedArg)) {
                    add = false
                    break
                }
            }
            if (add)
                additionalArguments.add(arg)
            else
                Logging.i("ArgProcessor", "Arg skipped: $arg")
        }

        //Add all the arguments
        userArguments.addAll(additionalArguments)
        return userArguments
    }

    /**
     * Parse and separate java arguments in a user friendly fashion
     * It supports multi line and absence of spaces between arguments
     * The function also supports auto-removal of improper arguments, although it may miss some.
     *
     * @param args The un-parsed argument list.
     * @return Parsed args as an ArrayList
     */
    @JvmStatic
    fun parseJavaArguments(args: String): ArrayList<String> {
        val parsedArguments = ArrayList<String>(0)
        var arguments = args.trim().replace(" ", "")
        //For each prefixes, we separate args.
        val separators = arrayOf("-XX:-", "-XX:+", "-XX:", "--", "-D", "-X", "-javaagent:", "-verbose")
        for (prefix in separators) {
            while (true) {
                val start = arguments.indexOf(prefix)
                if (start == -1) break
                //Get the end of the current argument by checking the nearest separator
                var end = -1
                for (separator in separators) {
                    val tempEnd = arguments.indexOf(separator, start + prefix.length)
                    if (tempEnd == -1) continue
                    if (end == -1) {
                        end = tempEnd
                        continue
                    }
                    end = minOf(end, tempEnd)
                }
                //Fallback
                if (end == -1) end = arguments.length

                //Extract it
                val parsedSubString = arguments.substring(start, end)
                arguments = arguments.replace(parsedSubString, "")

                //Check if two args aren't bundled together by mistake
                if (parsedSubString.indexOf('=') == parsedSubString.lastIndexOf('=')) {
                    val arraySize = parsedArguments.size
                    if (arraySize > 0) {
                        val lastString = parsedArguments[arraySize - 1]
                        // Looking for list elements
                        if (lastString[lastString.length - 1] == ',' ||
                            parsedSubString.contains(",")
                        ) {
                            parsedArguments[arraySize - 1] = lastString + parsedSubString
                            continue
                        }
                    }
                    parsedArguments.add(parsedSubString)
                } else Logging.w("JAVA ARGS PARSER", "Removed improper arguments: $parsedSubString")
            }
        }
        return parsedArguments
    }

    /**
     * Open the render library in accordance to the settings.
     * It will fallback if it fails to load the library.
     * @return The name of the loaded library
     */
    @JvmStatic
    fun loadGraphicsLibrary(): String? {
        return if (!Renderers.isCurrentRendererValid()) null
        else {
            val rendererPlugin = RendererPluginManager.selectedRendererPlugin
            if (rendererPlugin != null) {
                rendererPlugin.path + "/" + rendererPlugin.glName
            } else {
                Renderers.getCurrentRenderer().getRendererLibrary()
            }
        }
    }

    /**
     * Remove the argument from the list, if it exists
     * If the argument exists multiple times, they will all be removed.
     * @param argList The argument list to purge
     * @param argStart The argument to purge from the list.
     */
    private fun purgeArg(argList: MutableList<String>, argStart: String) {
        argList.removeIf { arg -> arg.startsWith(argStart) }
    }

    private const val EGL_OPENGL_ES_BIT = 0x0001
    private const val EGL_OPENGL_ES2_BIT = 0x0004
    private const val EGL_OPENGL_ES3_BIT_KHR = 0x0040

    @Suppress("SameParameterValue")
    private fun hasExtension(extensions: String, name: String): Boolean {
        var start = extensions.indexOf(name)
        while (start >= 0) {
            // check that we didn't find a prefix of a longer extension name
            val end = start + name.length
            if (end == extensions.length || extensions[end] == ' ') {
                return true
            }
            start = extensions.indexOf(name, end)
        }
        return false
    }

    @JvmStatic
    fun getDetectedVersion(): Int {
        /*
         * Get all the device configurations and check the EGL_RENDERABLE_TYPE attribute
         * to determine the highest ES version supported by any config. The
         * EGL_KHR_create_context extension is required to check for ES3 support; if the
         * extension is not present this test will fail to detect ES3 support. This
         * effectively makes the extension mandatory for ES3-capable devices.
         */
        val egl = EGLContext.getEGL() as EGL10
        val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        val numConfigs = IntArray(1)
        return if (egl.eglInitialize(display, null)) {
            try {
                val checkES3 = hasExtension(
                    egl.eglQueryString(display, EGL10.EGL_EXTENSIONS),
                    "EGL_KHR_create_context"
                )
                if (egl.eglGetConfigs(display, null, 0, numConfigs)) {
                    val configs = arrayOfNulls<EGLConfig>(numConfigs[0])
                    if (egl.eglGetConfigs(display, configs, numConfigs[0], numConfigs)) {
                        var highestEsVersion = 0
                        val value = IntArray(1)
                        for (i in 0 until numConfigs[0]) {
                            if (egl.eglGetConfigAttrib(
                                    display, configs[i],
                                    EGL10.EGL_RENDERABLE_TYPE, value
                                )
                            ) {
                                if (checkES3 && ((value[0] and EGL_OPENGL_ES3_BIT_KHR) ==
                                            EGL_OPENGL_ES3_BIT_KHR)
                                ) {
                                    if (highestEsVersion < 3) highestEsVersion = 3
                                } else if ((value[0] and EGL_OPENGL_ES2_BIT) == EGL_OPENGL_ES2_BIT) {
                                    if (highestEsVersion < 2) highestEsVersion = 2
                                } else if ((value[0] and EGL_OPENGL_ES_BIT) == EGL_OPENGL_ES_BIT) {
                                    if (highestEsVersion < 1) highestEsVersion = 1
                                }
                            } else {
                                Logging.w(
                                    "glesDetect", "Getting config attribute with " +
                                            "EGL10#eglGetConfigAttrib failed " +
                                            "(" + i + "/" + numConfigs[0] + "): " +
                                            egl.eglGetError()
                                )
                            }
                        }
                        highestEsVersion
                    } else {
                        Logging.e(
                            "glesDetect", "Getting configs with EGL10#eglGetConfigs failed: " +
                                    egl.eglGetError()
                        )
                        -1
                    }
                } else {
                    Logging.e(
                        "glesDetect", "Getting number of configs with EGL10#eglGetConfigs failed: " +
                                egl.eglGetError()
                    )
                    -2
                }
            } finally {
                egl.eglTerminate(display)
            }
        } else {
            Logging.e("glesDetect", "Couldn't initialize EGL.")
            -3
        }
    }

    @JvmStatic
    external fun chdir(path: String): Int

    @JvmStatic
    external fun dlopen(libPath: String?): Boolean

    @JvmStatic
    external fun setLdLibraryPath(ldLibraryPath: String)

    @JvmStatic
    external fun setupBridgeWindow(surface: Any?)

    @JvmStatic
    external fun releaseBridgeWindow()

    @JvmStatic
    external fun initializeGameExitHook()

    @JvmStatic
    external fun setupExitMethod(context: Context)

    // Obtain AWT screen pixels to render on Android SurfaceView
    @JvmStatic
    external fun renderAWTScreenFrame(/* Object canvas, int width, int height */): IntArray?

    init {
        System.loadLibrary("exithook")
        System.loadLibrary("eclipseexec")
        System.loadLibrary("eclipseexec_awt")
    }
}
