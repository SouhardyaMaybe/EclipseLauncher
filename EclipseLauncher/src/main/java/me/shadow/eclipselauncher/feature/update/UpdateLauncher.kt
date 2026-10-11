package me.shadow.eclipselauncher.feature.update

import me.shadow.eclipselauncher.task.TaskExecutors.Companion.runInUIThread
import me.shadow.eclipselauncher.utils.file.FileTools.Companion.formatFileSize
import me.shadow.eclipselauncher.utils.path.UrlManager.TIME_OUT

import android.app.Dialog
import android.content.Context

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.ui.dialog.ProgressDialog
import me.shadow.eclipselauncher.utils.EclipseTools
import me.shadow.eclipselauncher.utils.path.UrlManager

import me.shadow.eclipselauncher.core.Tools

import me.shadow.eclipselauncher.utils.io.FileUtils

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Timer
import java.util.TimerTask
import java.util.function.Consumer

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response

class UpdateLauncher(
    private val context: Context,
    private val launcherVersion: LauncherVersion
) {
    private val destinationFilePath: String = UpdateUtils.sApkFile.absolutePath
    private val call: Call = OkHttpClient.Builder()
        .writeTimeout(TIME_OUT.first.toLong(), TIME_OUT.second)
        .build()
        .newCall(
            UrlManager.createRequestBuilder(UpdateUtils.getDownloadUrl(launcherVersion)).build()
        )
    private var dialog: ProgressDialog? = null
    private var timer: Timer? = null
    private var isCanceled = false

    fun start() {
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                UpdateUtils.showFailToast(context, context.getString(R.string.update_fail))
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    UpdateUtils.showFailToast(context, context.getString(R.string.update_fail_code, response.code))
                    throw IOException("Unexpected code $response")
                } else {
                    val outputFile = File(destinationFilePath)
                    val body = response.body!!
                    try {
                        body.byteStream().use { inputStream ->
                            Files.newOutputStream(outputFile.toPath()).use { outputStream ->
                                val buffer = ByteArray(1024 * 1024)

                                runInUIThread {
                                    val progressDialog = ProgressDialog(context) {
                                        stop()
                                        true
                                    }
                                    dialog = progressDialog
                                    progressDialog.show()
                                }

                                var downloadedSize = 0L
                                var lastSize = 0L
                                var lastTime = EclipseTools.getCurrentTimeMillis()

                                //Limit the refresh rate
                                timer = Timer()
                                timer!!.schedule(object : TimerTask() {
                                    override fun run() {
                                        val size = downloadedSize
                                        val currentTime = EclipseTools.getCurrentTimeMillis()
                                        val timeElapsed = (currentTime - lastTime) / 1000.0
                                        val sizeChange = size - lastSize
                                        val rate = (sizeChange / timeElapsed).toLong()

                                        lastSize = size
                                        lastTime = currentTime

                                        val formattedDownloaded = formatFileSize(size)
                                        val totalSize = formatFileSize(UpdateUtils.getFileSize(launcherVersion.fileSize))
                                        handleDialog { dialog ->
                                            dialog.updateProgress(size.toDouble(), UpdateUtils.getFileSize(launcherVersion.fileSize).toDouble())
                                            dialog.updateRate(if (rate > 0) rate else 0L)
                                            dialog.updateText(String.format(context.getString(R.string.update_downloading), formattedDownloaded, totalSize))
                                        }
                                    }
                                }, 0, 120)

                                try {
                                    while (true) {
                                        val bytesRead = inputStream.read(buffer)
                                        if (bytesRead == -1) break
                                        outputStream.write(buffer, 0, bytesRead)
                                        downloadedSize += bytesRead
                                    }
                                    finish(outputFile)
                                } catch (e: Exception) {
                                    handleDownloadError(e)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        handleDownloadError(e)
                    }
                }
            }
        })
    }

    private fun finish(outputFile: File) {
        handleDialog(Dialog::dismiss)
        timer!!.cancel()

        UpdateUtils.installApk(context, outputFile)
    }

    fun handleDialog(func: Consumer<ProgressDialog>) {
        if (this.dialog != null) {
            runInUIThread { func.accept(this@UpdateLauncher.dialog!!) }
        }
    }

    private fun handleDownloadError(e: Exception) {
        if (isCanceled) {
            //The download has already been cancelled, so do not handle any exception caused by the cancellation
            return
        }

        handleDialog(Dialog::dismiss)
        runInUIThread { Tools.showError(context, R.string.update_fail, e) }
        timer!!.cancel()
        FileUtils.deleteQuietly(UpdateUtils.sApkFile)
        Logging.e("Update Launcher", "There was an exception downloading the update!", e)
    }

    private fun stop() {
        this.isCanceled = true
        this.call.cancel()
        this.timer!!.cancel()
        FileUtils.deleteQuietly(UpdateUtils.sApkFile)
    }
}
