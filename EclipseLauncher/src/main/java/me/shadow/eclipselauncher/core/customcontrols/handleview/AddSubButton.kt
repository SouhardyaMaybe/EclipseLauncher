package me.shadow.eclipselauncher.core.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet

import androidx.annotation.Nullable

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.ui.view.AnimButton

import me.shadow.eclipselauncher.core.customcontrols.ControlData
import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlDrawer
import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlInterface

@SuppressLint("AppCompatCustomView")
class AddSubButton : AnimButton, ActionButtonInterface {

    private var mCurrentlySelectedButton: ControlInterface? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    override fun init() {
        setOnClickListener(this)
        setText(R.string.customctrl_addsubbutton)
    }

    override fun shouldBeVisible(): Boolean {
        return mCurrentlySelectedButton != null && mCurrentlySelectedButton is ControlDrawer
    }

    override fun setFollowedView(view: ControlInterface?) {
        mCurrentlySelectedButton = view
    }

    override fun onClick() {
        val button = mCurrentlySelectedButton
        if (button is ControlDrawer) {
            button.getControlLayoutParent()!!.addSubButton(
                button,
                ControlData(context.getString(R.string.controls_add_control_button))
            )
        }
    }
}
