package me.shadow.eclipselauncher.core.customcontrols.handleview

import android.view.View

import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlInterface

/** Interface defining the behavior of action buttons */
interface ActionButtonInterface : View.OnClickListener {

    /** HAS TO BE CALLED BY THE CONSTRUCTOR */
    fun init()

    /** Called when the button should be made aware of the current target */
    fun setFollowedView(view: ControlInterface)

    /** Called when the button action should be executed on the target */
    fun onClick()

    /** Whether the button should be shown, given the current contextual information that it has */
    fun shouldBeVisible(): Boolean

    // Wrapper to remove the arg
    override fun onClick(v: View?) {
        onClick()
    }
}
