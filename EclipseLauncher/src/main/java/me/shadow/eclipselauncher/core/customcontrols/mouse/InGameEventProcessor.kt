package me.shadow.eclipselauncher.core.customcontrols.mouse

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent

import me.shadow.eclipselauncher.setting.AllSettings

import org.lwjgl.glfw.CallbackBridge

class InGameEventProcessor(private val mSensitivity: Double) : TouchEventProcessor {
    private val mGestureHandler = Handler(Looper.getMainLooper())
    private var mEventTransitioned = true
    private val mTracker = PointerTracker()
    private val mLeftClickGesture = LeftClickGesture(mGestureHandler)
    private val mRightClickGesture = RightClickGesture(mGestureHandler)

    override fun processTouchEvent(motionEvent: MotionEvent): Boolean {
        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTracker.startTracking(motionEvent)
                if (!AllSettings.disableGestures.getValue()) {
                    mEventTransitioned = false
                    checkGestures()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                mTracker.trackEvent(motionEvent)
                val motionVector = mTracker.getMotionVector()
                val deltaX = (motionVector[0] * mSensitivity).toFloat()
                val deltaY = (motionVector[1] * mSensitivity).toFloat()
                mLeftClickGesture.setMotion(deltaX, deltaY)
                mRightClickGesture.setMotion(deltaX, deltaY)
                CallbackBridge.mouseX += deltaX
                CallbackBridge.mouseY += deltaY
                CallbackBridge.sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
                if (!AllSettings.disableGestures.getValue()) checkGestures()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mTracker.cancelTracking()
                cancelGestures(false)
            }
        }
        return true
    }

    override fun cancelPendingActions() {
        cancelGestures(true)
    }

    private fun checkGestures() {
        mLeftClickGesture.inputEvent()
        // Only register right click events if it's a fresh event stream, not one after a transition.
        // This is done to avoid problems when people hold the button for just a bit too long after
        // exiting a menu for example.
        if (!mEventTransitioned) mRightClickGesture.inputEvent()
    }

    private fun cancelGestures(isSwitching: Boolean) {
        mEventTransitioned = true
        mLeftClickGesture.cancel(isSwitching)
        mRightClickGesture.cancel(isSwitching)
    }
}
