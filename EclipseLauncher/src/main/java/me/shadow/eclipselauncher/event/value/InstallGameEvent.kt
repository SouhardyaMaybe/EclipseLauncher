package me.shadow.eclipselauncher.event.value

import me.shadow.eclipselauncher.feature.version.install.Addon
import me.shadow.eclipselauncher.feature.version.install.InstallTaskItem

/**
 * This event is used to notify when an install task starts
 * @see me.shadow.eclipselauncher.ui.fragment.InstallGameFragment
 * @param minecraftVersion the vanilla MC version
 * @param customVersionName the custom version folder name
 * @param taskMap the install tasks
 */
class InstallGameEvent(
    val minecraftVersion: String,
    val customVersionName: String,
    val taskMap: Map<Addon, InstallTaskItem>
)