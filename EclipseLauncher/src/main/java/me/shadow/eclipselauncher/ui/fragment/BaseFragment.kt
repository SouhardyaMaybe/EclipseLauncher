package me.shadow.eclipselauncher.ui.fragment

import android.content.res.Configuration
import android.view.View
import androidx.fragment.app.Fragment
import me.shadow.eclipselauncher.pojav.progresskeeper.ProgressKeeper
import me.shadow.eclipselauncher.pojav.progresskeeper.TaskCountListener

abstract class BaseFragment : Fragment, TaskCountListener {
    private var mIsTaskRunning: Boolean = false
    private var paneSwitcher: PaneSwitcher? = null

    /**
     * Optional custom action for the portrait pane-opener button
     * (e.g. a section picker). When null the button toggles the secondary pane.
     * Set this from onViewCreated; it is wired when the switcher is created.
     */
    var onPaneOpenerClick: (() -> Unit)? = null

    /** The floating portrait opener button of this fragment (available after onStart). */
    val paneOpenerButton: View?
        get() = paneSwitcher?.openerButton

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
        switcher.openerButton?.setOnClickListener {
            val custom = onPaneOpenerClick
            if (custom != null) custom() else switcher.togglePane()
        }
        switcher.applyOrientation(currentView.resources.configuration)
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
        paneSwitcher?.applyOrientation(newConfig)
    }

    override fun onUpdateTaskCount(taskCount: Int) {
        this.mIsTaskRunning = taskCount != 0
    }
}
