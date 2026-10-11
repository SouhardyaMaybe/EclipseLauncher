package me.shadow.eclipselauncher.core.customcontrols.mouse

import android.view.MotionEvent

import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.setting.AllStaticSettings

import me.shadow.eclipselauncher.core.LwjglGlfwKeycode
import me.shadow.eclipselauncher.core.Tools

import org.lwjgl.glfw.CallbackBridge

class InGUIEventProcessor : TouchEventProcessor {
    private val mTracker = PointerTracker()
    private val mSingleTapDetector: TapDetector
    private var mTouchpad: AbstractTouchpad? = null
    private var mIsMouseDown = false
    private var mStartX = 0f
    private var mStartY = 0f
    private val mScroller = Scroller(FINGER_SCROLL_THRESHOLD)

    init {
        mSingleTapDetector = TapDetector(1, TapDetector.DETECTION_METHOD_BOTH)
    }

    override fun processTouchEvent(motionEvent: MotionEvent): Boolean {
        val singleTap = mSingleTapDetector.onTouchEvent(motionEvent)

        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTracker.startTracking(motionEvent)
                if (!touchpadDisplayed()) {
                    sendTouchCoordinates(motionEvent.x, motionEvent.y)

                    // disabled gestures means no scrolling possible, send gesture early
                    if (AllSettings.disableGestures.getValue()) enableMouse()
                    else setGestureStart(motionEvent)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerCount = motionEvent.pointerCount
                val pointerIndex = mTracker.trackEvent(motionEvent)
                if (pointerCount == 1 || AllSettings.disableGestures.getValue()) {
                    if (touchpadDisplayed()) {
                        mTouchpad!!.applyMotionVector(mTracker.getMotionVector())
                    } else {
                        val mainPointerX = motionEvent.getX(pointerIndex)
                        val mainPointerY = motionEvent.getY(pointerIndex)
                        sendTouchCoordinates(mainPointerX, mainPointerY)

                        if (!mIsMouseDown) {
                            if (!hasGestureStarted()) setGestureStart(motionEvent)
                            if (!LeftClickGesture.isFingerStill(mStartX, mStartY, FINGER_STILL_THRESHOLD))
                                enableMouse()
                        }
                    }
                } else mScroller.performScroll(mTracker.getMotionVector())
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mScroller.resetScrollOvershoot()
                mTracker.cancelTracking()

                // Handle single tap on gestures
                if ((!AllSettings.disableGestures.getValue() || touchpadDisplayed()) && !mIsMouseDown && singleTap) {
                    CallbackBridge.putMouseEventWithCoords(
                        LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(),
                        CallbackBridge.mouseX, CallbackBridge.mouseY
                    )
                }

                if (mIsMouseDown) disableMouse()
                resetGesture()
            }
        }

        return true
    }

    private fun touchpadDisplayed(): Boolean {
        return mTouchpad?.getDisplayState() == true
    }

    fun setAbstractTouchpad(touchpad: AbstractTouchpad?) {
        mTouchpad = touchpad
    }

    private fun sendTouchCoordinates(x: Float, y: Float) {
        CallbackBridge.sendCursorPos(x * AllStaticSettings.scaleFactor, y * AllStaticSettings.scaleFactor)
    }

    private fun enableMouse() {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), true)
        mIsMouseDown = true
    }

    private fun disableMouse() {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), false)
        mIsMouseDown = false
    }

    private fun setGestureStart(event: MotionEvent) {
        mStartX = event.x * AllStaticSettings.scaleFactor
        mStartY = event.y * AllStaticSettings.scaleFactor
    }

    private fun resetGesture() {
        mStartX = -1f
        mStartY = -1f
    }

    private fun hasGestureStarted(): Boolean {
        return mStartX != -1f || mStartY != -1f
    }

    override fun cancelPendingActions() {
        mScroller.resetScrollOvershoot()
        disableMouse()
    }

    companion object {
        val FINGER_SCROLL_THRESHOLD: Float = Tools.dpToPx(6f)
        val FINGER_STILL_THRESHOLD: Float = Tools.dpToPx(5f)
    }
}
