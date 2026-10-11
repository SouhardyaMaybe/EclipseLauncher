package me.shadow.eclipselauncher.core.customcontrols.buttons

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup

import me.shadow.eclipselauncher.core.customcontrols.ControlData
import me.shadow.eclipselauncher.core.customcontrols.ControlDrawerData
import me.shadow.eclipselauncher.core.customcontrols.ControlLayout
import me.shadow.eclipselauncher.core.customcontrols.handleview.EditControlPopup

@SuppressLint("ViewConstructor")
class ControlSubButton : ControlButton {

    @JvmField
    val parentDrawer: ControlDrawer

    constructor(layout: ControlLayout, properties: ControlData, parentDrawer: ControlDrawer) : super(layout, properties) {
        this.parentDrawer = parentDrawer
        filterProperties()
    }

    private fun filterProperties() {
        if (parentDrawer != null && parentDrawer.drawerData.orientation != ControlDrawerData.Orientation.FREE) {
            mProperties.setHeight(parentDrawer.getProperties().getHeight())
            mProperties.setWidth(parentDrawer.getProperties().getWidth())
        }

        setProperties(mProperties, false)
    }

    // STUB, visibility handled by the ControlDrawer
    //setVisibility(isVisible ? VISIBLE : (!mProperties.isHideable && parentDrawer.getVisibility() == GONE) ? VISIBLE : View.GONE);
    override fun setVisible(isVisible: Boolean) {
    }

    // STUB, visibility lifecycle handled by the ControlDrawer
    override fun onGrabState(isGrabbing: Boolean) {
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        if (parentDrawer != null && parentDrawer.drawerData.orientation != ControlDrawerData.Orientation.FREE) {
            params!!.width = parentDrawer.mProperties.getWidth().toInt()
            params!!.height = parentDrawer.mProperties.getHeight().toInt()
        }
        super.setLayoutParams(params)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!getControlLayoutParent()!!.getModifiable() || parentDrawer.drawerData.orientation == ControlDrawerData.Orientation.FREE) {
            return super.onTouchEvent(event)
        }

        if (event.actionMasked == MotionEvent.ACTION_UP) {
            //mCanTriggerLongClick = true;
            onLongClick(this)
        }
        return true
    }

    override fun cloneButton() {
        val cloneData = ControlData(getProperties())
        cloneData.dynamicX = "0.5 * \${screen_width}"
        cloneData.dynamicY = "0.5 * \${screen_height}"
        (parent as ControlLayout).addSubButton(parentDrawer, cloneData)
    }

    override fun removeButton() {
        parentDrawer.drawerData.buttonProperties.remove(getProperties())
        parentDrawer.drawerData.buttonProperties.remove(getProperties())
        parentDrawer.buttons.remove(this)

        parentDrawer.syncButtons()

        super.removeButton()
    }

    override fun snapAndAlign(x: Float, y: Float) {
        if (parentDrawer.drawerData.orientation == ControlDrawerData.Orientation.FREE)
            super.snapAndAlign(x, y)
        // Else the button is forced into place
    }

    override fun loadEditValues(editControlPopup: EditControlPopup) {
        editControlPopup.loadSubButtonValues(getProperties(), parentDrawer.drawerData.orientation)
    }
}
