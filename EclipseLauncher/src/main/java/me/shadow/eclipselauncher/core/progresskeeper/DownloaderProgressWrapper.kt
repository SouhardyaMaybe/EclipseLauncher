package me.shadow.eclipselauncher.core.progresskeeper

import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.Tools.BYTE_TO_MB
import me.shadow.eclipselauncher.core.tasks.SpeedCalculator
import me.shadow.eclipselauncher.utils.EclipseTools
import me.shadow.eclipselauncher.utils.file.FileTools

/**
 * A simple wrapper to send the downloader progress to ProgressKeeper
 * @param progressString the string that will be used in the progress reporter
 * @param progressRecord the record for ProgressKeeper
 */
class DownloaderProgressWrapper(
    private val mProgressString: Int,
    private val mProgressRecord: String
) : Tools.DownloaderFeedback {
    private val mSpeedCalculator = SpeedCalculator(128)
    private var progressUpdateTime = 0L

    override fun updateProgress(curr: Long, max: Long) {
        val currentTime = EclipseTools.getCurrentTimeMillis()
        // Throttle the updates to at most one every 150ms, like before
        if (currentTime - progressUpdateTime < 150) return
        progressUpdateTime = currentTime

        val va = arrayOfNulls<Any>(3)
        va[0] = curr / BYTE_TO_MB
        va[1] = max / BYTE_TO_MB
        va[2] = FileTools.formatFileSize(mSpeedCalculator.feed(curr))
        // the allocations are fine because thats how java implements variadic arguments in bytecode: an array of whatever
        ProgressKeeper.submitProgress(
            mProgressRecord,
            maxOf(curr.toFloat() / max * 100f, 0f).toInt(),
            mProgressString,
            *va
        )
    }
}
