package me.shadow.eclipselauncher.ui.fragment.download.addon

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.event.sticky.SelectInstallTaskEvent
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.mod.modloader.ModVersionListAdapter
import me.shadow.eclipselauncher.task.TaskExecutors
import me.shadow.eclipselauncher.ui.subassembly.modlist.ModListFragment
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.pojav.Tools
import me.shadow.eclipselauncher.feature.mod.modloader.OptiFineDownloadTask
import me.shadow.eclipselauncher.feature.version.install.Addon
import me.shadow.eclipselauncher.ui.fragment.InstallGameFragment.Companion.BUNDLE_MC_VERSION
import me.shadow.eclipselauncher.pojav.modloaders.OptiFineUtils
import me.shadow.eclipselauncher.pojav.modloaders.OptiFineUtils.OptiFineVersion
import me.shadow.eclipselauncher.pojav.modloaders.OptiFineUtils.OptiFineVersions
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Future
import java.util.function.Consumer

class DownloadOptiFineFragment : ModListFragment() {
    companion object {
        const val TAG: String = "DownloadOptiFineFragment"
    }

    override fun refreshCreatedView() {
        setIcon(ContextCompat.getDrawable(fragmentActivity!!, R.drawable.ic_optifine))
        setTitleText("OptiFine")
        setLink("https://www.optifine.net/home")
        setReleaseCheckBoxGone()
    }

    override fun initRefresh(): Future<*> {
        return refresh(false)
    }

    override fun refresh(): Future<*> {
        return refresh(true)
    }

    private fun refresh(force: Boolean): Future<*> {
        return TaskExecutors.getDefault().submit {
            runCatching {
                TaskExecutors.runInUIThread {
                    cancelFailedToLoad()
                    componentProcessing(true)
                }
                val optiFineVersions = OptiFineUtils.downloadOptiFineVersions(force)
                processModDetails(optiFineVersions)
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                Logging.e("DownloadOptiFineFragment", Tools.printToString(e))
            }
        }
    }

    private fun empty() {
        TaskExecutors.runInUIThread {
            componentProcessing(false)
            setFailedToLoad(getString(R.string.version_install_no_versions))
        }
    }

    private fun processModDetails(optiFineVersions: OptiFineVersions?) {
        optiFineVersions ?: run {
            empty()
            return
        }

        val mcVersion = arguments?.getString(BUNDLE_MC_VERSION) ?: throw IllegalArgumentException("The Minecraft version is not passed")

        val mOptiFineVersions: MutableMap<String, MutableList<OptiFineVersion>> = HashMap()
        optiFineVersions.optifineVersions.forEach(Consumer<List<OptiFineVersion>> { optiFineVersionList: List<OptiFineVersion> ->  // Iterate the version lists and merge them into a Minecraft version + OptiFine version map
            currentTask?.apply { if (isCancelled) return@Consumer }

            optiFineVersionList.forEach(Consumer Consumer2@{ optiFineVersion: OptiFineVersion ->
                currentTask?.apply { if (isCancelled) return@Consumer2 }
                addIfAbsent(mOptiFineVersions, optiFineVersion.minecraftVersion.removePrefix("Minecraft").trim(), optiFineVersion)
            })
        })

        if (currentTask!!.isCancelled) return

        val mcOptiFineVersions = mOptiFineVersions[mcVersion] ?: mOptiFineVersions[mcVersion] ?: run {
            empty()
            return
        }

        val adapter = ModVersionListAdapter(R.drawable.ic_optifine, mcOptiFineVersions)
        adapter.setOnItemClickListener { version: Any ->
            if (isTaskRunning()) return@setOnItemClickListener false

            val optifineVersion = version as OptiFineVersion
            EventBus.getDefault().postSticky(
                SelectInstallTaskEvent(
                    Addon.OPTIFINE,
                    optifineVersion.versionName,
                    OptiFineDownloadTask(optifineVersion)
                )
            )
            ZHTools.onBackPressed(requireActivity())
            true
        }

        currentTask?.apply { if (isCancelled) return }

        TaskExecutors.runInUIThread {
            val recyclerView = recyclerView
            runCatching {
                recyclerView.layoutManager = LinearLayoutManager(fragmentActivity!!)
                recyclerView.adapter = adapter
            }.getOrElse { e ->
                Logging.e("Set Adapter", Tools.printToString(e))
            }

            componentProcessing(false)
            recyclerView.scheduleLayoutAnimation()
        }
    }
}
