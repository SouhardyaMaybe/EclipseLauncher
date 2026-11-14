/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package me.shadow.eclipselauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.data.Instance
import me.shadow.eclipselauncher.data.LoaderKind
import me.shadow.eclipselauncher.data.MediaStorage
import android.graphics.BitmapFactory

/** Deterministic per-name tint so tiles are recognisable at a glance. */
private val tilePalette = listOf(
    Color(0xFF3B8526),
    Color(0xFF2F6B1E),
    Color(0xFF47484A),
    Color(0xFF39393B),
    Color(0xFF5AAE43),
    Color(0xFF2A6B7C),
)

/** Localised loader name for display next to a version string. */
@Composable
fun loaderName(instance: Instance): String = loaderName(instance.loader)

/** Localised loader name for a bare loader kind (composable). */
@Composable
fun loaderName(loader: LoaderKind): String = stringResource(
    when (loader) {
        LoaderKind.VANILLA -> R.string.loader_vanilla
        LoaderKind.FABRIC -> R.string.loader_fabric
        LoaderKind.FORGE -> R.string.loader_forge
        LoaderKind.NEOFORGE -> R.string.loader_neoforge
        LoaderKind.QUILT -> R.string.loader_quilt
        LoaderKind.OPTIFINE -> R.string.loader_optifine
    },
)

/** Loader name usable outside composition (toasts, coroutines, store writes). */
fun loaderNameString(loader: LoaderKind): String = when (loader) {
    LoaderKind.VANILLA -> "Vanilla"
    LoaderKind.FABRIC -> "Fabric"
    LoaderKind.FORGE -> "Forge"
    LoaderKind.NEOFORGE -> "NeoForge"
    LoaderKind.QUILT -> "Quilt"
    LoaderKind.OPTIFINE -> "OptiFine"
}

/** A rounded single-letter tile: the default icon for accounts/instances. */
@Composable
fun LetterTile(
    label: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val initial = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val color = tilePalette[(label.hashCode().mod(tilePalette.size))]
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size.value * 0.24f))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * The instance icon: the imported image when present, otherwise a letter
 * tile. Decoding happens off the main thread, keyed by file name.
 */
@Composable
fun InstanceIcon(
    instance: Instance,
    media: MediaStorage,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    var icon by remember(instance.iconFileName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(instance.iconFileName) {
        icon = instance.iconFileName
            ?.let { media.resolve(it) }
            ?.let { file ->
                withContext(Dispatchers.IO) {
                    runCatching {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    }.getOrNull()
                }
            }
    }
    val bitmap = icon
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size.value * 0.24f)),
            contentScale = ContentScale.Crop,
        )
    } else {
        LetterTile(label = instance.name, size = size, modifier = modifier)
    }
}
