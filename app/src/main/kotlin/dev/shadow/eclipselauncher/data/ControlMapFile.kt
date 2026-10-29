/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.KeyEvent
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.Random
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.max

/**
 * The data behind `CustomControlsActivity` / `ControlLayout`.
 *
 * The reference stores the editor model as Gson JSON, optionally wrapped in a
 * ZIP together with background bitmaps. This file keeps that on-disk shape so
 * maps created here remain loadable by the reference and vice versa.
 */

internal enum class DrawerOrientation {
    DOWN,
    LEFT,
    UP,
    RIGHT,
    FREE,
}

internal enum class ControlKind {
    BUTTON,
    DRAWER,
    SUB_BUTTON,
    JOYSTICK,
}

internal data class ControlProperties(
    val name: String = "button",
    val keycodes: List<Int> = List(4) { KeyEvent.KEYCODE_UNKNOWN },
    val dynamicX: String = "100.0",
    val dynamicY: String = "100.0",
    val width: Float = 50f,
    val height: Float = 50f,
    val opacity: Float = 1f,
    val bgColor: Int = 0x4D000000,
    val strokeColor: Int = -1,
    val strokeWidth: Float = 0f,
    val cornerRadius: Float = 0f,
    val isToggle: Boolean = false,
    val isSwipeable: Boolean = false,
    val passThruEnabled: Boolean = false,
    val displayInGame: Boolean = true,
    val displayInMenu: Boolean = true,
    val bitmapTag: String? = null,
    val forwardLock: Boolean = false,
    val absolute: Boolean = false,
)

internal data class EditorControl(
    val id: Long,
    val kind: ControlKind,
    val properties: ControlProperties,
    val orientation: DrawerOrientation = DrawerOrientation.LEFT,
    val children: List<EditorControl> = emptyList(),
)

internal data class ControlMap(
    val buttons: List<EditorControl> = emptyList(),
    val drawers: List<EditorControl> = emptyList(),
    val joysticks: List<EditorControl> = emptyList(),
    val scaledAt: Float = 100f,
) {
    fun maxId(): Long =
        (buttons + drawers + joysticks + drawers.flatMap { it.children })
            .maxOfOrNull { it.id }
            ?: 0L

    fun find(id: Long): EditorControl? {
        buttons.firstOrNull { it.id == id }?.let { return it }
        drawers.firstOrNull { it.id == id }?.let { return it }
        joysticks.firstOrNull { it.id == id }?.let { return it }
        drawers.forEach { drawer ->
            drawer.children.firstOrNull { it.id == id }?.let { return it }
        }
        return null
    }

    fun findWithParent(id: Long): Pair<EditorControl, EditorControl?>? {
        buttons.firstOrNull { it.id == id }?.let { return it to null }
        drawers.firstOrNull { it.id == id }?.let { return it to null }
        joysticks.firstOrNull { it.id == id }?.let { return it to null }
        drawers.forEach { drawer ->
            drawer.children.firstOrNull { it.id == id }?.let { return it to drawer }
        }
        return null
    }

    fun update(id: Long, transform: (EditorControl) -> EditorControl): ControlMap = copy(
        buttons = buttons.map { if (it.id == id) transform(it) else it },
        drawers = drawers.map { drawer ->
            when {
                drawer.id == id -> transform(drawer)
                drawer.children.any { it.id == id } -> drawer.copy(
                    children = drawer.children.map {
                        if (it.id == id) transform(it) else it
                    },
                )

                else -> drawer
            }
        },
        joysticks = joysticks.map { if (it.id == id) transform(it) else it },
    )

    fun remove(id: Long): ControlMap = copy(
        buttons = buttons.filterNot { it.id == id },
        drawers = drawers.filterNot { it.id == id }.map { drawer ->
            drawer.copy(children = drawer.children.filterNot { it.id == id })
        },
        joysticks = joysticks.filterNot { it.id == id },
    )

    fun addButton(control: EditorControl): ControlMap = copy(buttons = buttons + control)

    fun addDrawer(control: EditorControl): ControlMap = copy(drawers = drawers + control)

    fun addJoystick(control: EditorControl): ControlMap = copy(joysticks = joysticks + control)

    fun addSubButton(drawerId: Long, control: EditorControl): ControlMap = copy(
        drawers = drawers.map {
            if (it.id == drawerId) it.copy(children = it.children + control) else it
        },
    )
}

internal data class ControlPoint(val x: Float, val y: Float)

internal data class LoadedControlMap(
    val layout: ControlMap,
    val bitmaps: Map<String, Bitmap>,
)

internal fun newButtonControl(id: Long, name: String = "New"): EditorControl = EditorControl(
    id = id,
    kind = ControlKind.BUTTON,
    properties = ControlProperties(name = name),
)

internal fun newDrawerControl(id: Long): EditorControl = EditorControl(
    id = id,
    kind = ControlKind.DRAWER,
    properties = ControlProperties(name = "Drawer"),
    orientation = DrawerOrientation.LEFT,
)

internal fun newJoystickControl(id: Long): EditorControl = EditorControl(
    id = id,
    kind = ControlKind.JOYSTICK,
    properties = ControlProperties(),
)

internal fun newSubButtonControl(id: Long): EditorControl = EditorControl(
    id = id,
    kind = ControlKind.SUB_BUTTON,
    properties = ControlProperties(),
)

internal fun ensureDefaultControlMapFile(context: Context, directory: File): File {
    directory.mkdirs()
    val target = File(directory, "default.json")
    if (!target.isFile) {
        context.assets.open("default.json").use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }
    return target
}

internal fun controlMapFileName(file: File, directory: File): String {
    val full = file.absolutePath
    val root = directory.absolutePath
    val relative = if (full.startsWith(root)) {
        "." + full.removePrefix(root)
    } else {
        full
    }
    return relative.removeSuffix(".json")
}

internal fun controlMapTarget(directory: File, name: String): File =
    File(directory, "$name.json")

internal fun controlMapTag(random: Random = Random(System.nanoTime())): String =
    random.nextInt().toString()

internal fun readControlMap(
    file: File,
    screenWidth: Int,
    screenHeight: Int,
    preferredScale: Float,
): LoadedControlMap {
    val bytes = file.readBytes()
    val isZip = bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
    val bitmaps = mutableMapOf<String, Bitmap>()
    val json = if (isZip) {
        var layoutJson: String? = null
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                if (entry.name == "layout.json") {
                    layoutJson = zip.readBytes().toString(Charsets.UTF_8)
                } else {
                    BitmapFactory.decodeStream(zip)?.let { bitmaps[entry.name] = it }
                }
                zip.closeEntry()
            }
        }
        layoutJson ?: throw IOException("Incorrect ZIP file structure")
    } else {
        if (bytes.size > 25L * 1024L * 1024L) {
            throw IOException("Raw JSON control data size too large")
        }
        bytes.toString(Charsets.UTF_8)
    }

    return LoadedControlMap(
        layout = parseControlMap(json, screenWidth, screenHeight, preferredScale),
        bitmaps = bitmaps,
    )
}

private fun parseControlMap(
    json: String,
    screenWidth: Int,
    screenHeight: Int,
    preferredScale: Float,
): ControlMap {
    val root = JSONObject(json)
    val hasVersion = root.has("version")
    val version = if (hasVersion) root.optInt("version", -1) else 1
    if (version > 9) {
        throw IOException("Layout version $version is too new, only up to 9 is supported")
    }

    val ids = ControlIdSource()
    var controls: List<EditorControl>
    var drawers: List<EditorControl>
    var joysticks: List<EditorControl>
    var scaledAt = root.optDouble("scaledAt", 100.0).toFloat().takeIf { it > 0f } ?: 100f

    if (!hasVersion) {
        // LayoutConverter.convertV1Layout: there are only plain controls.
        controls = parseArray(root.optJSONArray("mControlDataList"), ids) { item, id ->
            parseV1Control(item, id, screenWidth, screenHeight)
        }
        drawers = emptyList()
        joysticks = emptyList()
        scaledAt = root.optDouble("scaledAt", scaledAt.toDouble()).toFloat()
            .takeIf { it > 0f } ?: 100f
    } else {
        controls = parseArray(root.optJSONArray("mControlDataList"), ids) { item, id ->
            parseControl(item, id, ControlKind.BUTTON)
        }
        drawers = parseArray(root.optJSONArray("mDrawerDataList"), ids) { item, id ->
            parseDrawer(item, id, version, screenWidth, screenHeight, ids)
        }
        joysticks = parseArray(root.optJSONArray("mJoystickDataList"), ids) { item, id ->
            parseControl(item, id, ControlKind.JOYSTICK)
        }
    }

    if (hasVersion && version == 2) {
        controls = controls.map { convertLegacyPosition(it, screenWidth, screenHeight) }
        drawers = drawers.map { drawer ->
            val converted = convertLegacyPosition(drawer, screenWidth, screenHeight)
            converted.copy(
                children = converted.children.map {
                    convertLegacyPosition(it, screenWidth, screenHeight)
                },
            )
        }
    }

    if (!hasVersion || version == 2) {
        controls = controls.map(::convertLegacyKeycodes)
        drawers = drawers.map { drawer ->
            drawer.copy(
                properties = convertLegacyKeycodes(drawer).properties,
                children = drawer.children.map(::convertLegacyKeycodes),
            )
        }
    }

    if (hasVersion && version in 3..5) {
        controls = controls.map(::convertLegacyStrokeWidth)
        drawers = drawers.map { drawer ->
            drawer.copy(
                properties = convertLegacyStrokeWidth(drawer).properties,
                children = drawer.children.map(::convertLegacyStrokeWidth),
            )
        }
    }

    if (hasVersion && version in 6..7) {
        joysticks = joysticks.map(::convertLegacyJoystick)
    }

    if (hasVersion && version == 8) {
        controls = controls.map(::convertLegacyKeycodes)
        drawers = drawers.map { drawer ->
            drawer.copy(
                properties = convertLegacyKeycodes(drawer).properties,
                children = drawer.children.map(::convertLegacyKeycodes),
            )
        }
        joysticks = joysticks.map(::convertLegacyKeycodes)
    }

    fun rescale(control: EditorControl): EditorControl = control.copy(
        properties = control.properties.copy(
            width = control.properties.width / scaledAt * preferredScale,
            height = control.properties.height / scaledAt * preferredScale,
        ),
        children = control.children.map(::rescale),
    )

    controls = controls.map(::rescale)
    drawers = drawers.map(::rescale)
    joysticks = joysticks.map(::rescale)

    return ControlMap(
        buttons = controls,
        drawers = drawers,
        joysticks = joysticks,
        scaledAt = preferredScale,
    )
}

private class ControlIdSource {
    private var next = 1L
    fun next(): Long = next++
}

private inline fun parseArray(
    array: JSONArray?,
    ids: ControlIdSource,
    parser: (JSONObject, Long) -> EditorControl,
): List<EditorControl> {
    if (array == null) return emptyList()
    val result = ArrayList<EditorControl>(array.length())
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        result += parser(item, ids.next())
    }
    return result
}

private fun parseDrawer(
    item: JSONObject,
    id: Long,
    version: Int,
    screenWidth: Int,
    screenHeight: Int,
    ids: ControlIdSource,
): EditorControl {
    val propertiesJson = item.optJSONObject("properties") ?: JSONObject()
    val orientation = runCatching {
        DrawerOrientation.valueOf(
            item.optString("orientation", "LEFT").uppercase(Locale.US),
        )
    }.getOrDefault(DrawerOrientation.LEFT)
    val children = parseArray(item.optJSONArray("buttonProperties"), ids) { child, childId ->
        val parsed = parseControl(child, childId, ControlKind.SUB_BUTTON)
        if (version == 2) convertLegacyPosition(parsed, screenWidth, screenHeight) else parsed
    }
    return EditorControl(
        id = id,
        kind = ControlKind.DRAWER,
        properties = parseProperties(propertiesJson),
        orientation = orientation,
        children = children,
    )
}

private fun parseControl(
    item: JSONObject,
    id: Long,
    kind: ControlKind,
): EditorControl = EditorControl(
    id = id,
    kind = kind,
    properties = parseProperties(item),
)

private fun parseV1Control(
    item: JSONObject,
    id: Long,
    screenWidth: Int,
    screenHeight: Int,
): EditorControl {
    val keycodes = MutableList(4) { KeyEvent.KEYCODE_UNKNOWN }
    var keyIndex = 0
    if (item.optBoolean("holdShift", false)) {
        keycodes[keyIndex++] = KeyEvent.KEYCODE_SHIFT_LEFT
    }
    if (item.optBoolean("holdCtrl", false)) {
        keycodes[keyIndex++] = KeyEvent.KEYCODE_CTRL_LEFT
    }
    if (item.optBoolean("holdAlt", false)) {
        keycodes[keyIndex++] = KeyEvent.KEYCODE_ALT_LEFT
    }
    keycodes[keyIndex] = keyCodeFromGlfw(item.optInt("keycode", 0))

    val width = item.optDouble("width", 50.0).toFloat()
    val height = item.optDouble("height", 50.0).toFloat()
    val transparency = item.optInt("transparency", 100)
    return EditorControl(
        id = id,
        kind = ControlKind.BUTTON,
        properties = ControlProperties(
            name = item.optString("name", "button"),
            keycodes = keycodes,
            dynamicX = item.optString("dynamicX", "")
                .ifBlank { legacyDynamic(item, "x", screenWidth, "width") },
            dynamicY = item.optString("dynamicY", "")
                .ifBlank { legacyDynamic(item, "y", screenHeight, "height") },
            width = width,
            height = height,
            opacity = ((transparency - 100) * -1) / 100f,
            bgColor = 0x4D000000,
            strokeWidth = 0f,
            cornerRadius = if (item.optBoolean("isRound", false)) 35f else 0f,
            isToggle = item.optBoolean("isToggle", false),
            passThruEnabled = item.optBoolean("passThruEnabled", false),
            displayInGame = true,
            displayInMenu = true,
        ),
    )
}

private fun parseProperties(item: JSONObject): ControlProperties {
    val keycodes = run {
        val array = item.optJSONArray("keycodes")
        val values = MutableList(4) { KeyEvent.KEYCODE_UNKNOWN }
        if (array != null) {
            for (index in 0 until minOf(4, array.length())) {
                values[index] = array.optInt(index, KeyEvent.KEYCODE_UNKNOWN)
            }
        }
        values
    }
    return ControlProperties(
        name = item.optString("name", "button"),
        keycodes = keycodes,
        dynamicX = item.optString("dynamicX", "100.0").ifBlank { "100.0" },
        dynamicY = item.optString("dynamicY", "100.0").ifBlank { "100.0" },
        width = item.optDouble("width", 50.0).toFloat(),
        height = item.optDouble("height", 50.0).toFloat(),
        opacity = item.optDouble("opacity", 1.0).toFloat(),
        bgColor = item.optInt("bgColor", 0x4D000000),
        strokeColor = item.optInt("strokeColor", -1),
        strokeWidth = item.optDouble("strokeWidth", 0.0).toFloat(),
        cornerRadius = item.optDouble("cornerRadius", 0.0).toFloat(),
        isToggle = item.optBoolean("isToggle", false),
        isSwipeable = item.optBoolean("isSwipeable", false),
        passThruEnabled = item.optBoolean("passThruEnabled", false),
        displayInGame = item.optBoolean("displayInGame", false),
        displayInMenu = item.optBoolean("displayInMenu", false),
        bitmapTag = item.optString("bitmapTag", "").takeIf { it.isNotBlank() },
        forwardLock = item.optBoolean("forwardLock", false),
        absolute = item.optBoolean("absolute", false),
    )
}

private fun legacyDynamic(
    item: JSONObject,
    coordinateKey: String,
    screenSize: Int,
    dimensionName: String,
): String {
    val coordinate = item.optDouble(coordinateKey, Double.NaN)
    return if (!coordinate.isNaN() && screenSize > 0) {
        "${coordinate / screenSize} * \${screen_$dimensionName}"
    } else {
        "100.0"
    }
}

private fun convertLegacyPosition(
    control: EditorControl,
    screenWidth: Int,
    screenHeight: Int,
): EditorControl {
    val properties = control.properties
    val dynamicX = if (properties.dynamicX.isBlank()) {
        legacyDynamicFromValue(properties, screenWidth, true)
    } else {
        properties.dynamicX
    }
    val dynamicY = if (properties.dynamicY.isBlank()) {
        legacyDynamicFromValue(properties, screenHeight, false)
    } else {
        properties.dynamicY
    }
    return control.copy(properties = properties.copy(dynamicX = dynamicX, dynamicY = dynamicY))
}

private fun legacyDynamicFromValue(
    properties: ControlProperties,
    screenSize: Int,
    horizontal: Boolean,
): String = if (screenSize > 0) {
    val size = if (horizontal) properties.width else properties.height
    "0.5 * \${screen_${if (horizontal) "width" else "height"}} - $size / 2"
} else {
    "100.0"
}

private fun convertLegacyKeycodes(control: EditorControl): EditorControl = control.copy(
    properties = control.properties.copy(
        keycodes = control.properties.keycodes.map { keycode ->
            if (keycode > 0) keyCodeFromGlfw(keycode) else keycode
        },
    ),
)

private fun convertLegacyStrokeWidth(control: EditorControl): EditorControl {
    val properties = control.properties
    val stroke = max(properties.width, properties.height) / 2f *
        (properties.strokeWidth / 100f)
    return control.copy(properties = properties.copy(strokeWidth = stroke))
}

private fun convertLegacyJoystick(control: EditorControl): EditorControl {
    val properties = control.properties
    if (properties.height <= properties.width) return control
    val ratio = properties.height / properties.width
    val dynamicX = properties.dynamicX.replace(
        "\${height}",
        "($ratio * \${height})",
    )
    val dynamicY = properties.dynamicY.replace(
        "\${height}",
        "($ratio * \${height})",
    ) + " + (${ratio - 1} * \${height})"
    return control.copy(
        properties = properties.copy(
            dynamicX = dynamicX,
            dynamicY = dynamicY,
            height = properties.width,
        ),
    )
}

/**
 * `LayoutConverter.keyCodeFromGLFW`. Printable GLFW keys use their ASCII value,
 * so the letter and digit runs can be mapped arithmetically; the remaining
 * named keys are listed explicitly.
 */
private fun keyCodeFromGlfw(keycode: Int): Int = when (keycode) {
    32 -> KeyEvent.KEYCODE_SPACE
    39 -> KeyEvent.KEYCODE_APOSTROPHE
    44 -> KeyEvent.KEYCODE_COMMA
    45 -> KeyEvent.KEYCODE_MINUS
    46 -> KeyEvent.KEYCODE_PERIOD
    47 -> KeyEvent.KEYCODE_SLASH
    in 48..57 -> KeyEvent.KEYCODE_0 + (keycode - 48)
    59 -> KeyEvent.KEYCODE_SEMICOLON
    61 -> KeyEvent.KEYCODE_EQUALS
    in 65..90 -> KeyEvent.KEYCODE_A + (keycode - 65)
    91 -> KeyEvent.KEYCODE_LEFT_BRACKET
    92 -> KeyEvent.KEYCODE_BACKSLASH
    93 -> KeyEvent.KEYCODE_RIGHT_BRACKET
    96 -> KeyEvent.KEYCODE_GRAVE
    256 -> KeyEvent.KEYCODE_ESCAPE
    257 -> KeyEvent.KEYCODE_ENTER
    258 -> KeyEvent.KEYCODE_TAB
    259 -> KeyEvent.KEYCODE_DEL
    260 -> KeyEvent.KEYCODE_INSERT
    261 -> KeyEvent.KEYCODE_FORWARD_DEL
    262 -> KeyEvent.KEYCODE_DPAD_RIGHT
    263 -> KeyEvent.KEYCODE_DPAD_LEFT
    264 -> KeyEvent.KEYCODE_DPAD_DOWN
    265 -> KeyEvent.KEYCODE_DPAD_UP
    266 -> KeyEvent.KEYCODE_PAGE_UP
    267 -> KeyEvent.KEYCODE_PAGE_DOWN
    268 -> KeyEvent.KEYCODE_MOVE_HOME
    269 -> KeyEvent.KEYCODE_MOVE_END
    280 -> KeyEvent.KEYCODE_CAPS_LOCK
    281 -> KeyEvent.KEYCODE_SCROLL_LOCK
    282 -> KeyEvent.KEYCODE_NUM_LOCK
    283 -> KeyEvent.KEYCODE_SYSRQ
    284 -> KeyEvent.KEYCODE_BREAK
    in 290..301 -> KeyEvent.KEYCODE_F1 + (keycode - 290)
    in 320..329 -> KeyEvent.KEYCODE_NUMPAD_0 + (keycode - 320)
    330 -> KeyEvent.KEYCODE_NUMPAD_DOT
    331 -> KeyEvent.KEYCODE_NUMPAD_DIVIDE
    333 -> KeyEvent.KEYCODE_NUMPAD_SUBTRACT
    334 -> KeyEvent.KEYCODE_NUMPAD_ADD
    335 -> KeyEvent.KEYCODE_NUMPAD_ENTER
    336 -> KeyEvent.KEYCODE_NUMPAD_EQUALS
    340 -> KeyEvent.KEYCODE_SHIFT_LEFT
    341 -> KeyEvent.KEYCODE_CTRL_LEFT
    342 -> KeyEvent.KEYCODE_ALT_LEFT
    344 -> KeyEvent.KEYCODE_SHIFT_RIGHT
    345 -> KeyEvent.KEYCODE_CTRL_RIGHT
    346 -> KeyEvent.KEYCODE_ALT_RIGHT
    else -> KeyEvent.KEYCODE_UNKNOWN
}

internal fun saveControlMap(
    file: File,
    layout: ControlMap,
    bitmaps: Map<String, Bitmap>,
    density: Float,
    screenWidth: Int,
    screenHeight: Int,
    preferredScale: Float,
) {
    file.parentFile?.mkdirs()
    val normalized = syncDrawerChildren(
        layout = layout,
        density = density,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        preferredScale = preferredScale,
    )
    val json = controlMapJson(normalized, preferredScale)
    if (bitmaps.isEmpty()) {
        file.writeText(json, Charsets.UTF_8)
        return
    }

    ZipOutputStream(BufferedOutputStream(file.outputStream())).use { zip ->
        zip.putNextEntry(ZipEntry("layout.json"))
        zip.write(json.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        for ((tag, bitmap) in bitmaps) {
            zip.putNextEntry(ZipEntry(tag))
            @Suppress("DEPRECATION")
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, zip)
            zip.closeEntry()
        }
    }
}

private fun controlMapJson(layout: ControlMap, preferredScale: Float): String {
    val root = JSONObject()
    root.put("version", 9)
    root.put("scaledAt", preferredScale.toDouble())
    root.put(
        "mControlDataList",
        JSONArray().also { array -> layout.buttons.forEach { array.put(propertiesJson(it)) } },
    )
    root.put(
        "mDrawerDataList",
        JSONArray().also { array ->
            layout.drawers.forEach { drawer ->
                val children = JSONArray()
                drawer.children.forEach { children.put(propertiesJson(it)) }
                array.put(
                    JSONObject().apply {
                        put("buttonProperties", children)
                        put("properties", propertiesJson(drawer))
                        put("orientation", drawer.orientation.name)
                    },
                )
            }
        },
    )
    root.put(
        "mJoystickDataList",
        JSONArray().also { array -> layout.joysticks.forEach { array.put(propertiesJson(it)) } },
    )
    return root.toString()
}

private fun propertiesJson(control: EditorControl): JSONObject {
    val properties = control.properties
    return JSONObject().apply {
        put("dynamicX", properties.dynamicX)
        put("dynamicY", properties.dynamicY)
        put("isToggle", properties.isToggle)
        put("passThruEnabled", properties.passThruEnabled)
        put("name", properties.name)
        put("keycodes", JSONArray().also { array -> properties.keycodes.forEach { array.put(it) } })
        put("opacity", properties.opacity.toDouble())
        put("bgColor", properties.bgColor)
        put("strokeColor", properties.strokeColor)
        put("strokeWidth", properties.strokeWidth.toDouble())
        put("cornerRadius", properties.cornerRadius.toDouble())
        put("isSwipeable", properties.isSwipeable)
        put("displayInGame", properties.displayInGame)
        put("displayInMenu", properties.displayInMenu)
        if (properties.bitmapTag != null) put("bitmapTag", properties.bitmapTag)
        put("width", properties.width.toDouble())
        put("height", properties.height.toDouble())
        if (control.kind == ControlKind.JOYSTICK) {
            put("forwardLock", properties.forwardLock)
            put("absolute", properties.absolute)
        }
    }
}

internal fun controlPosition(
    properties: ControlProperties,
    screenWidth: Int,
    screenHeight: Int,
    density: Float,
    preferredScale: Float,
): ControlPoint {
    val width = properties.width * density
    val height = properties.height * density
    val margin = 2f * density
    val values = mapOf(
        "top" to 0f,
        "left" to 0f,
        "right" to screenWidth - width,
        "bottom" to screenHeight - height,
        "width" to width,
        "height" to height,
        "screen_width" to screenWidth.toFloat(),
        "screen_height" to screenHeight.toFloat(),
        "margin" to margin,
        "preferred_scale" to preferredScale,
    )
    return ControlPoint(
        x = evaluateExpression(properties.dynamicX, values),
        y = evaluateExpression(properties.dynamicY, values),
    )
}

internal fun syncDrawerChildren(
    layout: ControlMap,
    density: Float,
    screenWidth: Int,
    screenHeight: Int,
    preferredScale: Float,
): ControlMap = layout.copy(
    drawers = layout.drawers.map { drawer ->
        if (drawer.orientation == DrawerOrientation.FREE || drawer.children.isEmpty()) {
            drawer
        } else {
            val origin = controlPosition(
                drawer.properties,
                screenWidth,
                screenHeight,
                density,
                preferredScale,
            )
            val widthPx = drawer.properties.width * density
            val heightPx = drawer.properties.height * density
            val margin = 2f * density
            val children = drawer.children.mapIndexed { index, child ->
                val x = when (drawer.orientation) {
                    DrawerOrientation.RIGHT, DrawerOrientation.LEFT -> {
                        val direction = if (drawer.orientation == DrawerOrientation.RIGHT) 1f else -1f
                        origin.x + direction * (widthPx + margin) * (index + 1)
                    }

                    else -> origin.x
                }
                val y = when (drawer.orientation) {
                    DrawerOrientation.DOWN, DrawerOrientation.UP -> {
                        val direction = if (drawer.orientation == DrawerOrientation.DOWN) 1f else -1f
                        origin.y + direction * (heightPx + margin) * (index + 1)
                    }

                    else -> origin.y
                }
                child.copy(
                    properties = child.properties.copy(
                        dynamicX = generateDynamicX(x, screenWidth, widthPx),
                        dynamicY = generateDynamicY(y, screenHeight, heightPx),
                        width = drawer.properties.width,
                        height = drawer.properties.height,
                    ),
                )
            }
            drawer.copy(children = children)
        }
    },
)

internal fun drawerChildPosition(
    drawer: EditorControl,
    index: Int,
    screenWidth: Int,
    screenHeight: Int,
    density: Float,
    preferredScale: Float,
): ControlPoint {
    val origin = controlPosition(
        drawer.properties,
        screenWidth,
        screenHeight,
        density,
        preferredScale,
    )
    if (drawer.orientation == DrawerOrientation.FREE) {
        val child = drawer.children[index]
        return controlPosition(
            child.properties,
            screenWidth,
            screenHeight,
            density,
            preferredScale,
        )
    }
    val widthPx = drawer.properties.width * density
    val heightPx = drawer.properties.height * density
    val margin = 2f * density
    val x = when (drawer.orientation) {
        DrawerOrientation.RIGHT, DrawerOrientation.LEFT -> {
            val direction = if (drawer.orientation == DrawerOrientation.RIGHT) 1f else -1f
            origin.x + direction * (widthPx + margin) * (index + 1)
        }

        else -> origin.x
    }
    val y = when (drawer.orientation) {
        DrawerOrientation.DOWN, DrawerOrientation.UP -> {
            val direction = if (drawer.orientation == DrawerOrientation.DOWN) 1f else -1f
            origin.y + direction * (heightPx + margin) * (index + 1)
        }

        else -> origin.y
    }
    return ControlPoint(x, y)
}

internal fun generateDynamicX(x: Float, screenWidth: Int, width: Float): String =
    if (screenWidth <= 0) {
        "100.0"
    } else if (x + width / 2f > screenWidth / 2f) {
        "${(x + width) / screenWidth} * \${screen_width} - \${width}"
    } else {
        "${x / screenWidth} * \${screen_width}"
    }

internal fun generateDynamicY(y: Float, screenHeight: Int, height: Float): String =
    if (screenHeight <= 0) {
        "100.0"
    } else if (y + height / 2f > screenHeight / 2f) {
        "${(y + height) / screenHeight} * \${screen_height} - \${height}"
    } else {
        "${y / screenHeight} * \${screen_height}"
    }

/**
 * `ControlData.insertDynamicPos` uses exp4j. Only the arithmetic needed by
 * shipped and generated layouts is implemented here; the `${...}` tokens are
 * substituted first, exactly like `JSONUtils.insertSingleJSONValue`.
 */
internal fun evaluateExpression(expression: String, values: Map<String, Float>): Float {
    val substituted = buildString {
        var index = 0
        while (index < expression.length) {
            if (expression[index] == '$' && index + 1 < expression.length &&
                expression[index + 1] == '{'
            ) {
                val end = expression.indexOf('}', index + 2)
                if (end < 0) {
                    append(expression.substring(index))
                    break
                }
                val key = expression.substring(index + 2, end)
                append(values[key] ?: 0f)
                index = end + 1
            } else {
                append(expression[index])
                index++
            }
        }
    }
    return runCatching { ExpressionParser(substituted).parse() }.getOrDefault(0f)
}

private class ExpressionParser(private val text: String) {
    private var position = 0

    fun parse(): Float {
        val value = parseExpression()
        skipWhitespace()
        return value
    }

    private fun parseExpression(): Float {
        var value = parseTerm()
        while (true) {
            skipWhitespace()
            when {
                consume('+') -> value += parseTerm()
                consume('-') -> value -= parseTerm()
                else -> return value
            }
        }
    }

    private fun parseTerm(): Float {
        var value = parseFactor()
        while (true) {
            skipWhitespace()
            when {
                consume('*') -> value *= parseFactor()
                consume('/') -> {
                    val divisor = parseFactor()
                    value = if (divisor == 0f) Float.POSITIVE_INFINITY else value / divisor
                }

                else -> return value
            }
        }
    }

    private fun parseFactor(): Float {
        skipWhitespace()
        if (consume('-')) return -parseFactor()
        if (consume('+')) return parseFactor()
        if (consume('(')) {
            val value = parseExpression()
            skipWhitespace()
            consume(')')
            return value
        }
        return parseNumber()
    }

    private fun parseNumber(): Float {
        skipWhitespace()
        val start = position
        while (position < text.length &&
            (text[position].isDigit() || text[position] == '.' ||
                text[position] == 'e' || text[position] == 'E' ||
                ((text[position] == '-' || text[position] == '+') &&
                    position > start && (text[position - 1] == 'e' || text[position - 1] == 'E')))
        ) {
            position++
        }
        return text.substring(start, position).toFloatOrNull() ?: 0f
    }

    private fun skipWhitespace() {
        while (position < text.length && text[position].isWhitespace()) position++
    }

    private fun consume(character: Char): Boolean {
        if (position < text.length && text[position] == character) {
            position++
            return true
        }
        return false
    }
}
