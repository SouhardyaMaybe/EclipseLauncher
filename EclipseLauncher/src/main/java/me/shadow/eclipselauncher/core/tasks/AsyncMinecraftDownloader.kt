package me.shadow.eclipselauncher.core.tasks

import me.shadow.eclipselauncher.core.JMinecraftVersionList
import me.shadow.eclipselauncher.event.sticky.MinecraftVersionValueEvent
import org.greenrobot.eventbus.EventBus

/**
 * Looks up Minecraft versions inside the version list published as the sticky
 * [MinecraftVersionValueEvent].
 *
 * The one-shot completion callbacks of the old async download entry point were replaced by the
 * suspending [MinecraftDownloader.start], which reports failures by throwing.
 */
object AsyncMinecraftDownloader {

    @JvmStatic
    fun getListedVersion(versionString: String): JMinecraftVersionList.Version? {
        val versionList = getJMinecraftVersionList() ?: return null // can't have listed versions if there's no list
        val versions = versionList.versions ?: return null
        for (version in versions) {
            if (version.id.equals(versionString)) return version
        }
        return null
    }

    private fun getJMinecraftVersionList(): JMinecraftVersionList? {
        val event = EventBus.getDefault().getStickyEvent(MinecraftVersionValueEvent::class.java)
        return event?.list
    }
}
