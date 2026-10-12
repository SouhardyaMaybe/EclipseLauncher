package me.shadow.eclipselauncher.core.customcontrols.handleview

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView

import androidx.constraintlayout.widget.ConstraintLayout

import me.shadow.eclipselauncher.widget.DefocusableScrollView
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.listener.SimpleTextWatcher
import me.shadow.eclipselauncher.ui.dialog.KeyboardDialog

import me.shadow.eclipselauncher.core.EfficientAndroidLWJGLKeycode
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.colorselector.ColorSelector
import me.shadow.eclipselauncher.core.customcontrols.ControlData
import me.shadow.eclipselauncher.core.customcontrols.ControlDrawerData
import me.shadow.eclipselauncher.core.customcontrols.ControlJoystickData
import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlDrawer
import me.shadow.eclipselauncher.core.customcontrols.buttons.ControlInterface

/**
 * Class providing a sort of popup on top of a Layout, allowing to edit a given ControlButton
 */
class EditControlPopup {
    private val context: Context
    protected val mKeycodeSpinners = arrayOfNulls<Spinner>(4)
    private lateinit var keyboardDialog: KeyboardDialog
    private val mScrollView: DefocusableScrollView
    private val mColorSelector: ColorSelector

    private val mEditPopupAnimator: ObjectAnimator
    private val mColorEditorAnimator: ObjectAnimator
    private val mMargin: Int

    @JvmField
    var internalChanges: Boolean = false // True when we programmatically change stuff.

    private val mLayoutChangedListener = object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View?, left: Int, top: Int, right: Int, bottom: Int,
            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
        ) {
            if (internalChanges) return

            internalChanges = true
            val width = safeParseFloat(mWidthEditText.text.toString()).toInt()

            if (width >= 0 && Math.abs(right - width) > 1) {
                mWidthEditText.setText((right - left).toString())
            }
            val height = safeParseFloat(mHeightEditText.text.toString()).toInt()
            if (height >= 0 && Math.abs(bottom - height) > 1) {
                mHeightEditText.setText((bottom - top).toString())
            }

            internalChanges = false
        }
    }

    protected lateinit var mNameEditText: EditText
    protected lateinit var mWidthEditText: EditText
    protected lateinit var mHeightEditText: EditText

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    protected lateinit var mToggleSwitch: Switch
    protected lateinit var mPassthroughSwitch: Switch
    protected lateinit var mSwipeableSwitch: Switch
    protected lateinit var mForwardLockSwitch: Switch
    protected lateinit var mAbsoluteTrackingSwitch: Switch

    protected lateinit var mOrientationSpinner: Spinner
    protected var mKeycodeTextviews = arrayOfNulls<TextView>(4)
    protected lateinit var mStrokeWidthSeekbar: SeekBar
    protected lateinit var mCornerRadiusSeekbar: SeekBar
    protected lateinit var mAlphaSeekbar: SeekBar
    protected lateinit var mStrokePercentTextView: TextView
    protected lateinit var mCornerRadiusPercentTextView: TextView
    protected lateinit var mAlphaPercentTextView: TextView
    protected lateinit var mSelectBackgroundColor: TextView
    protected lateinit var mSelectStrokeColor: TextView
    protected lateinit var mAdapter: ArrayAdapter<String>
    protected lateinit var mSpecialArray: List<String>
    protected lateinit var mDisplayInGameCheckbox: CheckBox
    protected lateinit var mDisplayInMenuCheckbox: CheckBox
    private lateinit var mRootView: ConstraintLayout
    private var mDisplaying = false
    private var mDisplayingColor = false
    private var mCurrentlyEditedButton: ControlInterface? = null

    // Decorative textviews
    private lateinit var mOrientationTextView: TextView
    private lateinit var mMappingTextView: TextView
    private lateinit var mNameTextView: TextView
    private lateinit var mCornerRadiusTextView: TextView
    private lateinit var mVisibilityTextView: TextView
    private lateinit var mSizeTextview: TextView
    private lateinit var mSizeXTextView: TextView

    constructor(context: Context, parent: ViewGroup) {
        this.context = context

        mScrollView = LayoutInflater.from(context).inflate(R.layout.dialog_control_button_setting, parent, false) as DefocusableScrollView
        parent.addView(mScrollView)

        mMargin = context.resources.getDimensionPixelOffset(R.dimen._20sdp)

        mColorSelector = ColorSelector(context, parent, null)
        mColorSelector.getRootView().elevation = 11f
        mColorSelector.getRootView().translationZ = 11f
        mColorSelector.getRootView().x = -context.resources.getDimensionPixelOffset(R.dimen._280sdp).toFloat()

        mEditPopupAnimator = ObjectAnimator.ofFloat(mScrollView, "x", 0f).setDuration(600L)
        mColorEditorAnimator = ObjectAnimator.ofFloat(mColorSelector.getRootView(), "x", 0f).setDuration(600L)
        val decelerate: Interpolator = AccelerateDecelerateInterpolator()
        mEditPopupAnimator.interpolator = decelerate
        mColorEditorAnimator.interpolator = decelerate

        mScrollView.elevation = 10f
        mScrollView.translationZ = 10f
        mScrollView.x = -context.resources.getDimensionPixelOffset(R.dimen._280sdp).toFloat()

        bindLayout()
        loadAdapter()

        setupRealTimeListeners()
    }

    /**
     * Slide the layout into the visible screen area
     */
    fun appear(fromRight: Boolean) {
        disappearColor() // When someone jumps from a button to another

        if (fromRight) {
            if (!mDisplaying || !isAtRight()) {
                mEditPopupAnimator.setFloatValues(
                    Tools.currentDisplayMetrics.widthPixels.toFloat(),
                    (Tools.currentDisplayMetrics.widthPixels - mScrollView.width - mMargin).toFloat()
                )
                mEditPopupAnimator.start()
            }
        } else {
            if (!mDisplaying || isAtRight()) {
                mEditPopupAnimator.setFloatValues((-mScrollView.width).toFloat(), mMargin.toFloat())
                mEditPopupAnimator.start()
            }
        }

        mDisplaying = true
    }

    /**
     * Slide out the layout
     */
    fun disappear() {
        if (!mDisplaying) return

        mDisplaying = false
        if (isAtRight())
            mEditPopupAnimator.setFloatValues(
                (Tools.currentDisplayMetrics.widthPixels - mScrollView.width - mMargin).toFloat(),
                Tools.currentDisplayMetrics.widthPixels.toFloat()
            )
        else
            mEditPopupAnimator.setFloatValues(mMargin.toFloat(), (-mScrollView.width).toFloat())

        mEditPopupAnimator.start()
    }

    /**
     * Slide the layout into the visible screen area
     */
    fun appearColor(fromRight: Boolean, color: Int) {
        if (fromRight) {
            if (!mDisplayingColor || !isAtRight()) {
                mColorEditorAnimator.setFloatValues(
                    Tools.currentDisplayMetrics.widthPixels.toFloat(),
                    (Tools.currentDisplayMetrics.widthPixels - mScrollView.width - mMargin).toFloat()
                )
                mColorEditorAnimator.start()
            }
        } else {
            if (!mDisplayingColor || isAtRight()) {
                mColorEditorAnimator.setFloatValues((-mScrollView.width).toFloat(), mMargin.toFloat())
                mColorEditorAnimator.start()
            }
        }

        // Adjust the color selector to have the same size as the control settings
        val params = mColorSelector.getRootView().layoutParams
        params.height = mScrollView.height
        mColorSelector.getRootView().layoutParams = params

        mDisplayingColor = true
        mColorSelector.show(if (color == -1) Color.WHITE else color)
    }

    /**
     * Slide out the layout
     */
    fun disappearColor() {
        if (!mDisplayingColor) return

        mDisplayingColor = false
        if (isAtRight())
            mColorEditorAnimator.setFloatValues(
                (Tools.currentDisplayMetrics.widthPixels - mScrollView.width - mMargin).toFloat(),
                Tools.currentDisplayMetrics.widthPixels.toFloat()
            )
        else
            mColorEditorAnimator.setFloatValues(mMargin.toFloat(), (-mScrollView.width).toFloat())

        mColorEditorAnimator.start()
    }

    /**
     * Slide out the first visible layer.
     *
     * @return True if the last layer is disappearing
     */
    fun disappearLayer(): Boolean {
        return if (mDisplayingColor) {
            disappearColor()
            false
        } else {
            disappear()
            true
        }
    }

    /**
     * Switch the panels position if needed
     */
    fun adaptPanelPosition() {
        if (mDisplaying) {
            val isAtRight = mCurrentlyEditedButton!!.getControlView().x + mCurrentlyEditedButton!!.getControlView().width / 2f < Tools.currentDisplayMetrics.widthPixels / 2f
            appear(isAtRight)
        }
    }

    fun destroy() {
        (mScrollView.parent as ViewGroup).removeView(mColorSelector.getRootView())
        (mScrollView.parent as ViewGroup).removeView(mScrollView)
    }

    private fun loadAdapter() {
        //Initialize adapter for keycodes
        mAdapter = ArrayAdapter(context, R.layout.item_centered_textview)
        mSpecialArray = ControlData.buildSpecialButtonArray(context)

        mAdapter.addAll(mSpecialArray)
        // addAll(vararg) mirrors the Java call element by element
        for (keyName in EfficientAndroidLWJGLKeycode.generateKeyName()) {
            mAdapter.add(keyName)
        }
        mAdapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice)

        for (spinner in mKeycodeSpinners) {
            spinner!!.setAdapter(mAdapter)
        }

        // Orientation spinner
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item)
        adapter.addAll(*ControlDrawerData.getOrientations(context))
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice)

        mOrientationSpinner.setAdapter(adapter)
    }

    private fun setDefaultVisibilitySetting() {
        for (i in 0 until mRootView.childCount) {
            mRootView.getChildAt(i)!!.visibility = View.VISIBLE
        }
        for (s in mKeycodeSpinners) {
            s!!.visibility = View.INVISIBLE
        }
    }

    private fun isAtRight(): Boolean {
        return mScrollView.x > Tools.currentDisplayMetrics.widthPixels / 2f
    }

    /* LOADING VALUES */

    /**
     * Load values for basic control data
     */
    fun loadValues(data: ControlData) {
        setDefaultVisibilitySetting()
        mOrientationTextView.visibility = View.GONE
        mOrientationSpinner.visibility = View.GONE
        mForwardLockSwitch.visibility = View.GONE
        mAbsoluteTrackingSwitch.visibility = View.GONE

        mNameEditText.setText(data.name)
        mWidthEditText.setText(data.getWidth().toString())
        mHeightEditText.setText(data.getHeight().toString())

        mAlphaSeekbar.setProgress((data.opacity * 100).toInt())
        mStrokeWidthSeekbar.setProgress(data.strokeWidth.toInt() * 10)
        mCornerRadiusSeekbar.setProgress(data.cornerRadius.toInt())

        setPercentageText(mAlphaPercentTextView, (data.opacity * 100).toInt())
        setPercentageText(mStrokePercentTextView, data.strokeWidth.toInt() * 10)
        setPercentageText(mCornerRadiusPercentTextView, data.cornerRadius.toInt())

        mToggleSwitch.setChecked(data.isToggle)
        mPassthroughSwitch.setChecked(data.passThruEnabled)
        mSwipeableSwitch.setChecked(data.isSwipeable)

        mDisplayInGameCheckbox.setChecked(data.displayInGame)
        mDisplayInMenuCheckbox.setChecked(data.displayInMenu)

        for (i in data.keycodes.indices) {
            if (data.keycodes[i] < 0) {
                mKeycodeSpinners[i]!!.setSelection(data.keycodes[i] + mSpecialArray.size)
            } else {
                mKeycodeSpinners[i]!!.setSelection(EfficientAndroidLWJGLKeycode.getIndexByValue(data.keycodes[i]) + mSpecialArray.size)
            }
        }
    }

    /**
     * Load values for extended control data
     */
    fun loadValues(data: ControlDrawerData) {
        loadValues(data.properties)

        mOrientationSpinner.setSelection(
            ControlDrawerData.orientationToInt(data.orientation)
        )

        mMappingTextView.visibility = View.GONE
        for (i in mKeycodeSpinners.indices) {
            mKeycodeSpinners[i]!!.visibility = View.GONE
            mKeycodeTextviews[i]!!.visibility = View.GONE
        }

        mOrientationTextView.visibility = View.VISIBLE
        mOrientationSpinner.visibility = View.VISIBLE

        mSwipeableSwitch.visibility = View.GONE
        mPassthroughSwitch.visibility = View.GONE
        mToggleSwitch.visibility = View.GONE
    }

    /**
     * Load values for the joystick
     */
    fun loadJoystickValues(data: ControlJoystickData) {
        loadValues(data)

        mMappingTextView.visibility = View.GONE
        for (i in mKeycodeSpinners.indices) {
            mKeycodeSpinners[i]!!.visibility = View.GONE
            mKeycodeTextviews[i]!!.visibility = View.GONE
        }

        mNameTextView.visibility = View.GONE
        mNameEditText.visibility = View.GONE

        mCornerRadiusTextView.visibility = View.GONE
        mCornerRadiusSeekbar.visibility = View.GONE
        mCornerRadiusPercentTextView.visibility = View.GONE

        mSwipeableSwitch.visibility = View.GONE
        mPassthroughSwitch.visibility = View.GONE
        mToggleSwitch.visibility = View.GONE

        mForwardLockSwitch.visibility = View.VISIBLE
        mForwardLockSwitch.setChecked(data.forwardLock)

        mAbsoluteTrackingSwitch.visibility = View.VISIBLE
        mAbsoluteTrackingSwitch.setChecked(data.absolute)
    }

    /**
     * Load values for sub buttons
     */
    fun loadSubButtonValues(data: ControlData, drawerOrientation: ControlDrawerData.Orientation) {
        loadValues(data)

        // Size linked to the parent drawer depending on the drawer settings
        if (drawerOrientation != ControlDrawerData.Orientation.FREE) {
            mSizeTextview.visibility = View.GONE
            mSizeXTextView.visibility = View.GONE
            mWidthEditText.visibility = View.GONE
            mHeightEditText.visibility = View.GONE
        }

        // No conditional, already depends on the parent drawer visibility
        mVisibilityTextView.visibility = View.GONE
        mDisplayInMenuCheckbox.visibility = View.GONE
        mDisplayInGameCheckbox.visibility = View.GONE
    }

    private fun bindLayout() {
        mRootView = mScrollView.findViewById(R.id.edit_layout)
        mNameEditText = mScrollView.findViewById(R.id.editName_editText)
        mWidthEditText = mScrollView.findViewById(R.id.editSize_editTextX)
        mHeightEditText = mScrollView.findViewById(R.id.editSize_editTextY)
        mToggleSwitch = mScrollView.findViewById(R.id.checkboxToggle)
        mPassthroughSwitch = mScrollView.findViewById(R.id.checkboxPassThrough)
        mSwipeableSwitch = mScrollView.findViewById(R.id.checkboxSwipeable)
        mForwardLockSwitch = mScrollView.findViewById(R.id.checkboxForwardLock)
        mAbsoluteTrackingSwitch = mScrollView.findViewById(R.id.checkboxAbsoluteFingerTracking)
        mKeycodeSpinners[0] = mScrollView.findViewById(R.id.editMapping_spinner_1)
        mKeycodeSpinners[1] = mScrollView.findViewById(R.id.editMapping_spinner_2)
        mKeycodeSpinners[2] = mScrollView.findViewById(R.id.editMapping_spinner_3)
        mKeycodeSpinners[3] = mScrollView.findViewById(R.id.editMapping_spinner_4)
        mKeycodeTextviews[0] = mScrollView.findViewById(R.id.mapping_1_textview)
        mKeycodeTextviews[1] = mScrollView.findViewById(R.id.mapping_2_textview)
        mKeycodeTextviews[2] = mScrollView.findViewById(R.id.mapping_3_textview)
        mKeycodeTextviews[3] = mScrollView.findViewById(R.id.mapping_4_textview)
        mOrientationSpinner = mScrollView.findViewById(R.id.editOrientation_spinner)
        mStrokeWidthSeekbar = mScrollView.findViewById(R.id.editStrokeWidth_seekbar)
        mCornerRadiusSeekbar = mScrollView.findViewById(R.id.editCornerRadius_seekbar)
        mAlphaSeekbar = mScrollView.findViewById(R.id.editButtonOpacity_seekbar)
        mSelectBackgroundColor = mScrollView.findViewById(R.id.editBackgroundColor_textView)
        mSelectStrokeColor = mScrollView.findViewById(R.id.editStrokeColor_textView)
        mStrokePercentTextView = mScrollView.findViewById(R.id.editStrokeWidth_textView_percent)
        mAlphaPercentTextView = mScrollView.findViewById(R.id.editButtonOpacity_textView_percent)
        mCornerRadiusPercentTextView = mScrollView.findViewById(R.id.editCornerRadius_textView_percent)
        mDisplayInGameCheckbox = mScrollView.findViewById(R.id.visibility_game_checkbox)
        mDisplayInMenuCheckbox = mScrollView.findViewById(R.id.visibility_menu_checkbox)

        //Decorative stuff
        mMappingTextView = mScrollView.findViewById(R.id.editMapping_textView)
        mOrientationTextView = mScrollView.findViewById(R.id.editOrientation_textView)
        mNameTextView = mScrollView.findViewById(R.id.editName_textView)
        mCornerRadiusTextView = mScrollView.findViewById(R.id.editCornerRadius_textView)
        mVisibilityTextView = mScrollView.findViewById(R.id.visibility_textview)
        mSizeTextview = mScrollView.findViewById(R.id.editSize_textView)
        mSizeXTextView = mScrollView.findViewById(R.id.editSize_x_textView)

        keyboardDialog = KeyboardDialog(this.context)
    }

    /**
     * A long function linking all the displayed data on the popup and,
     * the currently edited mCurrentlyEditedButton
     */
    fun setupRealTimeListeners() {
        mNameEditText.addTextChangedListener(SimpleTextWatcher { s ->
            if (internalChanges) return@SimpleTextWatcher

            mCurrentlyEditedButton!!.getProperties().name = s.toString()

            // Cheap and unoptimized, doesn't break the abstraction layer
            mCurrentlyEditedButton!!.setProperties(mCurrentlyEditedButton!!.getProperties(), false)
        })

        mWidthEditText.addTextChangedListener(SimpleTextWatcher { s ->
            if (internalChanges) return@SimpleTextWatcher

            val width = safeParseFloat(s.toString())
            if (width >= 0) {
                mCurrentlyEditedButton!!.getProperties().setWidth(width)
                if (mCurrentlyEditedButton!!.getProperties() is ControlJoystickData) {
                    // Joysticks are square
                    mCurrentlyEditedButton!!.getProperties().setHeight(width)
                }
                mCurrentlyEditedButton!!.updateProperties()
            }
        })

        mHeightEditText.addTextChangedListener(SimpleTextWatcher { s ->
            if (internalChanges) return@SimpleTextWatcher

            val height = safeParseFloat(s.toString())
            if (height >= 0) {
                mCurrentlyEditedButton!!.getProperties().setHeight(height)
                if (mCurrentlyEditedButton!!.getProperties() is ControlJoystickData) {
                    // Joysticks are square
                    mCurrentlyEditedButton!!.getProperties().setWidth(height)
                }
                mCurrentlyEditedButton!!.updateProperties()
            }
        })

        mSwipeableSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.getProperties().isSwipeable = isChecked
        }
        mToggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.getProperties().isToggle = isChecked
        }
        mPassthroughSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.getProperties().passThruEnabled = isChecked
        }
        mForwardLockSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            val properties = mCurrentlyEditedButton!!.getProperties()
            if (properties is ControlJoystickData) {
                properties.forwardLock = isChecked
            }
        }
        mAbsoluteTrackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            val properties = mCurrentlyEditedButton!!.getProperties()
            if (properties is ControlJoystickData) {
                properties.absolute = isChecked
            }
        }

        mAlphaSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (internalChanges) return
                mCurrentlyEditedButton!!.getProperties().opacity = mAlphaSeekbar.progress / 100f
                mCurrentlyEditedButton!!.getControlView().alpha = mAlphaSeekbar.progress / 100f
                setPercentageText(mAlphaPercentTextView, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        mStrokeWidthSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (internalChanges) return
                mCurrentlyEditedButton!!.getProperties().strokeWidth = mStrokeWidthSeekbar.progress / 10f
                mCurrentlyEditedButton!!.setBackground()
                setPercentageText(mStrokePercentTextView, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        mCornerRadiusSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (internalChanges) return
                mCurrentlyEditedButton!!.getProperties().cornerRadius = mCornerRadiusSeekbar.progress.toFloat()
                mCurrentlyEditedButton!!.setBackground()
                setPercentageText(mCornerRadiusPercentTextView, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        for (i in mKeycodeSpinners.indices) {
            val finalI = i
            mKeycodeTextviews[i]!!.setOnClickListener {
                keyboardDialog.setOnKeycodeSelectListener { index ->
                    mKeycodeSpinners[finalI]!!.setSelection(index)
                    updateKeycodeText(index, finalI)
                }.show()
            }

            mKeycodeSpinners[i]!!.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateKeycodeText(position, finalI)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            })
        }

        mOrientationSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Side note, spinner listeners are fired later than all the other ones.
                // Meaning the internalChanges bool is useless here.

                val button = mCurrentlyEditedButton
                if (button is ControlDrawer) {
                    button.drawerData.orientation = ControlDrawerData.intToOrientation(mOrientationSpinner.selectedItemPosition)!!
                    button.syncButtons()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        })

        mDisplayInGameCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.getProperties().displayInGame = isChecked
        }

        mDisplayInMenuCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (internalChanges) return@setOnCheckedChangeListener
            mCurrentlyEditedButton!!.getProperties().displayInMenu = isChecked
        }

        mSelectStrokeColor.setOnClickListener {
            mColorSelector.setAlphaEnabled(false)
            mColorSelector.setColorSelectionListener { color ->
                mCurrentlyEditedButton!!.getProperties().strokeColor = color
                mCurrentlyEditedButton!!.setBackground()
            }
            appearColor(isAtRight(), mCurrentlyEditedButton!!.getProperties().strokeColor)
        }

        mSelectBackgroundColor.setOnClickListener {
            mColorSelector.setAlphaEnabled(true)
            mColorSelector.setColorSelectionListener { color ->
                mCurrentlyEditedButton!!.getProperties().bgColor = color
                mCurrentlyEditedButton!!.setBackground()
            }
            appearColor(isAtRight(), mCurrentlyEditedButton!!.getProperties().bgColor)
        }
    }

    private fun updateKeycodeText(index: Int, finalI: Int) {
        // Side note, spinner listeners are fired later than all the other ones.
        // Meaning the internalChanges bool is useless here.
        if (index < mSpecialArray.size) {
            mCurrentlyEditedButton!!.getProperties().keycodes[finalI] = mKeycodeSpinners[finalI]!!.selectedItemPosition - mSpecialArray.size
        } else {
            mCurrentlyEditedButton!!.getProperties().keycodes[finalI] =
                EfficientAndroidLWJGLKeycode.getValueByIndex(mKeycodeSpinners[finalI]!!.selectedItemPosition - mSpecialArray.size).toInt()
        }
        mKeycodeTextviews[finalI]!!.setText(mKeycodeSpinners[finalI]!!.selectedItem as String?)
    }

    private fun safeParseFloat(string: String): Float {
        var out = -1f // -1
        try {
            out = string.toFloat()
        } catch (e: NumberFormatException) {
            Logging.e("EditControlPopup", e.toString())
        }
        return out
    }

    fun setCurrentlyEditedButton(button: ControlInterface) {
        mCurrentlyEditedButton?.getControlView()?.removeOnLayoutChangeListener(mLayoutChangedListener)
        mCurrentlyEditedButton = button
        mCurrentlyEditedButton!!.getControlView().addOnLayoutChangeListener(mLayoutChangedListener)
    }

    companion object {
        @JvmStatic
        fun setPercentageText(textView: TextView, progress: Int) {
            textView.setText(textView.context.getString(R.string.percent_format, progress))
        }
    }
}
