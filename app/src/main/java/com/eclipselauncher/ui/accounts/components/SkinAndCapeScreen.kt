package com.eclipselauncher.ui.accounts.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipselauncher.R
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun SkinAndCapeScreen(
    onNavigate: (String) -> Unit
) {
    EclipseLauncherTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EclipseColors.DarkBackground
        ) {
            // Modal dialog - 340dp wide, nearly full phone width
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(340.dp)
                        .padding(vertical = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = EclipseColors.DarkSurface
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Skin and cape",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = EclipseColors.DarkOnSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = EclipseColors.DarkOnSurface,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { onNavigate("accounts") }
                            )
                        }

                        // 3D Model stage - 240dp
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = EclipseColors.DarkSurfaceVariant,
                            border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // 3D model viewer placeholder
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Simple Steve model representation
                                    Surface(
                                        modifier = Modifier.size(120.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = EclipseColors.DarkSurface,
                                        border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.Image(
                                                painter = painterResource(id = R.drawable.steve),
                                                contentDescription = "Skin preview",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.size(100.dp)
                                            )
                                        }
                                    }
                                    
                                    Text(
                                        text = "Drag to rotate • Pinch to zoom",
                                        fontSize = 12.sp,
                                        color = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Actions: Pick skin / Clear skin, Pick cape / Clear cape
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Pick skin
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = EclipseColors.DarkPrimary,
                                    onClick = { /* Pick skin */ }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = "Pick skin",
                                            tint = EclipseColors.DarkOnPrimary,
                                            modifier = Modifier.size(20.dp).padding(end = 8.dp)
                                        )
                                        Text(
                                            text = "Pick skin",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EclipseColors.DarkOnPrimary
                                        )
                                    }
                                }

                                // Clear skin
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = EclipseColors.DarkError.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, EclipseColors.DarkError),
                                    onClick = { /* Clear skin */ }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear skin",
                                            tint = EclipseColors.DarkError,
                                            modifier = Modifier.size(20.dp).padding(end = 8.dp)
                                        )
                                        Text(
                                            text = "Clear skin",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EclipseColors.DarkError
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Pick cape
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = EclipseColors.DarkPrimary,
                                    onClick = { /* Pick cape */ }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "Pick cape",
                                            tint = EclipseColors.DarkOnPrimary,
                                            modifier = Modifier.size(20.dp).padding(end = 8.dp)
                                        )
                                        Text(
                                            text = "Pick cape",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EclipseColors.DarkOnPrimary
                                        )
                                    }
                                }

                                // Clear cape
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = EclipseColors.DarkError.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, EclipseColors.DarkError),
                                    onClick = { /* Clear cape */ }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear cape",
                                            tint = EclipseColors.DarkError,
                                            modifier = Modifier.size(20.dp).padding(end = 8.dp)
                                        )
                                        Text(
                                            text = "Clear cape",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EclipseColors.DarkError
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}