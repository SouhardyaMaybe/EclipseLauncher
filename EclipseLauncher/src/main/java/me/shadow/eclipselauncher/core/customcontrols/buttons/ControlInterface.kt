package me.shadow.eclipselauncher.core.customcontrols.buttons

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

import androidx.annotation.CallSuper
import androidx.annotation.NonNull
import androidx.core.math.MathUtils

import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.setting.AllSettings

import me.shadow.eclipselauncher.core.GrabListener
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.customcontrols.ControlData
import me.shadow.eclipselauncher.core.customcontrols.ControlLayout
import me.shadow.eclipselauncher.core.customcontrols.handleview.EditControlPopup

import org.lwjgl.glfw.CallbackBridge

/**
 * Interface injecting custom behavior to a View.
 * Most of the injected behavior is editing behavior,
 * sending keys has to be implemented by sub classes.
 */
interface ControlInterface : View.OnLongClickListener, GrabListener {
    fun getControlView(): View

    fun getProperties(): ControlData

    fun setProperties(properties: ControlData) {
        setProperties(properties, true)
    }

    /**
     * Remove the button presence from the CustomControl object
     * You need to use {getControlParent()} for this.
     */
    fun removeButton()

    /**
     * Duplicate the data of the button and add a view with the duplicated data
     * Relies on the ControlLayout for the implementation.
     */
    fun cloneButton()

    fun setVisible(isVisible: Boolean) {
        if (getProperties().isHideable)
            getControlView().visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    fun sendKeyPresses(isDown: Boolean)

    /**
     * Load the values and hide non useful forms
     */
    fun loadEditValues(editControlPopup: EditControlPopup)

    override fun onGrabState(isGrabbing: Boolean) {
        if (getControlLayoutParent() != null && getControlLayoutParent()!!.getModifiable()) return // Disable when edited
        setVisible(((getProperties().displayInGame && isGrabbing) || (getProperties().displayInMenu && !isGrabbing)) && getControlLayoutParent()!!.areControlVisible())
    }

    fun getControlLayoutParent(): ControlLayout? {
        return getControlView().parent as ControlLayout?
    }

    /**
     * Apply conversion steps for when the view is created
     */
    fun preProcessProperties(properties: ControlData, layout: ControlLayout): ControlData {
        //Size
        properties.setWidth(properties.getWidth() / layout.getLayoutScale() * AllSettings.buttonScale.getValue().toFloat())
        properties.setHeight(properties.getHeight() / layout.getLayoutScale() * AllSettings.buttonScale.getValue().toFloat())

        //Visibility
        properties.isHideable = !properties.containsKeycode(ControlData.SPECIALBTN_TOGGLECTRL) && !properties.containsKeycode(ControlData.SPECIALBTN_VIRTUALMOUSE)

        return properties
    }

    fun updateProperties() {
        setProperties(getProperties())
    }

    /* This function should be overridden to store the properties */
    @CallSuper
    fun setProperties(properties: ControlData, changePos: Boolean) {
        if (changePos) {
            getControlView().x = properties.insertDynamicPos(getProperties().dynamicX)
            getControlView().y = properties.insertDynamicPos(getProperties().dynamicY)
        }

        // Recycle layout params
        var params = getControlView().layoutParams
        if (params == null)
            params = FrameLayout.LayoutParams(properties.getWidth().toInt(), properties.getHeight().toInt())
        params.width = properties.getWidth().toInt()
        params.height = properties.getHeight().toInt()
        getControlView().layoutParams = params
    }

    /**
     * Apply the background according to properties
     */
    fun setBackground() {
        val gd = if (getControlView().background is GradientDrawable)
            getControlView().background as GradientDrawable
        else GradientDrawable()
        gd.color = getProperties().bgColor
        gd.setStroke(Tools.dpToPx(getProperties().strokeWidth * (getControlLayoutParent()!!.getLayoutScale() / 100f)).toInt(), getProperties().strokeColor)
        gd.cornerRadius = computeCornerRadius(getProperties().cornerRadius)

        getControlView().background = gd
    }

    /**
     * Apply the dynamic equation on the x axis.
     *
     * @param dynamicX The equation to compute the position from
     */
    fun setDynamicX(dynamicX: String) {
        getProperties().dynamicX = dynamicX
        getControlView().x = getProperties().insertDynamicPos(dynamicX)
    }

    /**
     * Apply the dynamic equation on the y axis.
     *
     * @param dynamicY The equation to compute the position from
     */
    fun setDynamicY(dynamicY: String) {
        getProperties().dynamicY = dynamicY
        getControlView().y = getProperties().insertDynamicPos(dynamicY)
    }

    /**
     * Generate a dynamic equation from an absolute position, used to scale properly across devices
     *
     * @param x The absolute position on the horizontal axis
     * @return The equation as a String
     */
    fun generateDynamicX(x: Float): String {
        if (x + getProperties().getWidth() / 2f > CallbackBridge.physicalWidth / 2f) {
            return ((x + getProperties().getWidth()) / CallbackBridge.physicalWidth).toString() + " * \${screen_width} - \${width}"
        } else {
            return (x / CallbackBridge.physicalWidth).toString() + " * \${screen_width}"
        }
    }

    /**
     * Generate a dynamic equation from an absolute position, used to scale properly across devices
     *
     * @param y The absolute position on the vertical axis
     * @return The equation as a String
     */
    fun generateDynamicY(y: Float): String {
        if (y + getProperties().getHeight() / 2f > CallbackBridge.physicalHeight / 2f) {
            return ((y + getProperties().getHeight()) / CallbackBridge.physicalHeight).toString() + " * \${screen_height} - \${height}"
        } else {
            return (y / CallbackBridge.physicalHeight).toString() + " * \${screen_height}"
        }
    }

    /**
     * Regenerate and apply coordinates with supposedly modified properties
     */
    fun regenerateDynamicCoordinates() {
        getProperties().dynamicX = generateDynamicX(getControlView().x)
        getProperties().dynamicY = generateDynamicY(getControlView().y)
        updateProperties()
    }

    /**
     * Do a pre-conversion of an equation using values from a button,
     * so the variables can be used for another button
     * <p>
     * Internal use only.
     *
     * @param equation The dynamic position as a String
     * @param button   The button to get the values from.
     * @return The pre-processed equation as a String.
     */
    fun applySize(equation: String, button: ControlInterface): String {
        return equation
            .replace("\${right}", "(\${screen_width} - \${width})")
            .replace("\${bottom}", "(\${screen_height} - \${height})")
            .replace("\${height}", "(px(" + Tools.pxToDp(button.getProperties().getHeight()) + ") /" + AllSettings.buttonScale.getValue() + " * \${preferred_scale})")
            .replace("\${width}", "(px(" + Tools.pxToDp(button.getProperties().getWidth()) + ") / " + AllSettings.buttonScale.getValue() + " * \${preferred_scale})")
    }

    /**
     * Convert a corner radius percentage into a px corner radius
     */
    fun computeCornerRadius(radiusInPercent: Float): Float {
        val minSize = Math.min(getProperties().getWidth(), getProperties().getHeight())
        return (minSize / 2) * (radiusInPercent / 100)
    }

    /**
     * Passe a series of checks to determine if the ControlButton isn't available to be snapped on.
     *
     * @param button The button to check
     * @return whether or not the button
     */
    @Suppress("BooleanMethodIsAlwaysInverted")
    fun canSnap(button: ControlInterface): Boolean {
        if (!AllSettings.buttonSnapping.getValue()) return false

        val MIN_DISTANCE = getSnapDistance()

        if (button === this) return false
        if (button.getControlView().visibility == View.GONE) return false
        return !(me.shadow.eclipselauncher.core.utils.MathUtils.dist(
            button.getControlView().x + button.getControlView().width / 2f,
            button.getControlView().y + button.getControlView().height / 2f,
            getControlView().x + getControlView().width / 2f,
            getControlView().y + getControlView().height / 2f
        )
                > Math.max(button.getControlView().width / 2f + getControlView().width / 2f,
            button.getControlView().height / 2f + getControlView().height / 2f) + MIN_DISTANCE)
    }

    /**
     * Try to snap, then align to neighboring buttons, given the provided coordinates.
     * The new position is automatically applied to the View,
     * regardless of if the View snapped or not.
     * <p>
     * The new position is always dynamic, thus replacing previous dynamic positions
     *
     * @param x Coordinate on the x axis
     * @param y Coordinate on the y axis
     */
    fun snapAndAlign(x: Float, y: Float) {
        val MIN_DISTANCE = getSnapDistance()
        var dynamicX = generateDynamicX(x)
        var dynamicY = generateDynamicY(y)

        getControlView().x = x
        getControlView().y = y

        for (button in (getControlView().parent as ControlLayout).getButtonChildren()) {
            //Step 1: Filter unwanted buttons
            if (!canSnap(button)) continue

            //Step 2: Get Coordinates
            val button_top = button.getControlView().y
            val button_bottom = button_top + button.getControlView().height
            val button_left = button.getControlView().x
            val button_right = button_left + button.getControlView().width

            val top = getControlView().y
            val bottom = getControlView().y + getControlView().height
            val left = getControlView().x
            val right = getControlView().x + getControlView().width

            //Step 3: For each axis, we try to snap to the nearest
            if (Math.abs(top - button_bottom) < MIN_DISTANCE) { // Bottom snap
                dynamicY = applySize(button.getProperties().dynamicY, button) + applySize(" + \${height}", button) + " + \${margin}"
            } else if (Math.abs(button_top - bottom) < MIN_DISTANCE) { //Top snap
                dynamicY = applySize(button.getProperties().dynamicY, button) + " - \${height} - \${margin}"
            }
            if (dynamicY != generateDynamicY(getControlView().y)) { //If we snapped
                if (Math.abs(button_left - left) < MIN_DISTANCE) { //Left align snap
                    dynamicX = applySize(button.getProperties().dynamicX, button)
                } else if (Math.abs(button_right - right) < MIN_DISTANCE) { //Right align snap
                    dynamicX = applySize(button.getProperties().dynamicX, button) + applySize(" + \${width}", button) + " - \${width}"
                }
            }

            if (Math.abs(button_left - right) < MIN_DISTANCE) { //Left snap
                dynamicX = applySize(button.getProperties().dynamicX, button) + " - \${width} - \${margin}"
            } else if (Math.abs(left - button_right) < MIN_DISTANCE) { //Right snap
                dynamicX = applySize(button.getProperties().dynamicX, button) + applySize(" + \${width}", button) + " + \${margin}"
            }
            if (dynamicX != generateDynamicX(getControlView().x)) { //If we snapped
                if (Math.abs(button_top - top) < MIN_DISTANCE) { //Top align snap
                    dynamicY = applySize(button.getProperties().dynamicY, button)
                } else if (Math.abs(button_bottom - bottom) < MIN_DISTANCE) { //Bottom align snap
                    dynamicY = applySize(button.getProperties().dynamicY, button) + applySize(" + \${height}", button) + " - \${height}"
                }
            }
        }

        setDynamicX(dynamicX)
        setDynamicY(dynamicY)
    }

    /**
     * Wrapper for multiple injections at once
     */
    fun injectBehaviors() {
        injectProperties()
        injectTouchEventBehavior()
        injectLayoutParamBehavior()
        injectGrabListenerBehavior()
    }

    /**
     * Inject the grab listener, remove it when the view is gone
     */
    fun injectGrabListenerBehavior() {
        if (getControlView() == null) {
            Logging.e(ControlInterface::class.toString(), "Failed to inject grab listener behavior !")
            return
        }

        getControlView().addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(@NonNull v: View) {
                CallbackBridge.addGrabListener(this@ControlInterface)
            }

            override fun onViewDetachedFromWindow(@NonNull v: View) {
                getControlView().removeOnAttachStateChangeListener(this)
                CallbackBridge.removeGrabListener(this@ControlInterface)
            }
        })
    }

    fun injectProperties() {
        getControlView().post { getControlView().translationZ = 10f }
    }

    /**
     * Inject a touch listener on the view to make editing controls straight forward
     */
    fun injectTouchEventBehavior() {
        getControlView().setOnTouchListener(object : View.OnTouchListener {
            private var mCanTriggerLongClick = true
            private var downX = 0f
            private var downY = 0f
            private var downRawX = 0f
            private var downRawY = 0f

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                if (!getControlLayoutParent()!!.getModifiable()) {
                    // Basically, editing behavior is forced while in game behavior is specific
                    view.onTouchEvent(event)
                    return true
                }

                /* If the button can be modified/moved */
                //Instantiate the gesture detector only when needed

                if (event.actionMasked == MotionEvent.ACTION_UP && mCanTriggerLongClick) {
                    //TODO change this.
                    onLongClick(view)
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        mCanTriggerLongClick = true
                        downRawX = event.rawX
                        downRawY = event.rawY
                        downX = downRawX - view.x
                        downY = downRawY - view.y
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (Math.abs(event.rawX - downRawX) > 8 || Math.abs(event.rawY - downRawY) > 8)
                            mCanTriggerLongClick = false
                        getControlLayoutParent()!!.adaptPanelPosition()
                        snapAndAlign(
                            MathUtils.clamp(event.rawX - downX, 0f, (CallbackBridge.physicalWidth - view.width).toFloat()),
                            MathUtils.clamp(event.rawY - downY, 0f, (CallbackBridge.physicalHeight - view.height).toFloat())
                        )
                    }
                }

                return true
            }
        })
    }

    fun injectLayoutParamBehavior() {
        getControlView().addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            getProperties().setWidth((right - left).toFloat())
            getProperties().setHeight((bottom - top).toFloat())
            setBackground()

            // Re-calculate position
            getControlView().x = getControlView().x
            getControlView().y = getControlView().y
        }
    }

    override fun onLongClick(v: View?): Boolean {
        if (getControlLayoutParent()!!.getModifiable()) {
            getControlLayoutParent()!!.editControlButton(this)
            getControlLayoutParent()!!.mActionRow!!.setFollowedButton(this)
        }

        return true
    }

    companion object {
        @JvmStatic
        fun getSnapDistance(): Float {
            return Tools.dpToPx(AllSettings.buttonSnappingDistance.getValue().toFloat())
        }

        @JvmStatic
        fun getMarginDistance(): Float {
            return Tools.dpToPx(2f)
        }
    }
}
