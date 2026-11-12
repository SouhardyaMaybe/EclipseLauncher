package com.eclipselauncher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.MaterialTheme

@Composable
fun HeaderBar(
    onFolderClick: () -> Unit,
    onDocumentClick: () -> Unit,
    onTaskClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App name
        Text(
            text = "Eclipse Launcher",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = EclipseColors.DarkPrimary,
            fontFamily = MaterialTheme.typography.titleLarge.fontFamily
        )

        // Action icons - folder, document, task list, gear
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(
                imageVector = Folder,
                contentDescription = "Manage Content",
                onClick = onFolderClick
            )
            HeaderIconButton(
                imageVector = Description,
                contentDescription = "Document",
                onClick = onDocumentClick
            )
            HeaderIconButton(
                imageVector = ListAlt,
                contentDescription = "Tasks",
                onClick = onTaskClick
            )
            HeaderIconButton(
                imageVector = Settings,
                contentDescription = "Settings",
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun HeaderIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = EclipseColors.DarkOnBackground.copy(alpha = 0.8f)
        )
    }
}