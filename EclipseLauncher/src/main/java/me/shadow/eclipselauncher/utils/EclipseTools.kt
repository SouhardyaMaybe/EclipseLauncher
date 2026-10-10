package me.shadow.eclipselauncher.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.GLES20
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import me.shadow.eclipselauncher.BuildConfig
import me.shadow.eclipselauncher.InfoDistributor
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.context.ContextExecutor
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.ui.dialog.TipDialog
import me.shadow.eclipselauncher.ui.fragment.FragmentWithAnim
import me.shadow.eclipselauncher.utils.file.FileTools
import me.shadow.eclipselauncher.utils.path.PathManager

import me.shadow.eclipselauncher.core.Tools

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.util.Date
import java.util.Locale
import java.util.zip.ZipOutputStream

object EclipseTools {

    /**
     * Zips the launcher's own log and the game log, then shares the archive. Runs on the shared IO
     * dispatcher; the dialog is shown/dismissed and the share intent is fired on the UI thread, like
     * the TaskExecutors callbacks it replaced.
     */
    private val shareLogsScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("EclipseTools-shareLogs"))

    @JvmStatic
    fun onBackPressed(fragmentActivity: FragmentActivity) {
        fragmentActivity.onBackPressedDispatcher.onBackPressed()
    }

    @JvmStatic
    fun setTooltipText(vararg views: ImageView) {
        for (view in views) {
            setTooltipText(view, view.contentDescription)
        }
    }

    @JvmStatic
    fun setTooltipText(view: View, tooltip: CharSequence?) {
        TooltipCompat.setTooltipText(view, tooltip)
    }

    @JvmStatic
    @Synchronized
    fun customMouse(context: Context): Drawable? {
        val mouseFile = getCustomMouse()
            ?: return ResourcesCompat.getDrawable(context.resources, R.drawable.ic_mouse_pointer, context.theme)

        // Mouse: custom mouse image
        return if (mouseFile.exists()) {
            Drawable.createFromPath(mouseFile.absolutePath)
        } else {
            ResourcesCompat.getDrawable(context.resources, R.drawable.ic_mouse_pointer, context.theme)
        }
    }

    @JvmStatic
    fun getCustomMouse(): File? {
        val customMouse = AllSettings.customMouse.value
        if (customMouse.isEmpty()) return null
        return File(PathManager.DIR_CUSTOM_MOUSE, customMouse)
    }

    @JvmStatic
    fun dialogForceClose(ctx: Context) {
        TipDialog.Builder(ctx)
            .setTitle(R.string.option_force_close)
            .setMessage(R.string.force_exit_confirm)
            .setConfirmClickListener {
                try {
                    killProcess()
                } catch (th: Throwable) {
                    Logging.w(InfoDistributor.LAUNCHER_NAME, "Could not enable System.exit() method!", th)
                }
            }.showDialog()
    }

    /**
     * Show a tip dialog telling the user about the link that is about to be opened in the browser; the user may choose not to visit it
     * @param link the link to visit
     */
    @JvmStatic
    fun openLink(context: Context, link: String) {
        openLink(context, link, null)
    }

    /**
     * Show a tip dialog telling the user about the link that is about to be opened in the browser; the user may choose not to visit it
     * @param link the link to visit
     * @param dataType the intent data and the explicit MIME data type
     */
    @JvmStatic
    fun openLink(context: Context, link: String, dataType: String?) {
        TipDialog.Builder(context)
            .setTitle(R.string.open_link)
            .setMessage(link)
            .setConfirmClickListener {
                val uri = Uri.parse(link)
                val browserIntent: Intent
                if (dataType != null) {
                    browserIntent = Intent(Intent.ACTION_VIEW)
                    browserIntent.setDataAndType(uri, dataType)
                } else {
                    browserIntent = Intent(Intent.ACTION_VIEW, uri)
                }
                context.startActivity(browserIntent)
            }.showDialog()
    }

    @JvmStatic
    fun swapFragmentWithAnim(
        fragment: Fragment,
        fragmentClass: Class<out Fragment>,
        fragmentTag: String?,
        bundle: Bundle?
    ) {
        if (fragment is FragmentWithAnim) {
            fragment.slideOut()
        }
        getFragmentTransaction(fragment)
            .replace(R.id.container_fragment, fragmentClass, bundle, fragmentTag)
            .addToBackStack(fragmentClass.name)
            .commit()
    }

    @JvmStatic
    fun addFragment(
        fragment: Fragment,
        fragmentClass: Class<out Fragment>,
        fragmentTag: String?,
        bundle: Bundle?
    ) {
        getFragmentTransaction(fragment)
            .addToBackStack(fragmentClass.name)
            .add(R.id.container_fragment, fragmentClass, bundle, fragmentTag)
            .hide(fragment)
            .commit()
    }

    private fun getFragmentTransaction(fragment: Fragment): FragmentTransaction {
        val transaction = fragment.requireActivity().supportFragmentManager.beginTransaction()
        if (AllSettings.animation.value) {
            transaction.setCustomAnimations(R.anim.cut_into, R.anim.cut_out, R.anim.cut_into, R.anim.cut_out)
        }
        return transaction.setReorderingAllowed(true)
    }

    @JvmStatic
    fun killProcess() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    @JvmStatic
    fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    @JvmStatic
    fun getVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    @JvmStatic
    fun getPackageName(): String {
        return BuildConfig.APPLICATION_ID
    }

    //Get the last update time of the software
    @JvmStatic
    fun getLastUpdateTime(context: Context): String {
        val packageManager = context.packageManager
        try {
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            val date = Date(packageInfo.lastUpdateTime)

            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return simpleDateFormat.format(date)
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
    }

    /**
     * @return whether the launcher is a pre-release
     */
    @JvmStatic
    fun isPreRelease(): Boolean {
        return "PRE_RELEASE" == InfoDistributor.BUILD_TYPE
    }

    /**
     * @return whether the launcher is a stable release
     */
    @JvmStatic
    fun isRelease(): Boolean {
        return "RELEASE" == InfoDistributor.BUILD_TYPE
    }

    /**
     * @return whether the launcher is a beta
     */
    @JvmStatic
    fun isDebug(): Boolean {
        return "DEBUG" == InfoDistributor.BUILD_TYPE
    }

    //Get the version status info
    @JvmStatic
    fun getVersionStatus(context: Context): String {
        val status = when {
            isPreRelease() -> context.getString(R.string.generic_pre_release)
            isRelease() -> context.getString(R.string.generic_release)
            else -> context.getString(R.string.generic_debug)
        }

        return status
    }

    @JvmStatic
    fun getDate(dateString: String): Date {
        val formatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendPattern("[XXX][X]")
            .toFormatter()
            .withZone(ZoneOffset.UTC)

        var instant: Instant
        try {
            instant = Instant.from(formatter.parse(dateString))
        } catch (e: DateTimeParseException) {
            try {
                instant = Instant.parse(dateString)
            } catch (e2: DateTimeParseException) {
                instant = OffsetDateTime.parse(dateString).toInstant()
            }
        }
        return Date.from(instant)
    }

    @JvmStatic
    fun getSystemLanguage(): String {
        val locale = Locale.getDefault()
        return locale.language + "_" + locale.country.toLowerCase(Locale.getDefault())
    }

    @JvmStatic
    fun checkForNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            ContextExecutor.getApplication(),
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_DENIED
    }

    @JvmStatic
    fun createTaskRunningDialog(context: Context): AlertDialog {
        return createTaskRunningDialog(context, null)
    }

    @JvmStatic
    fun createTaskRunningDialog(context: Context, message: String?): AlertDialog {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.view_task_running, null)

        val textView = dialogView.findViewById<TextView>(R.id.text_view)
        if (textView != null && message != null) {
            textView.text = message
        }

        return AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }

    @JvmStatic
    fun showTaskRunningDialog(context: Context): AlertDialog {
        val dialog = createTaskRunningDialog(context)
        dialog.show()
        return dialog
    }

    @JvmStatic
    fun showTaskRunningDialog(context: Context, message: String): AlertDialog {
        val dialog = createTaskRunningDialog(context, message)
        dialog.show()
        return dialog
    }

    @JvmStatic
    fun isAdrenoGPU(): Boolean {
        val eglDisplay: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            Logging.e("CheckVendor", "Failed to get EGL display")
            return false
        }

        if (!EGL14.eglInitialize(eglDisplay, null, 0, null, 0)) {
            Logging.e("CheckVendor", "Failed to initialize EGL")
            return false
        }

        val eglAttributes = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, eglAttributes, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
            EGL14.eglTerminate(eglDisplay)
            Logging.e("CheckVendor", "Failed to choose an EGL config")
            return false
        }

        val contextAttributes = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,  // OpenGL ES 2.0
            EGL14.EGL_NONE
        )

        val context: EGLContext =
            EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttributes, 0)
        if (context == EGL14.EGL_NO_CONTEXT) {
            EGL14.eglTerminate(eglDisplay)
            Logging.e("CheckVendor", "Failed to create EGL context")
            return false
        }

        if (!EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, context)) {
            EGL14.eglDestroyContext(eglDisplay, context)
            EGL14.eglTerminate(eglDisplay)
            Logging.e("CheckVendor", "Failed to make EGL context current")
            return false
        }

        val vendor = GLES20.glGetString(GLES20.GL_VENDOR)
        val renderer = GLES20.glGetString(GLES20.GL_RENDERER)
        val isAdreno = (vendor != null && renderer != null &&
                vendor.equals("Qualcomm", ignoreCase = true) &&
                renderer.toLowerCase(Locale.getDefault()).contains("adreno"))

        // Cleanup
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroyContext(eglDisplay, context)
        EGL14.eglTerminate(eglDisplay)

        Logging.d("CheckVendor", "Running on Adreno GPU: $isAdreno")
        return isAdreno
    }

    @JvmStatic
    @Synchronized
    fun shareLogs(context: Context) {
        val dialog = createTaskRunningDialog(context)

        shareLogsScope.launch {
            withContext(Dispatchers.Main) { dialog.show() }
            try {
                val zipFile = withContext(Dispatchers.IO) { zipLogs() }
                withContext(Dispatchers.Main) { FileTools.shareFile(context, zipFile) }
            } catch (t: Throwable) {
                Logging.e("Zip Log", Tools.printToString(t))
            } finally {
                withContext(Dispatchers.Main) { dialog.dismiss() }
            }
        }
    }

    private fun zipLogs(): File {
        val zipFile = File(PathManager.DIR_APP_CACHE, "logs.zip")

        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(fos).use { zos ->

                val logsFolder = File(PathManager.DIR_LAUNCHER_LOG)
                if (logsFolder.exists() && logsFolder.isDirectory) {
                    FileTools.zipDirectory(logsFolder, "launcher_logs/", { file ->
                        val fileName = file.name
                        fileName == "latestcrash.txt" || (fileName.startsWith("log") && fileName.endsWith(".txt"))
                    }, zos)
                } else Log.d("Zip Log", "The launcher log does not exist or is not available")

                val latestLogFile = File(PathManager.DIR_GAME_HOME, "/latestlog.txt")
                if (latestLogFile.exists() && latestLogFile.isFile) {
                    FileTools.zipFile(latestLogFile, latestLogFile.name, zos)
                } else Log.d("Zip Log", "The game run log does not exist")
            }
        }

        return zipFile
    }

    @JvmStatic
    fun isDarkMode(context: Context): Boolean {
        val configuration = context.resources.configuration
        return (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    @JvmStatic
    fun getWebViewAfterProcessing(view: WebView) {
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                val color = arrayOfNulls<String>(2)
                val darkMode = isDarkMode(view.context)
                color[0] = if (darkMode) "#333333" else "#CFCFCF"
                color[1] = if (darkMode) "#ffffff" else "#0E0E0E"

                val css = ("body { background-color: " + color[0] + "; color: " + color[1] + "; }" +
                        "a, a:link, a:visited, a:hover, a:active {" +
                        "  color: " + color[1] + ";" +
                        "  text-decoration: none;" +
                        "  pointer-events: none;" + //Disable link interactivity
                        "}")

                //JavaScript code used to add CSS styles to the WebView
                val js = ("var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "if (style.styleSheet){" +
                        "  style.styleSheet.cssText = '" + css.replace("'", "\\'") + "';" +
                        "} else {" +
                        "  style.appendChild(document.createTextNode('" + css.replace("'", "\\'") + "'));" +
                        "}" +
                        "parent.appendChild(style);")

                view.evaluateJavascript(js, null)
            }
        }
    }

    @JvmStatic
    fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
