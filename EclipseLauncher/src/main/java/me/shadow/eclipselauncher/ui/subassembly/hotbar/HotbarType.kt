package me.shadow.eclipselauncher.ui.subassembly.hotbar

import me.shadow.eclipselauncher.R

/**
 * Hotbar detection types
 * @param nameId the localized name resource id of the type
 * @param valueName the stored setting value of the type
 */
enum class HotbarType(val nameId: Int, val valueName: String) {
    /**
     * Adaptive: the width and height of the detection box are calculated automatically from the screen resolution and GUI scale (may be imprecise)
     */
    AUTO(R.string.option_hotbar_type_auto, "auto"),

    /**
     * Manual: let the user adjust the width and height of the detection box themselves
     */
    MANUALLY(R.string.option_hotbar_type_manually, "manually")
}