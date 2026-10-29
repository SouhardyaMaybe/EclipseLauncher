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
import dev.shadow.eclipselauncher.domain.mojang.MetadataException
import dev.shadow.eclipselauncher.domain.mojang.VersionResolver
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlinx.serialization.Serializable

/** Schema for the pending-plan cache. Bump when [PendingPlanRecord] changes. */
const val PENDING_PLAN_SCHEMA = 1

/**
 * A plan that has not been committed yet is a cache, not a promise. Keeping it
 * for a week lets a paused install survive process death without letting an
 * abandoned plan pin store objects forever.
 */
const val PENDING_PLAN_TTL_MILLIS = 7L * 24L * 60L * 60L * 1000L

/**
 * `install-plans/<instanceId>.json`: the plan a download was using when the
 * process died. It records the instance and version it belongs to, so a stale
 * file is ignored instead of committing the wrong tree.
 */
@Serializable
data class PendingPlanRecord(
    val schema: Int = PENDING_PLAN_SCHEMA,
    val instanceId: String,
    val versionId: String,
    val savedAtEpochMillis: Long,
    val plan: InstallPlan,
)

/**
 * Durable cache for the plan of an in-flight install.
 *
 * The committed `plan.json` inside the instance tree remains the source of
 * truth for repair and launch. This cache only covers the window between the
 * first metadata fetch and the atomic commit, so a process death does not have
 * to re-fetch Mojang metadata to resume. Every write goes through a `.part`
 * file, an fsync and a rename; a malformed or stale file is treated as absent.
 *
 * The collector asks [referencedPaths] for destinations it must not delete.
 * A file that cannot be decoded makes that call fail, so the collector stops
 * rather than deleting objects a resumable plan might still need.
 */
class PendingPlanStore(
    root: Path,
    private val clock: EpochClock = SystemEpochClock,
) {
    private val directory = root.toAbsolutePath().normalize()

    /**
     * Persists [plan] for [instanceId]. A failure is not fatal: the install
     * still has the in-memory plan, and the next run re-fetches metadata.
     */
    fun save(instanceId: String, plan: InstallPlan): AppResult<Unit> {
        val target = try {
            pathFor(instanceId)
        } catch (error: PendingPlanException) {
            return AppResult.Error(error.failure)
        }
        if (!validPlan(plan)) {
            return AppResult.Error(
                failure(
                    code = "INSTALL_PLAN_INVALID",
                    message = "The install plan is not internally consistent.",
                    detail = "refusing to persist a plan for $instanceId",
                ),
            )
        }

        val record = PendingPlanRecord(
            instanceId = instanceId,
            versionId = plan.versionId,
            savedAtEpochMillis = clock.nowEpochMillis(),
            plan = plan,
        )
        val part = directory.resolve("$instanceId.json.part")
        return try {
            Files.createDirectories(directory)
            val bytes = InstallJson.encodeToString(record).toByteArray(Charsets.UTF_8)
            Files.write(
                part,
                bytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            )
            FileChannel.open(part, StandardOpenOption.WRITE).use { it.force(true) }
            moveIntoPlace(part, target)
            AppResult.Success(Unit)
        } catch (error: IOException) {
            runCatching { Files.deleteIfExists(part) }
            AppResult.Error(
                failure(
                    code = "INSTALL_PLAN_IO",
                    message = "The pending install plan could not be saved.",
                    detail = error.message,
                ),
            )
        }
    }

    /**
     * Returns the cached plan only when it belongs to this instance and version
     * and is younger than [PENDING_PLAN_TTL_MILLIS]. Everything else is absent.
     */
    fun load(instanceId: String, versionId: String): InstallPlan? {
        val target = pathForOrNull(instanceId) ?: return null
        if (!Files.isRegularFile(target)) return null
        val record = read(target) ?: return null
        if (record.schema != PENDING_PLAN_SCHEMA) return null
        if (record.instanceId != instanceId) return null
        if (record.versionId != versionId) return null
        if (record.plan.versionId != versionId) return null
        val age = clock.nowEpochMillis() - record.savedAtEpochMillis
        if (age > PENDING_PLAN_TTL_MILLIS) return null
        if (!validPlan(record.plan)) return null
        return record.plan
    }

    /**
     * Store-relative destinations that live pending plans still expect.
     *
     * A malformed file is an error, not an empty set: the caller is about to
     * delete unreferenced objects, and an unreadable plan is not proof that its
     * objects are free.
     */
    fun referencedPaths(): AppResult<Set<String>> = try {
        if (!Files.isDirectory(directory)) {
            AppResult.Success(emptySet())
        } else {
            val paths = linkedSetOf<String>()
            Files.newDirectoryStream(directory, "*.json").use { entries ->
                for (entry in entries) {
                    if (Files.isSymbolicLink(entry) || !Files.isRegularFile(entry)) continue
                    val name = entry.fileName.toString()
                    val instanceId = name.removeSuffix(".json")
                    val expected = pathForOrNull(instanceId)
                    if (expected == null || expected != entry.toAbsolutePath().normalize()) {
                        throw PendingPlanException(
                            failure(
                                code = "INSTALL_PLAN_INVALID",
                                message = "A pending install plan has an invalid name.",
                                detail = name,
                            ),
                        )
                    }
                    val record = read(entry) ?: throw PendingPlanException(
                        failure(
                            code = "INSTALL_PLAN_INVALID",
                            message = "A pending install plan could not be read.",
                            detail = name,
                        ),
                    )
                    if (record.schema != PENDING_PLAN_SCHEMA ||
                        record.instanceId != instanceId ||
                        record.plan.versionId != record.versionId
                    ) {
                        throw PendingPlanException(
                            failure(
                                code = "INSTALL_PLAN_INVALID",
                                message = "A pending install plan does not match its instance.",
                                detail = name,
                            ),
                        )
                    }
                    val age = clock.nowEpochMillis() - record.savedAtEpochMillis
                    if (age > PENDING_PLAN_TTL_MILLIS) continue
                    if (!validPlan(record.plan)) {
                        throw PendingPlanException(
                            failure(
                                code = "INSTALL_PLAN_INVALID",
                                message = "A pending install plan is not internally consistent.",
                                detail = name,
                            ),
                        )
                    }
                    record.plan.downloads.mapTo(paths) { it.destination }
                }
            }
            AppResult.Success(paths)
        }
    } catch (error: PendingPlanException) {
        AppResult.Error(error.failure)
    } catch (error: IOException) {
        AppResult.Error(
            failure(
                code = "INSTALL_PLAN_IO",
                message = "The pending install plans could not be inspected.",
                detail = error.message,
            ),
        )
    }

    /** Removes the cache after a commit, or when the instance is discarded. */
    fun clear(instanceId: String) {
        val target = pathForOrNull(instanceId) ?: return
        runCatching { Files.deleteIfExists(target) }
        runCatching { Files.deleteIfExists(directory.resolve("$instanceId.json.part")) }
    }

    private fun pathFor(instanceId: String): Path {
        if (!INSTANCE_ID.matches(instanceId)) {
            throw PendingPlanException(
                failure(
                    code = "INSTANCE_ID_INVALID",
                    message = "The instance id cannot name a pending plan.",
                    detail = instanceId,
                ),
            )
        }
        return directory.resolve("$instanceId.json")
    }

    private fun pathForOrNull(instanceId: String): Path? = try {
        pathFor(instanceId)
    } catch (_: PendingPlanException) {
        null
    }

    private fun read(path: Path): PendingPlanRecord? = try {
        InstallJson.decodeFromString<PendingPlanRecord>(
            Files.readString(path, Charsets.UTF_8),
        )
    } catch (_: Exception) {
        null
    }

    /**
     * Checks the invariants the executor and transaction rely on. A plan read
     * from disk is untrusted input even though this app wrote it.
     */
    private fun validPlan(plan: InstallPlan): Boolean {
        if (plan.versionId.isBlank()) return false
        if (plan.downloads.isEmpty()) return false
        if (plan.downloads.distinctBy { it.destination }.size != plan.downloads.size) return false
        if (plan.downloads.any { it.size < 0L }) return false
        if (plan.downloads.any { !SHA1.matches(it.sha1) }) return false
        if (plan.downloads.any {
                runCatching { VersionResolver.safeRelativePath(it.destination) }.isFailure
            }
        ) {
            return false
        }
        if (plan.downloads.any {
                runCatching { requireHttpsDownloadUrl(it.url, it.kind) }.isFailure
            }
        ) {
            return false
        }

        val expected = buildList {
            add(plan.client)
            addAll(plan.libraries)
            add(plan.assetIndex)
            plan.logging?.let { add(it) }
            addAll(plan.assets)
        }
        if (expected != plan.downloads) return false
        return plan.totalBytes == plan.downloads.sumOf { it.size }
    }

    private fun moveIntoPlace(part: Path, target: Path) {
        try {
            Files.move(
                part,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            // A cache can tolerate a non-atomic replace; a torn file decodes
            // as absent on the next read.
            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun failure(code: String, message: String, detail: String?): Failure = Failure(
        code = code,
        stage = FailureStage.STORAGE,
        retryable = false,
        userMessage = message,
        userAction = UserAction.NONE,
        technicalDetail = detail,
    )

    private class PendingPlanException(val failure: Failure) : Exception(failure.userMessage)

    private companion object {
        val INSTANCE_ID = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
        val SHA1 = Regex("[0-9a-f]{40}")
    }
}

/** Shared plan-time URL guard; the network layer re-checks every redirect. */
internal fun requireHttpsDownloadUrl(url: String, kind: DownloadKind) {
    val uri = try {
        java.net.URI(url)
    } catch (error: java.net.URISyntaxException) {
        throw MetadataException("$kind has an invalid URL: ${error.message}")
    }
    if (!uri.scheme.equals("https", ignoreCase = true)) {
        throw MetadataException("$kind is not HTTPS: $url")
    }
    if (uri.host.isNullOrBlank()) {
        throw MetadataException("$kind has no host: $url")
    }
    if (uri.userInfo != null) {
        throw MetadataException("$kind URL contains credentials: $url")
    }
    if (uri.port != -1 && uri.port != 443) {
        throw MetadataException("$kind uses a non-443 port: $url")
    }
}
