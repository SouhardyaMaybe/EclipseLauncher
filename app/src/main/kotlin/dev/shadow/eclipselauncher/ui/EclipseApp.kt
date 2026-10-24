/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.shadow.eclipselauncher.designsystem.component.EclipseArc
import dev.shadow.eclipselauncher.designsystem.theme.EclipseTheme
import dev.shadow.eclipselauncher.ui.navigation.Discover
import dev.shadow.eclipselauncher.ui.navigation.Downloads
import dev.shadow.eclipselauncher.ui.navigation.Home
import dev.shadow.eclipselauncher.ui.navigation.Instances
import dev.shadow.eclipselauncher.ui.navigation.Settings
import dev.shadow.eclipselauncher.ui.navigation.TopDestination
import dev.shadow.eclipselauncher.ui.screens.DiscoverScreen
import dev.shadow.eclipselauncher.ui.screens.DownloadsScreen
import dev.shadow.eclipselauncher.ui.screens.HomeScreen
import dev.shadow.eclipselauncher.ui.screens.InstancesScreen
import dev.shadow.eclipselauncher.ui.screens.SettingsScreen

@Composable
fun EclipseApp() {
    var dynamicColor by rememberSaveable { mutableStateOf(false) }

    EclipseTheme(dynamicColor = dynamicColor) {
        val backStack = rememberNavBackStack(Home)
        val currentKey = backStack.lastOrNull()

        EclipseScaffold(
            currentKey = currentKey,
            onNavigate = { destination -> backStack.showTopLevel(destination.key) },
        ) {
            NavDisplay(
                modifier = Modifier.fillMaxSize(),
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<Home> {
                        HomeScreen(
                            onSetUpInstance = { backStack.showTopLevel(Instances) },
                        )
                    }
                    entry<Instances> { InstancesScreen() }
                    entry<Discover> { DiscoverScreen() }
                    entry<Downloads> { DownloadsScreen() }
                    entry<Settings> {
                        SettingsScreen(
                            dynamicColor = dynamicColor,
                            onDynamicColorChange = { dynamicColor = it },
                        )
                    }
                },
            )
        }
    }
}

/**
 * The shell is a navigation rail on wide windows and a navigation bar on
 * phones. It is the only place that knows the five top-level destinations.
 */
@Composable
private fun EclipseScaffold(
    currentKey: NavKey?,
    onNavigate: (TopDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (expanded) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Spacer(Modifier.height(24.dp))
                    EclipseArc(modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(24.dp))
                    TopDestination.entries.forEach { destination ->
                        NavigationRailItem(
                            selected = currentKey == destination.key,
                            onClick = { onNavigate(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
                Spacer(Modifier.width(1.dp))
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (!expanded) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            TopDestination.entries.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentKey == destination.key,
                                    onClick = { onNavigate(destination) },
                                    icon = { Icon(destination.icon, contentDescription = null) },
                                    label = { Text(stringResource(destination.labelRes)) },
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Selecting a top-level destination starts a fresh stack. Detail screens are
 * pushed on top of the current destination; replacing the stack here keeps the
 * back button from walking through tabs the user did not visit.
 */
private fun NavBackStack<NavKey>.showTopLevel(key: NavKey) {
    if (lastOrNull() == key) return
    clear()
    add(key)
}
