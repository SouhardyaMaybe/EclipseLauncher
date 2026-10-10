package me.shadow.eclipselauncher.utils.file

import android.content.Context
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.ui.dialog.ProgressDialog
import me.shadow.eclipselauncher.utils.EclipseTools

abstract class FileHandler(
    protected val context: Context,
) {
    protected var currentTask: Job? = null
    private var timerJob: Job? = null
    private var lastSize: Long = 0
    private var lastTime: Long = EclipseTools.getCurrentTimeMillis()

    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName("FileHandler"))
    private val workScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("FileHandler-work"))

    protected fun start(progress: FileSearchProgress) {
        uiScope.launch {
            val dialog = ProgressDialog(context) {
                cancelTask()
                onEnd()
                true
            }
            dialog.updateText(context.getString(R.string.file_operation_file, "0 B", "0 B", 0))

            currentTask = workScope.launch {
                uiScope.launch { dialog.show() }

                // Rate meter: the old java.util.Timer ticked every 100ms, this loop does the same
                timerJob = workScope.launch {
                    while (isActive) {
                        val pendingSize = progress.getPendingSize()
                        val totalSize = progress.getTotalSize()
                        val processedSize = totalSize - pendingSize

                        val currentTime = EclipseTools.getCurrentTimeMillis()
                        val timeElapsed = (currentTime - lastTime) / 1000.0
                        val sizeChange = processedSize - lastSize
                        val rate = (if (timeElapsed > 0) sizeChange / timeElapsed else 0.0).toLong()

                        lastSize = processedSize
                        lastTime = currentTime

                        uiScope.launch {
                            dialog.updateText(
                                context.getString(
                                    R.string.file_operation_file,
                                    FileTools.formatFileSize(pendingSize),
                                    FileTools.formatFileSize(totalSize),
                                    progress.getCurrentFileCount()
                                )
                            )
                            dialog.updateRate(rate)
                            dialog.updateProgress(
                                processedSize.toDouble(),
                                totalSize.toDouble()
                            )
                        }
                        delay(100)
                    }
                }

                searchFilesToProcess()
                if (!isActive) return@launch
                processFile()

                uiScope.launch { dialog.dismiss() }
                timerJob?.cancel()
                onEnd()
            }
        }
    }

    abstract fun searchFilesToProcess()

    abstract fun processFile()

    abstract fun onEnd()

    private fun cancelTask() {
        val task = currentTask
        if (task != null && !task.isCompleted) {
            task.cancel()
            timerJob?.cancel()
        }
    }
}
