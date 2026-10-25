/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.core.model.InstanceSummary
import dev.shadow.eclipselauncher.designsystem.component.EclipseCenteredHeading
import dev.shadow.eclipselauncher.designsystem.component.EclipseDivider
import dev.shadow.eclipselauncher.designsystem.component.EclipseMenuRow
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette
import dev.shadow.eclipselauncher.ui.components.AccountBar
import dev.shadow.eclipselauncher.ui.components.BottomInstanceBar

/**
 * The whole main page. There is exactly one section, Quick Actions, and every
 * row lives inside it. The account bar and the instance/Play bar are chrome,
 * not additional sections.
 */
@Composable
fun HomeScreen(
    accountName: String,
    taskCount: Int,
    instance: InstanceSummary?,
    onAccountClick: () -> Unit,
    onTasksClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onControlsClick: () -> Unit,
    onLogsClick: () -> Unit,
    onFilesClick: () -> Unit,
    onContentClick: () -> Unit,
    onInstallJarClick: () -> Unit,
    onCreateInstance: () -> Unit,
    onEditInstance: () -> Unit,
    onPlayClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        AccountBar(
            accountName = accountName,
            taskCount = taskCount,
            onAccountClick = onAccountClick,
            onTasksClick = onTasksClick,
            onSettingsClick = onSettingsClick,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            EclipseCenteredHeading(
                text = stringResource(R.string.home_quick_actions),
                topPadding = 24.dp,
            )

            EclipseMenuRow(
                icon = EclipsePixelIconKind.CONTROLS,
                label = stringResource(R.string.home_controls),
                onClick = onControlsClick,
            )
            EclipseDivider()
            EclipseMenuRow(
                icon = EclipsePixelIconKind.LOGS,
                label = stringResource(R.string.home_logs),
                onClick = onLogsClick,
            )
            EclipseDivider()
            EclipseMenuRow(
                icon = EclipsePixelIconKind.FOLDER,
                label = stringResource(R.string.home_files),
                onClick = onFilesClick,
            )
            EclipseDivider()
            EclipseMenuRow(
                icon = EclipsePixelIconKind.CONTENT,
                label = stringResource(R.string.home_content),
                onClick = onContentClick,
            )
            EclipseDivider()
            EclipseMenuRow(
                icon = EclipsePixelIconKind.JAR,
                label = stringResource(R.string.home_install_jar),
                onClick = onInstallJarClick,
            )
            EclipseDivider()

            Spacer(Modifier.height(24.dp))
        }

        BottomInstanceBar(
            instance = instance,
            onEditInstance = onEditInstance,
            onCreateInstance = onCreateInstance,
            onPlayClick = onPlayClick,
        )
    }
}
