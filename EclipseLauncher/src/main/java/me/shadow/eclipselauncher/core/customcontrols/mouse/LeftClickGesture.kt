package me.shadow.eclipselauncher.core.customcontrols.mouse

import android.os.Handler

import me.shadow.eclipselauncher.setting.AllStaticSettings

import me.shadow.eclipselauncher.core.LwjglGlfwKeycode
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.utils.MathUtils

import org.lwjgl.glfw.CallbackBridge

class LeftClickGesture(handler: Handler) : ValidatorGesture(handler) {
    private var mGestureStartX = 0f
    private var mGestureStartY = 0f
    private var mGestureEndX = 0f
    private var mGestureEndY = 0f
    private var mMouseActivated = false

    fun inputEvent() {
        if (submit()) {
            mGestureStartX = CallbackBridge.mouseX
            mGestureEndX = mGestureStartX
            mGestureStartY = CallbackBridge.mouseY
            mGestureEndY = mGestureStartY
        }
    }

    override fun getGestureDelay(): Int {
        return AllStaticSettings.timeLongPressTrigger
    }

    override fun checkAndTrigger(): Boolean {
        val fingerStill = isFingerStill(mGestureStartX, mGestureStartY, mGestureEndX, mGestureEndY, FINGER_STILL_THRESHOLD.toFloat())
        // If the finger is still, fire the gesture.
        if (fingerStill) {
            CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), true)
            mMouseActivated = true
        }
        // Otherwise, don't click but still keep it active
        return true
    }

    override fun onGestureCancelled(isSwitching: Boolean) {
        if (mMouseActivated) {
            CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), false)
            mMouseActivated = false
        }
    }

    fun setMotion(deltaX: Float, deltaY: Float) {
        mGestureEndX += deltaX
        mGestureEndY += deltaY
    }

    companion object {
        val FINGER_STILL_THRESHOLD: Int = Tools.dpToPx(9f).toInt()

        /**
         * Check if the finger is still when compared to mouseX/mouseY in CallbackBridge.
         * @param startX the starting X of the gesture
         * @param startY the starting Y of the gesture
         * @return whether the finger's position counts as "still" or not
         */
        fun isFingerStill(startX: Float, startY: Float, threshold: Float): Boolean {
            return MathUtils.dist(
                CallbackBridge.mouseX,
                CallbackBridge.mouseY,
                startX,
                startY
            ) <= threshold
        }

        fun isFingerStill(startX: Float, startY: Float, endX: Float, endY: Float, threshold: Float): Boolean {
            return MathUtils.dist(
                endX,
                endY,
                startX,
                startY
            ) <= threshold
        }
    }
}
