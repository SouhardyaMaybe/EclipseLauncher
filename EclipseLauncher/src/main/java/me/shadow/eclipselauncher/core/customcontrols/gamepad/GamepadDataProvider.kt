package me.shadow.eclipselauncher.core.customcontrols.gamepad

import me.shadow.eclipselauncher.core.GrabListener

interface GamepadDataProvider {
    fun getMenuMap(): GamepadMap
    fun getGameMap(): GamepadMap
    fun isGrabbing(): Boolean
    fun attachGrabListener(grabListener: GrabListener)
}
