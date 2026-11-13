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
import com.eclipselauncher.ui.settings.components.SettingsSliderRow
import com.eclipselauncher.ui.settings.components.SettingsSwitchRow
import com.eclipselauncher.ui.theme.EclipseColors

@Composable
fun ControlCustomizationSettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Control customization",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurface,
            modifier = Modifier.padding(horizontal = 4.dp, bottom = 8.dp)
        )

        // Edit custom controls (placeholder)
        SettingsRow(
            icon = "✎",
            title = "Edit custom controls",
            subtitle = "Move and resize the on-screen buttons",
            onClick = { /* Show not available dialog */ }
        )

        // Gestures heading
        Text(
            text = "Gestures",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, top = 16.dp, bottom = 8.dp)
        )

        SettingsSwitchRow(
            icon = "⊘",
            title = "Disable gestures",
            subtitle = "Turn off pinch and edge-swipe gestures in game",
            checked = settings.disableGestures,
            onCheckedChange = { viewModel.setDisableGestures(it) }
        )

        SettingsSwitchRow(
            icon = "◑",
            title = "Disable double tap to swap hands",
            subtitle = "Double tapping no longer swaps the off-hand item",
            checked = settings.disableDoubleTapSwap,
            onCheckedChange = { viewModel.setDisableDoubleTapSwap(it) }
        )

        if (!settings.disableGestures) {
            SettingsSliderRow(
                icon = "◷",
                title = "Trigger long press delay",
                subtitle = "How long a press must be held to count as a long press · ${settings.longPressDelay} ms",
                value = settings.longPressDelay.toFloat(),
                range = 100f..1000f,
                steps = 90,
                onValueChange = { viewModel.setLongPressDelay(it.roundToInt()) }
            )
        }

        // Buttons heading
        Text(
            text = "Buttons",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, top = 16.dp, bottom = 8.dp)
        )

        SettingsSliderRow(
            icon = "⊕",
            title = "Control buttons scaling",
            subtitle = "Size of the on-screen buttons · ${settings.controlScale} %",
            value = settings.controlScale.toFloat(),
            range = 80f..250f,
            steps = 34,
            onValueChange = { viewModel.setControlScale(it.roundToInt()) }
        )

        SettingsSliderRow(
            icon = "◍",
            title = "Buttons transparency",
            subtitle = "How see-through the on-screen buttons are · ${settings.buttonTransparency} %",
            value = settings.buttonTransparency.toFloat(),
            range = 0f..100f,
            steps = 20,
            onValueChange = { viewModel.setButtonTransparency(it.roundToInt()) }
        )

        SettingsSwitchRow(
            icon = "",
            title = "Only use capital letters",
            subtitle = "",
            checked = settings.capitalLetters,
            onCheckedChange = { viewModel.setCapitalLetters(it) }
        )

        // Virtual mouse heading
        Text(
            text = "Virtual mouse",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, top = 16.dp, bottom = 8.dp)
        )

        SettingsSliderRow(
            icon = "",
            title = "Mouse scaling",
            subtitle = "${settings.mouseScale} %",
            value = settings.mouseScale.toFloat(),
            range = 25f..300f,
            steps = 55,
            onValueChange = { viewModel.setMouseScale(it.roundToInt()) }
        )

        SettingsSliderRow(
            icon = "",
            title = "Mouse Speed",
            subtitle = "${settings.mouseSpeed} %",
            value = settings.mouseSpeed.toFloat(),
            range = 25f..300f,
            steps = 55,
            onValueChange = { viewModel.setMouseSpeed(it.roundToInt()) }
        )

        SettingsSwitchRow(
            icon = "",
            title = "Start with virtual mouse on",
            subtitle = "",
            checked = settings.startWithVirtualMouse,
            onCheckedChange = { viewModel.setStartWithVirtualMouse(it) }
        )

        // Gyroscope heading (conditionally shown)
        Text(
            text = "Gyroscope",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, top = 16.dp, bottom = 8.dp)
        )

        SettingsSwitchRow(
            icon = "",
            title = "Enable gyroscope",
            subtitle = "",
            checked = settings.enableGyro,
            onCheckedChange = { viewModel.setEnableGyro(it) }
        )

        if (settings.enableGyro) {
            SettingsSliderRow(
                icon = "",
                title = "Sensitivity",
                subtitle = "${settings.gyroSensitivity} %",
                value = settings.gyroSensitivity.toFloat(),
                range = 25f..300f,
                steps = 55,
                onValueChange = { viewModel.setGyroSensitivity(it.roundToInt()) }
            )

            SettingsSliderRow(
                icon = "",
                title = "Sampling rate",
                subtitle = "${settings.gyroSampleRate} ms",
                value = settings.gyroSampleRate.toFloat(),
                range = 5f..50f,
                steps = 45,
                onValueChange = { viewModel.setGyroSampleRate(it.roundToInt()) }
            )

            SettingsSwitchRow(
                icon = "",
                title = "Smoothing",
                subtitle = "",
                checked = settings.gyroSmoothing,
                onCheckedChange = { viewModel.setGyroSmoothing(it) }
            )

            SettingsSwitchRow(
                icon = "",
                title = "Invert X axis",
                subtitle = "",
                checked = settings.invertGyroX,
                onCheckedChange = { viewModel.setInvertGyroX(it) }
            )

            SettingsSwitchRow(
                icon = "",
                title = "Invert Y axis",
                subtitle = "",
                checked = settings.invertGyroY,
                onCheckedChange = { viewModel.setInvertGyroY(it) }
            )
        }

        // Controller heading
        Text(
            text = "Controller",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, top = 16.dp, bottom = 8.dp)
        )

        SettingsRow(
            icon = "",
            title = "Change controller key bindings",
            subtitle = "Not available yet",
            onClick = { /* Show not available */ }
        )

        SettingsRow(
            icon = "",
            title = "Reset controller mapping",
            subtitle = "Not available yet",
            onClick = { /* Show not available */ }
        )

        SettingsSliderRow(
            icon = "",
            title = "Joystick deadzone scale",
            subtitle = "${settings.joystickDeadzone} %",
            value = settings.joystickDeadzone.toFloat(),
            range = 50f..200f,
            steps = 30,
            onValueChange = { viewModel.setJoystickDeadzone(it.roundToInt()) }
        )

        // Keyboard heading
        Text(
            text = "Keyboard",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, top = 16.dp, bottom = 8.dp)
        )

        SettingsSwitchRow(
            icon = "",
            title = "Enable keyboard panning",
            subtitle = "",
            checked = settings.keyboardPanning,
            onCheckedChange = { viewModel.setKeyboardPanning(it) }
        )
    }
}

@Composable
fun ControlCustomizationSettingsScreenWithModals(viewModel: SettingsViewModel = viewModel()) {
    ControlCustomizationSettingsScreen(viewModel = viewModel)
}