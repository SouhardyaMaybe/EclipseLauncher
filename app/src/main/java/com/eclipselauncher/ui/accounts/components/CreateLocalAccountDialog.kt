package com.eclipselauncher.ui.accounts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun CreateLocalAccountDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    val isValid = username.length >= 3 && username.length <= 16 && username.all { it.isLetterOrDigit() || it == '_' }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        shape = RoundedCornerShape(24.dp),
        color = EclipseColors.DarkSurface
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Local account",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = EclipseColors.DarkOnSurface
            )

            // Username field with floating label
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = EclipseColors.DarkSurfaceVariant,
                border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
            ) {
                TextField(
                    value = username,
                    onValueChange = { 
                        if (it.length <= 16) username = it 
                    },
                    placeholder = { Text(text = "Username", color = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedTextColor = EclipseColors.DarkOnSurface,
                        unfocusedTextColor = EclipseColors.DarkOnSurface,
                        focusedPlaceholderColor = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f),
                        cursorColor = EclipseColors.DarkPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }

            // Error message if invalid
            if (username.isNotBlank() && !isValid) {
                Text(
                    text = "3-16 characters, letters/numbers/underscore only",
                    fontSize = 12.sp,
                    color = EclipseColors.DarkError
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
                Box(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { if (isValid) onCreate(username) },
                    enabled = isValid,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (isValid) EclipseColors.DarkPrimary else EclipseColors.DarkSurfaceVariant,
                        contentColor = if (isValid) EclipseColors.DarkOnPrimary else EclipseColors.DarkOnSurfaceVariant
                    )
                ) {
                    Text(text = "Create", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}