package me.shadow.eclipselauncher.ui.fragment.settings

import androidx.annotation.CallSuper
import me.shadow.eclipselauncher.anim.AnimPlayer
import me.shadow.eclipselauncher.event.single.SettingsChangeEvent
import me.shadow.eclipselauncher.event.value.SettingsPageSwapEvent
import me.shadow.eclipselauncher.ui.fragment.FragmentWithAnim
import me.shadow.eclipselauncher.pojav.prefs.LauncherPreferences
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

abstract class AbstractSettingsFragment(layoutId: Int, private val category: SettingCategory) : FragmentWithAnim(layoutId) {
    @Subscribe
    fun event(event: SettingsChangeEvent) {
        onChange()
    }

    @Subscribe
    fun event(event: SettingsPageSwapEvent) {
        if (event.index == category.ordinal) {
            slideIn()
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @CallSuper
    protected open fun onChange() {
        LauncherPreferences.loadPreferences()
    }

    override fun slideOut(animPlayer: AnimPlayer) {}
}