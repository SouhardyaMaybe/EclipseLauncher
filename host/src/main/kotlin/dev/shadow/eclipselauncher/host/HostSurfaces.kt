/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.host

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIcon
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.MojoPalette
import dev.shadow.eclipselauncher.designsystem.theme.MojoTheme
import dev.shadow.eclipselauncher.designsystem.theme.MojoType

/**
 * Visual-only surfaces from the reference launcher's JVM window, game process
 * and process-error activities. They are not launched by the launcher: this
 * build has no JVM, game runtime or crash handler to provide their real data.
 * Keeping the screens here means a later backend can attach to them without
 * rebuilding their chrome or copy.
 */

@Composable
fun AwtWindowScreen(onExit: () -> Unit) {
    var forceCloseOpen by remember { mutableStateOf(false) }
    var loggerOpen by remember { mutableStateOf(false) }
    var mouseOn by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mouseOnText = stringResource(R.string.host_awt_mouse_on)
    val mouseOffText = stringResource(R.string.host_awt_mouse_off)

    BackHandler { forceCloseOpen = true }

    MojoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AwtControlButton(stringResource(R.string.host_awt_keyboard)) {}
                AwtControlButton(stringResource(R.string.host_awt_copy)) {}
                AwtControlButton(stringResource(R.string.host_awt_paste)) {}
            }

            Column(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AwtControlButton(stringResource(R.string.host_awt_force_close)) {
                    forceCloseOpen = true
                }
                AwtControlButton(stringResource(R.string.host_awt_log_output)) {
                    loggerOpen = true
                }
                AwtControlButton(stringResource(R.string.host_awt_mouse)) {
                    mouseOn = !mouseOn
                    Toast.makeText(
                        context,
                        if (mouseOn) mouseOnText else mouseOffText,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }

            // Reference: `installmod_mouse_sec` sits flush with the bottom edge and
            // the 2dp margin belongs to `installmod_mouse_pri` above it.
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                AwtControlButton(stringResource(R.string.host_awt_primary)) {}
                AwtControlButton(stringResource(R.string.host_awt_secondary)) {}
            }

            Column(modifier = Modifier.align(Alignment.BottomEnd)) {
                Row {
                    AwtControlButton(stringResource(R.string.host_awt_up)) {}
                    AwtControlButton(stringResource(R.string.host_awt_right)) {}
                }
                Row {
                    AwtControlButton(stringResource(R.string.host_awt_down)) {}
                    AwtControlButton(stringResource(R.string.host_awt_left)) {}
                }
            }

            if (loggerOpen) {
                HostLoggerOverlay(onClose = { loggerOpen = false })
            }

            if (forceCloseOpen) {
                HostDialog(
                    message = stringResource(R.string.host_force_close_confirm),
                    confirmLabel = stringResource(R.string.host_ok),
                    onConfirm = {
                        forceCloseOpen = false
                        onExit()
                    },
                    dismissLabel = stringResource(R.string.host_cancel),
                    onDismiss = { forceCloseOpen = false },
                )
            }
        }
    }
}

@Composable
fun GameHudScreen(
    loading: Boolean = false,
    onExit: () -> Unit,
) {
    var drawerOpen by remember { mutableStateOf(false) }
    var loggerOpen by remember { mutableStateOf(false) }
    var forceCloseOpen by remember { mutableStateOf(false) }

    BackHandler { forceCloseOpen = true }

    MojoTheme {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            DrawerPull(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = maxWidth * 0.25f)
                    .clickable(role = Role.Button) { drawerOpen = true },
            )

            if (loading) {
                GameLoadingOverlay()
            }

            if (loggerOpen) {
                HostLoggerOverlay(onClose = { loggerOpen = false })
            }

            if (drawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(role = Role.Button) { drawerOpen = false },
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(200.dp)
                        .background(MojoPalette.Drawer),
                ) {
                    HudDrawerItem(stringResource(R.string.host_hud_force_close)) {
                        drawerOpen = false
                        forceCloseOpen = true
                    }
                    HudDrawerItem(stringResource(R.string.host_hud_log_output)) {
                        drawerOpen = false
                        loggerOpen = true
                    }
                    HudDrawerItem(stringResource(R.string.host_hud_send_keycode)) {
                        drawerOpen = false
                    }
                    HudDrawerItem(stringResource(R.string.host_hud_quick_settings)) {
                        drawerOpen = false
                    }
                    HudDrawerItem(stringResource(R.string.host_hud_custom_controls)) {
                        drawerOpen = false
                    }
                }
            }

            if (forceCloseOpen) {
                HostDialog(
                    message = stringResource(R.string.host_force_close_confirm),
                    confirmLabel = stringResource(R.string.host_ok),
                    onConfirm = {
                        forceCloseOpen = false
                        onExit()
                    },
                    dismissLabel = stringResource(R.string.host_cancel),
                    onDismiss = { forceCloseOpen = false },
                )
            }
        }
    }
}

@Composable
fun GameLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBB000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MojoPalette.Accent,
                strokeWidth = 2.dp,
            )
            Text(
                text = stringResource(R.string.host_loading_title),
                color = Color.White,
                fontSize = 16.sp,
            )
            Text(
                text = stringResource(R.string.host_loading_warning),
                color = Color.DarkGray,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun MissingStorageScreen() {
    MojoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StorageAlertGlyph(modifier = Modifier.size(128.dp))
            Text(
                text = stringResource(R.string.host_storage_required),
                modifier = Modifier.padding(start = 8.dp, top = 10.dp, end = 8.dp),
                color = MojoPalette.PrimaryText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ExitScreen(
    code: Int,
    isSignal: Boolean,
    onFinished: () -> Unit,
) {
    MojoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background),
        ) {
            HostDialog(
                message = if (isSignal) {
                    stringResource(R.string.host_exit_aborted)
                } else {
                    stringResource(R.string.host_exit_code, code)
                },
                confirmLabel = stringResource(R.string.host_share_log),
                onConfirm = onFinished,
                onDismiss = onFinished,
            )
        }
    }
}

@Composable
fun FatalErrorScreen(
    storageAllow: Boolean,
    savePath: String?,
    stackTrace: String,
    onFinished: () -> Unit,
    onRestart: () -> Unit,
) {
    val context = LocalContext.current
    val header = if (storageAllow) {
        stringResource(R.string.host_fatal_saved, savePath.orEmpty())
    } else {
        stringResource(R.string.host_fatal_no_storage)
    }

    MojoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MojoPalette.Background),
        ) {
            AlertDialog(
                onDismissRequest = {},
                shape = RectangleShape,
                containerColor = MojoPalette.StatusBar,
                titleContentColor = MojoPalette.PrimaryText,
                textContentColor = MojoPalette.PrimaryText,
                title = {
                    Text(
                        text = stringResource(R.string.host_fatal_title),
                        style = MojoType.dialogTitle,
                    )
                },
                text = {
                    Text(
                        text = "$header\n\n$stackTrace",
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MojoType.body.copy(fontFamily = FontFamily.Monospace),
                    )
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.End) {
                        DialogTextButton(stringResource(R.string.host_copy)) {
                            copyToClipboard(context, stackTrace)
                            onFinished()
                        }
                        DialogTextButton(stringResource(R.string.host_restart)) {
                            onRestart()
                        }
                        DialogTextButton(stringResource(R.string.host_ok)) {
                            onFinished()
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun HostDialog(
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String? = null,
    title: String? = null,
) {
    val titleContent: (@Composable () -> Unit)? = title?.let { value ->
        { Text(text = value, style = MojoType.dialogTitle) }
    }
    val dismissButton: (@Composable () -> Unit)? = if (dismissLabel != null) {
        { DialogTextButton(dismissLabel, onDismiss) }
    } else {
        null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = MojoPalette.StatusBar,
        titleContentColor = MojoPalette.PrimaryText,
        textContentColor = MojoPalette.PrimaryText,
        title = titleContent,
        text = {
            Text(
                text = message,
                style = MojoType.body,
            )
        },
        confirmButton = {
            DialogTextButton(confirmLabel, onConfirm)
        },
        dismissButton = dismissButton,
    )
}

@Composable
private fun DialogTextButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        // `Base.Widget.AppCompat.Button.ButtonBar.AlertDialog`: 64dp wide, 48dp tall.
        modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 48.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = MojoPalette.Accent,
            style = MojoType.body,
        )
    }
}

@Composable
private fun AwtControlButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 88.dp, minHeight = 48.dp)
            .background(if (pressed) MojoPalette.ControlPressed else MojoPalette.Control)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = Color.White,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun HudDrawerItem(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = MojoPalette.PrimaryText,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun DrawerPull(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 20.dp)
            .alpha(0.33f),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padding = 8.dp.toPx()
            drawArc(
                color = Color.Black,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(padding, -size.height + padding),
                size = Size(size.width - padding * 2f, (size.height - padding) * 2f),
            )
        }
        EclipsePixelIcon(
            kind = EclipsePixelIconKind.SETTINGS,
            modifier = Modifier.size(12.dp),
            tint = Color.White,
        )
    }
}

@Composable
private fun HostLoggerOverlay(onClose: () -> Unit) {
    var outputOn by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(MojoPalette.LoggerHeader),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.host_log_title),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 25.dp),
                color = Color.White,
                fontSize = 16.sp,
            )
            LoggerToggle(
                on = autoScroll,
                offLabel = stringResource(R.string.host_log_autoscroll_off),
                onLabel = stringResource(R.string.host_log_autoscroll_on),
                onClick = { autoScroll = !autoScroll },
            )
            LoggerToggle(
                on = outputOn,
                offLabel = stringResource(R.string.host_log_output_off),
                onLabel = stringResource(R.string.host_log_output_on),
                onClick = { outputOn = !outputOn },
            )
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                EclipsePixelIcon(
                    kind = EclipsePixelIconKind.TRASH,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White,
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
        )
    }
}

@Composable
private fun LoggerToggle(
    on: Boolean,
    offLabel: String,
    onLabel: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 10.dp)
            .background(if (on) MojoPalette.ControlPressed else MojoPalette.Control)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = (if (on) onLabel else offLabel).uppercase(),
            color = Color.White,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StorageAlertGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.width * 0.05f
        val left = size.width * 0.12f
        val top = size.height * 0.3f
        val width = size.width * 0.76f
        val height = size.height * 0.4f

        drawRoundRect(
            color = MojoPalette.PrimaryText,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = CornerRadius(size.width * 0.06f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = MojoPalette.PrimaryText,
            start = Offset(left + size.width * 0.06f, top + height * 0.7f),
            end = Offset(left + width - size.width * 0.06f, top + height * 0.7f),
            strokeWidth = stroke,
        )
        drawCircle(
            color = MojoPalette.PrimaryText,
            radius = size.width * 0.035f,
            center = Offset(left + width - size.width * 0.08f, top + height * 0.35f),
        )

        val warning = Path().apply {
            moveTo(size.width * 0.78f, size.height * 0.08f)
            lineTo(size.width * 0.95f, size.height * 0.38f)
            lineTo(size.width * 0.61f, size.height * 0.38f)
            close()
        }
        drawPath(warning, MojoPalette.Error)
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.78f, size.height * 0.15f),
            end = Offset(size.width * 0.78f, size.height * 0.27f),
            strokeWidth = stroke,
        )
        drawCircle(
            color = Color.White,
            radius = size.width * 0.02f,
            center = Offset(size.width * 0.78f, size.height * 0.33f),
        )
    }
}

private fun copyToClipboard(context: android.content.Context, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
        as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("error", text))
}
