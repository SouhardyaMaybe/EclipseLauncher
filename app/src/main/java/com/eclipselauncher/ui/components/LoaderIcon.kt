/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Loader icons using official loader branding assets.
 */
package com.eclipselauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.eclipselauncher.R
import com.eclipselauncher.data.LoaderKind

/**
 * Draws the official loader icon for [loader] scaled into a [size]x[size] square.
 */
@Composable
fun LoaderIcon(
    loader: LoaderKind,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val painter = when (loader) {
        LoaderKind.VANILLA -> painterResource(R.drawable.grassblock)
        LoaderKind.FABRIC -> painterResource(R.drawable.fabric)
        LoaderKind.FORGE -> painterResource(R.drawable.forge)
        LoaderKind.NEOFORGE -> painterResource(R.drawable.neoforge)
        LoaderKind.OPTIFINE -> painterResource(R.drawable.optifine)
        LoaderKind.QUILT -> painterResource(R.drawable.quilt)
    }

    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier.size(size),
    )
}