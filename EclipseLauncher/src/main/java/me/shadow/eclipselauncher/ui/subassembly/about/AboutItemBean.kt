package me.shadow.eclipselauncher.ui.subassembly.about

import android.app.Activity
import android.graphics.drawable.Drawable

/**
 * One entry of the About screen credits list: an icon, a title, a description and
 * an optional link button.
 */
class AboutItemBean(
    val icon: Drawable,
    val title: String,
    val desc: String,
    val buttonBean: AboutItemButtonBean?
) {
    class AboutItemButtonBean(
        val activity: Activity,
        val name: String,
        val url: String
    )
}
