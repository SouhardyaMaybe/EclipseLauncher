package com.eclipselauncher.ui.settings.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun DownloadSourcePickerModal(
    currentSource: String,
    onSourceSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sources = listOf("Default", "BMCLAPI")

    EclipseLauncherTheme {
        // Scrim
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
        ) {
            Surface(
                modifier = Modifier
                    .width(340.dp)
                    .padding(vertical = 24.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = EclipseColors.DarkSurface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Title
                    Text(
                        text = "Game download source",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkOnSurface
                    )
                    
                    // Source options
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            sources.forEach { source ->
                                val isSelected = source == currentSource
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    color = if (isSelected) 
                                        EclipseColors.DarkPrimary.copy(alpha = 0.15f) 
                                    else 
                                        EclipseColors.DarkSurface,
                                    onClick = { onSourceSelect(source); onDismiss() }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = source,
                                            fontSize = 14.sp,
                                            color = if (isSelected) EclipseColors.DarkPrimary else EclipseColors.DarkOnSurface
                                        )
                                        if (isSelected) {
                                            androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                                                val center = androidx.compose.ui.geometry.Offset(12.dp.toPx(), 12.dp.toPx())
                                                drawCircle(
                                                    color = EclipseColors.DarkPrimary,
                                                    center = center,
                                                    radius = 6.dp.toPx()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Cancel button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = EclipseColors.DarkSurfaceVariant,
                            contentColor = EclipseColors.DarkOnSurface
                        )
                    ) {
                        Text(text = "Cancel", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}