/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.shadow.eclipselauncher.data

import dev.shadow.eclipselauncher.R

/**
 * The six content families shown by Manage Content. The directory is relative
 * to the selected instance, except Worlds, which is the conventional saves
 * directory.
 */
enum class ContentType(val labelRes: Int, val directory: String) {
    MODS(R.string.content_mods, "mods"),
    RESOURCE_PACKS(R.string.content_resource_packs, "resourcepacks"),
    DATA_PACKS(R.string.content_data_packs, "datapacks"),
    SHADERS(R.string.content_shaders, "shaderpacks"),
    WORLDS(R.string.content_worlds, "saves"),
    MODPACKS(R.string.content_modpacks, "modpacks"),
}
