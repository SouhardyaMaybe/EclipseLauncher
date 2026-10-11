package me.shadow.eclipselauncher.core.customcontrols.mouse

import org.lwjgl.glfw.CallbackBridge

class Scroller(private val mScrollThreshold: Float) {

    private var mScrollOvershootH = 0f
    private var mScrollOvershootV = 0f

    /**
     * Perform a scrolling gesture.
     * @param dx the X coordinate of the primary pointer's vector
     * @param dy the Y coordinate of the primary pointer's vector
     */
    fun performScroll(dx: Float, dy: Float) {
        val hScroll = (dx / mScrollThreshold) + mScrollOvershootH
        val vScroll = (dy / mScrollThreshold) + mScrollOvershootV
        val hScrollRound = hScroll.toInt()
        val vScrollRound = vScroll.toInt()
        if (hScrollRound != 0 || vScrollRound != 0) CallbackBridge.sendScroll(hScroll.toDouble(), vScroll.toDouble())
        mScrollOvershootH = hScroll - hScrollRound
        mScrollOvershootV = vScroll - vScrollRound
    }

    /**
     * Perform a scrolling gesture.
     * @param vector a 2-component vector that stores the relative position of the primary pointer.
     */
    fun performScroll(vector: FloatArray) {
        performScroll(vector[0], vector[1])
    }

    /**
     * Reset scroll overshoot values. Scroll overshoot makes the scrolling feel less
     * choppy, but will cause anomailes if not reset on the end of a scrolling gesture.
     */
    fun resetScrollOvershoot() {
        mScrollOvershootH = 0f
        mScrollOvershootV = 0f
    }
}
