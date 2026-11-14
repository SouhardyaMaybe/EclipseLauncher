/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * INSTANCE EDITOR: edit an existing instance (the create flow now finishes on
 * the version picker, so this screen is reached from HOME's edit action or
 * after an import). The icon is centred with the loader glyph as the default
 * and a pencil overlay to pick a custom one (with Reset once changed); the
 * version line sits under it, then name, controlmap, JVM args, runtime and
 * renderer. There is no bottom save: the top bar carries Tick (save) and
 * Cross (cancel), greyed out until something actually changes. Edit mode also
 * offers Copy and Delete, each toasting its outcome.
 */
package me.shadow.eclipselauncher.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.data.ImportKind
import me.shadow.eclipselauncher.data.Instance
import me.shadow.eclipselauncher.data.LoaderKind
import me.shadow.eclipselauncher.data.MediaStorage
import me.shadow.eclipselauncher.data.Renderers
import me.shadow.eclipselauncher.data.newId
import me.shadow.eclipselauncher.ui.components.DropdownSelectField
import me.shadow.eclipselauncher.ui.components.InstanceIcon
import me.shadow.eclipselauncher.ui.components.LoaderIcon
import me.shadow.eclipselauncher.ui.components.SelectField
import me.shadow.eclipselauncher.ui.components.TextFieldDialog
import me.shadow.eclipselauncher.ui.components.loaderName
import me.shadow.eclipselauncher.ui.components.loaderNameString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstanceEditorScreen(
    instanceId: String?,
    presetLoader: LoaderKind?,
    instances: List<Instance>,
    media: MediaStorage,
    versions: me.shadow.eclipselauncher.data.VersionRepository,
    controlmapsDir: File,
    onSave: (Instance) -> Unit,
    onDelete: (Instance) -> Unit,
    onDuplicate: (Instance) -> Unit,
    onBack: () -> Unit,
) {
    val existing = instances.firstOrNull { it.id == instanceId }
    val isEditing = existing != null

    val effectiveLoader = existing?.loader ?: presetLoader ?: LoaderKind.VANILLA
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var nameEdited by remember { mutableStateOf(existing != null) }
    var iconFileName by remember { mutableStateOf(existing?.iconFileName) }
    var jvmArgs by remember { mutableStateOf(existing?.jvmArgs ?: "") }
    var renderer by remember { mutableStateOf(existing?.renderer ?: "") }
    var runtime by remember { mutableStateOf(existing?.runtime ?: "") }
    var controlmap by remember { mutableStateOf(existing?.controlmap ?: "") }
    var sharedData by remember { mutableStateOf(existing?.sharedData ?: false) }

    var showJvmArgs by remember { mutableStateOf(false) }
    var jvmArgsDraft by remember { mutableStateOf(jvmArgs) }
    var showDelete by remember { mutableStateOf(false) }

    // Control layouts on disk for the dropdown: every .json in the
    // instance's controlmap directory, global default first.
    var controlmaps by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(controlmapsDir) {
        controlmaps = withContext(Dispatchers.IO) {
            controlmapsDir.mkdirs()
            controlmapsDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".json") }
                ?.map { it.name }
                ?.sorted()
                .orEmpty()
        }
    }

    // The name follows the loader + version until the user edits it by hand,
    // so "Vanilla 1.21.4" appears without typing and a hand edit sticks.
    LaunchedEffect(existing?.id, nameEdited) {
        if (!nameEdited) {
            name = loaderNameString(effectiveLoader) + " " + (existing?.minecraftVersion ?: "")
        }
    }

    // Nothing to save (or cancel) while the form still matches the instance.
    val hasChanges = existing == null ||
        name != existing.name ||
        iconFileName != existing.iconFileName ||
        jvmArgs != existing.jvmArgs ||
        renderer != existing.renderer ||
        runtime != existing.runtime ||
        controlmap != existing.controlmap ||
        sharedData != existing.sharedData

    // Toast messages pre-loaded for callbacks (must be before launchers that use them)
    val errorMediaImport = stringResource(R.string.error_media_import)
    val instanceCopiedToast = stringResource(R.string.instance_copied_toast)
    val instanceCreatedToast = stringResource(R.string.instance_created_toast)
    val instanceDeletedToast = stringResource(R.string.instance_deleted_toast)

    val pickIcon = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val target = "icon_${newId()}.png"
                val stored = media.importImage(target, uri, ImportKind.INSTANCE_ICON)
                if (stored != null) {
                    iconFileName = stored
                } else {
                    Toast.makeText(
                        context,
                        errorMediaImport,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    fun draftInstance(): Instance = Instance(
        id = existing?.id ?: newId(),
        name = name.trim().ifEmpty { "Instance" },
        minecraftVersion = existing?.minecraftVersion ?: "1.21",
        loader = effectiveLoader,
        loaderVersion = existing?.loaderVersion ?: "",
        iconFileName = iconFileName,
        importFileName = existing?.importFileName,
        jvmArgs = jvmArgs,
        renderer = renderer,
        runtime = runtime,
        controlmap = controlmap,
        sharedData = sharedData,
    )

    // Version line: "Minecraft Version: <loader> <version> <loaderVersion>".
    // The loader name is skipped for Vanilla and the loader version only shows
    // when the loader actually has one.
    val versionLabel = stringResource(R.string.field_minecraft_version)
    val loaderLabel = if (effectiveLoader != LoaderKind.VANILLA) loaderName(effectiveLoader) else ""
    val mcVersion = existing?.minecraftVersion ?: ""
    val loaderVersion = existing?.loaderVersion ?: ""
    val versionLine = (listOf(versionLabel + ":") +
        listOfNotNull(loaderLabel.ifBlank { null }) +
        listOfNotNull(mcVersion.ifBlank { null }) +
        listOfNotNull(
            loaderVersion
                .takeIf { effectiveLoader != LoaderKind.VANILLA && it.isNotBlank() },
        )
        ).joinToString(" ")

    // Dropdown option tables: control maps (files + global default),
    // runtime (Auto is the only entry), renderers (all + Global last).
    val controlmapOptions = listOf(stringResource(R.string.hint_controlmap_default)) + controlmaps
    val controlmapIndex = if (controlmap.isBlank()) {
        0
    } else {
        controlmaps.indexOf(controlmap.substringAfterLast('/')).takeIf { it >= 0 }?.plus(1) ?: 0
    }
    val runtimeOptions = listOf(stringResource(R.string.hint_runtime_auto))
    val rendererOptions = Renderers.NAMES + stringResource(R.string.hint_renderer_global)
    val rendererIndex = if (renderer.isBlank()) {
        rendererOptions.lastIndex
    } else {
        Renderers.IDS.indexOf(renderer).takeIf { it >= 0 } ?: rendererOptions.lastIndex
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) {
                            stringResource(R.string.editor_edit_title)
                        } else {
                            stringResource(R.string.editor_new_title)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    // Copy and Delete only make sense for a saved instance.
                    if (isEditing) {
                        IconButton(
                            onClick = {
                                onDuplicate(requireNotNull(existing))
                                Toast.makeText(
                                    context,
                                    instanceCopiedToast,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.action_copy),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    // Cross = cancel: leaves without saving, as the back arrow does.
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Tick = save; greyed until something actually changed.
                    IconButton(
                        onClick = {
                            val instance = draftInstance()
                            onSave(instance)
                            if (!isEditing) {
                                Toast.makeText(
                                    context,
                                    instanceCreatedToast,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        enabled = name.isNotBlank() && hasChanges,
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = stringResource(R.string.action_save),
                            tint = if (name.isNotBlank() && hasChanges) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Centred icon: loader glyph by default, custom image when picked.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(120.dp)) {
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (iconFileName != null) {
                            InstanceIcon(
                                instance = draftInstance(),
                                media = media,
                                size = 120.dp,
                            )
                        } else {
                            LoaderIcon(loader = effectiveLoader, size = 120.dp)
                        }
                    }

                    // Pencil overlay: bottom-right corner of the icon.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                    ) {
                        Card(
                            shape = RoundedCornerShape(50),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        ) {
                            IconButton(onClick = { pickIcon.launch(arrayOf("image/png")) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.cd_pick_icon),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp).padding(8.dp),
                                )
                            }
                        }
                    }

                    // Reset only exists once a custom icon replaced the glyph.
                    if (iconFileName != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                        ) {
                            OutlinedButton(
                                onClick = { iconFileName = null },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text(
                                    text = stringResource(R.string.action_reset_icon),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }

            if (isEditing && versionLine.isNotBlank()) {
                Text(
                    text = versionLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(48)
                    nameEdited = true
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_name)) },
                placeholder = { Text(stringResource(R.string.hint_instance_name)) },
                singleLine = true,
            )

            DropdownSelectField(
                label = stringResource(R.string.field_controlmap),
                value = controlmapDisplay(controlmap),
                hint = stringResource(R.string.hint_controlmap_default),
                options = controlmapOptions,
                selectedIndex = controlmapIndex,
                onSelect = { index ->
                    controlmap = if (index == 0) "" else "./" + controlmaps[index - 1]
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = sharedData,
                        role = Role.Checkbox,
                        onValueChange = { sharedData = it },
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.field_shared_data),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = sharedData,
                    onCheckedChange = { sharedData = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.secondary,
                        checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
            Text(
                text = stringResource(
                    if (sharedData) R.string.shared_data_shared else R.string.shared_data_instance,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            SelectField(
                label = stringResource(R.string.field_jvm_args),
                value = jvmArgs,
                hint = stringResource(R.string.hint_jvm_args_default),
                onClick = {
                    jvmArgsDraft = jvmArgs
                    showJvmArgs = true
                },
            )

            DropdownSelectField(
                label = stringResource(R.string.field_runtime),
                value = runtime,
                hint = stringResource(R.string.hint_runtime_auto),
                options = runtimeOptions,
                selectedIndex = 0,
                onSelect = { runtime = "" },
            )

            DropdownSelectField(
                label = stringResource(R.string.field_renderer),
                value = rendererDisplay(renderer),
                hint = stringResource(R.string.hint_renderer_global),
                options = rendererOptions,
                selectedIndex = rendererIndex,
                onSelect = { index ->
                    renderer = if (index == rendererOptions.lastIndex) "" else Renderers.IDS[index]
                },
            )
        }
    }

    if (showJvmArgs) {
        TextFieldDialog(
            title = stringResource(R.string.field_jvm_args),
            value = jvmArgsDraft,
            onValueChange = { jvmArgsDraft = it },
            onConfirm = {
                jvmArgs = jvmArgsDraft
                showJvmArgs = false
            },
            onDismiss = { showJvmArgs = false },
            confirmLabel = stringResource(R.string.action_ok),
            dismissLabel = stringResource(R.string.action_cancel),
        )
    }

    if (showDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_instance_title)) },
            text = { Text(stringResource(R.string.delete_instance_message, existing.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        onDelete(existing)
                        Toast.makeText(
                            context,
                            instanceDeletedToast,
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/** The stored controlmap path as the dropdown shows it (file name). */
@Composable
private fun controlmapDisplay(controlmap: String): String =
    if (controlmap.isBlank()) {
        stringResource(R.string.hint_controlmap_default)
    } else {
        controlmap.substringAfterLast('/')
    }

@Composable
private fun rendererDisplay(value: String): String {
    val global = stringResource(R.string.hint_renderer_global)
    if (value.isBlank()) return global
    val index = Renderers.IDS.indexOf(value)
    return if (index >= 0) Renderers.NAMES[index] else global
}
