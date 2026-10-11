package me.shadow.eclipselauncher.core.customcontrols.mouse

import android.view.MotionEvent

import me.shadow.eclipselauncher.core.Tools

/**
 * Class aiming at better detecting X-tap events regardless of the POINTERS
 * Only uses the least amount of events possible,
 * since we aren't guaranteed to have all events in order
 */
class TapDetector(
    tapNumberToDetect: Int,
    private val mDetectionMethod: Int
) {
    //We expect both ACTION_DOWN and ACTION_UP for the DETECTION_METHOD_BOTH
    private val mTapNumberToDetect: Int =
        if (detectBothTouch()) 2 * tapNumberToDetect else tapNumberToDetect

    private var mCurrentTapNumber = 0

    private var mLastEventTime = 0L
    private var mLastX = 9999f
    private var mLastY = 9999f

    /**
     * A function to call when you have a touch event.
     * @param e The MotionEvent to inspect
     * @return whether or not a X-tap happened for a pointer
     */
    fun onTouchEvent(e: MotionEvent): Boolean {
        val eventAction = e.actionMasked
        var pointerIndex = -1

        //Get the event to look forward
        if (detectDownTouch()) {
            if (eventAction == MotionEvent.ACTION_DOWN) pointerIndex = 0
            else if (eventAction == MotionEvent.ACTION_POINTER_DOWN) pointerIndex = e.actionIndex
        }
        if (detectUpTouch()) {
            if (eventAction == MotionEvent.ACTION_UP) pointerIndex = 0
            else if (eventAction == MotionEvent.ACTION_POINTER_UP) pointerIndex = e.actionIndex
        }

        if (pointerIndex == -1) return false // Useless event

        //Store current event info
        val eventX = e.getX(pointerIndex)
        val eventY = e.getY(pointerIndex)
        val eventTime = e.eventTime

        //Compute deltas
        val deltaTime = eventTime - mLastEventTime
        val deltaX = mLastX.toInt() - eventX.toInt()
        val deltaY = mLastY.toInt() - eventY.toInt()

        //Store current event info to persist on next event
        mLastEventTime = eventTime
        mLastX = eventX
        mLastY = eventY

        //Check for high enough speed and precision
        if (mCurrentTapNumber > 0) {
            if ((deltaTime < TAP_MIN_DELTA_MS || deltaTime > TAP_MAX_DELTA_MS) ||
                ((deltaX * deltaX + deltaY * deltaY) > TAP_SLOP_SQUARE_PX)
            ) {
                if (mDetectionMethod == DETECTION_METHOD_BOTH &&
                    (eventAction == MotionEvent.ACTION_UP || eventAction == MotionEvent.ACTION_POINTER_UP)
                ) {
                    // For the both method, the user is expected to start with a down action.
                    resetTapDetectionState()
                    return false
                } else {
                    // We invalidate previous taps, not this one though
                    mCurrentTapNumber = 0
                }
            }
        }

        //A worthy tap happened
        mCurrentTapNumber += 1
        if (mCurrentTapNumber >= mTapNumberToDetect) {
            resetTapDetectionState()
            return true
        }

        //If not enough taps are reached
        return false
    }

    /**
     * Reset the double tap values.
     */
    private fun resetTapDetectionState() {
        mCurrentTapNumber = 0
        mLastEventTime = 0L
        mLastX = 9999f
        mLastY = 9999f
    }

    private fun detectDownTouch(): Boolean {
        return (mDetectionMethod and DETECTION_METHOD_DOWN) == DETECTION_METHOD_DOWN
    }

    private fun detectUpTouch(): Boolean {
        return (mDetectionMethod and DETECTION_METHOD_UP) == DETECTION_METHOD_UP
    }

    private fun detectBothTouch(): Boolean {
        return mDetectionMethod == DETECTION_METHOD_BOTH
    }

    companion object {
        const val DETECTION_METHOD_DOWN = 0x1
        const val DETECTION_METHOD_UP = 0x2
        const val DETECTION_METHOD_BOTH = 0x3 //Unused for now

        private const val TAP_MIN_DELTA_MS = -1
        private const val TAP_MAX_DELTA_MS = 300
        private val TAP_SLOP_SQUARE_PX: Int = Tools.dpToPx(2500f).toInt()
    }
}
