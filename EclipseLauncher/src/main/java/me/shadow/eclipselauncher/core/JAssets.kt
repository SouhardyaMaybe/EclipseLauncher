package me.shadow.eclipselauncher.core

import androidx.annotation.Keep

import me.shadow.eclipselauncher.json.annotations.SerializedName

@Keep
class JAssets {
    /* Used by older versions of mc, when the files were named and under .minecraft/resources  */
    @SerializedName("map_to_resources")
    @JvmField
    var mapToResources: Boolean = false

    @JvmField
    var objects: MutableMap<String, JAssetInfo>? = null

    /* Used by the legacy.json (~1.6.X) asset file, used for paths at the root of the .minecraft/assets folder */
    @JvmField
    var virtual: Boolean = false
}
