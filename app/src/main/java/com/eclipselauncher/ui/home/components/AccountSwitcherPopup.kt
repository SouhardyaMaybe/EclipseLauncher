package com.eclipselauncher.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipselauncher.R
import com.eclipselauncher.model.Account
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.MinecraftNametagFontFamily

@Composable
fun AccountSwitcherPopup(
    accounts: List<Account>,
    selectedAccount: Account?,
    onAccountSelect: (Account) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    // Fixed 300 x 340 dp panel with 24 dp corners
    Surface(
        modifier = Modifier
            .width(300.dp)
            .height(340.dp),
        shape = RoundedCornerShape(24.dp),
        color = EclipseColors.DarkSurface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Title
            Text(
                text = "Accounts",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = EclipseColors.DarkOnSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
            )

            // Account list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { account ->
                    AccountSwitcherRow(
                        account = account,
                        isSelected = account.id == selectedAccount?.id,
                        onClick = { onAccountSelect(account) }
                    )
                }
            }

            // Divider
            Divider(
                color = EclipseColors.DarkOutlineVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Add account footer
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = EclipseColors.DarkSurfaceVariant,
                onClick = { onAddAccount(); onDismiss() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add account",
                        tint = EclipseColors.DarkPrimary,
                        modifier = Modifier.size(20.dp).padding(end = 8.dp)
                    )
                    Text(
                        text = "Add account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = EclipseColors.DarkPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSwitcherRow(
    account: Account,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) 
            EclipseColors.DarkPrimary.copy(alpha = 0.15f) 
        else 
            EclipseColors.DarkSurfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio dot
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier.size(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(
                                color = EclipseColors.DarkPrimary,
                                radius = 5.dp.toPx()
                            )
                        }
                    }
                } else {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        drawCircle(
                            color = EclipseColors.DarkOutlineVariant,
                            radius = 10.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // Skin head (36dp)
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.steve),
                    contentDescription = "Skin",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Username and type
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = account.username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EclipseColors.DarkOnSurface,
                    fontFamily = MinecraftNametagFontFamily
                )
                Text(
                    text = when (account.type) {
                        Account.AccountType.LOCAL -> "Local"
                        Account.AccountType.MICROSOFT -> "Microsoft"
                    },
                    fontSize = 12.sp,
                    color = EclipseColors.DarkOnSurfaceVariant,
                    fontFamily = MinecraftNametagFontFamily
                )
            }
        }
    }
}