package me.shadow.eclipselauncher.ui.subassembly.menu

import android.app.Activity
import android.content.Intent
import android.provider.DocumentsContract
import android.view.View
import android.widget.CompoundButton
import com.google.android.material.slider.Slider
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.ViewControlMenuBinding
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.customcontrols.ControlData
import me.shadow.eclipselauncher.core.customcontrols.ControlDrawerData
import me.shadow.eclipselauncher.core.customcontrols.ControlJoystickData
import me.shadow.eclipselauncher.core.customcontrols.ControlLayout
import me.shadow.eclipselauncher.core.customcontrols.EditorExitable

class ControlMenu(
    private val activity: Activity,
    private val exitListener: EditorExitable,
    private val binding: ViewControlMenuBinding,
    private val controlLayout: ControlLayout,
    private val export: Boolean
) : View.OnClickListener, Slider.OnChangeListener, Slider.OnSliderTouchListener, CompoundButton.OnCheckedChangeListener {
    init {
        val listener = this
        binding.apply {
            snapping.isChecked = AllSettings.buttonSnapping.getValue()

            MenuUtils.initSliderValue(snappingDistance, AllSettings.buttonSnappingDistance.getValue(), snappingDistanceValue, "dp")

            addButton.setOnClickListener(listener)
            addDrawer.setOnClickListener(listener)
            addJoystick.setOnClickListener(listener)

            load.setOnClickListener(listener)
            save.setOnClickListener(listener)
            saveAndExit.setOnClickListener(listener)
            saveAndExport.setOnClickListener(listener)

            snappingLayout.setOnClickListener(listener)
            snapping.setOnCheckedChangeListener(listener)
            snappingDistance.addOnChangeListener(listener)
            snappingDistance.addOnSliderTouchListener(listener)
            snappingDistanceAdd.setOnClickListener(listener)
            snappingDistanceRemove.setOnClickListener(listener)
            selectDefault.setOnClickListener(listener)
            exit.setOnClickListener(listener)
            if (export) saveAndExport.visibility = View.VISIBLE
            else saveAndExport.visibility = View.GONE
        }
    }

    override fun onClick(v: View?) {
        binding.apply {
            when (v) {
                addButton -> controlLayout.addControlButton(ControlData(activity.getString(R.string.controls_add_control_button)))
                addDrawer -> controlLayout.addDrawer(ControlDrawerData())
                addJoystick -> controlLayout.addJoystickButton(ControlJoystickData())

                load -> controlLayout.openLoadDialog()
                save -> controlLayout.openSaveDialog()
                saveAndExit -> controlLayout.openSaveAndExitDialog(exitListener)
                saveAndExport -> {
                    try { // Saving the currently shown control
                        val contentUri = DocumentsContract.buildDocumentUri(
                            activity.getString(R.string.storageProviderAuthorities),
                            controlLayout.saveToDirectory(controlLayout.mLayoutFileName)
                        )

                        val shareIntent = Intent()
                        shareIntent.setAction(Intent.ACTION_SEND)
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        shareIntent.setType("application/json")
                        activity.startActivity(shareIntent)

                        val sendIntent = Intent.createChooser(shareIntent, controlLayout.mLayoutFileName)
                        activity.startActivity(sendIntent)
                    } catch (e: Exception) {
                        Tools.showError(activity, e)
                    }
                }

                snappingLayout -> MenuUtils.toggleSwitchState(snapping)
                snappingDistanceAdd -> MenuUtils.adjustSlider(snappingDistance, 1)
                snappingDistanceRemove -> MenuUtils.adjustSlider(snappingDistance, -1)
                selectDefault -> controlLayout.openSetDefaultDialog()

                exit -> controlLayout.openExitDialog(exitListener)
                else -> {}
            }
        }
    }

    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        updateSliderValue(slider, !fromUser)
    }

    override fun onStartTrackingTouch(slider: Slider) {}

    override fun onStopTrackingTouch(slider: Slider) {
        // Track is continuous (stepSize 0) for a smooth bar; snap back to a whole
        // number so the saved value stays an exact integer step.
        val snapped = Math.round(slider.value)
            .coerceIn(slider.valueFrom.toInt(), slider.valueTo.toInt())
            .toFloat()
        if (snapped != slider.value) {
            slider.value = snapped
        }
        updateSliderValue(slider, true)
    }

    private fun updateSliderValue(slider: Slider?, saveValue: Boolean) {
        val progress = slider?.value?.toInt() ?: 0

        binding.apply {
            when (slider) {
                snappingDistance -> {
                    if (saveValue) AllSettings.buttonSnappingDistance.put(progress).save()
                    MenuUtils.updateSliderValue(progress, snappingDistanceValue, "dp")
                }
                else -> {}
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        binding.apply {
            when (buttonView) {
                snapping -> AllSettings.buttonSnapping.put(isChecked).save()
                else -> {}
            }
        }
    }
}