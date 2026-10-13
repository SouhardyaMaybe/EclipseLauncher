package me.shadow.eclipselauncher.ui.fragment.settings

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import me.shadow.eclipselauncher.anim.AnimPlayer
import me.shadow.eclipselauncher.anim.animations.Animations
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.SettingsFragmentControlBinding
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.ui.fragment.CustomMouseFragment
import me.shadow.eclipselauncher.ui.fragment.FragmentWithAnim
import me.shadow.eclipselauncher.ui.fragment.settings.wrapper.BaseSettingsWrapper
import me.shadow.eclipselauncher.ui.fragment.settings.wrapper.SliderSettingsWrapper
import me.shadow.eclipselauncher.ui.fragment.settings.wrapper.SwitchSettingsWrapper
import me.shadow.eclipselauncher.utils.EclipseTools
import fr.spse.gamepad_remapper.Remapper
import me.shadow.eclipselauncher.core.fragments.GamepadMapperFragment

class ControlSettingsFragment() : AbstractSettingsFragment(R.layout.settings_fragment_control, SettingCategory.CONTROL) {
    private lateinit var binding: SettingsFragmentControlBinding
    private var parentFragment: FragmentWithAnim? = null

    constructor(parentFragment: FragmentWithAnim?) : this() {
        this.parentFragment = parentFragment
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsFragmentControlBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        SwitchSettingsWrapper(
            context,
            AllSettings.disableGestures,
            binding.disableGesturesLayout,
            binding.disableGestures
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.disableDoubleTap,
            binding.disableDoubleTapLayout,
            binding.disableDoubleTap
        )

        SliderSettingsWrapper(
            context,
            AllSettings.timeLongPressTrigger,
            binding.timeLongPressTriggerLayout,
            binding.timeLongPressTriggerTitle,
            binding.timeLongPressTriggerSummary,
            binding.timeLongPressTriggerValue,
            binding.timeLongPressTrigger,
            "ms"
        )

        SliderSettingsWrapper(
            context,
            AllSettings.buttonScale,
            binding.buttonscaleLayout,
            binding.buttonscaleTitle,
            binding.buttonscaleSummary,
            binding.buttonscaleValue,
            binding.buttonscale,
            "%"
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.buttonAllCaps,
            binding.buttonAllCapsLayout,
            binding.buttonAllCaps
        )

        SliderSettingsWrapper(
            context,
            AllSettings.mouseScale,
            binding.mousescaleLayout,
            binding.mousescaleTitle,
            binding.mousescaleSummary,
            binding.mousescaleValue,
            binding.mousescale,
            "%"
        )

        SliderSettingsWrapper(
            context,
            AllSettings.mouseSpeed,
            binding.mousespeedLayout,
            binding.mousespeedTitle,
            binding.mousespeedSummary,
            binding.mousespeedValue,
            binding.mousespeed,
            "%"
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.virtualMouseStart,
            binding.mouseStartLayout,
            binding.mouseStart
        )

        BaseSettingsWrapper(
            context,
            binding.customMouseLayout
        ) {
            parentFragment?.apply {
                EclipseTools.swapFragmentWithAnim(
                    this,
                    CustomMouseFragment::class.java,
                    CustomMouseFragment.TAG,
                    null
                )
            }
        }

        SwitchSettingsWrapper(
            context,
            AllSettings.enableGyro,
            binding.enableGyroLayout,
            binding.enableGyro
        )

        SliderSettingsWrapper(
            context,
            AllSettings.gyroSensitivity,
            binding.gyroSensitivityLayout,
            binding.gyroSensitivityTitle,
            binding.gyroSensitivitySummary,
            binding.gyroSensitivityValue,
            binding.gyroSensitivity,
            "%"
        )

        SliderSettingsWrapper(
            context,
            AllSettings.gyroSampleRate,
            binding.gyroSampleRateLayout,
            binding.gyroSampleRateTitle,
            binding.gyroSampleRateSummary,
            binding.gyroSampleRateValue,
            binding.gyroSampleRate,
            "ms"
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.gyroSmoothing,
            binding.gyroSmoothingLayout,
            binding.gyroSmoothing
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.gyroInvertX,
            binding.gyroInvertXLayout,
            binding.gyroInvertX
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.gyroInvertY,
            binding.gyroInvertYLayout,
            binding.gyroInvertY
        )

        BaseSettingsWrapper(
            context,
            binding.changeControllerBindingsLayout
        ) {
            parentFragment?.apply {
                EclipseTools.swapFragmentWithAnim(
                    this,
                    GamepadMapperFragment::class.java,
                    GamepadMapperFragment.TAG,
                    null
                )
            }
        }

        BaseSettingsWrapper(
            context,
            binding.resetControllerBindingsLayout
        ) {
            Remapper.wipePreferences(context)
            Toast.makeText(context, R.string.setting_controller_map_wiped, Toast.LENGTH_SHORT)
                .show()
        }

        SliderSettingsWrapper(
            context,
            AllSettings.deadZoneScale,
            binding.gamepadDeadzoneScaleLayout,
            binding.gamepadDeadzoneScaleTitle,
            binding.gamepadDeadzoneScaleSummary,
            binding.gamepadDeadzoneScaleValue,
            binding.gamepadDeadzoneScale,
            "%"
        )

        val mGyroAvailable =
            (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(
                Sensor.TYPE_GYROSCOPE
            ) != null
        binding.enableGyroCategory.visibility = if (mGyroAvailable) View.VISIBLE else View.GONE

        computeVisibility()
    }

    override fun onChange() {
        super.onChange()
        computeVisibility()
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.root, Animations.BounceInDown))
    }

    private fun computeVisibility() {
        binding.apply {
            setViewVisibility(
                timeLongPressTriggerLayout,
                !AllSettings.disableGestures.getValue()
            )
            setViewVisibility(gyroSensitivityLayout, AllSettings.enableGyro.getValue())
            setViewVisibility(gyroSampleRateLayout, AllSettings.enableGyro.getValue())
            setViewVisibility(gyroInvertXLayout, AllSettings.enableGyro.getValue())
            setViewVisibility(gyroInvertYLayout, AllSettings.enableGyro.getValue())
            setViewVisibility(gyroSmoothingLayout, AllSettings.enableGyro.getValue())
        }
    }

    private fun setViewVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }
}