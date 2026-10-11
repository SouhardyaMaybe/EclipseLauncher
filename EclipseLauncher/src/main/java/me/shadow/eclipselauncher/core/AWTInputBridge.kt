package me.shadow.eclipselauncher.core

object AWTInputBridge {
    const val EVENT_TYPE_CHAR = 1000
    const val EVENT_TYPE_CURSOR_POS = 1003
    const val EVENT_TYPE_KEY = 1005
    const val EVENT_TYPE_MOUSE_BUTTON = 1006

    @JvmStatic
    fun sendKey(keychar: Char, keycode: Int) {
        // TODO: Android -> AWT keycode mapping
        nativeSendData(EVENT_TYPE_KEY, keychar.code, keycode, 1, 0)
        nativeSendData(EVENT_TYPE_KEY, keychar.code, keycode, 0, 0)
    }

    @JvmStatic
    fun sendKey(keychar: Char, keycode: Int, state: Int) {
        // TODO: Android -> AWT keycode mapping
        nativeSendData(EVENT_TYPE_KEY, keychar.code, keycode, state, 0)
    }

    @JvmStatic
    fun sendChar(keychar: Char) {
        nativeSendData(EVENT_TYPE_CHAR, keychar.code, 0, 0, 0)
    }

    @JvmStatic
    fun sendMousePress(awtButtons: Int, isDown: Boolean) {
        nativeSendData(EVENT_TYPE_MOUSE_BUTTON, awtButtons, if (isDown) 1 else 0, 0, 0)
    }

    @JvmStatic
    fun sendMousePress(awtButtons: Int) {
        sendMousePress(awtButtons, true)
        sendMousePress(awtButtons, false)
    }

    @JvmStatic
    fun sendMousePos(x: Int, y: Int) {
        nativeSendData(EVENT_TYPE_CURSOR_POS, x, y, 0, 0)
    }

    init {
        System.loadLibrary("eclipseexec_awt")
    }

    @JvmStatic
    external fun nativeSendData(type: Int, i1: Int, i2: Int, i3: Int, i4: Int)
    @JvmStatic
    external fun nativeClipboardReceived(data: String?, mimeTypeSub: String?)
    @JvmStatic
    external fun nativeMoveWindow(xoff: Int, yoff: Int)
}
