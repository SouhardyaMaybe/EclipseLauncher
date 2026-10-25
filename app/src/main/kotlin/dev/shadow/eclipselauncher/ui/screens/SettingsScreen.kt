/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.BuildConfig
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.data.LauncherState
import dev.shadow.eclipselauncher.designsystem.component.EclipseBevelButton
import dev.shadow.eclipselauncher.designsystem.component.EclipseDivider
import dev.shadow.eclipselauncher.designsystem.component.EclipseMenuRow
import dev.shadow.eclipselauncher.designsystem.component.EclipsePanel
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.component.EclipseSectionLabel
import dev.shadow.eclipselauncher.designsystem.component.EclipseTopBar
import dev.shadow.eclipselauncher.designsystem.component.eclipseTextFieldColors
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette

@Composable
fun SettingsScreen(
    state: LauncherState,
    onBack: () -> Unit,
    onOpenAccounts: () -> Unit,
) {
    var keyDraft by remember(state.curseForgeKey) { mutableStateOf(state.curseForgeKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        EclipseTopBar(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            EclipseSectionLabel(stringResource(R.string.settings_account))
            EclipseMenuRow(
                icon = EclipsePixelIconKind.ACCOUNT,
                label = state.accountName,
                onClick = onOpenAccounts,
            )
            EclipseDivider()

            EclipseSectionLabel(stringResource(R.string.settings_appearance))
            SettingsValueRow(
                label = stringResource(R.string.settings_theme),
                value = stringResource(R.string.settings_theme_value),
            )
            EclipseDivider()

            EclipseSectionLabel(stringResource(R.string.settings_misc))
            EclipsePanel(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_curseforge_key),
                    style = MaterialTheme.typography.titleSmall,
                    color = EclipsePalette.Text,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyDraft,
                    onValueChange = { keyDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_curseforge_key_label)) },
                    supportingText = {
                        Text(stringResource(R.string.settings_curseforge_key_summary))
                    },
                    singleLine = true,
                    colors = eclipseTextFieldColors(),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EclipseBevelButton(
                        text = stringResource(R.string.settings_curseforge_save),
                        onClick = { state.setCurseForgeKey(keyDraft) },
                        modifier = Modifier.weight(1f),
                        primary = true,
                        height = 48.dp,
                    )
                    if (state.curseForgeKey.isNotBlank()) {
                        EclipseBevelButton(
                            text = stringResource(R.string.settings_curseforge_clear),
                            onClick = {
                                state.setCurseForgeKey("")
                                keyDraft = ""
                            },
                            modifier = Modifier.weight(1f),
                            primary = false,
                            height = 48.dp,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (state.curseForgeKey.isBlank()) {
                        stringResource(R.string.settings_curseforge_status_off)
                    } else {
                        stringResource(R.string.settings_curseforge_status_on)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.curseForgeKey.isBlank()) {
                        EclipsePalette.Muted
                    } else {
                        EclipsePalette.Success
                    },
                )
            }

            EclipseSectionLabel(stringResource(R.string.settings_about))
            SettingsValueRow(
                label = stringResource(R.string.settings_version),
                value = BuildConfig.VERSION_NAME,
            )
            EclipseDivider()
            SettingsValueRow(
                label = stringResource(R.string.settings_license),
                value = stringResource(R.string.settings_license_value),
            )
            EclipseDivider()
            SettingsValueRow(
                label = stringResource(R.string.settings_source),
                value = stringResource(R.string.settings_source_value),
            )
            EclipseDivider()

            Text(
                text = stringResource(R.string.settings_font_license),
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = EclipsePalette.Muted,
            )
            Text(
                text = stringResource(R.string.settings_disclaimer),
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = EclipsePalette.Muted,
            )
            Text(
                text = stringResource(R.string.settings_trademark),
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = EclipsePalette.Muted,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsValueRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = EclipsePalette.Text,
        )
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = EclipsePalette.Muted,
            textAlign = TextAlign.End,
        )
    }
}
