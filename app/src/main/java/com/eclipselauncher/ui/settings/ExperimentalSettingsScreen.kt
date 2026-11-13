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
import com.eclipselauncher.ui.settings.components.SettingsSwitchRow
import com.eclipselauncher.ui.theme.EclipseColors

@Composable
fun ExperimentalSettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Experimental",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurface,
            modifier = Modifier.padding(horizontal = 4.dp, bottom = 8.dp)
        )

        SettingsSwitchRow(
            icon = "◫",
            title = "Enable shader dumping",
            subtitle = "Save compiled shaders for inspection",
            checked = settings.shaderDumping,
            onCheckedChange = { viewModel.setShaderDumping(it) }
        )

        SettingsSwitchRow(
            icon = "▦",
            title = "Force the renderer to run on the big core",
            subtitle = "Pin rendering to the performance cores",
            checked = settings.forceBigCore,
            onCheckedChange = { viewModel.setForceBigCore(it) }
        )

        SettingsSwitchRow(
            icon = "▤",
            title = "Force rendering into the system memory",
            subtitle = "Allocate GPU buffers in system memory",
            checked = settings.forceSystemMemory,
            onCheckedChange = { viewModel.setForceSystemMemory(it) }
        )

        SettingsSwitchRow(
            icon = "♪",
            title = "Forcefully use OpenSL for audio",
            subtitle = "Use the OpenSL ES audio path",
            checked = settings.forceOpenSL,
            onCheckedChange = { viewModel.setForceOpenSL(it) }
        )

        SettingsSwitchRow(
            icon = "⚠",
            title = "Enable UBWC compression workaround",
            subtitle = "Work around broken UBWC compression on some GPUs",
            checked = settings.ubwcWorkaround,
            onCheckedChange = { viewModel.setUbwcWorkaround(it) }
        )
    }
}

@Composable
fun ExperimentalSettingsScreenWithModals(viewModel: SettingsViewModel = viewModel()) {
    ExperimentalSettingsScreen(viewModel = viewModel)
}