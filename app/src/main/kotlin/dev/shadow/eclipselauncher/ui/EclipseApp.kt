/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.shadow.eclipselauncher.EclipseApplication
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.core.model.InstanceSummary
import dev.shadow.eclipselauncher.core.model.LoaderKind
import dev.shadow.eclipselauncher.data.ContentType
import dev.shadow.eclipselauncher.data.LogStore
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.EclipseTheme
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoTheme
import dev.shadow.eclipselauncher.host.GameActivity
import dev.shadow.eclipselauncher.host.InstallerActivity
import dev.shadow.eclipselauncher.ui.components.MojoAccountChrome
import dev.shadow.eclipselauncher.ui.components.MojoDialog
import dev.shadow.eclipselauncher.ui.components.MojoHomeAccountMenu
import dev.shadow.eclipselauncher.ui.components.TasksDialog
import dev.shadow.eclipselauncher.ui.screens.AccountsScreen
import dev.shadow.eclipselauncher.ui.screens.CustomControlsScreen
import dev.shadow.eclipselauncher.ui.screens.FabricLoaderScreen
import dev.shadow.eclipselauncher.ui.screens.FileManagerScreen
import dev.shadow.eclipselauncher.ui.screens.GamepadMapperScreen
import dev.shadow.eclipselauncher.ui.screens.HomeScreen
import dev.shadow.eclipselauncher.ui.screens.InstanceEditorScreen
import dev.shadow.eclipselauncher.ui.screens.ManageContentScreen
import dev.shadow.eclipselauncher.ui.screens.ManageLogsScreen
import dev.shadow.eclipselauncher.ui.screens.ModVersionLoaderScreen
import dev.shadow.eclipselauncher.ui.screens.ModpackSearchScreen
import dev.shadow.eclipselauncher.ui.screens.ProfileTypeScreen
import dev.shadow.eclipselauncher.ui.screens.SettingsPage
import dev.shadow.eclipselauncher.ui.screens.SettingsScreen
import dev.shadow.eclipselauncher.ui.screens.TextViewerScreen
import java.io.File

private sealed interface AppScreen {
    data object Home : AppScreen
    data object Accounts : AppScreen
    data object ProfileType : AppScreen
    data object InstanceEditor : AppScreen
    data class Loader(val kind: LoaderKind) : AppScreen
    data object ModpackSearch : AppScreen
    data class Settings(val page: SettingsPage?) : AppScreen
    data object CustomControls : AppScreen
    data object GamepadMapper : AppScreen
    data object Logs : AppScreen
    data class Files(val initialPath: String? = null) : AppScreen
    data class Content(
        val installed: Boolean = true,
        val type: ContentType = ContentType.MODS,
    ) : AppScreen

    data class Viewer(val path: String) : AppScreen
}

private fun encodeScreen(screen: AppScreen): String = when (screen) {
    AppScreen.Home -> "home"
    AppScreen.Accounts -> "accounts"
    AppScreen.ProfileType -> "profile-type"
    AppScreen.InstanceEditor -> "instance-editor"
    is AppScreen.Loader -> "loader:${screen.kind.name}"
    AppScreen.ModpackSearch -> "modpack-search"
    is AppScreen.Settings -> screen.page?.let { "settings:${it.name}" } ?: "settings"
    AppScreen.CustomControls -> "custom-controls"
    AppScreen.GamepadMapper -> "gamepad-mapper"
    AppScreen.Logs -> "logs"
    is AppScreen.Files -> screen.initialPath?.let { "files:$it" } ?: "files"
    is AppScreen.Content -> "content:${if (screen.installed) 1 else 0}:${screen.type.name}"
    is AppScreen.Viewer -> "viewer:${screen.path}"
}

private fun decodeScreen(value: String): AppScreen? {
    val separator = value.indexOf(':')
    val tag = if (separator >= 0) value.substring(0, separator) else value
    val rest = if (separator >= 0) value.substring(separator + 1) else ""

    return when (tag) {
        "home" -> AppScreen.Home
        "accounts" -> AppScreen.Accounts
        "profile-type" -> AppScreen.ProfileType
        "instance-editor" -> AppScreen.InstanceEditor
        "loader" -> LoaderKind.entries
            .firstOrNull { it.name == rest }
            ?.let { AppScreen.Loader(it) }
        "modpack-search" -> AppScreen.ModpackSearch
        "settings" -> AppScreen.Settings(
            SettingsPage.entries.firstOrNull { it.name == rest },
        )
        "custom-controls" -> AppScreen.CustomControls
        "gamepad-mapper" -> AppScreen.GamepadMapper
        "logs" -> AppScreen.Logs
        "files" -> AppScreen.Files(rest.takeIf { it.isNotEmpty() })
        "content" -> {
            val parts = rest.split(':', limit = 2)
            AppScreen.Content(
                installed = parts.getOrNull(0) != "0",
                type = ContentType.entries.firstOrNull { it.name == parts.getOrNull(1) }
                    ?: ContentType.MODS,
            )
        }
        "viewer" -> rest.takeIf { it.isNotEmpty() }?.let { AppScreen.Viewer(it) }
        else -> null
    }
}

@Composable
fun EclipseApp() {
    EclipseTheme {
        val context = LocalContext.current
        val container = (context.applicationContext as EclipseApplication).container
        val state = container.state
        val settings = container.mojoSettings
        val metadata = container.metadata
        val tasks = container.tasks
        val clipboard = container.fileClipboard
        val installer = container.installer

        val backStack = rememberSaveable(
            saver = listSaver<SnapshotStateList<AppScreen>, String>(
                save = { stack -> stack.map(::encodeScreen) },
                restore = { saved ->
                    mutableStateListOf<AppScreen>().apply {
                        saved.mapNotNullTo(this) { decodeScreen(it) }
                        if (isEmpty()) add(AppScreen.Home)
                    }
                },
            ),
        ) { mutableStateListOf<AppScreen>(AppScreen.Home) }
        val screen = backStack.last()
        var showTasks by remember { mutableStateOf(false) }
        var authError by remember { mutableStateOf<String?>(null) }

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

        fun startInstall(instance: InstanceSummary) {
            val started = installer.start(
                instance = instance,
                onInstalled = { state.markInstanceInstalled() },
            )
            if (started) showTasks = true
        }

        fun play() {
            if (tasks.activeCount() > 0) {
                Toast.makeText(
                    context,
                    context.getString(R.string.mojo_tasks_ongoing),
                    Toast.LENGTH_LONG,
                ).show()
                return
            }
            val instance = state.instance
            if (instance == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.mojo_no_instance),
                    Toast.LENGTH_LONG,
                ).show()
                return
            }
            if (!state.instanceInstalled) {
                startInstall(instance)
                return
            }
            if (instance.minecraftVersion.isBlank()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.mojo_error_no_version),
                    Toast.LENGTH_LONG,
                ).show()
                return
            }
            if (state.accountName.isBlank()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.mojo_no_saved_accounts),
                    Toast.LENGTH_LONG,
                ).show()
                navigate(AppScreen.Accounts)
                return
            }
            context.startActivity(Intent(context, GameActivity::class.java))
        }

        LaunchedEffect(Unit) {
            state.instance?.let { LogStore.bootstrapOnce(state.logsDir(it)) }
        }

        val jarPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
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

        if (screen == AppScreen.CustomControls) {
            CustomControlsScreen(
                state = state,
                settings = settings,
                onExit = ::goBack,
            )
        } else {
            when (val current = screen) {
                AppScreen.Home -> MojoHomeAccountMenu(
                    accounts = state.accounts,
                    onAddAccount = { navigate(AppScreen.Accounts) },
                    onSelectAccount = state::selectAccount,
                    onDeleteAccount = state::deleteAccount,
                ) { openAccountMenu ->
                    HomeScreen(
                        accountName = if (state.accounts.isEmpty()) {
                            "Add account"
                        } else {
                            state.accountName
                        },
                        taskCount = tasks.activeCount(),
                        instance = state.instance,
                        onAccountClick = openAccountMenu,
                        onTasksClick = { showTasks = true },
                        onSettingsClick = { navigate(AppScreen.Settings(null)) },
                        onControlsClick = { navigate(AppScreen.CustomControls) },
                        onLogsClick = { navigate(AppScreen.Logs) },
                        onFilesClick = { navigate(AppScreen.Files()) },
                        onContentClick = { navigate(AppScreen.Content()) },
                        onInstallJarClick = {
                            jarPicker.launch(arrayOf("application/java-archive"))
                        },
                        onCreateInstance = { navigate(AppScreen.ProfileType) },
                        onEditInstance = {
                            if (state.instance == null) {
                                navigate(AppScreen.ProfileType)
                            } else {
                                navigate(AppScreen.InstanceEditor)
                            }
                        },
                        onPlayClick = ::play,
                    )
                }
    
                else -> MojoChrome(
                    accountName = state.accountName,
                    accounts = state.accounts,
                    onSelectAccount = state::selectAccount,
                    onAddAccount = { navigate(AppScreen.Accounts) },
                    onDeleteAccount = state::deleteAccount,
                    onHome = ::goHome,
                ) {
                    when (current) {
                        AppScreen.Home -> Unit
    
                        AppScreen.Accounts -> AccountsScreen(
                            state = state,
                            onBack = ::goBack,
                            onDone = ::goHome,
                            onAuthError = { message, returnHome ->
                                if (returnHome) goHome()
                                authError = message.ifBlank {
                                    context.getString(R.string.mojo_unknown_authentication_error)
                                }
                            },
                        )
    
                        AppScreen.ProfileType -> ProfileTypeScreen(
                            onPick = { loader ->
                                when (loader) {
                                    LoaderKind.VANILLA -> {
                                        state.createInstance(LoaderKind.VANILLA)
                                        navigate(AppScreen.InstanceEditor)
                                    }
    
                                    LoaderKind.FABRIC,
                                    LoaderKind.QUILT,
                                    LoaderKind.LEGACY_FABRIC,
                                    LoaderKind.FORGE,
                                    LoaderKind.NEOFORGE,
                                    LoaderKind.OPTIFINE,
                                    LoaderKind.BTA -> navigate(AppScreen.Loader(loader))
    
                                    LoaderKind.MODPACK -> navigate(AppScreen.ModpackSearch)
                                }
                            },
                        )
    
                        AppScreen.InstanceEditor -> InstanceEditorScreen(
                            state = state,
                            metadata = metadata,
                            onSaved = { name, version, sharedData, jvmArgs, renderer, runtime, controlmap ->
                                val previous = state.instance
                                if (previous != null) {
                                    if (version.trim() != previous.minecraftVersion) {
                                        installer.cancel(previous)
                                    }
                                    if (name.trim().isNotEmpty() &&
                                        name.trim() != previous.name
                                    ) {
                                        state.renameInstanceDirectory(name)
                                    }
                                }
                                state.updateInstance(name, version)
                                state.updateInstanceOptions(
                                    sharedData = sharedData,
                                    jvmArgs = jvmArgs,
                                    renderer = renderer,
                                    runtime = runtime,
                                    controlmap = controlmap,
                                )
                                goHome()
                            },
                            onDeleted = {
                                val instance = state.instance
                                goBack()
                                instance?.let(installer::cancel)
                                state.deleteInstance()
                            },
                            onBack = ::goBack,
                        )
    
                        is AppScreen.Loader -> when (current.kind) {
                            LoaderKind.FABRIC,
                            LoaderKind.QUILT,
                            LoaderKind.LEGACY_FABRIC -> FabricLoaderScreen(current.kind)
    
                            else -> ModVersionLoaderScreen(current.kind)
                        }
    
                        AppScreen.ModpackSearch -> ModpackSearchScreen(
                            metadata = metadata,
                            versionsDir = File(state.minecraftDir, "versions"),
                        )
    
                        is AppScreen.Settings -> SettingsScreen(
                            page = current.page,
                            state = state,
                            settings = settings,
                            onBack = ::goBack,
                            onOpenPage = { page -> navigate(AppScreen.Settings(page)) },
                            onOpenCustomControls = { navigate(AppScreen.CustomControls) },
                            onOpenGamepadMapper = { navigate(AppScreen.GamepadMapper) },
                        )
    
                        AppScreen.GamepadMapper -> GamepadMapperScreen(
                            settings = settings,
                            onBack = ::goBack,
                        )
    
                        AppScreen.Logs -> ManageLogsScreen(
                            instance = state.instance,
                            logsDir = state.instance?.let { state.logsDir(it) },
                            clipboard = clipboard,
                            onBack = ::goBack,
                            onOpenFile = { file -> navigate(AppScreen.Viewer(file.absolutePath)) },
                        )
    
                        is AppScreen.Files -> FileManagerScreen(
                            root = state.minecraftDir,
                            initialDir = current.initialPath?.let(::File),
                            clipboard = clipboard,
                            onBack = ::goBack,
                            onDirectoryChanged = { directory ->
                                val target = backStack.last()
                                if (target is AppScreen.Files &&
                                    target.initialPath != directory.absolutePath
                                ) {
                                    backStack[backStack.lastIndex] =
                                        target.copy(initialPath = directory.absolutePath)
                                }
                            },
                            onOpenFile = { file ->
                                navigate(AppScreen.Viewer(file.absolutePath))
                            },
                        )
    
                        is AppScreen.Content -> ManageContentScreen(
                            instance = state.instance,
                            state = state,
                            initialInstalled = current.installed,
                            initialType = current.type,
                            onBack = ::goBack,
                            onStateChanged = { installed, type ->
                                val target = backStack.last()
                                if (target is AppScreen.Content &&
                                    (target.installed != installed || target.type != type)
                                ) {
                                    backStack[backStack.lastIndex] =
                                        target.copy(installed = installed, type = type)
                                }
                            },
                            onOpenFile = { file ->
                                navigate(AppScreen.Viewer(file.absolutePath))
                            },
                            onOpenDirectory = { directory ->
                                navigate(AppScreen.Files(directory.absolutePath))
                            },
                        )
    
                        is AppScreen.Viewer -> TextViewerScreen(
                            file = File(current.path),
                            onBack = ::goBack,
                        )
    
                        AppScreen.CustomControls -> Unit
                    }
                }
            }
        }

        if (showTasks) {
            MojoTheme {
                TasksDialog(tasks = tasks, onDismiss = { showTasks = false })
            }
        }

        authError?.let { message ->
            MojoTheme {
                MojoDialog(
                    onDismiss = { authError = null },
                    confirmLabel = stringResource(R.string.mojo_ok),
                    onConfirm = { authError = null },
                    title = stringResource(R.string.mojo_error),
                    message = message,
                )
            }
        }
    }
}

/**
 * The reference launcher keeps its account strip and the 52dp action button
 * above every fragment. The custom-control editor is a separate full-screen
 * activity and bypasses this wrapper.
 */
@Composable
private fun MojoChrome(
    accountName: String,
    accounts: List<String>,
    onSelectAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    onDeleteAccount: (String) -> Unit,
    onHome: () -> Unit,
    content: @Composable () -> Unit,
) {
    MojoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background),
        ) {
            MojoAccountChrome(
                accountName = if (accounts.isEmpty()) {
                    "Add account"
                } else {
                    accountName
                },
                accounts = accounts,
                onSelectAccount = onSelectAccount,
                onAddAccount = onAddAccount,
                onDeleteAccount = onDeleteAccount,
                onChromeAction = onHome,
                actionIcon = EclipsePixelIconKind.HOME,
                actionDescription = "Home",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                content()
            }
        }
    }
}
