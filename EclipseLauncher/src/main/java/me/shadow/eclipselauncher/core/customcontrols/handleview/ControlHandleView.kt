package me.shadow.eclipselauncher.core.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver

import androidx.annotation.Nullable
import androidx.core.content.res.ResourcesCompat

import me.shadow.eclipselauncher.R

import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlInterface

class ControlHandleView : View {

    private val mDrawable: Drawable =
        ResourcesCompat.getDrawable(resources, R.drawable.ic_view_handle, context.theme)!!
    private var mView: ControlInterface? = null
    private var mXOffset = 0f
    private var mYOffset = 0f
    private val mPositionListener: ViewTreeObserver.OnPreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (mView == null || !mView!!.getControlView().isShown) {
                hide()
                return true
            }

            x = mView!!.getControlView().x + mView!!.getControlView().width
            y = mView!!.getControlView().y + mView!!.getControlView().height
            return true
        }
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        val size = resources.getDimensionPixelOffset(R.dimen._22sdp)
        mDrawable.setBounds(0, 0, size, size)
        val params = ViewGroup.LayoutParams(size, size)
        layoutParams = params
        background = mDrawable
        setTranslationZ(10.5f)
    }

    fun setControlButton(controlInterface: ControlInterface) {
        val view = mView
        if (view != null) view.getControlView().viewTreeObserver.removeOnPreDrawListener(mPositionListener)

        visibility = View.VISIBLE
        mView = controlInterface
        mView!!.getControlView().viewTreeObserver.addOnPreDrawListener(mPositionListener)

        x = controlInterface.getControlView().x + controlInterface.getControlView().width
        y = controlInterface.getControlView().y + controlInterface.getControlView().height
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mXOffset = event.x
                mYOffset = event.y
            }

            MotionEvent.ACTION_MOVE -> {
                x = x + event.x - mXOffset
                y = y + event.y - mYOffset

                println(x - mView!!.getControlView().x)
                println(y - mView!!.getControlView().y)

                mView!!.getProperties().setWidth(x - mView!!.getControlView().x)
                mView!!.getProperties().setHeight(y - mView!!.getControlView().y)
                mView!!.regenerateDynamicCoordinates()
            }
        }

        return true
    }

    fun hide() {
        val view = mView
        if (view != null) view.getControlView().viewTreeObserver.removeOnPreDrawListener(mPositionListener)
        visibility = View.GONE
    }
}
