/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The dress button's dialog: a live 3D model of the account's skin the user
 * can turn with a drag and inspect closer with a pinch, over icon+text
 * actions — pick a skin, clear the skin. Emote picker for 8 poses.
 * Microsoft accounts keep the model and full-screen control but hide the
 * pick/clear actions.
 */
package me.shadow.eclipselauncher.ui.components

import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.align
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTransformGestures
import kotlinx.coroutines.launch
import java.io.FileInputStream
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.data.Account
import me.shadow.eclipselauncher.data.ImportKind
import me.shadow.eclipselauncher.data.MediaStorage
import me.shadow.eclipselauncher.data.newId
import me.shadow.eclipselauncher.ui.character.EmotePoseIcon
import me.shadow.eclipselauncher.ui.character.Minecraft3DPlayer
import me.shadow.eclipselauncher.ui.character.Player3DAnimation
import me.shadow.eclipselauncher.ui.character.PlayerLookTarget
import me.shadow.eclipselauncher.ui.character.SkinManager
import androidx.compose.material3.BasicAlertDialog
import me.shadow.eclipselauncher.ui.theme.EclipseRadius
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween

/**
 * Skin management for one account: the draggable/zoomable 3D model
 * plus pick/clear actions for skin. Emote picker for 8 poses.
 * For Microsoft accounts, the model is shown but pick/clear actions are hidden.
 */
@Composable
fun SkinCustomizationDialog(
    account: Account,
    media: MediaStorage,
    onUpdate: (Account) -> Unit,
    onDismiss: () -> Unit,
    canCustomize: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val skinError = stringResource(R.string.error_skin_size)
    val capeError = stringResource(R.string.error_cape_size)
    // Saveable so rotating while full screen keeps the full-screen dialog.
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    val pickSkin = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val stored = media.importImage(newId() + "_skin.png", uri, ImportKind.SKIN)
                if (stored != null) {
                    onUpdate(account.copy(skinFileName = stored))
                } else {
                    Toast.makeText(context, skinError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val pickCape = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val stored = media.importImage(newId() + "_cape.png", uri, ImportKind.CAPE)
                if (stored != null) {
                    onUpdate(account.copy(capeFileName = stored))
                } else {
                    Toast.makeText(context, capeError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Animation state
    val animationTime by animateFloatAsState(
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skinDialogAnimTime",
    )

    // Drag/pinch state
    var yaw by remember { mutableStateOf(0f) }
    var pitch by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }

    // Emote picker state
    var showEmotes by remember { mutableStateOf(false) }
    var selectedEmote by remember { mutableStateOf(Player3DAnimation.IDLE) }

    // Look target for head tracking
    val lookTarget = remember { PlayerLookTarget() }

    if (isFullscreen) {
        // A real window, not a resized card: it spans the whole screen behind
        // the system bars. Back reaches onDismissRequest, which exits full
        // screen instead of closing the whole skin dialog.
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            // Set dialog window to fill entire screen (LaunchedEffect runs after composition
            // when the Dialog window exists). Traverse context wrappers to find a window.
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                var ctx: android.content.Context? = context
                while (ctx != null) {
                    val window = when {
                        (ctx as? android.app.Activity)?.window != null -> (ctx as android.app.Activity).window
                        (ctx as? android.app.Dialog)?.window != null -> (ctx as android.app.Dialog).window
                        else -> null
                    }
                    if (window != null) {
                        window.setLayout(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT
                        )
                        break
                    }
                    ctx = if (ctx is android.content.ContextWrapper) ctx.baseContext else null
                }
            }
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                SkinModelStage(
                    skinFileName = account.skinFileName,
                    skinVariant = account.skinVariant,
                    media = media,
                    isFullscreen = true,
                    onFullscreenToggle = { isFullscreen = false },
                    onEmoteToggle = { showEmotes = !showEmotes },
                    showEmotes = showEmotes,
                    selectedEmote = selectedEmote,
                    onEmoteSelect = { selectedEmote = it },
                    yaw = yaw, pitch = pitch, zoom = zoom,
                    onDrag = { dy, dz -> yaw += dy; pitch += dz },
                    onZoom = { z -> zoom = z.coerceIn(0.65f, 2.6f) },
                    animationTime = animationTime,
                    lookTarget = lookTarget,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    } else {
        BasicAlertDialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(EclipseRadius.dialog),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.skin_section),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // The model stage: drag to turn, pinch to zoom.
                    SkinModelStage(
                        skinFileName = account.skinFileName,
                        skinVariant = account.skinVariant,
                        media = media,
                        isFullscreen = false,
                        onFullscreenToggle = { isFullscreen = true },
                        onEmoteToggle = { showEmotes = !showEmotes },
                        showEmotes = showEmotes,
                        selectedEmote = selectedEmote,
                        onEmoteSelect = { selectedEmote = it },
                        yaw = yaw, pitch = pitch, zoom = zoom,
                        onDrag = { dy, dz -> yaw += dy; pitch += dz },
                        onZoom = { z -> zoom = z.coerceIn(0.65f, 2.6f) },
                        animationTime = animationTime,
                        lookTarget = lookTarget,
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Emote toggle button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { showEmotes = !showEmotes },
                            modifier = Modifier.weight(1f).height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showEmotes) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = if (showEmotes) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            shape = RoundedCornerShape(EclipseRadius.tile),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEmotions,
                                    contentDescription = stringResource(R.string.emotes_section),
                                    tint = if (showEmotes) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp),
                                )
                                Spacer(modifier = Modifier.size(7.dp))
                                Text(
                                    text = stringResource(if (showEmotes) R.string.emotes_hide else R.string.emotes_show),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    // Pick and clear skin
                    if (canCustomize) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DialogActionButton(
                                icon = Icons.Filled.Checkroom,
                                text = stringResource(R.string.action_pick_skin),
                                modifier = Modifier.weight(1f),
                                primary = true,
                                onClick = { pickSkin.launch(arrayOf("image/png")) },
                            )
                            DialogActionButton(
                                icon = Icons.Filled.Delete,
                                text = stringResource(R.string.action_clear_skin),
                                modifier = Modifier.weight(1f),
                                enabled = account.skinFileName != null,
                                destructive = true,
                                onClick = { onUpdate(account.copy(skinFileName = null)) },
                            )
                        }
                    }

                    // Emote picker grid
                    if (showEmotes) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.emotes_choose),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            EmotePickerGrid(
                                selectedEmote = selectedEmote,
                                onEmoteSelect = { selectedEmote = it },
                                animationTime = animationTime,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The model stage shared by the normal card and the full-screen window.
 */
@Composable
fun SkinModelStage(
    skinFileName: String?,
    skinVariant: String? = null,
    media: MediaStorage,
    isFullscreen: Boolean,
    onFullscreenToggle: () -> Unit,
    onEmoteToggle: () -> Unit,
    showEmotes: Boolean,
    selectedEmote: Player3DAnimation,
    onEmoteSelect: (Player3DAnimation) -> Unit,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    onDrag: (Float, Float) -> Unit,
    onZoom: (Float) -> Unit,
    animationTime: Float,
    lookTarget: PlayerLookTarget,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.then(
            if (isFullscreen) {
                Modifier
            } else {
                Modifier
                    .clip(RoundedCornerShape(EclipseRadius.panel))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            },
        ),
        contentAlignment = Alignment.Center,
    ) {
        val isSlim = (skinVariant == "SLIM")
        val lookTarget = remember { PlayerLookTarget() }

        val skinBitmap = loadSkinBitmap(media, skinFileName)

        Minecraft3DPlayer(
            bitmap = skinBitmap,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        // Horizontal: drag right = rotate model right (yaw increases)
                        onDrag(pan.x * 0.32f, pan.y * 0.32f)
                        // Pinch zoom
                        onZoom((zoom * zoomChange).coerceIn(0.65f, 2.6f))
                    }
                },
            slim = isSlim,
            animation = selectedEmote,
            lookTarget = lookTarget,
            capeBitmap = null, // Cape disabled for now
            onDraw = {},
        )

        // Full-screen toggle at top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .then(
                    if (isFullscreen) {
                        Modifier
                    } else {
                        Modifier
                    },
                ),
        ) {
            androidx.compose.material3.FilledTonalIconButton(
                onClick = onFullscreenToggle,
                modifier = Modifier.size(48.dp).testTag("fullscreenButton"),
                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(
                        if (isFullscreen) {
                            R.drawable.ic_fullscreen_exit_24
                        } else {
                            R.drawable.ic_fullscreen_24
                        },
                    ),
                    contentDescription = stringResource(
                        if (isFullscreen) {
                            R.string.cd_exit_fullscreen
                        } else {
                            R.string.cd_enter_fullscreen
                        },
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }

    // Emote toggle button
    if (!isFullscreen) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        ) {
            androidx.compose.material3.FilledTonalIconButton(
                onClick = onEmoteToggle,
                modifier = Modifier.size(48.dp).testTag("emoteButton"),
                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (showEmotes) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (showEmotes) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEmotions,
                    contentDescription = stringResource(if (showEmotes) R.string.emotes_hide else R.string.emotes_show),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** Loads the skin bitmap from MediaStorage or returns built-in Steve. */
@Composable
fun loadSkinBitmap(media: MediaStorage, skinFileName: String?): android.graphics.Bitmap {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (skinFileName == null) {
        // Return built-in Steve skin
        val inputStream = context.assets.open("skins/steve.png")
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return SkinManager.ensureModernSkin(bitmap)
    }
    // Try to load from MediaStorage
    val file = media.resolve(skinFileName)
    if (file != null && file.exists()) {
        val inputStream = FileInputStream(file)
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return SkinManager.ensureModernSkin(bitmap)
    }
    // Fallback to built-in Steve
    val inputStream = context.assets.open("skins/steve.png")
    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
    inputStream.close()
    return SkinManager.ensureModernSkin(bitmap)
}

/** Grid of 8 emote pose icons. */
@Composable
fun EmotePickerGrid(
    selectedEmote: Player3DAnimation,
    onEmoteSelect: (Player3DAnimation) -> Unit,
    animationTime: Float,
) {
    val emotes = Player3DAnimation.values()

    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
    ) {
        items(emotes.size) { index ->
            val emote = emotes[index]
            EmotePoseIcon(
                animation = emote,
                timeSeconds = animationTime,
                modifier = Modifier.size(56.dp),
                selected = (selectedEmote == emote),
                onClick = { onEmoteSelect(emote) },
            )
        }
    }
}

/** One icon+text action inside the skin dialog. */
@Composable
private fun DialogActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val container = when {
        destructive -> MaterialTheme.colorScheme.errorContainer
        primary -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = when {
        destructive -> MaterialTheme.colorScheme.onErrorContainer
        primary -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
        shape = RoundedCornerShape(EclipseRadius.tile),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.size(7.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}