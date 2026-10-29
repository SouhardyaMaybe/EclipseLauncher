/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap

/**
 * Local persistence for the rebuilt preference pages, using the reference
 * launcher's own preference keys. These values describe this device's UI
 * configuration only; no value pretends to start a game or install a loader.
 */
class MojoSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val values: SnapshotStateMap<String, Any?> = mutableStateMapOf()

    init {
        prefs.all.forEach { (key, value) ->
            if (value != null) values[key] = value
        }
    }

    fun string(key: String, default: String): String =
        (values[key] as? String) ?: prefs.getString(key, default) ?: default

    fun boolean(key: String, default: Boolean): Boolean =
        (values[key] as? Boolean) ?: prefs.getBoolean(key, default)

    fun int(key: String, default: Int): Int = when (val value = values[key]) {
        is Int -> value
        is Long -> value.toInt()
        is String -> value.toIntOrNull() ?: default
        else -> prefs.getInt(key, default)
    }

    fun setString(key: String, value: String) {
        values[key] = value
        prefs.edit().putString(key, value).apply()
    }

    fun setBoolean(key: String, value: Boolean) {
        values[key] = value
        prefs.edit().putBoolean(key, value).apply()
    }

    fun setInt(key: String, value: Int) {
        values[key] = value
        prefs.edit().putInt(key, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "mojo_preferences"

        const val RENDERER = "renderer"
        const val IGNORE_NOTCH = "ignoreNotch"
        const val RESOLUTION = "resolutionRatio"
        const val SUSTAINED_PERFORMANCE = "sustainedPerformance"
        const val ALTERNATE_SURFACE = "alternate_surface"
        const val FORCE_VSYNC = "force_vsync"
        const val USE_ANGLE = "use_angle"
        const val SYSTEM_VULKAN = "zinkPreferSystemDriver"
        const val DISABLE_GESTURES = "disableGestures"
        const val DISABLE_DOUBLE_TAP = "disableDoubleTap"
        const val LONG_PRESS = "timeLongPressTrigger"
        const val BUTTON_SCALE = "buttonscale"
        const val BUTTON_TRANSPARENCY = "buttonTransparency"
        const val BUTTON_ALL_CAPS = "buttonAllCaps"
        const val DEFAULT_CTRL = "defaultCtrl"
        const val MOUSE_SCALE = "mousescale"
        const val MOUSE_SPEED = "mousespeed"
        const val MOUSE_START = "mouse_start"
        const val ENABLE_GYRO = "enableGyro"
        const val GYRO_SENSITIVITY = "gyroSensitivity"
        const val GYRO_SAMPLE_RATE = "gyroSampleRate"
        const val GYRO_SMOOTHING = "gyroSmoothing"
        const val GYRO_INVERT_X = "gyroInvertX"
        const val GYRO_INVERT_Y = "gyroInvertY"
        const val DEADZONE = "gamepad_deadzone_scale"
        const val KEYBOARD_AUTOPAN = "keyboardAutoPanning"
        const val JAVA_ARGS = "javaArgs"
        const val ALLOCATION = "allocation"
        const val JAVA_SANDBOX = "java_sandbox"
        const val CHECK_GAME_FILES = "checkGameFiles"
        const val FAST_STARTUP = "fastStartupCheck"
        const val DOWNLOAD_SOURCE = "downloadSource"
        const val VERIFY_MANIFEST = "verifyManifest"
        const val SHOW_MEMORY_WARNING = "showMemoryWarning"
        const val DISABLE_SERVER_BLOCKLIST = "disableServerBlocklist"
        const val FORCE_ENGLISH = "force_english"
        const val SHADER_DUMP = "dump_shaders"
        const val BIG_CORE = "bigCoreAffinity"
        const val FREEDRENO_SYSMEM = "freedrenoSysmem"
        const val ALSOFT_OPENSL = "alsoftForceOpenSL"
        const val UBWC = "ubwcWorkaround"

        const val DEFAULT_RENDERER = "opengles2"
        const val DEFAULT_DOWNLOAD_SOURCE = "default"
    }
}
