/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.shadow.eclipselauncher.R

@Composable
fun InstancesScreen() {
    SectionScreen(
        title = stringResource(R.string.instances_title),
        body = stringResource(R.string.instances_body),
        emptyTitle = stringResource(R.string.instances_empty_title),
        emptyBody = stringResource(R.string.instances_empty_body),
    )
}
