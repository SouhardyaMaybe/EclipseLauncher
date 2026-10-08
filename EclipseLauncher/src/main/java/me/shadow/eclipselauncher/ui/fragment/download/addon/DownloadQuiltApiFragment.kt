package me.shadow.eclipselauncher.ui.fragment.download.addon

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.version.install.Addon

class DownloadQuiltApiFragment: DownloadFabricLikeApiModFragment(
    Addon.QSL,
    "qvIfYCYJ",
    "https://modrinth.com/mod/qsl",
    R.drawable.ic_quilt
) {
    companion object {
        const val TAG: String = "DownloadQuiltApiFragment"
    }
}