package me.shadow.eclipselauncher.feature.download

import me.shadow.eclipselauncher.feature.download.item.DependenciesInfoItem
import me.shadow.eclipselauncher.feature.download.item.ModLikeVersionItem
import me.shadow.eclipselauncher.feature.download.item.ModVersionItem
import me.shadow.eclipselauncher.feature.download.item.VersionItem

/**
 * Cache the search results in memory, so the previous results can be read directly from memory on the next load
 */
class InfoCache {
    abstract class CacheBase<V> {
        private val cache: MutableMap<String, V> = HashMap()

        /**
         * Store the searched value in memory by ModId
         */
        fun put(modId: String, value: V) {
            cache[modId] = value
        }

        /**
         * Get the value stored in memory by ModId, returning null if it is absent
         */
        fun get(modId: String): V? {
            return cache[modId]
        }

        /**
         * Check whether the given ModId is already stored in memory
         */
        fun containsKey(modId: String): Boolean {
            return cache.containsKey(modId)
        }
    }

    object DependencyInfoCache : CacheBase<DependenciesInfoItem>()
    object VersionCache : CacheBase<MutableList<VersionItem>>()
    object ModVersionCache : CacheBase<MutableList<ModVersionItem>>()
    object ModPackVersionCache : CacheBase<MutableList<ModLikeVersionItem>>()
}