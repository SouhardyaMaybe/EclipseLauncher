package me.shadow.eclipselauncher.ui.fragment.about

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import me.shadow.eclipselauncher.InfoCenter
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.FragmentAboutInfoPageBinding
import me.shadow.eclipselauncher.ui.subassembly.about.AboutItemBean
import me.shadow.eclipselauncher.ui.subassembly.about.AboutItemBean.AboutItemButtonBean
import me.shadow.eclipselauncher.ui.subassembly.about.AboutRecyclerAdapter
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.utils.path.UrlManager

class AboutInfoPageFragment() : Fragment(R.layout.fragment_about_info_page) {
    private lateinit var binding: FragmentAboutInfoPageBinding
    private val mAboutData: MutableList<AboutItemBean> = ArrayList()
    private var parentPager2: ViewPager2? = null

    constructor(parentPager: ViewPager2): this() {
        this.parentPager2 = parentPager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutInfoPageBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadAboutData(requireContext().resources)

        val context = requireActivity()

        binding.apply {
            dec1.text = InfoCenter.replaceName(context, R.string.about_dec1)
            dec2.text = InfoCenter.replaceName(context, R.string.about_dec2)
            dec3.text = InfoCenter.replaceName(context, R.string.about_dec3)

            githubButton.setOnClickListener { ZHTools.openLink(requireActivity(), UrlManager.URL_HOME) }
            licenseButton.setOnClickListener { ZHTools.openLink(requireActivity(), "https://www.gnu.org/licenses/gpl-3.0.html") }

            val aboutAdapter = AboutRecyclerAdapter(this@AboutInfoPageFragment.mAboutData)
            aboutRecycler.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = aboutAdapter
            }
            sponsor.setOnClickListener { _ ->
                parentPager2?.currentItem = 1
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadAboutData(resources: Resources) {
        mAboutData.clear()

        mAboutData.add(
            AboutItemBean(
                resources.getDrawable(R.drawable.ic_pojav_full, requireContext().theme),
                "PojavLauncherTeam",
                getString(R.string.about_PojavLauncher_desc),
                AboutItemButtonBean(requireActivity(), "Github", "https://github.com/PojavLauncherTeam/PojavLauncher")
            )
        )
        mAboutData.add(
            AboutItemBean(
                resources.getDrawable(R.drawable.image_about_shadow, requireContext().theme),
                "Shadow",
                getString(R.string.about_Shadow_desc),
                AboutItemButtonBean(
                    requireActivity(),
                    getString(R.string.about_access_space),
                    "https://github.com/SouhardyaMaybe/EclipseLauncher"
                )
            )
        )
        mAboutData.add(
            AboutItemBean(
                resources.getDrawable(R.drawable.image_about_verafirefly, requireContext().theme),
                "Vera-Firefly",
                getString(R.string.about_VeraFirefly_desc),
                AboutItemButtonBean(
                    requireActivity(),
                    getString(R.string.about_access_space),
                    "https://space.bilibili.com/1412062866"
                )
            )
        )
        mAboutData.add(
            AboutItemBean(
                resources.getDrawable(R.drawable.image_about_lingmuqiuzhu, requireContext().theme),
                "柃木湫竹",
                getString(R.string.about_LingMuQiuZhu_desc),
                AboutItemButtonBean(
                    requireActivity(),
                    getString(R.string.about_access_space),
                    "https://space.bilibili.com/515165764"
                )
            )
        )
        mAboutData.add(
            AboutItemBean(
                resources.getDrawable(R.drawable.image_about_shirosakimio, requireContext().theme),
                "ShirosakiMio",
                getString(R.string.about_ShirosakiMio_desc),
                AboutItemButtonBean(
                    requireActivity(),
                    getString(R.string.about_access_space),
                    "https://space.bilibili.com/35801833"
                )
            )
        )
    }
}

