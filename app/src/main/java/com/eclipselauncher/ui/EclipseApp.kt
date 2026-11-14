/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The app root: dependency access and the Navigation 3 back stack (HOME at the
 * root plus overlay routes for ACCOUNTS, Microsoft sign-in, instance creation,
 * the instance editor, UTILS and SETTINGS — there is no bottom navigation bar;
 * HOME's top-right buttons open the latter two). The tasks dialog floats
 * above everything, and play holds an honest stop: no runtime is wired yet.
 */
package com.eclipselauncher.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.eclipselauncher.EclipseContainer
import com.eclipselauncher.R
import com.eclipselauncher.data.Account
import com.eclipselauncher.data.AccountKind
import com.eclipselauncher.data.Instance
import com.eclipselauncher.data.LoaderKind
import com.eclipselauncher.data.newId
import com.eclipselauncher.data.offlineUuid
import com.eclipselauncher.ui.components.TasksDialog
import com.eclipselauncher.ui.components.loaderNameString
import com.eclipselauncher.ui.screens.AccountsScreen
import com.eclipselauncher.ui.screens.CreateInstanceScreen
import com.eclipselauncher.ui.screens.HomeScreen
import com.eclipselauncher.ui.screens.InstanceEditorScreen
import com.eclipselauncher.ui.screens.LoaderPickerScreen
import com.eclipselauncher.ui.screens.MicrosoftAuthScreen
import com.eclipselauncher.ui.screens.ModrinthClient
import com.eclipselauncher.ui.screens.ModrinthHit
import com.eclipselauncher.ui.screens.ContentType
import com.eclipselauncher.ui.screens.Provider
import com.eclipselauncher.ui.screens.InstalledContent
import com.eclipselauncher.ui.screens.ContentSegmentedControl
import com.eclipselauncher.ui.screens.CollapsibleFilterPanel
import com.eclipselauncher.ui.screens.ContentTypeChip
import com.eclipselauncher.ui.screens.ContentIcon
import com.eclipselauncher.ui.screens.InstalledContentRow
import com.eclipselauncher.ui.screens.BrowseContentRow
import com.eclipselauncher.ui.screens.ProviderSegmentedControl
import com.eclipselauncher.ui.screens.SearchField
import com.eclipselauncher.ui.screens.PaginationPill
import com.eclipselauncher.ui.screens.ModpackBrowserScreen
import com.eclipselauncher.ui.screens.SettingsPage
import com.eclipselauncher.ui.screens.SettingsScreen
import com.eclipselauncher.ui.theme.EclipseTheme
import dagger.hilt.EntryPoints
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** Destinations: HOME plus the overlay screens. */
@Serializable
private sealed interface Route : NavKey {
    @Serializable
    data object Home : Route

    @Serializable
    data object Accounts : Route

    @Serializable
    data object MsAuth : Route

    @Serializable
    data object Create : Route

    /** Step 2 of creation: the six loaders for one Minecraft version. */
    @Serializable
    data class LoaderPicker(val mcVersion: String) : Route

    @Serializable
    data object ModpackBrowser : Route

    @Serializable
    data object ManageContent : Route

    @Serializable
    data class Editor(val instanceId: String?, val presetLoader: LoaderKind?) : Route

    @Serializable
    data class Settings(val initialPage: SettingsPage?) : Route
}

@Composable
fun EclipseApp() {
    val context = LocalContext.current
    // The process-wide Hilt graph is created by the Application.
    val container = remember {
        EntryPoints.get(context.applicationContext, EclipseContainer::class.java)
    }
    val scope = rememberCoroutineScope()

    val accountState by container.accounts.state.collectAsState()
    val instances by container.instances.state.collectAsState()
    val tasks by container.tasks.state.collectAsState()

    val backStack = remember { NavBackStack<Route>(Route.Home) }

    // Task dialog visibility.
    var showTasks by remember { mutableStateOf(false) }

    // Instance HOME should select after a create; consumed once applied.
    var pendingSelectionId by remember { mutableStateOf<String?>(null) }

    fun push(route: Route) {
        backStack.add(route)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    // The play button is honest this round: no game runtime is wired yet.
    val playToast = stringResource(R.string.play_not_wired)
    val playTaskTitle = stringResource(R.string.play_task_title)
    val importTaskTitle = stringResource(R.string.import_task_title)
    val importError = stringResource(R.string.error_import_file)
    val instanceCreatedToast = stringResource(R.string.instance_created_toast)

    // Theme: dynamic (Material You) by default; readable from SettingsStore.
    val themeMode = container.settings.themeMode()

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
            backStack.add(Route.Editor(created.id, null))
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

    EclipseTheme(themeMode = themeMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                onBack = ::pop,
                entryProvider = entryProvider {
                    entry<Route.Home> {
                        HomeScreen(
                            accountState = accountState,
                            instances = instances,
                            media = container.media,
                            onOpenAccounts = { push(Route.Accounts) },
                            onSelectAccount = { id -> scope.launch { container.accounts.select(id) } },
                            onCreateInstance = { push(Route.Create) },
                            onEditInstance = { push(Route.Editor(it.id, null)) },
                            onPlay = ::play,
                            onOpenFileManager = { push(Route.ManageContent) },
                            onOpenManageLogs = { /* TODO: open manage logs */ },
                            onOpenSettings = { push(Route.Settings(initialPage = null)) },
                            onOpenTasks = { showTasks = true },
                            pendingSelectionId = pendingSelectionId,
                        )
                    }

                    entry<Route.Accounts> {
                        AccountsScreen(
                            accountState = accountState,
                            media = container.media,
                            onBack = ::pop,
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
                    }

                    entry<Route.MsAuth> {
                        MicrosoftAuthScreen(
                            auth = container.auth,
                            onSignedIn = { account ->
                                scope.launch {
                                    container.accounts.add(account)
                                    // Success returns to the screen beneath: HOME
                                    // or ACCOUNTS, depending on the entry point.
                                    pop()
                                }
                            },
                            onCancel = ::pop,
                        )
                    }

                    entry<Route.Create> {
                        CreateInstanceScreen(
                            versions = container.versions,
                            onBack = ::pop,
                            onPickVersion = { mcVersion -> push(Route.LoaderPicker(mcVersion)) },
                            onModpack = { push(Route.ModpackBrowser) },
                        )
                    }

                    entry<Route.LoaderPicker> { route ->
                        LoaderPickerScreen(
                            mcVersion = route.mcVersion,
                            versions = container.versions,
                            onBack = ::pop,
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
                                        instanceCreatedToast,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    pendingSelectionId = created.id
                                    repeat(2) { pop() }
                                }
                            },
                        )
                    }

                    entry<Route.ModpackBrowser> {
                        ModpackBrowserScreen(
                            client = container.modrinth,
                            onBack = ::pop,
                            onPickModpack = { uri -> scope.launch { importPack(uri) } },
                        )
                    }

                    entry<Route.Editor> { route ->
                        InstanceEditorScreen(
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
                            onBack = ::pop,
                        )
                    }

                    entry<Route.ManageContent> {
                        ManageContentScreen(
                            client = container.modrinth,
                            onBack = ::pop,
                        )
                    }

                    entry<Route.Settings> { route ->
                        SettingsScreen(
                            settings = container.settings,
                            initialPage = route.initialPage,
                            onBack = ::pop,
                        )
                    }
                },
            )

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

/** MANAGE CONTENT SCREEN (inlined to avoid cross-source-set resolution issues) */
@Composable
private fun ManageContentScreen(
    client: ModrinthClient,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    var mainTab by remember { mutableStateOf(0) } // 0 = Installed, 1 = Browse
    var installedContentType by remember { mutableStateOf(ContentType.MODS) }
    var browseContentType by remember { mutableStateOf(ContentType.MODS) }
    var browseProvider by remember { mutableStateOf(Provider.MODRINTH) }
    var browseQuery by remember { mutableStateOf("") }
    var browseResults by remember { mutableStateOf<List<ModrinthHit>>(emptyList()) }
    var browseLoading by remember { mutableStateOf(false) }
    var browseError by remember { mutableStateOf<String?>(null) }
    var browsePage by remember { mutableStateOf(1) }
    var browseTotalPages by remember { mutableStateOf(1) }

    // Collapse state remembered per tab
    var installedTypeCollapsed by remember { mutableStateOf(false) }
    var browseTypeCollapsed by remember { mutableStateOf(false) }
    var pageNavCollapsed by remember { mutableStateOf(false) }

    // Mock installed content
    val installedContent = remember {
        listOf(
            InstalledContent("1", "Sodium", "0.5.8", ContentType.MODS, null, true),
            InstalledContent("2", "Iris Shaders", "1.7.4", ContentType.MODS, null, true),
            InstalledContent("3", "Fabric API", "0.96.0", ContentType.MODS, null, true),
            InstalledContent("4", "Vanilla Resource Pack", "1.21", ContentType.RESOURCE_PACKS, null, true),
            InstalledContent("5", "BSL Shaders", "8.2.01", ContentType.SHADERS, null, true),
            InstalledContent("6", "My World", "1.0", ContentType.WORLDS, null, true),
        )
    }

    // Pre-load strings for use in non-composable callbacks
    val manageContentTitle = stringResource(R.string.manage_content_title)
    val manageContentImport = stringResource(R.string.manage_content_import)
    val manageContentImportStarted = stringResource(R.string.manage_content_import_started)
    val providerComingSoonStr = stringResource(R.string.provider_coming_soon)
    val marketErrorStr = stringResource(R.string.market_error)

    // Import .zip launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(context, manageContentImportStarted, Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-load browse content on initial load and when content type/page/query changes
    LaunchedEffect(browseContentType, browseProvider, browsePage, browseQuery) {
        if (mainTab == 1) {
            scope.launch {
                browseLoading = true
                browseError = null
                val effectiveQuery = if (browseQuery.isNotBlank()) browseQuery else ""
                val offset = (browsePage - 1) * 25
                val result = client.searchMods(
                    query = effectiveQuery,
                    limit = 25,
                    offset = offset,
                    projectType = browseContentType.modrinthProjectType,
                )
                if (result.totalPages > 0 && result.hits.isNotEmpty()) {
                    browseResults = result.hits
                    browseTotalPages = result.totalPages
                    browsePage = result.pageNumber
                    browseLoading = false
                } else {
                    browseError = "Failed to load results"
                    browseLoading = false
                }
            }
        }
    }

    // Scroll to top when page changes
    LaunchedEffect(browsePage) {
        if (mainTab == 1 && !browseLoading) {
            lazyListState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(manageContentTitle) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            },
            actions = {
                IconButton(onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = stringResource(R.string.manage_content_import), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        // Segmented control: Installed | Browse
        ContentSegmentedControl(
            selectedIndex = mainTab,
            onTabSelected = { mainTab = it },
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))

        // Collapsible filter panel with content-type chips + drag handle
        val activeContentType = if (mainTab == 0) installedContentType else browseContentType
        val activeCollapsed = if (mainTab == 0) installedTypeCollapsed else browseTypeCollapsed
        val onCollapseToggle: () -> Unit = if (mainTab == 0) { { installedTypeCollapsed = !installedTypeCollapsed } } else { { browseTypeCollapsed = !browseTypeCollapsed } }
        val onTypeClick: (ContentType) -> Unit = if (mainTab == 0) { { installedContentType = it } } else { { browseContentType = it } }
        val onDrag = { dragAmount: Offset ->
            // Drag vertically to collapse/expand (threshold-based)
            if (dragAmount.y > 20 && !activeCollapsed) {
                onCollapseToggle()
            } else if (dragAmount.y < -20 && activeCollapsed) {
                onCollapseToggle()
            }
        }

        CollapsibleFilterPanel(
            contentType = activeContentType,
            onContentTypeClick = onTypeClick,
            installedContent = installedContent,
            isInstalledTab = mainTab == 0,
            collapsed = activeCollapsed,
            onCollapseToggle = onCollapseToggle,
            onDrag = onDrag,
        )

        // Search field (full-width, leading icon, clear button, keyboard search action)
        SearchField(
            query = browseQuery,
            onQueryChange = { browseQuery = it },
            onSearch = {
                if (mainTab == 1) {
                    scope.launch {
                        browseLoading = true
                        browseError = null
                        browsePage = 1 // Reset to first page on new search
                        val offset = 0
                        val result = client.searchMods(
                            query = browseQuery,
                            limit = 25,
                            offset = offset,
                            projectType = browseContentType.modrinthProjectType,
                        )
                        if (result.totalPages > 0 && result.hits.isNotEmpty()) {
                            browseResults = result.hits
                            browseTotalPages = result.totalPages
                            browsePage = result.pageNumber
                            browseLoading = false
                        } else {
                            browseError = "Failed to load results"
                            browseLoading = false
                        }
                    }
                }
            },
            onClear = { browseQuery = "" },
        )

        // Source selector (Browse only): full-width segmented control below search
        if (mainTab == 1) {
            ProviderSegmentedControl(
                selectedProvider = browseProvider,
                onProviderChange = { browseProvider = it },
                context = context,
                providerComingSoonStr = providerComingSoonStr,
            )
        }

        // Content list
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (mainTab) {
                0 -> {
                    val filteredContent = installedContent.filter { it.type == installedContentType }
                    when {
                        filteredContent.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.manage_content_empty),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        else -> LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp), // Space for pagination pill (not shown on Installed)
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredContent, key = { it.id }) { content ->
                                InstalledContentRow(
                                    content = content,
                                    onUpdate = { /* open version dialog */ },
                                    onToggle = { /* toggle enabled */ },
                                    onDelete = { /* delete with confirmation */ },
                                )
                            }
                        }
                    }
                }
                1 -> {
                    val listPaddingBottom = if (browseTotalPages > 1) 100.dp else 80.dp // Extra space for pagination pill
                    when {
                        browseLoading && browseResults.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary) }

                        browseError != null -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { Text(text = browseError ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }

                        browseResults.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { Text(text = stringResource(R.string.manage_content_empty), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }

                        else -> LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = listPaddingBottom),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(browseResults, key = { it.project_id }) { hit ->
                                BrowseContentRow(
                                    hit = hit,
                                    onClick = { /* open mod detail */ },
                                    onDownload = { /* open versions dialog */ },
                                )
                            }
                        }
                    }
                }
            }
            Unit // Explicit Unit to satisfy Box content lambda
        }

        // Floating pagination pill (Browse only, when multiple pages)
        if (mainTab == 1 && browseTotalPages > 1) {
            PaginationPill(
                currentPage = browsePage,
                totalPages = browseTotalPages,
                loading = browseLoading,
                collapsed = pageNavCollapsed,
                onCollapseToggle = { pageNavCollapsed = !pageNavCollapsed },
                onPrev = { if (browsePage > 1) browsePage-- },
                onNext = { if (browsePage < browseTotalPages) browsePage++ },
                onPageChange = { /* triggers LaunchedEffect to scroll to top */ },
            )
        }
    }
}
}
