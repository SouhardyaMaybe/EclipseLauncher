package me.shadow.eclipselauncher.core.tasks

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.core.JAssetInfo
import me.shadow.eclipselauncher.core.JAssets
import me.shadow.eclipselauncher.core.JMinecraftVersionList
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.utils.DownloadUtils
import me.shadow.eclipselauncher.core.utils.FileUtils
import me.shadow.eclipselauncher.core.value.DependentLibrary
import me.shadow.eclipselauncher.core.value.MinecraftClientInfo
import me.shadow.eclipselauncher.feature.customprofilepath.ProfilePathHome
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.mcgui.ProgressLayout
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.utils.path.PathManager
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

/**
 * Downloads a Minecraft client version: metadata first (single-threaded), then every scheduled
 * file download on a bounded set of worker coroutines that pull tasks from a FIFO channel -
 * the exact ordering and concurrency the old ThreadPoolExecutor setup produced.
 */
class MinecraftDownloader {
    companion object {
        private const val ONE_MEGABYTE = 1024.0 * 1024.0
        const val MINECRAFT_RES = "https://resources.download.minecraft.net/"

        // Download class markers: which kind of file a scheduled download refers to
        const val DOWNLOAD_CLASS_LIBRARIES = 0
        const val DOWNLOAD_CLASS_METADATA = 1
        const val DOWNLOAD_CLASS_ASSETS = 2

        private const val MAVEN_CENTRAL_REPO1 = "https://repo1.maven.org/maven2/"

        /** Progress cadence of the old polling loop: it waited 33ms per iteration. */
        private const val PROGRESS_INTERVAL_MS = 33L

        private val sThreadLocalDownloadBuffer = ThreadLocal<ByteArray>()

        private fun getLocalBuffer(): ByteArray {
            sThreadLocalDownloadBuffer.get()?.let { return it }
            val buffer = ByteArray(32768)
            sThreadLocalDownloadBuffer.set(buffer)
            return buffer
        }
    }

    private lateinit var mDownloaderThreadException: AtomicReference<Exception?>
    private lateinit var mScheduledDownloadTasks: ArrayList<DownloaderTask>
    private lateinit var mDeclaredNatives: ArrayList<File>
    private lateinit var mProcessedFileCounter: AtomicLong
    private lateinit var mProcessedSizeCounter: AtomicLong // Total bytes of processed files (passed SHA1 or downloaded)
    private lateinit var mInternetUsageCounter: AtomicLong // How many bytes downloaded over Internet
    private var mTotalFileCount = 0L
    private var mTotalSize = 0L
    private var mSourceJarFile: File? = null // The source client JAR picked during the inheritance process
    private lateinit var mTargetJarFile: File // The destination client JAR to which the source will be copied to.
    private var mUseFileCounter = false // Whether a file counter or a size counter should be used for progress

    /**
     * Start the game version download process inside the caller's coroutine.
     * The caller is responsible for launching this as a coroutine and for mapping the thrown
     * failure to whatever UI reaction the old completion listener had.
     *
     * @param version The JMinecraftVersionList.Version from the version list, if available
     * @param realVersion The version ID (necessary) // this was there for a reason
     * @throws Exception when an exception occurs in the function body or in any of the download workers.
     */
    suspend fun start(version: JMinecraftVersionList.Version?, realVersion: String) {
        try {
            downloadGame(version, realVersion)
        } finally {
            ProgressLayout.clearProgress(ProgressLayout.DOWNLOAD_MINECRAFT)
        }
    }

    /**
     * Download the game version.
     * @param verInfo The JMinecraftVersionList.Version from the version list, if available
     * @param versionName The version ID (necessary)
     * @throws Exception when an exception occurs in the function body or in any of the download workers.
     */
    private suspend fun downloadGame(verInfo: JMinecraftVersionList.Version?, versionName: String) {
        // Put up a dummy progress line, for the activity to start the service and do all the other necessary
        // work to keep the launcher alive. We will replace this line when we will start downloading stuff.
        ProgressLayout.setProgress(ProgressLayout.DOWNLOAD_MINECRAFT, 0, R.string.newdl_starting)
        val speedCalculator = SpeedCalculator()

        mTargetJarFile = createGameJarPath(versionName)
        mScheduledDownloadTasks = ArrayList()
        mDeclaredNatives = ArrayList()
        mProcessedFileCounter = AtomicLong(0)
        mProcessedSizeCounter = AtomicLong(0)
        mInternetUsageCounter = AtomicLong(0)
        mDownloaderThreadException = AtomicReference(null)
        mUseFileCounter = false

        downloadAndProcessMetadata(verInfo, versionName)

        // The tasks are pushed into the FIFO channel up front (like the old pre-fill loop), then
        // a fixed number of workers pull them one by one, preserving the original submission order.
        val (workerScope, workers) = startDownloadWorkers()

        try {
            while (true) {
                if (mDownloaderThreadException.get() != null) break
                // Wait up to 33ms for all workers to finish, like awaitTermination(33ms) did
                if (withTimeoutOrNull(PROGRESS_INTERVAL_MS) { joinAll(workers) } != null) break

                val speed = speedCalculator.feed(mInternetUsageCounter.get()) / ONE_MEGABYTE
                if (mUseFileCounter) reportProgressFileCounter(speed)
                else reportProgressSizeCounter(speed)
            }
            val thrownException = mDownloaderThreadException.get()
            if (thrownException != null) {
                throw thrownException
            } else {
                ensureJarFileCopy()
                extractNatives(versionName)
            }
        } catch (cancellation: CancellationException) {
            // Equivalent of the original InterruptedException handling: the waiting thread was
            // interrupted, which means the download was cancelled. Kill all downloading workers
            // immediately and ignore any exceptions thrown by them. Like before, this returns
            // normally - the JAR copy and the native extraction are skipped.
            workerScope.cancel(cancellation)
        }
    }

    /**
     * Schedules every scheduled download onto a bounded worker pool and returns the scope holding
     * the workers together with their jobs.
     *
     * The sizing logic reproduces the original ThreadPoolExecutor setup down to its constructor
     * checks: ArrayBlockingQueue(scheduledCount) rejected an empty queue and the executor rejected
     * a non-positive maximum pool size (both IllegalArgumentException), and because the queue was
     * as large as the task list, the pool never grew past its core size:
     * max(1, min(maxDownloadThreads, taskCount) / 2) workers.
     */
    private fun startDownloadWorkers(): Pair<CoroutineScope, List<Job>> {
        val scheduledCount = mScheduledDownloadTasks.size
        if (scheduledCount <= 0) throw IllegalArgumentException()
        var maxThreads = AllSettings.maxDownloadThreads.getValue()
        if (scheduledCount <= maxThreads) maxThreads = scheduledCount
        val corePoolSize = max(1, maxThreads / 2)
        if (maxThreads <= 0 || maxThreads < corePoolSize) throw IllegalArgumentException()

        val taskQueue = Channel<DownloaderTask>(Channel.UNLIMITED)
        for (scheduledTask in mScheduledDownloadTasks) taskQueue.trySend(scheduledTask)
        taskQueue.close()

        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("MinecraftDownloader"))
        val workers = (1..corePoolSize).map {
            workerScope.launch {
                for (task in taskQueue) {
                    try {
                        // Maps coroutine cancellation onto the old Thread.interrupt() of a worker
                        // thread, so a blocking download stops at the same point it used to.
                        runInterruptible { task.execute() }
                    } catch (ignored: Exception) {
                        // Cancellation of this worker: on the interrupt path the original code
                        // ignored every exception thrown by its downloading threads as well.
                    } catch (ignored: Throwable) {
                        // The old thread pool silently recycled the worker thread after an Error
                        // without recording it; do the same instead of failing the scope.
                    }
                    // A cancelled worker (the shutdownNow() equivalent) must not pull another
                    // task off the queue; a worker of a live pool keeps draining it.
                    ensureActive()
                }
            }
        }
        return workerScope to workers
    }

    private fun reportProgressFileCounter(speed: Double) {
        val dlFileCounter = mProcessedFileCounter.get()
        val progress = ((dlFileCounter * 100L) / mTotalFileCount).toInt()
        ProgressLayout.setProgress(
            ProgressLayout.DOWNLOAD_MINECRAFT, progress,
            R.string.newdl_downloading_game_files, dlFileCounter,
            mTotalFileCount, speed
        )
    }

    private fun reportProgressSizeCounter(speed: Double) {
        val dlFileSize = mProcessedSizeCounter.get()
        val dlSizeMegabytes = dlFileSize.toDouble() / ONE_MEGABYTE
        val dlTotalMegabytes = mTotalSize.toDouble() / ONE_MEGABYTE
        val progress = ((dlFileSize * 100L) / mTotalSize).toInt()
        ProgressLayout.setProgress(
            ProgressLayout.DOWNLOAD_MINECRAFT, progress,
            R.string.newdl_downloading_game_files_size, dlSizeMegabytes, dlTotalMegabytes, speed
        )
    }

    private fun createGameJsonPath(versionId: String): File =
        File(ProfilePathHome.getVersionsHome(), versionId + File.separator + versionId + ".json")

    private fun createGameJarPath(versionId: String): File =
        File(ProfilePathHome.getVersionsHome(), versionId + File.separator + versionId + ".jar")

    /**
     * Ensure that there is a copy of the client JAR file in the version folder, if a copy is
     * needed.
     * @throws IOException if the copy fails
     */
    private fun ensureJarFileCopy() {
        val sourceJarFile = mSourceJarFile ?: return
        if (sourceJarFile == mTargetJarFile) return
        if (mTargetJarFile.exists()) return
        FileUtils.ensureParentDirectory(mTargetJarFile)
        Logging.i("NewMCDownloader", "Copying " + sourceJarFile.name + " to " + mTargetJarFile.absolutePath)
        me.shadow.eclipselauncher.utils.io.FileUtils.copyFile(sourceJarFile, mTargetJarFile, false)
    }

    private fun extractNatives(versionName: String) {
        if (mDeclaredNatives.isEmpty()) return
        val totalCount = mDeclaredNatives.size

        ProgressLayout.setProgress(
            ProgressLayout.DOWNLOAD_MINECRAFT, 0,
            R.string.newdl_extracting_native_libraries, 0, totalCount
        )

        val targetDirectory = File(PathManager.DIR_CACHE, "natives/" + versionName)
        FileUtils.ensureDirectory(targetDirectory)
        val nativesExtractor = NativesExtractor(targetDirectory)
        var extractedCount = 0
        for (source in mDeclaredNatives) {
            nativesExtractor.extractFromAar(source)
            extractedCount++
            ProgressLayout.setProgress(
                ProgressLayout.DOWNLOAD_MINECRAFT, extractedCount * 100 / totalCount,
                R.string.newdl_extracting_native_libraries, extractedCount, totalCount
            )
        }
    }

    private fun downloadGameJson(verInfo: JMinecraftVersionList.Version): File {
        val targetFile = createGameJsonPath(verInfo.id)
        if (verInfo.sha1 == null && targetFile.canRead() && targetFile.isFile) return targetFile
        FileUtils.ensureParentDirectory(targetFile)
        DownloadUtils.ensureSha1<File?>(
            targetFile,
            if (AllSettings.verifyManifest.getValue()) verInfo.sha1 else null
        ) {
            ProgressLayout.setProgress(
                ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                R.string.newdl_downloading_metadata, targetFile.name
            )
            DownloadUtils.downloadFile(verInfo.url, targetFile)
            null
        }
        return targetFile
    }

    private fun downloadAssetsIndex(verInfo: JMinecraftVersionList.Version): JAssets? {
        val assetIndex = verInfo.assetIndex
        if (assetIndex == null || verInfo.assets == null) return null
        val targetFile =
            File(ProfilePathHome.getAssetsHome(), "indexes" + File.separator + verInfo.assets + ".json")
        FileUtils.ensureParentDirectory(targetFile)
        DownloadUtils.ensureSha1<File?>(targetFile, assetIndex.sha1) {
            ProgressLayout.setProgress(
                ProgressLayout.DOWNLOAD_MINECRAFT, 0,
                R.string.newdl_downloading_metadata, targetFile.name
            )
            DownloadUtils.downloadFile(assetIndex.url, targetFile)
            null
        }
        return Tools.JSON_CODEC.fromJson(Tools.read(targetFile), JAssets::class.java)
    }

    private fun getClientInfo(verInfo: JMinecraftVersionList.Version): MinecraftClientInfo? {
        val downloads: MutableMap<String, MinecraftClientInfo>? = verInfo.downloads
        if (downloads == null) return null
        return downloads["client"]
    }

    /**
     * Download (if necessary) and process a version's metadata, scheduling all downloads that this
     * version needs.
     * @param verInfoInput The JMinecraftVersionList.Version from the version list, if available
     * @param versionName The version ID (necessary)
     * @throws IOException if the download of any of the metadata files fails
     */
    private fun downloadAndProcessMetadata(verInfoInput: JMinecraftVersionList.Version?, versionName: String) {
        val versionJsonFile = if (verInfoInput != null) downloadGameJson(verInfoInput) else createGameJsonPath(versionName)
        val verInfo: JMinecraftVersionList.Version
        if (versionJsonFile.canRead()) {
            verInfo = Tools.JSON_CODEC.fromJson(Tools.read(versionJsonFile), JMinecraftVersionList.Version::class.java)
        } else {
            throw IOException("Unable to read Version JSON for version " + versionName)
        }

        val assets = downloadAssetsIndex(verInfo)
        if (assets != null) scheduleAssetDownloads(assets)

        val minecraftClientInfo = getClientInfo(verInfo)
        if (minecraftClientInfo != null) scheduleGameJarDownload(minecraftClientInfo, versionName)

        if (verInfo.libraries != null) scheduleLibraryDownloads(verInfo.libraries)

        if (Tools.isValidString(verInfo.inheritsFrom)) {
            val inheritedVersion = AsyncMinecraftDownloader.getListedVersion(verInfo.inheritsFrom)
            // Infinite inheritance !?! :noway:
            downloadAndProcessMetadata(inheritedVersion, verInfo.inheritsFrom)
        }
    }

    private fun growDownloadList(addedElementCount: Int) {
        mScheduledDownloadTasks.ensureCapacity(mScheduledDownloadTasks.size + addedElementCount)
    }

    private fun scheduleDownload(
        targetFile: File, downloadClass: Int, url: String?, sha1: String?,
        size: Long, skipIfFailed: Boolean
    ) {
        FileUtils.ensureParentDirectory(targetFile)
        mTotalFileCount++
        var downloadSize = size
        // Only attempt to check size if we still use the size counter and didn't switch to file counter.
        if (downloadSize <= 0 && !mUseFileCounter) {
            try {
                downloadSize = DownloadUtils.getContentLength(url)
            } catch (e: Exception) {
                downloadSize = -1
            }
        }
        if (downloadSize < 0) {
            // If we were unable to get the content length ourselves, we automatically fall back
            // to tracking the progress using the file counter.
            downloadSize = 0
            mUseFileCounter = true
            Logging.i(
                "MinecraftDownloader",
                "Failed to determine size of " + targetFile.name + ", switching to file counter"
            )
        } else {
            mTotalSize += downloadSize
        }
        mScheduledDownloadTasks.add(
            DownloaderTask(targetFile, downloadClass, url, sha1, downloadSize, skipIfFailed)
        )
    }

    /**
     * Schedule the download of an AAR library containing the required natives, for later extraction
     * and adding to the library path.
     * @param baseRepository the source Maven repository to download from.
     * @param dependentLibrary the DependentLibrary to get the path from
     */
    private fun scheduleNativeLibraryDownload(baseRepository: String, dependentLibrary: DependentLibrary) {
        val libArtifactPath = Tools.artifactToPath(dependentLibrary) ?: return
        val path = FileUtils.removeExtension(libArtifactPath) + ".aar"
        val downloadUrl = baseRepository + path
        val targetPath = File(ProfilePathHome.getLibrariesHome(), path)
        mDeclaredNatives.add(targetPath)
        scheduleDownload(targetPath, DOWNLOAD_CLASS_LIBRARIES, downloadUrl, null, 0, true)
    }

    private fun scheduleLibraryDownloads(dependentLibraries: Array<DependentLibrary>) {
        Tools.preProcessLibraries(dependentLibraries)
        growDownloadList(dependentLibraries.size)
        for (dependentLibrary in dependentLibraries) {
            // Don't download lwjgl, we have our own bundled in.
            if (dependentLibrary.name.startsWith("org.lwjgl")) continue
            // Special handling for JNA Android natives
            if (dependentLibrary.name.startsWith("net.java.dev.jna:jna:")) {
                scheduleNativeLibraryDownload(MAVEN_CENTRAL_REPO1, dependentLibrary)
            }

            val libArtifactPath = Tools.artifactToPath(dependentLibrary) ?: continue

            var sha1: String? = null
            var url: String? = null
            var size = 0L
            var skipIfFailed = false
            if (dependentLibrary.downloads != null) {
                val artifact = dependentLibrary.downloads!!.artifact
                if (artifact != null) {
                    sha1 = artifact.sha1
                    url = artifact.url
                    size = artifact.size.toLong()
                } else {
                    // If the library has a downloads section but doesn't have an artifact in
                    // it, it is likely natives-only, which means it can be skipped.
                    Logging.i(
                        "NewMCDownloader",
                        "Skipped library " + dependentLibrary.name + " due to lack of artifact"
                    )
                    continue
                }
            }
            if (url == null) {
                val declaredUrl = dependentLibrary.url
                url = (if (declaredUrl == null) "https://libraries.minecraft.net/"
                else declaredUrl.replace("http://", "https://")) + libArtifactPath
                skipIfFailed = true
            }
            if (!AllSettings.checkLibraries.getValue()) sha1 = null
            scheduleDownload(
                File(ProfilePathHome.getLibrariesHome(), libArtifactPath),
                DOWNLOAD_CLASS_LIBRARIES,
                url, sha1, size, skipIfFailed
            )
        }
    }

    private fun scheduleAssetDownloads(assets: JAssets) {
        val assetObjects: MutableMap<String, JAssetInfo>? = assets.objects
        if (assetObjects == null) return
        val assetNames = assetObjects.keys
        growDownloadList(assetNames.size)
        for (asset in assetNames) {
            val assetInfo = assetObjects[asset] ?: continue
            val hashedPath = assetInfo.hash.substring(0, 2) + File.separator + assetInfo.hash
            val basePath =
                if (assets.mapToResources) ProfilePathHome.getResourcesHome() else ProfilePathHome.getAssetsHome()
            val targetFile = if (assets.virtual || assets.mapToResources) {
                File(basePath, asset)
            } else {
                File(basePath, "objects" + File.separator + hashedPath)
            }
            val sha1 = if (AllSettings.checkLibraries.getValue()) assetInfo.hash else null
            scheduleDownload(
                targetFile,
                DOWNLOAD_CLASS_ASSETS,
                MINECRAFT_RES + hashedPath,
                sha1,
                assetInfo.size.toLong(),
                false
            )
        }
    }

    private fun scheduleGameJarDownload(minecraftClientInfo: MinecraftClientInfo, versionName: String) {
        val clientJar = createGameJarPath(versionName)
        val clientSha1 = if (AllSettings.checkLibraries.getValue()) minecraftClientInfo.sha1 else null
        growDownloadList(1)
        scheduleDownload(
            clientJar,
            DOWNLOAD_CLASS_LIBRARIES,
            minecraftClientInfo.url,
            clientSha1,
            minecraftClientInfo.size.toLong(),
            false
        )
        // Store the path of the JAR to copy it into our new version folder later.
        mSourceJarFile = clientJar
    }

    /**
     * One file download; executed by exactly one worker coroutine at a time, so its mutable
     * per-task fields need no synchronization - same as when it was a pool Runnable.
     */
    private inner class DownloaderTask(
        private val mTargetPath: File,
        private val mDownloadClass: Int,
        private val mTargetUrl: String?,
        targetSha1: String?,
        private val mDownloadSize: Long,
        private val mSkipIfFailed: Boolean
    ) : Tools.DownloaderFeedback {
        private var mTargetSha1: String? = targetSha1
        private var mLastCurr = 0L

        private fun downloadSha1(): String? {
            var downloadedHash = DownloadUtils.downloadString(mTargetUrl + ".sha1")
            if (!Tools.isValidString(downloadedHash)) return null
            // Ensure that we don't have leading/trailing whitespaces before checking hash length
            downloadedHash = downloadedHash.trim()
            // SHA1 is made up of 20 bytes, which means 40 hexadecimal digits, which means 40 chars
            if (downloadedHash.length != 40) return null
            return downloadedHash
        }

        /*
         * Maven repositories usually have the hash of a library near it, like:
         * .../libraryName-1.0.jar
         * .../libraryName.1.0.jar.sha1
         * Since Minecraft libraries are stored in maven repositories, try to use
         * this when downloading libraries without hashes in the json.
         */
        private fun tryGetLibrarySha1() {
            var resultHash: String? = null
            try {
                resultHash = downloadSha1()
                // The hash is a 40-byte download.
                mInternetUsageCounter.getAndAdd(40)
            } catch (e: IOException) {
                Logging.i("MinecraftDownloader", "Failed to download hash", e)
            }
            if (resultHash != null) {
                Logging.i(
                    "MinecraftDownloader",
                    "Got hash: " + resultHash + " for " + FileUtils.getFileName(mTargetUrl)
                )
                mTargetSha1 = resultHash
            }
        }

        fun execute() {
            try {
                executeInternal()
            } catch (e: Exception) {
                mDownloaderThreadException.set(e)
            }
        }

        private fun executeInternal() {
            if (mDownloadClass == DOWNLOAD_CLASS_LIBRARIES && !Tools.isValidString(mTargetSha1)) {
                // If we're downloading a library, try to get sha1 since it might be available as a file
                tryGetLibrarySha1()
            }
            if (Tools.isValidString(mTargetSha1)) {
                verifyFileSha1()
            } else {
                // Nullify SHA1 as DownloadUtils.ensureSha1 only checks for null,
                // not for string validity
                mTargetSha1 = null
                if (mTargetPath.exists()) finishWithoutDownloading()
                else downloadFile()
            }
        }

        private fun verifyFileSha1() {
            if (mTargetPath.isFile && mTargetPath.canRead() && Tools.compareSHA1(mTargetPath, mTargetSha1)) {
                finishWithoutDownloading()
            } else {
                // Rely on the download function to throw an IOE in case if the file is not
                // writable/not a file/etc...
                downloadFile()
            }
        }

        private fun downloadFile() {
            try {
                DownloadUtils.ensureSha1<File?>(mTargetPath, mTargetSha1) {
                    DownloadUtils.downloadFileMonitored(mTargetUrl, mTargetPath, getLocalBuffer(), this@DownloaderTask)
                    null
                }
            } catch (e: Exception) {
                if (!mSkipIfFailed) throw e
            }
            mProcessedFileCounter.incrementAndGet()
        }

        private fun finishWithoutDownloading() {
            mProcessedFileCounter.incrementAndGet()
            mProcessedSizeCounter.addAndGet(mDownloadSize)
        }

        override fun updateProgress(curr: Long, max: Long) {
            val delta = curr - mLastCurr
            mProcessedSizeCounter.addAndGet(delta)
            mInternetUsageCounter.addAndGet(delta)
            mLastCurr = curr
        }
    }
}
