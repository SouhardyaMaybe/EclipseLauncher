/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.data.ControlKind
import dev.shadow.eclipselauncher.data.ControlMap
import dev.shadow.eclipselauncher.data.ControlProperties
import dev.shadow.eclipselauncher.data.DrawerOrientation
import dev.shadow.eclipselauncher.data.EditorControl
import dev.shadow.eclipselauncher.data.LauncherState
import dev.shadow.eclipselauncher.data.LoadedControlMap
import dev.shadow.eclipselauncher.data.MojoSettings
import dev.shadow.eclipselauncher.data.controlMapFileName
import dev.shadow.eclipselauncher.data.controlMapTag
import dev.shadow.eclipselauncher.data.ensureDefaultControlMapFile
import dev.shadow.eclipselauncher.data.generateDynamicX
import dev.shadow.eclipselauncher.data.generateDynamicY
import dev.shadow.eclipselauncher.data.newButtonControl
import dev.shadow.eclipselauncher.data.newDrawerControl
import dev.shadow.eclipselauncher.data.newJoystickControl
import dev.shadow.eclipselauncher.data.newSubButtonControl
import dev.shadow.eclipselauncher.data.readControlMap
import dev.shadow.eclipselauncher.data.saveControlMap
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoTheme
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import dev.shadow.eclipselauncher.ui.components.MojoDialog
import dev.shadow.eclipselauncher.ui.components.MojoErrorDialog
import dev.shadow.eclipselauncher.ui.components.MojoFileSelectorDialog
import dev.shadow.eclipselauncher.ui.components.MojoTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class EditorColorLayer {
    BACKGROUND,
    STROKE,
}

private enum class ControlPickerMode {
    LOAD,
    SELECT_DEFAULT,
}

private data class EditorHit(
    val control: EditorControl,
    val drawer: EditorControl?,
)

private data class EditorFailure(
    val throwable: Throwable,
    val exitOnDismiss: Boolean,
)

private data class EditorCrop(
    val targetId: Long,
    val bitmap: Bitmap,
    val aspectRatio: Float,
    val targetMaxSide: Int,
)

private data class DragPreview(
    val controlId: Long,
    val drawerId: Long?,
    val delta: Offset,
)

/**
 * `CustomControlsActivity` and `ControlLayout`.
 *
 * The screen has no application chrome. Controls are laid out from the same
 * dynamic expressions and density conversions as the reference, the editor
 * drawer and side sheets keep the reference labels and order, and every file
 * operation writes the reference v9 layout format.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun CustomControlsScreen(
    state: LauncherState,
    settings: MojoSettings,
    onExit: () -> Unit,
) {
    MojoTheme {
        val context = LocalContext.current
        val density = LocalDensity.current.density
        val scope = rememberCoroutineScope()
        val keyboard = LocalSoftwareKeyboardController.current
        val textMeasurer = rememberTextMeasurer()
        val preferredScale = settings.int(MojoSettings.BUTTON_SCALE, 100).toFloat()
        val allCaps = settings.boolean(MojoSettings.BUTTON_ALL_CAPS, true)
        val directory = state.controlmapDir

        var viewport by remember { mutableStateOf(IntSize.Zero) }
        var layout by remember { mutableStateOf(ControlMap()) }
        var bitmaps by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
        var loadedName by remember { mutableStateOf("") }
        var modified by remember { mutableStateOf(false) }
        var initialized by remember { mutableStateOf(false) }

        var selectedId by remember { mutableStateOf<Long?>(null) }
        var panelAtRight by remember { mutableStateOf(true) }
        var colorLayer by remember { mutableStateOf<EditorColorLayer?>(null) }
        var dragPreview by remember { mutableStateOf<DragPreview?>(null) }

        var drawerOpen by remember { mutableStateOf(false) }
        var showSaveDialog by remember { mutableStateOf(false) }
        var saveName by remember { mutableStateOf("") }
        var saveNameError by remember { mutableStateOf<String?>(null) }
        var showExitDialog by remember { mutableStateOf(false) }
        var pickerMode by remember { mutableStateOf<ControlPickerMode?>(null) }
        var failure by remember { mutableStateOf<EditorFailure?>(null) }
        var cropState by remember { mutableStateOf<EditorCrop?>(null) }
        var bitmapTargetId by remember { mutableStateOf<Long?>(null) }

        val metrics = remember(viewport, density, preferredScale) {
            EditorMetrics(
                width = viewport.width,
                height = viewport.height,
                density = density,
                preferredScale = preferredScale,
            )
        }

        val emptyFieldError = stringResource(R.string.mojo_global_error_field_empty)
        val selectionCancelled = stringResource(R.string.mojo_cropper_select_cancelled)
        val imageNotSupported = "Image format not supported"

        val layoutState = rememberUpdatedState(layout)
        val selectedState = rememberUpdatedState(selectedId)
        val dragState = rememberUpdatedState(dragPreview)

        fun reportError(throwable: Throwable, exitOnDismiss: Boolean = false) {
            failure = EditorFailure(throwable, exitOnDismiss)
        }

        fun baseGeometry(control: EditorControl, drawer: EditorControl?) =
            metrics.controlGeometry(control, drawer)

        fun effectiveGeometry(control: EditorControl, drawer: EditorControl?): ControlGeometry {
            val base = baseGeometry(control, drawer)
            val preview = dragPreview ?: return base
            val applies = control.id == preview.controlId || drawer?.id == preview.controlId
            return if (applies) {
                base.copy(x = base.x + preview.delta.x, y = base.y + preview.delta.y)
            } else {
                base
            }
        }

        fun closeEditorWindow() {
            selectedId = null
            colorLayer = null
            dragPreview = null
        }

        fun selectControl(hit: EditorHit) {
            colorLayer = null
            selectedId = hit.control.id
            val geometry = baseGeometry(hit.control, hit.drawer)
            panelAtRight = geometry.centerX < metrics.width / 2f
        }

        fun applyLoaded(file: File, loaded: LoadedControlMap) {
            layout = loaded.layout
            bitmaps = loaded.bitmaps
            loadedName = controlMapFileName(file, directory)
            modified = false
            selectedId = null
            colorLayer = null
            dragPreview = null
        }

        fun applyLoadFailure(throwable: Throwable) {
            layout = ControlMap()
            bitmaps = emptyMap()
            modified = false
            selectedId = null
            colorLayer = null
            dragPreview = null
            reportError(throwable)
        }

        fun loadMap(file: File) {
            if (metrics.width <= 0 || metrics.height <= 0) return
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        readControlMap(
                            file = file,
                            screenWidth = metrics.width,
                            screenHeight = metrics.height,
                            preferredScale = preferredScale,
                        )
                    }
                }
                result.onSuccess { loaded -> applyLoaded(file, loaded) }
                result.onFailure { throwable -> applyLoadFailure(throwable) }
            }
        }

        LaunchedEffect(metrics.width, metrics.height, preferredScale) {
            if (initialized || metrics.width <= 0 || metrics.height <= 0) return@LaunchedEffect
            initialized = true
            val configured = settings.string(MojoSettings.DEFAULT_CTRL, "")
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    ensureDefaultControlMapFile(context, directory)
                    val configuredFile = File(configured)
                    val target = if (configured.isNotBlank() && configuredFile.isFile) {
                        configuredFile
                    } else {
                        File(directory, "default.json")
                    }
                    target to readControlMap(
                        file = target,
                        screenWidth = metrics.width,
                        screenHeight = metrics.height,
                        preferredScale = preferredScale,
                    )
                }
            }
            result.onSuccess { (file, loaded) -> applyLoaded(file, loaded) }
            result.onFailure { throwable -> applyLoadFailure(throwable) }
        }

        fun saveNamed(name: String, exitAfterSave: Boolean) {
            if (name.isEmpty()) {
                saveNameError = emptyFieldError
                return
            }
            val target = File(directory, "$name.json")
            val snapshotLayout = layoutState.value
            val snapshotBitmaps = bitmaps
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        saveControlMap(
                            file = target,
                            layout = snapshotLayout,
                            bitmaps = snapshotBitmaps,
                            density = density,
                            screenWidth = metrics.width,
                            screenHeight = metrics.height,
                            preferredScale = preferredScale,
                        )
                    }
                }
                result.onSuccess {
                    modified = false
                    showSaveDialog = false
                    Toast.makeText(
                        context,
                        context.getString(R.string.mojo_save) + ": " + target.absolutePath,
                        Toast.LENGTH_SHORT,
                    ).show()
                    if (exitAfterSave) {
                        onExit()
                    }
                }
                result.onFailure { throwable -> reportError(throwable, exitAfterSave) }
            }
        }

        fun exportMap() {
            val name = loadedName.ifEmpty { "null" }
            val target = File(directory, "$name.json")
            val snapshotLayout = layoutState.value
            val snapshotBitmaps = bitmaps
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        saveControlMap(
                            file = target,
                            layout = snapshotLayout,
                            bitmaps = snapshotBitmaps,
                            density = density,
                            screenWidth = metrics.width,
                            screenHeight = metrics.height,
                            preferredScale = preferredScale,
                        )
                    }
                }
                saved.onFailure { throwable -> reportError(throwable) }
                saved.onSuccess {
                    modified = false
                    runCatching {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.files",
                            target,
                        )
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(
                                send,
                                loadedName.takeIf { it.isNotEmpty() },
                            ),
                        )
                    }.onFailure { throwable -> reportError(throwable) }
                }
            }
        }

        fun applyCropped(targetId: Long, bitmap: Bitmap) {
            val entry = layoutState.value.findWithParent(targetId) ?: return
            val oldTag = entry.first.properties.bitmapTag
            var newTag = controlMapTag()
            while (bitmaps.containsKey(newTag)) newTag = controlMapTag()
            bitmaps = bitmaps.toMutableMap().apply {
                if (oldTag != null) remove(oldTag)
                put(newTag, bitmap)
            }
            layout = layout.update(targetId) {
                it.copy(properties = it.properties.copy(bitmapTag = newTag))
            }
        }

        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            val targetId = bitmapTargetId
            bitmapTargetId = null
            if (uri == null) {
                Toast.makeText(context, selectionCancelled, Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            if (targetId == null) return@rememberLauncherForActivityResult
            val entry = layoutState.value.findWithParent(targetId)
                ?: return@rememberLauncherForActivityResult
            val geometry = metrics.controlGeometry(entry.first, entry.second)
            val aspect = if (geometry.height > 0f) {
                geometry.width / geometry.height
            } else {
                1f
            }
            val maxSide = max(geometry.width, geometry.height).roundToInt().coerceAtLeast(1)
            scope.launch {
                val decoded = withContext(Dispatchers.IO) {
                    runCatching {
                        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                        bitmap ?: throw IOException(imageNotSupported)
                    }
                }
                decoded.onSuccess { bitmap ->
                    cropState = EditorCrop(targetId, bitmap, aspect, maxSide)
                }
                decoded.onFailure { throwable -> reportError(throwable) }
            }
        }

        fun askToExit() {
            if (modified) {
                saveName = loadedName
                saveNameError = null
                showSaveDialog = true
            } else {
                showExitDialog = true
            }
        }

        BackHandler {
            if (drawerOpen) {
                drawerOpen = false
            } else {
                askToExit()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.StatusBar),
        ) {
            val editorInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(editorInsets)
                    .onSizeChanged { viewport = it },
            ) {
                val imeVisible = rememberUpdatedState(WindowInsets.isImeVisible)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Text(
                        text = stringResource(R.string.mojo_hint_control_mapping),
                        style = MojoType.bodySmall,
                        color = MojoPalette.PrimaryText,
                        modifier = Modifier.widthIn(max = 620.dp),
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(metrics.width, metrics.height) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val hit = hitTest(layoutState.value, down.position, metrics)
                                down.consume()

                                val drag = hit?.let { hitControl ->
                                    val lockedToDrawer = hitControl.control.kind == ControlKind.SUB_BUTTON &&
                                        hitControl.drawer != null &&
                                        hitControl.drawer.orientation != DrawerOrientation.FREE
                                    if (lockedToDrawer) {
                                        null
                                    } else {
                                        val geometry = metrics.controlGeometry(
                                            hitControl.control,
                                            hitControl.drawer,
                                        )
                                        ActiveDrag(
                                            hit = hitControl,
                                            originX = geometry.x,
                                            originY = geometry.y,
                                            width = geometry.width,
                                            height = geometry.height,
                                            targetX = geometry.x,
                                            targetY = geometry.y,
                                            changed = false,
                                        )
                                    }
                                }

                                var moved = false
                                var finished = false
                                while (!finished) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: break

                                    if (change.changedToUpIgnoreConsumed()) {
                                        if (hit == null) {
                                            if (imeVisible.value) {
                                                keyboard?.hide()
                                            } else {
                                                if (colorLayer != null) {
                                                    colorLayer = null
                                                } else if (selectedState.value != null) {
                                                    selectedId = null
                                                    dragPreview = null
                                                }
                                            }
                                        } else {
                                            val active = drag
                                            if (active != null && active.changed) {
                                                commitDrag(active, metrics, layoutState.value) { updated ->
                                                    layout = updated
                                                }
                                            }
                                            val lockedToDrawer = hit.control.kind == ControlKind.SUB_BUTTON &&
                                                hit.drawer != null &&
                                                hit.drawer.orientation != DrawerOrientation.FREE
                                            if (!moved || lockedToDrawer) {
                                                selectControl(hit)
                                            }
                                            dragPreview = null
                                        }
                                        change.consume()
                                        finished = true
                                        break
                                    }

                                    if (change.positionChanged()) {
                                        val delta = change.position - down.position
                                        if (abs(delta.x) > 8f || abs(delta.y) > 8f) moved = true
                                        if (drag != null) {
                                            val clamped = clampControlPosition(
                                                x = drag.originX + delta.x,
                                                y = drag.originY + delta.y,
                                                width = drag.width,
                                                height = drag.height,
                                                metrics = metrics,
                                            )
                                            val snapped = snapControlPosition(
                                                layout = layoutState.value,
                                                movingId = drag.hit.control.id,
                                                x = clamped.x,
                                                y = clamped.y,
                                                width = drag.width,
                                                height = drag.height,
                                                metrics = metrics,
                                            )
                                            drag.targetX = snapped.x
                                            drag.targetY = snapped.y
                                            drag.changed = true
                                            dragPreview = DragPreview(
                                                controlId = drag.hit.control.id,
                                                drawerId = drag.hit.drawer?.id,
                                                delta = Offset(
                                                    snapped.x - drag.originX,
                                                    snapped.y - drag.originY,
                                                ),
                                            )
                                            if (selectedState.value == drag.hit.control.id) {
                                                panelAtRight = snapped.x + drag.width / 2f <
                                                    metrics.width / 2f
                                            }
                                        }
                                        change.consume()
                                    }
                                }
                                dragPreview = null
                            }
                        },
                ) {
                    val imageBitmaps = bitmapCache(bitmaps)
                    for ((control, drawer) in layout.allEditorControls()) {
                        val geometry = effectiveGeometry(control, drawer)
                        val image = control.properties.bitmapTag?.let { imageBitmaps[it] }
                        drawEditorControl(
                            control = control,
                            geometry = geometry,
                            bitmap = image,
                            allCaps = allCaps,
                            textMeasurer = textMeasurer,
                            density = density,
                            preferredScale = preferredScale,
                        )
                    }
                }

                DrawerPullButton(
                    onClick = { drawerOpen = true },
                    modifier = Modifier.offset {
                        IntOffset(
                            x = (metrics.width * 0.75f - 20.dp.toPx()).roundToInt(),
                            y = 0,
                        )
                    },
                )

                val selectedEntry = selectedId?.let { id -> layout.findWithParent(id) }
                val selectedGeometry = selectedEntry?.let { (control, drawer) ->
                    effectiveGeometry(control, drawer)
                }

                if (selectedEntry != null) {
                    val (control, drawer) = selectedEntry
                    ControlSidePanel(
                        control = control,
                        parentDrawer = drawer,
                        density = density,
                        panelAtRight = panelAtRight,
                        onPropertiesChange = { properties ->
                            layout = layout.update(control.id) {
                                it.copy(properties = properties)
                            }
                        },
                        onOrientationChange = { orientation ->
                            layout = layout.update(control.id) {
                                it.copy(orientation = orientation)
                            }
                        },
                        onBackgroundImage = {
                            bitmapTargetId = control.id
                            imagePicker.launch(arrayOf("image/*"))
                        },
                        onBackgroundColor = { colorLayer = EditorColorLayer.BACKGROUND },
                        onStrokeColor = { colorLayer = EditorColorLayer.STROKE },
                    )
                }

                val colorTarget = colorLayer?.let { layer ->
                    selectedEntry?.let { (control, _) ->
                        layer to control
                    }
                }
                if (colorTarget != null) {
                    val (layer, control) = colorTarget
                    ControlColorPanel(
                        initialColor = if (layer == EditorColorLayer.BACKGROUND) {
                            control.properties.bgColor
                        } else {
                            control.properties.strokeColor
                        },
                        alphaEnabled = layer == EditorColorLayer.BACKGROUND,
                        panelAtRight = panelAtRight,
                        onColorChange = { color ->
                            val current = layoutState.value.find(control.id) ?: return@ControlColorPanel
                            val tag = current.properties.bitmapTag
                            if (tag != null) {
                                bitmaps = bitmaps - tag
                            }
                            layout = layout.update(control.id) {
                                if (layer == EditorColorLayer.BACKGROUND) {
                                    it.copy(
                                        properties = it.properties.copy(
                                            bitmapTag = null,
                                            bgColor = color,
                                        ),
                                    )
                                } else {
                                    it.copy(
                                        properties = it.properties.copy(
                                            bitmapTag = null,
                                            strokeColor = color,
                                        ),
                                    )
                                }
                            }
                        },
                    )
                }

                if (selectedEntry != null && selectedGeometry != null) {
                    ResizeHandle(
                        selectedId = selectedEntry.first.id,
                        geometry = selectedGeometry,
                        density = density,
                        onResize = { widthDp, heightDp ->
                            layout = layout.update(selectedEntry.first.id) {
                                it.copy(
                                    properties = it.properties.copy(
                                        width = widthDp,
                                        height = heightDp,
                                    ),
                                )
                            }
                        },
                    )
                }

                if (selectedEntry != null && selectedGeometry != null) {
                    val actionHeight = 40.dp
                    val actionHeightPx = with(LocalDensity.current) { actionHeight.toPx() }
                    val topPx = if (selectedGeometry.y < with(LocalDensity.current) { 20.dp.toPx() }) {
                        selectedGeometry.y + selectedGeometry.height
                    } else {
                        selectedGeometry.y - actionHeightPx
                    }
                    ActionRow(
                        isDrawer = selectedEntry.first.kind == ControlKind.DRAWER,
                        y = topPx,
                        onDelete = {
                            layout = layout.remove(selectedEntry.first.id)
                            closeEditorWindow()
                        },
                        onClone = {
                            cloneControl(
                                layout = layout,
                                hit = EditorHit(selectedEntry.first, selectedEntry.second),
                            )?.let { cloned ->
                                layout = cloned.layout
                                if (cloned.modified) modified = true
                                if (cloned.closeEditor) closeEditorWindow()
                            }
                        },
                        onAddSubButton = {
                            val drawer = selectedEntry.first
                            if (drawer.kind == ControlKind.DRAWER) {
                                val id = layout.maxId() + 1
                                var child = newSubButtonControl(id)
                                if (drawer.orientation != DrawerOrientation.FREE) {
                                    child = child.copy(
                                        properties = child.properties.copy(
                                            width = drawer.properties.width,
                                            height = drawer.properties.height,
                                        ),
                                    )
                                }
                                layout = layout.addSubButton(drawer.id, child)
                                modified = true
                            }
                        },
                    )
                }

                if (drawerOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { drawerOpen = false },
                            ),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(200.dp)
                            .background(MojoPalette.Drawer),
                    ) {
                        DrawerEntry(R.string.mojo_customctrl_addbutton) {
                            drawerOpen = false
                            val id = layout.maxId() + 1
                            layout = layout.addButton(newButtonControl(id))
                            modified = true
                        }
                        DrawerEntry(R.string.mojo_customctrl_addbutton_drawer) {
                            drawerOpen = false
                            val id = layout.maxId() + 1
                            layout = layout.addDrawer(newDrawerControl(id))
                            modified = true
                        }
                        DrawerEntry(R.string.mojo_customctrl_addbutton_joystick) {
                            drawerOpen = false
                            val id = layout.maxId() + 1
                            layout = layout.addJoystick(newJoystickControl(id))
                        }
                        DrawerEntry(R.string.mojo_load) {
                            drawerOpen = false
                            pickerMode = ControlPickerMode.LOAD
                        }
                        DrawerEntry(R.string.mojo_save) {
                            drawerOpen = false
                            saveName = loadedName
                            saveNameError = null
                            showSaveDialog = true
                        }
                        DrawerEntry(R.string.mojo_customctrl_selectdefault) {
                            drawerOpen = false
                            pickerMode = ControlPickerMode.SELECT_DEFAULT
                        }
                        DrawerEntry(R.string.mojo_customctrl_export) {
                            drawerOpen = false
                            exportMap()
                        }
                    }
                }
            }
        }

        if (pickerMode != null) {
            MojoFileSelectorDialog(
                root = directory,
                onSelect = { file ->
                    when (pickerMode) {
                        ControlPickerMode.SELECT_DEFAULT -> {
                            settings.setString(MojoSettings.DEFAULT_CTRL, file.absolutePath)
                            pickerMode = null
                            loadMap(file)
                        }

                        ControlPickerMode.LOAD, null -> {
                            pickerMode = null
                            loadMap(file)
                        }
                    }
                },
                onDismiss = { pickerMode = null },
                selectFolder = false,
                showFiles = true,
                showFolders = false,
                pathTitle = true,
                cancelLabel = stringResource(R.string.mojo_cancel),
                fileExtension = "json",
                showCreateFolder = false,
            )
        }

        if (showSaveDialog) {
            MojoDialog(
                onDismiss = { showSaveDialog = false },
                confirmLabel = stringResource(R.string.mojo_ok),
                onConfirm = { saveNamed(saveName, exitAfterSave = false) },
                dismissLabel = stringResource(R.string.mojo_cancel),
                onDismissButton = { showSaveDialog = false },
                neutralLabel = stringResource(R.string.mojo_global_save_and_exit),
                onNeutralButton = { saveNamed(saveName, exitAfterSave = true) },
                title = stringResource(R.string.mojo_save),
                content = {
                    MojoTextField(
                        value = saveName,
                        onValueChange = {
                            saveName = it
                            saveNameError = null
                        },
                        errorText = saveNameError,
                    )
                },
            )
        }

        if (showExitDialog) {
            MojoDialog(
                onDismiss = { showExitDialog = false },
                confirmLabel = stringResource(R.string.mojo_yes),
                onConfirm = {
                    showExitDialog = false
                    onExit()
                },
                dismissLabel = stringResource(R.string.mojo_no),
                onDismissButton = { showExitDialog = false },
                title = stringResource(R.string.mojo_customctrl_editor_exit_title),
                message = stringResource(R.string.mojo_customctrl_editor_exit_msg),
            )
        }

        cropState?.let { crop ->
            MojoCropDialog(
                bitmap = crop.bitmap,
                aspectRatio = crop.aspectRatio,
                targetMaxSide = crop.targetMaxSide,
                onCancel = { cropState = null },
                onCropped = { bitmap ->
                    cropState = null
                    applyCropped(crop.targetId, bitmap)
                },
            )
        }

        failure?.let { error ->
            MojoErrorDialog(
                message = error.throwable.message ?: error.throwable.toString(),
                details = error.throwable.stackTraceText(),
                dismissible = !error.exitOnDismiss,
                onDismiss = { failure = null },
                onAfterDismiss = if (error.exitOnDismiss) {
                    { onExit() }
                } else {
                    null
                },
            )
        }
    }
}

/* ---------------------------------------------------------------------- */
/* Hit testing and drag state                                              */
/* ---------------------------------------------------------------------- */

private class ActiveDrag(
    val hit: EditorHit,
    val originX: Float,
    val originY: Float,
    val width: Float,
    val height: Float,
    var targetX: Float,
    var targetY: Float,
    var changed: Boolean,
)

private fun hitTest(
    layout: ControlMap,
    position: Offset,
    metrics: EditorMetrics,
): EditorHit? {
    val controls = layout.allEditorControls()
    for (index in controls.indices.reversed()) {
        val (control, drawer) = controls[index]
        val geometry = metrics.controlGeometry(control, drawer)
        if (geometry.width <= 0f || geometry.height <= 0f) continue
        if (position.x > geometry.x && position.x < geometry.x + geometry.width &&
            position.y > geometry.y && position.y < geometry.y + geometry.height
        ) {
            return EditorHit(control, drawer)
        }
    }
    return null
}

private fun commitDrag(
    drag: ActiveDrag,
    metrics: EditorMetrics,
    layout: ControlMap,
    onLayout: (ControlMap) -> Unit,
) {
    val control = layout.find(drag.hit.control.id) ?: return
    val properties = control.properties.copy(
        dynamicX = generateDynamicX(drag.targetX, metrics.width, drag.width),
        dynamicY = generateDynamicY(drag.targetY, metrics.height, drag.height),
    )
    onLayout(layout.update(control.id) { it.copy(properties = properties) })
}

private data class CloneResult(
    val layout: ControlMap,
    val modified: Boolean,
    val closeEditor: Boolean,
)

private fun cloneControl(layout: ControlMap, hit: EditorHit): CloneResult? {
    val control = hit.control
    return when (control.kind) {
        ControlKind.JOYSTICK -> {
            val id = layout.maxId() + 1
            val clone = control.copy(id = id)
            CloneResult(
                layout = layout.addJoystick(clone),
                modified = false,
                closeEditor = true,
            )
        }

        ControlKind.BUTTON -> {
            val id = layout.maxId() + 1
            val clone = control.copy(
                id = id,
                properties = control.properties.copy(
                    dynamicX = "0.5 * \${screen_width}",
                    dynamicY = "0.5 * \${screen_height}",
                ),
            )
            CloneResult(
                layout = layout.addButton(clone),
                modified = true,
                closeEditor = true,
            )
        }

        ControlKind.DRAWER -> {
            var next = layout.maxId()
            val children = control.children.map { child ->
                next += 1
                child.copy(id = next)
            }
            next += 1
            val clone = control.copy(
                id = next,
                children = children,
                properties = control.properties.copy(
                    dynamicX = "0.5 * \${screen_width}",
                    dynamicY = "0.5 * \${screen_height}",
                ),
            )
            CloneResult(
                layout = layout.addDrawer(clone),
                modified = true,
                closeEditor = true,
            )
        }

        ControlKind.SUB_BUTTON -> {
            val drawer = hit.drawer ?: return null
            val id = layout.maxId() + 1
            val clone = control.copy(
                id = id,
                properties = control.properties.copy(
                    dynamicX = "0.5 * \${screen_width}",
                    dynamicY = "0.5 * \${screen_height}",
                ),
            )
            CloneResult(
                layout = layout.addSubButton(drawer.id, clone),
                modified = true,
                closeEditor = true,
            )
        }
    }
}

/* ---------------------------------------------------------------------- */
/* Chrome pieces                                                           */
/* ---------------------------------------------------------------------- */

@Composable
private fun DrawerPullButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Canvas(
        modifier = modifier
            .size(40.dp, 20.dp)
            .alpha(0.33f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        val padding = 6.dp.toPx()
        drawArc(
            color = Color.Black,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(padding, -size.height + padding),
            size = Size(
                width = size.width - padding * 2f,
                height = size.height * 2f - padding * 2f,
            ),
        )

        val center = Offset(size.width / 2f, 7.dp.toPx())
        val outer = 7.dp.toPx()
        val inner = 3.6.dp.toPx()
        drawCircle(Color.White, radius = inner, center = center)
        drawCircle(Color(0xFF000000), radius = inner * 0.55f, center = center)
        for (index in 0 until 8) {
            val angle = Math.toRadians(index * 45.0)
            val start = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * inner,
                y = center.y + kotlin.math.sin(angle).toFloat() * inner,
            )
            val end = Offset(
                x = center.x + kotlin.math.cos(angle).toFloat() * outer,
                y = center.y + kotlin.math.sin(angle).toFloat() * outer,
            )
            drawLine(
                color = Color.White,
                start = start,
                end = end,
                strokeWidth = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun DrawerEntry(
    stringRes: Int,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(stringRes),
            style = TextStyle(fontSize = 16.sp, color = MojoPalette.PrimaryText),
        )
    }
}

@Composable
private fun ActionRow(
    isDrawer: Boolean,
    y: Float,
    onDelete: () -> Unit,
    onClone: () -> Unit,
    onAddSubButton: () -> Unit,
) {
    Row(
        modifier = Modifier
            .offset { IntOffset(0, y.roundToInt()) }
            .fillMaxWidth()
            .height(40.dp),
    ) {
        ActionButton(
            text = stringResource(R.string.mojo_delete),
            onClick = onDelete,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            text = stringResource(R.string.mojo_clone),
            onClick = onClone,
            modifier = Modifier.weight(1f),
        )
        if (isDrawer) {
            ActionButton(
                text = stringResource(R.string.mojo_customctrl_addsubbutton),
                onClick = onAddSubButton,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (pressed) MojoPalette.ControlPressed else MojoPalette.ActionButton)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MojoType.button,
            color = MojoPalette.PrimaryText,
            maxLines = 1,
        )
    }
}

@Composable
private fun ResizeHandle(
    selectedId: Long,
    geometry: ControlGeometry,
    density: Float,
    onResize: (Float, Float) -> Unit,
) {
    val currentGeometry by rememberUpdatedState(geometry)
    val currentResize by rememberUpdatedState(onResize)
    Canvas(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = currentGeometry.x.roundToInt() + currentGeometry.width.roundToInt(),
                    y = currentGeometry.y.roundToInt() + currentGeometry.height.roundToInt(),
                )
            }
            .size(22.dp)
            .pointerInput(selectedId) {
                var startWidth = 0f
                var startHeight = 0f
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        total = Offset.Zero
                        startWidth = currentGeometry.width
                        startHeight = currentGeometry.height
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        total += amount
                        currentResize(
                            max(0f, startWidth + total.x) / density,
                            max(0f, startHeight + total.y) / density,
                        )
                    },
                )
            },
    ) {
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(0f, size.height * 0.38f)
            cubicTo(
                size.width * 0.12f,
                size.height * 0.12f,
                size.width * 0.38f,
                0f,
                size.width,
                0f,
            )
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path, MojoPalette.Accent)
    }
}

/* ---------------------------------------------------------------------- */
/* Canvas drawing                                                          */
/* ---------------------------------------------------------------------- */

private fun bitmapCache(bitmaps: Map<String, Bitmap>): Map<String, ImageBitmap> =
    bitmaps.mapValues { (_, bitmap) -> bitmap.asImageBitmap() }

private fun DrawScope.drawEditorControl(
    control: EditorControl,
    geometry: ControlGeometry,
    bitmap: ImageBitmap?,
    allCaps: Boolean,
    textMeasurer: TextMeasurer,
    density: Float,
    preferredScale: Float,
) {
    if (geometry.width <= 0f || geometry.height <= 0f) return
    val properties = control.properties
    val opacity = properties.opacity.coerceIn(0f, 1f)

    if (control.kind == ControlKind.JOYSTICK) {
        drawJoystick(geometry, properties, opacity, density, preferredScale)
        return
    }

    if (properties.bitmapTag != null) {
        if (bitmap != null) {
            val tint = if (properties.isToggle) {
                MojoPalette.Accent.copy(alpha = 128f / 255f)
            } else {
                Color.White.copy(alpha = 60f / 255f)
            }
            drawImage(
                image = bitmap,
                dstOffset = IntOffset(
                    geometry.x.roundToInt(),
                    geometry.y.roundToInt(),
                ),
                dstSize = IntSize(
                    geometry.width.roundToInt().coerceAtLeast(1),
                    geometry.height.roundToInt().coerceAtLeast(1),
                ),
                alpha = opacity,
                colorFilter = ColorFilter.tint(tint, BlendMode.SrcAtop),
            )
        }
    } else {
        val radius = min(geometry.width, geometry.height) / 2f *
            (properties.cornerRadius / 100f)
        drawRoundRect(
            color = Color(properties.bgColor).withOpacity(opacity),
            topLeft = Offset(geometry.x, geometry.y),
            size = Size(geometry.width, geometry.height),
            cornerRadius = CornerRadius(radius),
        )
        val strokeWidth = properties.strokeWidth * density * preferredScale / 100f
        if (strokeWidth > 0f) {
            val inset = strokeWidth / 2f
            drawRoundRect(
                color = Color(properties.strokeColor).withOpacity(opacity),
                topLeft = Offset(geometry.x + inset, geometry.y + inset),
                size = Size(
                    max(0f, geometry.width - strokeWidth),
                    max(0f, geometry.height - strokeWidth),
                ),
                cornerRadius = CornerRadius(radius),
                style = Stroke(strokeWidth),
            )
        }
    }

    val label = if (allCaps) properties.name.uppercase() else properties.name
    if (label.isNotEmpty()) {
        val horizontalPadding = 4.dp.toPx()
        val maxWidth = (geometry.width - horizontalPadding * 2f).roundToInt()
        if (maxWidth > 0) {
            val measured = textMeasurer.measure(
                text = AnnotatedString(label),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 4,
                constraints = Constraints(maxWidth = maxWidth),
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    geometry.x + (geometry.width - measured.size.width) / 2f,
                    geometry.y + (geometry.height - measured.size.height) / 2f,
                ),
                alpha = opacity,
            )
        }
    }
}

private fun DrawScope.drawJoystick(
    geometry: ControlGeometry,
    properties: ControlProperties,
    opacity: Float,
    density: Float,
    preferredScale: Float,
) {
    val side = min(geometry.width, geometry.height)
    if (side <= 0f) return
    val center = Offset(geometry.x + side / 2f, geometry.y + side / 2f)
    val declaredStroke = (properties.strokeWidth * density * preferredScale / 100f).toInt()
    val drawnStroke = max(1, declaredStroke).toFloat()
    val borderRadius = side / 2f * 0.75f
    val backgroundRadius = max(0f, borderRadius - declaredStroke / 2f)
    val knobRadius = side / 2f * 0.25f

    drawCircle(
        color = Color(properties.bgColor).withOpacity(opacity),
        radius = backgroundRadius,
        center = center,
    )

    val strokeColor = Color(properties.strokeColor)
    val borderTint = if (properties.strokeColor == 0) {
        Color.Transparent
    } else {
        strokeColor.copy(alpha = opacity)
    }
    drawCircle(
        color = borderTint,
        radius = borderRadius,
        center = center,
        style = Stroke(drawnStroke),
    )

    drawCircle(
        color = Color.Black.withOpacity(opacity),
        radius = knobRadius,
        center = center,
    )
}

private fun Color.withOpacity(opacity: Float): Color =
    copy(alpha = alpha * opacity.coerceIn(0f, 1f))
