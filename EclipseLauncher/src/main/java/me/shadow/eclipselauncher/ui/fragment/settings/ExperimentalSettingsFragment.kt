package me.shadow.eclipselauncher.ui.fragment.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.shadow.eclipselauncher.anim.AnimPlayer
import me.shadow.eclipselauncher.anim.animations.Animations
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.SettingsFragmentExperimentalBinding
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.ui.fragment.settings.wrapper.SeekBarSettingsWrapper
import me.shadow.eclipselauncher.ui.fragment.settings.wrapper.SwitchSettingsWrapper

class ExperimentalSettingsFragment :
    AbstractSettingsFragment(R.layout.settings_fragment_experimental, SettingCategory.EXPERIMENTAL) {
    private lateinit var binding: SettingsFragmentExperimentalBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsFragmentExperimentalBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()

        SwitchSettingsWrapper(
            context,
            AllSettings.dumpShaders,
            binding.dumpShadersLayout,
            binding.dumpShaders
        )

        SwitchSettingsWrapper(
            context,
            AllSettings.bigCoreAffinity,
            binding.bigCoreAffinityLayout,
            binding.bigCoreAffinity
        )

    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.root, Animations.BounceInDown))
    }
}