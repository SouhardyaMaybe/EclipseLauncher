package com.eclipselauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.model.Account
import com.eclipselauncher.model.Instance
import com.eclipselauncher.ui.home.components.AccountPanel
import com.eclipselauncher.ui.home.components.AccountSwitcherPopup
import com.eclipselauncher.ui.home.components.HeaderBar
import com.eclipselauncher.ui.home.components.InstancePanel
import com.eclipselauncher.ui.home.components.InstanceSwitcherPopup
import com.eclipselauncher.ui.home.components.PlayBar
import com.eclipselauncher.ui.home.components.PlayBlockedDialog
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val instances by viewModel.instances.collectAsStateWithLifecycle()
    val selectedInstance by viewModel.selectedInstance.collectAsStateWithLifecycle()
    val showAccountSwitcher by viewModel.showAccountSwitcher.collectAsStateWithLifecycle()
    val showInstanceSwitcher by viewModel.showInstanceSwitcher.collectAsStateWithLifecycle()
    val showPlayBlockedDialog by viewModel.showPlayBlockedDialog.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var accountSwitcherAnchor, setAccountSwitcherAnchor = mutableStateOf<LayoutCoordinates?>(null)
    var instanceSwitcherAnchor, setInstanceSwitcherAnchor = mutableStateOf<LayoutCoordinates?>(null)

    EclipseLauncherTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EclipseColors.DarkBackground
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main content
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status bar spacer (24dp)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                    )

                    // Header bar
                    HeaderBar(
                        onFolderClick = { onNavigate("tools/manage_content") },
                        onDocumentClick = { /* Document action */ },
                        onTaskClick = { onNavigate("tools") },
                        onSettingsClick = { onNavigate("settings") }
                    )

                    // Center content - Account panel
                    AccountPanel(
                        account = selectedAccount,
                        onAccountClick = { viewModel.toggleAccountSwitcher() },
                        onSwitcherClick = { viewModel.toggleAccountSwitcher() }
                    )

                    // Spacer
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                    )

                    // Bottom stack - Instance panel + Play bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Instance panel with switcher anchor
                        InstancePanel(
                            instance = selectedInstance,
                            onInstanceClick = { viewModel.toggleInstanceSwitcher() },
                            onEditClick = { 
                                if (selectedInstance != null) {
                                    onNavigate("create_instance/editor/${selectedInstance.id}")
                                }
                            },
                            onSwitcherClick = { viewModel.toggleInstanceSwitcher() }
                        )

                        // Play bar
                        PlayBar(
                            instance = selectedInstance,
                            onPlayClick = { viewModel.onPlayClick() }
                        )
                    }
                }

                // Account switcher popup (anchored to nametag)
                if (showAccountSwitcher) {
                    AccountSwitcherPopup(
                        accounts = accounts,
                        selectedAccount = selectedAccount,
                        onAccountSelect = { account ->
                            viewModel.selectAccount(account)
                        },
                        onAddAccount = { onNavigate("accounts") },
                        onDismiss = { viewModel.showAccountSwitcher.value = false }
                    )
                }

                // Instance switcher popup (anchored to instance panel)
                if (showInstanceSwitcher) {
                    InstanceSwitcherPopup(
                        instances = instances,
                        selectedInstance = selectedInstance,
                        onInstanceSelect = { instance ->
                            viewModel.selectInstance(instance)
                        },
                        onCreateInstance = { onNavigate("create_instance") },
                        onDismiss = { viewModel.showInstanceSwitcher.value = false }
                    )
                }

                // Play blocked dialog
                if (showPlayBlockedDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        PlayBlockedDialog(
                            onDismiss = { viewModel.dismissPlayBlockedDialog() },
                            onConnectAccount = { onNavigate("accounts") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    EclipseLauncherTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EclipseColors.DarkBackground
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = EclipseColors.DarkPrimary
                )
            }
        }
    }
}