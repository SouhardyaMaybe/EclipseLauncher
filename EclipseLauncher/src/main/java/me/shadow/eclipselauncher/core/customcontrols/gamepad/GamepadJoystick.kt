package me.shadow.eclipselauncher.core.customcontrols.gamepad

import android.view.InputDevice
import android.view.MotionEvent

import me.shadow.eclipselauncher.core.utils.MathUtils

class GamepadJoystick {

    companion object {
        //Directions
        const val DIRECTION_NONE = -1 //GamepadJoystick at the center

        const val DIRECTION_EAST = 0
        const val DIRECTION_NORTH_EAST = 1
        const val DIRECTION_NORTH = 2
        const val DIRECTION_NORTH_WEST = 3
        const val DIRECTION_WEST = 4
        const val DIRECTION_SOUTH_WEST = 5
        const val DIRECTION_SOUTH = 6
        const val DIRECTION_SOUTH_EAST = 7

        @JvmStatic
        fun isJoystickEvent(event: MotionEvent): Boolean {
            return (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                    && event.action == MotionEvent.ACTION_MOVE
        }
    }

    private val mInputDevice: InputDevice

    private val mHorizontalAxis: Int
    private val mVerticalAxis: Int
    private var mVerticalAxisValue = 0f
    private var mHorizontalAxisValue = 0f

    constructor(horizontalAxis: Int, verticalAxis: Int, device: InputDevice) {
        mHorizontalAxis = horizontalAxis
        mVerticalAxis = verticalAxis
        this.mInputDevice = device
    }

    fun getAngleRadian(): Double {
        //From -PI to PI
        // TODO misuse of the deadzone here !
        return -Math.atan2(getVerticalAxis().toDouble(), getHorizontalAxis().toDouble())
    }

    fun getAngleDegree(): Double {
        //From 0 to 360 degrees
        var result = Math.toDegrees(getAngleRadian())
        if (result < 0) result += 360

        return result
    }

    fun getMagnitude(): Double {
        val x = Math.abs(mHorizontalAxisValue)
        val y = Math.abs(mVerticalAxisValue)

        return MathUtils.dist(0f, 0f, x, y).toDouble()
    }

    fun getVerticalAxis(): Float {
        return mVerticalAxisValue
    }

    fun getHorizontalAxis(): Float {
        return mHorizontalAxisValue
    }

    fun getHeightDirection(): Int {
        if (getMagnitude() == 0.0) return DIRECTION_NONE
        return ((getAngleDegree() + 22.5) / 45).toInt() % 8
    }

    /* Setters */
    fun setXAxisValue(value: Float) {
        this.mHorizontalAxisValue = value
    }

    fun setYAxisValue(value: Float) {
        this.mVerticalAxisValue = value
    }
}
