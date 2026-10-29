/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.data.ControlKind
import dev.shadow.eclipselauncher.data.ControlProperties
import dev.shadow.eclipselauncher.data.DrawerOrientation
import dev.shadow.eclipselauncher.data.EditorControl
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import dev.shadow.eclipselauncher.ui.components.MojoTextField
import java.util.Locale
import kotlin.math.roundToInt

/**
 * `dialog_control_button_setting.xml` and `EditControlSideDialog`.
 *
 * The panel is a side sheet: 280dp wide, flush with the vertical screen edges,
 * with the same fields, in the same order and with the same visibility rules
 * as the reference. Every edit is applied to the editor model immediately,
 * exactly like the reference text watchers, switches and seek bars.
 */
@Composable
internal fun ControlSidePanel(
    control: EditorControl,
    parentDrawer: EditorControl?,
    density: Float,
    panelAtRight: Boolean,
    onPropertiesChange: (ControlProperties) -> Unit,
    onOrientationChange: (DrawerOrientation) -> Unit,
    onBackgroundImage: () -> Unit,
    onBackgroundColor: () -> Unit,
    onStrokeColor: () -> Unit,
) {
    val kind = control.kind
    val properties = control.properties
    val isDrawer = kind == ControlKind.DRAWER
    val isJoystick = kind == ControlKind.JOYSTICK
    val isSubButton = kind == ControlKind.SUB_BUTTON
    val lockedToDrawer = isSubButton && parentDrawer != null &&
        parentDrawer.orientation != DrawerOrientation.FREE
    val hasBitmap = properties.bitmapTag != null

    var nameText by remember(control.id) { mutableStateOf(properties.name) }
    var widthText by remember(control.id) {
        mutableStateOf(formatPixelValue(properties.width * density))
    }
    var heightText by remember(control.id) {
        mutableStateOf(formatPixelValue(properties.height * density))
    }

    LaunchedEffect(control.id, properties.width, density) {
        val formatted = formatPixelValue(properties.width * density)
        if (parsePixelValue(widthText) != properties.width * density) widthText = formatted
    }
    LaunchedEffect(control.id, properties.height, density) {
        val formatted = formatPixelValue(properties.height * density)
        if (parsePixelValue(heightText) != properties.height * density) heightText = formatted
    }

    fun applyWidth(text: String) {
        widthText = text
        val px = text.toFloatOrNull() ?: return
        if (px < 0f) return
        val dp = px / density
        onPropertiesChange(
            if (isJoystick) {
                properties.copy(width = dp, height = dp)
            } else {
                properties.copy(width = dp)
            },
        )
    }

    fun applyHeight(text: String) {
        heightText = text
        val px = text.toFloatOrNull() ?: return
        if (px < 0f) return
        val dp = px / density
        onPropertiesChange(
            if (isJoystick) {
                properties.copy(width = dp, height = dp)
            } else {
                properties.copy(height = dp)
            },
        )
    }

    val consume = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(if (panelAtRight) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .width(280.dp)
                .fillMaxHeight()
                .background(MojoPalette.Background)
                .clickable(
                    interactionSource = consume,
                    indication = null,
                    role = Role.Button,
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(6.dp),
            ) {
                if (!isJoystick) {
                    PanelLabel(stringResource(R.string.mojo_name))
                    MojoTextField(
                        value = nameText,
                        onValueChange = {
                            nameText = it
                            onPropertiesChange(properties.copy(name = it))
                        },
                        placeholder = stringResource(R.string.mojo_customctrl_button_name_hint),
                        centerText = true,
                    )
                    Spacer(Modifier.height(4.dp))
                }

                if (!lockedToDrawer) {
                    PanelLabel(stringResource(R.string.mojo_customctrl_size))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MojoTextField(
                            value = widthText,
                            onValueChange = ::applyWidth,
                            modifier = Modifier.weight(1f),
                            decimal = true,
                            centerText = true,
                        )
                        Text(
                            text = "x",
                            modifier = Modifier.padding(horizontal = 3.dp),
                            style = MojoType.body,
                            color = MojoPalette.PrimaryText,
                        )
                        MojoTextField(
                            value = heightText,
                            onValueChange = ::applyHeight,
                            modifier = Modifier.weight(1f),
                            decimal = true,
                            centerText = true,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (!isDrawer && !isJoystick) {
                    PanelLabel(stringResource(R.string.mojo_customctrl_mapping))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KeycodeField(
                            selection = keycodeSelection(properties.keycodes[0]),
                            modifier = Modifier.weight(1f),
                        ) { selection ->
                            val codes = properties.keycodes.toMutableList()
                            codes[0] = selectionKeycode(selection)
                            onPropertiesChange(properties.copy(keycodes = codes))
                        }
                        Text("+", style = MojoType.body, color = MojoPalette.PrimaryText)
                        KeycodeField(
                            selection = keycodeSelection(properties.keycodes[1]),
                            modifier = Modifier.weight(1f),
                        ) { selection ->
                            val codes = properties.keycodes.toMutableList()
                            codes[1] = selectionKeycode(selection)
                            onPropertiesChange(properties.copy(keycodes = codes))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KeycodeField(
                            selection = keycodeSelection(properties.keycodes[2]),
                            modifier = Modifier.weight(1f),
                        ) { selection ->
                            val codes = properties.keycodes.toMutableList()
                            codes[2] = selectionKeycode(selection)
                            onPropertiesChange(properties.copy(keycodes = codes))
                        }
                        Text("+", style = MojoType.body, color = MojoPalette.PrimaryText)
                        KeycodeField(
                            selection = keycodeSelection(properties.keycodes[3]),
                            modifier = Modifier.weight(1f),
                        ) { selection ->
                            val codes = properties.keycodes.toMutableList()
                            codes[3] = selectionKeycode(selection)
                            onPropertiesChange(properties.copy(keycodes = codes))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (isDrawer) {
                    PanelLabel(stringResource(R.string.mojo_customctrl_orientation))
                    OrientationField(control.orientation, onOrientationChange)
                    Spacer(Modifier.height(4.dp))
                }

                if (!isDrawer && !isJoystick) {
                    PanelSwitch(
                        label = stringResource(R.string.mojo_customctrl_toggle),
                        checked = properties.isToggle,
                    ) { onPropertiesChange(properties.copy(isToggle = it)) }
                    PanelSwitch(
                        label = stringResource(R.string.mojo_customctrl_passthru),
                        checked = properties.passThruEnabled,
                    ) { onPropertiesChange(properties.copy(passThruEnabled = it)) }
                    PanelSwitch(
                        label = stringResource(R.string.mojo_customctrl_swipeable),
                        checked = properties.isSwipeable,
                    ) { onPropertiesChange(properties.copy(isSwipeable = it)) }
                }

                if (isJoystick) {
                    PanelSwitch(
                        label = stringResource(R.string.mojo_customctrl_forward_lock),
                        checked = properties.forwardLock,
                    ) { onPropertiesChange(properties.copy(forwardLock = it)) }
                    PanelSwitch(
                        label = stringResource(R.string.mojo_customctrl_absolute_tracking),
                        checked = properties.absolute,
                    ) { onPropertiesChange(properties.copy(absolute = it)) }
                }

                if (!isJoystick) {
                    PanelValueRow(
                        label = stringResource(R.string.mojo_customctrl_background_bitmap),
                        onClick = onBackgroundImage,
                    )
                }
                PanelValueRow(
                    label = stringResource(R.string.mojo_customctrl_background_color),
                    onClick = onBackgroundColor,
                )
                if (hasBitmap) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "⚠",
                            style = MojoType.body,
                            color = MojoPalette.PrimaryText,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.mojo_customctrl_background_color_warning),
                            style = MojoType.bodySmall,
                            color = MojoPalette.SecondaryText,
                        )
                    }
                }

                if (!hasBitmap) {
                    PanelValueRow(
                        label = stringResource(R.string.mojo_customctrl_stroke_color),
                        onClick = onStrokeColor,
                    )
                    PercentSlider(
                        label = stringResource(R.string.mojo_customctrl_stroke_width),
                        progress = (properties.strokeWidth * 10f).roundToInt().coerceIn(0, 100),
                    ) { progress ->
                        onPropertiesChange(properties.copy(strokeWidth = progress / 10f))
                    }
                    if (!isJoystick) {
                        PercentSlider(
                            label = stringResource(R.string.mojo_customctrl_corner_radius),
                            progress = properties.cornerRadius.roundToInt().coerceIn(0, 100),
                        ) { progress ->
                            onPropertiesChange(properties.copy(cornerRadius = progress.toFloat()))
                        }
                    }
                }

                PercentSlider(
                    label = stringResource(R.string.mojo_customctrl_button_opacity),
                    progress = (properties.opacity * 100f).roundToInt().coerceIn(0, 100),
                ) { progress ->
                    onPropertiesChange(properties.copy(opacity = progress / 100f))
                }

                if (!isSubButton) {
                    PanelLabel(stringResource(R.string.mojo_customctrl_visibility_title))
                    PanelCheckbox(
                        label = stringResource(R.string.mojo_customctrl_visibility_ingame),
                        checked = properties.displayInGame,
                    ) { onPropertiesChange(properties.copy(displayInGame = it)) }
                    PanelCheckbox(
                        label = stringResource(R.string.mojo_customctrl_visibility_in_menus),
                        checked = properties.displayInMenu,
                    ) { onPropertiesChange(properties.copy(displayInMenu = it)) }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PanelLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 2.dp),
        style = MojoType.body,
        color = MojoPalette.PrimaryText,
    )
}

@Composable
private fun PanelValueRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(35.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MojoType.body,
            color = MojoPalette.PrimaryText,
        )
        Text(
            text = "›",
            style = MojoType.title,
            color = MojoPalette.PrimaryText,
        )
    }
}

@Composable
private fun PanelSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MojoType.body,
            color = MojoPalette.PrimaryText,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MojoPalette.Accent,
                checkedTrackColor = MojoPalette.SecondaryButton,
                uncheckedThumbColor = MojoPalette.SecondaryText,
                uncheckedTrackColor = MojoPalette.SecondaryButton,
                uncheckedBorderColor = MojoPalette.Line,
            ),
        )
    }
}

@Composable
private fun PanelCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MojoPalette.Accent,
                uncheckedColor = MojoPalette.Line,
                checkmarkColor = MojoPalette.Background,
            ),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MojoType.body,
            color = MojoPalette.PrimaryText,
        )
    }
}

@Composable
private fun PercentSlider(
    label: String,
    progress: Int,
    onProgress: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
            )
            Text(
                text = stringResource(R.string.mojo_percent_format, progress),
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
            )
        }
        Slider(
            value = progress.toFloat(),
            onValueChange = { onProgress(it.roundToInt().coerceIn(0, 100)) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = MojoPalette.Accent,
                activeTrackColor = MojoPalette.Accent,
                inactiveTrackColor = MojoPalette.Line,
            ),
        )
    }
}

@Composable
private fun KeycodeField(
    selection: Int,
    modifier: Modifier = Modifier,
    onSelection: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val name = CONTROL_KEY_NAMES.getOrElse(selection) { CONTROL_KEY_NAMES.first() }
    Box(modifier = modifier) {
        Text(
            text = name,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(MojoPalette.Input)
                .clickable(role = Role.Button) { expanded = true }
                .padding(horizontal = 2.dp),
            style = MojoType.body,
            color = MojoPalette.PrimaryText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LazyColumn(
                modifier = Modifier
                    .width(260.dp)
                    .heightIn(max = 360.dp),
            ) {
                items(CONTROL_KEY_NAMES.size) { index ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = CONTROL_KEY_NAMES[index],
                                style = MojoType.body,
                                color = MojoPalette.PrimaryText,
                            )
                        },
                        onClick = {
                            onSelection(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OrientationField(
    orientation: DrawerOrientation,
    onOrientationChange: (DrawerOrientation) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = orientation.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(MojoPalette.Input)
                .clickable(role = Role.Button) { expanded = true }
                .padding(horizontal = 6.dp),
            style = MojoType.body,
            color = MojoPalette.PrimaryText,
            textAlign = TextAlign.Center,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            listOf(
                DrawerOrientation.DOWN,
                DrawerOrientation.LEFT,
                DrawerOrientation.UP,
                DrawerOrientation.RIGHT,
                DrawerOrientation.FREE,
            ).forEach { value ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = value.name,
                            style = MojoType.body,
                            color = MojoPalette.PrimaryText,
                        )
                    },
                    onClick = {
                        onOrientationChange(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun formatPixelValue(value: Float): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    return value.formatEditorNumber()
}

private fun parsePixelValue(text: String): Float? = text.toFloatOrNull()

/* ---------------------------------------------------------------------- */
/* Color selector                                                          */
/* ---------------------------------------------------------------------- */

/**
 * `dialog_color_selector.xml` and `ColorSelector`. The panel keeps the
 * reference layout: a 150dp saturation/value field, a 94dp colour sample, a
 * hex field, and 25dp hue (and optionally alpha) bars.
 */
@Composable
internal fun ControlColorPanel(
    initialColor: Int,
    alphaEnabled: Boolean,
    panelAtRight: Boolean,
    onColorChange: (Int) -> Unit,
) {
    var red by remember { mutableFloatStateOf(0f) }
    var green by remember { mutableFloatStateOf(0f) }
    var blue by remember { mutableFloatStateOf(0f) }
    var hue by remember { mutableFloatStateOf(360f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    var alpha by remember { mutableIntStateOf(255) }
    var hexText by remember { mutableStateOf("") }
    var hexValid by remember { mutableStateOf(true) }

    fun syncColor(colorInt: Int, notify: Boolean) {
        red = android.graphics.Color.red(colorInt) / 255f
        green = android.graphics.Color.green(colorInt) / 255f
        blue = android.graphics.Color.blue(colorInt) / 255f
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (red * 255f).roundToInt(),
            (green * 255f).roundToInt(),
            (blue * 255f).roundToInt(),
            hsv,
        )
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        alpha = android.graphics.Color.alpha(colorInt)
        hexText = String.format(Locale.US, "%08X", colorInt)
        hexValid = true
        if (notify) onColorChange(colorInt)
    }

    fun dispatch() {
        val colorInt = android.graphics.Color.HSVToColor(
            alpha,
            floatArrayOf(hue, saturation, value),
        )
        hexText = String.format(Locale.US, "%08X", colorInt)
        hexValid = true
        onColorChange(colorInt)
    }

    LaunchedEffect(initialColor) { syncColor(initialColor, notify = false) }

    val selectedColor = Color(android.graphics.Color.HSVToColor(
        alpha,
        floatArrayOf(hue, saturation, value),
    ))
    val hueColor = Color(android.graphics.Color.HSVToColor(
        floatArrayOf(hue, 1f, 1f),
    ))

    val consume = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(if (panelAtRight) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .width(280.dp)
                .fillMaxHeight()
                .background(MojoPalette.Background)
                .clickable(
                    interactionSource = consume,
                    indication = null,
                    role = Role.Button,
                    onClick = {},
                )
                .padding(6.dp),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    SaturationValueField(
                        hueColor = hueColor,
                        saturation = saturation,
                        value = value,
                        onChange = { newSaturation, newValue ->
                            saturation = newSaturation
                            value = newValue
                            dispatch()
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    ColorSample(
                        color = selectedColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(94.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    HexField(
                        text = hexText,
                        valid = hexValid,
                        onTextChange = { text ->
                            hexText = text
                            val parsed = text.toLongOrNull(16)?.toInt()
                            if (parsed == null) {
                                hexValid = false
                            } else {
                                hexValid = true
                                val oldAlpha = alpha
                                syncColor(parsed, notify = true)
                                // The reference hides the alpha bar for stroke
                                // colours but still keeps the current alpha.
                                if (!alphaEnabled) alpha = oldAlpha
                            }
                        },
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (alphaEnabled) {
                    AlphaBar(
                        color = hueColor,
                        alpha = alpha,
                        onChange = {
                            alpha = it
                            dispatch()
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                }
                HueBar(
                    hue = hue,
                    onChange = {
                        hue = it
                        dispatch()
                    },
                )
            }
        }
    }
}

@Composable
private fun SaturationValueField(
    hueColor: Color,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .pointerInput(Unit) {
                fun update(offset: Offset) {
                    onChange(
                        (offset.x / size.width).coerceIn(0f, 1f),
                        (1f - offset.y / size.height).coerceIn(0f, 1f),
                    )
                }
                detectTapGestures { update(it) }
            }
            .pointerInput(Unit) {
                fun update(offset: Offset) {
                    onChange(
                        (offset.x / size.width).coerceIn(0f, 1f),
                        (1f - offset.y / size.height).coerceIn(0f, 1f),
                    )
                }
                detectDragGestures(
                    onDragStart = { update(it) },
                    onDrag = { change, _ ->
                        change.consume()
                        update(change.position)
                    },
                )
            },
    ) {
        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val pointer = Offset(saturation * size.width, (1f - value) * size.height)
        drawCircle(Color.Black, radius = 6.dp.toPx(), center = pointer)
        drawCircle(
            Color.White,
            radius = 6.dp.toPx(),
            center = pointer,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
private fun ColorSample(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCheckerboard()
        drawRect(color)
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onChange: (Float) -> Unit,
) {
    Canvas(
        modifier = Modifier
            .width(25.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                fun update(offset: Offset) {
                    onChange((offset.y / size.height).coerceIn(0f, 1f) * 360f)
                }
                detectTapGestures { update(it) }
            }
            .pointerInput(Unit) {
                fun update(offset: Offset) {
                    onChange((offset.y / size.height).coerceIn(0f, 1f) * 360f)
                }
                detectDragGestures(
                    onDragStart = { update(it) },
                    onDrag = { change, _ ->
                        change.consume()
                        update(change.position)
                    },
                )
            },
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red,
                ),
            ),
        )
        val y = hue.coerceIn(0f, 360f) / 360f * size.height
        drawLine(
            color = Color.White,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
        )
        drawLine(
            color = Color.Black,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

@Composable
private fun AlphaBar(
    color: Color,
    alpha: Int,
    onChange: (Int) -> Unit,
) {
    Canvas(
        modifier = Modifier
            .width(25.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                fun update(offset: Offset) {
                    onChange(
                        ((1f - offset.y / size.height).coerceIn(0f, 1f) * 255f).roundToInt(),
                    )
                }
                detectTapGestures { update(it) }
            }
            .pointerInput(Unit) {
                fun update(offset: Offset) {
                    onChange(
                        ((1f - offset.y / size.height).coerceIn(0f, 1f) * 255f).roundToInt(),
                    )
                }
                detectDragGestures(
                    onDragStart = { update(it) },
                    onDrag = { change, _ ->
                        change.consume()
                        update(change.position)
                    },
                )
            },
    ) {
        drawCheckerboard()
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    color.copy(alpha = 1f),
                    color.copy(alpha = 0f),
                ),
            ),
        )
        val y = (1f - alpha / 255f) * size.height
        drawLine(
            color = Color.White,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
        )
        drawLine(
            color = Color.Black,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

@Composable
private fun HexField(
    text: String,
    valid: Boolean,
    onTextChange: (String) -> Unit,
) {
    val hint = stringResource(R.string.mojo_color_default_hex)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, if (valid) MojoPalette.Line else MojoPalette.Error),
        contentAlignment = Alignment.Center,
    ) {
        if (text.isEmpty()) {
            Text(
                text = hint,
                style = MojoType.body.copy(fontFamily = FontFamily.Monospace),
                color = MojoPalette.SecondaryText,
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            singleLine = true,
            textStyle = MojoType.body.copy(
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                color = if (valid) MojoPalette.PrimaryText else MojoPalette.Error,
            ),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCheckerboard(
    cell: Float = 8.dp.toPx(),
) {
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = 0f
        var column = 0
        while (x < size.width) {
            drawRect(
                color = if ((row + column) % 2 == 0) Color(0xFF808080) else Color(0xFFC0C0C0),
                topLeft = Offset(x, y),
                size = Size(
                    minOf(cell, size.width - x),
                    minOf(cell, size.height - y),
                ),
            )
            x += cell
            column++
        }
        y += cell
        row++
    }
}

/** Density-aware preview used by the editor while the side panel is open. */
@Composable
internal fun editorDensity(): Float = LocalDensity.current.density
