package com.eclipselauncher.ui.accounts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.ui.accounts.AccountsViewModel
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun MicrosoftSignInScreen(
    viewModel: AccountsViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val signInState by viewModel.signInState.collectAsStateWithLifecycle()

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
                            .fillMaxWidth(),
                        onClick = { onNavigate("accounts") }
                    )
                    Text(
                        text = "Microsoft sign in",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkOnSurface
                    )
                    Box(modifier = Modifier.size(48.dp))
                }

                // Content area - WebView placeholder for Microsoft sign-in page
                when (signInState) {
                    AccountsViewModel.SignInState.Loading -> {
                        // Show embedded page frame
                        MicrosoftSignInPage()
                    }
                    AccountsViewModel.SignInState.Progress -> {
                        SignInProgress()
                    }
                    AccountsViewModel.SignInState.Failure -> {
                        SignInFailure(onRetry = { viewModel.retrySignIn() })
                    }
                }
            }
        }
    }
}

@Composable
fun MicrosoftSignInPage() {
    // This would embed a WebView in a real implementation
    // For now, show the page frame with Microsoft branding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Microsoft logo placeholder
            Surface(
                modifier = Modifier.size(80.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = EclipseColors.DarkSurfaceVariant,
                border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0078D4) // Microsoft blue
                    )
                }
            }
            
            Text(
                text = "Microsoft sign-in page would load here",
                fontSize = 14.sp,
                color = EclipseColors.DarkOnSurfaceVariant
            )
            
            Text(
                text = "(WebView implementation required)",
                fontSize = 12.sp,
                color = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SignInProgress() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                color = EclipseColors.DarkPrimary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Signing in with Microsoft",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = EclipseColors.DarkOnSurface
            )
        }
    }
}

@Composable
fun SignInFailure(
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = EclipseColors.DarkSurface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sign-in failed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EclipseColors.DarkError
                )
                
                Text(
                    text = "Network error during sign-in",
                    fontSize = 14.sp,
                    color = EclipseColors.DarkOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = onRetry,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = EclipseColors.DarkPrimary,
                        contentColor = EclipseColors.DarkOnPrimary
                    )
                ) {
                    Text(
                        text = "Try again",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}