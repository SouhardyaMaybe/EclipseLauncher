package me.shadow.eclipselauncher.core.customcontrols.mouse

import android.os.Handler

import me.shadow.eclipselauncher.setting.AllStaticSettings

import me.shadow.eclipselauncher.core.LwjglGlfwKeycode

import org.lwjgl.glfw.CallbackBridge

class DropGesture(private val mHandler: Handler) : Runnable {
    private var mActive = false

    fun submit() {
        if (!mActive) {
            mActive = true
            mHandler.postDelayed(this, AllStaticSettings.timeLongPressTrigger.toLong())
        }
    }

    fun cancel() {
        mActive = false
        mHandler.removeCallbacks(this)
    }

    override fun run() {
        if (!mActive) return
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_Q.toInt())
        mHandler.postDelayed(this, 250L)
    }
}
