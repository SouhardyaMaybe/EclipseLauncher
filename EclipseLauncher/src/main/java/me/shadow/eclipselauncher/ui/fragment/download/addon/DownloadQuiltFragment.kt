package me.shadow.eclipselauncher.ui.fragment.download.addon

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.mod.modloader.FabricLikeUtils

class DownloadQuiltFragment : DownloadFabricLikeFragment(FabricLikeUtils.QUILT_UTILS, R.drawable.ic_quilt) {
    companion object {
        const val TAG: String = "DownloadQuiltFragment"
    }
}