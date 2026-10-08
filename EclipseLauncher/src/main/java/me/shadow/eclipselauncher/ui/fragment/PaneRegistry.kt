package me.shadow.eclipselauncher.ui.fragment

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import me.shadow.eclipselauncher.R

/**
 * Maps each two-pane fragment to its pane views and its portrait single-pane mode.
 * Entries match subclasses too (e.g. every mod download page built on ModListFragment).
 */
object PaneRegistry {

    class Entry(
        @IdRes val left: Int,
        @IdRes val right: Int,
        @IdRes val openerLabel: Int,
        val mode: PaneSwitcher.Mode = PaneSwitcher.Mode.OVERLAY
    )

    private val entries: List<Pair<Class<*>, Entry>> = listOf(
        me.shadow.eclipselauncher.pojav.fragments.MainMenuFragment::class.java to
            Entry(R.id.play_layout, R.id.launcher_menu, R.string.pane_menu, PaneSwitcher.Mode.SWITCH),
        me.shadow.eclipselauncher.ui.fragment.VersionsListFragment::class.java to
            Entry(R.id.version_layout, R.id.operate_layout, R.string.pane_actions),
        me.shadow.eclipselauncher.ui.fragment.VersionSelectorFragment::class.java to
            Entry(R.id.version_layout, R.id.operate_layout, R.string.pane_search),
        me.shadow.eclipselauncher.ui.fragment.VersionConfigFragment::class.java to
            Entry(R.id.editor_layout, R.id.operate_layout, R.string.pane_done),
        me.shadow.eclipselauncher.ui.fragment.ModsFragment::class.java to
            Entry(R.id.mods_layout, R.id.operate_layout, R.string.pane_actions),
        me.shadow.eclipselauncher.ui.subassembly.modlist.ModListFragment::class.java to
            Entry(R.id.mods_layout, R.id.operate_layout, R.string.pane_details),
        me.shadow.eclipselauncher.ui.fragment.FilesFragment::class.java to
            Entry(R.id.files_layout, R.id.operate_layout, R.string.pane_actions),
        me.shadow.eclipselauncher.ui.fragment.AccountFragment::class.java to
            Entry(R.id.account_menu, R.id.operation_layout, R.string.pane_details),
        me.shadow.eclipselauncher.ui.fragment.ControlButtonFragment::class.java to
            Entry(R.id.control_layout, R.id.operate_layout, R.string.pane_actions),
        me.shadow.eclipselauncher.ui.fragment.CustomBackgroundFragment::class.java to
            Entry(R.id.background_layout, R.id.operate_layout, R.string.pane_actions),
        me.shadow.eclipselauncher.ui.fragment.CustomMouseFragment::class.java to
            Entry(R.id.mouse_layout, R.id.operate_layout, R.string.pane_actions),
        me.shadow.eclipselauncher.pojav.fragments.GamepadMapperFragment::class.java to
            Entry(R.id.controller_layout, R.id.operate_layout, R.string.pane_options),
        me.shadow.eclipselauncher.ui.fragment.download.resource.AbstractResourceDownloadFragment::class.java to
            Entry(R.id.download_layout, R.id.operate_layout, R.string.pane_options),
        me.shadow.eclipselauncher.ui.fragment.VersionManagerFragment::class.java to
            Entry(R.id.shortcuts_layout, R.id.edit_layout, R.string.pane_actions, PaneSwitcher.Mode.SWITCH),
        me.shadow.eclipselauncher.ui.fragment.SettingsFragment::class.java to
            Entry(R.id.settings_layout, R.id.settings_viewpager, R.string.pane_sections, PaneSwitcher.Mode.RAIL),
        me.shadow.eclipselauncher.ui.fragment.DownloadFragment::class.java to
            Entry(R.id.classify_layout, R.id.download_viewpager, R.string.pane_sections, PaneSwitcher.Mode.RAIL)
    )

    /** Finds the pane entry for a fragment, including its superclasses. */
    fun find(fragment: Fragment): Entry? = entries.firstOrNull { (cls, _) -> cls.isInstance(fragment) }?.second
}
