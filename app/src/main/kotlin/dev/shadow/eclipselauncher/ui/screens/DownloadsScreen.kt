/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.shadow.eclipselauncher.R

@Composable
fun DownloadsScreen() {
    SectionScreen(
        title = stringResource(R.string.downloads_title),
        body = stringResource(R.string.downloads_body),
        emptyTitle = stringResource(R.string.downloads_empty_title),
        emptyBody = stringResource(R.string.downloads_empty_body),
    )
}
