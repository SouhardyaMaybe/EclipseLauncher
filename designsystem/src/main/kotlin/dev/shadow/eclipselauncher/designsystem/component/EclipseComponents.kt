/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette

/** Original 12×12 pixel glyphs. No game textures or third-party icon packs. */
enum class EclipsePixelIconKind(val pattern: List<String>) {
    ACCOUNT(
        listOf(
            "....####....",
            "...######...",
            "...######...",
            "....####....",
            "............",
            "..########..",
            ".##########.",
            ".##########.",
            ".##########.",
            "............",
            "............",
            "............",
        ),
    ),
    CONTROLS(
        listOf(
            "............",
            "..##....##..",
            ".####..####.",
            ".##########.",
            ".##.####.##.",
            ".##.#..#.##.",
            ".##.####.##.",
            ".##########.",
            "..##....##..",
            "............",
            "............",
            "............",
        ),
    ),
    LOGS(
        listOf(
            "...######...",
            "...#....#...",
            "...#.##.#...",
            "...#....#...",
            "...#.##.#...",
            "...#....#...",
            "...#.##.#...",
            "...#....#...",
            "...######...",
            "............",
            "............",
            "............",
        ),
    ),
    FOLDER(
        listOf(
            "............",
            "..####......",
            ".#....#.....",
            ".#....######",
            ".#.........#",
            ".#.........#",
            ".#.........#",
            ".#.........#",
            ".###########",
            "............",
            "............",
            "............",
        ),
    ),
    CONTENT(
        listOf(
            "............",
            "..########..",
            ".#........#.",
            ".#........#.",
            ".#........#.",
            ".#........#.",
            ".#........#.",
            ".#........#.",
            "..########..",
            "............",
            "............",
            "............",
        ),
    ),
    JAR(
        listOf(
            "..########..",
            "..#......#..",
            "..#..##..#..",
            "..#..##..#..",
            "..#......#..",
            "..#..##..#..",
            "..#..##..#..",
            "..#......#..",
            "..########..",
            "............",
            "............",
            "............",
        ),
    ),
    TASKS(
        listOf(
            "...####.....",
            "..#....#....",
            "..######....",
            "..#....#....",
            "..#.##.#....",
            "..#....#....",
            "..#.##.#....",
            "..#....#....",
            "..######....",
            "............",
            "............",
            "............",
        ),
    ),
    SETTINGS(
        listOf(
            ".....##.....",
            "..##.##.##..",
            ".#...##...#.",
            ".#..####..#.",
            "###.#..#.###",
            ".#..####..#.",
            ".#...##...#.",
            "..##.##.##..",
            ".....##.....",
            "............",
            "............",
            "............",
        ),
    ),
    ARROW_LEFT(
        listOf(
            "............",
            "............",
            "...##.......",
            "..##........",
            ".##.........",
            "##########..",
            "##########..",
            ".##.........",
            "..##........",
            "...##.......",
            "............",
            "............",
        ),
    ),
    FILTER(
        listOf(
            "............",
            "............",
            "############",
            "############",
            ".##########.",
            "..########..",
            "...######...",
            "....####....",
            ".....##.....",
            ".....##.....",
            "............",
            "............",
        ),
    ),
    CHEVRON_RIGHT(
        listOf(
            "............",
            "........#...",
            "........##..",
            ".........##.",
            "..........##",
            ".........##.",
            "........##..",
            "........#...",
            "............",
            "............",
            "............",
            "............",
        ),
    ),
    CHEVRON_UP(
        listOf(
            "............",
            "............",
            "............",
            ".....##.....",
            "....####....",
            "...######...",
            "..########..",
            "............",
            "............",
            "............",
            "............",
            "............",
        ),
    ),
    CHEVRON_DOWN(
        listOf(
            "............",
            "............",
            "............",
            "..########..",
            "...######...",
            "....####....",
            ".....##.....",
            "............",
            "............",
            "............",
            "............",
            "............",
        ),
    ),
    FILE(
        listOf(
            "...######...",
            "...#....#...",
            "...#.##.#...",
            "...#....#...",
            "...#.##.#...",
            "...#....#...",
            "...#.##.#...",
            "...#....#...",
            "...######...",
            "............",
            "............",
            "............",
        ),
    ),
    EDIT(
        listOf(
            "............",
            ".........##.",
            "........####",
            ".......####.",
            "......####..",
            ".....####...",
            "....####....",
            "...####.....",
            "..####......",
            ".###........",
            "##..........",
            "............",
        ),
    ),
    SEARCH(
        listOf(
            "............",
            "...#####....",
            "..#.....#...",
            ".#.......#..",
            ".#.......#..",
            ".#.......#..",
            "..#.....#...",
            "...#####....",
            "........##..",
            ".........##.",
            "..........##",
            "............",
        ),
    ),
    PLUS(
        listOf(
            "............",
            ".....##.....",
            ".....##.....",
            ".....##.....",
            "..########..",
            "..########..",
            ".....##.....",
            ".....##.....",
            ".....##.....",
            "............",
            "............",
            "............",
        ),
    ),
    CHECK(
        listOf(
            "............",
            "..........##",
            ".........##.",
            "........##..",
            "..##...##...",
            ".####.##....",
            "..#####.....",
            "...###......",
            "....#.......",
            "............",
            "............",
            "............",
        ),
    ),
    WORLD(
        listOf(
            "............",
            "...#####....",
            "..#..#..#...",
            ".#...#...#..",
            ".#..###..#..",
            ".#...#...#..",
            ".#..###..#..",
            "..#..#..#...",
            "...#####....",
            "............",
            "............",
            "............",
        ),
    ),
    CLOSE(
        listOf(
            "............",
            "..##....##..",
            ".####..####.",
            "..########..",
            "...######...",
            "....####....",
            "...######...",
            "..########..",
            ".####..####.",
            "..##....##..",
            "............",
            "............",
        ),
    ),
    MORE(
        listOf(
            "............",
            "............",
            "............",
            "..##..##..##",
            "..##..##..##",
            "............",
            "............",
            "............",
            "............",
            "............",
            "............",
            "............",
        ),
    ),
}

@Composable
fun EclipsePixelIcon(
    kind: EclipsePixelIconKind,
    modifier: Modifier = Modifier,
    tint: Color = EclipsePalette.Text,
    description: String? = null,
) {
    val semantics = if (description == null) {
        Modifier
    } else {
        Modifier.semantics { contentDescription = description }
    }
    val mirror = kind == EclipsePixelIconKind.CHEVRON_RIGHT &&
        LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.then(semantics).then(if (mirror) Modifier.scale(-1f, 1f) else Modifier)) {
        val cell = size.minDimension / 12f
        val left = (size.width - cell * 12f) / 2f
        val top = (size.height - cell * 12f) / 2f
        for (y in 0 until 12) {
            for (x in 0 until 12) {
                if (kind.pattern[y][x] == '#') {
                    drawRect(
                        color = tint,
                        topLeft = Offset(left + x * cell, top + y * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
    }
}

/**
 * Draws the 2 dp focus ring from the design tokens. Custom clickable controls
 * do not get one from Material, so every one of them applies this before its
 * `clickable`/`selectable` modifier; `onFocusChanged` has to sit outside the
 * focus target to observe it.
 */
@Composable
fun Modifier.eclipseFocusRing(enabled: Boolean = true): Modifier {
    var focused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { focused = enabled && it.isFocused }
        .border(2.dp, if (focused) EclipsePalette.Focus else Color.Transparent)
}

/**
 * A square button with a two-pixel top edge and a three-pixel bottom edge.
 * The press state is the only motion; there is no shadow or gradient.
 */
@Composable
fun EclipseBevelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    height: Dp = 56.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val face = when {
        !enabled -> EclipsePalette.Control
        pressed && primary -> EclipsePalette.PrimaryPressed
        pressed -> EclipsePalette.ControlPressed
        primary -> EclipsePalette.Primary
        else -> EclipsePalette.Control
    }
    val topEdge = when {
        !enabled -> EclipsePalette.Border
        primary -> EclipsePalette.PrimaryTop
        else -> EclipsePalette.ControlHover
    }
    val bottomEdge = when {
        !enabled -> EclipsePalette.Border
        primary -> EclipsePalette.PrimaryBottom
        else -> EclipsePalette.Canvas
    }
    val content = if (enabled) EclipsePalette.Text else EclipsePalette.Disabled

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .background(face)
            .eclipseFocusRing(enabled)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .drawEdges(topEdge, bottomEdge)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Modifier.drawEdges(top: Color, bottom: Color): Modifier = this.then(
    Modifier.drawBehind {
        drawRect(color = top, size = Size(size.width, 2.dp.toPx()))
        drawRect(
            color = bottom,
            topLeft = Offset(0f, size.height - 3.dp.toPx()),
            size = Size(size.width, 3.dp.toPx()),
        )
    },
)

@Composable
fun EclipseMenuRow(
    icon: EclipsePixelIconKind,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .eclipseFocusRing(enabled)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(start = 22.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EclipsePixelIcon(
            kind = icon,
            modifier = Modifier.size(30.dp),
            tint = if (enabled) EclipsePalette.Text else EclipsePalette.Disabled,
        )
        Spacer(Modifier.width(22.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) EclipsePalette.Text else EclipsePalette.Disabled,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        EclipsePixelIcon(
            kind = EclipsePixelIconKind.CHEVRON_RIGHT,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) EclipsePalette.Muted else EclipsePalette.Disabled,
        )
    }
}

@Composable
fun EclipseSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier
            .padding(start = 22.dp, top = 20.dp, bottom = 8.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.labelSmall,
        color = EclipsePalette.Muted,
    )
}

@Composable
fun EclipseCenteredHeading(
    text: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = 24.dp,
    dividerThickness: Dp = 2.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = EclipsePalette.Text,
            textAlign = TextAlign.Center,
        )
        EclipseDivider(
            modifier = Modifier.padding(top = 12.dp),
            thickness = dividerThickness,
        )
    }
}

@Composable
fun EclipseTopBar(
    title: String,
    backContentDescription: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(EclipsePalette.AccountBar)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backContentDescription,
                    tint = EclipsePalette.Text,
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = EclipsePalette.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

@Composable
fun EclipseTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .eclipseFocusRing(enabled)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !enabled -> EclipsePalette.Disabled
                selected -> EclipsePalette.Text
                else -> EclipsePalette.Muted
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .height(2.dp)
                .width(if (selected && enabled) 24.dp else 0.dp)
                .background(if (selected && enabled) EclipsePalette.Primary else Color.Transparent),
        )
    }
}

@Composable
fun EclipseStatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier
            .border(1.dp, color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
    )
}

@Composable
fun EclipsePanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RectangleShape,
        color = EclipsePalette.Panel,
        contentColor = EclipsePalette.Text,
        border = BorderStroke(2.dp, EclipsePalette.Border),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun EclipseEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EclipsePixelIcon(
            kind = EclipsePixelIconKind.CONTENT,
            modifier = Modifier.size(48.dp),
            tint = EclipsePalette.Muted,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = EclipsePalette.Text,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = EclipsePalette.Muted,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

@Composable
fun EclipseDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = EclipsePalette.Border,
    )
}

@Composable
fun eclipseTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = EclipsePalette.Focus,
    unfocusedBorderColor = EclipsePalette.Border,
    disabledBorderColor = EclipsePalette.Border,
    focusedLabelColor = EclipsePalette.Focus,
    unfocusedLabelColor = EclipsePalette.Muted,
    disabledLabelColor = EclipsePalette.Disabled,
    cursorColor = EclipsePalette.Primary,
    focusedTextColor = EclipsePalette.Text,
    unfocusedTextColor = EclipsePalette.Text,
    disabledTextColor = EclipsePalette.Disabled,
    focusedContainerColor = EclipsePalette.Panel,
    unfocusedContainerColor = EclipsePalette.Panel,
    disabledContainerColor = EclipsePalette.Panel,
    focusedSupportingTextColor = EclipsePalette.Muted,
    unfocusedSupportingTextColor = EclipsePalette.Muted,
)
