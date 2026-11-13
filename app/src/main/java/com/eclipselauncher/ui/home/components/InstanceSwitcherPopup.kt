package com.eclipselauncher.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipselauncher.R
import com.eclipselauncher.model.Instance
import com.eclipselauncher.ui.theme.EclipseColors

@Composable
fun InstanceSwitcherPopup(
    instances: List<Instance>,
    selectedInstance: Instance?,
    onInstanceSelect: (Instance) -> Unit,
    onCreateInstance: () -> Unit,
    onDismiss: () -> Unit
) {
    // Anchored popup - 320dp wide, 170dp tall, no scrim
    Surface(
        modifier = Modifier
            .width(320.dp)
            .height(170.dp),
        shape = RoundedCornerShape(16.dp),
        color = EclipseColors.DarkSurface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Instance list (scrollable, snaps to selected)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                instances.forEach { instance ->
                    InstanceSwitcherRow(
                        instance = instance,
                        isSelected = instance.id == selectedInstance?.id,
                        onClick = { onInstanceSelect(instance); onDismiss() }
                    )
                }
            }

            // Divider
            Divider(
                color = EclipseColors.DarkOutlineVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Create instance footer
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = EclipseColors.DarkSurfaceVariant,
                onClick = { onCreateInstance(); onDismiss() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create instance",
                        tint = EclipseColors.DarkPrimary,
                        modifier = Modifier.size(20.dp).padding(end = 8.dp)
                    )
                    Text(
                        text = "Create instance",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = EclipseColors.DarkPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun InstanceSwitcherRow(
    instance: Instance,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) 
            EclipseColors.DarkPrimary.copy(alpha = 0.15f) 
        else 
            EclipseColors.DarkSurfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Instance icon (30dp)
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = instance.name.first().uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EclipseColors.DarkPrimary
                )
            }

            // Name and version/loader
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = instance.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = EclipseColors.DarkOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${instance.version} · ${instance.loader.name}",
                    fontSize = 11.sp,
                    color = EclipseColors.DarkOnSurfaceVariant
                )
            }

            // Check mark on selected
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = EclipseColors.DarkPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}