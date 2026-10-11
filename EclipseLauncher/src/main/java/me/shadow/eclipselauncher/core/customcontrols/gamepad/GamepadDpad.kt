package me.shadow.eclipselauncher.core.customcontrols.gamepad

import android.view.InputDevice
import android.view.KeyEvent


class GamepadDpad {

    companion object {
        @JvmStatic
        fun isDpadEvent(event: KeyEvent): Boolean {
            val device = event.device
            return event.isFromSource(InputDevice.SOURCE_GAMEPAD) && (device == null || device.keyboardType != InputDevice.KEYBOARD_TYPE_ALPHABETIC)
        }
    }
}
