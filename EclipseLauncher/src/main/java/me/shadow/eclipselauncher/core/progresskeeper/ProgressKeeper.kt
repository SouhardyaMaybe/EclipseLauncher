package me.shadow.eclipselauncher.core.progresskeeper

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * Central registry of the progress of all ongoing tasks.
 *
 * The source of truth is [progressStates], a [StateFlow] of immutable snapshots; every mutation
 * happens through atomic updates, so no locks are needed. The classic listener API
 * ([addListener]/[addTaskCountListener]/[waitUntilDone]) is kept as a thin coroutine bridge over
 * those flows: each registration subscribes to the flow and replays the current state to the
 * listener first, exactly like the original implementation did when a listener attached to an
 * already-running task.
 */
object ProgressKeeper {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("ProgressKeeper"))

    private val sessionCounter = AtomicLong(0)

    /** Snapshot of every ongoing progress record; an absent key means "no ongoing progress". */
    private val mutableProgressStates = MutableStateFlow<Map<String, ProgressState>>(emptyMap())

    /**
     * The live progress records. New collectors immediately receive the current snapshot.
     */
    val progressStates: StateFlow<Map<String, ProgressState>> = mutableProgressStates.asStateFlow()

    /**
     * The number of ongoing progress records as a flow; new collectors immediately receive the
     * current count.
     */
    val taskCountFlow: Flow<Int> = progressStates.map { it.size }

    private class ProgressListenerRegistration(val listener: ProgressListener, val job: Job)
    private class TaskCountListenerRegistration(val listener: TaskCountListener, val job: Job)

    private val progressListenerRegistrations = ConcurrentHashMap<String, CopyOnWriteArrayList<ProgressListenerRegistration>>()
    private val taskCountListenerRegistrations = CopyOnWriteArrayList<TaskCountListenerRegistration>()

    /**
     * Report the progress of a task.
     *
     * Reporting `(resid == -1 && progress == -1)` marks the record as finished: it is removed from
     * the registry, the task count drops and observers receive `onProgressEnded`. The first report
     * for a record starts it (observers receive `onProgressStarted`); every report after that
     * updates it (`onProgressUpdated`).
     */
    @JvmStatic
    fun submitProgress(progressRecord: String, progress: Int, resid: Int, vararg va: Any?) {
        val shouldCallEnded = resid == -1 && progress == -1
        val args = va.toList()
        mutableProgressStates.update { states ->
            val existingState = states[progressRecord]
            when {
                // Finishing a record: remove it (a no-op when it was not present, like before)
                shouldCallEnded -> if (existingState != null) states - progressRecord else states
                // Starting a record: mint a new session token
                existingState == null ->
                    states + (progressRecord to ProgressState(progress, resid, args, sessionCounter.incrementAndGet()))
                // Updating a record: keep the session token, replace the snapshot
                else -> states + (progressRecord to existingState.copy(progress = progress, resid = resid, args = args))
            }
        }
    }

    /**
     * Flow of the state of a single progress record. New collectors first receive the current
     * state of the record (or `null` when there is no ongoing progress for it).
     */
    fun observeProgress(progressRecord: String): Flow<ProgressState?> =
        progressStates.map { it[progressRecord] }.distinctUntilChanged()

    /**
     * Attach a [ProgressListener] to a progress record: the listener first receives the current
     * state replayed as `onProgressStarted` + `onProgressUpdated` (or `onProgressEnded` when
     * nothing is running), then every subsequent change of the record.
     */
    @JvmStatic
    fun addListener(progressRecord: String, listener: ProgressListener) {
        val registrations = progressListenerRegistrations.computeIfAbsent(progressRecord) { CopyOnWriteArrayList() }
        val job = scope.launch {
            // The session of the record this listener has already been told about; null means
            // "the listener currently believes nothing is running for this record".
            var lastSession: Long? = null
            var firstEmission = true
            observeProgress(progressRecord).collect { state ->
                if (firstEmission) {
                    firstEmission = false
                    lastSession = state?.session
                    if (state == null) listener.onProgressEnded()
                    else {
                        listener.onProgressStarted()
                        listener.onProgressUpdated(state.progress, state.resid, *state.args.toTypedArray())
                    }
                } else when {
                    state == null -> if (lastSession != null) {
                        lastSession = null
                        listener.onProgressEnded()
                    }
                    lastSession == null -> {
                        // Progress for this record started after the listener attached
                        lastSession = state.session
                        listener.onProgressStarted()
                    }
                    state.session != lastSession -> {
                        // A task ended and a new one began without an emission in between
                        lastSession = state.session
                        listener.onProgressEnded()
                        listener.onProgressStarted()
                    }
                    else -> listener.onProgressUpdated(state.progress, state.resid, *state.args.toTypedArray())
                }
            }
        }
        registrations.add(ProgressListenerRegistration(listener, job))
    }

    /**
     * Detach a [ProgressListener] from a progress record. Like before, only the first matching
     * registration is removed.
     */
    @JvmStatic
    fun removeListener(progressRecord: String, listener: ProgressListener) {
        val registrations = progressListenerRegistrations[progressRecord] ?: return
        val index = registrations.indexOfFirst { it.listener == listener }
        if (index >= 0) registrations.removeAt(index).job.cancel()
    }

    /**
     * Attach a [TaskCountListener] and immediately replay the current task count, unless the
     * listener was already attached (in which case only the replay happens, like before).
     */
    @JvmStatic
    @JvmOverloads
    fun addTaskCountListener(listener: TaskCountListener, runUpdate: Boolean = true) {
        if (taskCountListenerRegistrations.any { it.listener == listener }) {
            // Already attached: replay the current count (when requested) but do not double-subscribe
            if (runUpdate) scope.launch { listener.onUpdateTaskCount(getTaskCount()) }
            return
        }
        val registrationValue = getTaskCount()
        val job = scope.launch {
            var firstEmission = true
            taskCountFlow.collect { count ->
                if (firstEmission) {
                    firstEmission = false
                    // The value observed at subscription time either is the registration-time count
                    // (the replay; suppressed when runUpdate is false), or a change that raced with
                    // the registration (always delivered, so no update is ever lost).
                    if (!runUpdate && count == registrationValue) return@collect
                }
                listener.onUpdateTaskCount(count)
            }
        }
        taskCountListenerRegistrations.add(TaskCountListenerRegistration(listener, job))
    }

    /**
     * Detach a [TaskCountListener]; like before, only the first matching registration is removed.
     */
    @JvmStatic
    fun removeTaskCountListener(listener: TaskCountListener) {
        val index = taskCountListenerRegistrations.indexOfFirst { it.listener == listener }
        if (index >= 0) taskCountListenerRegistrations.removeAt(index).job.cancel()
    }

    /**
     * @return whether the task key exists in the current task set
     */
    @JvmStatic
    fun containsProgress(progressKey: String): Boolean = progressStates.value.containsKey(progressKey)

    /**
     * Run the runnable once every task is done. When no tasks are pending it runs synchronously
     * on the calling thread; otherwise it is invoked from the keeper's dispatcher as soon as the
     * task count drops to zero (the original ran it on the thread that updated the count last,
     * which could equally be a background thread).
     */
    @JvmStatic
    fun waitUntilDone(runnable: Runnable) {
        // If tasks are already done, run right away instead of registering a listener
        if (getTaskCount() == 0) {
            runnable.run()
            return
        }
        scope.launch {
            taskCountFlow.first { it == 0 }
            runnable.run()
        }
    }

    /**
     * @return the number of ongoing progress records
     */
    @JvmStatic
    fun getTaskCount(): Int = progressStates.value.size

    /**
     * @return whether there is at least one ongoing progress record
     */
    @JvmStatic
    fun hasOngoingTasks(): Boolean = getTaskCount() > 0
}
