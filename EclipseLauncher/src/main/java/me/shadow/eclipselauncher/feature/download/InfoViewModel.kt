package me.shadow.eclipselauncher.feature.download

import androidx.lifecycle.ViewModel
import me.shadow.eclipselauncher.feature.download.item.InfoItem
import me.shadow.eclipselauncher.feature.download.platform.AbstractPlatformHelper

class InfoViewModel : ViewModel() {
    var platformHelper: AbstractPlatformHelper? = null
    var infoItem: InfoItem? = null
}