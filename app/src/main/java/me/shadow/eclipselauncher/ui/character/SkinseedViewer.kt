/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Compose wrapper for SkinseedView.
 */
package me.shadow.eclipselauncher.ui.character

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import me.shadow.eclipselauncher.R

/**
 * Compose wrapper for [SkinseedView].
 * Provides a 3D skin viewer with drag rotation, pose system, and part selection.
 */
@Composable
fun SkinseedViewer(
    skinBitmap: Bitmap?,
    overlayBitmap: Bitmap? = null,
    capeBitmap: Bitmap? = null,
    variant: String = "CLASSIC",
    overlayEnabled: Boolean = true,
    autoRotate: Boolean = true,
    isFullscreen: Boolean = false,
    onFullscreenToggle: (() -> Unit)? = null,
    onPartSelected: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var skin by remember(skinBitmap) { mutableStateOf(skinBitmap) }
    var overlay by remember(overlayBitmap) { mutableStateOf(overlayBitmap) }
    var cape by remember(capeBitmap) { mutableStateOf(capeBitmap) }
    var viewRef by remember { mutableStateOf<SkinseedView?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                SkinseedView(ctx).apply {
                    setTag(SKIN_MODEL_CANVAS_TAG)
                    updateSkin(skin)
                    updateOverlay(overlay)
                    updateCape(cape)
                    updateVariant(variant)
                    updateOverlayEnabled(overlayEnabled)
                    setAutoRotate(autoRotate)
                }
            },
            update = { view ->
                viewRef = view
                view.updateSkin(skin)
                view.updateOverlay(overlay)
                view.updateCape(cape)
                view.updateVariant(variant)
                view.updateOverlayEnabled(overlayEnabled)
                view.setAutoRotate(autoRotate)
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag(SKIN_MODEL_CANVAS_TAG),
        )

        // Full-screen toggle at top-right
        if (onFullscreenToggle != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                FilledTonalIconButton(
                    onClick = onFullscreenToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(FULLSCREEN_BUTTON_TAG),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
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
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/**
 * Extension functions for easier API.
 */
fun SkinseedView.setPose(poseName: String) = setPose(poseName)
fun SkinseedView.setCustomPose(pose: Map<String, FloatArray>) = setCustomPose(pose)
fun SkinseedView.resetPose() = resetPose()