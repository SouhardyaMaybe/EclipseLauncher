package me.shadow.eclipselauncher.core.customcontrols.keyboard

import me.shadow.eclipselauncher.core.AWTInputBridge
import me.shadow.eclipselauncher.core.AWTInputEvent

/** Send chars via the AWT Bridgee */
class AwtCharSender : CharacterSenderStrategy {
    override fun sendBackspace() {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_BACK_SPACE)
    }

    override fun sendEnter() {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_ENTER)
    }

    override fun sendChar(character: Char) {
        AWTInputBridge.sendChar(character)
    }
}
