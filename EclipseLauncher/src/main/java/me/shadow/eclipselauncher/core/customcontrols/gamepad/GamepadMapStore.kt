package me.shadow.eclipselauncher.core.customcontrols.gamepad

import android.util.Log

import me.shadow.eclipselauncher.json.JsonParseException
import me.shadow.eclipselauncher.utils.path.PathManager

import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.utils.FileUtils

import java.io.File
import java.io.IOException

class GamepadMapStore {

    companion object {
        private val STORE_FILE = File(PathManager.DIR_DATA, "gamepad_map.json")
        private var sMapStore: GamepadMapStore? = null

        private fun createDefault(): GamepadMapStore {
            val mapStore = GamepadMapStore()
            mapStore.mInGameMap = GamepadMap.getDefaultGameMap()
            mapStore.mInMenuMap = GamepadMap.getDefaultMenuMap()
            return mapStore
        }

        private fun loadIfNecessary() {
            if (sMapStore == null) return
            load()
        }

        @JvmStatic
        fun load() {
            var mapStore: GamepadMapStore? = null
            if (STORE_FILE.exists() && STORE_FILE.canRead()) {
                try {
                    val storeFileContent = Tools.read(STORE_FILE)
                    mapStore = Tools.JSON_CODEC.fromJson(storeFileContent, GamepadMapStore::class.java)
                } catch (e: JsonParseException) {
                    Log.w("GamepadMapStore", "Map store failed to load!", e)
                } catch (e: IOException) {
                    Log.w("GamepadMapStore", "Map store failed to load!", e)
                }
            }
            if (mapStore == null) mapStore = createDefault()
            sMapStore = mapStore
        }

        @JvmStatic
        @Throws(IOException::class)
        fun save() {
            val mapStore = sMapStore ?: throw RuntimeException("Must load map store first!")
            FileUtils.ensureParentDirectory(STORE_FILE)
            val jsonData = Tools.JSON_CODEC.toJson(mapStore)
            Tools.write(STORE_FILE.absolutePath, jsonData)
        }

        @JvmStatic
        fun getGameMap(): GamepadMap {
            loadIfNecessary()
            return sMapStore!!.mInGameMap!!
        }

        @JvmStatic
        fun getMenuMap(): GamepadMap {
            loadIfNecessary()
            return sMapStore!!.mInMenuMap!!
        }
    }

    private var mInMenuMap: GamepadMap? = null
    private var mInGameMap: GamepadMap? = null
}
