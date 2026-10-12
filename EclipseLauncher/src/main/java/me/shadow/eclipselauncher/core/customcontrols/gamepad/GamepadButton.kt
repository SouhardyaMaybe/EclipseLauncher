package me.shadow.eclipselauncher.core.customcontrols.gamepad

/**
 *  This class corresponds to a button that does exist on the gamepad
 */
open class GamepadButton : GamepadEmulatedButton() {
    @JvmField
    var isToggleable = false
    private var mIsToggled = false

    override fun onDownStateChanged(isDown: Boolean) {
        if (isToggleable) {
            if (!isDown) return
            mIsToggled = !mIsToggled
            Gamepad.sendInput(keycodes, mIsToggled)
            return
        }
        super.onDownStateChanged(isDown)
    }

    override fun resetButtonState() {
        if (!mIsDown && mIsToggled) {
            Gamepad.sendInput(keycodes, false)
            mIsToggled = false
        } else {
            super.resetButtonState()
        }
    }
}
