package me.shadow.eclipselauncher.core.customcontrols.mouse

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver

import me.shadow.eclipselauncher.setting.AllStaticSettings

import me.shadow.eclipselauncher.core.MinecraftGLSurface
import me.shadow.eclipselauncher.core.Tools

import org.lwjgl.glfw.CallbackBridge

class AndroidPointerCapture(private val mTouchpad: AbstractTouchpad?, private val mHostView: View) :
    ViewTreeObserver.OnWindowFocusChangeListener, View.OnCapturedPointerListener {

    private val mMousePrescale: Float = Tools.dpToPx(1f)
    private val mPointerTracker = PointerTracker()
    private val mScroller = Scroller(TOUCHPAD_SCROLL_THRESHOLD)
    private val mVector: FloatArray = mPointerTracker.getMotionVector()

    private var mInputDeviceIdentifier = 0
    private var mDeviceSupportsRelativeAxis = false

    init {
        mHostView.setOnCapturedPointerListener(this)
        mHostView.viewTreeObserver.addOnWindowFocusChangeListener(this)
    }

    private fun enableTouchpadIfNecessary() {
        if (!mTouchpad!!.getDisplayState()) mTouchpad.enable(true)
    }

    fun handleAutomaticCapture() {
        if (!mHostView.hasWindowFocus()) {
            mHostView.requestFocus()
        } else {
            mHostView.requestPointerCapture()
        }
    }

    override fun onCapturedPointer(view: View, event: MotionEvent): Boolean {
        checkSameDevice(event.device)
        // Yes, we actually not only receive relative mouse events here, but also absolute touchpad ones!
        // Therefore, we need to know when it's a touchpad and when it's a mouse.

        if ((event.source and InputDevice.SOURCE_CLASS_TRACKBALL) != 0) {
            // If the source claims to be a relative device by belonging to the trackball class,
            // use its coordinates directly.
            if (mDeviceSupportsRelativeAxis) {
                // If some OEM decides to do a funny and make an absolute touchpad report itself as
                // a trackball, we will at least have semi-valid relative positions
                mVector[0] = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
                mVector[1] = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
            } else {
                // Otherwise trust the OS, i guess??
                mVector[0] = event.x
                mVector[1] = event.y
            }
        } else {
            // If it's not a trackball, it's likely a touchpad and needs tracking like a touchscreen.
            mPointerTracker.trackEvent(event)
            // The relative position will already be written down into the mVector variable.
        }

        if (!CallbackBridge.isGrabbing()) {
            enableTouchpadIfNecessary()
            // Yes, if the user's touchpad is multi-touch we will also receive events for that.
            // So, handle the scrolling gesture ourselves.
            mVector[0] *= mMousePrescale
            mVector[1] *= mMousePrescale
            if (event.pointerCount < 2) {
                mTouchpad!!.applyMotionVector(mVector)
                mScroller.resetScrollOvershoot()
            } else {
                mScroller.performScroll(mVector)
            }
        } else {
            // Position is updated by many events, hence it is send regardless of the event value
            CallbackBridge.mouseX += (mVector[0] * AllStaticSettings.scaleFactor)
            CallbackBridge.mouseY += (mVector[1] * AllStaticSettings.scaleFactor)
            CallbackBridge.sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
        }

        return when (event.actionMasked) {
            MotionEvent.ACTION_MOVE ->
                true

            MotionEvent.ACTION_BUTTON_PRESS ->
                MinecraftGLSurface.sendMouseButtonUnconverted(event.actionButton, true)

            MotionEvent.ACTION_BUTTON_RELEASE ->
                MinecraftGLSurface.sendMouseButtonUnconverted(event.actionButton, false)

            MotionEvent.ACTION_SCROLL -> {
                CallbackBridge.sendScroll(
                    event.getAxisValue(MotionEvent.AXIS_HSCROLL).toDouble(),
                    event.getAxisValue(MotionEvent.AXIS_VSCROLL).toDouble()
                )
                true
            }

            MotionEvent.ACTION_UP -> {
                mPointerTracker.cancelTracking()
                true
            }

            else ->
                false
        }
    }

    private fun checkSameDevice(inputDevice: InputDevice?) {
        if (inputDevice == null) return
        val newIdentifier = inputDevice.id
        if (mInputDeviceIdentifier != newIdentifier) {
            reinitializeDeviceSpecificProperties(inputDevice)
            mInputDeviceIdentifier = newIdentifier
        }
    }

    private fun reinitializeDeviceSpecificProperties(inputDevice: InputDevice) {
        mPointerTracker.cancelTracking()
        val relativeXSupported = inputDevice.getMotionRange(MotionEvent.AXIS_RELATIVE_X) != null
        val relativeYSupported = inputDevice.getMotionRange(MotionEvent.AXIS_RELATIVE_Y) != null
        mDeviceSupportsRelativeAxis = relativeXSupported && relativeYSupported
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) mHostView.requestPointerCapture()
    }

    fun detach() {
        mHostView.setOnCapturedPointerListener(null)
        mHostView.viewTreeObserver.removeOnWindowFocusChangeListener(this)
    }

    companion object {
        private const val TOUCHPAD_SCROLL_THRESHOLD = 1f
    }
}
