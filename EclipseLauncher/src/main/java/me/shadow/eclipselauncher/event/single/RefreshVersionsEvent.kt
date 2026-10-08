package me.shadow.eclipselauncher.event.single

/**
 * Notify through this event when the version list is refreshed
 * Versions are refreshed asynchronously, so make sure the event is handled on the UI thread
 * @see me.shadow.eclipselauncher.feature.version.VersionsManager
 */
class RefreshVersionsEvent(val mode: MODE) {
    enum class MODE {
        START, END
    }
}