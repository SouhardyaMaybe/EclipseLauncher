package me.shadow.eclipselauncher.utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.utils.file.FileTools
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.io.FileUtils
import java.io.File

class CleanUpCache {
    companion object {
        private var isCleaning = false

        private val cleanUpScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("CleanUpCache"))

        @JvmStatic
        fun start(context: Context) {
            if (isCleaning) return
            isCleaning = true

            var totalSize: Long = 0
            var fileCount = 0
            try {
                cleanUpScope.launch {
                    val list = PathManager.DIR_CACHE.listFiles()?.let {
                        PathManager.DIR_APP_CACHE.listFiles()?.let { it1 ->
                            getList(it, it1)
                        }
                    }

                    PathManager.FILE_VERSION_LIST.let {
                        val file = File(it)
                        if (file.exists()) list?.add(file)
                    }

                    list?.let {
                        for (file in list) {
                            ++fileCount
                            totalSize += FileUtils.sizeOf(file)
                            FileUtils.deleteQuietly(file)
                        }
                    }

                    // The toast was posted to the UI thread through TaskExecutors before
                    withContext(Dispatchers.Main) {
                        if (fileCount != 0) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.clear_up_cache_clean_up, FileTools.formatFileSize(totalSize)),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.clear_up_cache_not_found),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } finally {
                isCleaning = false
            }
        }

        private fun getList(vararg filesArray: Array<File>): MutableList<File> {
            val filesList: MutableList<File> = ArrayList()
            for (fileArray in filesArray) {
                filesList.addAll(listOf(*fileArray))
            }

            return filesList
        }
    }
}
