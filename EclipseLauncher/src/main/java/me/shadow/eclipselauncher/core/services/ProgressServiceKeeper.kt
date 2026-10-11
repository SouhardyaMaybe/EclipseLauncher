package me.shadow.eclipselauncher.core.services

import android.content.Context

import me.shadow.eclipselauncher.core.progresskeeper.TaskCountListener

class ProgressServiceKeeper(private val context: Context) : TaskCountListener {
    override fun onUpdateTaskCount(taskCount: Int) {
        if (taskCount > 0) ProgressService.startService(context)
    }
}
