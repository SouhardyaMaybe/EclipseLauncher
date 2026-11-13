package com.eclipselauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.ui.settings.components.SettingsRow
import com.eclipselauncher.ui.settings.components.SettingsSwitchRow
import com.eclipselauncher.ui.settings.modals.CurseForgeApiKeyEditorModal
import com.eclipselauncher.ui.settings.modals.DownloadSourcePickerModal
import com.eclipselauncher.ui.theme.EclipseColors

@Composable
fun MiscellaneousSettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Miscellaneous settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurface,
            modifier = Modifier.padding(horizontal = 4.dp, bottom = 8.dp)
        )

        SettingsSwitchRow(
            icon = "✔",
            title = "Verify game files",
            subtitle = "Check game files before launch",
            checked = settings.verifyGameFiles,
            onCheckedChange = { viewModel.setVerifyGameFiles(it) }
        )

        SettingsSwitchRow(
            icon = "⚡",
            title = "Fast game startup",
            subtitle = "Skip optional work at launch",
            checked = settings.fastStartup,
            onCheckedChange = { viewModel.setFastStartup(it) }
        )

        SettingsRow(
            icon = "⇩",
            title = "Game download source",
            subtitle = settings.downloadSource,
            onClick = { viewModel.showDownloadSourcePicker() }
        )

        SettingsSwitchRow(
            icon = "✧",
            title = "Verify game version manifest",
            subtitle = "Check the version manifest before launch",
            checked = settings.verifyManifest,
            onCheckedChange = { viewModel.setVerifyManifest(it) }
        )

        SettingsRow(
            icon = "▰",
            title = "Import data",
            subtitle = "Bring settings and instances from another install",
            onClick = { /* Show not available yet */ }
        )

        SettingsRow(
            icon = "↻",
            title = "Clear metadata cache",
            subtitle = "Delete cached version lists so they reload",
            onClick = { /* Clear cache */ }
        )

        SettingsRow(
            icon = "⌫",
            title = "Clear download cache",
            subtitle = "Frees up cached downloads",
            onClick = { /* Clear download cache */ }
        )

        SettingsSwitchRow(
            icon = "⚠",
            title = "Show memory warning dialog",
            subtitle = "Warn when the device is low on memory",
            checked = settings.memoryWarning,
            onCheckedChange = { viewModel.setMemoryWarning(it) }
        )

        SettingsSwitchRow(
            icon = "",
            title = "Disable server blocklist",
            subtitle = "",
            checked = settings.disableBlocklist,
            onCheckedChange = { viewModel.setDisableBlocklist(it) }
        )

        // Eclipse settings section
        Text(
            text = "Eclipse settings",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, top = 16.dp, bottom = 8.dp)
        )

        SettingsRow(
            icon = "",
            title = "CurseForge API key",
            subtitle = if (settings.curseforgeApiKey.isBlank()) "Not set" else "Set",
            onClick = { viewModel.showCurseForgeKeyEditor() }
        )

        SettingsSwitchRow(
            icon = "",
            title = "Auto-install required dependencies",
            subtitle = "",
            checked = settings.autoInstallDeps,
            onCheckedChange = { viewModel.setAutoInstallDeps(it) }
        )
    }
}

@Composable
fun MiscellaneousSettingsScreenWithModals(viewModel: SettingsViewModel = viewModel()) {
    val showDownloadSourcePicker by viewModel.showDownloadSourcePicker.collectAsStateWithLifecycle()
    val showCurseForgeKeyEditor by viewModel.showCurseForgeKeyEditor.collectAsStateWithLifecycle()

    MiscellaneousSettingsScreen(viewModel = viewModel)

    if (showDownloadSourcePicker) {
        DownloadSourcePickerModal(
            currentSource = viewModel.settings.value.downloadSource,
            onSourceSelect = { source -> viewModel.setDownloadSource(source) },
            onDismiss = { viewModel.dismissDownloadSourcePicker() }
        )
    }

    if (showCurseForgeKeyEditor) {
        CurseForgeApiKeyEditorModal(
            currentKey = viewModel.settings.value.curseforgeApiKey,
            onSave = { key -> viewModel.setCurseforgeApiKey(key) },
            onClear = { viewModel.setCurseforgeApiKey("") },
            onDismiss = { viewModel.dismissCurseForgeKeyEditor() }
        )
    }
}