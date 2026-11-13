/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The app root: dependency wiring and the back stack (HOME at the root plus
 * overlay routes for ACCOUNTS, Microsoft sign-in, instance creation, the
 * instance editor, UTILS and SETTINGS — there is no bottom navigation bar;
 * HOME's top-right buttons open the latter two). The tasks dialog floats
 * above everything, and play holds an honest stop: no runtime is wired yet.
 */
package com.eclipselauncher.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.eclipselauncher.R
import com.eclipselauncher.data.Account
import com.eclipselauncher.data.AccountKind
import com.eclipselauncher.data.Instance
import com.eclipselauncher.data.LoaderKind
import com.eclipselauncher.data.newId
import com.eclipselauncher.data.offlineUuid
import com.eclipselauncher.EclipseContainer
import com.eclipselauncher.EclipseLauncherApplication
import com.eclipselauncher.ui.components.TasksDialog
import com.eclipselauncher.ui.components.loaderNameString
import com.eclipselauncher.ui.screens.AccountsScreen
import com.eclipselauncher.ui.screens.CreateInstanceScreen
import com.eclipselauncher.ui.screens.HomeScreen
import com.eclipselauncher.ui.screens.InstanceEditorScreen
import com.eclipselauncher.ui.screens.LoaderPickerScreen
import com.eclipselauncher.ui.screens.ManageContentScreen
import com.eclipselauncher.ui.screens.MicrosoftAuthScreen
import com.eclipselauncher.ui.screens.ModpackBrowserScreen
import com.eclipselauncher.ui.screens.SettingsPage
import com.eclipselauncher.ui.screens.SettingsScreen
import com.eclipselauncher.ui.theme.EclipseTheme

/** Destinations: HOME plus the overlay screens. */
private sealed interface Route {
    data object Home : Route
    data object Accounts : Route
    data object MsAuth : Route
    data object Create : Route
    /** Step 2 of creation: the six loaders for one Minecraft version. */
    data class LoaderPicker(val mcVersion: String) : Route
    data object ModpackBrowser : Route
    data object ManageContent : Route
    data class Editor(val instanceId: String?, val presetLoader: LoaderKind?) : Route
    data class Settings(val initialPage: SettingsPage?) : Route
}

@Composable
fun EclipseApp() {
    val context = LocalContext.current
    // The process-wide container is created by the Application.
    val container = remember {
        (context.applicationContext as EclipseLauncherApplication).container
    }
    val scope = rememberCoroutineScope()

    val accountState by container.accounts.state.collectAsState()
    val instances by container.instances.state.collectAsState()
    val tasks by container.tasks.state.collectAsState()

    var stack by remember { mutableStateOf(listOf<Route>(Route.Home)) }
    val current = stack.last()

    // Task dialog visibility.
    var showTasks by remember { mutableStateOf(false) }

    // Instance HOME should select after a create; consumed once applied.
    var pendingSelectionId by remember { mutableStateOf<String?>(null) }

    fun push(route: Route) {
        stack = stack + route
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    // Hardware/gesture back: pop overlays; HOME stays (app exits).
    BackHandler(enabled = stack.size > 1) { pop() }

    // The play button is honest this round: no game runtime is wired yet.
    val playToast = stringResource(R.string.play_not_wired)
    val playTaskTitle = stringResource(R.string.play_task_title)
    val importTaskTitle = stringResource(R.string.import_task_title)
    val importError = stringResource(R.string.error_import_file)

    fun play(instance: Instance) {
        Toast.makeText(context, playToast, Toast.LENGTH_SHORT).show()
        scope.launch {
            val taskId = container.tasks.start("$playTaskTitle: ${instance.name}")
            try {
                delay(2_500)
                container.tasks.finish(taskId, error = playToast)
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    // Modpack import: a real tracked task — stream-copy the archive with
    // progress, honouring pause and cancel, then create the instance and
    // open its editor so the user can name it and set version/loader/icon.
    suspend fun importPack(uri: Uri) {
        val name = importedPackName(uri)
        val target = newId() + ".zip"
        val taskId = container.tasks.start("$importTaskTitle: $name")
        try {
            val stored = withContext(Dispatchers.IO) {
                val out = container.media.file(target)
                val total = runCatching {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
                }.getOrNull() ?: -1L
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("unreadable")
                input.use { ins ->
                    FileOutputStream(out).use { outs ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L
                        while (true) {
                            // Suspends while paused, throws once cancelled.
                            container.tasks.step(taskId)
                            val read = ins.read(buffer)
                            if (read < 0) break
                            outs.write(buffer, 0, read)
                            copied += read
                            if (total > 0) {
                                container.tasks.report(taskId, copied.toFloat() / total)
                            }
                        }
                    }
                }
                target
            }
            val created = container.instances.create(
                Instance(
                    id = newId(),
                    name = name,
                    minecraftVersion = "1.21",
                    loader = LoaderKind.VANILLA,
                    importFileName = stored,
                ),
            )
            container.tasks.finish(taskId)
            stack = stack + Route.Editor(created.id, null)
        } catch (cancelled: CancellationException) {
            // Partial file is garbage; the task status is already CANCELLED.
            withContext(Dispatchers.IO) { container.media.delete(target) }
            throw cancelled
        } catch (e: Exception) {
            withContext(Dispatchers.IO) { container.media.delete(target) }
            container.tasks.finish(taskId, error = e.message ?: importError)
            Toast.makeText(context, importError, Toast.LENGTH_SHORT).show()
        }
    }

    EclipseTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val route = current) {
                Route.Home -> HomeScreen(
                    accountState = accountState,
                    instances = instances,
                    media = container.media,
                    onOpenAccounts = { push(Route.Accounts) },
                    onSelectAccount = { id -> scope.launch { container.accounts.select(id) } },
                    onCreateInstance = { push(Route.Create) },
                    onEditInstance = { push(Route.Editor(it.id, null)) },
                    onPlay = ::play,
                    onOpenFileManager = { push(Route.ManageContent) }, // File Manager opens Manage Content
                    onOpenManageLogs = { /* TODO: open manage logs */ },
                    onOpenSettings = { push(Route.Settings(initialPage = null)) },
                    onOpenTasks = { showTasks = true },
                    pendingSelectionId = pendingSelectionId,
                )

                Route.Accounts -> AccountsScreen(
                    accountState = accountState,
                    media = container.media,
                    onBack = { pop() },
                    onAddMicrosoft = { push(Route.MsAuth) },
                    onSelect = { id -> scope.launch { container.accounts.select(id) } },
                    onCreateLocal = { name ->
                        scope.launch {
                            container.accounts.add(
                                Account(
                                    id = newId(),
                                    kind = AccountKind.LOCAL,
                                    username = name,
                                    uuid = offlineUuid(name),
                                ),
                            )
                        }
                    },
                    onUpdate = { account -> scope.launch { container.accounts.update(account) } },
                    onRemove = { id -> scope.launch { container.accounts.remove(id) } },
                )

                Route.MsAuth -> MicrosoftAuthScreen(
                    onSignedIn = { account ->
                        scope.launch {
                            container.accounts.add(account)
                            // Success returns to the screen beneath: HOME
                            // or ACCOUNTS, depending on the entry point.
                            pop()
                        }
                    },
                    onCancel = { pop() },
                )

                Route.Create -> CreateInstanceScreen(
                    versions = container.versions,
                    onBack = { pop() },
                    onPickVersion = { mcVersion -> push(Route.LoaderPicker(mcVersion)) },
                    onModpack = { push(Route.ModpackBrowser) },
                )

                is Route.LoaderPicker -> LoaderPickerScreen(
                    mcVersion = route.mcVersion,
                    versions = container.versions,
                    onBack = { pop() },
                    onInstall = { loader, loaderVersion ->
                        // TODO(install): Install is recorded, not performed —
                        // this block is the hook the real installer plugs
                        // into. When the download pipeline lands, start a
                        // tracked task here (client jar, assets, libraries,
                        // natives and the loader's own install step) before
                        // the instance record is written, and keep the
                        // toast + return-home behaviour underneath it.
                        // The record itself stays exactly as it is today.
                        scope.launch {
                            val created = container.instances.create(
                                Instance(
                                    id = newId(),
                                    name = loaderNameString(loader) + " " + route.mcVersion,
                                    minecraftVersion = route.mcVersion,
                                    loader = loader,
                                    loaderVersion = loaderVersion,
                                ),
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.instance_created_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                            pendingSelectionId = created.id
                            stack = stack.dropLast(2)
                        }
                    },
                )

                Route.ModpackBrowser -> ModpackBrowserScreen(
                    onBack = { pop() },
                    onPickModpack = { uri -> scope.launch { importPack(uri) } },
                )

                is Route.Editor -> InstanceEditorScreen(
                    instanceId = route.instanceId,
                    presetLoader = route.presetLoader,
                    instances = instances,
                    media = container.media,
                    versions = container.versions,
                    controlmapsDir = container.controlmapsDir,
                    onSave = { instance ->
                        scope.launch {
                            if (instances.any { it.id == instance.id }) {
                                container.instances.update(instance)
                            } else {
                                container.instances.create(instance)
                            }
                            pop()
                        }
                    },
                    onDelete = { instance ->
                        scope.launch {
                            container.instances.remove(instance.id)
                            pop()
                        }
                    },
                    onDuplicate = { instance ->
                        scope.launch { container.instances.duplicate(instance.id) }
                    },
                    onBack = { pop() },
                )

                Route.ManageContent -> ManageContentScreen(
                    onBack = { pop() },
                )

                is Route.Settings -> SettingsScreen(
                    settings = container.settings,
                    initialPage = route.initialPage,
                    onBack = { pop() },
                )
            }

            // The tasks dialog is always composed; `visible` drives its
            // spring-open / corner-suck animation. Opening it first sweeps
            // finished tasks so the list is ongoing-only.
            TasksDialog(
                visible = showTasks,
                tasks = tasks,
                onPause = { id -> scope.launch { container.tasks.pause(id) } },
                onResume = { id -> scope.launch { container.tasks.resume(id) } },
                onCancel = { id -> scope.launch { container.tasks.cancel(id) } },
                onDismiss = { showTasks = false },
            )
            LaunchedEffect(showTasks) {
                if (showTasks) container.tasks.clearFinished()
            }
        }
    }
}

/** Best-effort display name for an imported modpack archive. */
private fun importedPackName(uri: android.net.Uri): String {
    val display = runCatching {
        uri.lastPathSegment
    }.getOrNull()
    val base = display
        ?.substringAfterLast('/')
        ?.substringBeforeLast('.')
        ?.replace('_', ' ')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    return base ?: "Imported pack"
}