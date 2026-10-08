package me.shadow.eclipselauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.shadow.eclipselauncher.InfoCenter
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.anim.AnimPlayer
import me.shadow.eclipselauncher.anim.animations.Animations
import me.shadow.eclipselauncher.databinding.FragmentAboutBinding
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.utils.path.UrlManager
import me.shadow.eclipselauncher.utils.stringutils.StringUtils

class AboutFragment : FragmentWithAnim(R.layout.fragment_about) {
    companion object {
        const val TAG: String = "AboutFragment"
    }

    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            dec1.text = InfoCenter.replaceName(requireActivity(), R.string.about_dec1)
            dec2.text = InfoCenter.replaceName(requireActivity(), R.string.about_dec2)
            dec3.text = InfoCenter.replaceName(requireActivity(), R.string.about_dec3)

            appInfo.text = StringUtils.insertNewline(StringUtils.insertSpace(getString(R.string.about_version_name), ZHTools.getVersionName()),
                StringUtils.insertSpace(getString(R.string.about_version_code), ZHTools.getVersionCode()),
                StringUtils.insertSpace(getString(R.string.about_last_update_time), ZHTools.getLastUpdateTime(requireContext())),
                StringUtils.insertSpace(getString(R.string.about_version_status), ZHTools.getVersionStatus(requireContext())))
            appInfo.setOnClickListener { StringUtils.copyText("text", appInfo.text.toString(), requireContext()) }

            githubButton.setOnClickListener { ZHTools.openLink(requireActivity(), UrlManager.URL_HOME) }
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.aboutContent, Animations.BounceInDown))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.aboutContent, Animations.FadeOutUp))
    }
}
