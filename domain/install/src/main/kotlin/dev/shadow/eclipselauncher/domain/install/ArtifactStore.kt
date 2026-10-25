/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.Failure
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction
import dev.shadow.eclipselauncher.core.network.DownloadSource
import dev.shadow.eclipselauncher.core.network.NetworkException
import dev.shadow.eclipselauncher.domain.mojang.MetadataException
import dev.shadow.eclipselauncher.domain.mojang.VersionResolver
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/** A store failure with a stable code the UI can explain. */
class StoreException(
    val code: String,
    val stage: FailureStage,
    val retryable: Boolean,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/**
 * A content-addressed artifact store rooted at one directory.
 *
 * Every write goes to `<destination>.part`, is hashed while streaming, and is
 * moved into place only after the digest and size match. A crash can leave a
 * `.part`, never a file the verifier would accept as complete.
 *
 * The first downloader is sequential and does not resume a `.part`: an
 * interrupted object is deleted and fetched again. Range resume belongs to the
 * scheduler slice, where the HTTP layer can report the stored offset.
 */
class ArtifactStore(root: Path) {
    private val rootPath = root.toAbsolutePath().normalize()

    /** The normalised store root, used by the collector to bound its walk. */
    val root: Path
        get() = rootPath

    init {
        Files.createDirectories(rootPath)
    }

    fun pathFor(destination: String): Path {
        val safe = try {
            VersionResolver.safeRelativePath(destination)
        } catch (error: MetadataException) {
            throw StoreException(
                code = "STORAGE_PATH_INVALID",
                stage = FailureStage.STORAGE,
                retryable = false,
                message = error.message ?: "unsafe store path",
                cause = error,
            )
        }
        val resolved = rootPath.resolve(safe).normalize()
        if (!resolved.startsWith(rootPath)) {
            throw StoreException(
                code = "STORAGE_PATH_INVALID",
                stage = FailureStage.STORAGE,
                retryable = false,
                message = "store path escapes its root: $destination",
            )
        }
        return resolved
    }

    /** True when the destination exists, has the expected size and digest. */
    fun isVerified(item: DownloadItem): Boolean {
        val file = pathFor(item.destination)
        if (!Files.isRegularFile(file)) return false
        if (Files.size(file) != item.size) return false
        return sha1(file) == item.sha1
    }

    /**
     * Bytes the executor still has to fetch, using existence and size only.
     * This is the input to the space preflight; [isVerified] and the executor
     * still check SHA-1 before an object is accepted. A same-size corrupt file
     * may be counted as present here and downloaded again later, which only
     * makes the reservation slightly optimistic; the 512 MiB headroom covers
     * that case.
     */
    fun missingBytes(items: List<DownloadItem>): AppResult<Long> {
        if (items.any { it.size < 0L }) {
            return AppResult.Error(
                Failure(
                    code = "INSTALL_PLAN_INVALID",
                    stage = FailureStage.STORAGE,
                    retryable = false,
                    userMessage = "The install plan contains a negative file size.",
                    userAction = UserAction.NONE,
                    technicalDetail = "a download item had a negative size",
                ),
            )
        }
        return try {
            var total = 0L
            for (item in items) {
                val file = pathFor(item.destination)
                val present = Files.isRegularFile(file) && Files.size(file) == item.size
                if (!present) total = saturatedAdd(total, item.size)
            }
            AppResult.Success(total)
        } catch (error: StoreException) {
            AppResult.Error(
                Failure(
                    code = error.code,
                    stage = error.stage,
                    retryable = error.retryable,
                    userMessage = error.message ?: "A storage path is not valid.",
                    userAction = UserAction.NONE,
                    technicalDetail = error.message,
                ),
            )
        } catch (error: IOException) {
            AppResult.Error(
                Failure(
                    code = "STORAGE_IO",
                    stage = FailureStage.STORAGE,
                    retryable = true,
                    userMessage = "The launcher could not inspect the download store.",
                    userAction = UserAction.RETRY,
                    technicalDetail = error.message,
                ),
            )
        }
    }

    private fun saturatedAdd(left: Long, right: Long): Long {
        val result = left + right
        return if (result < left) Long.MAX_VALUE else result
    }

    /**
     * Stores one item. Returns the number of bytes read from the source; a
     * verified existing object returns zero and does not touch the network.
     *
     * [isCancelled] is polled before the request and between chunks, so pausing
     * a large client jar does not wait for the whole object. [onBytes] receives
     * the bytes written for this object, throttled to [PROGRESS_STEP].
     */
    @Throws(IOException::class)
    fun store(
        item: DownloadItem,
        source: DownloadSource,
        isCancelled: () -> Boolean = { false },
        onBytes: (Long) -> Unit = {},
    ): Long {
        val destination = pathFor(item.destination)
        if (isVerified(item)) return 0
        if (isCancelled()) throw cancelled()

        try {
            Files.createDirectories(destination.parent)
        } catch (error: IOException) {
            throw storageFailure("the store directory could not be created", error)
        }

        val part = destination.resolveSibling(destination.fileName.toString() + ".part")
        deleteQuietly(part)

        val digest = MessageDigest.getInstance("SHA-1")
        var total = 0L
        var reported = 0L
        try {
            source.open(item.url).use { input ->
                val output = try {
                    Files.newOutputStream(
                        part,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE,
                    )
                } catch (error: IOException) {
                    throw storageFailure("the partial file could not be created", error)
                }

                output.use {
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        if (isCancelled()) throw cancelled()
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        total += read
                        if (total > item.size) {
                            throw StoreException(
                                code = "DOWNLOAD_SIZE_MISMATCH",
                                stage = FailureStage.VERIFY,
                                retryable = true,
                                message = "the download is larger than its manifest entry",
                            )
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                        if (total - reported >= PROGRESS_STEP) {
                            reported = total
                            onBytes(total)
                        }
                    }
                }
            }

            if (total != item.size) {
                throw StoreException(
                    code = "DOWNLOAD_SIZE_MISMATCH",
                    stage = FailureStage.VERIFY,
                    retryable = true,
                    message = "expected ${item.size} bytes, received $total",
                )
            }
            val actual = digest.digest().toHex()
            if (actual != item.sha1) {
                throw StoreException(
                    code = "DOWNLOAD_HASH_MISMATCH",
                    stage = FailureStage.VERIFY,
                    retryable = true,
                    message = "expected SHA-1 ${item.sha1}, received $actual",
                )
            }

            force(part)
            moveIntoPlace(part, destination)
            return total
        } catch (error: StoreException) {
            deleteQuietly(part)
            throw error
        } catch (error: NetworkException) {
            deleteQuietly(part)
            throw error
        } catch (error: IOException) {
            deleteQuietly(part)
            throw StoreException(
                code = "DOWNLOAD_IO",
                stage = FailureStage.DOWNLOAD,
                retryable = true,
                message = error.message ?: "the download could not be read",
                cause = error,
            )
        }
    }

    /** Moves a corrupt object aside so a repair can download a clean copy. */
    @Throws(IOException::class)
    fun quarantine(item: DownloadItem): Path? {
        val destination = pathFor(item.destination)
        if (!Files.exists(destination)) return null
        val bad = destination.resolveSibling(destination.fileName.toString() + ".bad")
        Files.deleteIfExists(bad)
        Files.move(destination, bad, StandardCopyOption.REPLACE_EXISTING)
        return bad
    }

    private fun moveIntoPlace(part: Path, destination: Path) {
        try {
            Files.move(
                part,
                destination,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(part, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /**
     * Flushes the object before the rename. A failed fsync is a storage
     * failure, not a warning: the whole point of the rename is that the bytes
     * behind it are durable.
     */
    private fun force(path: Path) {
        try {
            FileChannel.open(path, StandardOpenOption.WRITE).use { it.force(true) }
        } catch (error: IOException) {
            throw storageFailure("the downloaded object could not be flushed", error)
        }
    }

    private fun sha1(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-1")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun cancelled() = StoreException(
        code = "INSTALL_CANCELLED",
        stage = FailureStage.INSTALL,
        retryable = true,
        message = "The install was paused.",
    )

    private fun storageFailure(message: String, cause: IOException) = StoreException(
        code = "STORAGE_IO",
        stage = FailureStage.STORAGE,
        retryable = true,
        message = "$message: ${cause.message ?: "unknown I/O error"}",
        cause = cause,
    )

    private fun deleteQuietly(path: Path) {
        try {
            Files.deleteIfExists(path)
        } catch (_: IOException) {
            // A leftover .part is never accepted as a complete object.
        }
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val PROGRESS_STEP = 256 * 1024L
    }
}

/**
 * Runs an install plan sequentially. The downloader is deliberately not
 * parallel yet: the store contract and the progress math are easier to verify
 * before adding the six-connection scheduler from doc 07 §5.4.
 */
class InstallExecutor(
    private val store: ArtifactStore,
    private val source: DownloadSource,
) {
    fun execute(
        plan: InstallPlan,
        onProgress: (InstallProgress) -> Unit = {},
        isCancelled: () -> Boolean = { false },
    ): AppResult<Unit> {
        var completedItems = 0
        var completedBytes = 0L
        var downloadedBytes = 0L
        val totalItems = plan.downloads.size

        for ((index, item) in plan.downloads.withIndex()) {
            if (isCancelled()) {
                return failure(
                    code = "INSTALL_CANCELLED",
                    stage = FailureStage.INSTALL,
                    retryable = true,
                    message = "The install was paused before the next file.",
                    userAction = UserAction.NONE,
                )
            }

            val bytes = try {
                store.store(
                    item = item,
                    source = source,
                    isCancelled = isCancelled,
                    onBytes = { written ->
                        onProgress(
                            InstallProgress(
                                completedItems = index,
                                totalItems = totalItems,
                                completedBytes = completedBytes + written,
                                totalBytes = plan.totalBytes,
                                downloadedBytes = downloadedBytes + written,
                                current = item,
                            ),
                        )
                    },
                )
            } catch (error: StoreException) {
                return failure(
                    code = error.code,
                    stage = error.stage,
                    retryable = error.retryable,
                    message = error.message ?: "The file could not be stored.",
                    detail = error.message,
                    userAction = if (error.code == "INSTALL_CANCELLED") {
                        UserAction.NONE
                    } else {
                        UserAction.RETRY
                    },
                )
            } catch (error: NetworkException) {
                return failure(
                    code = error.code,
                    stage = FailureStage.DOWNLOAD,
                    retryable = error.retryable,
                    message = networkMessage(error.code),
                    detail = error.message,
                    userAction = if (error.retryable) UserAction.RETRY else UserAction.NONE,
                )
            } catch (error: IOException) {
                return failure(
                    code = "DOWNLOAD_IO",
                    stage = FailureStage.DOWNLOAD,
                    retryable = true,
                    message = "The file could not be downloaded.",
                    detail = error.message,
                )
            }

            completedItems = index + 1
            completedBytes += item.size
            downloadedBytes += bytes
            onProgress(
                InstallProgress(
                    completedItems = completedItems,
                    totalItems = totalItems,
                    completedBytes = completedBytes,
                    totalBytes = plan.totalBytes,
                    downloadedBytes = downloadedBytes,
                    current = item,
                ),
            )
        }
        return AppResult.Success(Unit)
    }

    private fun networkMessage(code: String): String = when (code) {
        "NET_NOT_FOUND" -> "A game file is no longer available."
        "NET_HOST_NOT_ALLOWED" -> "A game file is hosted on a server the launcher does not trust."
        "NET_TLS" -> "A secure connection to the file server failed."
        "NET_RETRY_EXHAUSTED" -> "The file server is busy. Try again later."
        "NET_URL_INVALID", "NET_REDIRECT_INVALID", "NET_REDIRECT_LIMIT" ->
            "A game file has an invalid download address."
        else -> "The file could not be downloaded."
    }

    private fun failure(
        code: String,
        stage: FailureStage,
        retryable: Boolean,
        message: String,
        detail: String? = null,
        userAction: UserAction = UserAction.RETRY,
    ) = AppResult.Error(
        Failure(
            code = code,
            stage = stage,
            retryable = retryable,
            userMessage = message,
            userAction = userAction,
            technicalDetail = detail,
        ),
    )
}

/**
 * Progress is measured in completed bytes, including objects that were
 * already verified. [downloadedBytes] is the network traffic of this run, so a
 * resumed or repaired install does not claim to have fetched the whole plan.
 */
data class InstallProgress(
    val completedItems: Int,
    val totalItems: Int,
    val completedBytes: Long,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val current: DownloadItem,
) {
    val fraction: Float
        get() = if (totalBytes <= 0L) {
            1f
        } else {
            (completedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        }
}

