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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

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
    onPartSelected: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var skin by remember(skinBitmap) { mutableStateOf(skinBitmap) }
    var overlay by remember(overlayBitmap) { mutableStateOf(overlayBitmap) }
    var cape by remember(capeBitmap) { mutableStateOf(capeBitmap) }
    var viewRef by remember { mutableStateOf<SkinseedView?>(null) }

    AndroidView(
        factory = { ctx ->
            SkinseedView(ctx).apply {
                setSkin(skin)
                setOverlay(overlay)
                setCape(cape)
                updateVariant(variant)
                updateOverlayEnabled(overlayEnabled)
                setAutoRotate(autoRotate)
            }
        },
        update = { view ->
            viewRef = view
            view.setSkin(skin)
            view.setOverlay(overlay)
            view.setCape(cape)
            view.updateVariant(variant)
            view.updateOverlayEnabled(overlayEnabled)
            view.setAutoRotate(autoRotate)
        },
        modifier = modifier
    )
}

/**
 * Extension functions for easier API.
 */
fun SkinseedView.setPose(poseName: String) = setPose(poseName)
fun SkinseedView.setCustomPose(pose: Map<String, FloatArray>) = setCustomPose(pose)
fun SkinseedView.resetPose() = resetPose()