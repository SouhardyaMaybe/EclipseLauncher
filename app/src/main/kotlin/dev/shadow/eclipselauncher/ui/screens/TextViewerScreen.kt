/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.data.FileOps
import dev.shadow.eclipselauncher.designsystem.component.EclipseEmptyState
import dev.shadow.eclipselauncher.designsystem.component.EclipseTopBar
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface Preview {
    data object Loading : Preview
    data object Binary : Preview
    data object Error : Preview
    data class Text(val content: String, val truncated: Boolean) : Preview
}

@Composable
fun TextViewerScreen(
    file: File,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var preview by remember(file.absolutePath) { mutableStateOf<Preview>(Preview.Loading) }

    LaunchedEffect(file.absolutePath) {
        preview = withContext(Dispatchers.IO) {
            runCatching {
                when {
                    !file.isFile -> Preview.Error
                    !FileOps.isProbablyText(file) -> Preview.Binary
                    else -> {
                        val (content, truncated) = FileOps.readText(file)
                        Preview.Text(content, truncated)
                    }
                }
            }.getOrElse { Preview.Error }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        EclipseTopBar(
            title = file.name,
            backContentDescription = stringResource(R.string.action_back),
            onBack = onBack,
            actions = {
                TextButton(onClick = { FileOps.share(context, listOf(file)) }) {
                    Text(stringResource(R.string.action_share))
                }
            },
        )

        when (val state = preview) {
            Preview.Loading -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.action_loading),
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = EclipsePalette.Muted,
                    )
                }
            }

            Preview.Binary -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EclipseEmptyState(
                        title = stringResource(R.string.logs_binary_title),
                        body = stringResource(R.string.logs_binary_body),
                        action = {
                            TextButton(onClick = { FileOps.share(context, listOf(file)) }) {
                                Text(stringResource(R.string.action_share))
                            }
                        },
                    )
                }
            }

            Preview.Error -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EclipseEmptyState(
                        title = stringResource(R.string.logs_read_error),
                        body = file.absolutePath,
                        action = {
                            TextButton(onClick = { FileOps.share(context, listOf(file)) }) {
                                Text(stringResource(R.string.action_share))
                            }
                        },
                    )
                }
            }

            is Preview.Text -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    if (state.truncated) {
                        Text(
                            text = stringResource(R.string.logs_too_large),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = EclipsePalette.Warning,
                        )
                    }
                    SelectionContainer {
                        Text(
                            text = state.content,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = EclipsePalette.Text,
                        )
                    }
                }
            }
        }
    }
}
