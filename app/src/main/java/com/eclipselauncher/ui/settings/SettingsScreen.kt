package com.eclipselauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.ui.navigation.NavigationDestination
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val currentCategory by viewModel.currentCategory.collectAsState()
    val category = currentCategory

    EclipseLauncherTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EclipseColors.DarkBackground
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Status bar spacer
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                    )

                    // Top bar with back arrow
                    if (currentCategory != null) {
                        // In-category screen - back arrow + title + in-page back
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back to categories",
                                    tint = EclipseColors.DarkOnSurface,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .fillMaxWidth()
                                        .clickable { viewModel.goBack() }
                                )
                                Text(
                                    text = currentCategory?.name ?: "",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EclipseColors.DarkOnSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                                )
                            }
                            
                            // In-page back line
                            Text(
                                text = "←  Back to the last screen",
                                fontSize = 14.sp,
                                color = EclipseColors.DarkPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            )
                        }
                    } else {
                        // Categories list - just title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Leave Settings",
                                tint = EclipseColors.DarkOnSurface,
                                modifier = Modifier
                                    .size(48.dp)
                                    .fillMaxWidth()
                                    .clickable { onNavigate("home") }
                            )
                            Text(
                                text = "Settings",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = EclipseColors.DarkOnSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    // Content
                    if (category == null) {
                        SettingsCategoriesList(viewModel = viewModel, onNavigate = onNavigate)
                    } else {
                        when (category) {
                            SettingsViewModel.SettingsCategory.VIDEO_RENDERER -> 
                                VideoRendererSettingsScreenWithModals(viewModel = viewModel)
                            SettingsViewModel.SettingsCategory.CONTROL_CUSTOMIZATION -> 
                                ControlCustomizationSettingsScreenWithModals(viewModel = viewModel)
                            SettingsViewModel.SettingsCategory.JAVA_TWEAKS -> 
                                JavaTweaksSettingsScreenWithModals(viewModel = viewModel)
                            SettingsViewModel.SettingsCategory.MISCELLANEOUS -> 
                                MiscellaneousSettingsScreenWithModals(viewModel = viewModel)
                            SettingsViewModel.SettingsCategory.EXPERIMENTAL -> 
                                ExperimentalSettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCategoriesList(
    viewModel: SettingsViewModel,
    onNavigate: (String) -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Categories label
        Text(
            text = "Categories",
            fontSize = 14.sp,
            color = EclipseColors.DarkOnSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
        )

        // Category cards
        SettingsCategoryCard(
            icon = "▣",
            title = "Video and renderer",
            subtitle = "Resolution, surface and driver options",
            onClick = { viewModel.selectCategory(SettingsViewModel.SettingsCategory.VIDEO_RENDERER) }
        )
        SettingsCategoryCard(
            icon = "✛",
            title = "Control customization",
            subtitle = "On-screen controls, mouse and gyroscope",
            onClick = { viewModel.selectCategory(SettingsViewModel.SettingsCategory.CONTROL_CUSTOMIZATION) }
        )
        SettingsCategoryCard(
            icon = "⌘",
            title = "Java Tweaks",
            subtitle = "Runtime, arguments and memory",
            onClick = { viewModel.selectCategory(SettingsViewModel.SettingsCategory.JAVA_TWEAKS) }
        )
        SettingsCategoryCard(
            icon = "▦",
            title = "Miscellaneous settings",
            subtitle = "Files, downloads and behaviour",
            onClick = { viewModel.selectCategory(SettingsViewModel.SettingsCategory.MISCELLANEOUS) }
        )
        SettingsCategoryCard(
            icon = "⚗",
            title = "Experimental Stuff",
            subtitle = "Risky options — expect rough edges",
            onClick = { viewModel.selectCategory(SettingsViewModel.SettingsCategory.EXPERIMENTAL) }
        )

        // Independent rows
        SettingsSwitchCard(
            icon = "◐",
            title = "Force language to English",
            subtitle = "Ignore the system language and use English",
            checked = settings.forceEnglish,
            onCheckedChange = { viewModel.setForceEnglish(it) }
        )

        SettingsSwitchCard(
            icon = "◔",
            title = "Allow notifications",
            subtitle = "Let Eclipse notify you about background work",
            checked = settings.allowNotifications,
            onCheckedChange = { viewModel.setAllowNotifications(it) }
        )
    }
}

@Composable
fun SettingsCategoryCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = EclipseColors.DarkSurface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 24.sp,
                color = EclipseColors.DarkPrimary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EclipseColors.DarkOnSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = EclipseColors.DarkOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchCard(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = EclipseColors.DarkSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 24.sp,
                color = EclipseColors.DarkPrimary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EclipseColors.DarkOnSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = EclipseColors.DarkOnSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (checked) EclipseColors.DarkPrimary else EclipseColors.DarkOutlineVariant,
                    checkedTrackColor = if (checked) EclipseColors.DarkPrimary.copy(alpha = 0.3f) else EclipseColors.DarkSurfaceVariant
                )
            )
        }
    }
}