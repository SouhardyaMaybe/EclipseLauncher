package com.eclipselauncher.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipselauncher.R
import com.eclipselauncher.model.Instance
import com.eclipselauncher.ui.theme.EclipseColors

@Composable
fun InstancePanel(
    instance: Instance?,
    onInstanceClick: () -> Unit,
    onEditClick: () -> Unit,
    onSwitcherClick: () -> Unit
) {
    if (instance != null) {
        // Existing instance panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(18.dp),
            color = EclipseColors.DarkSurface,
            border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant),
            onClick = onInstanceClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Letter tile (30dp)
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = instance.name.first().uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkPrimary
                    )
                }

                // Instance info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = instance.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = EclipseColors.DarkOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${instance.version} · ${instance.loader.name}",
                        fontSize = 12.sp,
                        color = EclipseColors.DarkOnSurfaceVariant
                    )
                }

                // Caret + Edit button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Switch instance",
                        tint = EclipseColors.DarkOnSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 8.dp)
                    )
                    
                    // Edit button (pencil) - 58dp cell
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = EclipseColors.DarkSurfaceVariant,
                        onClick = onEditClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit instance",
                            tint = EclipseColors.DarkOnSurface,
                            modifier = Modifier
                                .size(58.dp)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    } else {
        // No instances - empty state row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(18.dp),
            color = EclipseColors.DarkSurface,
            border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant),
            onClick = onInstanceClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plus icon
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create instance",
                    tint = EclipseColors.DarkPrimary,
                    modifier = Modifier.size(24.dp)
                )
                
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No instances",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = EclipseColors.DarkOnSurface
                    )
                    Text(
                        text = "Tap here to create a new instance",
                        fontSize = 12.sp,
                        color = EclipseColors.DarkOnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PlayBar(
    instance: Instance?,
    onPlayClick: () -> Unit
) {
    val isEnabled = instance != null
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isEnabled) EclipseColors.DarkPrimary else EclipseColors.DarkSurfaceVariant,
        onClick = { if (isEnabled) onPlayClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = if (isEnabled) EclipseColors.DarkOnPrimary else EclipseColors.DarkOnSurfaceVariant,
                modifier = Modifier.size(24.dp).padding(end = 8.dp)
            )
            Text(
                text = "Play",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) EclipseColors.DarkOnPrimary else EclipseColors.DarkOnSurfaceVariant
            )
        }
    }
}