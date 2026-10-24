/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import dev.shadow.eclipselauncher.R
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavKey

@Serializable
data object Instances : NavKey

@Serializable
data object Discover : NavKey

@Serializable
data object Downloads : NavKey

@Serializable
data object Settings : NavKey

/**
 * The five top-level destinations. They are data objects rather than strings
 * so Navigation 3 stores a typed back stack and a rename cannot silently break
 * a saved state.
 */
enum class TopDestination(
    val key: NavKey,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(Home, R.string.nav_home, Icons.Outlined.Home),
    INSTANCES(Instances, R.string.nav_instances, Icons.Outlined.List),
    DISCOVER(Discover, R.string.nav_discover, Icons.Outlined.Search),
    DOWNLOADS(Downloads, R.string.nav_downloads, Icons.Outlined.KeyboardArrowDown),
    SETTINGS(Settings, R.string.nav_settings, Icons.Outlined.Settings),
}
