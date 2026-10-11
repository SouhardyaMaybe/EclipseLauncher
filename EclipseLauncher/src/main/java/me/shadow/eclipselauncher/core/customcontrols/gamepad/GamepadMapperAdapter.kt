package me.shadow.eclipselauncher.core.customcontrols.gamepad

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView

import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.ui.dialog.KeyboardDialog

import me.shadow.eclipselauncher.core.EfficientAndroidLWJGLKeycode
import me.shadow.eclipselauncher.core.GrabListener
import me.shadow.eclipselauncher.core.Tools

class GamepadMapperAdapter(context: Context) : RecyclerView.Adapter<GamepadMapperAdapter.ViewHolder>(),
    GamepadDataProvider {

    private val keyboardDialog: KeyboardDialog
    private lateinit var mSimulatedGamepadMap: GamepadMap
    private lateinit var mRebinderButtons: Array<RebinderButton?>
    private lateinit var mRealButtons: Array<GamepadEmulatedButton?>
    private val mKeyAdapter: ArrayAdapter<String>
    private val mSpecialKeycodeCount: Int
    private var mGamepadGrabListener: GrabListener? = null
    private var mGrabState = false
    private var mOldState = false

    init {
        GamepadMapStore.load()
        mKeyAdapter = ArrayAdapter(context, R.layout.item_centered_textview_large)
        val specialKeycodeNames = GamepadMap.getSpecialKeycodeNames(context)
        mSpecialKeycodeCount = specialKeycodeNames.size
        mKeyAdapter.addAll(*specialKeycodeNames)
        mKeyAdapter.addAll(*EfficientAndroidLWJGLKeycode.generateKeyName())
        createRebinderMap()
        updateRealButtons()

        keyboardDialog = KeyboardDialog(context, true)
    }

    private fun createRebinderMap() {
        mRebinderButtons = arrayOfNulls(BUTTON_COUNT)
        mRealButtons = arrayOfNulls(BUTTON_COUNT)
        mSimulatedGamepadMap = GamepadMap()
        var index = 0
        // Mirrors the Java chained assignment: create, store into the array slot,
        // advance the index, then hand the same instance to the simulated map field
        fun rebinder(iconResourceId: Int, localeResourceId: Int): RebinderButton {
            val button = RebinderButton(iconResourceId, localeResourceId)
            mRebinderButtons[index] = button
            index++
            return button
        }
        mSimulatedGamepadMap.BUTTON_A = rebinder(R.drawable.button_a, R.string.controller_button_a)
        mSimulatedGamepadMap.BUTTON_B = rebinder(R.drawable.button_b, R.string.controller_button_b)
        mSimulatedGamepadMap.BUTTON_X = rebinder(R.drawable.button_x, R.string.controller_button_x)
        mSimulatedGamepadMap.BUTTON_Y = rebinder(R.drawable.button_y, R.string.controller_button_y)
        mSimulatedGamepadMap.BUTTON_START = rebinder(R.drawable.button_start, R.string.controller_button_start)
        mSimulatedGamepadMap.BUTTON_SELECT = rebinder(R.drawable.button_select, R.string.generic_select)
        mSimulatedGamepadMap.TRIGGER_RIGHT = rebinder(R.drawable.trigger_right, R.string.controller_button_trigger_right)
        mSimulatedGamepadMap.TRIGGER_LEFT = rebinder(R.drawable.trigger_left, R.string.controller_button_trigger_left)
        mSimulatedGamepadMap.SHOULDER_RIGHT = rebinder(R.drawable.shoulder_right, R.string.controller_button_shoulder_right)
        mSimulatedGamepadMap.SHOULDER_LEFT = rebinder(R.drawable.shoulder_left, R.string.controller_button_shoulder_left)
        mSimulatedGamepadMap.DIRECTION_FORWARD = rebinder(R.drawable.stick_right, R.string.controller_direction_forward)
        mSimulatedGamepadMap.DIRECTION_RIGHT = rebinder(R.drawable.stick_right, R.string.controller_direction_right)
        mSimulatedGamepadMap.DIRECTION_LEFT = rebinder(R.drawable.stick_right, R.string.controller_direction_left)
        mSimulatedGamepadMap.DIRECTION_BACKWARD = rebinder(R.drawable.stick_right, R.string.controller_direction_backward)
        mSimulatedGamepadMap.THUMBSTICK_RIGHT = rebinder(R.drawable.stick_right_click, R.string.controller_stick_press_r)
        mSimulatedGamepadMap.THUMBSTICK_LEFT = rebinder(R.drawable.stick_left_click, R.string.controller_stick_press_l)
        mSimulatedGamepadMap.DPAD_UP = rebinder(R.drawable.dpad_up, R.string.controller_dpad_up)
        mSimulatedGamepadMap.DPAD_DOWN = rebinder(R.drawable.dpad_down, R.string.controller_dpad_down)
        mSimulatedGamepadMap.DPAD_RIGHT = rebinder(R.drawable.dpad_right, R.string.controller_dpad_right)
        mSimulatedGamepadMap.DPAD_LEFT = rebinder(R.drawable.dpad_left, R.string.controller_dpad_left)
    }

    private fun updateRealButtons() {
        val currentRealMap = if (mGrabState) GamepadMapStore.getGameMap() else GamepadMapStore.getMenuMap()
        var index = 0
        mRealButtons[index++] = currentRealMap.BUTTON_A
        mRealButtons[index++] = currentRealMap.BUTTON_B
        mRealButtons[index++] = currentRealMap.BUTTON_X
        mRealButtons[index++] = currentRealMap.BUTTON_Y
        mRealButtons[index++] = currentRealMap.BUTTON_START
        mRealButtons[index++] = currentRealMap.BUTTON_SELECT
        mRealButtons[index++] = currentRealMap.TRIGGER_RIGHT
        mRealButtons[index++] = currentRealMap.TRIGGER_LEFT
        mRealButtons[index++] = currentRealMap.SHOULDER_RIGHT
        mRealButtons[index++] = currentRealMap.SHOULDER_LEFT
        mRealButtons[index++] = currentRealMap.DIRECTION_FORWARD
        mRealButtons[index++] = currentRealMap.DIRECTION_RIGHT
        mRealButtons[index++] = currentRealMap.DIRECTION_LEFT
        mRealButtons[index++] = currentRealMap.DIRECTION_BACKWARD
        mRealButtons[index++] = currentRealMap.THUMBSTICK_RIGHT
        mRealButtons[index++] = currentRealMap.THUMBSTICK_LEFT
        mRealButtons[index++] = currentRealMap.DPAD_UP
        mRealButtons[index++] = currentRealMap.DPAD_DOWN
        mRealButtons[index++] = currentRealMap.DPAD_RIGHT
        mRealButtons[index] = currentRealMap.DPAD_LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.item_controller_mapping, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.attach(position)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.detach()
    }

    override fun getItemCount(): Int {
        return mRebinderButtons.size
    }

    private fun updateStickIcons() {
        // Which stick is used for keyboard emulation depends on grab state, so we need
        // to update the mapper UI icons accordingly
        val stickIcon = if (mGrabState) R.drawable.stick_left else R.drawable.stick_right
        (mSimulatedGamepadMap.DIRECTION_FORWARD as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_BACKWARD as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_RIGHT as RebinderButton).iconResourceId = stickIcon
        (mSimulatedGamepadMap.DIRECTION_LEFT as RebinderButton).iconResourceId = stickIcon
    }

    private class RebinderButton(iconResourceId: Int, localeResourceId: Int) : GamepadButton() {
        @JvmField
        var iconResourceId: Int = iconResourceId
        val localeResourceId: Int = localeResourceId
        private var mButtonHolder: ViewHolder? = null

        fun changeViewHolder(viewHolder: ViewHolder?) {
            mButtonHolder = viewHolder
            mButtonHolder?.setPressed(mIsDown)
        }

        override fun onDownStateChanged(isDown: Boolean) {
            mButtonHolder?.setPressed(isDown)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        AdapterView.OnItemSelectedListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

        private val mContext: Context = itemView.context
        private val mButtonIcon: ImageView = itemView.findViewById(R.id.controller_mapper_button)
        private val mExpandedView: View = itemView.findViewById(R.id.controller_mapper_expanded_view)
        private val mExpansionIndicator: ImageView = itemView.findViewById(R.id.controller_mapper_expand_button)
        private val mKeycodeLabel: TextView = itemView.findViewById(R.id.controller_mapper_keycode_label)
        private val mToggleableSwitch: SwitchCompat = itemView.findViewById(R.id.controller_mapper_toggleable_switch)
        private var mAttachedPosition = -1
        private var mAttachedButton: GamepadEmulatedButton? = null
        private var mKeycodes: ShortArray? = null
        private val mKeySpinners: Array<Spinner> = arrayOf(
            itemView.findViewById<Spinner>(R.id.controller_mapper_key_spinner1),
            itemView.findViewById<Spinner>(R.id.controller_mapper_key_spinner2),
            itemView.findViewById<Spinner>(R.id.controller_mapper_key_spinner3),
            itemView.findViewById<Spinner>(R.id.controller_mapper_key_spinner4)
        )

        init {
            mToggleableSwitch.setOnCheckedChangeListener(this)
            val defaultView = itemView.findViewById<View>(R.id.controller_mapper_default_view)
            defaultView.setOnClickListener(this)
            val mKeyClickViews = arrayOf(
                itemView.findViewById<View>(R.id.controller_mapper_key_view1),
                itemView.findViewById<View>(R.id.controller_mapper_key_view2),
                itemView.findViewById<View>(R.id.controller_mapper_key_view3),
                itemView.findViewById<View>(R.id.controller_mapper_key_view4)
            )
            for (i in mKeySpinners.indices) {
                mKeySpinners[i].adapter = mKeyAdapter
                mKeyClickViews[i].setOnClickListener {
                    keyboardDialog.setOnKeycodeSelectListener(mKeySpinners[i]::setSelection).show()
                }
                mKeySpinners[i].setOnItemSelectedListener(this)
            }
        }

        internal fun attach(index: Int) {
            val rebinderButton = mRebinderButtons[index]!!
            mExpandedView.visibility = View.GONE
            mButtonIcon.setImageResource(rebinderButton.iconResourceId)
            val buttonName = mContext.getString(rebinderButton.localeResourceId)
            mButtonIcon.contentDescription = buttonName
            rebinderButton.changeViewHolder(this)

            val realButton = mRealButtons[index]!!

            mAttachedButton = realButton
            if (realButton is GamepadButton) {
                mToggleableSwitch.isChecked = realButton.isToggleable
                mToggleableSwitch.visibility = View.VISIBLE
            } else {
                mToggleableSwitch.visibility = View.GONE
            }

            val keycodes = realButton.keycodes
            mKeycodes = keycodes

            // Populate spinners with known keycodes until we run out of keycodes
            var spinnerIndex = 0
            while (spinnerIndex < keycodes.size) {
                val keySpinner = mKeySpinners[spinnerIndex]
                keySpinner.isEnabled = true
                val keyCode = keycodes[spinnerIndex]
                val selected = if (keyCode < 0) keyCode + mSpecialKeycodeCount
                    else EfficientAndroidLWJGLKeycode.getIndexByValue(keyCode.toInt()) + mSpecialKeycodeCount
                keySpinner.setSelection(selected)
                spinnerIndex++
            }
            // In case if there is too much spinners, disable the rest of them
            while (spinnerIndex < mKeySpinners.size) {
                mKeySpinners[spinnerIndex].isEnabled = false
                spinnerIndex++
            }
            updateKeycodeLabel()
            updateIndicator(mExpandedView.visibility != View.VISIBLE)

            mAttachedPosition = index
        }

        internal fun detach() {
            mRebinderButtons[mAttachedPosition]!!.changeViewHolder(null)
            mAttachedPosition = -1
            mAttachedButton = null
        }

        internal fun setPressed(pressed: Boolean) {
            itemView.setBackgroundColor(if (pressed) COLOR_ACTIVE_BUTTON else Color.TRANSPARENT)
        }

        private fun updateKeycodeLabel() {
            val labelBuilder = StringBuilder()
            var first = true
            val unspecifiedPosition = GamepadMap.UNSPECIFIED + mSpecialKeycodeCount
            for (keySpinner in mKeySpinners) {
                if (keySpinner.selectedItemPosition == unspecifiedPosition) continue
                if (!first) labelBuilder.append(" + ") else first = false
                labelBuilder.append(keySpinner.selectedItem!!.toString())
            }
            if (labelBuilder.length == 0) labelBuilder.append(mKeyAdapter.getItem(unspecifiedPosition))
            mKeycodeLabel.text = labelBuilder.toString()
        }

        private fun updateIndicator(rotation: Boolean) {
            mExpansionIndicator.rotation = (if (rotation) 180 else 0).toFloat()
        }

        override fun onItemSelected(adapterView: AdapterView<*>, view: View?, selectionIndex: Int, selectionId: Long) {
            if (mAttachedPosition == -1) return
            var editedKeycodeIndex = -1
            for (i in 0 until minOf(mKeySpinners.size, mKeycodes!!.size)) {
                if (adapterView != mKeySpinners[i]) continue
                editedKeycodeIndex = i
                break
            }
            if (editedKeycodeIndex == -1) return
            val keycodeOffset = selectionIndex - mSpecialKeycodeCount
            if (selectionIndex <= mSpecialKeycodeCount) {
                mKeycodes!![editedKeycodeIndex] = keycodeOffset.toShort()
            } else {
                mKeycodes!![editedKeycodeIndex] = EfficientAndroidLWJGLKeycode.getValueByIndex(keycodeOffset)
            }
            updateKeycodeLabel()
            try {
                GamepadMapStore.save()
            } catch (e: Exception) {
                Tools.showError(adapterView.context, e)
            }
        }

        override fun onNothingSelected(adapterView: AdapterView<*>?) {}

        override fun onClick(view: View) {
            val visibility = mExpandedView.visibility
            when (visibility) {
                View.INVISIBLE, View.GONE -> {
                    updateIndicator(false)
                    mExpandedView.visibility = View.VISIBLE
                }
                View.VISIBLE -> {
                    updateIndicator(true)
                    mExpandedView.visibility = View.GONE
                }
            }
        }

        override fun onCheckedChanged(compoundButton: CompoundButton, checked: Boolean) {
            val attachedButton = mAttachedButton
            if (attachedButton !is GamepadButton) return
            attachedButton.isToggleable = checked
            try {
                GamepadMapStore.save()
            } catch (e: Exception) {
                Tools.showError(compoundButton.context, e)
            }
        }
    }

    override fun getMenuMap(): GamepadMap {
        return mSimulatedGamepadMap
    }

    override fun getGameMap(): GamepadMap {
        return mSimulatedGamepadMap
    }

    override fun isGrabbing(): Boolean {
        return mGrabState
    }

    override fun attachGrabListener(grabListener: GrabListener) {
        mGamepadGrabListener = grabListener
        grabListener.onGrabState(mGrabState)
    }

    fun setGrabState(newState: Boolean) {
        mGrabState = newState
        mGamepadGrabListener?.onGrabState(newState)
        if (mGrabState == mOldState) return
        updateRealButtons()
        updateStickIcons()
        notifyItemRangeChanged(0, mRebinderButtons.size)
        mOldState = mGrabState
    }

    companion object {
        private const val BUTTON_COUNT = 20
        private const val COLOR_ACTIVE_BUTTON = 0x2000FF00
    }
}
