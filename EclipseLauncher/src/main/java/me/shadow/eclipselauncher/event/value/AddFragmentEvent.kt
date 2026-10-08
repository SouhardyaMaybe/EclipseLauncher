package me.shadow.eclipselauncher.event.value

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * Add a new Fragment to the transaction manager, where it is received and handled by LauncherActivity
 * Ensure that when a Fragment is added, its parent Fragment is always the current Fragment
 * @see me.shadow.eclipselauncher.pojav.LauncherActivity
 * @see me.shadow.eclipselauncher.utils.ZHTools.addFragment
 */
class AddFragmentEvent(
    val fragmentClass: Class<out Fragment?>,
    val fragmentTag: String?,
    val bundle: Bundle?,
    val fragmentActivityCallback: FragmentActivityCallBack?
) {
    /**
     * Some callback handling for the current Fragment's FragmentActivity
     */
    fun interface FragmentActivityCallBack {
        fun callBack(fragmentActivity: FragmentActivity)
    }
}