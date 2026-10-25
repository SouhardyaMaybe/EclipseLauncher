/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.shadow.eclipselauncher.EclipseApplication
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.core.model.LoaderKind
import dev.shadow.eclipselauncher.data.ContentType
import dev.shadow.eclipselauncher.data.LogStore
import dev.shadow.eclipselauncher.designsystem.theme.EclipseTheme
import dev.shadow.eclipselauncher.host.InstallerActivity
import dev.shadow.eclipselauncher.ui.components.InstanceEditorDialog
import dev.shadow.eclipselauncher.ui.components.MessageDialog
import dev.shadow.eclipselauncher.ui.components.TasksDialog
import dev.shadow.eclipselauncher.ui.components.displayName
import dev.shadow.eclipselauncher.ui.screens.AccountsScreen
import dev.shadow.eclipselauncher.ui.screens.ControlsScreen
import dev.shadow.eclipselauncher.ui.screens.CreateInstanceScreen
import dev.shadow.eclipselauncher.ui.screens.FileManagerScreen
import dev.shadow.eclipselauncher.ui.screens.HomeScreen
import dev.shadow.eclipselauncher.ui.screens.ManageContentScreen
import dev.shadow.eclipselauncher.ui.screens.ManageLogsScreen
import dev.shadow.eclipselauncher.ui.screens.SettingsScreen
import dev.shadow.eclipselauncher.ui.screens.TextViewerScreen
import java.io.File

private sealed interface AppScreen {
    data object Home : AppScreen
    data object CreateInstance : AppScreen
    data object Controls : AppScreen
    data object Logs : AppScreen
    data class Files(val initialPath: String? = null) : AppScreen
    data class Content(
        val installed: Boolean = true,
        val type: ContentType = ContentType.MODS,
    ) : AppScreen

    data object Settings : AppScreen
    data object Accounts : AppScreen
    data class Viewer(val path: String) : AppScreen
}

@Composable
fun EclipseApp() {
    EclipseTheme {
        val context = LocalContext.current
        val container = (context.applicationContext as EclipseApplication).container
        val state = container.state
        val tasks = container.tasks
        val clipboard = container.fileClipboard

        val backStack = remember { mutableStateListOf<AppScreen>(AppScreen.Home) }
        val screen = backStack.last()
        var showTasks by remember { mutableStateOf(false) }
        var showPlayNotice by remember { mutableStateOf(false) }
        var showJarNotice by remember { mutableStateOf(false) }
        var pendingLoader by remember { mutableStateOf<LoaderKind?>(null) }
        var editingInstance by remember { mutableStateOf(false) }

        fun navigate(next: AppScreen) {
            backStack.add(next)
        }

        fun goBack() {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        }

        fun goHome() {
            backStack.clear()
            backStack.add(AppScreen.Home)
        }

        LaunchedEffect(Unit) {
            state.instance?.let { LogStore.bootstrapOnce(state.logsDir(it)) }
        }

        val jarPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult

            val displayName = queryDisplayName(context, uri)
            if (displayName == null || !displayName.endsWith(".jar", ignoreCase = true)) {
                showJarNotice = true
                return@rememberLauncherForActivityResult
            }

            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            context.startActivity(
                Intent(context, InstallerActivity::class.java).apply {
                    putExtra("modUri", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
            )
        }

        BackHandler(enabled = backStack.size > 1) {
            goBack()
        }

        when (val current = screen) {
            AppScreen.Home -> HomeScreen(
                accountName = state.accountName,
                taskCount = tasks.activeCount(),
                instance = state.instance,
                onAccountClick = { navigate(AppScreen.Accounts) },
                onTasksClick = { showTasks = true },
                onSettingsClick = { navigate(AppScreen.Settings) },
                onControlsClick = { navigate(AppScreen.Controls) },
                onLogsClick = { navigate(AppScreen.Logs) },
                onFilesClick = { navigate(AppScreen.Files()) },
                onContentClick = { navigate(AppScreen.Content()) },
                onInstallJarClick = { jarPicker.launch(arrayOf("*/*")) },
                onCreateInstance = { navigate(AppScreen.CreateInstance) },
                onEditInstance = {
                    if (state.instance == null) {
                        navigate(AppScreen.CreateInstance)
                    } else {
                        editingInstance = true
                    }
                },
                onPlayClick = { showPlayNotice = true },
            )

            AppScreen.CreateInstance -> CreateInstanceScreen(
                onBack = { goBack() },
                onPick = { loader ->
                    when (loader) {
                        LoaderKind.QUILT,
                        LoaderKind.FORGE,
                        LoaderKind.NEOFORGE,
                        LoaderKind.OPTIFINE -> pendingLoader = loader

                        LoaderKind.MODPACK -> {
                            navigate(
                                AppScreen.Content(
                                    installed = false,
                                    type = ContentType.MODPACKS,
                                ),
                            )
                        }

                        LoaderKind.VANILLA,
                        LoaderKind.FABRIC -> {
                            state.createInstance(loader)
                            goHome()
                        }
                    }
                },
            )

            AppScreen.Controls -> ControlsScreen(onBack = { goBack() })

            AppScreen.Logs -> ManageLogsScreen(
                instance = state.instance,
                logsDir = state.instance?.let { state.logsDir(it) },
                clipboard = clipboard,
                onBack = { goBack() },
                onOpenFile = { file -> navigate(AppScreen.Viewer(file.absolutePath)) },
            )

            is AppScreen.Files -> FileManagerScreen(
                root = state.minecraftDir,
                initialDir = current.initialPath?.let(::File),
                clipboard = clipboard,
                onBack = { goBack() },
                onOpenFile = { file -> navigate(AppScreen.Viewer(file.absolutePath)) },
            )

            is AppScreen.Content -> ManageContentScreen(
                instance = state.instance,
                state = state,
                initialInstalled = current.installed,
                initialType = current.type,
                onBack = { goBack() },
                onOpenFile = { file -> navigate(AppScreen.Viewer(file.absolutePath)) },
                onOpenDirectory = { directory ->
                    navigate(AppScreen.Files(directory.absolutePath))
                },
            )

            AppScreen.Settings -> SettingsScreen(
                state = state,
                onBack = { goBack() },
                onOpenAccounts = { navigate(AppScreen.Accounts) },
            )

            AppScreen.Accounts -> AccountsScreen(
                state = state,
                onBack = { goBack() },
            )

            is AppScreen.Viewer -> TextViewerScreen(
                file = File(current.path),
                onBack = { goBack() },
            )
        }

        if (showTasks) {
            TasksDialog(tasks = tasks, onDismiss = { showTasks = false })
        }

        if (showPlayNotice) {
            MessageDialog(
                title = stringResource(R.string.home_play_unavailable_title),
                body = stringResource(R.string.home_play_unavailable_body),
                onDismiss = { showPlayNotice = false },
            )
        }

        if (showJarNotice) {
            MessageDialog(
                title = stringResource(R.string.home_jar_wrong_type_title),
                body = stringResource(R.string.home_jar_wrong_type_body),
                onDismiss = { showJarNotice = false },
            )
        }

        pendingLoader?.let { loader ->
            val loaderName = loader.displayName()
            MessageDialog(
                title = stringResource(R.string.create_instance_coming_soon_title, loaderName),
                body = stringResource(R.string.create_instance_coming_soon_body, loaderName),
                onDismiss = { pendingLoader = null },
            )
        }

        if (editingInstance) {
            state.instance?.let { instance ->
                InstanceEditorDialog(
                    instance = instance,
                    onDismiss = { editingInstance = false },
                    onSave = { name, version ->
                        state.updateInstance(name, version)
                        editingInstance = false
                    },
                )
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return@use cursor.getString(index)
            }
            null
        }
    }.getOrNull() ?: uri.lastPathSegment
