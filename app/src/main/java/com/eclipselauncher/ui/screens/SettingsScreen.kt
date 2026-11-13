/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * SETTINGS: a category root plus five preference pages (video, control,
 * java, misc, experimental). Every switch and slider reads from and writes
 * through to SettingsStore immediately. Actions that would need the game
 * engine, custom drivers or migration tooling stop at an honest toast or
 * info dialog instead of pretending to work.
 */
package com.eclipselauncher.ui.screens

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale
import com.eclipselauncher.R
import com.eclipselauncher.data.Renderers
import com.eclipselauncher.data.SettingsStore
import com.eclipselauncher.data.ThemeMode
import com.eclipselauncher.ui.components.ConfirmDialog
import com.eclipselauncher.ui.components.PreferenceRow
import com.eclipselauncher.ui.components.PreferenceSwitch
import com.eclipselauncher.ui.components.SectionHeader
import com.eclipselauncher.ui.components.SingleChoiceDialog
import com.eclipselauncher.ui.components.SliderPreference
import com.eclipselauncher.ui.components.TextFieldDialog

/**
 * The five preference pages behind the settings root. Public (not private)
 * so the UTILS hub's "Edit controls" entry can deep-link to CONTROL.
 */
enum class SettingsPage {
    VIDEO,
    CONTROL,
    JAVA,
    MISC,
    EXPERIMENTAL,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsStore,
    initialPage: SettingsPage? = null,
    onBack: (() -> Unit)? = null,
) {
    var page by remember { mutableStateOf(initialPage) }

    // Handle system back: when in a sub-page, go back to root; otherwise delegate to onBack
    BackHandler(enabled = page != null) { page = null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (page != null) {
                BackRow(onClick = { page = null })
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                when (page) {
                    null -> Box(
                        modifier = Modifier.semantics { 
                            set(androidx.compose.ui.semantics.SemanticsProperties.TestTag, "settings_root_page") 
                        }
                    ) {
                        SettingsRoot(settings = settings, onOpenPage = { page = it })
                    }
                    SettingsPage.VIDEO -> Box(
                        modifier = Modifier.semantics { 
                            set(androidx.compose.ui.semantics.SemanticsProperties.TestTag, "settings_video_page") 
                        }
                    ) {
                        VideoSettings(settings)
                    }
                    SettingsPage.CONTROL -> ControlSettings(settings)
                    SettingsPage.JAVA -> JavaSettings(settings)
                    SettingsPage.MISC -> MiscSettings(settings)
                    SettingsPage.EXPERIMENTAL -> ExperimentalSettings(settings)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/** The in-screen back affordance shown above a sub-page. */
@Composable
private fun BackRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.settings_back),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun SettingsRoot(
    settings: SettingsStore,
    onOpenPage: (SettingsPage) -> Unit,
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    val themeModes = listOf(ThemeMode.DYNAMIC, ThemeMode.URBAN)
    var selectedThemeIndex by remember { mutableStateOf(themeModes.indexOf(settings.themeMode())) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.settings_categories))
        PreferenceRow(
            icon = Icons.Filled.Settings,
            title = stringResource(R.string.settings_category_video),
            summary = stringResource(R.string.settings_category_video_summary),
            onClick = { onOpenPage(SettingsPage.VIDEO) },
        )
        PreferenceRow(
            icon = Icons.Filled.Games,
            title = stringResource(R.string.settings_category_control),
            summary = stringResource(R.string.settings_category_control_summary),
            onClick = { onOpenPage(SettingsPage.CONTROL) },
        )
        PreferenceRow(
            icon = Icons.Filled.Code,
            title = stringResource(R.string.settings_category_java),
            summary = stringResource(R.string.settings_category_java_summary),
            onClick = { onOpenPage(SettingsPage.JAVA) },
        )
        PreferenceRow(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.settings_category_misc),
            summary = stringResource(R.string.settings_category_misc_summary),
            onClick = { onOpenPage(SettingsPage.MISC) },
        )
        PreferenceRow(
            icon = Icons.Filled.Science,
            title = stringResource(R.string.settings_category_experimental),
            summary = stringResource(R.string.settings_category_experimental_summary),
            onClick = { onOpenPage(SettingsPage.EXPERIMENTAL) },
        )
        PreferenceRow(
            icon = Icons.Filled.ColorLens,
            title = stringResource(R.string.settings_theme),
            summary = settings.themeMode().name.lowercase().capitalize(),
            onClick = { showThemeDialog = true },
        )
        NotificationPermissionRow()
        MicrophonePermissionRow()
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    themeModes.forEachIndexed { index, mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = index == selectedThemeIndex,
                                    onClick = {
                                        selectedThemeIndex = index
                                        settings.setThemeMode(mode)
                                        showThemeDialog = false
                                    },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = index == selectedThemeIndex, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    ThemeMode.DYNAMIC -> stringResource(R.string.settings_theme_dynamic)
                                    ThemeMode.URBAN -> stringResource(R.string.settings_theme_urban)
                                    else -> stringResource(R.string.settings_theme_dynamic)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun NotificationPermissionRow() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasNotificationPermission(context)) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !granted) {
        PreferenceRow(
            icon = Icons.Filled.Notifications,
            title = stringResource(R.string.settings_allow_notifications),
            summary = stringResource(R.string.settings_allow_notifications_summary),
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
private fun VideoSettings(settings: SettingsStore) {
    val context = LocalContext.current
    val rendererState = settings.getStringFlow(SettingsStore.RENDERER, SettingsStore.DEFAULT_RENDERER).collectAsState()
    var renderer by remember { mutableStateOf(rendererState.value) }
    val alternateSurface by settings.getBooleanFlow(SettingsStore.ALTERNATE_SURFACE, false).collectAsState()
    val systemVulkan by settings.getBooleanFlow(SettingsStore.SYSTEM_VULKAN, false).collectAsState()
    var showRenderer by remember { mutableStateOf(false) }
    val toastUnavailable = stringResource(R.string.settings_vulkan_driver_unavailable)

    val angleAvailable = remember {
        File("/system/lib64", "libEGL_angle.so").exists() ||
            File("/system/lib", "libEGL_angle.so").exists()
    }
    val supportsCustomDrivers = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val cutout = WindowInsets.displayCutout
    val hasNotch = cutout.getTop(density) > 0 ||
        cutout.getBottom(density) > 0 ||
        cutout.getLeft(density, layoutDirection) > 0 ||
        cutout.getRight(density, layoutDirection) > 0

    Column(
        modifier = Modifier.semantics { set(androidx.compose.ui.semantics.SemanticsProperties.TestTag, "settings_video_page") },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader(text = stringResource(R.string.settings_video_header))
        PreferenceRow(
            icon = Icons.Filled.Videocam,
            title = stringResource(R.string.settings_renderer),
            summary = Renderers.displayName(renderer),
            onClick = { showRenderer = true },
        )
        if (hasNotch) {
            PreferenceSwitch(
                icon = Icons.Filled.PhoneAndroid,
                title = stringResource(R.string.settings_ignore_notch),
                summary = stringResource(R.string.settings_ignore_notch_summary),
                checked = settings.getBooleanFlow(SettingsStore.IGNORE_NOTCH, false).collectAsState().value,
                onCheckedChange = { settings.setBoolean(SettingsStore.IGNORE_NOTCH, it) },
            )
        }
        SliderPreference(
            icon = Icons.Filled.FilterList,
            title = stringResource(R.string.settings_resolution_scaler),
            summary = stringResource(R.string.settings_resolution_scaler_summary),
            value = settings.getIntFlow(SettingsStore.RESOLUTION, 100).collectAsState().value,
            valueRange = 25..100,
            step = 5,
            suffix = " %",
            onValueChange = { settings.setInt(SettingsStore.RESOLUTION, it) },
        )
        PreferenceSwitch(
            icon = Icons.Filled.Speed,
            title = stringResource(R.string.settings_sustained_performance),
            summary = stringResource(R.string.settings_sustained_performance_summary),
            checked = settings.getBooleanFlow(SettingsStore.SUSTAINED_PERFORMANCE, false).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.SUSTAINED_PERFORMANCE, it) },
        )
        PreferenceSwitch(
            icon = Icons.Filled.Layers,
            title = stringResource(R.string.settings_alternate_surface),
            summary = stringResource(R.string.settings_alternate_surface_summary),
            checked = alternateSurface,
            onCheckedChange = { settings.setBoolean(SettingsStore.ALTERNATE_SURFACE, it) },
        )
        if (alternateSurface) {
            PreferenceSwitch(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.settings_force_vsync),
                summary = stringResource(R.string.settings_force_vsync_summary),
                checked = settings.getBooleanFlow(SettingsStore.FORCE_VSYNC, false).collectAsState().value,
                onCheckedChange = { settings.setBoolean(SettingsStore.FORCE_VSYNC, it) },
            )
        }
        if (angleAvailable) {
            PreferenceSwitch(
                icon = Icons.Filled.Memory,
                title = stringResource(R.string.settings_use_angle),
                summary = stringResource(R.string.settings_use_angle_summary),
                checked = settings.getBooleanFlow(SettingsStore.USE_ANGLE, false).collectAsState().value,
                onCheckedChange = { settings.setBoolean(SettingsStore.USE_ANGLE, it) },
            )
        }
        if (supportsCustomDrivers && !systemVulkan) {
            PreferenceRow(
                icon = Icons.Filled.SdStorage,
                title = stringResource(R.string.settings_vulkan_driver),
                summary = stringResource(R.string.settings_vulkan_driver_summary),
                onClick = {
                    Toast.makeText(context, toastUnavailable, Toast.LENGTH_LONG).show()
                },
            )
        }
        if (supportsCustomDrivers) {
            PreferenceSwitch(
                icon = Icons.Filled.Android,
                title = stringResource(R.string.settings_system_vulkan),
                summary = stringResource(R.string.settings_system_vulkan_summary),
                checked = systemVulkan,
                onCheckedChange = { settings.setBoolean(SettingsStore.SYSTEM_VULKAN, it) },
            )
        }
    }

    if (showRenderer) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_renderer),
            options = Renderers.NAMES,
            selectedIndex = Renderers.IDS.indexOf(renderer).coerceAtLeast(0),
            onSelect = { index ->
                renderer = Renderers.IDS[index]
                settings.setString(SettingsStore.RENDERER, Renderers.IDS[index])
                showRenderer = false
            },
            onDismiss = { showRenderer = false },
        )
    }
}

@Composable
private fun ControlSettings(settings: SettingsStore) {
    val context = LocalContext.current
    val gyroAvailable = remember {
        val manager = context.getSystemService(SensorManager::class.java)
        manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }
    val disableGestures by settings.getBooleanFlow(SettingsStore.DISABLE_GESTURES, false).collectAsState()
    val enableGyro by settings.getBooleanFlow(SettingsStore.ENABLE_GYRO, false).collectAsState()
    val toastControlsUnavailable = stringResource(R.string.settings_controls_unavailable)
    val toastBindingsUnavailable = stringResource(R.string.settings_controller_bindings_unavailable)
    val toastResetUnavailable = stringResource(R.string.settings_controller_reset_unavailable)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PreferenceRow(
            icon = Icons.Filled.Create,
            title = stringResource(R.string.settings_edit_controls),
            summary = stringResource(R.string.settings_edit_controls_summary),
            onClick = {
                Toast.makeText(context, toastControlsUnavailable, Toast.LENGTH_SHORT).show()
            },
        )

        SectionHeader(text = stringResource(R.string.settings_gestures_header))
        PreferenceSwitch(
            icon = Icons.Filled.Block,
            title = stringResource(R.string.settings_disable_gestures),
            summary = stringResource(R.string.settings_disable_gestures_summary),
            checked = disableGestures,
            onCheckedChange = { settings.setBoolean(SettingsStore.DISABLE_GESTURES, it) },
        )
        PreferenceSwitch(
            icon = Icons.Filled.TouchApp,
            title = stringResource(R.string.settings_disable_double_tap),
            summary = stringResource(R.string.settings_disable_double_tap_summary),
            checked = settings.getBooleanFlow(SettingsStore.DISABLE_DOUBLE_TAP, false).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.DISABLE_DOUBLE_TAP, it) },
        )
        if (!disableGestures) {
            SliderPreference(
                icon = Icons.Filled.Timer,
                title = stringResource(R.string.settings_long_press),
                summary = stringResource(R.string.settings_long_press_summary),
                value = settings.getIntFlow(SettingsStore.LONG_PRESS, 300).collectAsState().value,
                valueRange = 100..1000,
                step = 10,
                suffix = " ms",
                onValueChange = { settings.setInt(SettingsStore.LONG_PRESS, it) },
            )
        }

        SectionHeader(text = stringResource(R.string.settings_buttons_header))
        SliderPreference(
            icon = Icons.Filled.ZoomIn,
            title = stringResource(R.string.settings_button_scale),
            summary = stringResource(R.string.settings_button_scale_summary),
            value = settings.getIntFlow(SettingsStore.BUTTON_SCALE, 100).collectAsState().value,
            valueRange = 80..250,
            step = 5,
            suffix = " %",
            onValueChange = { settings.setInt(SettingsStore.BUTTON_SCALE, it) },
        )
        SliderPreference(
            icon = Icons.Filled.Opacity,
            title = stringResource(R.string.settings_button_transparency),
            summary = stringResource(R.string.settings_button_transparency_summary),
            value = settings.getIntFlow(SettingsStore.BUTTON_TRANSPARENCY, 100).collectAsState().value,
            valueRange = 0..100,
            step = 5,
            suffix = " %",
            onValueChange = { settings.setInt(SettingsStore.BUTTON_TRANSPARENCY, it) },
        )
        PreferenceSwitch(
            icon = Icons.Filled.TextFields,
            title = stringResource(R.string.settings_button_all_caps),
            summary = stringResource(R.string.settings_button_all_caps_summary),
            checked = settings.getBooleanFlow(SettingsStore.BUTTON_ALL_CAPS, true).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.BUTTON_ALL_CAPS, it) },
        )

        SectionHeader(text = stringResource(R.string.settings_virtual_mouse_header))
        SliderPreference(
            icon = Icons.Filled.Mouse,
            title = stringResource(R.string.settings_mouse_scale),
            summary = stringResource(R.string.settings_mouse_scale_summary),
            value = settings.getIntFlow(SettingsStore.MOUSE_SCALE, 100).collectAsState().value,
            valueRange = 25..300,
            step = 5,
            suffix = " %",
            onValueChange = { settings.setInt(SettingsStore.MOUSE_SCALE, it) },
        )
        SliderPreference(
            icon = Icons.Filled.Speed,
            title = stringResource(R.string.settings_mouse_speed),
            summary = stringResource(R.string.settings_mouse_speed_summary),
            value = settings.getIntFlow(SettingsStore.MOUSE_SPEED, 100).collectAsState().value,
            valueRange = 25..300,
            step = 5,
            suffix = " %",
            onValueChange = { settings.setInt(SettingsStore.MOUSE_SPEED, it) },
        )
        PreferenceSwitch(
            icon = Icons.Filled.DesktopWindows,
            title = stringResource(R.string.settings_mouse_start),
            summary = stringResource(R.string.settings_mouse_start_summary),
            checked = settings.getBooleanFlow(SettingsStore.MOUSE_START, false).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.MOUSE_START, it) },
        )

        if (gyroAvailable) {
            SectionHeader(text = stringResource(R.string.settings_gyro_header))
            PreferenceSwitch(
                icon = Icons.Filled.RotateRight,
                title = stringResource(R.string.settings_enable_gyro),
                summary = stringResource(R.string.settings_enable_gyro_summary),
                checked = enableGyro,
                onCheckedChange = { settings.setBoolean(SettingsStore.ENABLE_GYRO, it) },
            )
            if (enableGyro) {
                SliderPreference(
                    icon = Icons.Filled.Speed,
                    title = stringResource(R.string.settings_gyro_sensitivity),
                    summary = stringResource(R.string.settings_gyro_sensitivity_summary),
                    value = settings.getIntFlow(SettingsStore.GYRO_SENSITIVITY, 100).collectAsState().value,
                    valueRange = 25..300,
                    step = 5,
                    suffix = " %",
                    onValueChange = { settings.setInt(SettingsStore.GYRO_SENSITIVITY, it) },
                )
                SliderPreference(
                    icon = Icons.Filled.Timer,
                    title = stringResource(R.string.settings_gyro_sample_rate),
                    summary = stringResource(R.string.settings_gyro_sample_rate_summary),
                    value = settings.getIntFlow(SettingsStore.GYRO_SAMPLE_RATE, 16).collectAsState().value,
                    valueRange = 5..50,
                    step = 1,
                    suffix = " ms",
                    onValueChange = { settings.setInt(SettingsStore.GYRO_SAMPLE_RATE, it) },
                )
                PreferenceSwitch(
                    icon = Icons.Filled.Waves,
                    title = stringResource(R.string.settings_gyro_smoothing),
                    summary = stringResource(R.string.settings_gyro_smoothing_summary),
                    checked = settings.getBooleanFlow(SettingsStore.GYRO_SMOOTHING, true).collectAsState().value,
                    onCheckedChange = { settings.setBoolean(SettingsStore.GYRO_SMOOTHING, it) },
                )
                PreferenceSwitch(
                    icon = Icons.Filled.SwapHoriz,
                    title = stringResource(R.string.settings_invert_x),
                    summary = stringResource(R.string.settings_invert_x_summary),
                    checked = settings.getBooleanFlow(SettingsStore.GYRO_INVERT_X, false).collectAsState().value,
                    onCheckedChange = { settings.setBoolean(SettingsStore.GYRO_INVERT_X, it) },
                )
                PreferenceSwitch(
                    icon = Icons.Filled.SwapVert,
                    title = stringResource(R.string.settings_invert_y),
                    summary = stringResource(R.string.settings_invert_y_summary),
                    checked = settings.getBooleanFlow(SettingsStore.GYRO_INVERT_Y, false).collectAsState().value,
                    onCheckedChange = { settings.setBoolean(SettingsStore.GYRO_INVERT_Y, it) },
                )
            }
        }

        SectionHeader(text = stringResource(R.string.settings_controller_header))
        PreferenceRow(
            icon = Icons.Filled.SportsEsports,
            title = stringResource(R.string.settings_controller_bindings),
            summary = stringResource(R.string.settings_controller_bindings_summary),
            onClick = {
                Toast.makeText(context, toastBindingsUnavailable, Toast.LENGTH_SHORT).show()
            },
        )
        PreferenceRow(
            icon = Icons.Filled.Restore,
            title = stringResource(R.string.settings_controller_reset),
            summary = stringResource(R.string.settings_controller_reset_summary),
            onClick = {
                Toast.makeText(context, toastResetUnavailable, Toast.LENGTH_SHORT).show()
            },
        )
        SliderPreference(
            icon = Icons.Filled.Games,
            title = stringResource(R.string.settings_deadzone),
            summary = stringResource(R.string.settings_deadzone_summary),
            value = settings.getIntFlow(SettingsStore.DEADZONE, 100).collectAsState().value,
            valueRange = 50..200,
            step = 5,
            suffix = " %",
            onValueChange = { settings.setInt(SettingsStore.DEADZONE, it) },
        )

        SectionHeader(text = stringResource(R.string.settings_keyboard_header))
        PreferenceSwitch(
            icon = Icons.Filled.Keyboard,
            title = stringResource(R.string.settings_keyboard_autopan),
            summary = stringResource(R.string.settings_keyboard_autopan_summary),
            checked = settings.getBooleanFlow(SettingsStore.KEYBOARD_AUTOPAN, true).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.KEYBOARD_AUTOPAN, it) },
        )
    }
}

@Composable
private fun JavaSettings(settings: SettingsStore) {
    val context = LocalContext.current
    val javaArgsState = settings.getStringFlow(SettingsStore.JAVA_ARGS, "").collectAsState()
    var showJavaArgs by remember { mutableStateOf(false) }
    var javaArgs by remember { mutableStateOf(javaArgsState.value) }
    var showRuntimes by remember { mutableStateOf(false) }

    val maxRam = remember { maxAllocation(context) }
    val defaultAllocation = remember { findBestRamAllocation(context) }

    val doneLabel = stringResource(R.string.action_done)
    val cancelLabel = stringResource(R.string.action_cancel)
    val okLabel = stringResource(R.string.settings_ok)
    val argsTitle = stringResource(R.string.settings_jvm_args)
    val runtimeTitle = stringResource(R.string.settings_runtimes_title)
    val runtimeMessage = stringResource(R.string.settings_runtimes_message)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.settings_category_java))
        PreferenceRow(
            icon = Icons.Filled.Folder,
            title = stringResource(R.string.settings_runtime_manager),
            summary = stringResource(R.string.settings_runtime_manager_summary),
            onClick = { showRuntimes = true },
        )
        PreferenceRow(
            icon = Icons.Filled.Description,
            title = stringResource(R.string.settings_jvm_args),
            summary = stringResource(R.string.settings_jvm_args_summary),
            onClick = {
                javaArgs = javaArgsState.value
                showJavaArgs = true
            },
        )
        SliderPreference(
            icon = Icons.Filled.Memory,
            title = stringResource(R.string.settings_memory_allocation),
            summary = stringResource(R.string.settings_memory_allocation_summary),
            value = settings.getIntFlow(SettingsStore.ALLOCATION, defaultAllocation).collectAsState().value.coerceIn(256, maxRam),
            valueRange = 256..maxRam,
            step = 8,
            suffix = " MB",
            onValueChange = { settings.setInt(SettingsStore.ALLOCATION, it) },
        )
    }

    if (showJavaArgs) {
        TextFieldDialog(
            title = argsTitle,
            value = javaArgs,
            onValueChange = { javaArgs = it },
            onConfirm = {
                settings.setString(SettingsStore.JAVA_ARGS, javaArgs)
                showJavaArgs = false
            },
            onDismiss = { showJavaArgs = false },
            confirmLabel = doneLabel,
            dismissLabel = cancelLabel,
        )
    }

    if (showRuntimes) {
        ConfirmDialog(
            title = runtimeTitle,
            message = runtimeMessage,
            confirmLabel = okLabel,
            dismissLabel = cancelLabel,
            onConfirm = { showRuntimes = false },
            onDismiss = { showRuntimes = false },
        )
    }
}

@Composable
private fun MiscSettings(settings: SettingsStore) {
    val context = LocalContext.current
    var cacheBytes by remember { mutableStateOf(partialCacheBytes(context)) }
    var showKeyDialog by remember { mutableStateOf(false) }
    val curseKey by settings.getStringFlow(SettingsStore.CURSEFORGE_KEY, "").collectAsState()

    val cacheCleared = stringResource(R.string.settings_cache_cleared)
    val cacheClearFailed = stringResource(R.string.settings_cache_clear_failed)
    val downloadCleared = stringResource(R.string.settings_download_cache_cleared)
    val clearSummary = String.format(
        Locale.US,
        stringResource(R.string.settings_clear_download_summary),
        cacheBytes / 1024.0 / 1024.0,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.settings_category_misc))
        PreferenceSwitch(
            icon = Icons.Filled.CheckCircle,
            title = stringResource(R.string.settings_verify_files),
            summary = stringResource(R.string.settings_verify_files_summary),
            checked = settings.getBooleanFlow(SettingsStore.CHECK_GAME_FILES, true).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.CHECK_GAME_FILES, it) },
        )
        PreferenceSwitch(
            icon = Icons.Filled.FlashOn,
            title = stringResource(R.string.settings_fast_startup),
            summary = stringResource(R.string.settings_fast_startup_summary),
            checked = settings.getBooleanFlow(SettingsStore.FAST_STARTUP, true).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.FAST_STARTUP, it) },
        )
        PreferenceSwitch(
            icon = Icons.Filled.Verified,
            title = stringResource(R.string.settings_verify_manifest),
            summary = stringResource(R.string.settings_verify_manifest_summary),
            checked = settings.getBooleanFlow(SettingsStore.VERIFY_MANIFEST, true).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.VERIFY_MANIFEST, it) },
        )
        PreferenceRow(
            icon = Icons.Filled.Cached,
            title = stringResource(R.string.settings_clear_metadata),
            summary = stringResource(R.string.settings_clear_metadata_summary),
            onClick = {
                val removed = File(context.cacheDir, "meta_cache").deleteRecursively()
                Toast.makeText(
                    context,
                    if (removed) cacheCleared else cacheClearFailed,
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
        PreferenceRow(
            icon = Icons.Filled.Delete,
            title = stringResource(R.string.settings_clear_download),
            summary = clearSummary,
            onClick = {
                val freed = String.format(
                    Locale.US,
                    downloadCleared,
                    cacheBytes / 1024.0 / 1024.0,
                )
                deletePartialCache(context)
                cacheBytes = partialCacheBytes(context)
                Toast.makeText(context, freed, Toast.LENGTH_SHORT).show()
            },
        )
        PreferenceSwitch(
            icon = Icons.Filled.Gavel,
            title = stringResource(R.string.settings_memory_warning),
            summary = stringResource(R.string.settings_memory_warning_summary),
            checked = settings.getBooleanFlow(SettingsStore.SHOW_MEMORY_WARNING, true).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.SHOW_MEMORY_WARNING, it) },
        )
        PreferenceRow(
            icon = Icons.Filled.VpnKey,
            title = stringResource(R.string.settings_curseforge_key),
            summary = stringResource(R.string.settings_curseforge_key_summary),
            onClick = { showKeyDialog = true },
            trailing = if (curseKey.isBlank()) {
                stringResource(R.string.settings_key_not_set)
            } else {
                stringResource(R.string.settings_key_set)
            },
        )
    }
    if (showKeyDialog) {
        CurseKeyDialog(
            initial = curseKey,
            onSave = {
                settings.setString(SettingsStore.CURSEFORGE_KEY, it.trim())
                showKeyDialog = false
            },
            onClear = {
                settings.setString(SettingsStore.CURSEFORGE_KEY, "")
                showKeyDialog = false
            },
            onDismiss = { showKeyDialog = false },
        )
    }
    }
    }

/** The CurseForge key editor: a text field plus an explicit clear action. */
@Composable
private fun CurseKeyDialog(
    initial: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var key by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_curseforge_key)) },
        text = {
            Column {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.settings_clear_key))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(key) }) {
                Text(stringResource(R.string.settings_save_key))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun ExperimentalSettings(settings: SettingsStore) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.settings_experimental_header))
        PreferenceSwitch(
            icon = Icons.Filled.Texture,
            title = stringResource(R.string.settings_shader_dump),
            summary = stringResource(R.string.settings_shader_dump_summary),
            checked = settings.getBooleanFlow(SettingsStore.SHADER_DUMP, false).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.SHADER_DUMP, it) },
        )
        PreferenceSwitch(
            icon = Icons.Filled.Memory,
            title = stringResource(R.string.settings_big_core),
            summary = stringResource(R.string.settings_big_core_summary),
            checked = settings.getBooleanFlow(SettingsStore.BIG_CORE, false).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.BIG_CORE, it) },
        )
        PreferenceSwitch(
            icon = Icons.Filled.Storage,
            title = stringResource(R.string.settings_freedreno_sysmem),
            summary = stringResource(R.string.settings_freedreno_sysmem_summary),
            checked = settings.getBooleanFlow(SettingsStore.FREEDRENO_SYSMEM, false).collectAsState().value,
            onCheckedChange = { settings.setBoolean(SettingsStore.FREEDRENO_SYSMEM, it) },
        )
    }
}

/** POST_NOTIFICATIONS only exists on 13+; older versions are always granted. */
private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

/** RECORD_AUDIO microphone permission: granted on older versions, checked on 13+. */
private fun hasMicrophonePermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
}

/** RECORD_AUDIO microphone permission row: shows granted/denied state, re-asks on tap. */
@Composable
private fun MicrophonePermissionRow() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasMicrophonePermission(context)) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    PreferenceRow(
        icon = Icons.Filled.Mic,
        title = stringResource(R.string.settings_microphone),
        summary = if (granted) stringResource(R.string.settings_mic_granted) else stringResource(R.string.settings_mic_denied),
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                granted = true
            }
        },
    )
}

/** Total device RAM capped the way the reference launcher caps allocation. */
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

/** A sensible default allocation for this device, matching the reference. */
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

/** Bytes still held by partial download staging: .staging, .trash and .part files. */
private fun partialCacheBytes(context: Context): Long {
    val instances = File(context.filesDir, "minecraft/instances")
    val directories = listOf(
        File(instances, ".staging"),
        File(instances, ".trash"),
    )
    var bytes = directories.filter { it.exists() }.sumOf { directorySize(it) }
    instances.listFiles()?.forEach { child ->
        if (child.isFile && child.name.endsWith(".part")) bytes += child.length()
    }
    return bytes
}

/** Removes partial download staging under the instances directory. */
private fun deletePartialCache(context: Context) {
    val instances = File(context.filesDir, "minecraft/instances")
    File(instances, ".staging").deleteRecursively()
    File(instances, ".trash").deleteRecursively()
    instances.listFiles()?.forEach { child ->
        if (child.isFile && child.name.endsWith(".part")) child.delete()
    }
}

private fun directorySize(file: File): Long = when {
    file.isFile -> file.length()
    file.isDirectory -> file.listFiles()?.sumOf { directorySize(it) } ?: 0L
    else -> 0L
}
