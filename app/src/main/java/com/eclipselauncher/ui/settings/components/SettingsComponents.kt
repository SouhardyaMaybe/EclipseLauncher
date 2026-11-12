package com.eclipselauncher.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipselauncher.ui.theme.EclipseColors

@Composable
fun SettingsRow(
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
            if (icon.isNotBlank()) {
                Text(
                    text = icon,
                    fontSize = 24.sp,
                    color = EclipseColors.DarkPrimary
                )
            } else {
                androidx.compose.foundation.layout.Box(modifier = Modifier.width(24.dp))
            }
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
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = EclipseColors.DarkOnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
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
            if (icon.isNotBlank()) {
                Text(
                    text = icon,
                    fontSize = 24.sp,
                    color = EclipseColors.DarkPrimary
                )
            } else {
                androidx.compose.foundation.layout.Box(modifier = Modifier.width(24.dp))
            }
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
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = EclipseColors.DarkOnSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    thumbColor = if (checked) EclipseColors.DarkPrimary else EclipseColors.DarkOutlineVariant,
                    trackColor = if (checked) EclipseColors.DarkPrimary.copy(alpha = 0.3f) else EclipseColors.DarkSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun SettingsSliderRow(
    icon: String,
    title: String,
    subtitle: String,
    value: Float,
    range: androidx.compose.ui.unit.FloatRange,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = EclipseColors.DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon.isNotBlank()) {
                    Text(
                        text = icon,
                        fontSize = 24.sp,
                        color = EclipseColors.DarkPrimary
                    )
                } else {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.width(24.dp))
                }
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
            
            // Slider
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    range = range,
                    steps = steps,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        activeTrackColor = EclipseColors.DarkPrimary,
                        inactiveTrackColor = EclipseColors.DarkOutlineVariant,
                        thumbColor = EclipseColors.DarkPrimary
                    )
                )
            }
        }
    }
}