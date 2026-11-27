/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Ported exactly from dev.obsidianreborn.android.ui.launcher.PoseTransform
 */
package me.shadow.eclipselauncher.ui.character

data class PoseTransform(
    val rx: Float = 0f,
    val ry: Float = 0f,
    val rz: Float = 0f,
    val tx: Float = 0f,
    val ty: Float = 0f,
    val tz: Float = 0f
)
