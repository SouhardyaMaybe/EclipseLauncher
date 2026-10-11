package me.shadow.eclipselauncher.core.customcontrols.gamepad

import android.content.Context
import android.view.Choreographer
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView

import androidx.core.math.MathUtils

import me.shadow.eclipselauncher.event.single.MCOptionChangeEvent
import me.shadow.eclipselauncher.feature.MCOptions
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.setting.AllStaticSettings
import me.shadow.eclipselauncher.utils.EclipseTools

import me.shadow.eclipselauncher.core.GrabListener
import me.shadow.eclipselauncher.core.LwjglGlfwKeycode
import me.shadow.eclipselauncher.core.Tools

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.lwjgl.glfw.CallbackBridge

import fr.spse.gamepad_remapper.GamepadHandler
import fr.spse.gamepad_remapper.Settings

open class Gamepad : GrabListener, GamepadHandler {

    /* Sensitivity, adjusted according to screen size */
    private val mSensitivityFactor = 1.4 * (1080f / Tools.currentDisplayMetrics.heightPixels)

    private val mPointerImageView: ImageView

    private val mLeftJoystick: GamepadJoystick
    private var mCurrentJoystickDirection = GamepadJoystick.DIRECTION_NONE

    private val mRightJoystick: GamepadJoystick
    private var mLastHorizontalValue = 0.0f
    private var mLastVerticalValue = 0.0f

    private var mMouseMagnitude = 0.0
    private var mMouseAngle = 0.0
    private var mMouseSensitivity = 19.0

    private var mGameMap: GamepadMap? = null
    private var mMenuMap: GamepadMap? = null
    private var mCurrentMap: GamepadMap? = null

    private var isGrabbing = false

    /* Choreographer with time to compute delta on ticking */
    private val mScreenChoreographer: Choreographer
    private var mLastFrameTime = 0L

    private val mMapProvider: GamepadDataProvider

    constructor(contextView: View, inputDevice: InputDevice, mapProvider: GamepadDataProvider, showCursor: Boolean) {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        Settings.setDeadzoneScale(AllSettings.deadZoneScale.getValue().toFloat() / 100f)

        mScreenChoreographer = Choreographer.getInstance()
        val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                tick(frameTimeNanos)
                mScreenChoreographer.postFrameCallback(this)
            }
        }
        mScreenChoreographer.postFrameCallback(frameCallback)
        mLastFrameTime = System.nanoTime()

        mLeftJoystick = GamepadJoystick(MotionEvent.AXIS_X, MotionEvent.AXIS_Y, inputDevice)
        mRightJoystick = GamepadJoystick(MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ, inputDevice)

        val ctx = contextView.context
        mPointerImageView = ImageView(ctx)
        mPointerImageView.setImageDrawable(EclipseTools.customMouse(ctx))
        mPointerImageView.drawable!!.setFilterBitmap(false)

        val size = ((22 * MCOptions.mcScale) / AllStaticSettings.scaleFactor).toInt()
        mPointerImageView.layoutParams = FrameLayout.LayoutParams(size, size)

        mMapProvider = mapProvider

        CallbackBridge.sendCursorPos(CallbackBridge.windowWidth / 2f, CallbackBridge.windowHeight / 2f)

        if (showCursor) {
            (contextView.parent as ViewGroup).addView(mPointerImageView)
        }

        placePointerView(CallbackBridge.physicalWidth / 2, CallbackBridge.physicalHeight / 2)

        reloadGamepadMaps()
        mMapProvider.attachGrabListener(this)
    }

    fun reloadGamepadMaps() {
        mGameMap?.resetPressedState()
        mMenuMap?.resetPressedState()
        GamepadMapStore.load()
        mGameMap = mMapProvider.getGameMap()
        mMenuMap = mMapProvider.getMenuMap()
        mCurrentMap = mGameMap
        // Force state refresh
        val currentGrab = CallbackBridge.isGrabbing()
        isGrabbing = !currentGrab
        onGrabState(currentGrab)
    }

    fun updateJoysticks() {
        updateDirectionalJoystick()
        updateMouseJoystick()
    }

    @Subscribe
    fun event(event: MCOptionChangeEvent) {
        notifyGUISizeChange(MCOptions.mcScale)
    }

    fun notifyGUISizeChange(newSize: Int) {
        //Change the pointer size to match UI
        val size = ((22 * newSize) / AllStaticSettings.scaleFactor).toInt()
        mPointerImageView.post { mPointerImageView.layoutParams = FrameLayout.LayoutParams(size, size) }
    }

    /**
     * Send the new mouse position, computing the delta
     * @param frameTimeNanos The time to render the frame, used to compute mouse delta
     */
    private fun tick(frameTimeNanos: Long) {
        //update mouse position
        var newFrameTime = System.nanoTime()
        if (mLastHorizontalValue != 0.0f || mLastVerticalValue != 0.0f) {

            var acceleration = Math.pow(mMouseMagnitude, MOUSE_MAX_ACCELERATION)
            if (acceleration > 1) acceleration = 1.0

            // Compute delta since last tick time
            var deltaX = (Math.cos(mMouseAngle) * acceleration * mMouseSensitivity).toFloat()
            var deltaY = (Math.sin(mMouseAngle) * acceleration * mMouseSensitivity).toFloat()
            newFrameTime = System.nanoTime() // More accurate delta
            val deltaTimeScale = ((newFrameTime - mLastFrameTime) / 16666666f) // Scale of 1 = 60Hz
            deltaX *= deltaTimeScale
            deltaY *= deltaTimeScale

            CallbackBridge.mouseX += deltaX
            CallbackBridge.mouseY -= deltaY

            if (!isGrabbing) {
                CallbackBridge.mouseX = MathUtils.clamp(CallbackBridge.mouseX, 0f, CallbackBridge.windowWidth.toFloat())
                CallbackBridge.mouseY = MathUtils.clamp(CallbackBridge.mouseY, 0f, CallbackBridge.windowHeight.toFloat())
                placePointerView((CallbackBridge.mouseX / AllStaticSettings.scaleFactor).toInt(), (CallbackBridge.mouseY / AllStaticSettings.scaleFactor).toInt())
            }

            //Send the mouse to the game
            CallbackBridge.sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY)
        }

        // Update last nano time
        mLastFrameTime = newFrameTime
    }

    private fun updateMouseJoystick() {
        val currentJoystick = if (isGrabbing) mRightJoystick else mLeftJoystick
        val horizontalValue = currentJoystick.getHorizontalAxis()
        val verticalValue = currentJoystick.getVerticalAxis()
        if (horizontalValue != mLastHorizontalValue || verticalValue != mLastVerticalValue) {
            mLastHorizontalValue = horizontalValue
            mLastVerticalValue = verticalValue

            mMouseMagnitude = currentJoystick.getMagnitude()
            mMouseAngle = currentJoystick.getAngleRadian()

            tick(System.nanoTime())
            return
        }
        mLastHorizontalValue = horizontalValue
        mLastVerticalValue = verticalValue

        mMouseMagnitude = currentJoystick.getMagnitude()
        mMouseAngle = currentJoystick.getAngleRadian()
    }

    private fun updateDirectionalJoystick() {
        val currentJoystick = if (isGrabbing) mLeftJoystick else mRightJoystick

        val lastJoystickDirection = mCurrentJoystickDirection
        mCurrentJoystickDirection = currentJoystick.getHeightDirection()

        if (mCurrentJoystickDirection == lastJoystickDirection) return

        sendDirectionalKeycode(lastJoystickDirection, false, getCurrentMap())
        sendDirectionalKeycode(mCurrentJoystickDirection, true, getCurrentMap())
    }

    private fun getCurrentMap(): GamepadMap {
        return mCurrentMap!!
    }

    /** Place the pointer on the screen, offsetting the image size */
    private fun placePointerView(x: Int, y: Int) {
        mPointerImageView.x = x.toFloat()
        mPointerImageView.y = y.toFloat()
    }

    /** Update the grabbing state, and change the currentMap, mouse position and sensibility */
    override fun onGrabState(isGrabbing: Boolean) {
        val lastGrabbingValue = this.isGrabbing
        this.isGrabbing = isGrabbing
        if (lastGrabbingValue == isGrabbing) return

        // Switch grabbing state then
        mCurrentMap!!.resetPressedState()
        if (isGrabbing) {
            mCurrentMap = mGameMap
            mPointerImageView.visibility = View.INVISIBLE
            mMouseSensitivity = 18.0
            return
        }

        mCurrentMap = mMenuMap
        sendDirectionalKeycode(mCurrentJoystickDirection, false, mGameMap!!) // removing what we were doing

        CallbackBridge.sendCursorPos(CallbackBridge.windowWidth / 2f, CallbackBridge.windowHeight / 2f)
        placePointerView(CallbackBridge.physicalWidth / 2, CallbackBridge.physicalHeight / 2)
        mPointerImageView.visibility = View.VISIBLE
        // Sensitivity in menu is MC and HARDWARE resolution dependent
        mMouseSensitivity = 19 * AllStaticSettings.scaleFactor / mSensitivityFactor
    }

    override fun handleGamepadInput(keycode: Int, value: Float) {
        val isKeyEventDown = value == 1f
        when (keycode) {
            KeyEvent.KEYCODE_BUTTON_A ->
                getCurrentMap().BUTTON_A.update(isKeyEventDown)

            KeyEvent.KEYCODE_BUTTON_B ->
                getCurrentMap().BUTTON_B.update(isKeyEventDown)

            KeyEvent.KEYCODE_BUTTON_X ->
                getCurrentMap().BUTTON_X.update(isKeyEventDown)

            KeyEvent.KEYCODE_BUTTON_Y ->
                getCurrentMap().BUTTON_Y.update(isKeyEventDown)

            //Shoulders
            KeyEvent.KEYCODE_BUTTON_L1 ->
                getCurrentMap().SHOULDER_LEFT.update(isKeyEventDown)

            KeyEvent.KEYCODE_BUTTON_R1 ->
                getCurrentMap().SHOULDER_RIGHT.update(isKeyEventDown)

            //Triggers
            KeyEvent.KEYCODE_BUTTON_L2 ->
                getCurrentMap().TRIGGER_LEFT.update(isKeyEventDown)

            KeyEvent.KEYCODE_BUTTON_R2 ->
                getCurrentMap().TRIGGER_RIGHT.update(isKeyEventDown)

            //L3 || R3
            KeyEvent.KEYCODE_BUTTON_THUMBL ->
                getCurrentMap().THUMBSTICK_LEFT.update(isKeyEventDown)

            KeyEvent.KEYCODE_BUTTON_THUMBR ->
                getCurrentMap().THUMBSTICK_RIGHT.update(isKeyEventDown)

            //DPAD
            KeyEvent.KEYCODE_DPAD_UP ->
                getCurrentMap().DPAD_UP.update(isKeyEventDown)

            KeyEvent.KEYCODE_DPAD_DOWN ->
                getCurrentMap().DPAD_DOWN.update(isKeyEventDown)

            KeyEvent.KEYCODE_DPAD_LEFT ->
                getCurrentMap().DPAD_LEFT.update(isKeyEventDown)

            KeyEvent.KEYCODE_DPAD_RIGHT ->
                getCurrentMap().DPAD_RIGHT.update(isKeyEventDown)

            KeyEvent.KEYCODE_DPAD_CENTER -> {
                getCurrentMap().DPAD_RIGHT.update(false)
                getCurrentMap().DPAD_LEFT.update(false)
                getCurrentMap().DPAD_UP.update(false)
                getCurrentMap().DPAD_DOWN.update(false)
            }

            //Start/select
            KeyEvent.KEYCODE_BUTTON_START ->
                getCurrentMap().BUTTON_START.update(isKeyEventDown)

            KeyEvent.KEYCODE_BUTTON_SELECT ->
                getCurrentMap().BUTTON_SELECT.update(isKeyEventDown)

            /* Now, it is time for motionEvents */
            MotionEvent.AXIS_HAT_X -> {
                getCurrentMap().DPAD_RIGHT.update(value > 0.85)
                getCurrentMap().DPAD_LEFT.update(value < -0.85)
            }

            MotionEvent.AXIS_HAT_Y -> {
                getCurrentMap().DPAD_DOWN.update(value > 0.85)
                getCurrentMap().DPAD_UP.update(value < -0.85)
            }

            // Left joystick
            MotionEvent.AXIS_X -> {
                mLeftJoystick.setXAxisValue(value)
                updateJoysticks()
            }

            MotionEvent.AXIS_Y -> {
                mLeftJoystick.setYAxisValue(value)
                updateJoysticks()
            }

            // Right joystick
            MotionEvent.AXIS_Z -> {
                mRightJoystick.setXAxisValue(value)
                updateJoysticks()
            }

            MotionEvent.AXIS_RZ -> {
                mRightJoystick.setYAxisValue(value)
                updateJoysticks()
            }

            // Triggers
            MotionEvent.AXIS_RTRIGGER ->
                getCurrentMap().TRIGGER_RIGHT.update(value > 0.5)

            MotionEvent.AXIS_LTRIGGER ->
                getCurrentMap().TRIGGER_LEFT.update(value > 0.5)

            else ->
                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_SPACE.toInt(), CallbackBridge.getCurrentMods(), isKeyEventDown)
        }
    }

    companion object {
        private const val MOUSE_MAX_ACCELERATION = 2.0

        @JvmStatic
        fun sendInput(keycodes: ShortArray, isDown: Boolean) {
            for (keycode in keycodes) {
                when (keycode) {
                    GamepadMap.MOUSE_SCROLL_DOWN ->
                        if (isDown) CallbackBridge.sendScroll(0.0, -1.0)

                    GamepadMap.MOUSE_SCROLL_UP ->
                        if (isDown) CallbackBridge.sendScroll(0.0, 1.0)

                    GamepadMap.MOUSE_LEFT ->
                        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), isDown)

                    GamepadMap.MOUSE_MIDDLE ->
                        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE.toInt(), isDown)

                    GamepadMap.MOUSE_RIGHT ->
                        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), isDown)

                    GamepadMap.UNSPECIFIED -> {}

                    else -> {
                        CallbackBridge.sendKeyPress(keycode.toInt(), CallbackBridge.getCurrentMods(), isDown)
                        CallbackBridge.setModifiers(keycode.toInt(), isDown)
                    }
                }
            }
        }

        @JvmStatic
        fun isGamepadEvent(event: MotionEvent): Boolean {
            return GamepadJoystick.isJoystickEvent(event)
        }

        @JvmStatic
        fun isGamepadEvent(event: KeyEvent): Boolean {
            val device = event.device
            val isGamepad = ((event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                    || ((device != null) && ((device.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD))

            return isGamepad && GamepadDpad.isDpadEvent(event)
        }

        private fun sendDirectionalKeycode(direction: Int, isDown: Boolean, map: GamepadMap) {
            when (direction) {
                GamepadJoystick.DIRECTION_NORTH ->
                    map.DIRECTION_FORWARD.update(isDown)

                GamepadJoystick.DIRECTION_NORTH_EAST -> {
                    map.DIRECTION_FORWARD.update(isDown)
                    map.DIRECTION_RIGHT.update(isDown)
                }

                GamepadJoystick.DIRECTION_EAST ->
                    map.DIRECTION_RIGHT.update(isDown)

                GamepadJoystick.DIRECTION_SOUTH_EAST -> {
                    map.DIRECTION_RIGHT.update(isDown)
                    map.DIRECTION_BACKWARD.update(isDown)
                }

                GamepadJoystick.DIRECTION_SOUTH ->
                    map.DIRECTION_BACKWARD.update(isDown)

                GamepadJoystick.DIRECTION_SOUTH_WEST -> {
                    map.DIRECTION_BACKWARD.update(isDown)
                    map.DIRECTION_LEFT.update(isDown)
                }

                GamepadJoystick.DIRECTION_WEST ->
                    map.DIRECTION_LEFT.update(isDown)

                GamepadJoystick.DIRECTION_NORTH_WEST -> {
                    map.DIRECTION_FORWARD.update(isDown)
                    map.DIRECTION_LEFT.update(isDown)
                }
            }
        }
    }
}
