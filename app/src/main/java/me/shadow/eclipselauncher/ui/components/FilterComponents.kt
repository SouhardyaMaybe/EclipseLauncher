/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * Shared filter components used by ManageContent and ModpackBrowser screens.
 */
package me.shadow.eclipselauncher.ui.components

/** One selectable row inside an expanded filter panel. */
data class FilterOption(
    val value: String?,
    val label: String,
    val enabled: Boolean = true,
    val detail: String? = null,
)

/** The filter categories shown as compact headers. */
enum class ContentFilter { LOADER, VERSION, PLATFORM }

/** The filter categories shown as compact headers (modpack browser). */
enum class ModpackFilter { LOADER, VERSION, PLATFORM }
