package me.shadow.eclipselauncher.ui.fragment

import android.content.res.Configuration
import androidx.fragment.app.Fragment
import me.shadow.eclipselauncher.pojav.progresskeeper.ProgressKeeper
import me.shadow.eclipselauncher.pojav.progresskeeper.TaskCountListener

abstract class BaseFragment : Fragment, TaskCountListener {
    private var mIsTaskRunning: Boolean = false
    private var paneSwitcher: PaneSwitcher? = null

    constructor() : super()

    constructor(contentLayoutId: Int) : super(contentLayoutId)

    open fun onBackPressed(): Boolean = true

    fun isTaskRunning() = mIsTaskRunning

    fun forceBack() {
        requireActivity().supportFragmentManager.popBackStackImmediate()
    }

    /** Shows the secondary pane in portrait single-pane mode; no-op in landscape. */
    fun openPane() {
        paneSwitcher?.openPane()
    }

    /** Hides the secondary pane again; returns true when one was visible. */
    fun closeOpenPane(): Boolean {
        return paneSwitcher?.closePane() ?: false
    }

    private fun ensurePaneSwitcher() {
        val currentView = view ?: return
        val existing = paneSwitcher
        if (existing != null && existing.root === currentView) {
            existing.applyOrientation(currentView.resources.configuration)
            return
        }
        val entry = PaneRegistry.find(this) ?: return
        val switcher = PaneSwitcher(
            currentView, entry.left, entry.right, entry.openerLabel, entry.mode
        )
        paneSwitcher = switcher
        switcher.openerButton?.setOnClickListener { switcher.togglePane() }
        switcher.applyOrientation(currentView.resources.configuration)
    }

    /**
     * Re-applies the pane layout from the view's current configuration.
     * Called by the activity after every configuration change as a safety net;
     * [applyOrientation] is idempotent, so duplicate calls are harmless.
     */
    fun reapplyPaneOrientation() {
        ensurePaneSwitcher()
    }

    override fun onStart() {
        super.onStart()
        ProgressKeeper.addTaskCountListener(this)
        ensurePaneSwitcher()
    }

    override fun onStop() {
        super.onStop()
        ProgressKeeper.removeTaskCountListener(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        reapplyPaneOrientation()
    }

    override fun onUpdateTaskCount(taskCount: Int) {
        this.mIsTaskRunning = taskCount != 0
    }
}
