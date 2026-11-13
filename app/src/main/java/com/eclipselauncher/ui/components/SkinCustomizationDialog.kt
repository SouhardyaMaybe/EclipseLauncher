/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The dress button's dialog: a live 3D model of the account's skin the user
 * can turn with a drag and inspect closer with a pinch, over four icon+text
 * actions — pick a skin, pick a cape, clear the skin, clear the cape. Picks
 * import through MediaStorage (validated on the IO dispatcher) and land on
 * the account via onUpdate; clears just null the field so the default Steve
 * shows again. Presented in the standard Material 3 basic dialog.
 */
package com.eclipselauncher.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.eclipselauncher.R
import com.eclipselauncher.data.Account
import com.eclipselauncher.data.ImportKind
import com.eclipselauncher.data.MediaStorage
import com.eclipselauncher.data.newId
import androidx.compose.material3.BasicAlertDialog
import com.eclipselauncher.ui.character.SkinModelView
import com.eclipselauncher.ui.theme.EclipseRadius

/**
 * Skin and cape management for one account: the draggable/zoomable 3D model
 * plus pick/clear actions for both media slots.
 */
@Composable
fun SkinCustomizationDialog(
    account: Account,
    media: MediaStorage,
    onUpdate: (Account) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val skinError = stringResource(R.string.error_skin_size)
    val mediaError = stringResource(R.string.error_media_import)

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
                    Toast.makeText(context, mediaError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(EclipseRadius.panel))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    SkinModelView(
                        skinFileName = account.skinFileName,
                        media = media,
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pick and clear, skin over cape: icon + text on every action.
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
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DialogActionButton(
                        icon = Icons.Filled.Checkroom,
                        text = stringResource(R.string.action_pick_cape),
                        modifier = Modifier.weight(1f),
                        primary = true,
                        onClick = { pickCape.launch(arrayOf("image/png")) },
                    )
                    DialogActionButton(
                        icon = Icons.Filled.Delete,
                        text = stringResource(R.string.action_clear_cape),
                        modifier = Modifier.weight(1f),
                        enabled = account.capeFileName != null,
                        destructive = true,
                        onClick = { onUpdate(account.copy(capeFileName = null)) },
                    )
                }
            }
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
