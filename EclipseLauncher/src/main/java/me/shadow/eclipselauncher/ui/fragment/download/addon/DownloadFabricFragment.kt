package me.shadow.eclipselauncher.ui.fragment.download.addon

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.mod.modloader.FabricLikeUtils

class DownloadFabricFragment : DownloadFabricLikeFragment(FabricLikeUtils.FABRIC_UTILS, R.drawable.ic_fabric) {
    companion object {
        const val TAG: String = "DownloadFabricFragment"
    }
}