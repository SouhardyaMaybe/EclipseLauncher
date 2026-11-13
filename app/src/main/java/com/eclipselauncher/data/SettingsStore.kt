/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * SETTINGS STORE: typed MMKV access for every launcher preference, plus the
 * renderer id/name tables shared between the settings screens and the instance
 * editor. Keys mirror the reference launcher so values survive a migration.
 */
package com.eclipselauncher.data

import android.content.Context
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local persistence for the settings screens. Every accessor reads straight
 * through to MMKV so a switch or slider survives a restart; writes are
 * synchronous and visible to the next read immediately.
 *
 * Additionally, each setting exposes a StateFlow so UI can observe changes
 * reactively. When a value is written, both MMKV and the corresponding
 * StateFlow are updated.
 */

/** UI theme: DYNAMIC follows Material You wallpaper colors, URBAN is the fixed
 *  dark/gray palette copied from Zalith Launcher 2. */
enum class ThemeMode { DYNAMIC, URBAN }

class SettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val mmkv: MMKV = MMKV.mmkvWithID(MMKV_ID, MMKV.SINGLE_PROCESS_MODE)

    // Maps of observable flows for each setting key
    private val booleanFlows = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val intFlows = mutableMapOf<String, MutableStateFlow<Int>>()
    private val stringFlows = mutableMapOf<String, MutableStateFlow<String>>()

    init {
        migrateLegacyPreferences()
    }

    private fun booleanFlow(key: String, defaultValue: Boolean): MutableStateFlow<Boolean> =
        booleanFlows.getOrPut(key) {
            MutableStateFlow(boolean(key, defaultValue))
        }

    private fun intFlow(key: String, defaultValue: Int): MutableStateFlow<Int> =
        intFlows.getOrPut(key) {
            MutableStateFlow(int(key, defaultValue))
        }

    private fun stringFlow(key: String, defaultValue: String): MutableStateFlow<String> =
        stringFlows.getOrPut(key) {
            MutableStateFlow(string(key, defaultValue))
        }

    fun boolean(key: String, defaultValue: Boolean): Boolean = mmkv.decodeBool(key, defaultValue)

    fun setBoolean(key: String, value: Boolean) {
        mmkv.encode(key, value)
        booleanFlow(key, false).value = value
    }

    fun string(key: String, defaultValue: String): String =
        mmkv.decodeString(key, defaultValue) ?: defaultValue

    fun setString(key: String, value: String) {
        mmkv.encode(key, value)
        stringFlow(key, "").value = value
    }

    fun int(key: String, defaultValue: Int): Int = mmkv.decodeInt(key, defaultValue)

    fun setInt(key: String, value: Int) {
        mmkv.encode(key, value)
        intFlow(key, 0).value = value
    }

    // Observable flows for reactive UI
    fun getBooleanFlow(key: String, defaultValue: Boolean): StateFlow<Boolean> =
        booleanFlow(key, defaultValue).asStateFlow()

    fun getIntFlow(key: String, defaultValue: Int): StateFlow<Int> =
        intFlow(key, defaultValue).asStateFlow()

    fun getStringFlow(key: String, defaultValue: String): StateFlow<String> =
        stringFlow(key, defaultValue).asStateFlow()

    /**
     * Settings used to live in SharedPreferences. Copy them once so an
     * in-place update keeps every choice the user already made.
     */
    private fun migrateLegacyPreferences() {
        if (mmkv.decodeBool(KEY_MIGRATED, false)) return
        val legacy = appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        legacy.all.forEach { (key, value) ->
            if (!mmkv.containsKey(key)) {
                when (value) {
                    is Boolean -> mmkv.encode(key, value)
                    is Int -> mmkv.encode(key, value)
                    is Long -> mmkv.encode(key, value)
                    is Float -> mmkv.encode(key, value)
                    is String -> mmkv.encode(key, value)
                }
            }
        }
        mmkv.encode(KEY_MIGRATED, true)
        legacy.edit().clear().apply()
    }

    companion object {
        private const val MMKV_ID = "eclipse_settings"
        private const val LEGACY_PREFS = "eclipse_settings"
        private const val KEY_MIGRATED = "_migrated_from_prefs"
        private const val THEME_MODE = "theme_mode"

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
        const val CHECK_GAME_FILES = "checkGameFiles"
        const val FAST_STARTUP = "fastStartupCheck"
        const val VERIFY_MANIFEST = "verifyManifest"
        const val SHOW_MEMORY_WARNING = "showMemoryWarning"
        const val SHADER_DUMP = "dump_shaders"
        const val BIG_CORE = "bigCoreAffinity"
        const val FREEDRENO_SYSMEM = "freedrenoSysmem"
        const val CURSEFORGE_KEY = "curseforge.key"

        const val DEFAULT_RENDERER = "opengles2"
    }

    /** Reads the persisted theme mode; defaults to dynamic (Material You). */
    fun themeMode(): ThemeMode =
        ThemeMode.valueOf(mmkv.decodeString(THEME_MODE, ThemeMode.DYNAMIC.name) ?: ThemeMode.DYNAMIC.name)

    /** Persists the chosen theme mode. */
    fun setThemeMode(mode: ThemeMode) {
        mmkv.encode(THEME_MODE, mode.name)
    }
}

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
