package me.shadow.eclipselauncher.ui.fragment.settings.wrapper

import android.content.Context
import android.view.View
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.ui.dialog.TipDialog
import me.shadow.eclipselauncher.utils.ZHTools

abstract class AbstractSettingsWrapper(
    private val mainView: View
) {
    private var isRequiresReboot = false

    fun setRequiresReboot(): AbstractSettingsWrapper {
        isRequiresReboot = true
        return this
    }

    fun checkShowRebootDialog(context: Context) {
        if (isRequiresReboot) {
            TipDialog.Builder(context)
                .setTitle(R.string.generic_tip)
                .setMessage(R.string.setting_reboot_tip)
                .setConfirmClickListener { ZHTools.killProcess() }
                .showDialog()
        }
    }

    fun setGone() {
        mainView.visibility = View.GONE
    }
}