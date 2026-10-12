package me.shadow.eclipselauncher.core.customcontrols.handleview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet

import androidx.annotation.Nullable

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.ui.view.AnimButton

import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlInterface

@SuppressLint("AppCompatCustomView")
class DeleteButton : AnimButton, ActionButtonInterface {

    private var mCurrentlySelectedButton: ControlInterface? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    override fun init() {
        setOnClickListener(this)
        setText(R.string.generic_delete)
    }

    override fun shouldBeVisible(): Boolean {
        return mCurrentlySelectedButton != null
    }

    override fun setFollowedView(view: ControlInterface?) {
        mCurrentlySelectedButton = view
    }

    override fun onClick() {
        if (mCurrentlySelectedButton == null) return

        mCurrentlySelectedButton!!.removeButton()
    }
}
