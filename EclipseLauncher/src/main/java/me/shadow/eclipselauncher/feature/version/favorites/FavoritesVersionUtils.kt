package me.shadow.eclipselauncher.feature.version.favorites

import me.shadow.eclipselauncher.feature.version.VersionsManager
import java.util.concurrent.ConcurrentHashMap

class FavoritesVersionUtils private constructor() {
    companion object {
        private inline fun modifyFavorites(action: (MutableMap<String, MutableSet<String>>) -> Unit) {
            VersionsManager.currentGameInfo.apply {
                action(favoritesMap)
                saveCurrentInfo()
            }
        }

        /**
         * Atomically rename a version
         */
        fun renameVersion(oldName: String, newName: String) = modifyFavorites { map ->
            map.values.forEach { versions ->
                if (oldName in versions) {
                    versions.remove(oldName)
                    versions.add(newName)
                }
            }
        }

        /**
         * Add a favorite folder
         */
        fun addFolder(name: String) = modifyFavorites { map ->
            map.putIfAbsent(name, ConcurrentHashMap.newKeySet())
        }

        /**
         * Remove a favorite folder
         */
        fun removeFolder(name: String) = modifyFavorites { map ->
            map.remove(name)
        }

        /**
         * Update the version's favorite folders
         * @param version the target version
         * @param targetFolders the set of folders that should contain this version
         */
        fun updateVersionFolders(version: String, targetFolders: Set<String>) = modifyFavorites { map ->
            //Add to the target folders
            targetFolders.forEach { folder ->
                map.getOrPut(folder) { ConcurrentHashMap.newKeySet() }.add(version)
            }

            //Remove from folders that are not targets
            map.keys.filterNot { it in targetFolders }.forEach { folder ->
                map[folder]?.remove(version)
            }
        }

        /**
         * Get the valid favorites structure
         */
        fun getFavoritesStructure(): Map<String, Set<String>> =
            VersionsManager.currentGameInfo.favoritesMap.let { map ->
                map.entries.associate { (k, v) -> k to v.toSet() }
            }

        /**
         * Get the valid versions of the given folder
         */
        fun getValidVersions(folder: String): Set<String> =
            VersionsManager.currentGameInfo.favoritesMap[folder]
                ?.filter { VersionsManager.checkVersionExistsByName(it) }
                .orEmpty()
                .toSet()
    }
}