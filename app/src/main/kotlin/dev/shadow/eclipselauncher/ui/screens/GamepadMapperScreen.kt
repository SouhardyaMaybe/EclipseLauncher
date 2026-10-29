/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shadow.eclipselauncher.data.MojoSettings
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoTheme
import dev.shadow.eclipselauncher.designsystem.theme.MojoType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * `fragment_controller_remapper.xml` + `GamepadMapperAdapter`.
 *
 * The twenty rows are in the reference adapter's order, the four keycode
 * spinners per row are populated from `GamepadMap.getSpecialKeycodeNames()`
 * followed by `KeycodeUtils.generateKeyName()`, and the menu/game maps start
 * at the reference defaults. Selecting "In-game" edits the in-game map,
 * selecting "In menus" edits the menu map; both are stored locally under the
 * reference preference keys. Holding a hardware Select button for 400 ms
 * invokes the same back action as the reference fragment.
 */
@Composable
fun GamepadMapperScreen(
    settings: MojoSettings,
    onBack: () -> Unit,
) {
    var inGame by rememberSaveable { mutableStateOf(false) }
    var rows by remember(inGame) {
        mutableStateOf(loadGamepadRows(settings, inGame))
    }

    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var exitJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MojoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.nativeKeyEvent.keyCode != KeyEvent.KEYCODE_BUTTON_SELECT) {
                        return@onPreviewKeyEvent false
                    }
                    when (event.nativeKeyEvent.action) {
                        KeyEvent.ACTION_DOWN -> {
                            if (exitJob == null) {
                                exitJob = scope.launch {
                                    delay(400)
                                    onBack()
                                }
                            }
                            true
                        }

                        KeyEvent.ACTION_UP -> {
                            exitJob?.cancel()
                            exitJob = null
                            true
                        }

                        else -> false
                    }
                }
                .focusable(),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Hold",
                            style = MojoType.body,
                            color = MojoPalette.PrimaryText,
                        )
                        Spacer(Modifier.width(4.dp))
                        GamepadGlyph(
                            action = GamepadAction.SELECT,
                            size = 30.dp,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "to exit",
                            style = MojoType.body,
                            color = MojoPalette.PrimaryText,
                        )
                    }
                    ModeSelector(
                        inGame = inGame,
                        onChange = { inGame = it },
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(rows, key = { it.action.name }) { row ->
                        GamepadRow(
                            row = row,
                            inGame = inGame,
                            onKeyChange = { index, keycode ->
                                val updated = rows.map {
                                    if (it.action != row.action) {
                                        it
                                    } else {
                                        it.copy(
                                            keys = it.keys.toMutableList().also { keys ->
                                                keys[index] = keycode
                                            },
                                        )
                                    }
                                }
                                rows = updated
                                saveGamepadRows(settings, inGame, updated)
                            },
                            onToggleableChange = { checked ->
                                val updated = rows.map {
                                    if (it.action != row.action) {
                                        it
                                    } else {
                                        it.copy(toggleable = checked)
                                    }
                                }
                                rows = updated
                                saveGamepadRows(settings, inGame, updated)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(
    inGame: Boolean,
    onChange: (Boolean) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .clickable(role = Role.Button) { open = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Current mode",
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (inGame) "In-game" else "In menus",
                    style = MojoType.body,
                    color = MojoPalette.PrimaryText,
                )
                Spacer(Modifier.width(4.dp))
                EclipsePixelIcon(
                    kind = EclipsePixelIconKind.CHEVRON_UP,
                    modifier = Modifier
                        .size(10.dp)
                        .rotate(180f),
                    tint = MojoPalette.PrimaryText,
                )
            }
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier
                .width(180.dp)
                .background(MojoPalette.StatusBar),
            shape = RoundedCornerShape(0.dp),
            containerColor = MojoPalette.StatusBar,
        ) {
            listOf(false to "In menus", true to "In-game").forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .background(
                            if (value == inGame) MojoPalette.Overlay else Color.Transparent,
                        )
                        .clickable(role = Role.Button) {
                            open = false
                            onChange(value)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MojoType.body,
                        color = MojoPalette.PrimaryText,
                    )
                }
            }
        }
    }
}

@Composable
private fun GamepadRow(
    row: GamepadRowState,
    inGame: Boolean,
    onKeyChange: (Int, Int) -> Unit,
    onToggleableChange: (Boolean) -> Unit,
) {
    var expanded by remember(row.action, inGame) { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GamepadGlyph(
                action = row.action,
                size = 55.dp,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = keycodeLabel(row.keys),
                modifier = Modifier.weight(1f),
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            EclipsePixelIcon(
                kind = EclipsePixelIconKind.CHEVRON_UP,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(if (expanded) 0f else 180f),
                tint = MojoPalette.PrimaryText,
                description = "Expand to change keycodes",
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                ) {
                    KeySpinner(
                        value = row.keys[0],
                        onValueChange = { onKeyChange(0, it) },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "+",
                        modifier = Modifier
                            .width(24.dp)
                            .align(Alignment.CenterVertically),
                        style = MojoType.body,
                        color = MojoPalette.PrimaryText,
                        textAlign = TextAlign.Center,
                    )
                    KeySpinner(
                        value = row.keys[1],
                        onValueChange = { onKeyChange(1, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .padding(top = 6.dp),
                ) {
                    KeySpinner(
                        value = row.keys[2],
                        onValueChange = { onKeyChange(2, it) },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "+",
                        modifier = Modifier
                            .width(24.dp)
                            .align(Alignment.CenterVertically),
                        style = MojoType.body,
                        color = MojoPalette.PrimaryText,
                        textAlign = TextAlign.Center,
                    )
                    KeySpinner(
                        value = row.keys[3],
                        onValueChange = { onKeyChange(3, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.action.canToggle) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .padding(top = 6.dp)
                            .height(48.dp)
                            .toggleable(
                                value = row.toggleable,
                                role = Role.Switch,
                                onValueChange = onToggleableChange,
                            )
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Toggleable",
                            modifier = Modifier.weight(1f),
                            style = MojoType.body,
                            color = MojoPalette.PrimaryText,
                        )
                        Switch(
                            checked = row.toggleable,
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
            }
        }
    }
}

@Composable
private fun KeySpinner(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val selected = spinnerIndex(value)

    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(role = Role.Button) { open = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = KEY_NAMES[selected],
                modifier = Modifier.weight(1f),
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier
                .width(300.dp)
                .heightIn(max = 420.dp)
                .background(MojoPalette.StatusBar),
            shape = RoundedCornerShape(0.dp),
            containerColor = MojoPalette.StatusBar,
        ) {
            KEY_NAMES.forEachIndexed { index, name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .background(
                            if (index == selected) MojoPalette.Overlay else Color.Transparent,
                        )
                        .clickable(role = Role.Button) {
                            open = false
                            onValueChange(keycodeForSelection(index))
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        style = MojoType.body,
                        color = MojoPalette.PrimaryText,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * The original drawables are not part of the approved asset set. These glyphs
 * keep the same button identity (face letter, start/select pill, trigger,
 * shoulder, stick direction, thumbstick click, D-pad arm) without copying the
 * reference vectors.
 */
@Composable
private fun GamepadGlyph(
    action: GamepadAction,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = action.label },
        contentAlignment = Alignment.Center,
    ) {
        when (action) {
            GamepadAction.A,
            GamepadAction.B,
            GamepadAction.X,
            GamepadAction.Y,
            -> FaceButton(
                letter = action.letter,
                color = faceColor(action),
                size = size,
            )

            GamepadAction.START,
            GamepadAction.SELECT,
            -> PillButton(
                text = when {
                    size < 40.dp && action == GamepadAction.SELECT -> "SEL"
                    size < 40.dp -> "START"
                    else -> action.letter
                },
                size = size,
            )

            GamepadAction.TRIGGER_RIGHT,
            GamepadAction.TRIGGER_LEFT,
            -> TriggerGlyph(
                text = action.shortLabel,
                size = size,
            )

            GamepadAction.SHOULDER_RIGHT,
            GamepadAction.SHOULDER_LEFT,
            -> ShoulderGlyph(
                text = action.shortLabel,
                size = size,
            )

            GamepadAction.STICK_UP,
            GamepadAction.STICK_RIGHT,
            GamepadAction.STICK_LEFT,
            GamepadAction.STICK_DOWN,
            -> StickGlyph(
                direction = action.stickDirection,
                size = size,
            )

            GamepadAction.THUMBSTICK_RIGHT,
            GamepadAction.THUMBSTICK_LEFT,
            -> ThumbstickClickGlyph(
                text = action.letter,
                size = size,
            )

            GamepadAction.DPAD_UP,
            GamepadAction.DPAD_DOWN,
            GamepadAction.DPAD_RIGHT,
            GamepadAction.DPAD_LEFT,
            -> DPadGlyph(
                direction = action.stickDirection,
                size = size,
            )
        }
    }
}

@Composable
private fun FaceButton(
    letter: String,
    color: Color,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .size(size * 0.9f)
            .background(color, CircleShape)
            .border(2.dp, color.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MojoType.body.copy(
                fontSize = (size.value * 0.36f).sp,
                lineHeight = (size.value * 0.40f).sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MojoPalette.Background,
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .width(size * 0.92f)
            .height(size * 0.58f)
            .background(MojoPalette.SecondaryButton, RoundedCornerShape(size * 0.29f))
            .border(2.dp, MojoPalette.Accent, RoundedCornerShape(size * 0.29f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MojoType.body.copy(
                fontSize = maxOf(size.value * 0.11f, 5f).sp,
                lineHeight = maxOf(size.value * 0.13f, 6f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            color = MojoPalette.Accent,
            maxLines = 1,
        )
    }
}

@Composable
private fun TriggerGlyph(
    text: String,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .width(size * 0.94f)
            .height(size * 0.56f)
            .background(
                MojoPalette.SecondaryButton,
                RoundedCornerShape(
                    topStart = size * 0.22f,
                    topEnd = size * 0.22f,
                    bottomStart = size * 0.08f,
                    bottomEnd = size * 0.08f,
                ),
            )
            .border(
                2.dp,
                MojoPalette.SecondaryText,
                RoundedCornerShape(
                    topStart = size * 0.22f,
                    topEnd = size * 0.22f,
                    bottomStart = size * 0.08f,
                    bottomEnd = size * 0.08f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MojoType.body.copy(
                fontSize = (size.value * 0.22f).sp,
                lineHeight = (size.value * 0.24f).sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MojoPalette.PrimaryText,
        )
    }
}

@Composable
private fun ShoulderGlyph(
    text: String,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .width(size * 0.94f)
            .height(size * 0.44f)
            .background(MojoPalette.SecondaryButton, RoundedCornerShape(size * 0.14f))
            .border(2.dp, MojoPalette.SecondaryText, RoundedCornerShape(size * 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MojoType.body.copy(
                fontSize = (size.value * 0.22f).sp,
                lineHeight = (size.value * 0.24f).sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MojoPalette.PrimaryText,
        )
    }
}

@Composable
private fun StickGlyph(
    direction: StickDirection,
    size: Dp,
) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        // The `size: Dp` parameter shadows `DrawScope.size`; the glyph is
        // drawn against the canvas' pixel size.
        val radius = this.size.minDimension / 2f * 0.82f
        drawCircle(
            color = MojoPalette.SecondaryText,
            radius = radius,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawCircle(
            color = MojoPalette.SecondaryText,
            radius = radius * 0.48f,
            style = Stroke(width = 2.dp.toPx()),
        )
        val angle = when (direction) {
            StickDirection.UP -> -Math.PI / 2
            StickDirection.RIGHT -> 0.0
            StickDirection.DOWN -> Math.PI / 2
            StickDirection.LEFT -> Math.PI
        }
        val tip = Offset(
            x = center.x + (cos(angle) * radius * 0.86).toFloat(),
            y = center.y + (sin(angle) * radius * 0.86).toFloat(),
        )
        val base = Offset(
            x = center.x + (cos(angle) * radius * 0.48).toFloat(),
            y = center.y + (sin(angle) * radius * 0.48).toFloat(),
        )
        val perpendicular = Offset(
            x = (-sin(angle) * radius * 0.20).toFloat(),
            y = (cos(angle) * radius * 0.20).toFloat(),
        )
        val path = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(base.x + perpendicular.x, base.y + perpendicular.y)
            lineTo(base.x - perpendicular.x, base.y - perpendicular.y)
            close()
        }
        drawPath(path, MojoPalette.Accent)
    }
}

@Composable
private fun ThumbstickClickGlyph(
    text: String,
    size: Dp,
) {
    Box(contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f * 0.82f
            drawCircle(
                color = MojoPalette.SecondaryText,
                radius = radius,
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = MojoPalette.Accent,
                radius = radius * 0.52f,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        Text(
            text = text,
            style = MojoType.body.copy(
                fontSize = (size.value * 0.20f).sp,
                lineHeight = (size.value * 0.22f).sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MojoPalette.Accent,
        )
    }
}

@Composable
private fun DPadGlyph(
    direction: StickDirection,
    size: Dp,
) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val armWidth = this.size.minDimension * 0.30f
        val armLength = this.size.minDimension * 0.86f
        val corner = CornerRadius(armWidth * 0.16f)
        drawRoundRect(
            color = MojoPalette.SecondaryText,
            topLeft = Offset(center.x - armWidth / 2f, center.y - armLength / 2f),
            size = Size(armWidth, armLength),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = MojoPalette.SecondaryText,
            topLeft = Offset(center.x - armLength / 2f, center.y - armWidth / 2f),
            size = Size(armLength, armWidth),
            cornerRadius = corner,
        )
        when (direction) {
            StickDirection.UP -> drawRoundRect(
                color = MojoPalette.Accent,
                topLeft = Offset(center.x - armWidth / 2f, center.y - armLength / 2f),
                size = Size(armWidth, armLength / 2f),
                cornerRadius = corner,
            )

            StickDirection.DOWN -> drawRoundRect(
                color = MojoPalette.Accent,
                topLeft = Offset(center.x - armWidth / 2f, center.y),
                size = Size(armWidth, armLength / 2f),
                cornerRadius = corner,
            )

            StickDirection.LEFT -> drawRoundRect(
                color = MojoPalette.Accent,
                topLeft = Offset(center.x - armLength / 2f, center.y - armWidth / 2f),
                size = Size(armLength / 2f, armWidth),
                cornerRadius = corner,
            )

            StickDirection.RIGHT -> drawRoundRect(
                color = MojoPalette.Accent,
                topLeft = Offset(center.x, center.y - armWidth / 2f),
                size = Size(armLength / 2f, armWidth),
                cornerRadius = corner,
            )
        }
    }
}

private enum class StickDirection { UP, RIGHT, DOWN, LEFT }

private enum class GamepadAction(
    val label: String,
    val letter: String,
    val shortLabel: String,
    val stickDirection: StickDirection = StickDirection.UP,
    val canToggle: Boolean = true,
    val menuDefaults: List<Int>,
    val gameDefaults: List<Int>,
    val gameToggleable: Boolean = false,
) {
    A(
        label = "A",
        letter = "A",
        shortLabel = "A",
        menuDefaults = four(MOUSE_LEFT),
        gameDefaults = four(KeyEvent.KEYCODE_SPACE),
    ),
    B(
        label = "B",
        letter = "B",
        shortLabel = "B",
        menuDefaults = four(KeyEvent.KEYCODE_ESCAPE),
        gameDefaults = four(KeyEvent.KEYCODE_Q),
    ),
    X(
        label = "X",
        letter = "X",
        shortLabel = "X",
        menuDefaults = four(MOUSE_RIGHT),
        gameDefaults = four(KeyEvent.KEYCODE_E),
    ),
    Y(
        label = "Y",
        letter = "Y",
        shortLabel = "Y",
        menuDefaults = four(KeyEvent.KEYCODE_SHIFT_LEFT, MOUSE_RIGHT),
        gameDefaults = four(KeyEvent.KEYCODE_F),
    ),
    START(
        label = "Start",
        letter = "START",
        shortLabel = "START",
        menuDefaults = four(),
        gameDefaults = four(KeyEvent.KEYCODE_ESCAPE),
    ),
    SELECT(
        label = "Select",
        letter = "SELECT",
        shortLabel = "SELECT",
        menuDefaults = four(KeyEvent.KEYCODE_ESCAPE),
        gameDefaults = four(KeyEvent.KEYCODE_TAB),
    ),
    TRIGGER_RIGHT(
        label = "Right Trigger",
        letter = "RT",
        shortLabel = "RT",
        menuDefaults = four(),
        gameDefaults = four(MOUSE_LEFT),
    ),
    TRIGGER_LEFT(
        label = "Left Trigger",
        letter = "LT",
        shortLabel = "LT",
        menuDefaults = four(),
        gameDefaults = four(MOUSE_RIGHT),
    ),
    SHOULDER_RIGHT(
        label = "Right Shoulder",
        letter = "RB",
        shortLabel = "RB",
        menuDefaults = four(SCROLL_DOWN),
        gameDefaults = four(SCROLL_DOWN),
    ),
    SHOULDER_LEFT(
        label = "Left Shoulder",
        letter = "LB",
        shortLabel = "LB",
        menuDefaults = four(SCROLL_UP),
        gameDefaults = four(SCROLL_UP),
    ),
    STICK_UP(
        label = "Stick Up",
        letter = "UP",
        shortLabel = "UP",
        stickDirection = StickDirection.UP,
        canToggle = false,
        menuDefaults = listOf(SCROLL_UP, SCROLL_UP, SCROLL_UP, SCROLL_UP),
        gameDefaults = four(KeyEvent.KEYCODE_W),
    ),
    STICK_RIGHT(
        label = "Stick Right",
        letter = "RIGHT",
        shortLabel = "RIGHT",
        stickDirection = StickDirection.RIGHT,
        canToggle = false,
        menuDefaults = four(),
        gameDefaults = four(KeyEvent.KEYCODE_D),
    ),
    STICK_LEFT(
        label = "Stick Left",
        letter = "LEFT",
        shortLabel = "LEFT",
        stickDirection = StickDirection.LEFT,
        canToggle = false,
        menuDefaults = four(),
        gameDefaults = four(KeyEvent.KEYCODE_A),
    ),
    STICK_DOWN(
        label = "Stick Down",
        letter = "DOWN",
        shortLabel = "DOWN",
        stickDirection = StickDirection.DOWN,
        canToggle = false,
        menuDefaults = listOf(SCROLL_DOWN, SCROLL_DOWN, SCROLL_DOWN, SCROLL_DOWN),
        gameDefaults = four(KeyEvent.KEYCODE_S),
    ),
    THUMBSTICK_RIGHT(
        label = "Right Thumbstick (click)",
        letter = "R3",
        shortLabel = "R3",
        menuDefaults = four(),
        gameDefaults = four(KeyEvent.KEYCODE_SHIFT_LEFT),
        gameToggleable = true,
    ),
    THUMBSTICK_LEFT(
        label = "Left Thumbstick (click)",
        letter = "L3",
        shortLabel = "L3",
        menuDefaults = four(),
        gameDefaults = four(KeyEvent.KEYCODE_CTRL_LEFT),
    ),
    DPAD_UP(
        label = "D-Pad Up",
        letter = "UP",
        shortLabel = "UP",
        stickDirection = StickDirection.UP,
        menuDefaults = four(),
        gameDefaults = four(KeyEvent.KEYCODE_SHIFT_LEFT),
    ),
    DPAD_DOWN(
        label = "D-Pad Down",
        letter = "DOWN",
        shortLabel = "DOWN",
        stickDirection = StickDirection.DOWN,
        menuDefaults = four(KeyEvent.KEYCODE_O),
        gameDefaults = four(KeyEvent.KEYCODE_O),
    ),
    DPAD_RIGHT(
        label = "D-Pad Right",
        letter = "RIGHT",
        shortLabel = "RIGHT",
        stickDirection = StickDirection.RIGHT,
        menuDefaults = four(KeyEvent.KEYCODE_K),
        gameDefaults = four(KeyEvent.KEYCODE_K),
    ),
    DPAD_LEFT(
        label = "D-Pad Left",
        letter = "LEFT",
        shortLabel = "LEFT",
        stickDirection = StickDirection.LEFT,
        menuDefaults = four(KeyEvent.KEYCODE_J),
        gameDefaults = four(KeyEvent.KEYCODE_J),
    ),
}

private data class GamepadRowState(
    val action: GamepadAction,
    val keys: List<Int>,
    val toggleable: Boolean,
)

private fun four(vararg keycodes: Int): List<Int> =
    keycodes.toList() + List(4 - keycodes.size) { UNSPECIFIED }

private fun faceColor(action: GamepadAction): Color = when (action) {
    GamepadAction.A -> Color(0xFF75C043)
    GamepadAction.B -> Color(0xFFE05A4E)
    GamepadAction.X -> Color(0xFF4A90E2)
    GamepadAction.Y -> Color(0xFFF2C94C)
    else -> MojoPalette.Accent
}

private fun keycodeLabel(keys: List<Int>): String {
    val names = keys
        .map { KEY_NAMES[spinnerIndex(it)] }
        .filter { it != SPECIAL_NAMES[0] }
    return if (names.isEmpty()) SPECIAL_NAMES[0] else names.joinToString(" + ")
}

private fun spinnerIndex(keycode: Int): Int {
    if (keycode < 0) return keycode + SPECIAL_NAMES.size
    val index = ANDROID_KEYCODES.binarySearch(keycode)
    return (if (index < 1) 0 else index) + SPECIAL_NAMES.size
}

private fun keycodeForSelection(selection: Int): Int =
    if (selection <= SPECIAL_NAMES.size) {
        selection - SPECIAL_NAMES.size
    } else {
        ANDROID_KEYCODES[selection - SPECIAL_NAMES.size]
    }

private fun loadGamepadRows(
    settings: MojoSettings,
    inGame: Boolean,
): List<GamepadRowState> {
    val defaults = GamepadAction.entries.map { action ->
        GamepadRowState(
            action = action,
            keys = if (inGame) action.gameDefaults else action.menuDefaults,
            toggleable = inGame && action.gameToggleable,
        )
    }
    val stored = settings.string(gamepadStoreKey(inGame), "")
    if (stored.isBlank()) return defaults

    val parsed = stored.split(";").mapNotNull { entry ->
        val parts = entry.split("|")
        if (parts.size != 3) return@mapNotNull null
        val action = GamepadAction.entries.firstOrNull { it.name == parts[0] }
            ?: return@mapNotNull null
        val keys = parts[1].split(",").mapNotNull { it.toIntOrNull() }
        if (keys.size != 4) return@mapNotNull null
        action to GamepadRowState(
            action = action,
            keys = keys,
            toggleable = action.canToggle && parts[2] == "1",
        )
    }.toMap()

    return defaults.map { parsed[it.action] ?: it }
}

private fun saveGamepadRows(
    settings: MojoSettings,
    inGame: Boolean,
    rows: List<GamepadRowState>,
) {
    val encoded = rows.joinToString(";") { row ->
        "${row.action.name}|${row.keys.joinToString(",")}|${if (row.toggleable) 1 else 0}"
    }
    settings.setString(gamepadStoreKey(inGame), encoded)
}

private fun gamepadStoreKey(inGame: Boolean): String =
    if (inGame) GAMEPAD_GAME_KEY else GAMEPAD_MENU_KEY

internal fun resetGamepadMappings(settings: MojoSettings) {
    settings.setString(GAMEPAD_MENU_KEY, "")
    settings.setString(GAMEPAD_GAME_KEY, "")
}

private const val GAMEPAD_MENU_KEY = "gamepad_menu_map"
private const val GAMEPAD_GAME_KEY = "gamepad_game_map"

private const val UNSPECIFIED = -6
private const val MOUSE_RIGHT = -5
private const val MOUSE_MIDDLE = -4
private const val MOUSE_LEFT = -3
private const val SCROLL_UP = -2
private const val SCROLL_DOWN = -1

private val SPECIAL_NAMES = listOf(
    "UNSPECIFIED",
    "MOUSE_RIGHT",
    "MOUSE_MIDDLE",
    "MOUSE_LEFT",
    "SCROLL_UP",
    "SCROLL_DOWN",
)

/**
 * `KeycodeUtils.sAndroidKeycodes` in declaration order. The reference array is
 * declared as 106 entries but only receives 105 `add` calls, leaving a trailing
 * zero. That trailing entry is part of the spinner list, so it is kept here.
 */
internal val ANDROID_KEYCODES = intArrayOf(
    KeyEvent.KEYCODE_UNKNOWN,
    KeyEvent.KEYCODE_BACK,
    KeyEvent.KEYCODE_0,
    KeyEvent.KEYCODE_1,
    KeyEvent.KEYCODE_2,
    KeyEvent.KEYCODE_3,
    KeyEvent.KEYCODE_4,
    KeyEvent.KEYCODE_5,
    KeyEvent.KEYCODE_6,
    KeyEvent.KEYCODE_7,
    KeyEvent.KEYCODE_8,
    KeyEvent.KEYCODE_9,
    KeyEvent.KEYCODE_POUND,
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_A,
    KeyEvent.KEYCODE_B,
    KeyEvent.KEYCODE_C,
    KeyEvent.KEYCODE_D,
    KeyEvent.KEYCODE_E,
    KeyEvent.KEYCODE_F,
    KeyEvent.KEYCODE_G,
    KeyEvent.KEYCODE_H,
    KeyEvent.KEYCODE_I,
    KeyEvent.KEYCODE_J,
    KeyEvent.KEYCODE_K,
    KeyEvent.KEYCODE_L,
    KeyEvent.KEYCODE_M,
    KeyEvent.KEYCODE_N,
    KeyEvent.KEYCODE_O,
    KeyEvent.KEYCODE_P,
    KeyEvent.KEYCODE_Q,
    KeyEvent.KEYCODE_R,
    KeyEvent.KEYCODE_S,
    KeyEvent.KEYCODE_T,
    KeyEvent.KEYCODE_U,
    KeyEvent.KEYCODE_V,
    KeyEvent.KEYCODE_W,
    KeyEvent.KEYCODE_X,
    KeyEvent.KEYCODE_Y,
    KeyEvent.KEYCODE_Z,
    KeyEvent.KEYCODE_COMMA,
    KeyEvent.KEYCODE_PERIOD,
    KeyEvent.KEYCODE_ALT_LEFT,
    KeyEvent.KEYCODE_ALT_RIGHT,
    KeyEvent.KEYCODE_SHIFT_LEFT,
    KeyEvent.KEYCODE_SHIFT_RIGHT,
    KeyEvent.KEYCODE_TAB,
    KeyEvent.KEYCODE_SPACE,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_DEL,
    KeyEvent.KEYCODE_GRAVE,
    KeyEvent.KEYCODE_MINUS,
    KeyEvent.KEYCODE_EQUALS,
    KeyEvent.KEYCODE_LEFT_BRACKET,
    KeyEvent.KEYCODE_RIGHT_BRACKET,
    KeyEvent.KEYCODE_BACKSLASH,
    KeyEvent.KEYCODE_SEMICOLON,
    KeyEvent.KEYCODE_APOSTROPHE,
    KeyEvent.KEYCODE_SLASH,
    KeyEvent.KEYCODE_AT,
    KeyEvent.KEYCODE_PLUS,
    KeyEvent.KEYCODE_PAGE_UP,
    KeyEvent.KEYCODE_PAGE_DOWN,
    KeyEvent.KEYCODE_ESCAPE,
    KeyEvent.KEYCODE_CTRL_LEFT,
    KeyEvent.KEYCODE_CTRL_RIGHT,
    KeyEvent.KEYCODE_CAPS_LOCK,
    KeyEvent.KEYCODE_BREAK,
    KeyEvent.KEYCODE_MOVE_HOME,
    KeyEvent.KEYCODE_MOVE_END,
    KeyEvent.KEYCODE_INSERT,
    KeyEvent.KEYCODE_F1,
    KeyEvent.KEYCODE_F2,
    KeyEvent.KEYCODE_F3,
    KeyEvent.KEYCODE_F4,
    KeyEvent.KEYCODE_F5,
    KeyEvent.KEYCODE_F6,
    KeyEvent.KEYCODE_F7,
    KeyEvent.KEYCODE_F8,
    KeyEvent.KEYCODE_F9,
    KeyEvent.KEYCODE_F10,
    KeyEvent.KEYCODE_F11,
    KeyEvent.KEYCODE_F12,
    KeyEvent.KEYCODE_NUM_LOCK,
    KeyEvent.KEYCODE_NUMPAD_0,
    KeyEvent.KEYCODE_NUMPAD_1,
    KeyEvent.KEYCODE_NUMPAD_2,
    KeyEvent.KEYCODE_NUMPAD_3,
    KeyEvent.KEYCODE_NUMPAD_4,
    KeyEvent.KEYCODE_NUMPAD_5,
    KeyEvent.KEYCODE_NUMPAD_6,
    KeyEvent.KEYCODE_NUMPAD_7,
    KeyEvent.KEYCODE_NUMPAD_8,
    KeyEvent.KEYCODE_NUMPAD_9,
    KeyEvent.KEYCODE_NUMPAD_DIVIDE,
    KeyEvent.KEYCODE_NUMPAD_MULTIPLY,
    KeyEvent.KEYCODE_NUMPAD_SUBTRACT,
    KeyEvent.KEYCODE_NUMPAD_ADD,
    KeyEvent.KEYCODE_NUMPAD_DOT,
    KeyEvent.KEYCODE_NUMPAD_COMMA,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
    KeyEvent.KEYCODE_NUMPAD_EQUALS,
    0,
)

private val KEY_NAMES: List<String> by lazy {
    SPECIAL_NAMES + ANDROID_KEYCODES.map { keycode ->
        KeyEvent.keyCodeToString(keycode).removePrefix("KEYCODE_")
    }
}
