package com.eclipselauncher.ui.tools

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun ToolsScreen(
    onNavigate: (String) -> Unit
) {
    EclipseLauncherTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EclipseColors.DarkBackground
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                // Status bar spacer
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                )

                // Bare back arrow at top left
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, top = 8.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = EclipseColors.DarkPrimary,
                        modifier = Modifier
                            .size(48.dp),
                        onClick = { onNavigate("home") }
                    )
                }

                // Tools label
                Text(
                    text = "Tools",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EclipseColors.DarkPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, top = 8.dp, bottom = 16.dp)
                )

                // Tool cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToolCard(
                        icon = "◈",
                        title = "Mod market",
                        subtitle = "Browse and search mods on Modrinth",
                        onClick = { onNavigate("tools/mod_market") }
                    )
                    ToolCard(
                        icon = "▰",
                        title = "File manager",
                        subtitle = "Browse the launcher's private files",
                        onClick = { onNavigate("tools/file_manager") }
                    )
                    ToolCard(
                        icon = "▦",
                        title = "Manage content",
                        subtitle = "Skins, capes, icons and modpack archives",
                        onClick = { onNavigate("tools/manage_content") }
                    )
                    ToolCard(
                        icon = "▤",
                        title = "Manage logs",
                        subtitle = "Read and clear the launcher's log files",
                        onClick = { onNavigate("tools/manage_logs") }
                    )
                    ToolCard(
                        icon = "⌘",
                        title = "Execute a .jar",
                        subtitle = "Pick a jar file to run",
                        onClick = { onNavigate("tools/execute_jar") }
                    )
                    ToolCard(
                        icon = "✛",
                        title = "Edit controls",
                        subtitle = "Control mappings and sensitivity settings",
                        onClick = { onNavigate("settings/control_customization") }
                    )
                }
            }
        }
    }
}

@Composable
fun ToolCard(
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