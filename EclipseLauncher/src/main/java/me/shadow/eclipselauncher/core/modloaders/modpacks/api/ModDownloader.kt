package me.shadow.eclipselauncher.core.modloaders.modpacks.api

import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.setting.AllSettings

import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.utils.DownloadUtils

import okhttp3.internal.notify
import okhttp3.internal.wait

import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ModDownloader(destinationDirectory: File, useFileCount: Boolean) {
    private val mDownloadPool: ThreadPoolExecutor
    private val mTerminator = AtomicBoolean(false)
    private val mDownloadProgress = AtomicInteger(0)
    private val mDownloadedSize = AtomicLong(0)
    private val mExceptionSyncPoint = Any()
    private val mDestinationDirectory: File = destinationDirectory
    private val mUseFileCount: Boolean = useFileCount
    private var mFirstIOException: IOException? = null
    private var mTotalSize = 0L

    constructor(destinationDirectory: File) : this(destinationDirectory, false)

    init {
        val maxThreads = AllSettings.maxDownloadThreads.getValue()
        mDownloadPool = ThreadPoolExecutor(
            Math.max(1, maxThreads / 2),
            maxThreads,
            100,
            TimeUnit.MILLISECONDS,
            LinkedBlockingQueue()
        )

        mDownloadPool.setRejectedExecutionHandler(ThreadPoolExecutor.DiscardPolicy())
    }

    fun submitDownload(fileSize: Int, relativePath: String, downloadHash: String?, vararg url: String) {
        if (mUseFileCount) mTotalSize += 1
        else mTotalSize += fileSize
        mDownloadPool.execute(DownloadTask(url, File(mDestinationDirectory, relativePath), downloadHash))
    }

    fun submitDownload(infoProvider: FileInfoProvider) {
        if (!mUseFileCount) throw RuntimeException("This method can only be used in a file-counting ModDownloader")
        mTotalSize += 1
        mDownloadPool.execute(FileInfoQueryTask(infoProvider))
    }

    @Throws(IOException::class)
    fun awaitFinish(feedback: Tools.DownloaderFeedback) {
        awaitFinish { feedback.updateProgress(mDownloadedSize.get(), mTotalSize) }
    }

    @Throws(IOException::class)
    fun awaitFinish(listener: DownloadProgressListener) {
        awaitFinish { listener.feedback(mDownloadProgress.get(), mTotalSize.toInt(), mDownloadedSize.get()) }
    }

    @Throws(IOException::class)
    private fun awaitFinish(listener: OnFileDownloadedListener) {
        try {
            mDownloadPool.shutdown()
            while (!mDownloadPool.awaitTermination(20, TimeUnit.MILLISECONDS) && !mTerminator.get()) {
                listener.downloaded()
            }
            if (mTerminator.get()) {
                mDownloadPool.shutdownNow()
                synchronized(mExceptionSyncPoint) {
                    if (mFirstIOException == null) mExceptionSyncPoint.wait()
                    throw mFirstIOException!!
                }
            }
        } catch (e: InterruptedException) {
            Logging.e("ModDownloader", Tools.printToString(e))
        }
    }

    private fun downloadFailed(exception: IOException) {
        mTerminator.set(true)
        synchronized(mExceptionSyncPoint) {
            if (mFirstIOException == null) {
                mFirstIOException = exception
                mExceptionSyncPoint.notify()
            }
        }
    }

    inner class FileInfoQueryTask(private val mFileInfoProvider: FileInfoProvider) : Runnable {
        override fun run() {
            try {
                val fileInfo = mFileInfoProvider.getFileInfo() ?: return
                DownloadTask(arrayOf(fileInfo.url), File(mDestinationDirectory, fileInfo.relativePath), fileInfo.sha1).run()
            } catch (e: IOException) {
                downloadFailed(e)
            }
        }
    }

    inner class DownloadTask(
        private val mDownloadUrls: Array<out String>,
        private val mDestination: File,
        private val mSha1: String?
    ) : Runnable, Tools.DownloaderFeedback {
        private var last = 0L

        override fun run() {
            for (sourceUrl in mDownloadUrls) {
                try {
                    DownloadUtils.ensureSha1(mDestination, mSha1, DownloadUtils.DownloadFunction<Void?> {
                        val exception = tryDownload(sourceUrl)
                        if (exception != null) {
                            throw exception
                        }
                        null
                    })
                } catch (e: IOException) {
                    downloadFailed(e)
                }
            }
        }

        @Throws(InterruptedIOException::class)
        private fun tryDownload(sourceUrl: String): IOException? {
            var exception: IOException? = null
            for (i in 0 until 5) {
                try {
                    DownloadUtils.downloadFileMonitored(sourceUrl, mDestination, getThreadLocalBuffer(), this)
                    if (mUseFileCount) mDownloadProgress.addAndGet(1)
                    return null
                } catch (e: InterruptedIOException) {
                    throw e
                } catch (e: IOException) {
                    Logging.e("ModDownloader", Tools.printToString(e))
                    exception = e
                }
                mDownloadedSize.addAndGet(-last)
                last = 0
            }
            return exception
        }

        override fun updateProgress(curr: Long, max: Long) {
            val size = curr - last
            mDownloadedSize.addAndGet(size)
            last = curr
        }
    }

    class FileInfo(
        val url: String,
        val relativePath: String,
        val sha1: String?
    )

    fun interface FileInfoProvider {
        @Throws(IOException::class)
        fun getFileInfo(): FileInfo?
    }

    private fun interface OnFileDownloadedListener {
        fun downloaded()
    }

    /**
     * A listener for the number of downloaded files and the total size of the downloaded files
     */
    fun interface DownloadProgressListener {
        fun feedback(downloadedCount: Int, totalCount: Int, downloadedSize: Long)
    }

    companion object {
        private val sThreadLocalBuffer = ThreadLocal<ByteArray>()

        private fun getThreadLocalBuffer(): ByteArray {
            var buffer = sThreadLocalBuffer.get()
            if (buffer != null) return buffer
            buffer = ByteArray(8192)
            sThreadLocalBuffer.set(buffer)
            return buffer
        }
    }
}
