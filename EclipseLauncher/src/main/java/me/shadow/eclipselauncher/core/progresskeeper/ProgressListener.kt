package me.shadow.eclipselauncher.core.progresskeeper

interface ProgressListener {
    fun onProgressStarted()
    fun onProgressUpdated(progress: Int, resid: Int, vararg va: Any?)
    fun onProgressEnded()
}
