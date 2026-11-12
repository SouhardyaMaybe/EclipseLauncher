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
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
fun JvmArgumentsEditorModal(
    initialArgs: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var argsText by remember { mutableStateOf(initialArgs) }

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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Text(
                        text = "JVM Launch arguments",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkOnSurface
                    )
                    
                    // Text field
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = EclipseColors.DarkSurface,
                        border = androidx.compose.ui.graphics.BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
                    ) {
                        TextField(
                            value = argsText,
                            onValueChange = { argsText = it },
                            placeholder = { Text(text = "", color = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f)) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            singleLine = false,
                            maxLines = 6,
                            colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                                containerColor = Color.Transparent,
                                textColor = EclipseColors.DarkOnSurface,
                                placeholderColor = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f),
                                cursorColor = EclipseColors.DarkPrimary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                    }
                    
                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = EclipseColors.DarkSurfaceVariant,
                                contentColor = EclipseColors.DarkOnSurface
                            )
                        ) {
                            Text(text = "Cancel", fontWeight = FontWeight.Medium)
                        }
                        androidx.compose.foundation.layout.Box(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onSave(argsText); onDismiss() },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = EclipseColors.DarkPrimary,
                                contentColor = EclipseColors.DarkOnPrimary
                            )
                        ) {
                            Text(text = "Done", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}