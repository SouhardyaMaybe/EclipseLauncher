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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.shadow.eclipselauncher.BuildConfig
import dev.shadow.eclipselauncher.R
import dev.shadow.eclipselauncher.data.LauncherState
import dev.shadow.eclipselauncher.designsystem.component.EclipseBackRow
import dev.shadow.eclipselauncher.designsystem.component.EclipseBevelButton
import dev.shadow.eclipselauncher.designsystem.component.EclipseDivider
import dev.shadow.eclipselauncher.designsystem.component.EclipsePanel
import dev.shadow.eclipselauncher.designsystem.component.EclipsePixelIconKind
import dev.shadow.eclipselauncher.designsystem.component.EclipsePreferenceRow
import dev.shadow.eclipselauncher.designsystem.component.EclipseSectionLabel
import dev.shadow.eclipselauncher.designsystem.component.EclipseSwitchRow
import dev.shadow.eclipselauncher.designsystem.component.EclipseValueRow
import dev.shadow.eclipselauncher.designsystem.component.eclipseTextFieldColors
import dev.shadow.eclipselauncher.designsystem.theme.EclipsePalette

/** The sub-screens reached from the Settings category list. */
enum class SettingsPage {
    APPEARANCE,
    MISCELLANEOUS,
    ABOUT,
}

@Composable
fun SettingsScreen(
    state: LauncherState,
    onBack: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit,
) {
    val curseForgeSummary = if (state.curseForgeKey.isBlank()) {
        stringResource(R.string.settings_curseforge_status_off)
    } else {
        stringResource(R.string.settings_curseforge_status_on)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        EclipseBackRow(
            label = stringResource(R.string.action_back),
            onClick = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            EclipseSectionLabel(stringResource(R.string.settings_account))
            EclipsePreferenceRow(
                icon = EclipsePixelIconKind.ACCOUNT,
                title = state.accountName,
                summary = stringResource(R.string.account_local),
                onClick = onOpenAccounts,
            )
            EclipseDivider()

            EclipseSectionLabel(stringResource(R.string.settings_appearance))
            EclipsePreferenceRow(
                icon = EclipsePixelIconKind.EDIT,
                title = stringResource(R.string.settings_theme),
                summary = stringResource(R.string.settings_theme_value),
                onClick = { onOpenPage(SettingsPage.APPEARANCE) },
            )
            EclipseDivider()

            EclipseSectionLabel(stringResource(R.string.settings_misc))
            EclipsePreferenceRow(
                icon = EclipsePixelIconKind.SETTINGS,
                title = stringResource(R.string.settings_misc),
                summary = curseForgeSummary,
                onClick = { onOpenPage(SettingsPage.MISCELLANEOUS) },
            )
            EclipseDivider()

            EclipseSectionLabel(stringResource(R.string.settings_about))
            EclipsePreferenceRow(
                icon = EclipsePixelIconKind.MORE,
                title = stringResource(R.string.settings_about),
                summary = stringResource(R.string.settings_version_summary, BuildConfig.VERSION_NAME),
                onClick = { onOpenPage(SettingsPage.ABOUT) },
            )
            EclipseDivider()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsDetailScreen(
    state: LauncherState,
    page: SettingsPage,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EclipsePalette.Canvas),
    ) {
        EclipseBackRow(
            label = stringResource(R.string.action_back),
            onClick = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                SettingsPage.APPEARANCE -> AppearanceSettings()
                SettingsPage.MISCELLANEOUS -> MiscellaneousSettings(state)
                SettingsPage.ABOUT -> AboutSettings()
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AppearanceSettings() {
    SettingsHeading(stringResource(R.string.settings_appearance))

    EclipseSectionLabel(stringResource(R.string.settings_appearance_theme))
    EclipseValueRow(
        label = stringResource(R.string.settings_theme),
        value = stringResource(R.string.settings_theme_value),
    )
    EclipseDivider()
    EclipseValueRow(
        label = stringResource(R.string.settings_appearance_font),
        value = stringResource(R.string.settings_appearance_font_value),
    )
    EclipseDivider()

    Text(
        text = stringResource(R.string.settings_appearance_note),
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = EclipsePalette.Muted,
    )
}

@Composable
private fun MiscellaneousSettings(state: LauncherState) {
    var keyDraft by remember(state.curseForgeKey) { mutableStateOf(state.curseForgeKey) }

    SettingsHeading(stringResource(R.string.settings_misc))

    EclipseSectionLabel(stringResource(R.string.settings_misc_curseforge))
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
                onClick = { state.updateCurseForgeKey(keyDraft) },
                modifier = Modifier.weight(1f),
                primary = true,
                height = 48.dp,
            )
            if (state.curseForgeKey.isNotBlank()) {
                EclipseBevelButton(
                    text = stringResource(R.string.settings_curseforge_clear),
                    onClick = {
                        state.updateCurseForgeKey("")
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

    EclipseSectionLabel(stringResource(R.string.settings_misc_downloads))
    EclipseSwitchRow(
        title = stringResource(R.string.content_auto_deps),
        summary = stringResource(R.string.content_auto_deps_summary),
        checked = state.autoInstallDependencies,
        onCheckedChange = state::updateAutoInstallDependencies,
    )
    EclipseDivider()

    Text(
        text = stringResource(R.string.settings_misc_curseforge_note),
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = EclipsePalette.Muted,
    )
}

@Composable
private fun AboutSettings() {
    SettingsHeading(stringResource(R.string.settings_about))

    EclipseValueRow(
        label = stringResource(R.string.settings_version),
        value = BuildConfig.VERSION_NAME,
    )
    EclipseDivider()
    EclipseValueRow(
        label = stringResource(R.string.settings_license),
        value = stringResource(R.string.settings_license_value),
    )
    EclipseDivider()
    EclipseValueRow(
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
}

@Composable
private fun SettingsHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        color = EclipsePalette.Text,
    )
}
