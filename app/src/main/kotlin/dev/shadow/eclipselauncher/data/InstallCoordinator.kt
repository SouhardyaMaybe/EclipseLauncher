/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.core.common.AppResult
import dev.shadow.eclipselauncher.core.common.Failure
import dev.shadow.eclipselauncher.core.common.FailureStage
import dev.shadow.eclipselauncher.core.common.UserAction
import dev.shadow.eclipselauncher.core.model.InstanceSummary
import dev.shadow.eclipselauncher.core.model.LoaderKind
import dev.shadow.eclipselauncher.core.network.DownloadSource
import dev.shadow.eclipselauncher.core.network.HttpDownloadSource
import dev.shadow.eclipselauncher.domain.install.ArtifactStore
import dev.shadow.eclipselauncher.domain.install.InstallExecutor
import dev.shadow.eclipselauncher.domain.install.InstallPlan
import dev.shadow.eclipselauncher.domain.install.InstallPlanBuilder
import dev.shadow.eclipselauncher.domain.install.InstallProgress
import dev.shadow.eclipselauncher.domain.install.InstanceTransaction
import dev.shadow.eclipselauncher.domain.install.decodeInstallPlan
import dev.shadow.eclipselauncher.domain.install.decodeInstanceManifest
import dev.shadow.eclipselauncher.domain.mojang.LaunchContext
import dev.shadow.eclipselauncher.domain.mojang.VersionResolver
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * The Android-side bridge from a selected instance to a committed install.
 *
 * One worker owns the store and the transaction, so two installs cannot race
 * for the same object or journal. Metadata, planning and downloading all run
 * off the main thread; the only main-thread work is updating [TaskCenter].
 *
 * Pause stops the current pass between 64 KiB chunks. Resume starts a new pass
 * with the same plan; the store skips every object whose size and SHA-1 already
 * match, so a pause does not throw away completed work. Cancel removes the
 * task and leaves verified store objects for the collector's grace period.
 */
class InstallCoordinator(
    private val context: Context,
    private val minecraftDir: File,
    private val tasks: TaskCenter,
    source: DownloadSource = HttpDownloadSource(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) {
    private val instancesDir = File(minecraftDir, "instances")
    private val store = ArtifactStore(minecraftDir.toPath())
    private val transaction = InstanceTransaction(instancesDir.toPath(), store)
    private val metadata = MojangMetadataClient(source)
    private val executor = InstallExecutor(store, source)
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "eclipse-install").apply { isDaemon = true }
    }
    private val runs = ConcurrentHashMap<String, InstallRun>()

    /**
     * Starts a Vanilla install. Returns false when a task for this instance is
     * already running; the caller should open the Tasks dialog instead of
     * starting a second download.
     */
    fun start(instance: InstanceSummary, onInstalled: () -> Unit = {}): Boolean {
        if (instance.loader != LoaderKind.VANILLA) return false
        val taskId = taskId(instance)
        if (runs.containsKey(taskId)) return false

        val run = InstallRun(
            taskId = taskId,
            instance = instance,
            title = context.getString(R.string.install_task_title, instance.minecraftVersion),
            onInstalled = onInstalled,
        )
        runs[taskId] = run

        onMain {
            tasks.add(
                LauncherTask(
                    id = taskId,
                    title = run.title,
                    onPause = { run.paused = true },
                    onResume = { resume(run) },
                    onCancel = { cancel(run) },
                    onRetry = { retry(run) },
                ),
            )
        }
        launch(run)
        return true
    }

    /**
     * Repairs the one journal state a crash can leave behind. The transaction
     * also does this before an install, but doing it at startup means a stale
     * staging tree does not survive until the next download.
     */
    fun recover() {
        onMain { tasks.finish(RECOVERY_TASK_ID) }
        worker.execute {
            when (val result = transaction.recover()) {
                is AppResult.Error -> onMain {
                    tasks.add(
                        LauncherTask(
                            id = RECOVERY_TASK_ID,
                            title = context.getString(R.string.install_recovery_title),
                            state = TaskState.FAILED,
                            failure = result.failure,
                            onRetry = { recover() },
                            onCancel = { tasks.finish(RECOVERY_TASK_ID) },
                        ),
                    )
                }

                is AppResult.Success -> Unit
            }
        }
    }

    /**
     * Stops a task that belongs to an instance the user is about to replace or
     * retarget. The committed tree is untouched; only the worker is told to
     * stop before its next chunk.
     */
    fun cancel(instance: InstanceSummary) {
        val run = runs[taskId(instance)] ?: return
        cancel(run)
        onMain { tasks.finish(run.taskId) }
    }

    private fun taskId(instance: InstanceSummary): String = "install:${instance.id.value}"

    private fun launch(run: InstallRun): Boolean {
        if (!run.executing.compareAndSet(false, true)) return false
        val attempt = run.attempt.incrementAndGet()
        worker.execute { execute(run, attempt) }
        return true
    }

    private fun execute(run: InstallRun, attempt: Int) {
        val terminal: () -> Unit = try {
            runInstall(run)
        } catch (error: Exception) {
            { fail(run, unexpectedFailure(error)) }
        } finally {
            // Clear the flag before the terminal state is posted. A user who
            // sees "Paused" or "Failed" must be able to resume or retry
            // immediately, without racing the worker's last instruction.
            run.executing.set(false)
        }
        // A resume or retry can start a new attempt in the window between the
        // flag being cleared and this terminal state being posted. A stale
        // attempt must not overwrite the newer attempt's RUNNING state.
        if (run.attempt.get() != attempt || run.cancelled) return
        terminal()
    }

    private fun runInstall(run: InstallRun): () -> Unit {
        onMain { tasks.setPhase(run.taskId, TaskPhase.PREPARING) }

        val planResult = run.plan?.let { AppResult.Success(it) } ?: preparePlan(run)
        val plan = when (planResult) {
            is AppResult.Error -> return terminalFor(run, planResult.failure)
            is AppResult.Success -> planResult.value
        }
        run.plan = plan
        if (run.cancelled) return {}
        if (run.paused) return { pause(run) }

        onMain { tasks.setPhase(run.taskId, TaskPhase.DOWNLOADING) }
        val download = executor.execute(
            plan = plan,
            onProgress = { progress -> report(run, progress) },
            isCancelled = { run.paused || run.cancelled },
        )
        if (run.cancelled) return {}
        if (download is AppResult.Error) return terminalFor(run, download.failure)
        if (run.paused) return { pause(run) }

        if (!committedPlanMatches(run.instance, plan)) {
            onMain { tasks.setPhase(run.taskId, TaskPhase.COMMITTING) }
            val commit = transaction.install(
                id = run.instance.id.value,
                name = run.instance.name,
                minecraftVersion = run.instance.minecraftVersion,
                loader = run.instance.loader.name,
                plan = plan,
            )
            return when (commit) {
                is AppResult.Error -> ({ fail(run, commit.failure) })
                is AppResult.Success -> ({ finish(run) })
            }
        }
        return { finish(run) }
    }

    private fun preparePlan(run: InstallRun): AppResult<InstallPlan> {
        readExistingPlan(run.instance)?.let { return AppResult.Success(it) }
        val isCancelled = { run.paused || run.cancelled }

        val manifest = when (val result = metadata.fetchManifest(isCancelled)) {
            is AppResult.Error -> return result
            is AppResult.Success -> result.value
        }
        if (isCancelled()) return AppResult.Error(installCancelledFailure())

        val summary = manifest.versions.firstOrNull { it.id == run.instance.minecraftVersion }
            ?: return AppResult.Error(
                Failure(
                    code = "MOJANG_VERSION_UNKNOWN",
                    stage = FailureStage.RESOLVE,
                    retryable = false,
                    userMessage = "Minecraft ${run.instance.minecraftVersion} is not in the official version list.",
                    userAction = UserAction.CHOOSE_VERSION,
                    technicalDetail = "version ${run.instance.minecraftVersion} is not in the Mojang manifest",
                ),
            )

        val version = when (val result = metadata.fetchVersion(summary.url, isCancelled)) {
            is AppResult.Error -> return result
            is AppResult.Success -> result.value
        }
        if (isCancelled()) return AppResult.Error(installCancelledFailure())

        val resolved = when (val result = VersionResolver.resolve(version, launchContext())) {
            is AppResult.Error -> return result
            is AppResult.Success -> result.value
        }

        val assetIndex = when (val result = metadata.fetchAssetIndex(resolved.assetIndex.url, isCancelled)) {
            is AppResult.Error -> return result
            is AppResult.Success -> result.value
        }
        if (isCancelled()) return AppResult.Error(installCancelledFailure())

        return InstallPlanBuilder.build(resolved, assetIndex)
    }

    /**
     * A committed plan is the cheapest repair path: it lets a paused or failed
     * download continue without fetching Mojang metadata again. A plan is only
     * reused when the committed manifest names the same version and plan hash.
     */
    private fun readExistingPlan(instance: InstanceSummary): InstallPlan? {
        val planFile = File(File(instancesDir, instance.id.value), "plan.json")
        if (!planFile.isFile) return null
        val plan = runCatching {
            decodeInstallPlan(planFile.readText(Charsets.UTF_8))
        }.getOrNull() ?: return null
        if (plan.versionId != instance.minecraftVersion) return null
        if (!committedPlanMatches(instance, plan)) return null
        return plan
    }

    private fun committedPlanMatches(instance: InstanceSummary, plan: InstallPlan): Boolean {
        val manifestFile = File(File(instancesDir, instance.id.value), "manifest.json")
        if (!manifestFile.isFile) return false
        val manifest = runCatching {
            decodeInstanceManifest(manifestFile.readText(Charsets.UTF_8))
        }.getOrNull() ?: return false
        return manifest.planHash == plan.planHash &&
            manifest.versionId == instance.minecraftVersion
    }

    /**
     * The resolver needs the platform rules, not the host OS name. Android is
     * Linux with an ARM or x86_64 ABI; desktop natives are dropped separately
     * by EclipseNatives.
     */
    private fun launchContext(): LaunchContext {
        val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        val arch = when {
            abi.startsWith("x86_64") -> "x86_64"
            else -> "arm64"
        }
        return LaunchContext(osName = "linux", osArch = arch)
    }

    private fun report(run: InstallRun, progress: InstallProgress) {
        onMain {
            tasks.progress(
                id = run.taskId,
                completedItems = progress.completedItems,
                totalItems = progress.totalItems,
                completedBytes = progress.completedBytes,
                totalBytes = progress.totalBytes,
                fraction = progress.fraction,
            )
        }
    }

    private fun terminalFor(run: InstallRun, failure: Failure): () -> Unit =
        if (failure.code == "INSTALL_CANCELLED" && run.paused) {
            { pause(run) }
        } else {
            { fail(run, failure) }
        }

    private fun pause(run: InstallRun) {
        onMain { tasks.markPaused(run.taskId) }
    }

    private fun fail(run: InstallRun, failure: Failure) {
        onMain { tasks.fail(run.taskId, failure) }
    }

    private fun finish(run: InstallRun) {
        runs.remove(run.taskId)
        onMain {
            tasks.finish(run.taskId)
            run.onInstalled()
        }
    }

    private fun resume(run: InstallRun) {
        if (!runs.containsKey(run.taskId)) return
        run.paused = false
        if (!launch(run)) {
            run.paused = true
            return
        }
        onMain { tasks.markRunning(run.taskId) }
    }

    private fun retry(run: InstallRun) {
        if (!runs.containsKey(run.taskId)) return
        run.paused = false
        run.cancelled = false
        if (!launch(run)) return
        onMain { tasks.markRunning(run.taskId) }
    }

    private fun cancel(run: InstallRun) {
        run.cancelled = true
        run.paused = false
        runs.remove(run.taskId)
    }

    private fun unexpectedFailure(error: Exception): Failure = Failure(
        code = "INSTALL_UNEXPECTED",
        stage = FailureStage.INSTALL,
        retryable = true,
        userMessage = "The install stopped unexpectedly.",
        userAction = UserAction.RETRY,
        technicalDetail = error.message,
    )

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private class InstallRun(
        val taskId: String,
        val instance: InstanceSummary,
        val title: String,
        val onInstalled: () -> Unit,
    ) {
        val executing = AtomicBoolean(false)
        val attempt = AtomicInteger(0)

        @Volatile
        var paused: Boolean = false

        @Volatile
        var cancelled: Boolean = false

        @Volatile
        var plan: InstallPlan? = null
    }

    private companion object {
        const val RECOVERY_TASK_ID = "install:recovery"
    }
}
