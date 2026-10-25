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
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/** The committed instance tree and the two documents that describe it. */
data class InstalledInstance(
    val root: Path,
    val record: InstanceRecord,
    val manifest: InstanceManifest,
)

/**
 * Materialises a downloaded plan into an instance tree and commits it with an
 * atomic rename.
 *
 * The sequence is: verify every shared-store object, write a complete staging
 * tree, fsync it, write a journal, rename the old instance aside, rename the
 * staging tree into place, then mark the journal committed. A crash at any
 * point leaves either the old instance or a recoverable journal, never a
 * half-written instance under the final id.
 */
class InstanceTransaction(
    instancesRoot: Path,
    private val store: ArtifactStore,
    private val clock: EpochClock = SystemEpochClock,
) {
    private val root = instancesRoot.toAbsolutePath().normalize()
    private val journalPath = root.resolve(JOURNAL_FILE)

    init {
        Files.createDirectories(root)
    }

    fun install(
        id: String,
        name: String,
        minecraftVersion: String,
        loader: String,
        plan: InstallPlan,
        txid: String? = null,
    ): AppResult<InstalledInstance> = try {
        recoverOrThrow()
        AppResult.Success(
            installOrThrow(
                id = id,
                name = name,
                minecraftVersion = minecraftVersion,
                loader = loader,
                plan = plan,
                txid = txid ?: "$id-${clock.nowEpochMillis()}",
            ),
        )
    } catch (error: InstallException) {
        failure(error)
    } catch (error: StoreException) {
        storeFailure(error)
    } catch (error: IOException) {
        ioFailure(error)
    }

    /**
     * Repairs the one journal state that can exist after a crash. Call this at
     * startup, before reading the instance list, so the filesystem is the
     * source of truth again.
     */
    fun recover(): AppResult<Unit> = try {
        recoverOrThrow()
        AppResult.Success(Unit)
    } catch (error: InstallException) {
        failure(error)
    } catch (error: IOException) {
        ioFailure(error)
    }

    private fun installOrThrow(
        id: String,
        name: String,
        minecraftVersion: String,
        loader: String,
        plan: InstallPlan,
        txid: String,
    ): InstalledInstance {
        requireSegment(id, "instance id", "INSTANCE_ID_INVALID")
        requireSegment(txid, "transaction id", "INSTALL_TXID_INVALID")

        val cleanName = name.trim()
        val cleanVersion = minecraftVersion.trim()
        val cleanLoader = loader.trim()
        if (cleanName.isEmpty() || cleanVersion.isEmpty() || cleanLoader.isEmpty()) {
            throw InstallException(
                "INSTANCE_RECORD_INVALID",
                "an instance needs a name, a Minecraft version and a loader",
            )
        }
        if (plan.versionId != cleanVersion) {
            throw InstallException(
                "INSTALL_PLAN_INVALID",
                "the plan is for ${plan.versionId}, not $cleanVersion",
            )
        }

        val missing = plan.downloads.filterNot { store.isVerified(it) }
        if (missing.isNotEmpty()) {
            throw InstallException(
                "INSTALL_INCOMPLETE",
                "${missing.size} of ${plan.downloads.size} files are not verified in the store",
            )
        }

        val finalRoot = root.resolve(id)
        val staging = root.resolve(STAGING_DIR).resolve(txid)
        if (Files.exists(staging)) deleteTree(staging)
        Files.createDirectories(staging)
        for (directory in SKELETON) {
            Files.createDirectories(staging.resolve(directory))
        }

        // The transaction replaces the metadata tree, but the player's game
        // directory is not launcher-managed. Hard-link it into the new tree
        // (falling back to a copy) so a repair or version change cannot delete
        // mods, worlds, options or screenshots. Symlinks are skipped.
        preserveUserContent(
            source = finalRoot.resolve(USER_CONTENT_DIR),
            target = staging.resolve(USER_CONTENT_DIR),
        )

        val createdAt = clock.nowEpochMillis()
        val planHash = plan.planHash
        val record = InstanceRecord(
            id = id,
            name = cleanName,
            minecraftVersion = cleanVersion,
            loader = cleanLoader,
            createdAtEpochMillis = createdAt,
            planHash = planHash,
        )
        val manifest = InstanceManifest(
            instanceId = id,
            versionId = plan.versionId,
            createdAtEpochMillis = createdAt,
            planHash = planHash,
            files = plan.downloads.map { item ->
                ManagedFile(
                    path = item.destination,
                    sha1 = item.sha1,
                    size = item.size,
                    kind = item.kind,
                    origin = item.url,
                )
            },
        )

        writeNew(staging.resolve(INSTANCE_FILE), InstallJson.encodeToString(record))
        writeNew(staging.resolve(MANIFEST_FILE), InstallJson.encodeToString(manifest))
        writeNew(staging.resolve(PLAN_FILE), InstallJson.encodeToString(plan))
        forceTree(staging)

        val backupRelative = if (Files.exists(finalRoot)) {
            Files.createDirectories(root.resolve(TRASH_DIR))
            val candidate = "$TRASH_DIR/$id-$txid"
            if (Files.exists(root.resolve(candidate))) deleteTree(root.resolve(candidate))
            candidate
        } else {
            null
        }

        val prepared = InstallJournal(
            txid = txid,
            instanceId = id,
            state = STATE_PREPARED,
            backup = backupRelative,
        )
        writeJournal(prepared)
        writeJournal(prepared.copy(state = STATE_COMMITTING))

        try {
            if (backupRelative != null) {
                atomicMove(finalRoot, root.resolve(backupRelative))
            }
            atomicMove(staging, finalRoot)
            forceDirectory(root)
        } catch (error: IOException) {
            restoreOldInstance(finalRoot, backupRelative)
            throw InstallException(
                "INSTALL_COMMIT_FAILED",
                "the instance could not be committed: ${error.message ?: "rename failed"}",
                error,
            )
        } catch (error: InstallException) {
            restoreOldInstance(finalRoot, backupRelative)
            throw error
        }

        // The instance is committed. A stale committed journal is harmless and
        // will be removed by recovery; do not turn a successful commit into a
        // failure just because cleanup was interrupted.
        try {
            writeJournal(prepared.copy(state = STATE_COMMITTED))
        } catch (_: IOException) {
            // Recovery treats an existing final directory as committed.
        }
        try {
            Files.deleteIfExists(journalPath)
        } catch (_: IOException) {
            // Recovery will remove it on the next start.
        }

        return InstalledInstance(root = finalRoot, record = record, manifest = manifest)
    }

    private fun recoverOrThrow() {
        val journal = readJournal() ?: return
        requireSegment(journal.instanceId, "journal instance id", "INSTALL_RECOVERY_FAILED")
        requireSegment(journal.txid, "journal transaction id", "INSTALL_RECOVERY_FAILED")

        val staging = root.resolve(STAGING_DIR).resolve(journal.txid)
        val finalRoot = root.resolve(journal.instanceId)
        val backup = journal.backup?.let { relative ->
            val candidate = root.resolve(relative).normalize()
            if (!candidate.startsWith(root)) {
                throw InstallException(
                    "INSTALL_RECOVERY_FAILED",
                    "the journal backup escapes the instance root",
                )
            }
            candidate
        }

        when (journal.state) {
            STATE_PREPARED -> {
                deleteTree(staging)
                Files.deleteIfExists(journalPath)
            }

            STATE_COMMITTING -> when {
                Files.isDirectory(finalRoot) -> {
                    deleteTree(staging)
                    Files.deleteIfExists(journalPath)
                }

                Files.isDirectory(staging) -> {
                    atomicMove(staging, finalRoot)
                    forceDirectory(root)
                    Files.deleteIfExists(journalPath)
                }

                backup != null && Files.isDirectory(backup) -> {
                    atomicMove(backup, finalRoot)
                    forceDirectory(root)
                    Files.deleteIfExists(journalPath)
                }

                else -> throw InstallException(
                    "INSTALL_RECOVERY_FAILED",
                    "the journal has neither a staging tree nor a backup to recover",
                )
            }

            STATE_COMMITTED -> Files.deleteIfExists(journalPath)

            else -> throw InstallException(
                "INSTALL_RECOVERY_FAILED",
                "unknown journal state: ${journal.state}",
            )
        }
    }

    /**
     * Preserves everything under the old `game/` directory. A hard link keeps
     * large saves and modpacks from being duplicated; a filesystem that does
     * not support links gets a normal copy. Symlinked files and directories
     * are skipped so a hostile tree cannot make the launcher read elsewhere.
     */
    private fun preserveUserContent(source: Path, target: Path) {
        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) return
        Files.walkFileTree(
            source,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    directory: Path,
                    attributes: BasicFileAttributes,
                ): FileVisitResult {
                    if (Files.isSymbolicLink(directory)) return FileVisitResult.SKIP_SUBTREE
                    Files.createDirectories(target.resolve(source.relativize(directory)))
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attributes: BasicFileAttributes,
                ): FileVisitResult {
                    if (Files.isSymbolicLink(file)) return FileVisitResult.CONTINUE
                    val destination = target.resolve(source.relativize(file))
                    Files.createDirectories(destination.parent)
                    try {
                        Files.createLink(destination, file)
                    } catch (_: IOException) {
                        Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING)
                    } catch (_: UnsupportedOperationException) {
                        Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING)
                    }
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    private fun readJournal(): InstallJournal? {
        if (!Files.isRegularFile(journalPath)) return null
        val text = try {
            String(Files.readAllBytes(journalPath), Charsets.UTF_8)
        } catch (error: IOException) {
            throw InstallException("INSTALL_RECOVERY_FAILED", "the install journal could not be read", error)
        }
        return try {
            InstallJson.decodeFromString<InstallJournal>(text)
        } catch (error: Exception) {
            throw InstallException("INSTALL_RECOVERY_FAILED", "the install journal is not valid", error)
        }
    }

    private fun writeJournal(journal: InstallJournal) {
        Files.write(
            journalPath,
            InstallJson.encodeToString(journal).toByteArray(Charsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        )
        forceFile(journalPath)
    }

    private fun writeNew(path: Path, text: String) {
        Files.write(
            path,
            text.toByteArray(Charsets.UTF_8),
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE,
        )
    }

    private fun forceTree(path: Path) {
        Files.walk(path).use { stream ->
            stream.filter { Files.isRegularFile(it) }.forEach { forceFile(it) }
        }
        forceDirectory(path)
    }

    private fun forceFile(path: Path) {
        FileChannel.open(path, StandardOpenOption.WRITE).use { it.force(true) }
    }

    private fun forceDirectory(path: Path) {
        try {
            FileChannel.open(path, StandardOpenOption.READ).use { it.force(true) }
        } catch (_: IOException) {
            // Directory fsync is not portable. Files are already fsynced and
            // the final rename is atomic; the verifier catches a torn tree.
        }
    }

    private fun atomicMove(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (error: AtomicMoveNotSupportedException) {
            throw InstallException(
                "INSTALL_COMMIT_FAILED",
                "the filesystem does not support an atomic rename",
                error,
            )
        }
    }

    private fun restoreOldInstance(finalRoot: Path, backupRelative: String?) {
        if (backupRelative == null) return
        val backup = root.resolve(backupRelative)
        if (!Files.exists(finalRoot) && Files.isDirectory(backup)) {
            try {
                atomicMove(backup, finalRoot)
            } catch (_: Exception) {
                // Leave the journal for recovery rather than deleting the only copy.
            }
        }
    }

    private fun deleteTree(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder<Path>()).forEach { Files.deleteIfExists(it) }
        }
    }

    private fun requireSegment(value: String, what: String, code: String) {
        if (!SEGMENT.matches(value)) {
            throw InstallException(code, "$what is not a safe lowercase identifier: $value")
        }
    }

    private fun failure(error: InstallException): AppResult.Error {
        val retryable = when (error.code) {
            "INSTALL_INCOMPLETE", "INSTALL_IO", "INSTALL_COMMIT_FAILED" -> true
            else -> false
        }
        val stage = when (error.code) {
            "INSTALL_INCOMPLETE" -> FailureStage.VERIFY
            "INSTALL_IO" -> FailureStage.STORAGE
            else -> FailureStage.INSTALL
        }
        val message = when (error.code) {
            "INSTALL_INCOMPLETE" -> "Some game files are missing from the shared store."
            "INSTALL_COMMIT_FAILED" -> "The instance could not be committed."
            "INSTALL_RECOVERY_FAILED" -> "A previous install could not be recovered."
            "INSTALL_PLAN_INVALID" -> "The install plan does not match this instance."
            "INSTANCE_ID_INVALID", "INSTALL_TXID_INVALID" -> "The instance identifier is not valid."
            "INSTANCE_RECORD_INVALID" -> "The instance is missing a name, version or loader."
            else -> "The instance could not be installed."
        }
        return AppResult.Error(
            Failure(
                code = error.code,
                stage = stage,
                retryable = retryable,
                userMessage = message,
                userAction = if (retryable) UserAction.RETRY else UserAction.NONE,
                technicalDetail = error.message,
            ),
        )
    }

    private fun storeFailure(error: StoreException): AppResult.Error = AppResult.Error(
        Failure(
            code = error.code,
            stage = error.stage,
            retryable = error.retryable,
            userMessage = "A game file could not be read from storage.",
            userAction = if (error.code == "INSTALL_CANCELLED") {
                UserAction.NONE
            } else {
                UserAction.RETRY
            },
            technicalDetail = error.message,
        ),
    )

    private fun ioFailure(error: IOException): AppResult.Error = AppResult.Error(
        Failure(
            code = "INSTALL_IO",
            stage = FailureStage.STORAGE,
            retryable = true,
            userMessage = "The instance could not be written to storage.",
            userAction = UserAction.RETRY,
            technicalDetail = error.message,
        ),
    )

    private companion object {
        const val INSTANCE_FILE = "instance.json"
        const val MANIFEST_FILE = "manifest.json"
        const val PLAN_FILE = "plan.json"
        const val JOURNAL_FILE = "journal.json"
        const val STAGING_DIR = ".staging"
        const val TRASH_DIR = ".trash"
        const val USER_CONTENT_DIR = "game"
        const val STATE_PREPARED = "prepared"
        const val STATE_COMMITTING = "committing"
        const val STATE_COMMITTED = "committed"
        val SEGMENT = Regex("[a-z0-9][a-z0-9-]{0,199}")
        val SKELETON = listOf(
            "game",
            "game/mods",
            "game/config",
            "game/resourcepacks",
            "game/shaderpacks",
            "game/saves",
            "logs",
            "cache",
            "crash",
        )
    }
}

@Serializable
internal data class InstallJournal(
    val schema: Int = INSTANCE_SCHEMA,
    val txid: String,
    val instanceId: String,
    val state: String,
    val backup: String? = null,
)

private class InstallException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
