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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.ui.settings.components.SettingsRow
import com.eclipselauncher.ui.settings.components.SettingsSliderRow
import com.eclipselauncher.ui.settings.components.SettingsSwitchRow
import com.eclipselauncher.ui.settings.modals.RendererPickerModal
import com.eclipselauncher.ui.theme.EclipseColors

@Composable
fun VideoRendererSettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val showRendererPicker by viewModel.showRendererPicker.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Video and renderer",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurface,
            modifier = Modifier.padding(horizontal = 4.dp, bottom = 8.dp)
        )

        // Renderer
        SettingsRow(
            icon = "▣",
            title = "Renderer",
            subtitle = settings.renderer,
            onClick = { viewModel.showRendererPicker() }
        )

        // Ignore notch
        SettingsSwitchRow(
            icon = "▯",
            title = "Ignore notch",
            subtitle = "Draw across the whole screen, ignoring the notch area",
            checked = settings.ignoreNotch,
            onCheckedChange = { viewModel.setIgnoreNotch(it) }
        )

        // Resolution Scaler
        SettingsSliderRow(
            icon = "≡",
            title = "Resolution Scaler",
            subtitle = "Render the game at a lower resolution · ${settings.resolutionScaler} %",
            value = settings.resolutionScaler.toFloat(),
            range = 25f..100f,
            steps = 15,
            onValueChange = { viewModel.setResolutionScaler(it.roundToInt()) }
        )

        // Sustained performance mode
        SettingsSwitchRow(
            icon = "≫",
            title = "Sustained performance mode",
            subtitle = "Prefer consistent speed over short bursts",
            checked = settings.sustainedPerformance,
            onCheckedChange = { viewModel.setSustainedPerformance(it) }
        )

        // Alternate surface
        SettingsSwitchRow(
            icon = "▤",
            title = "Alternate surface",
            subtitle = "Use a different surface type for the game view",
            checked = settings.alternateSurface,
            onCheckedChange = { viewModel.setAlternateSurface(it) }
        )

        // Vulkan Driver (placeholder)
        SettingsRow(
            icon = "▤",
            title = "Vulkan Driver",
            subtitle = "Install a custom Adreno driver",
            onClick = { /* Show dialog: not available yet */ }
        )

        // Use system Vulkan driver
        SettingsSwitchRow(
            icon = "▣",
            title = "Use system Vulkan driver",
            subtitle = "Skip the bundled Vulkan driver",
            checked = settings.useSystemVulkan,
            onCheckedChange = { viewModel.setUseSystemVulkan(it) }
        )
    }
}

@Composable
fun VideoRendererSettingsScreenWithModals(viewModel: SettingsViewModel = viewModel()) {
    val showRendererPicker by viewModel.showRendererPicker.collectAsStateWithLifecycle()

    VideoRendererSettingsScreen(viewModel = viewModel)

    if (showRendererPicker) {
        RendererPickerModal(
            currentRenderer = viewModel.settings.value.renderer,
            onRendererSelect = { renderer -> viewModel.setRenderer(renderer) },
            onDismiss = { viewModel.dismissRendererPicker() }
        )
    }
}