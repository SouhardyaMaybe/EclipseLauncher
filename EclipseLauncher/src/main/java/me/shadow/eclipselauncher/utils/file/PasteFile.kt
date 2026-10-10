package me.shadow.eclipselauncher.utils.file

import android.app.Activity
import android.widget.Toast

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.task.Task

import java.io.File

class PasteFile private constructor() {

    private val copyFiles = ArrayList<File>()
    private var mRoot: File? = null

    var pasteType: PasteType? = null
        private set

    fun setCopyFiles(files: List<File>) {
        this.copyFiles.clear()
        this.copyFiles.addAll(files)
    }

    fun setPaste(root: File, files: List<File>, type: PasteType?) {
        setCopyFiles(files)
        this.mRoot = root
        this.pasteType = type
    }

    fun pasteFiles(
        activity: Activity,
        target: File,
        fileExtensionGetter: FileCopyHandler.FileExtensionGetter?,
        endTask: Task<*>
    ) {
        if (copyFiles.isEmpty()) {
            // The toast was posted to the UI thread before; Dispatchers.Main does the same.
            pasteScope.launch {
                Toast.makeText(activity, activity.getString(R.string.file_does_not_exist), Toast.LENGTH_SHORT).show()
            }
            return
        }
        FileCopyHandler(
            activity, pasteType, copyFiles, mRoot, target, fileExtensionGetter, endTask.beforeStart(this::resetState)
        ).start()
    }

    private fun resetState() {
        pasteType = null
        copyFiles.clear()
    }

    enum class PasteType {
        COPY, MOVE
    }

    companion object {
        private val pasteScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName("PasteFile"))

        private val instance = PasteFile()

        @JvmStatic
        fun getInstance(): PasteFile {
            return instance
        }
    }
}
