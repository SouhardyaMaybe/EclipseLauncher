/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.PlayerLookTarget
 */
package me.shadow.eclipselauncher.ui.character

import androidx.compose.ui.geometry.Offset

class PlayerLookTarget {
    @Volatile
    var rootPosition: Offset? = null
}
