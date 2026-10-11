package me.shadow.eclipselauncher.core.customcontrols.buttons

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.TextView

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.setting.AllSettings

import me.shadow.eclipselauncher.core.LwjglGlfwKeycode
import me.shadow.eclipselauncher.core.MainActivity
import me.shadow.eclipselauncher.core.customcontrols.ControlData
import me.shadow.eclipselauncher.core.customcontrols.ControlLayout
import me.shadow.eclipselauncher.core.customcontrols.handleview.EditControlPopup

import org.lwjgl.glfw.CallbackBridge

@SuppressLint("ViewConstructor", "AppCompatCustomView")
open class ControlButton : TextView, ControlInterface {
    private val mRectPaint = Paint()

    // Internal within the buttons package: the drawer mutates its sub buttons' sizes directly
    internal lateinit var mProperties: ControlData
    private val mControlLayout: ControlLayout

    /* Cache value from the ControlData radius for drawing purposes */
    private var mComputedRadius = 0f

    protected var mIsToggled = false
    protected var mIsPointerOutOfBounds = false

    constructor(layout: ControlLayout, properties: ControlData) : super(layout.context) {
        mControlLayout = layout
        gravity = Gravity.CENTER
        setAllCaps(AllSettings.buttonAllCaps.getValue())
        setTextColor(Color.WHITE)
        setPadding(4, 4, 4, 4)
        setTextSize(14f) // Nullify the default size setting
        setOutlineProvider(null) // Disable shadow casting, removing one drawing pass

        //setOnLongClickListener(this);

        //When a button is created, the width/height has yet to be processed to fit the scaling.
        setProperties(preProcessProperties(properties, layout))

        injectBehaviors()
    }

    override fun getControlView(): View {
        return this
    }

    override fun getProperties(): ControlData {
        return mProperties
    }

    override fun setProperties(properties: ControlData, changePos: Boolean) {
        mProperties = properties
        super<ControlInterface>.setProperties(properties, changePos)
        mComputedRadius = super<ControlInterface>.computeCornerRadius(mProperties.cornerRadius)

        if (mProperties.isToggle) {
            //For the toggle layer
            val value = TypedValue()
            context.theme.resolveAttribute(R.attr.colorAccent, value, true)
            mRectPaint.color = value.data
            mRectPaint.alpha = 128
        } else {
            mRectPaint.color = Color.WHITE
            mRectPaint.alpha = 60
        }

        setText(properties.name)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (mIsToggled || (!mProperties.isToggle && isActivated))
            canvas!!.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), mComputedRadius, mComputedRadius, mRectPaint)
    }

    override fun loadEditValues(editControlPopup: EditControlPopup) {
        editControlPopup.loadValues(getProperties())
    }

    /** Add another instance of the ControlButton to the parent layout */
    override fun cloneButton() {
        val cloneData = ControlData(getProperties())
        cloneData.dynamicX = "0.5 * \${screen_width}"
        cloneData.dynamicY = "0.5 * \${screen_height}"
        (parent as ControlLayout).addControlButton(cloneData)
    }

    /** Remove any trace of this button from the layout */
    override fun removeButton() {
        getControlLayoutParent()!!.getLayout()!!.mControlDataList.remove(getProperties())
        getControlLayoutParent()!!.removeView(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                //Send the event to be taken as a mouse action
                if (getProperties().passThruEnabled && CallbackBridge.isGrabbing()) {
                    val gameSurface = getControlLayoutParent()!!.getGameSurface()
                    gameSurface?.dispatchTouchEvent(event)
                }

                //If out of bounds
                if (event.x < getControlView().left || event.x > getControlView().right ||
                    event.y < getControlView().top || event.y > getControlView().bottom
                ) {
                    if (getProperties().isSwipeable && !mIsPointerOutOfBounds) {
                        //Remove keys
                        if (!triggerToggle()) {
                            sendKeyPresses(false)
                        }
                    }
                    mIsPointerOutOfBounds = true
                    getControlLayoutParent()!!.onTouch(this, event)
                } else {
                    //Else if we now are in bounds
                    if (mIsPointerOutOfBounds) {
                        getControlLayoutParent()!!.onTouch(this, event)
                        //RE-press the button
                        if (getProperties().isSwipeable && !getProperties().isToggle) {
                            sendKeyPresses(true)
                        }
                    }
                    mIsPointerOutOfBounds = false
                }
            }

            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> // 0, 5
                if (!getProperties().isToggle) {
                    sendKeyPresses(true)
                }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> { // 1, 3, 6
                if (getProperties().passThruEnabled) {
                    val gameSurface = getControlLayoutParent()!!.getGameSurface()
                    gameSurface?.dispatchTouchEvent(event)
                }
                if (mIsPointerOutOfBounds) getControlLayoutParent()!!.onTouch(this, event)
                mIsPointerOutOfBounds = false

                if (!triggerToggle()) {
                    sendKeyPresses(false)
                }
            }

            else -> return false
        }

        return super.onTouchEvent(event)
    }

    @Suppress("BooleanMethodIsAlwaysInverted")
    fun triggerToggle(): Boolean {
        //returns true a the toggle system is triggered
        if (mProperties.isToggle) {
            mIsToggled = !mIsToggled
            invalidate()
            sendKeyPresses(mIsToggled)
            return true
        }
        return false
    }

    override fun sendKeyPresses(isDown: Boolean) {
        setActivated(isDown)
        for (keycode in mProperties.keycodes) {
            if (keycode >= LwjglGlfwKeycode.GLFW_KEY_UNKNOWN) {
                CallbackBridge.sendKeyPress(keycode, CallbackBridge.getCurrentMods(), isDown)
                CallbackBridge.setModifiers(keycode, isDown)
            } else {
                sendSpecialKey(keycode, isDown)
            }
        }
    }

    private fun sendSpecialKey(keycode: Int, isDown: Boolean) {
        when (keycode) {
            ControlData.SPECIALBTN_KEYBOARD ->
                if (isDown) MainActivity.switchKeyboardState()

            ControlData.SPECIALBTN_TOGGLECTRL ->
                if (isDown) getControlLayoutParent()!!.toggleControlVisible()

            ControlData.SPECIALBTN_VIRTUALMOUSE ->
                if (isDown) MainActivity.toggleMouse(context)

            ControlData.SPECIALBTN_MOUSEPRI ->
                CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), isDown)

            ControlData.SPECIALBTN_MOUSEMID ->
                CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE.toInt(), isDown)

            ControlData.SPECIALBTN_MOUSESEC ->
                CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), isDown)

            ControlData.SPECIALBTN_SCROLLDOWN ->
                if (!isDown) CallbackBridge.sendScroll(0.0, 1.0)

            ControlData.SPECIALBTN_SCROLLUP ->
                if (!isDown) CallbackBridge.sendScroll(0.0, -1.0)

            ControlData.SPECIALBTN_MENU ->
                mControlLayout.notifyAppMenu()
        }
    }

    override fun hasOverlappingRendering(): Boolean {
        return false
    }
}
