package com.eclipselauncher.ui.tools

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
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.model.TaskInfo
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun TasksScreen(
    onNavigate: (String) -> Unit
) {
    val tasks = remember { mutableStateOf(listOf(
        TaskInfo("1", "Launch", "Vanilla 1.21.4", 0.42f, TaskInfo.TaskStatus.RUNNING, true),
        TaskInfo("2", "Import modpack", "Fabulous Optimized.zip", 0.85f, TaskInfo.TaskStatus.RUNNING, false),
        TaskInfo("3", "Download assets", "312 files", 0.15f, TaskInfo.TaskStatus.RUNNING, false)
    )) }

    var showTasks by remember { mutableStateOf(true) }

    // If showing tasks modal
    if (showTasks) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            
            // Tasks dialog
            Surface(
                modifier = Modifier
                    .width(340.dp)
                    .height(380.dp),
                shape = RoundedCornerShape(24.dp),
                color = EclipseColors.DarkSurface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Text(
                        text = "Tasks",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkOnSurface
                    )
                    
                    // Task list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        tasks.value.forEach { task ->
                            TaskRow(
                                task = task,
                                onTogglePause = { /* Toggle pause */ },
                                onCancel = { /* Cancel task */ },
                                onExpand = { /* Toggle expand */ }
                            )
                        }
                    }
                    
                    // Minimize button
                    Text(
                        text = "Minimize",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = EclipseColors.DarkPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        // Minimized state - just a floating button or indicator
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .size(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = EclipseColors.DarkPrimary,
                shadowElevation = 8.dp,
                onClick = { showTasks = true }
            ) {
                Icon(
                    imageVector = Icons.Default.ListAlt,
                    contentDescription = "Show tasks",
                    tint = EclipseColors.DarkOnPrimary,
                    modifier = Modifier.size(24.dp).padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun TaskRow(
    task: TaskInfo,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit,
    onExpand: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = EclipseColors.DarkSurfaceVariant,
        border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status dot
                Box(
                    modifier = Modifier.size(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(
                            color = when (task.status) {
                                TaskInfo.TaskStatus.RUNNING -> EclipseColors.DarkPrimary
                                TaskInfo.TaskStatus.PAUSED -> Color(0xFFFFD600)
                                TaskInfo.TaskStatus.COMPLETED -> Color(0xFF4CAF50)
                                TaskInfo.TaskStatus.FAILED -> EclipseColors.DarkError
                                TaskInfo.TaskStatus.CANCELLED -> EclipseColors.DarkOnSurfaceVariant
                            },
                            radius = 5.dp.toPx()
                        )
                    }
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = EclipseColors.DarkOnSurface
                    )
                    Text(
                        text = task.detail,
                        fontSize = 12.sp,
                        color = EclipseColors.DarkOnSurfaceVariant
                    )
                }
                
                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause/Play
                    Icon(
                        imageVector = if (task.status == TaskInfo.TaskStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (task.status == TaskInfo.TaskStatus.RUNNING) "Pause" else "Resume",
                        tint = EclipseColors.DarkOnSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(8.dp)
                            .clickable { onTogglePause() }
                    )
                    
                    // Cancel
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = EclipseColors.DarkError,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(8.dp)
                            .clickable { onCancel() }
                    )
                    
                    // Expand
                    Icon(
                        imageVector = if (task.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (task.isExpanded) "Collapse" else "Expand",
                        tint = EclipseColors.DarkOnSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(8.dp)
                            .clickable { onExpand() }
                    )
                }
            }
            
            // Progress bar
            if (task.progress >= 0) {
                LinearProgressIndicator(
                    progress = task.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = EclipseColors.DarkPrimary,
                    trackColor = EclipseColors.DarkOutlineVariant
                )
            }
            
            // Expanded details
            if (task.isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(label = "Status", value = task.status.name)
                    DetailRow(label = "Progress", value = "${(task.progress * 100).roundToInt()}%")
                    DetailRow(label = "Started", value = "03:41:12") // Would be dynamic
                    // Error line would go here if failed
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = EclipseColors.DarkOnSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = EclipseColors.DarkOnSurface
        )
    }
}