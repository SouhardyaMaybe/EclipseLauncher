package me.shadow.eclipselauncher.core.customcontrols.mouse

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View

import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.setting.AllStaticSettings
import me.shadow.eclipselauncher.utils.EclipseTools
import me.shadow.eclipselauncher.utils.image.ImageUtils

import me.shadow.eclipselauncher.core.GrabListener
import me.shadow.eclipselauncher.core.Tools

import org.lwjgl.glfw.CallbackBridge

/**
 * Class dealing with the virtual mouse
 */
class Touchpad @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), GrabListener, AbstractTouchpad {
    /* Whether the Touchpad should be displayed */
    private var mDisplayState = false
    /* Mouse pointer icon used by the touchpad */
    private var mMousePointerDrawable: Drawable? = null
    private var mMouseX = 0f
    private var mMouseY = 0f

    init {
        init()
    }

    /** Enable the touchpad */
    private fun _enable() {
        visibility = VISIBLE
        placeMouseAt(Tools.currentDisplayMetrics.widthPixels / 2f, Tools.currentDisplayMetrics.heightPixels / 2f)
    }

    /** Disable the touchpad and hides the mouse */
    private fun _disable() {
        visibility = GONE
    }

    /** @return The new state, enabled or disabled */
    fun switchState(): Boolean {
        mDisplayState = !mDisplayState
        if (!CallbackBridge.isGrabbing()) {
            if (mDisplayState) _enable()
            else _disable()
        }
        return mDisplayState
    }

    fun placeMouseAt(x: Float, y: Float) {
        mMouseX = x
        mMouseY = y
        updateMousePosition()
    }

    private fun sendMousePosition() {
        CallbackBridge.sendCursorPos((mMouseX * AllStaticSettings.scaleFactor), (mMouseY * AllStaticSettings.scaleFactor))
    }

    private fun updateMousePosition() {
        sendMousePosition()
        // I wanted to implement a dirty rect for this, but it is ignored since API level 21
        // (which is our min API)
        // Let's hope the "internally calculated area" is good enough.
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.translate(mMouseX, mMouseY)
        mMousePointerDrawable!!.draw(canvas)
    }

    private fun init() {
        // Setup mouse pointer
        updateMouseDrawable()
        // For some reason it's annotated as Nullable even though it doesn't seem to actually
        // ever return null
        assert mMousePointerDrawable != null

        updateMouseScale()

        isFocusable = false
        defaultFocusHighlightEnabled = false

        // When the game is grabbing, we should not display the mouse
        disable()
        mDisplayState = false
    }

    fun updateMouseScale() {
        val mouseScale = ImageUtils.resizeWithRatio(
            mMousePointerDrawable!!.intrinsicWidth, mMousePointerDrawable!!.intrinsicHeight,
            AllSettings.mouseScale.getValue()
        )
        mMousePointerDrawable!!.setBounds(0, 0, (mouseScale.width * 0.5).toInt(), (mouseScale.height * 0.5).toInt())
    }

    fun updateMouseDrawable() {
        mMousePointerDrawable = EclipseTools.customMouse(context)
        updateMouseScale()
    }

    override fun onGrabState(isGrabbing: Boolean) {
        post { updateGrabState(isGrabbing) }
    }

    private fun updateGrabState(isGrabbing: Boolean) {
        if (!isGrabbing) {
            if (mDisplayState && visibility != VISIBLE) _enable()
            if (!mDisplayState && visibility == VISIBLE) _disable()
        } else {
            if (visibility != View.GONE) _disable()
        }
    }

    override fun getDisplayState(): Boolean {
        return mDisplayState
    }

    override fun applyMotionVector(x: Float, y: Float) {
        mMouseX = Math.max(
            0f,
            Math.min(
                Tools.currentDisplayMetrics.widthPixels.toFloat(),
                mMouseX + x * (AllSettings.mouseSpeed.getValue() / 100f)
            )
        )
        mMouseY = Math.max(
            0f,
            Math.min(
                Tools.currentDisplayMetrics.heightPixels.toFloat(),
                mMouseY + y * (AllSettings.mouseSpeed.getValue() / 100f)
            )
        )
        updateMousePosition()
    }

    override fun enable(supposed: Boolean) {
        if (mDisplayState) return
        mDisplayState = true
        if (supposed && CallbackBridge.isGrabbing()) return
        _enable()
    }

    override fun disable() {
        if (!mDisplayState) return
        mDisplayState = false
        _disable()
    }
}
