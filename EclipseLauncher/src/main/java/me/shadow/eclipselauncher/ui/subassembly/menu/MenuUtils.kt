package me.shadow.eclipselauncher.ui.subassembly.menu

import android.annotation.SuppressLint
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MenuUtils {
    companion object {
        /**
         * Adjust the value of the seek bar
         * @param seekBar the seek bar
         * @param v the amount to adjust the value by
         */
        @JvmStatic
        fun adjustSeekbar(seekBar: SeekBar, v: Int) {
            seekBar.progress += v
        }

        /**
         * Invert the Switch's current checked state
         */
        @JvmStatic
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        fun toggleSwitchState(switchView: Switch) {
            switchView.isChecked = !switchView.isChecked
        }

        /**
         * Initialize the SeekBar's value
         */
        @JvmStatic
        fun initSeekBarValue(seek: SeekBar, value: Int, valueView: TextView, suffix: String) {
            seek.progress = value
            updateSeekbarValue(value, valueView, suffix)
        }

        /**
         * Update the text of the number next to the SeekBar
         */
        @JvmStatic
        fun updateSeekbarValue(value: Int, valueView: TextView, suffix: String) {
            val valueText = "$value $suffix"
            valueView.text = valueText.trim { it <= ' ' }
        }
    }
}