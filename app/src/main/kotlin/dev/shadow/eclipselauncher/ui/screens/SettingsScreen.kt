/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.shadow.eclipselauncher.data.LauncherState
import dev.shadow.eclipselauncher.data.MojoSettings
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoTheme
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import dev.shadow.eclipselauncher.ui.components.MojoBackRow
import dev.shadow.eclipselauncher.ui.components.MojoDialog
import dev.shadow.eclipselauncher.ui.components.MojoDivider
import dev.shadow.eclipselauncher.ui.components.MojoPreferenceRow
import dev.shadow.eclipselauncher.ui.components.MojoPreferenceSwitch
import dev.shadow.eclipselauncher.ui.components.MojoSectionHeader
import dev.shadow.eclipselauncher.ui.components.MojoSingleChoiceDialog
import dev.shadow.eclipselauncher.ui.components.MojoSliderPreference
import dev.shadow.eclipselauncher.ui.components.MojoTextField
import java.io.File
import java.util.Locale

enum class SettingsPage {
    VIDEO,
    CONTROL,
    JAVA,
    MISC,
    EXPERIMENTAL,
}

/**
 * `pref_main.xml` and the five reference preference screens.
 *
 * Values use the reference preference keys and local SharedPreferences, so a
 * switch or slider survives a restart. Actions that need hardware request the
 * real Android permission; actions that would need the reference game engine
 * stop at the reference failure toast/dialog instead of pretending to work.
 */
@Composable
fun SettingsScreen(
    page: SettingsPage?,
    state: LauncherState,
    settings: MojoSettings,
    onBack: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit,
    onOpenCustomControls: () -> Unit,
    onOpenGamepadMapper: () -> Unit,
) {
    MojoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background),
        ) {
            MojoBackRow(label = "Back to the last screen", onClick = onBack)
            MojoDivider()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (page) {
                    null -> SettingsRoot(
                        settings = settings,
                        onOpenPage = onOpenPage,
                    )

                    SettingsPage.VIDEO -> VideoSettings(settings)
                    SettingsPage.CONTROL -> ControlSettings(
                        settings = settings,
                        onOpenCustomControls = onOpenCustomControls,
                        onOpenGamepadMapper = onOpenGamepadMapper,
                    )

                    SettingsPage.JAVA -> JavaSettings(settings)
                    SettingsPage.MISC -> MiscSettings(
                        state = state,
                        settings = settings,
                    )

                    SettingsPage.EXPERIMENTAL -> ExperimentalSettings(settings)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsRoot(
    settings: MojoSettings,
    onOpenPage: (SettingsPage) -> Unit,
) {
    MojoSectionHeader("Categories")
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.SETTINGS,
        title = "Video and renderer",
        summary = "Resolution, renderer and performance",
        onClick = { onOpenPage(SettingsPage.VIDEO) },
    )
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.CONTROLS,
        title = "Control customization",
        summary = "Gestures, buttons and scaling",
        onClick = { onOpenPage(SettingsPage.CONTROL) },
    )
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.JAR,
        title = "Java Tweaks",
        summary = "Java Runtimes, JVM Arguments, RAM amount and sandbox",
        onClick = { onOpenPage(SettingsPage.JAVA) },
    )
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.MORE,
        title = "Miscellaneous settings",
        summary = "Version list and libraries check",
        onClick = { onOpenPage(SettingsPage.MISC) },
    )
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.EDIT,
        title = "Experimental Stuff",
        summary = "Use things there with consideration, no support",
        onClick = { onOpenPage(SettingsPage.EXPERIMENTAL) },
    )
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.WORLD,
        title = "Force language to English",
        summary = "Allows you to see original strings, as intended by developers. Requires a restart.",
        checked = settings.boolean(MojoSettings.FORCE_ENGLISH, false),
        onCheckedChange = { settings.setBoolean(MojoSettings.FORCE_ENGLISH, it) },
    )
    NotificationPermissionRow()
}

@Composable
private fun NotificationPermissionRow() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    }

    if (!granted) {
        MojoPreferenceRow(
            icon = EclipsePixelIconKind.TASKS,
            title = "Allow notifications",
            summary = "Click to re-request the notification permission, required for game/modpack background downloads to work properly.",
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoSettings(settings: MojoSettings) {
    val context = LocalContext.current
    var renderer by remember {
        mutableStateOf(settings.string(MojoSettings.RENDERER, MojoSettings.DEFAULT_RENDERER))
    }
    var alternateSurface by remember {
        mutableStateOf(settings.boolean(MojoSettings.ALTERNATE_SURFACE, false))
    }
    var systemVulkan by remember {
        mutableStateOf(settings.boolean(MojoSettings.SYSTEM_VULKAN, false))
    }
    var showRenderer by remember { mutableStateOf(false) }
    var showDrivers by remember { mutableStateOf(false) }
    var driverEditing by remember { mutableStateOf(false) }
    val angleAvailable = remember { hasAngleProvider(context) }
    val supportsCustomDrivers = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    val zipMime = remember {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip") ?: "*/*"
    }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val cutout = WindowInsets.displayCutout
    val hasNotch = cutout.getTop(density) > 0 ||
        cutout.getBottom(density) > 0 ||
        cutout.getLeft(density, layoutDirection) > 0 ||
        cutout.getRight(density, layoutDirection) > 0
    val driverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            Toast.makeText(
                context,
                "Incorrect or corrupted driver package, try another",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    MojoSectionHeader("Video settings")
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.SETTINGS,
        title = "Renderer",
        summary = rendererName(renderer),
        onClick = { showRenderer = true },
    )
    if (hasNotch) {
        MojoPreferenceSwitch(
            icon = EclipsePixelIconKind.WORLD,
            title = "Ignore notch",
            summary = "Ignores the notch and extends the screen under it.\nGives you a more immersive experience.",
            checked = settings.boolean(MojoSettings.IGNORE_NOTCH, false),
            onCheckedChange = { settings.setBoolean(MojoSettings.IGNORE_NOTCH, it) },
        )
    }
    MojoSliderPreference(
        icon = EclipsePixelIconKind.FILTER,
        title = "Resolution Scaler",
        summary = "Allows you to decrease the game resolution. 100% is full resolution.",
        value = settings.int(MojoSettings.RESOLUTION, 100),
        valueRange = 25..100,
        step = 5,
        suffix = " %",
        onValueChange = { settings.setInt(MojoSettings.RESOLUTION, it) },
    )
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.PLAY,
        title = "Enable sustained performance mode",
        summary = "Limit thermal throttling by limiting peak performance.",
        checked = settings.boolean(MojoSettings.SUSTAINED_PERFORMANCE, false),
        onCheckedChange = { settings.setBoolean(MojoSettings.SUSTAINED_PERFORMANCE, it) },
    )
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.HOME,
        title = "Use alternate surface rendering",
        summary = "May help performance in GPU bound scenarios.",
        checked = alternateSurface,
        onCheckedChange = {
            alternateSurface = it
            settings.setBoolean(MojoSettings.ALTERNATE_SURFACE, it)
        },
    )
    if (alternateSurface) {
        MojoPreferenceSwitch(
            icon = EclipsePixelIconKind.PLAY,
            title = "Force enable VSync",
            summary = "Limit thermal throttling by limiting peak performance.",
            checked = settings.boolean(MojoSettings.FORCE_VSYNC, false),
            onCheckedChange = { settings.setBoolean(MojoSettings.FORCE_VSYNC, it) },
        )
    }
    if (angleAvailable) {
        MojoPreferenceSwitch(
            title = "Use ANGLE",
            summary = "Allows using ANGLE instead of the system driver. Can fix graphical issues with mods on some GPUs",
            checked = settings.boolean(MojoSettings.USE_ANGLE, false),
            onCheckedChange = { settings.setBoolean(MojoSettings.USE_ANGLE, it) },
        )
    }
    if (supportsCustomDrivers && !systemVulkan) {
        MojoPreferenceRow(
            icon = EclipsePixelIconKind.PLAY,
            title = "Vulkan Driver",
            summary = "Manage installed AdrenoTools Vulkan drivers and install new",
            onClick = { showDrivers = true },
        )
    }
    if (supportsCustomDrivers) {
        MojoPreferenceSwitch(
            icon = EclipsePixelIconKind.PLAY,
            title = "Use system Vulkan driver",
            summary = "Force the launcher to use the system Vulkan driver. Affects anything that uses Vulkan API.",
            checked = systemVulkan,
            onCheckedChange = {
                systemVulkan = it
                settings.setBoolean(MojoSettings.SYSTEM_VULKAN, it)
            },
        )
    }

    if (showRenderer) {
        val names = RENDERER_NAMES
        val ids = RENDERER_IDS
        MojoSingleChoiceDialog(
            title = "Renderer",
            options = names,
            selectedIndex = ids.indexOf(renderer).coerceAtLeast(0),
            onSelect = { index ->
                renderer = ids[index]
                settings.setString(MojoSettings.RENDERER, ids[index])
                showRenderer = false
            },
            onDismiss = { showRenderer = false },
        )
    }

    if (showDrivers) {
        MojoDialog(
            onDismiss = { showDrivers = false },
            confirmLabel = "Add",
            onConfirm = {
                showDrivers = false
                Toast.makeText(
                    context,
                    "Select a valid AdrenoTools driver package",
                    Toast.LENGTH_LONG,
                ).show()
                driverPicker.launch(arrayOf(zipMime))
            },
            neutralLabel = if (driverEditing) "Select" else "Remove",
            onNeutralButton = { driverEditing = !driverEditing },
            title = "Installed Drivers",
        )
    }
}

@Composable
private fun ControlSettings(
    settings: MojoSettings,
    onOpenCustomControls: () -> Unit,
    onOpenGamepadMapper: () -> Unit,
) {
    val context = LocalContext.current
    val gyroAvailable = remember {
        val manager = context.getSystemService(SensorManager::class.java)
        manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }
    val disableGestures = settings.boolean(MojoSettings.DISABLE_GESTURES, false)
    val enableGyro = settings.boolean(MojoSettings.ENABLE_GYRO, false)

    MojoPreferenceRow(
        icon = EclipsePixelIconKind.CONTROLS,
        title = "Edit custom controls",
        summary = "Tweak the control scheme to fit your needs.",
        onClick = onOpenCustomControls,
    )

    MojoSectionHeader("Gestures")
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.CONTROLS,
        title = "Disable gestures",
        summary = "Disables gestures, such as hold to break block, and tap to place a block.",
        checked = disableGestures,
        onCheckedChange = { settings.setBoolean(MojoSettings.DISABLE_GESTURES, it) },
    )
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.CONTROLS,
        title = "Disable double tap to swap hands",
        summary = "Disables double tapping on the hotbar item to swap it in the second hand.",
        checked = settings.boolean(MojoSettings.DISABLE_DOUBLE_TAP, false),
        onCheckedChange = { settings.setBoolean(MojoSettings.DISABLE_DOUBLE_TAP, it) },
    )
    if (!disableGestures) {
        MojoSliderPreference(
            icon = EclipsePixelIconKind.CONTROLS,
            title = "Trigger long press delay",
            summary = "Change long press trigger delay for destroying blocks and dropping items.",
            value = settings.int(MojoSettings.LONG_PRESS, 300),
            valueRange = 100..1000,
            step = 10,
            suffix = " ms",
            onValueChange = { settings.setInt(MojoSettings.LONG_PRESS, it) },
        )
    }

    MojoSectionHeader("Buttons")
    MojoSliderPreference(
        icon = EclipsePixelIconKind.CONTROLS,
        title = "Control buttons scaling",
        summary = "Upscale them if the buttons are too small.",
        value = settings.int(MojoSettings.BUTTON_SCALE, 100),
        valueRange = 80..250,
        step = 5,
        suffix = " %",
        onValueChange = { settings.setInt(MojoSettings.BUTTON_SCALE, it) },
    )
    MojoSliderPreference(
        title = "Buttons transparency",
        summary = "Changes on-screen buttons transparency",
        value = settings.int(MojoSettings.BUTTON_TRANSPARENCY, 100),
        valueRange = 0..100,
        step = 5,
        suffix = " %",
        onValueChange = { settings.setInt(MojoSettings.BUTTON_TRANSPARENCY, it) },
    )
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.EDIT,
        title = "Only use capital letters in button labels",
        summary = "Disable this if you want to use lowercase letters in your controls.",
        checked = settings.boolean(MojoSettings.BUTTON_ALL_CAPS, true),
        onCheckedChange = { settings.setBoolean(MojoSettings.BUTTON_ALL_CAPS, it) },
    )

    MojoSectionHeader("Virtual mouse")
    MojoSliderPreference(
        icon = EclipsePixelIconKind.CONTROLS,
        title = "Mouse scaling",
        summary = "Change the size of the virtual mouse.",
        value = settings.int(MojoSettings.MOUSE_SCALE, 100),
        valueRange = 25..300,
        step = 5,
        suffix = " %",
        onValueChange = { settings.setInt(MojoSettings.MOUSE_SCALE, it) },
    )
    MojoSliderPreference(
        icon = EclipsePixelIconKind.PLAY,
        title = "Mouse Speed",
        summary = "Changes the speed of the virtual mouse.",
        value = settings.int(MojoSettings.MOUSE_SPEED, 100),
        valueRange = 25..300,
        step = 5,
        suffix = " %",
        onValueChange = { settings.setInt(MojoSettings.MOUSE_SPEED, it) },
    )
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.CONTROLS,
        title = "Start with virtual mouse on",
        summary = "The virtual mouse appears at startup when this option is on.",
        checked = settings.boolean(MojoSettings.MOUSE_START, false),
        onCheckedChange = { settings.setBoolean(MojoSettings.MOUSE_START, it) },
    )

    if (gyroAvailable) {
        MojoSectionHeader("Gyroscope controls")
        MojoPreferenceSwitch(
            title = "Enable gyroscope controls",
            summary = "Enabling this will allow you to turn in-game by turning your phone.",
            checked = enableGyro,
            onCheckedChange = { settings.setBoolean(MojoSettings.ENABLE_GYRO, it) },
        )
        if (enableGyro) {
            MojoSliderPreference(
                title = "Gyroscope controls sensitivity",
                summary = "Adjust the sensitivity of gyroscope controls.",
                value = settings.int(MojoSettings.GYRO_SENSITIVITY, 100),
                valueRange = 25..300,
                step = 5,
                suffix = " %",
                onValueChange = { settings.setInt(MojoSettings.GYRO_SENSITIVITY, it) },
            )
            MojoSliderPreference(
                title = "Gyroscope sampling rate",
                summary = "If you have performance issues with gyroscope controls, increase this.",
                value = settings.int(MojoSettings.GYRO_SAMPLE_RATE, 16),
                valueRange = 5..50,
                step = 1,
                suffix = " ms",
                onValueChange = { settings.setInt(MojoSettings.GYRO_SAMPLE_RATE, it) },
            )
            MojoPreferenceSwitch(
                title = "Enable gyroscope smoothing",
                summary = "Reduce jitter in exchange of latency.",
                checked = settings.boolean(MojoSettings.GYRO_SMOOTHING, true),
                onCheckedChange = { settings.setBoolean(MojoSettings.GYRO_SMOOTHING, it) },
            )
            MojoPreferenceSwitch(
                title = "Invert X axis",
                summary = "Invert the horizontal axis.",
                checked = settings.boolean(MojoSettings.GYRO_INVERT_X, false),
                onCheckedChange = { settings.setBoolean(MojoSettings.GYRO_INVERT_X, it) },
            )
            MojoPreferenceSwitch(
                title = "Invert Y axis",
                summary = "Invert the vertical axis.",
                checked = settings.boolean(MojoSettings.GYRO_INVERT_Y, false),
                onCheckedChange = { settings.setBoolean(MojoSettings.GYRO_INVERT_Y, it) },
            )
        }
    }

    MojoSectionHeader("Controller settings")
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.CONTROLS,
        title = "Change controller key bindings",
        summary = "Allows you to modify the keyboard keys bound to each controller button",
        onClick = onOpenGamepadMapper,
    )
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.TRASH,
        title = "Reset controller mapping",
        summary = "Allows you to remap the controller buttons.",
        onClick = {
            resetGamepadMappings(settings)
            Toast.makeText(
                context,
                "The controller config has been wiped.",
                Toast.LENGTH_SHORT,
            ).show()
        },
    )
    MojoSliderPreference(
        title = "Joystick deadzone scale",
        summary = "Increase it if the joystick drifts.",
        value = settings.int(MojoSettings.DEADZONE, 100),
        valueRange = 50..200,
        step = 5,
        suffix = " %",
        onValueChange = { settings.setInt(MojoSettings.DEADZONE, it) },
    )

    MojoSectionHeader("Keyboard settings")
    MojoPreferenceSwitch(
        title = "Enable keyboard panning",
        summary = "Allows on-screen keyboard to move the game window instead of overlapping it if the last touch was below the visible zone. Has no effect on custom control layout buttons with the same function",
        checked = settings.boolean(MojoSettings.KEYBOARD_AUTOPAN, true),
        onCheckedChange = { settings.setBoolean(MojoSettings.KEYBOARD_AUTOPAN, it) },
    )
}

@Composable
private fun JavaSettings(settings: MojoSettings) {
    val context = LocalContext.current
    var showJavaArgs by remember { mutableStateOf(false) }
    var javaArgs by remember { mutableStateOf(settings.string(MojoSettings.JAVA_ARGS, "")) }
    var showRuntimes by remember { mutableStateOf(false) }
    var runtimeEditing by remember { mutableStateOf(false) }
    var runtimeError by remember { mutableStateOf(false) }
    val runtimeMime = remember {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension("xz") ?: "*/*"
    }
    val runtimePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) runtimeError = true
    }

    val maxRam = remember { maxAllocation(context) }
    val defaultAllocation = remember { findBestRamAllocation(context) }

    MojoSectionHeader("Java Tweaks")
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.JAR,
        title = "Runtime Manager",
        summary = "Manage installed Java Runtimes.",
        onClick = { showRuntimes = true },
    )
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.LOGS,
        title = "JVM Launch arguments",
        summary = "Be careful, this may make game crash if modified without knowledge.",
        onClick = {
            javaArgs = settings.string(MojoSettings.JAVA_ARGS, "")
            showJavaArgs = true
        },
    )
    MojoSliderPreference(
        icon = EclipsePixelIconKind.TASKS,
        title = "Memory allocation",
        summary = "Controls how much memory is given to the Java runtime.",
        value = settings.int(MojoSettings.ALLOCATION, defaultAllocation).coerceAtMost(maxRam),
        valueRange = 256..maxRam,
        step = 8,
        suffix = " MB",
        onValueChange = { settings.setInt(MojoSettings.ALLOCATION, it) },
    )
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.PLAY,
        title = "Sandbox .jar execution",
        summary = "Control the availability of the sandbox security manager when executing a .jar file.",
        checked = settings.boolean(MojoSettings.JAVA_SANDBOX, true),
        onCheckedChange = { settings.setBoolean(MojoSettings.JAVA_SANDBOX, it) },
    )

    if (showJavaArgs) {
        MojoDialog(
            onDismiss = { showJavaArgs = false },
            dismissLabel = "Cancel",
            onDismissButton = { showJavaArgs = false },
            confirmLabel = "OK",
            onConfirm = {
                settings.setString(MojoSettings.JAVA_ARGS, javaArgs)
                showJavaArgs = false
            },
            title = "JVM Launch arguments",
            content = {
                MojoTextField(
                    value = javaArgs,
                    onValueChange = { javaArgs = it },
                )
            },
        )
    }

    if (showRuntimes) {
        MojoDialog(
            onDismiss = { showRuntimes = false },
            confirmLabel = "Add new",
            onConfirm = {
                showRuntimes = false
                runtimePicker.launch(arrayOf(runtimeMime))
            },
            neutralLabel = if (runtimeEditing) "Set default" else "Delete runtime",
            onNeutralButton = { runtimeEditing = !runtimeEditing },
            title = "Java Runtimes",
        )
    }

    if (runtimeError) {
        MojoDialog(
            onDismiss = { runtimeError = false },
            confirmLabel = "OK",
            onConfirm = { runtimeError = false },
            title = "Error",
            message = "Failed to install JRE 17",
        )
    }
}

@Composable
private fun MiscSettings(
    state: LauncherState,
    settings: MojoSettings,
) {
    val context = LocalContext.current
    var showDownloadSource by remember { mutableStateOf(false) }
    var showMigrationWarning by remember { mutableStateOf(false) }
    val migrationPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) showMigrationWarning = true
    }
    var showKeyDialog by remember { mutableStateOf(false) }
    var keyValue by remember { mutableStateOf(state.curseForgeKey) }
    var cacheSize by remember { mutableIntStateOf(partialCacheMegabytes(state.minecraftDir)) }
    var microphoneVisible by remember { mutableStateOf(microphoneRequestVisible(context)) }
    val microphoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { microphoneVisible = microphoneRequestVisible(context) }

    val source = settings.string(
        MojoSettings.DOWNLOAD_SOURCE,
        MojoSettings.DEFAULT_DOWNLOAD_SOURCE,
    )

    MojoSectionHeader("Miscellaneous settings")
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.CHECK,
        title = "Verify game files",
        summary = "Verify downloaded game files against the checksum and size set in the metadata. Metadata JSON files are verified regardless of this setting.",
        checked = settings.boolean(MojoSettings.CHECK_GAME_FILES, true),
        onCheckedChange = { settings.setBoolean(MojoSettings.CHECK_GAME_FILES, it) },
    )
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.PLAY,
        title = "Fast game startup",
        summary = "Don't verify file checksums upon sequential restarts. Makes the game start-up very fast.",
        checked = settings.boolean(MojoSettings.FAST_STARTUP, true),
        onCheckedChange = { settings.setBoolean(MojoSettings.FAST_STARTUP, it) },
    )
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.DOWNLOAD,
        title = "Game download source",
        summary = if (source == "bmclapi") "BMCLAPI" else "Default",
        onClick = { showDownloadSource = true },
    )
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.CHECK,
        title = "Verify game version manifest",
        summary = "When enabled, the launcher will check the game version manifest along with the libraries.",
        checked = settings.boolean(MojoSettings.VERIFY_MANIFEST, true),
        onCheckedChange = { settings.setBoolean(MojoSettings.VERIFY_MANIFEST, it) },
    )
    if (microphoneVisible) {
        MojoPreferenceRow(
            icon = EclipsePixelIconKind.CONTROLS,
            title = "Request microphone access permission",
            summary = "Requests system to give access to device microphone for the game",
            onClick = { microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        )
    }
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.FOLDER,
        title = "Import data",
        summary = "Begin importing data (instances, control maps) from the old app installation",
        onClick = { migrationPicker.launch(null) },
    )
    val metadataCache = File(context.cacheDir, "string_cache")
    var metadataCacheExists by remember { mutableStateOf(metadataCache.exists()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        microphoneVisible = microphoneRequestVisible(context)
        metadataCacheExists = metadataCache.exists()
    }
    if (metadataCacheExists) {
        MojoPreferenceRow(
            icon = EclipsePixelIconKind.EDIT,
            title = "Clear metadata cache",
            summary = "Removes all strings metadata cache and forces a rebuild. Use this if the launcher can't see new game or modloader versions",
            onClick = {
                val removed = metadataCache.deleteRecursively()
                if (removed) metadataCacheExists = false
                Toast.makeText(
                    context,
                    if (removed) {
                        "Completed. Restart the launcher to apply the changes"
                    } else {
                        "Failed to remove cache files"
                    },
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.TRASH,
        title = "Clear download cache",
        summary = String.format(
            Locale.US,
            "Clear partially downloaded game files. %.2f MB of space in use",
            cacheSize / 1024f / 1024f,
        ),
        onClick = {
            deletePartialCache(state.minecraftDir)
            cacheSize = partialCacheMegabytes(state.minecraftDir)
        },
    )
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.TASKS,
        title = "Show memory warning dialog",
        summary = "If enabled, displays the warning dialog that is shown during game launch if the amount of free memory is less than allocated to the game",
        checked = settings.boolean(MojoSettings.SHOW_MEMORY_WARNING, true),
        onCheckedChange = { settings.setBoolean(MojoSettings.SHOW_MEMORY_WARNING, it) },
    )
    MojoPreferenceSwitch(
        title = "Disable server blocklist",
        summary = "Prevent the MC server blocklist from loading. This allows you to play on blocked servers (like 6b6t.org)",
        checked = settings.boolean(MojoSettings.DISABLE_SERVER_BLOCKLIST, true),
        onCheckedChange = { settings.setBoolean(MojoSettings.DISABLE_SERVER_BLOCKLIST, it) },
    )

    MojoSectionHeader("Eclipse settings")
    MojoPreferenceRow(
        icon = EclipsePixelIconKind.CONTENT,
        title = "CurseForge API key",
        summary = "Stored on this device only. Leave blank to keep CurseForge disabled.",
        onClick = {
            keyValue = state.curseForgeKey
            showKeyDialog = true
        },
        trailing = if (state.curseForgeKey.isBlank()) "Not set" else "Set",
    )
    MojoPreferenceSwitch(
        icon = EclipsePixelIconKind.CONTENT,
        title = "Auto-install required dependencies",
        summary = "Install required dependencies with the selected project.",
        checked = state.autoInstallDependencies,
        onCheckedChange = { state.updateAutoInstallDependencies(it) },
    )

    if (showDownloadSource) {
        val values = listOf(MojoSettings.DEFAULT_DOWNLOAD_SOURCE, "bmclapi")
        val names = listOf("Default", "BMCLAPI")
        MojoSingleChoiceDialog(
            title = "Game download source",
            options = names,
            selectedIndex = values.indexOf(source).coerceAtLeast(0),
            onSelect = { index ->
                settings.setString(MojoSettings.DOWNLOAD_SOURCE, values[index])
                showDownloadSource = false
            },
            onDismiss = { showDownloadSource = false },
        )
    }

    if (showMigrationWarning) {
        MojoDialog(
            onDismiss = { showMigrationWarning = false },
            dismissLabel = "Cancel",
            onDismissButton = { showMigrationWarning = false },
            confirmLabel = "OK",
            onConfirm = {
                showMigrationWarning = false
                Toast.makeText(context, "Failed to migrate data!", Toast.LENGTH_LONG).show()
            },
            title = "Data Migration",
            message = "You're going to launch a data migration process. This process will copy all selected launcher's instances and control maps to the current app installation. All conflicting files will be OVERWRITTEN! Consider making backups. You have been warned.",
        )
    }

    if (showKeyDialog) {
        MojoDialog(
            onDismiss = { showKeyDialog = false },
            dismissLabel = "Cancel",
            onDismissButton = { showKeyDialog = false },
            confirmLabel = "Save key",
            onConfirm = {
                state.updateCurseForgeKey(keyValue)
                showKeyDialog = false
            },
            title = "CurseForge API key",
            content = {
                MojoTextField(
                    value = keyValue,
                    onValueChange = { keyValue = it },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Clear key",
                        modifier = Modifier
                            .clickable {
                                keyValue = ""
                                state.updateCurseForgeKey("")
                                showKeyDialog = false
                            }
                            .padding(8.dp),
                        style = MojoType.body,
                        color = MojoPalette.Accent,
                    )
                }
            },
        )
    }
}

@Composable
private fun ExperimentalSettings(settings: MojoSettings) {
    MojoSectionHeader("Experimental fuckury")
    MojoPreferenceSwitch(
        title = "Enable shader dumping",
        summary = "Log converted shaders into the log file.",
        checked = settings.boolean(MojoSettings.SHADER_DUMP, false),
        onCheckedChange = { settings.setBoolean(MojoSettings.SHADER_DUMP, it) },
    )
    MojoPreferenceSwitch(
        title = "Force the renderer to run on the big core",
        summary = "Forces the game's render thread to run on the core with the highest maximum frequency.",
        checked = settings.boolean(MojoSettings.BIG_CORE, false),
        onCheckedChange = { settings.setBoolean(MojoSettings.BIG_CORE, it) },
    )
    MojoPreferenceSwitch(
        title = "Force rendering into the system memory",
        summary = "Forces Freedreno to render directly into the system memory. Enable if performance is low or you experience rendering issues",
        checked = settings.boolean(MojoSettings.FREEDRENO_SYSMEM, false),
        onCheckedChange = { settings.setBoolean(MojoSettings.FREEDRENO_SYSMEM, it) },
    )
    MojoPreferenceSwitch(
        title = "Forcefully use OpenSL for audio",
        summary = "Forcefully enables OpenSL backend in ALSoft instead of Oboe. Can fix audio capture issues on some devices. Disable if you see audio playback/capture issues",
        checked = settings.boolean(MojoSettings.ALSOFT_OPENSL, false),
        onCheckedChange = { settings.setBoolean(MojoSettings.ALSOFT_OPENSL, it) },
    )
    MojoPreferenceSwitch(
        title = "Enable UBWC compression workaround",
        summary = "Enables \"tp_ubwc_flag_hint\" flag needed for select Adreno GPUs (Adreno 740). Enable if you see rendering issues on freedreno or Turnip",
        checked = settings.boolean(MojoSettings.UBWC, false),
        onCheckedChange = { settings.setBoolean(MojoSettings.UBWC, it) },
    )
}

private fun hasPermission(context: Context, permission: String): Boolean {
    if (permission == Manifest.permission.POST_NOTIFICATIONS &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
        return true
    }
    return ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED
}

private fun microphoneRequestVisible(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val permission = Manifest.permission.RECORD_AUDIO
    if (ContextCompat.checkSelfPermission(context, permission) !=
        PackageManager.PERMISSION_DENIED
    ) {
        return false
    }
    val activity = context as? Activity ?: return true
    return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

private fun hasSystemAngle(): Boolean {
    val base = if (Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()) {
        "/system/lib64"
    } else {
        "/system/lib"
    }
    return File(base, "libEGL_angle.so").exists() &&
        File(base, "libGLESv2_angle.so").exists()
}

private fun hasAngleProvider(context: Context): Boolean {
    if (hasSystemAngle()) return true
    val info = runCatching {
        context.packageManager.getPackageInfo(
            "git.mojo.angle",
            PackageManager.GET_SHARED_LIBRARY_FILES,
        )
    }.getOrNull() ?: return false
    val libraryDir = info.applicationInfo?.nativeLibraryDir ?: return false
    return File(libraryDir, "libEGL_angle.so").exists() &&
        File(libraryDir, "libGLESv2_angle.so").exists()
}

private fun maxAllocation(context: Context): Int {
    val manager = context.getSystemService(ActivityManager::class.java)
    val info = ActivityManager.MemoryInfo()
    manager?.getMemoryInfo(info)
    val deviceRam = ((info.totalMem / (1024L * 1024L)).toInt()).coerceAtLeast(512)
    val is32Bit = Build.SUPPORTED_64_BIT_ABIS.isEmpty()
    val max = if (is32Bit || deviceRam < 2048) {
        minOf(1024, deviceRam)
    } else {
        deviceRam - if (deviceRam < 3064) 800 else 1024
    }
    return ((max - 256) / 8) * 8 + 256
}

private fun findBestRamAllocation(context: Context): Int {
    val manager = context.getSystemService(ActivityManager::class.java)
    val info = ActivityManager.MemoryInfo()
    manager?.getMemoryInfo(info)
    val deviceRam = ((info.totalMem / (1024L * 1024L)).toInt()).coerceAtLeast(512)
    if (deviceRam < 1024) return 296
    if (deviceRam < 1536) return 448
    if (deviceRam < 2048) return 656
    if (Build.SUPPORTED_64_BIT_ABIS.isEmpty()) return 696
    if (deviceRam < 3064) return 936
    if (deviceRam < 4096) return 1144
    if (deviceRam < 6144) return 1536
    return 2048
}

private fun partialCacheMegabytes(minecraftDir: File): Int {
    val instances = File(minecraftDir, "instances")
    val directories = listOf(
        File(instances, ".staging"),
        File(instances, ".trash"),
    )
    val bytes = directories.filter { it.exists() }.sumOf { directorySize(it) }
    return (bytes / (1024L * 1024L)).toInt()
}

private fun directorySize(file: File): Long = when {
    file.isFile -> file.length()
    file.isDirectory -> file.listFiles()?.sumOf { directorySize(it) } ?: 0L
    else -> 0L
}

private fun deletePartialCache(minecraftDir: File) {
    val instances = File(minecraftDir, "instances")
    File(instances, ".staging").deleteRecursively()
    File(instances, ".trash").deleteRecursively()
    instances.listFiles()?.forEach { child ->
        if (child.isFile && child.name.endsWith(".part")) child.delete()
    }
}

private val RENDERER_IDS = listOf(
    "opengles2",
    "opengles3_ltw",
    "vulkan_zink",
    "freedreno_kgsl",
    "mesa_desktop",
    "mesa_desktop_ext",
    "vulkan_legacyzink",
)

private val RENDERER_NAMES = listOf(
    "GL4ES (OpenGL ES 2) - 1.21.4 and lower",
    "LTW (OpenGL ES 3) - 1.17+ only",
    "Zink (Vulkan) - all versions",
    "Freedreno (KGSL) - all versions",
    "Mesa (DRM) - all versions",
    "Mesa (DRM, external) - all versions",
    "Legacy Zink (Vulkan) - all versions, low performance",
)

private fun rendererName(id: String): String {
    val index = RENDERER_IDS.indexOf(id)
    return if (index >= 0) RENDERER_NAMES[index] else RENDERER_NAMES.first()
}
