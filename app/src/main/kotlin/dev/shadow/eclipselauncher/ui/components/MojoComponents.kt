/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoType

/**
 * The reusable parts of the reference launcher's AppCompat screens, rebuilt as
 * Compose primitives: the fixed account strip, square input fields, flat
 * preference rows, spinners and AlertDialog surfaces.
 */

@Composable
fun MojoScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MojoPalette.Background),
        content = content,
    )
}

@Composable
fun MojoAccountChrome(
    accountName: String,
    accounts: List<String>,
    onSelectAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    onDeleteAccount: (String) -> Unit,
    onChromeAction: () -> Unit,
    actionIcon: EclipsePixelIconKind = EclipsePixelIconKind.HOME,
    actionDescription: String = "Home",
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MojoPalette.StatusBar)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            if (accounts.isEmpty()) onAddAccount() else menuOpen = true
                        },
                    )
                    .padding(start = 12.dp, end = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = accountName,
                    modifier = Modifier.weight(1f),
                    style = MojoType.body.copy(fontSize = 16.sp),
                    color = MojoPalette.PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Reference: `AccountSpinner.setBackgroundColor` replaces the
                // AppCompat arrow background, so the closed account bar shows
                // no dropdown indicator.
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(52.dp)
                    .clickable(role = Role.Button, onClick = onChromeAction),
                contentAlignment = Alignment.Center,
            ) {
                EclipsePixelIcon(
                    kind = actionIcon,
                    modifier = Modifier.size(22.dp),
                    tint = MojoPalette.PrimaryText,
                    description = actionDescription,
                )
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier
                .width(250.dp)
                .background(MojoPalette.Drawer),
            shape = RectangleShape,
            containerColor = MojoPalette.Drawer,
        ) {
            MojoAccountMenu(
                accounts = accounts,
                onAddAccount = {
                    menuOpen = false
                    onAddAccount()
                },
                onSelectAccount = { account ->
                    menuOpen = false
                    onSelectAccount(account)
                },
                onDeleteAccount = { account ->
                    menuOpen = false
                    pendingDelete = account
                },
            )
        }
    }

    pendingDelete?.let { account ->
        MojoAccountDeleteDialog(
            onConfirm = {
                pendingDelete = null
                onDeleteAccount(account)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/**
 * The reference account dropdown body. AppCompat draws the spinner popup over
 * the content with the floating background colour, so the panel is its own
 * 250dp-wide surface rather than part of the account strip.
 */
@Composable
fun MojoAccountMenu(
    accounts: List<String>,
    onAddAccount: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MojoPalette.Drawer),
    ) {
        MojoAccountMenuItem(
            text = "Add account",
            onClick = onAddAccount,
            icon = EclipsePixelIconKind.PLUS,
        )
        accounts.forEach { account ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable(role = Role.Button) { onSelectAccount(account) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EclipsePixelIcon(
                    kind = EclipsePixelIconKind.ACCOUNT,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(30.dp),
                    tint = MojoPalette.PrimaryText,
                )
                Text(
                    text = account,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    style = MojoType.body.copy(fontSize = 16.sp),
                    color = MojoPalette.PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clickable(role = Role.Button) { onDeleteAccount(account) },
                    contentAlignment = Alignment.Center,
                ) {
                    EclipsePixelIcon(
                        kind = EclipsePixelIconKind.TRASH,
                        modifier = Modifier.size(22.dp),
                        tint = MojoPalette.PrimaryText,
                    )
                }
            }
        }
    }
}

/**
 * The home account bar is part of the approved [HomeScreen], so the dropdown
 * is drawn as an overlay instead of changing that file.
 */
@Composable
fun MojoHomeAccountMenu(
    accounts: List<String>,
    onAddAccount: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (openMenu: () -> Unit) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val topOffset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 52.dp

    BackHandler(enabled = menuOpen) { menuOpen = false }

    Box(modifier = modifier.fillMaxSize()) {
        content {
            if (accounts.isEmpty()) onAddAccount() else menuOpen = true
        }

        if (menuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { menuOpen = false },
            )
            Column(
                modifier = Modifier
                    .padding(top = topOffset)
                    .width(250.dp)
                    .background(MojoPalette.Drawer),
            ) {
                MojoAccountMenu(
                    accounts = accounts,
                    onAddAccount = {
                        menuOpen = false
                        onAddAccount()
                    },
                    onSelectAccount = { account ->
                        menuOpen = false
                        onSelectAccount(account)
                    },
                    onDeleteAccount = { account ->
                        menuOpen = false
                        pendingDelete = account
                    },
                )
            }
        }
    }

    pendingDelete?.let { account ->
        MojoAccountDeleteDialog(
            onConfirm = {
                pendingDelete = null
                onDeleteAccount(account)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun MojoAccountMenuItem(
    text: String,
    onClick: () -> Unit,
    icon: EclipsePixelIconKind,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EclipsePixelIcon(
            kind = icon,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(30.dp),
            tint = MojoPalette.PrimaryText,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MojoType.body.copy(fontSize = 16.sp),
            color = MojoPalette.PrimaryText,
        )
    }
}

/** Cancel is the platform positive button; Delete is the neutral button. */
@Composable
fun MojoAccountDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MojoDialog(
        onDismiss = onDismiss,
        neutralLabel = "Delete",
        onNeutralButton = onConfirm,
        confirmLabel = "Cancel",
        onConfirm = onDismiss,
        message = "This account will be removed!",
    )
}

@Composable
fun MojoBackRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: EclipsePixelIconKind = EclipsePixelIconKind.ARROW_LEFT,
) {
    MojoPreferenceRow(
        icon = icon,
        title = label,
        summary = "",
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun MojoPreferenceRow(
    icon: EclipsePixelIconKind,
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(start = 20.dp, end = 20.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EclipsePixelIcon(
            kind = icon,
            modifier = Modifier.size(26.dp),
            tint = if (enabled) MojoPalette.PrimaryText else MojoPalette.SecondaryButton,
        )
        Spacer(Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MojoType.preferenceTitle,
                color = if (enabled) MojoPalette.PrimaryText else MojoPalette.SecondaryText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = summary,
                    style = MojoType.preferenceSummary,
                    color = if (enabled) MojoPalette.SecondaryText else MojoPalette.SecondaryButton,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = trailing,
                style = MojoType.preferenceSummary,
                color = MojoPalette.SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MojoPreferenceSwitch(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: EclipsePixelIconKind? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(start = 20.dp, end = 20.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            EclipsePixelIcon(
                kind = icon,
                modifier = Modifier.size(26.dp),
                tint = MojoPalette.PrimaryText,
            )
            Spacer(Modifier.width(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MojoType.preferenceTitle,
                color = MojoPalette.PrimaryText,
            )
            if (summary.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = summary,
                    style = MojoType.preferenceSummary,
                    color = MojoPalette.SecondaryText,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
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
fun MojoSliderPreference(
    title: String,
    summary: String,
    value: Int,
    valueRange: IntRange,
    step: Int,
    suffix: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: EclipsePixelIconKind? = null,
) {
    val span = (valueRange.last - valueRange.first).coerceAtLeast(1)
    val steps = if (step > 0) (span / step) - 1 else 0
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                EclipsePixelIcon(
                    kind = icon,
                    modifier = Modifier.size(26.dp),
                    tint = MojoPalette.PrimaryText,
                )
                Spacer(Modifier.width(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MojoType.preferenceTitle,
                    color = MojoPalette.PrimaryText,
                )
                Text(
                    text = summary,
                    style = MojoType.preferenceSummary,
                    color = MojoPalette.SecondaryText,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "$value$suffix",
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
            )
        }
        Slider(
            value = value.coerceIn(valueRange.first, valueRange.last).toFloat(),
            onValueChange = { raw ->
                val snapped = ((raw.toInt() - valueRange.first) / step.coerceAtLeast(1)) *
                    step.coerceAtLeast(1) + valueRange.first
                onValueChange(snapped.coerceIn(valueRange.first, valueRange.last))
            },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = steps.coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = MojoPalette.Accent,
                activeTrackColor = MojoPalette.Accent,
                inactiveTrackColor = MojoPalette.Line,
            ),
        )
    }
}

@Composable
fun MojoListPreference(
    title: String,
    summary: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: EclipsePixelIconKind? = null,
) {
    MojoPreferenceRow(
        icon = icon ?: EclipsePixelIconKind.CHEVRON_DOWN,
        title = title,
        summary = summary,
        onClick = onClick,
        modifier = modifier,
        trailing = value,
    )
}

@Composable
fun MojoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
        style = MojoType.body.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MojoPalette.Accent,
        ),
    )
}

@Composable
fun MojoTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 18.dp),
            style = MojoType.title,
            color = MojoPalette.PrimaryText,
            textAlign = TextAlign.Center,
        )
        MojoDivider()
    }
}

@Composable
fun MojoDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MojoPalette.Overlay),
    )
}

/**
 * `MineButton`: the reference 9-patch is a cyan top highlight over a dark teal
 * body; pressed and disabled states use the dark variant. Standard platform
 * buttons (`primary = false`) use `colorButtonNormal` from the dark AppCompat
 * theme.
 */
@Composable
fun MojoMineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
    height: Dp = 48.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // `mine_button_background` swaps to the dark 9-patch while pressed or
    // disabled; the standard AppCompat button background (`button_flat`) is a
    // static `colorButtonNormal` shape with no pressed or disabled tint.
    val background: Brush = when {
        primary && (!enabled || pressed) -> Brush.verticalGradient(
            listOf(
                Color(0xFF2A2A2A),
                Color(0xFF0A0A0A),
                Color(0xFF020202),
            ),
        )

        primary -> Brush.verticalGradient(
            0f to Color(0xFF91DFFB),
            0.07f to Color(0xFF47E6FF),
            0.17f to Color(0xFF1F7387),
            0.86f to Color(0xFF1F7387),
            1f to Color(0xFF0A3945),
        )

        else -> SolidColor(Color(0xFF5A595B))
    }
    val foreground = if (enabled) MojoPalette.PrimaryText else MojoPalette.SecondaryText
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MojoType.button,
            color = foreground,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MojoButtonRow(
    startLabel: String,
    onStart: () -> Unit,
    endLabel: String,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
    startEnabled: Boolean = true,
    endEnabled: Boolean = true,
    horizontalPadding: Dp = 16.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MojoMineButton(
            text = startLabel,
            onClick = onStart,
            modifier = Modifier.weight(1f),
            enabled = startEnabled,
        )
        MojoMineButton(
            text = endLabel,
            onClick = onEnd,
            modifier = Modifier.weight(1f),
            enabled = endEnabled,
            primary = true,
        )
    }
}

@Composable
fun MojoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    numeric: Boolean = false,
    decimal: Boolean = false,
    centerText: Boolean = false,
    errorText: String? = null,
) {
    MojoInput(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        singleLine = singleLine,
        numeric = numeric,
        decimal = decimal,
        centerText = centerText,
        errorText = errorText,
        backgroundColor = Color.Transparent,
        drawBorder = true,
        textSize = 13.sp,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
    )
}

/** `MineEditText`: a 37dp `#131313` field with 5dp padding and 16sp text. */
@Composable
fun MojoMineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(37.dp)
            .background(Color(0xFF131313)),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            textStyle = MojoType.body.copy(
                fontSize = 16.sp,
                color = MojoPalette.PrimaryText,
            ),
            singleLine = true,
            cursorBrush = SolidColor(MojoPalette.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
    }
}

@Composable
private fun MojoInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String,
    singleLine: Boolean,
    numeric: Boolean,
    decimal: Boolean,
    centerText: Boolean,
    errorText: String?,
    backgroundColor: Color,
    drawBorder: Boolean,
    textSize: TextUnit,
    contentPadding: PaddingValues,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        errorText != null -> MojoPalette.Error
        focused -> MojoPalette.Accent
        else -> MojoPalette.Line
    }
    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .then(
                    if (drawBorder) {
                        Modifier.border(2.dp, borderColor, RectangleShape)
                    } else {
                        Modifier
                    },
                )
                .padding(contentPadding),
            textStyle = MojoType.body.copy(
                fontSize = textSize,
                color = MojoPalette.PrimaryText,
                textAlign = if (centerText) TextAlign.Center else TextAlign.Start,
            ),
            singleLine = singleLine,
            cursorBrush = SolidColor(MojoPalette.Accent),
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(
                keyboardType = when {
                    decimal -> KeyboardType.Decimal
                    numeric -> KeyboardType.Number
                    else -> KeyboardType.Text
                },
            ),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = if (centerText) {
                        Alignment.Center
                    } else {
                        Alignment.CenterStart
                    },
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MojoType.body.copy(fontSize = textSize),
                            color = MojoPalette.SecondaryText,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (errorText != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = errorText,
                style = MojoType.secondary,
                color = MojoPalette.Error,
            )
        }
    }
}

@Composable
fun MojoDialog(
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String? = null,
    onDismissButton: (() -> Unit)? = null,
    neutralLabel: String? = null,
    onNeutralButton: (() -> Unit)? = null,
    title: String? = null,
    message: String? = null,
    confirmEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = MojoPalette.StatusBar,
        titleContentColor = MojoPalette.PrimaryText,
        textContentColor = MojoPalette.PrimaryText,
        title = title?.let {
            {
                Text(
                    text = it,
                    style = MojoType.dialogTitle,
                )
            }
        },
        text = if (message != null || content != null) {
            {
                Column {
                    if (message != null) {
                        Text(
                            text = message,
                            style = MojoType.body,
                            color = MojoPalette.PrimaryText,
                        )
                    }
                    content?.invoke(this)
                }
            }
        } else {
            null
        },
        confirmButton = {
            TextButton(
                enabled = confirmEnabled,
                onClick = onConfirm,
                modifier = Modifier.defaultMinSize(
                    minWidth = 64.dp,
                    minHeight = 48.dp,
                ),
            ) {
                Text(
                    text = confirmLabel.uppercase(),
                    style = MojoType.body,
                    color = if (confirmEnabled) MojoPalette.Accent else MojoPalette.SecondaryText,
                )
            }
        },
        dismissButton = if (dismissLabel != null || neutralLabel != null) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (neutralLabel != null) {
                        TextButton(
                            onClick = { onNeutralButton?.invoke() ?: onDismiss() },
                            modifier = Modifier.defaultMinSize(
                                minWidth = 64.dp,
                                minHeight = 48.dp,
                            ),
                        ) {
                            Text(
                                text = neutralLabel.uppercase(),
                                style = MojoType.body,
                                color = MojoPalette.PrimaryText,
                            )
                        }
                    }
                    if (dismissLabel != null) {
                        TextButton(
                            onClick = { onDismissButton?.invoke() ?: onDismiss() },
                            modifier = Modifier.defaultMinSize(
                                minWidth = 64.dp,
                                minHeight = 48.dp,
                            ),
                        ) {
                            Text(
                                text = dismissLabel.uppercase(),
                                style = MojoType.body,
                                color = MojoPalette.PrimaryText,
                            )
                        }
                    }
                }
            }
        } else {
            null
        },
    )
}

@Composable
fun MojoConfirmDialog(
    title: String?,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MojoDialog(
        onDismiss = onDismiss,
        dismissLabel = dismissLabel,
        onDismissButton = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        title = title,
        message = message,
    )
}

@Composable
fun MojoSingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
) {
    MojoDialog(
        onDismiss = onDismiss,
        confirmLabel = dismissLabel,
        onConfirm = onDismiss,
        title = title,
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
            ) {
                itemsIndexed(options) { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .background(
                                if (index == selectedIndex) {
                                    MojoPalette.Overlay
                                } else {
                                    Color.Transparent
                                },
                            )
                            .clickable {
                                onSelect(index)
                            }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option,
                            style = MojoType.body,
                            color = MojoPalette.PrimaryText,
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun MojoProgressBar(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp),
        color = MojoPalette.Accent,
        trackColor = MojoPalette.SecondaryButton,
    )
}

@Composable
fun MojoInlineError(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "Retry",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            style = MojoType.body.copy(fontWeight = FontWeight.Bold),
            color = MojoPalette.Error,
        )
        if (onRetry != null) {
            // The width is pinned: an unconstrained button would fill the Row and
            // squeeze the weighted message out. `Base.Widget.AppCompat.Button`
            // measures 88dp wide by 48dp high.
            MojoMineButton(
                text = retryLabel,
                onClick = onRetry,
                modifier = Modifier.width(88.dp),
                primary = false,
                height = 48.dp,
            )
        }
    }
}

@Composable
fun MojoEmptyState(
    title: String?,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MojoType.title.copy(fontSize = 17.sp),
                color = MojoPalette.PrimaryText,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = body,
            style = MojoType.body,
            color = MojoPalette.SecondaryText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun MojoListRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    secondary: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(if (selected) MojoPalette.Overlay else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MojoType.secondary,
                    color = MojoPalette.SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun MojoSearchRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onFilter: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MojoPalette.Overlay)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            MojoTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
            )
        }
        if (onFilter != null) {
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(role = Role.Button, onClick = onFilter),
                contentAlignment = Alignment.Center,
            ) {
                EclipsePixelIcon(
                    kind = EclipsePixelIconKind.FILTER,
                    modifier = Modifier.size(22.dp),
                    tint = MojoPalette.PrimaryText,
                )
            }
        }
    }
}

@Composable
fun MojoCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: EclipsePixelIconKind = EclipsePixelIconKind.CONTENT,
    trailing: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MojoPalette.StatusBar)
            .border(BorderStroke(1.dp, MojoPalette.Overlay))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MojoPalette.Input),
            contentAlignment = Alignment.Center,
        ) {
            EclipsePixelIcon(
                kind = icon,
                modifier = Modifier.size(34.dp),
                tint = MojoPalette.Accent,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MojoType.preferenceTitle,
                    color = MojoPalette.PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = trailing,
                        style = MojoType.secondary,
                        color = MojoPalette.Accent,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MojoType.bodySmall,
                color = MojoPalette.SecondaryText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MojoErrorScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
    backLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MojoPalette.Background),
    ) {
        MojoBackRow(label = backLabel, onClick = onBack)
        MojoDivider()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MojoType.title.copy(color = MojoPalette.Error),
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                style = MojoType.body,
                color = MojoPalette.PrimaryText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MojoLoadingBar(label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MojoType.bodySmall,
            color = MojoPalette.SecondaryText,
        )
        MojoProgressBar()
    }
}

@Composable
fun MojoLoggerHeader(
    autoscroll: Boolean,
    output: Boolean,
    onAutoscroll: () -> Unit,
    onOutput: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(MojoPalette.LoggerHeader)
            .padding(horizontal = 25.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Log output:",
            modifier = Modifier.weight(1f),
            style = MojoType.body.copy(fontSize = 16.sp),
            color = MojoPalette.PrimaryText,
        )
        TextButton(onClick = onAutoscroll) {
            Text(
                text = if (autoscroll) "Autoscroll\nON" else "Autoscroll\nOFF",
                style = MojoType.bodySmall,
                color = MojoPalette.PrimaryText,
                textAlign = TextAlign.Center,
            )
        }
        TextButton(onClick = onOutput) {
            Text(
                text = if (output) "Output\nON" else "Output\nOFF",
                style = MojoType.bodySmall,
                color = MojoPalette.PrimaryText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun MojoLabelledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    numeric: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MojoType.body,
            color = MojoPalette.PrimaryText,
        )
        Spacer(Modifier.height(2.dp))
        MojoTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            numeric = numeric,
        )
    }
}

/**
 * Height of a `background_line` row: 11dp top padding, the body line and 11dp
 * bottom padding, matching `padding_input_top`/`padding_input_bottom`.
 */
@Composable
fun MojoFieldHeight(): Dp = with(LocalDensity.current) {
    MojoType.body.lineHeight.toDp() + 22.dp
}

@Composable
fun MojoSelectField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    buttonLabel: String = "Select",
) {
    val fieldHeight = MojoFieldHeight()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MojoType.body,
            color = MojoPalette.PrimaryText,
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(fieldHeight)
                    .border(2.dp, MojoPalette.Line)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = value.ifBlank { hint },
                    style = MojoType.body,
                    color = if (value.isBlank()) {
                        MojoPalette.SecondaryText
                    } else {
                        MojoPalette.PrimaryText
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(4.dp))
            // Reference: `wrap_content` (88dp minimum) stretched to the field's
            // height by the surrounding constraints.
            MojoMineButton(
                text = buttonLabel,
                onClick = onClick,
                modifier = Modifier
                    .width(88.dp)
                    .height(fieldHeight),
                primary = false,
                height = fieldHeight,
            )
        }
    }
}

@Composable
fun MojoInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MojoType.body,
            color = MojoPalette.SecondaryText,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MojoType.body,
            color = MojoPalette.PrimaryText,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MojoFlatButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = 64.dp, minHeight = 48.dp),
        shape = RectangleShape,
    ) {
        Text(
            text = text.uppercase(),
            style = MojoType.button,
            color = MojoPalette.PrimaryText,
        )
    }
}

/**
 * `Tools.showError`. The rolled-up message is shown first, "Show more" swaps in
 * the full stack trace, "Copy" puts the trace on the clipboard, and OK closes
 * the dialog. When the reference would finish its activity after OK or Copy,
 * [onAfterDismiss] runs at the same point.
 */
@Composable
fun MojoErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    details: String? = null,
    dismissible: Boolean = true,
    onAfterDismiss: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val shown = if (expanded && details != null) details else message
    val clipboardText = details ?: message

    AlertDialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        shape = RectangleShape,
        containerColor = MojoPalette.StatusBar,
        titleContentColor = MojoPalette.PrimaryText,
        textContentColor = MojoPalette.PrimaryText,
        title = {
            Text(text = "Error", style = MojoType.dialogTitle)
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = shown,
                    style = MojoType.body,
                    color = MojoPalette.PrimaryText,
                )
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("error", clipboardText))
                        onDismiss()
                        onAfterDismiss?.invoke()
                    },
                    modifier = Modifier.defaultMinSize(
                        minWidth = 64.dp,
                        minHeight = 48.dp,
                    ),
                ) {
                    Text(
                        text = "Copy".uppercase(),
                        style = MojoType.body,
                        color = MojoPalette.PrimaryText,
                    )
                }
                if (details != null) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.defaultMinSize(
                            minWidth = 64.dp,
                            minHeight = 48.dp,
                        ),
                    ) {
                        Text(
                            text = (if (expanded) "Show less" else "Show more").uppercase(),
                            style = MojoType.body,
                            color = MojoPalette.PrimaryText,
                        )
                    }
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        onAfterDismiss?.invoke()
                    },
                    modifier = Modifier.defaultMinSize(
                        minWidth = 64.dp,
                        minHeight = 48.dp,
                    ),
                ) {
                    Text(
                        text = "OK",
                        style = MojoType.body,
                        color = MojoPalette.Accent,
                    )
                }
            }
        },
    )
}
