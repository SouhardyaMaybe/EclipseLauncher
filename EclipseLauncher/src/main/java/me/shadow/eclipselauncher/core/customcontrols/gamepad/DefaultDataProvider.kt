package me.shadow.eclipselauncher.core.customcontrols.gamepad

import me.shadow.eclipselauncher.core.GrabListener

import org.lwjgl.glfw.CallbackBridge

class DefaultDataProvider private constructor() : GamepadDataProvider {

    override fun getGameMap(): GamepadMap {
        return GamepadMapStore.getGameMap()
    }

    override fun getMenuMap(): GamepadMap {
        return GamepadMapStore.getMenuMap()
    }

    override fun isGrabbing(): Boolean {
        return CallbackBridge.isGrabbing()
    }

    override fun attachGrabListener(grabListener: GrabListener) {
        CallbackBridge.addGrabListener(grabListener)
    }

    companion object {
        @JvmField
        val INSTANCE = DefaultDataProvider()
    }
}
