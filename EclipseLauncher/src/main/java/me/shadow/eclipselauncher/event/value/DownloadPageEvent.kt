package me.shadow.eclipselauncher.event.value

/**
 * Some events of the download page
 */
class DownloadPageEvent {
    /**
     * When switching download pages, use this event to tell the Fragment to play an animation
     * @param index the category index of the Fragment
     * @param classify the animation type (IN: enter animation, OUT: exit animation)
     */
    class PageSwapEvent(val index: Int, val classify: Int) {
        companion object {
            const val IN = 0
            const val OUT = 1
        }
    }

    /**
     * Download page destroyed event
     */
    class PageDestroyEvent

    /**
     * Whether the RecyclerView is disabled
     */
    class RecyclerEnableEvent(val enable: Boolean)
}