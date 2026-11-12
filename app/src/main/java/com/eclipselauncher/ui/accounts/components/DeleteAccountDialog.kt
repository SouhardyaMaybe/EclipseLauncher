package com.eclipselauncher.ui.accounts.components

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
import com.eclipselauncher.model.Account
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun DeleteAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = EclipseColors.DarkSurface
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Delete this account?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = EclipseColors.DarkOnSurface
            )

            Text(
                text = "\"${account.username}\" will be removed from the launcher with its skin settings.",
                fontSize = 14.sp,
                color = EclipseColors.DarkOnSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

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
                    onClick = onDelete,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = EclipseColors.DarkError,
                        contentColor = EclipseColors.DarkOnError
                    )
                ) {
                    Text(text = "Delete", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}