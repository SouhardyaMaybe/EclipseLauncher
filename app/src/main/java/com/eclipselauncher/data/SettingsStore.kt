/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * SETTINGS STORE: typed SharedPreferences access for every launcher
 * preference, plus the renderer id/name tables shared between the settings
 * screen and the instance editor. Keys mirror the reference launcher so
 * values survive a migration.
 */
package com.eclipselauncher.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local persistence for the settings screens. Every accessor reads straight
 * through to SharedPreferences so a switch or slider survives a restart;
 * writes use apply() and are visible to the next read immediately.
 * 
 * Additionally, each setting exposes a StateFlow so UI can observe changes
 * reactively. When a value is written, both SharedPreferences and the
 * corresponding StateFlow are updated.
 */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("eclipse_settings", Context.MODE_PRIVATE)

    // Maps of observable flows for each setting key
    private val booleanFlows = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val intFlows = mutableMapOf<String, MutableStateFlow<Int>>()
    private val stringFlows = mutableMapOf<String, MutableStateFlow<String>>()

    private fun booleanFlow(key: String, defaultValue: Boolean): MutableStateFlow<Boolean> =
        booleanFlows.getOrPut(key) {
            MutableStateFlow(prefs.getBoolean(key, defaultValue))
        }

    private fun intFlow(key: String, defaultValue: Int): MutableStateFlow<Int> =
        intFlows.getOrPut(key) {
            MutableStateFlow(prefs.getInt(key, defaultValue))
        }

    private fun stringFlow(key: String, defaultValue: String): MutableStateFlow<String> =
        stringFlows.getOrPut(key) {
            MutableStateFlow(prefs.getString(key, defaultValue) ?: defaultValue)
        }

    fun boolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)

    fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        booleanFlow(key, false).value = value
    }

    fun string(key: String, defaultValue: String): String = prefs.getString(key, defaultValue) ?: defaultValue

    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
        stringFlow(key, "").value = value
    }

    fun int(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)

    fun setInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
        intFlow(key, 0).value = value
    }

    // Observable flows for reactive UI
    fun getBooleanFlow(key: String, defaultValue: Boolean): StateFlow<Boolean> =
        booleanFlow(key, defaultValue).asStateFlow()

    fun getIntFlow(key: String, defaultValue: Int): StateFlow<Int> =
        intFlow(key, defaultValue).asStateFlow()

    fun getStringFlow(key: String, defaultValue: String): StateFlow<String> =
        stringFlow(key, defaultValue).asStateFlow()

    companion object {
        const val RENDERER = "renderer"
        const val ALTERNATE_SURFACE = "alternate_surface"
        const val SYSTEM_VULKAN = "zinkPreferSystemDriver"
        const val IGNORE_NOTCH = "ignoreNotch"
        const val RESOLUTION = "resolutionRatio"
        const val SUSTAINED_PERFORMANCE = "sustainedPerformance"
        const val FORCE_VSYNC = "force_vsync"
        const val USE_ANGLE = "use_angle"
        const val DISABLE_GESTURES = "disableGestures"
        const val DISABLE_DOUBLE_TAP = "disableDoubleTap"
        const val LONG_PRESS = "timeLongPressTrigger"
        const val BUTTON_SCALE = "buttonscale"
        const val BUTTON_TRANSPARENCY = "buttonTransparency"
        const val BUTTON_ALL_CAPS = "buttonAllCaps"
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
        const val CURSEFORGE_KEY = "curseforge.key"
        const val AUTO_INSTALL_DEPENDENCIES = "content.auto_dependencies"

        const val DEFAULT_RENDERER = "opengles2"
        const val DEFAULT_DOWNLOAD_SOURCE = "default"
    }
}

/**
 * Renderer ids and display names, shared by the settings screen and the
 * instance editor. The names are product strings (renderer plus Minecraft
 * version guidance), so they stay identical to the ids: untranslated data.
 */
object Renderers {
    val IDS = listOf(
        "opengles2",
        "opengles3_ltw",
        "vulkan_zink",
        "freedreno_kgsl",
        "mesa_desktop",
        "mesa_desktop_ext",
        "vulkan_legacyzink",
    )

    val NAMES = listOf(
        "GL4ES (OpenGL ES 2) - 1.21.4 and lower",
        "LTW (OpenGL ES 3) - 1.17+ only",
        "Zink (Vulkan) - all versions",
        "Freedreno (KGSL) - all versions",
        "Mesa (DRM) - all versions",
        "Mesa (DRM, external) - all versions",
        "Legacy Zink (Vulkan) - all versions, low performance",
    )

    fun displayName(id: String): String {
        val index = IDS.indexOf(id)
        return if (index >= 0) NAMES[index] else NAMES.first()
    }
}
