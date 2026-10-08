package me.shadow.eclipselauncher.event.sticky

import me.shadow.eclipselauncher.feature.version.install.Addon
import me.shadow.eclipselauncher.feature.version.install.InstallTask

/**
 * This event is used to notify after an install task is selected
 * @param addon whose install task was selected
 * @param selectedVersion the selected version
 * @param task the selected task
 * @see me.shadow.eclipselauncher.feature.version.install.Addon
 */
class SelectInstallTaskEvent(val addon: Addon, val selectedVersion: String, val task: InstallTask)