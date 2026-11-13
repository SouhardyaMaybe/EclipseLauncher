package com.eclipselauncher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

@Composable
fun PlayBlockedDialog(
    onDismiss: () -> Unit,
    onConnectAccount: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        shape = RoundedCornerShape(16.dp),
        color = EclipseColors.DarkSurface,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Message - 3 lines
            Text(
                text = "An account is compulsory to\nperform that action. Press the\nfollowing button to continue.",
                fontSize = 16.sp,
                color = EclipseColors.DarkOnSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 24.sp
            )

            // Connect account button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onConnectAccount(); onDismiss() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = EclipseColors.DarkPrimary,
                        contentColor = EclipseColors.DarkOnPrimary
                    )
                ) {
                    Text(
                        text = "Connect an account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}