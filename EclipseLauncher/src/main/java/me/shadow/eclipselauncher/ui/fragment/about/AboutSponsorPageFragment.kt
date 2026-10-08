package me.shadow.eclipselauncher.ui.fragment.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.FragmentAboutSponsorPageBinding
import me.shadow.eclipselauncher.feature.CheckSponsor
import me.shadow.eclipselauncher.feature.CheckSponsor.Companion.check
import me.shadow.eclipselauncher.feature.CheckSponsor.Companion.getSponsorData
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.task.TaskExecutors
import me.shadow.eclipselauncher.ui.subassembly.about.SponsorMeta
import me.shadow.eclipselauncher.ui.subassembly.about.SponsorRecyclerAdapter

class AboutSponsorPageFragment : Fragment(R.layout.fragment_about_sponsor_page) {
    private lateinit var binding: FragmentAboutSponsorPageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutSponsorPageBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        check(object : CheckSponsor.CheckListener {
            override fun onFailure() {
                setSponsorVisible(false)
            }

            override fun onSuccessful(data: SponsorMeta?) {
                setSponsorVisible(true)
            }
        })
    }

    private fun setSponsorVisible(visible: Boolean) {
        TaskExecutors.runInUIThread {
            try {
                binding.sponsorLayout.visibility = if (visible) {
                    binding.sponsorRecycler.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        adapter = SponsorRecyclerAdapter(getSponsorData())
                    }
                    binding.loadingProgress.visibility = View.GONE
                    View.VISIBLE
                } else View.GONE
            } catch (e: Exception) {
                Logging.e("setSponsorVisible", e.toString())
            }
        }
    }
}

