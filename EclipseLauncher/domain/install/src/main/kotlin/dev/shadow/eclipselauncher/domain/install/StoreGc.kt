/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.domain.install

import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.EpochClock
import dev.shadow.eclipselauncher.core.common.Failure
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.SystemEpochClock
import dev.shadow.eclipselauncher.core.common.UserAction
import java.io.IOException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlinx.serialization.decodeFromString

/** Why one store path could not be collected. GC continues after a failure. */
enum class GcFailureReason {
    READ,
    DELETE,
    PRUNE,
}

data class GcFailure(
    val path: String,
    val reason: GcFailureReason,
    val detail: String? = null,
)

/**
 * The result of one collection pass. [reclaimedBytes] is the size of files
 * actually deleted, not the size the walk hoped to free.
 */
data class GcReport(
    val scannedFiles: Int = 0,
    val deletedFiles: Int = 0,
    val reclaimedBytes: Long = 0L,
    val keptReferenced: Int = 0,
    val keptYoung: Int = 0,
    val abortedByLock: Boolean = false,
    val failures: List<GcFailure> = emptyList(),
) {
    val healthy: Boolean
        get() = failures.isEmpty() && !abortedByLock
}

/**
 * Refcounted garbage collection for the shared artifact store.
 *
 * The reference set is the union of every committed instance manifest plus
 * whatever the caller pins for an in-flight install or the last known good
 * plan. An object with no reference is kept for [GRACE_MILLIS] so a failed or
 * paused transaction can still be retried cheaply.
 *
 * Safety rules:
 * - Only `versions/`, `libraries/`, `assets/` and `logging/` are walked. The
 *   instance trees, worlds, logs and user files are never candidates.
 * - A manifest that cannot be read aborts the pass. Deleting with an
 *   incomplete reference set would be worse than not collecting at all.
 * - Symbolic links are skipped, never followed and never deleted.
 * - A caller-supplied lock is checked before every deletion. If it is held,
 *   the pass stops and reports [GcReport.abortedByLock].
 */
class StoreGc(
    private val store: ArtifactStore,
    instancesRoot: Path,
    private val clock: EpochClock = SystemEpochClock,
) {
    private val instances = instancesRoot.toAbsolutePath().normalize()

    /** Reads every committed instance manifest into one store-relative path set. */
    fun collectReferences(): AppResult<Set<String>> = try {
        AppResult.Success(collectReferencesOrThrow())
    } catch (error: GcException) {
        AppResult.Error(error.failure)
    }

    /** Collects the references first, then sweeps. The usual entry point. */
    fun run(
        pinned: Set<String> = emptySet(),
        isLocked: () -> Boolean = { false },
    ): AppResult<GcReport> {
        if (isLocked()) return AppResult.Success(GcReport(abortedByLock = true))
        return when (val references = collectReferences()) {
            is AppResult.Success -> sweep(references.value, pinned, isLocked)
            is AppResult.Error -> references
        }
    }

    /**
     * Deletes unreferenced store objects older than the grace period.
     *
     * [references] and [pinned] are store-relative paths. [isLocked] must
     * return true while an install, a launch or a repair owns the store.
     */
    fun sweep(
        references: Set<String>,
        pinned: Set<String> = emptySet(),
        isLocked: () -> Boolean = { false },
        nowEpochMillis: Long = clock.nowEpochMillis(),
    ): AppResult<GcReport> {
        if (isLocked()) return AppResult.Success(GcReport(abortedByLock = true))

        val protectedPaths = try {
            normalise(references + pinned)
        } catch (error: Exception) {
            return AppResult.Error(referencesFailure(error.message ?: "unsafe reference path"))
        }

        var scannedFiles = 0
        var deletedFiles = 0
        var reclaimedBytes = 0L
        var keptReferenced = 0
        var keptYoung = 0
        var abortedByLock = false
        val failures = mutableListOf<GcFailure>()

        for (managed in MANAGED_ROOTS) {
            val managedRoot = store.root.resolve(managed)
            if (!Files.isDirectory(managedRoot) || Files.isSymbolicLink(managedRoot)) continue

            val visitor = object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!attrs.isRegularFile) return FileVisitResult.CONTINUE
                    scannedFiles++

                    if (protectedPaths.contains(file.toString())) {
                        keptReferenced++
                        return FileVisitResult.CONTINUE
                    }

                    val age = nowEpochMillis - attrs.lastModifiedTime().toMillis()
                    if (age < GRACE_MILLIS) {
                        keptYoung++
                        return FileVisitResult.CONTINUE
                    }

                    if (isLocked()) {
                        abortedByLock = true
                        return FileVisitResult.TERMINATE
                    }

                    try {
                        val size = attrs.size()
                        if (Files.deleteIfExists(file)) {
                            deletedFiles++
                            reclaimedBytes += size
                        }
                    } catch (error: IOException) {
                        failures += GcFailure(
                            path = file.toString(),
                            reason = GcFailureReason.DELETE,
                            detail = error.message,
                        )
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, error: IOException?): FileVisitResult {
                    if (error != null) {
                        failures += GcFailure(
                            path = dir.toString(),
                            reason = GcFailureReason.READ,
                            detail = error.message,
                        )
                        return FileVisitResult.CONTINUE
                    }
                    if (dir == managedRoot) return FileVisitResult.CONTINUE

                    if (isLocked()) {
                        abortedByLock = true
                        return FileVisitResult.TERMINATE
                    }

                    try {
                        val empty = Files.newDirectoryStream(dir).use { entries ->
                            !entries.iterator().hasNext()
                        }
                        if (empty) Files.deleteIfExists(dir)
                    } catch (_: DirectoryNotEmptyException) {
                        // A concurrent writer added a file; the directory stays.
                    } catch (error: IOException) {
                        failures += GcFailure(
                            path = dir.toString(),
                            reason = GcFailureReason.PRUNE,
                            detail = error.message,
                        )
                    }
                    return FileVisitResult.CONTINUE
                }
            }

            try {
                Files.walkFileTree(managedRoot, visitor)
            } catch (error: IOException) {
                failures += GcFailure(
                    path = managedRoot.toString(),
                    reason = GcFailureReason.READ,
                    detail = error.message,
                )
            }
            if (abortedByLock) break
        }

        return AppResult.Success(
            GcReport(
                scannedFiles = scannedFiles,
                deletedFiles = deletedFiles,
                reclaimedBytes = reclaimedBytes,
                keptReferenced = keptReferenced,
                keptYoung = keptYoung,
                abortedByLock = abortedByLock,
                failures = failures,
            ),
        )
    }

    private fun collectReferencesOrThrow(): Set<String> {
        if (!Files.exists(instances)) return emptySet()
        if (!Files.isDirectory(instances)) {
            throw GcException(
                "STORAGE_GC_REFERENCES_INVALID",
                "the instance root is not a directory: $instances",
            )
        }

        val paths = linkedSetOf<String>()
        try {
            Files.newDirectoryStream(instances).use { entries ->
                for (entry in entries) {
                    val name = entry.fileName.toString()
                    if (name.startsWith(".")) continue
                    if (!Files.isDirectory(entry) || Files.isSymbolicLink(entry)) continue

                    val recordFile = entry.resolve(INSTANCE_FILE)
                    val manifestFile = entry.resolve(MANIFEST_FILE)
                    if (!Files.exists(recordFile) && !Files.exists(manifestFile)) continue

                    if (Files.isSymbolicLink(manifestFile) || !Files.isRegularFile(manifestFile)) {
                        throw GcException(
                            "STORAGE_GC_REFERENCES_INVALID",
                            "instance '$name' has no readable manifest.json",
                        )
                    }

                    val text = try {
                        String(Files.readAllBytes(manifestFile), Charsets.UTF_8)
                    } catch (error: IOException) {
                        throw GcException(
                            "STORAGE_GC_REFERENCES_INVALID",
                            "instance '$name' manifest could not be read: ${error.message}",
                            error,
                        )
                    }
                    val manifest = try {
                        InstallJson.decodeFromString<InstanceManifest>(text)
                    } catch (error: Exception) {
                        throw GcException(
                            "STORAGE_GC_REFERENCES_INVALID",
                            "instance '$name' manifest is not valid: ${error.message}",
                            error,
                        )
                    }

                    for (file in manifest.files) {
                        val absolute = try {
                            store.pathFor(file.path)
                        } catch (error: Exception) {
                            throw GcException(
                                "STORAGE_GC_REFERENCES_INVALID",
                                "instance '$name' references an unsafe path '${file.path}': ${error.message}",
                                error,
                            )
                        }
                        paths += store.root.relativize(absolute).joinToString("/") { it.toString() }
                    }
                }
            }
        } catch (error: IOException) {
            throw GcException(
                "STORAGE_GC_IO",
                "the instance list could not be read: ${error.message}",
                error,
            )
        }
        return paths
    }

    private fun normalise(paths: Set<String>): Set<String> =
        paths.mapTo(linkedSetOf<String>()) { store.pathFor(it).toString() }

    private fun referencesFailure(detail: String): Failure = Failure(
        code = "STORAGE_GC_REFERENCES_INVALID",
        stage = FailureStage.STORAGE,
        retryable = false,
        userMessage = "The shared file store could not be checked safely.",
        userAction = UserAction.NONE,
        technicalDetail = detail,
    )

    private class GcException(
        val code: String,
        detail: String,
        cause: Throwable? = null,
    ) : Exception(detail, cause) {
        val failure: Failure
            get() {
                val retryable = code == "STORAGE_GC_IO"
                return Failure(
                    code = code,
                    stage = FailureStage.STORAGE,
                    retryable = retryable,
                    userMessage = if (retryable) {
                        "The shared file store could not be cleaned up."
                    } else {
                        "The shared file store could not be checked safely."
                    },
                    userAction = if (retryable) UserAction.RETRY else UserAction.NONE,
                    technicalDetail = message,
                )
            }
    }

    companion object {
        /** Seven days, matching the deletion grace in docs 07 §5.8. */
        const val GRACE_MILLIS: Long = 7L * 24L * 60L * 60L * 1000L

        private val MANAGED_ROOTS = listOf("versions", "libraries", "assets", "logging")
        private const val INSTANCE_FILE = "instance.json"
        private const val MANIFEST_FILE = "manifest.json"
    }
}
