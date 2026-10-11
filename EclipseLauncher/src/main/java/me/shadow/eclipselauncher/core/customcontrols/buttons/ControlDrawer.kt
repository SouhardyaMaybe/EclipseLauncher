package me.shadow.eclipselauncher.core.customcontrols.buttons

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

import me.shadow.eclipselauncher.core.customcontrols.ControlData
import me.shadow.eclipselauncher.core.customcontrols.ControlDrawerData
import me.shadow.eclipselauncher.core.customcontrols.ControlLayout
import me.shadow.eclipselauncher.core.customcontrols.handleview.EditControlPopup

import java.util.ArrayList

@SuppressLint("ViewConstructor")
class ControlDrawer : ControlButton {

    @JvmField
    val buttons: ArrayList<ControlSubButton>
    @JvmField
    val drawerData: ControlDrawerData
    @JvmField
    val parentLayout: ControlLayout
    @JvmField
    var areButtonsVisible: Boolean = false

    constructor(layout: ControlLayout, drawerData: ControlDrawerData) : super(layout, drawerData.properties) {
        buttons = ArrayList(drawerData.buttonProperties.size)
        this.parentLayout = layout
        this.drawerData = drawerData
        areButtonsVisible = layout.getModifiable()
    }

    fun addButton(properties: ControlData) {
        addButton(ControlSubButton(parentLayout, properties, this))
    }

    fun addButton(button: ControlSubButton) {
        buttons.add(button)
        syncButtons()
        setControlButtonVisibility(button, areButtonsVisible)
    }

    private fun setControlButtonVisibility(button: ControlButton, isVisible: Boolean) {
        button.getControlView().visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun switchButtonVisibility() {
        areButtonsVisible = !areButtonsVisible
        val visibility = if (areButtonsVisible) View.VISIBLE else View.GONE
        for (button in buttons) {
            button.getControlView().visibility = visibility
        }
    }

    //Syncing stuff
    private fun alignButtons() {
        if (buttons == null) return
        if (drawerData.orientation == ControlDrawerData.Orientation.FREE) return
        val margin = ControlInterface.getMarginDistance().toInt()

        for (i in buttons.indices) {
            when (drawerData.orientation) {
                ControlDrawerData.Orientation.RIGHT -> {
                    buttons[i].setDynamicX(generateDynamicX(x + (drawerData.properties.getWidth() + margin) * (i + 1)))
                    buttons[i].setDynamicY(generateDynamicY(y))
                }

                ControlDrawerData.Orientation.LEFT -> {
                    buttons[i].setDynamicX(generateDynamicX(x - (drawerData.properties.getWidth() + margin) * (i + 1)))
                    buttons[i].setDynamicY(generateDynamicY(y))
                }

                ControlDrawerData.Orientation.UP -> {
                    buttons[i].setDynamicY(generateDynamicY(y - (drawerData.properties.getHeight() + margin) * (i + 1)))
                    buttons[i].setDynamicX(generateDynamicX(x))
                }

                ControlDrawerData.Orientation.DOWN -> {
                    buttons[i].setDynamicY(generateDynamicY(y + (drawerData.properties.getHeight() + margin) * (i + 1)))
                    buttons[i].setDynamicX(generateDynamicX(x))
                }
            }
            buttons[i].updateProperties()
        }
    }

    private fun resizeButtons() {
        if (buttons == null || drawerData.orientation == ControlDrawerData.Orientation.FREE) return
        for (subButton in buttons) {
            subButton.mProperties.setWidth(mProperties.getWidth())
            subButton.mProperties.setHeight(mProperties.getHeight())

            subButton.updateProperties()
        }
    }

    fun syncButtons() {
        alignButtons()
        resizeButtons()
    }

    /**
     * Check whether or not the button passed as a parameter belongs to this drawer.
     *
     * @param button The button to look for
     * @return Whether the button is in the buttons list of the drawer.
     */
    fun containsChild(button: ControlInterface): Boolean {
        for (childButton in buttons) {
            if (childButton === button) return true
        }
        return false
    }

    override fun preProcessProperties(properties: ControlData, layout: ControlLayout): ControlData {
        val data = super<ControlInterface>.preProcessProperties(properties, layout)
        data.isHideable = true
        return data
    }

    override fun setVisible(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        this.visibility = visibility
        if (visibility == View.GONE || areButtonsVisible) {
            for (button in buttons) {
                button.getControlView().visibility =
                    if (isVisible) View.VISIBLE else if (!mProperties.isHideable && this.visibility == View.GONE) View.VISIBLE else View.GONE
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!getControlLayoutParent()!!.getModifiable()) {
            when (event.actionMasked) {
                MotionEvent.ACTION_UP, // 1
                MotionEvent.ACTION_POINTER_UP -> // 6
                    switchButtonVisibility()
            }
            return true
        }

        return super.onTouchEvent(event)
    }

    override fun setX(x: Float) {
        super.setX(x)
        alignButtons()
    }

    override fun setY(y: Float) {
        super.setY(y)
        alignButtons()
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        syncButtons()
    }

    override fun canSnap(button: ControlInterface): Boolean {
        val result = super<ControlInterface>.canSnap(button)
        return result && !containsChild(button)
    }

    //Getters
    fun getDrawerData(): ControlDrawerData {
        return drawerData
    }

    override fun loadEditValues(editControlPopup: EditControlPopup) {
        editControlPopup.loadValues(drawerData)
    }

    override fun cloneButton() {
        val cloneData = ControlDrawerData(getDrawerData())
        cloneData.properties.dynamicX = "0.5 * \${screen_width}"
        cloneData.properties.dynamicY = "0.5 * \${screen_height}"
        (parent as ControlLayout).addDrawer(cloneData)
    }

    override fun removeButton() {
        val layout = getControlLayoutParent()!!
        for (subButton in buttons) {
            layout.removeView(subButton)
        }

        layout.getLayout()!!.mDrawerDataList.remove(getDrawerData())
        layout.removeView(this)
    }
}
