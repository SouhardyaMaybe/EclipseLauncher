package me.shadow.eclipselauncher.core.customcontrols.gamepad

import android.view.KeyEvent

/**
 * This class corresponds to a button that does not physically exist on the gamepad, but is
 * emulated from other inputs on it (like WASD directional keys)
 */
open class GamepadEmulatedButton {
    lateinit var keycodes: ShortArray
    protected var mIsDown = false

    fun update(event: KeyEvent) {
        val isKeyDown = event.action == KeyEvent.ACTION_DOWN
        update(isKeyDown)
    }

    fun update(isKeyDown: Boolean) {
        if (isKeyDown != mIsDown) {
            mIsDown = isKeyDown
            onDownStateChanged(mIsDown)
        }
    }

    open fun resetButtonState() {
        if (mIsDown) Gamepad.sendInput(keycodes, false)
        mIsDown = false
    }

    protected open fun onDownStateChanged(isDown: Boolean) {
        Gamepad.sendInput(keycodes, mIsDown)
    }
}
