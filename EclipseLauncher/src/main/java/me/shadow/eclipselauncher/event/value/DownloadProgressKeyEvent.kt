package me.shadow.eclipselauncher.event.value

/**
 * When a new download task starts, use this task key to notify LauncherActivity
 * Making it easy to observe the download progress of this task
 * @param observe whether to keep observing
 * @see me.shadow.eclipselauncher.pojav.LauncherActivity
 */
class DownloadProgressKeyEvent(val progressKey: String, val observe: Boolean)