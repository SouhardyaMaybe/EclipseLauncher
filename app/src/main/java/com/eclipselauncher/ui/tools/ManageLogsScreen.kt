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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun ManageLogsScreen(
    onNavigate: (String) -> Unit
) {
    val logs = remember { mutableStateOf(listOf(
        "2026-11-11.log",
        "2026-11-10.log",
        "2026-11-09.log"
    )) }

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

                // Bare back arrow + title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, top = 8.dp, bottom = 16.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = EclipseColors.DarkPrimary,
                            modifier = Modifier
                                .size(48.dp),
                            onClick = { onNavigate("tools") }
                        )
                        Text(
                            text = "Manage logs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EclipseColors.DarkPrimary
                        )
                    }
                }

                // Count row + Clear all
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${logs.value.size} log files",
                        fontSize = 14.sp,
                        color = EclipseColors.DarkOnSurface
                    )
                    Text(
                        text = "Clear all logs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = EclipseColors.DarkError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Log list
                if (logs.value.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = "No logs yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = EclipseColors.DarkOnSurfaceVariant,
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        logs.value.forEach { logName ->
                            LogRow(
                                logName = logName,
                                onClick = { /* Open log viewer */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogRow(
    logName: String,
    onClick: () -> Unit
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Log file",
                tint = EclipseColors.DarkOnSurfaceVariant,
                modifier = Modifier.size(24.dp).padding(end = 12.dp)
            )
            Text(
                text = logName,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = EclipseColors.DarkOnSurface
            )
        }
    }
}

@Composable
fun LogViewerScreen(
    logName: String,
    onNavigate: (String) -> Unit
) {
    val logContent = remember { mutableStateOf(
        """03:38:04  launcher started
03:38:04  tasks restored
03:38:05  files scan complete
03:41:12  launch preparing 1.21.4
03:41:13  loader vanilla
03:41:14  runtime not found
03:41:14  launch stopped
03:42:02  market search sodium
03:42:03  market 3 results
03:44:55  files deleted minecraft
03:45:10  logs open 2026-11-11.log"""
    ) }

    EclipseLauncherTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EclipseColors.DarkBackground
        ) {
            // Modal overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Scrim
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                )
                
                // Dialog
                Surface(
                    modifier = Modifier
                        .width(340.dp)
                        .height(360.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = EclipseColors.DarkSurface
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title
                        Text(
                            text = logName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EclipseColors.DarkOnSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        // Log content area
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = EclipseColors.DarkSurfaceVariant,
                            border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Text(
                                    text = logContent.value,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        
                        // Close button
                        Text(
                            text = "Close",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = EclipseColors.DarkPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}