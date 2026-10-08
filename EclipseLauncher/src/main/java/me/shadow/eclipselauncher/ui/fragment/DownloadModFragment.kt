package me.shadow.eclipselauncher.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import me.shadow.eclipselauncher.anim.AnimPlayer
import me.shadow.eclipselauncher.anim.animations.Animations
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.event.value.DownloadPageEvent
import me.shadow.eclipselauncher.feature.download.InfoViewModel
import me.shadow.eclipselauncher.feature.download.ScreenshotAdapter
import me.shadow.eclipselauncher.feature.download.VersionAdapter
import me.shadow.eclipselauncher.feature.download.enums.Classify
import me.shadow.eclipselauncher.feature.download.enums.ModLoader
import me.shadow.eclipselauncher.feature.download.item.InfoItem
import me.shadow.eclipselauncher.feature.download.item.ModVersionItem
import me.shadow.eclipselauncher.feature.download.item.ScreenshotItem
import me.shadow.eclipselauncher.feature.download.item.VersionItem
import me.shadow.eclipselauncher.feature.download.platform.AbstractPlatformHelper
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.version.VersionsManager
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.task.Task
import me.shadow.eclipselauncher.task.TaskExecutors
import me.shadow.eclipselauncher.ui.subassembly.modlist.ModListAdapter
import me.shadow.eclipselauncher.ui.subassembly.modlist.ModListFragment
import me.shadow.eclipselauncher.ui.subassembly.modlist.ModListItemBean
import me.shadow.eclipselauncher.ui.view.AnimButton
import me.shadow.eclipselauncher.utils.MCVersionRegex.Companion.RELEASE_REGEX
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.utils.stringutils.StringUtilsKt
import me.shadow.eclipselauncher.pojav.Tools
import org.greenrobot.eventbus.EventBus
import org.jackhuang.hmcl.util.versioning.VersionNumber
import java.util.Objects
import java.util.concurrent.Future
import java.util.function.Consumer

class DownloadModFragment : ModListFragment() {
    companion object {
        const val TAG: String = "DownloadModFragment"
    }

    private lateinit var platformHelper: AbstractPlatformHelper
    private lateinit var mInfoItem: InfoItem
    private var linkGetSubmit: Future<*>? = null

    override fun init() {
        parseViewModel()
        super.init()
    }

    @SuppressLint("CheckResult")
    override fun refreshCreatedView() {
        linkGetSubmit = TaskExecutors.getDefault().submit {
            runCatching {
                val webUrl = platformHelper.getWebUrl(mInfoItem)
                fragmentActivity?.runOnUiThread { setLink(webUrl) }
            }.getOrElse { e ->
                Logging.e("DownloadModFragment", "Failed to retrieve the website link, ${Tools.printToString(e)}")
            }
        }

        mInfoItem.apply {
            setTitleText(title)
            setDescription(description)
            loadScreenshots()

            iconUrl?.apply {
                Glide.with(fragmentActivity!!).load(this).apply {
                    if (!AllSettings.resourceImageCache.getValue()) diskCacheStrategy(DiskCacheStrategy.NONE)
                }.into(getIconView())
            }
        }
    }

    override fun initRefresh(): Future<*> {
        return refresh(false)
    }

    override fun refresh(): Future<*> {
        return refresh(true)
    }

    override fun onDestroy() {
        EventBus.getDefault().post(DownloadPageEvent.RecyclerEnableEvent(true))
        linkGetSubmit?.apply {
            if (!isCancelled && !isDone) cancel(true)
        }
        super.onDestroy()
    }

    private fun refresh(force: Boolean): Future<*> {
        return TaskExecutors.getDefault().submit {
            runCatching {
                TaskExecutors.runInUIThread {
                    cancelFailedToLoad()
                    componentProcessing(true)
                }
                val versions = platformHelper.getVersions(mInfoItem, force)
                processDetails(versions)
            }.getOrElse { e ->
                TaskExecutors.runInUIThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
                Logging.e("DownloadModFragment", Tools.printToString(e))
            }
        }
    }

    private fun processDetails(versions: List<VersionItem>?) {
        val pattern = RELEASE_REGEX

        val releaseCheckBoxChecked = releaseCheckBox.isChecked
        // Record the MC version together with the mod loader information in the key, so loaders can be subdivided later
        val mModVersionsByMinecraftVersion: MutableMap<Pair<String, ModLoader?>, MutableList<VersionItem>> = HashMap()

        versions?.forEach(Consumer { versionItem ->
            currentTask?.apply { if (isCancelled) return@Consumer }

            for (mcVersion in versionItem.mcVersions) {
                currentTask?.apply { if (isCancelled) return@Consumer }

                if (releaseCheckBoxChecked) {
                    val matcher = pattern.matcher(mcVersion)
                    if (!matcher.matches()) {
                        // Skip to the next item when this is not a release version
                        continue
                    }
                }

                if (versionItem is ModVersionItem) {
                    val modloaders = versionItem.modloaders
                    if (modloaders.isNotEmpty()) {
                        modloaders.forEach {
                            addIfAbsent(mModVersionsByMinecraftVersion, Pair(mcVersion, it), versionItem)
                        }
                        // When the entry is a ModVersionItem, check its mod loader: if it is not empty, put the version into each loader's own list
                        // This makes it easier for users to find versions matching the mod loader they need
                        continue // Already sorted, no need to add this version to the plain version list
                    }
                }
                addIfAbsent(mModVersionsByMinecraftVersion, Pair(mcVersion, null), versionItem)
            }
        })

        currentTask?.apply { if (isCancelled) return }

        val currentVersion = VersionsManager.getCurrentVersion()
        // Locate the first compatible version and record its index; after loading, the RecyclerView scrolls to it
        var firstAdaptIndex: Int? = null

        val mData: MutableList<ModListItemBean> = ArrayList()
        mModVersionsByMinecraftVersion.entries
            .sortedWith { entry1, entry2 ->
                val mcVersionComparison = -VersionNumber.compare(entry1.key.first, entry2.key.first)
                if (mcVersionComparison != 0) {
                    mcVersionComparison
                } else {
                    val name1 = entry1.key.second?.name ?: ""
                    val name2 = entry2.key.second?.name ?: ""
                    // Keep versions that have a mod loader first
                    if (name1.isEmpty() && name2.isNotEmpty()) 1
                    else if (name1.isNotEmpty() && name2.isEmpty()) -1
                    else name1.compareTo(name2)
                }
            }
            .forEachIndexed { index: Int, entry: Map.Entry<Pair<String, ModLoader?>, List<VersionItem>> ->
                currentTask?.apply { if (isCancelled) return }

                val isAdapt: Boolean = when (mInfoItem.classify) {
                    Classify.MODPACK -> false
                    else -> currentVersion?.let { version ->
                        val itemVersion = VersionNumber.asVersion(entry.key.first).canonical
                        val currentVersionString = VersionNumber.asVersion(version.getVersionInfo()?.minecraftVersion ?: "").canonical

                        if (!Objects.equals(itemVersion, currentVersionString)) return@let false

                        val modloader = entry.key.second
                        val loaderInfo = version.getVersionInfo()?.loaderInfo

                        when {
                            // The resource has no loader information, treat it as compatible
                            modloader == null -> true
                            // The resource has a loader but this version has none, not compatible
                            // (What mod would you even install without a mod loader?)
                            loaderInfo == null -> false
                            // Match the mod loader
                            else -> loaderInfo.any { loader -> Objects.equals(modloader.loaderName, loader.name) }
                        }
                    } ?: false
                }

                if (isAdapt) {
                    firstAdaptIndex ?: run {
                        firstAdaptIndex = index
                    }
                }

                mData.add(
                    ModListItemBean(
                        entry.key.first,
                        entry.key.second,
                        isAdapt,
                        VersionAdapter(mInfoItem, platformHelper, entry.value)
                    )
                )
            }

        currentTask?.apply { if (isCancelled) return }

        Task.runTask(TaskExecutors.getAndroidUI()) {
            runCatching {
                var modAdapter = recyclerView.adapter as ModListAdapter?
                modAdapter ?: run {
                    modAdapter = ModListAdapter(this, mData)
                    recyclerView.layoutManager = LinearLayoutManager(fragmentActivity!!)
                    recyclerView.adapter = modAdapter
                    return@runCatching
                }
                modAdapter?.updateData(mData)
            }.getOrElse { e ->
                Logging.e("Set Adapter", Tools.printToString(e))
            }

            componentProcessing(false)
            recyclerView.scheduleLayoutAnimation()

            firstAdaptIndex?.let {
                recyclerView.postDelayed(
                    {
                        // Scroll straight to the previously found first-compatible index, offset two positions down
                        recyclerView.smoothScrollToPosition((it + 2).coerceAtMost(mData.size - 1))
                    },
                    500
                )
            }
        }.execute()
    }

    private fun parseViewModel() {
        val viewModel = ViewModelProvider(fragmentActivity!!)[InfoViewModel::class.java]
        platformHelper = viewModel.platformHelper ?: run {
            ZHTools.onBackPressed(fragmentActivity!!)
            return
        }
        mInfoItem = viewModel.infoItem ?: run {
            ZHTools.onBackPressed(fragmentActivity!!)
            return
        }
    }

    private fun loadScreenshots() {
        val progressBar = createProgressView(fragmentActivity!!)
        addMoreView(progressBar)

        Task.runTask {
            platformHelper.getScreenshots(mInfoItem.projectId)
        }.ended(TaskExecutors.getAndroidUI()) { screenshotItems ->
            screenshotItems?.let addButton@{ items ->
                if (items.isEmpty()) return@addButton
                fragmentActivity?.let { activity ->
                    // Add a button that loads the screenshot data when clicked
                    addMoreView(AnimButton(activity).apply {
                        layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                        setText(R.string.download_info_load_screenshot)
                        setOnClickListener {
                            setScreenshotView(items)
                            AnimPlayer.play().apply(AnimPlayer.Entry(this, Animations.FadeOut))
                                .setOnEnd { removeMoreView(this) }
                                .start()
                        }
                    })
                }
            }
            removeMoreView(progressBar)
        }.onThrowable { e ->
            Logging.e(
                "DownloadModFragment",
                "Unable to load screenshots, ${Tools.printToString(e)}"
            )
        }.execute()
    }

    @SuppressLint("CheckResult")
    private fun setScreenshotView(screenshotItems: List<ScreenshotItem>) {
        fragmentActivity?.let { activity ->
            val recyclerView = RecyclerView(activity).apply {
                layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                layoutManager = LinearLayoutManager(activity)
                adapter = ScreenshotAdapter(screenshotItems)
            }

            addMoreView(recyclerView)
        }
    }

    private fun createProgressView(context: Context): ProgressBar {
        return ProgressBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
    }
}
