package com.eclipselauncher.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipselauncher.R
import com.eclipselauncher.model.Account
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.MinecraftNametagFontFamily

@Composable
fun AccountPanel(
    account: Account?,
    onAccountClick: () -> Unit,
    onSwitcherClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Skin head - 132dp, pixel-sharp front face only
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(0.dp)) // No rounding for pixel-sharp
                .background(EclipseColors.DarkSurfaceVariant)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // In real implementation, load skin from account.skinPath
            // For now, show steve.png placeholder
            Image(
                painter = painterResource(id = R.drawable.steve),
                contentDescription = "Account skin",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(132.dp)
            )
        }

        // Nametag plate
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(14.dp),
            color = EclipseColors.DarkSurfaceVariant.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant),
            onClick = onAccountClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = account?.username ?: "No Account",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkOnSurface,
                        fontFamily = MinecraftNametagFontFamily
                    )
                    Text(
                        text = when (account?.type) {
                            Account.AccountType.LOCAL -> "Local"
                            Account.AccountType.MICROSOFT -> "Microsoft"
                            else -> ""
                        },
                        fontSize = 12.sp,
                        color = EclipseColors.DarkOnSurfaceVariant,
                        fontFamily = MinecraftNametagFontFamily
                    )
                }
                
                // Caret icon
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Switch account",
                    tint = EclipseColors.DarkPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}