package me.shadow.eclipselauncher.ui.subassembly.menu

import android.annotation.SuppressLint
import com.google.android.material.slider.Slider
import android.widget.CompoundButton
import android.widget.TextView

class MenuUtils {
    companion object {
        /**
         * Adjust the value of the slider
         * @param slider the slider
         * @param v the amount to adjust the value by
         */
        @JvmStatic
        fun adjustSlider(slider: Slider, v: Int) {
            slider.value = (slider.value + v).coerceIn(slider.valueFrom, slider.valueTo)
        }

        /**
         * Invert the Switch's current checked state
         */
        @JvmStatic
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        fun toggleSwitchState(switchView: CompoundButton) {
            switchView.isChecked = !switchView.isChecked
        }

        /**
         * Initialize the slider's value
         */
        @JvmStatic
        fun initSliderValue(seek: Slider, value: Int, valueView: TextView, suffix: String) {
            seek.value = value.toFloat().coerceIn(seek.valueFrom, seek.valueTo)
            updateSliderValue(seek.value.toInt(), valueView, suffix)
        }

        /**
         * Update the text of the number next to the slider
         */
        @JvmStatic
        fun updateSliderValue(value: Int, valueView: TextView, suffix: String) {
            val valueText = "$value $suffix"
            valueView.text = valueText.trim { it <= ' ' }
        }
    }
}