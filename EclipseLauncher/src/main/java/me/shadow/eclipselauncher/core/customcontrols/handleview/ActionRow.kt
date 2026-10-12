package me.shadow.eclipselauncher.core.customcontrols.handleview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout

import androidx.annotation.Nullable
import androidx.core.math.MathUtils

import me.shadow.eclipselauncher.R

import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlInterface

/**
 * Layout floating around a Control Button, displaying contextual actions
 */
class ActionRow : LinearLayout {

    companion object {
        const val SIDE_LEFT = 0x0
        const val SIDE_TOP = 0x1
        const val SIDE_RIGHT = 0x2
        const val SIDE_BOTTOM = 0x3
        const val SIDE_AUTO = 0x4
    }

    @JvmField
    val mFollowedViewListener: ViewTreeObserver.OnPreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (mFollowedView == null || !mFollowedView!!.isShown) {
                hide()
                return true
            }

            setNewPosition()
            return true
        }
    }

    private val actionButtons = arrayOfNulls<ActionButtonInterface>(3)
    private var mFollowedView: View? = null
    private val mSide = SIDE_AUTO

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    /** Add action buttons and configure them */
    private fun init() {
        setTranslationZ(11f)
        visibility = View.GONE
        setOrientation(HORIZONTAL)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            resources.getDimensionPixelOffset(R.dimen._40sdp)
        )

        actionButtons[0] = DeleteButton(context)
        actionButtons[1] = CloneButton(context)
        actionButtons[2] = AddSubButton(context)

        // This is not pretty code, don't do this.
        for (buttonInterface in actionButtons) {
            val button = buttonInterface as View
            addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }

        setElevation(5f)
    }

    fun setFollowedButton(controlInterface: ControlInterface?) {
        if (mFollowedView != null)
            mFollowedView!!.viewTreeObserver.removeOnPreDrawListener(mFollowedViewListener)

        for (buttonInterface in actionButtons) {
            buttonInterface!!.setFollowedView(controlInterface)
            (buttonInterface as View).visibility = if (buttonInterface.shouldBeVisible()) View.VISIBLE else View.GONE
        }

        visibility = View.VISIBLE
        mFollowedView = controlInterface as View?
        if (mFollowedView != null)
            mFollowedView!!.viewTreeObserver.addOnPreDrawListener(mFollowedViewListener)
    }

    private fun getXPosition(side: Int): Float {
        return if (side == SIDE_LEFT) {
            mFollowedView!!.x - width
        } else if (side == SIDE_RIGHT) {
            mFollowedView!!.x + mFollowedView!!.width
        } else {
            mFollowedView!!.x + mFollowedView!!.width / 2f - width / 2f
        }
    }

    private fun getYPosition(side: Int): Float {
        return if (side == SIDE_TOP) {
            mFollowedView!!.y - height
        } else if (side == SIDE_BOTTOM) {
            mFollowedView!!.y + mFollowedView!!.height
        } else {
            mFollowedView!!.y + mFollowedView!!.height / 2f - height / 2f
        }
    }

    private fun setNewPosition() {
        if (mFollowedView == null) return
        val side = pickSide()

        x = MathUtils.clamp(getXPosition(side), 0f, (Tools.currentDisplayMetrics.widthPixels - width).toFloat())
        y = getYPosition(side)
    }

    private fun pickSide(): Int {
        if (mFollowedView == null) return mSide //Value should not matter

        if (mSide != SIDE_AUTO) return mSide
        //TODO improve the "algo"
        val parent = mFollowedView!!.parent as ViewGroup?
        if (parent == null) return mSide //Value should not matter

        var side = SIDE_TOP
        val futurePos = getYPosition(side)
        if (futurePos + height > (parent.height + height / 2f)) {
            side = SIDE_TOP
        } else if (futurePos < -height / 2f) {
            side = SIDE_BOTTOM
        }

        return side
    }

    fun hide() {
        if (mFollowedView != null)
            mFollowedView!!.viewTreeObserver.removeOnPreDrawListener(mFollowedViewListener)
        visibility = View.GONE
    }
}
