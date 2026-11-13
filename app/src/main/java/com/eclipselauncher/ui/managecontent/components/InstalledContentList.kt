package com.eclipselauncher.ui.managecontent.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipselauncher.model.ModInfo
import com.eclipselauncher.ui.theme.EclipseColors

@Composable
fun ColumnScope.InstalledContentList(
    items: List<ModInfo>,
    onItemClick: (ModInfo) -> Unit,
    onRefreshClick: (ModInfo) -> Unit,
    onToggleClick: (ModInfo) -> Unit,
    onDeleteClick: (ModInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            InstalledContentCard(
                item = item,
                onClick = { onItemClick(item) },
                onRefreshClick = { onRefreshClick(item) },
                onToggleClick = { onToggleClick(item) },
                onDeleteClick = { onDeleteClick(item) }
            )
        }
    }
}

@Composable
fun InstalledContentCard(
    item: ModInfo,
    onClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onToggleClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = EclipseColors.DarkSurface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon well (56dp)
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = EclipseColors.DarkSurfaceVariant,
                    border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Placeholder for mod icon
                        Text(
                            text = item.name.first().uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EclipseColors.DarkPrimary
                        )
                    }
                }
            }

            // Name and version line
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EclipseColors.DarkOnSurface
                )
                Text(
                    text = "v${item.name} · Mods", // Simplified
                    fontSize = 12.sp,
                    color = EclipseColors.DarkOnSurfaceVariant
                )
            }

            // Trailing actions: refresh, toggle, delete
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = EclipseColors.DarkOnSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(8.dp)
                        .clickable { onRefreshClick() }
                )
                
                // Toggle (placeholder - shows as enabled)
                Surface(
                    modifier = Modifier
                        .width(44.dp)
                        .height(24.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = EclipseColors.DarkPrimary,
                    onClick = onToggleClick
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(start = 4.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            drawCircle(
                                color = EclipseColors.DarkOnPrimary,
                                radius = 8.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(18.dp.toPx(), 10.dp.toPx())
                            )
                        }
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = EclipseColors.DarkError,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(8.dp)
                        .clickable { onDeleteClick() }
                )
            }
        }
    }
}