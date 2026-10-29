/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import dev.shadow.eclipselauncher.data.ControlKind
import dev.shadow.eclipselauncher.data.ControlMap
import dev.shadow.eclipselauncher.data.ControlPoint
import dev.shadow.eclipselauncher.data.DrawerOrientation
import dev.shadow.eclipselauncher.data.EditorControl
import dev.shadow.eclipselauncher.data.controlPosition
import dev.shadow.eclipselauncher.data.drawerChildPosition
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Shared geometry and key-binding helpers for the rebuilt custom-control
 * editor. Coordinates stay in device pixels, exactly like the reference
 * layout, so the stored dynamic expressions keep working across devices.
 */
internal data class EditorMetrics(
    val width: Int,
    val height: Int,
    val density: Float,
    val preferredScale: Float,
)

internal data class ControlGeometry(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
}

internal fun EditorMetrics.controlGeometry(
    control: EditorControl,
    drawer: EditorControl? = null,
): ControlGeometry {
    if (control.kind == ControlKind.SUB_BUTTON && drawer != null &&
        drawer.orientation != DrawerOrientation.FREE
    ) {
        val index = drawer.children.indexOfFirst { it.id == control.id }.coerceAtLeast(0)
        val position = drawerChildPosition(
            drawer = drawer,
            index = index,
            screenWidth = width,
            screenHeight = height,
            density = density,
            preferredScale = preferredScale,
        )
        return ControlGeometry(
            x = position.x,
            y = position.y,
            width = drawer.properties.width * density,
            height = drawer.properties.height * density,
        )
    }
    val position = controlPosition(
        properties = control.properties,
        screenWidth = width,
        screenHeight = height,
        density = density,
        preferredScale = preferredScale,
    )
    val storedWidth = control.properties.width * density
    val storedHeight = control.properties.height * density
    // `ControlJoystick` is measured as a square: JoystickView takes the
    // smallest of its two measured sides. The editor draws, hits and snaps the
    // same square.
    val squareSide = if (control.kind == ControlKind.JOYSTICK) {
        minOf(storedWidth, storedHeight)
    } else {
        null
    }
    return ControlGeometry(
        x = position.x,
        y = position.y,
        width = squareSide ?: storedWidth,
        height = squareSide ?: storedHeight,
    )
}

internal fun ControlMap.allEditorControls(): List<Pair<EditorControl, EditorControl?>> {
    val result = ArrayList<Pair<EditorControl, EditorControl?>>()
    joysticks.forEach { result += it to null }
    buttons.forEach { result += it to null }
    drawers.forEach { drawer ->
        result += drawer to null
        drawer.children.forEach { child -> result += child to drawer }
    }
    return result
}

internal fun ControlMap.referencedBitmapTags(): Set<String> =
    allEditorControls().mapNotNull { it.first.properties.bitmapTag }.toSet()

/**
 * `ControlInterface.snapAndAlign`, reduced to the visible behaviour: edges
 * snap together with the reference 2dp margin, and a snapped pair is aligned
 * on the perpendicular axis. The resulting position is then stored as a
 * dynamic expression by the caller.
 */
internal fun snapControlPosition(
    layout: ControlMap,
    movingId: Long,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    metrics: EditorMetrics,
): Offset {
    val threshold = 6f * metrics.density
    val margin = 2f * metrics.density
    var resultX = x
    var resultY = y

    for ((other, drawer) in layout.allEditorControls()) {
        if (other.id == movingId) continue
        // A drawer cannot snap onto its own children.
        if (drawer != null && drawer.id == movingId) continue
        if (drawer != null && other.id == movingId) continue
        val geometry = metrics.controlGeometry(other, drawer)
        if (geometry.width <= 0f || geometry.height <= 0f) continue

        val otherTop = geometry.y
        val otherBottom = geometry.y + geometry.height
        val otherLeft = geometry.x
        val otherRight = geometry.x + geometry.width

        var snappedY = false
        if (abs(resultY - otherBottom) < threshold) {
            resultY = otherBottom + margin
            snappedY = true
        } else if (abs(otherTop - (resultY + height)) < threshold) {
            resultY = otherTop - height - margin
            snappedY = true
        }
        if (snappedY) {
            if (abs(resultX - otherLeft) < threshold) {
                resultX = otherLeft
            } else if (abs(otherRight - (resultX + width)) < threshold) {
                resultX = otherRight - width
            }
        }

        var snappedX = false
        if (abs(resultX - otherRight) < threshold) {
            resultX = otherRight + margin
            snappedX = true
        } else if (abs(otherLeft - (resultX + width)) < threshold) {
            resultX = otherLeft - width - margin
            snappedX = true
        }
        if (snappedX) {
            if (abs(resultY - otherTop) < threshold) {
                resultY = otherTop
            } else if (abs(otherBottom - (resultY + height)) < threshold) {
                resultY = otherBottom - height
            }
        }
    }

    return Offset(resultX, resultY)
}

/** The reversed `ControlData.buildSpecialButtonArray()` order. */
internal val CONTROL_SPECIAL_NAMES = listOf(
    "SPECIAL_KeyboardPan",
    "SPECIAL_MENU",
    "SPECIAL_SCROLLDOWN",
    "SPECIAL_SCROLLUP",
    "SPECIAL_MID",
    "SPECIAL_Mouse",
    "SPECIAL_SEC",
    "SPECIAL_PRI",
    "SPECIAL_GUI",
    "SPECIAL_Keyboard",
)

internal val CONTROL_KEY_NAMES: List<String> by lazy {
    CONTROL_SPECIAL_NAMES + ANDROID_KEYCODES.map { keycode ->
        KeyEvent.keyCodeToString(keycode).removePrefix("KEYCODE_")
    }
}

internal fun keycodeSelection(keycode: Int): Int {
    if (keycode < 0) return keycode + CONTROL_SPECIAL_NAMES.size
    val found = ANDROID_KEYCODES.binarySearch(keycode)
    val index = if (found < 1) 0 else found
    return index + CONTROL_SPECIAL_NAMES.size
}

internal fun selectionKeycode(selection: Int): Int {
    if (selection < CONTROL_SPECIAL_NAMES.size) {
        return selection - CONTROL_SPECIAL_NAMES.size
    }
    val index = selection - CONTROL_SPECIAL_NAMES.size
    return ANDROID_KEYCODES.getOrElse(index) { KeyEvent.KEYCODE_UNKNOWN }
}

internal fun Throwable.stackTraceText(): String =
    StringWriter().also { writer -> printStackTrace(PrintWriter(writer)) }.toString()

internal fun Float.formatEditorNumber(): String {
    if (this == roundToInt().toFloat()) return roundToInt().toString()
    return String.format(java.util.Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
}

internal fun fitSelectionRect(
    width: Float,
    height: Float,
    aspectRatio: Float,
    padding: Float,
): androidx.compose.ui.geometry.Rect {
    val safeAspect = aspectRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
    val lesser = max(0f, minOf(width, height) - padding)
    var targetWidth = lesser
    var targetHeight = lesser
    if (safeAspect < 1f) {
        targetWidth = lesser * safeAspect
    } else if (safeAspect > 1f) {
        targetHeight = lesser * (1f / safeAspect)
    }
    val left = (width - targetWidth) / 2f
    val top = (height - targetHeight) / 2f
    return androidx.compose.ui.geometry.Rect(
        left = left,
        top = top,
        right = left + targetWidth,
        bottom = top + targetHeight,
    )
}

internal fun clampControlPosition(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    metrics: EditorMetrics,
): ControlPoint {
    // The reference clamps the vertical drag with the layout width, not the
    // layout height. That quirk is preserved so controls stop in the same
    // place they do upstream.
    val maxX = max(0f, metrics.width - width)
    val maxY = max(0f, metrics.width - height)
    return ControlPoint(
        x = x.coerceIn(0f, maxX),
        y = y.coerceIn(0f, maxY),
    )
}
