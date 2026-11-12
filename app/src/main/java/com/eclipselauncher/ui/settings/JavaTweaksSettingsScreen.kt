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
import com.eclipselauncher.ui.settings.modals.JvmArgumentsEditorModal
import com.eclipselauncher.ui.theme.EclipseColors

@Composable
fun JavaTweaksSettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Java Tweaks",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkOnSurface,
            modifier = Modifier.padding(horizontal = 4.dp, bottom = 8.dp)
        )

        // Runtime Manager
        SettingsRow(
            icon = "▰",
            title = "Runtime Manager",
            subtitle = "Install and pick Java runtimes",
            onClick = { /* Show info dialog: not available yet */ }
        )

        // JVM Launch arguments
        SettingsRow(
            icon = "▤",
            title = "JVM Launch arguments",
            subtitle = "Extra arguments passed to the Java virtual machine",
            onClick = { viewModel.showJvmArgsEditor() }
        )

        // Memory allocation
        SettingsSliderRow(
            icon = "▦",
            title = "Memory allocation",
            subtitle = "How much RAM the game may use · ${settings.memoryAllocation} MB",
            value = settings.memoryAllocation.toFloat(),
            range = 256f..8192f,
            steps = 992,
            onValueChange = { viewModel.setMemoryAllocation(it.roundToInt()) }
        )

        // Sandbox .jar execution
        SettingsSwitchRow(
            icon = "◈",
            title = "Sandbox .jar execution",
            subtitle = "Run jars in an isolated process",
            checked = settings.sandboxJar,
            onCheckedChange = { viewModel.setSandboxJar(it) }
        )
    }
}

@Composable
fun JavaTweaksSettingsScreenWithModals(viewModel: SettingsViewModel = viewModel()) {
    val showJvmArgsEditor by viewModel.showJvmArgsEditor.collectAsStateWithLifecycle()

    JavaTweaksSettingsScreen(viewModel = viewModel)

    if (showJvmArgsEditor) {
        JvmArgumentsEditorModal(
            initialArgs = viewModel.settings.value.jvmArguments,
            onSave = { args -> viewModel.setJvmArguments(args) },
            onDismiss = { viewModel.dismissJvmArgsEditor() }
        )
    }
}