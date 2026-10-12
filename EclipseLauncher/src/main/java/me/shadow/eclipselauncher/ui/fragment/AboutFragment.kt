package me.shadow.eclipselauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import me.shadow.eclipselauncher.InfoCenter
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.anim.AnimPlayer
import me.shadow.eclipselauncher.anim.animations.Animations
import me.shadow.eclipselauncher.databinding.FragmentAboutBinding
import me.shadow.eclipselauncher.ui.subassembly.about.AboutItemBean
import me.shadow.eclipselauncher.ui.subassembly.about.AboutRecyclerAdapter
import me.shadow.eclipselauncher.utils.EclipseTools
import me.shadow.eclipselauncher.utils.path.UrlManager
import me.shadow.eclipselauncher.utils.stringutils.StringUtils

class AboutFragment : FragmentWithAnim(R.layout.fragment_about) {
    companion object {
        const val TAG: String = "AboutFragment"
    }

    private lateinit var binding: FragmentAboutBinding
    private val aboutItems = mutableListOf<AboutItemBean>()

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

            appInfo.text = StringUtils.insertNewline(StringUtils.insertSpace(getString(R.string.about_version_name), EclipseTools.getVersionName()),
                StringUtils.insertSpace(getString(R.string.about_version_code), EclipseTools.getVersionCode()),
                StringUtils.insertSpace(getString(R.string.about_last_update_time), EclipseTools.getLastUpdateTime(requireContext())),
                StringUtils.insertSpace(getString(R.string.about_version_status), EclipseTools.getVersionStatus(requireContext())))
            appInfo.setOnClickListener { StringUtils.copyText("text", appInfo.text.toString(), requireContext()) }

            githubButton.setOnClickListener { EclipseTools.openLink(requireActivity(), UrlManager.URL_HOME) }

            loadAboutData()
            aboutRecycler.layoutManager = LinearLayoutManager(requireContext())
            aboutRecycler.adapter = AboutRecyclerAdapter(aboutItems)
        }
    }

    private fun loadAboutData() {
        aboutItems.clear()

        val icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_about)!!
        val linkName = getString(R.string.about_access_link)

        aboutItems.add(AboutItemBean(icon, "OpenJDK", getString(R.string.about_lib_openjdk),
            AboutItemBean.AboutItemButtonBean(requireActivity(), linkName, "https://github.com/openjdk")))
        aboutItems.add(AboutItemBean(icon, "LWJGL", getString(R.string.about_lib_lwjgl),
            AboutItemBean.AboutItemButtonBean(requireActivity(), linkName, "https://github.com/LWJGL/lwjgl3")))
        aboutItems.add(AboutItemBean(icon, "GLFW", getString(R.string.about_lib_glfw),
            AboutItemBean.AboutItemButtonBean(requireActivity(), linkName, "https://github.com/glfw/glfw")))
        aboutItems.add(AboutItemBean(icon, "gl4es", getString(R.string.about_lib_gl4es),
            AboutItemBean.AboutItemButtonBean(requireActivity(), linkName, "https://github.com/ptitSeb/gl4es")))
        aboutItems.add(AboutItemBean(icon, "Mesa 3D", getString(R.string.about_lib_mesa),
            AboutItemBean.AboutItemButtonBean(requireActivity(), linkName, "https://gitlab.freedesktop.org/mesa/mesa")))
        aboutItems.add(AboutItemBean(icon, "SDL", getString(R.string.about_lib_sdl),
            AboutItemBean.AboutItemButtonBean(requireActivity(), linkName, "https://github.com/libsdl-org/SDL")))
        aboutItems.add(AboutItemBean(icon, "FFmpeg", getString(R.string.about_lib_ffmpeg),
            AboutItemBean.AboutItemButtonBean(requireActivity(), linkName, "https://github.com/FFmpeg/FFmpeg")))
        aboutItems.add(AboutItemBean(icon, "cacio-ctc", getString(R.string.about_lib_cacio), null))
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.aboutContent, Animations.BounceInDown))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.aboutContent, Animations.FadeOutUp))
    }
}
