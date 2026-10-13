package me.shadow.eclipselauncher.ui.fragment.settings.wrapper

import android.annotation.SuppressLint
import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnChangeListener
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.log.Logging.e
import me.shadow.eclipselauncher.setting.unit.IntSettingUnit
import me.shadow.eclipselauncher.ui.dialog.EditTextDialog

@SuppressLint("UseSwitchCompatOrMaterialCode", "StringFormatInvalid")
class SliderSettingsWrapper(
    val context: Context,
    val unit: IntSettingUnit,
    val mainView: View,
    val titleView: TextView,
    val summaryView: TextView,
    val valueView: TextView,
    val sliderView: Slider,
    val suffix: String,
    onStartListener: OnStartInit?
) : AbstractSettingsWrapper(mainView) {
    private var listener: OnSliderProgressChangeListener? = null

    constructor(
        context: Context,
        unit: IntSettingUnit,
        mainView: View,
        titleView: TextView,
        summaryView: TextView,
        valueView: TextView,
        sliderView: Slider,
        suffix: String,
    ) : this(
        context,
        unit,
        mainView,
        titleView,
        summaryView,
        valueView,
        sliderView,
        suffix,
        null
    )

    init {
        onStartListener?.onStart(this)

        // Slider enforces valueFrom <= value <= valueTo (it throws instead of
        // clamping like SeekBar did), so every write goes through a clamped setter.
        sliderView.value = unit.getValue().toFloat().coerceIn(sliderView.valueFrom, sliderView.valueTo)

        valueView.background = ContextCompat.getDrawable(context, R.drawable.background_text)
        setSliderValueTextView()

        sliderView.addOnChangeListener(object : OnChangeListener {
            fun updateSliderValue(saveValue: Boolean) {
                val progress = sliderView.value.toInt()

                listener?.onChange(progress)
                if (saveValue) unit.put(progress).save()
                setSliderValueTextView()
                checkShowRebootDialog(context)
            }

            override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
                updateSliderValue(!fromUser)
            }
        })

        // stepSize is 0 so the track draws as a smooth bar instead of discrete
        // ticks. Snap the thumb back to a whole number the moment the user lets
        // go, so the stored setting still moves in exact +/-1 steps.
        sliderView.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                val snapped = Math.round(slider.value)
                    .coerceIn(slider.valueFrom.toInt(), slider.valueTo.toInt())
                    .toFloat()
                if (snapped != slider.value) {
                    slider.value = snapped
                }
            }
        })

        mainView.setOnClickListener {
            EditTextDialog.Builder(context)
                .setEditText(sliderView.value.toInt().toString())
                .setInputType(InputType.TYPE_CLASS_NUMBER)
                .setTitle(titleView.text.toString())
                .setMessage(summaryView.text.toString())
                .setAsRequired()
                .setConfirmListener { editBox, _ ->
                    val string = editBox.text.toString()

                    val value: Int
                    try {
                        value = string.toInt()
                    } catch (ex: NumberFormatException) {
                        e("SliderSettingsWrapper", "The data is illegal", ex)

                        editBox.error = context.getString(R.string.generic_input_invalid)
                        return@setConfirmListener false
                    }

                    if (value < sliderView.valueFrom.toInt()) {
                        val minValue =
                            String.format("%s %s", sliderView.valueFrom.toInt(), suffix)
                        editBox.error =
                            context.getString(R.string.generic_input_too_small, minValue)
                        return@setConfirmListener false
                    }
                    if (value > sliderView.valueTo.toInt()) {
                        val maxValue =
                            String.format("%s %s", sliderView.valueTo.toInt(), suffix)
                        editBox.error =
                            context.getString(R.string.generic_input_too_big, maxValue)
                        return@setConfirmListener false
                    }

                    sliderView.value = value.toFloat().coerceIn(sliderView.valueFrom, sliderView.valueTo)
                    true
                }.showDialog()
        }
    }

    fun setSliderValueTextView() {
        val text = "${sliderView.value.toInt()} $suffix".trim()
        valueView.text = text
    }

    fun setOnSliderProgressChangeListener(listener: OnSliderProgressChangeListener) {
        this.listener = listener
    }

    // Some cases need to run before the slider is wrapped, e.g. adjusting the max or min value dynamically
    fun interface OnStartInit {
        fun onStart(wrapper: SliderSettingsWrapper)
    }

    fun interface OnSliderProgressChangeListener {
        fun onChange(progress: Int)
    }
}
