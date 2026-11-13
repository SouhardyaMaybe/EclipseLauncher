package com.eclipselauncher.ui.accounts

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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.model.Account
import com.eclipselauncher.ui.accounts.components.CreateLocalAccountDialog
import com.eclipselauncher.ui.accounts.components.DeleteAccountDialog
import com.eclipselauncher.ui.accounts.components.MicrosoftSignInScreen
import com.eclipselauncher.ui.accounts.components.SkinAndCapeScreen
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val signInState by viewModel.signInState.collectAsState()
    val showCreateLocalDialog by viewModel.showCreateLocalDialog.collectAsState()
    val showSkinAndCape by viewModel.showSkinAndCape.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    EclipseLauncherTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EclipseColors.DarkBackground
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Accounts screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Status bar spacer
                    androidx.compose.foundation.layout.Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                    )

                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = EclipseColors.DarkOnSurface,
                            modifier = Modifier
                                .size(48.dp)
                                .fillMaxWidth()
                                .clickable { onNavigate("home") }
                        )
                        Text(
                            text = "Accounts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EclipseColors.DarkOnSurface
                        )
                        androidx.compose.foundation.layout.Box(modifier = Modifier.size(48.dp))
                    }

                    // Pinned add account buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Microsoft sign in
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = EclipseColors.DarkSurface,
                            border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant),
                            onClick = { viewModel.startMicrosoftSignIn() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Microsoft",
                                    tint = EclipseColors.DarkPrimary,
                                    modifier = Modifier.size(24.dp).padding(end = 12.dp)
                                )
                                Text(
                                    text = "Sign in with Microsoft",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EclipseColors.DarkOnSurface
                                )
                            }
                        }

                        // Create local account
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = EclipseColors.DarkPrimary,
                            onClick = { viewModel.showCreateLocalAccountDialog() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = EclipseColors.DarkOnPrimary,
                                    modifier = Modifier.size(20.dp).padding(end = 8.dp)
                                )
                                Text(
                                    text = "Create local account",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EclipseColors.DarkOnPrimary
                                )
                            }
                        }

                        // Account list
                        if (accounts.isEmpty()) {
                            Text(
                                text = "No account Here",
                                fontSize = 16.sp,
                                color = EclipseColors.DarkOnSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 56.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                accounts.forEach { account ->
                                    AccountRow(
                                        account = account,
                                        onClick = { viewModel.selectAccount(account) },
                                        onEditSkin = { viewModel.showSkinAndCape() },
                                        onDelete = { viewModel.showDeleteConfirmation(account) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Microsoft Sign-in screen (overlay)
                if (signInState != AccountsViewModel.SignInState.Idle) {
                    MicrosoftSignInScreen(
                        viewModel = viewModel,
                        onNavigate = { route -> 
                            if (route == "accounts") {
                                // Stay on accounts, sign-in state handled by ViewModel
                            } else {
                                onNavigate(route)
                            }
                        }
                    )
                }

                // Skin and Cape screen (overlay)
                if (showSkinAndCape) {
                    SkinAndCapeScreen(
                        onNavigate = { route ->
                            if (route == "accounts") {
                                viewModel.dismissSkinAndCape()
                            } else {
                                onNavigate(route)
                            }
                        }
                    )
                }

                // Create Local Account dialog
                if (showCreateLocalDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        CreateLocalAccountDialog(
                            onDismiss = { viewModel.dismissCreateLocalDialog() },
                            onCreate = { username ->
                                viewModel.createLocalAccount(username)
                            }
                        )
                    }
                }

                // Delete Account dialog
                if (showDeleteDialog != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        DeleteAccountDialog(
                            account = showDeleteDialog!!,
                            onDismiss = { viewModel.dismissDeleteDialog() },
                            onDelete = { viewModel.confirmDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountRow(
    account: Account,
    onClick: () -> Unit,
    onEditSkin: () -> Unit,
    onDelete: () -> Unit
) {
    val isSelected = account.isSelected

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Account avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EclipseColors.DarkPrimary.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = EclipseColors.DarkPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }

            // Account info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = account.username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EclipseColors.DarkOnSurface
                )
                if (account.type == Account.AccountType.MICROSOFT) {
                    Text(
                        text = "Microsoft Account",
                        fontSize = 12.sp,
                        color = EclipseColors.DarkOnSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Local Account",
                        fontSize = 12.sp,
                        color = EclipseColors.DarkOnSurfaceVariant
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit skin/cape",
                    tint = EclipseColors.DarkOnSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .fillMaxWidth()
                        .clickable { onEditSkin() }
                )
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete account",
                    tint = EclipseColors.DarkError,
                    modifier = Modifier
                        .size(24.dp)
                        .fillMaxWidth()
                        .clickable { onDelete() }
                )
            }
        }
    }
}