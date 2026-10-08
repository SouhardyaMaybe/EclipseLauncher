package me.shadow.eclipselauncher.ui.fragment.download.resource

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import me.shadow.eclipselauncher.anim.animations.Animations
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.event.value.InstallLocalModpackEvent
import me.shadow.eclipselauncher.feature.download.enums.Classify
import me.shadow.eclipselauncher.feature.download.utils.CategoryUtils
import me.shadow.eclipselauncher.feature.mod.modpack.install.InstallExtra
import me.shadow.eclipselauncher.task.Task
import me.shadow.eclipselauncher.task.TaskExecutors
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.utils.anim.ViewAnimUtils.Companion.setViewAnim
import me.shadow.eclipselauncher.utils.file.FileTools
import me.shadow.eclipselauncher.pojav.Tools
import me.shadow.eclipselauncher.pojav.contracts.OpenDocumentWithExtension
import org.greenrobot.eventbus.EventBus

class ModPackDownloadFragment(parentFragment: Fragment? = null) : AbstractResourceDownloadFragment(
    parentFragment,
    Classify.MODPACK,
    CategoryUtils.getModPackCategory(),
    true
) {
    private var openDocumentLauncher: ActivityResultLauncher<Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(OpenDocumentWithExtension(null)) { uris: List<Uri>? ->
            uris?.let { uriList ->
                uriList[0].let { result ->
                    if (!isTaskRunning()) {
                        val dialog = ZHTools.showTaskRunningDialog(requireContext())
                        Task.runTask {
                            FileTools.copyFileInBackground(requireContext(), result, PathManager.DIR_CACHE.absolutePath)
                        }.ended(TaskExecutors.getAndroidUI()) { modPackFile ->
                            modPackFile?.let {
                                EventBus.getDefault().post(InstallLocalModpackEvent(InstallExtra(true, it.absolutePath)))
                            }
                        }.onThrowable { e ->
                            Tools.showErrorRemote(e)
                        }.finallyTask(TaskExecutors.getAndroidUI()) {
                            dialog.dismiss()
                        }.execute()
                    }
                }
            }
        }
    }

    override fun initInstallButton(installButton: Button) {
        installButton.setOnClickListener {
            if (!isTaskRunning()) {
                Toast.makeText(requireActivity(), getString(R.string.select_modpack_local_tip), Toast.LENGTH_SHORT).show()
                openDocumentLauncher?.launch(null)
            } else {
                setViewAnim(installButton, Animations.Shake)
                Toast.makeText(requireActivity(), getString(R.string.tasks_ongoing), Toast.LENGTH_SHORT).show()
            }
        }
    }
}