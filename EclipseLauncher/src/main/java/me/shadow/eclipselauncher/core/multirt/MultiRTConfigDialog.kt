package me.shadow.eclipselauncher.core.multirt

import android.annotation.SuppressLint
import android.content.Context

import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import me.shadow.eclipselauncher.R

class MultiRTConfigDialog {
    private lateinit var mDialog: AlertDialog
    private lateinit var mDialogView: RecyclerView

    /**
     * Show the dialog, refreshes the adapter data before showing it
     */
    fun show() {
        refresh()
        mDialog.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    //only used to completely refresh the list, it is necessary
    fun refresh() {
        val adapter = mDialogView.adapter ?: return
        adapter.notifyDataSetChanged()
    }

    /**
     * Build the dialog behavior and style
     */
    fun prepare(activity: Context, installJvmLauncher: ActivityResultLauncher<Any?>) {
        mDialogView = RecyclerView(activity)
        mDialogView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        val adapter = RTRecyclerViewAdapter(MultiRTUtils.getRuntimes())
        mDialogView.adapter = adapter

        mDialog = AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme)
            .setTitle(R.string.multirt_config_title)
            .setView(mDialogView)
            .setPositiveButton(R.string.multirt_import) { _, _ -> installJvmLauncher.launch(null) }
            .setNeutralButton(R.string.multirt_delete_runtime, null)
            .create()

        // Custom button behavior without dismiss
        mDialog.setOnShowListener { dialog ->
            val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_NEUTRAL)
            button.setOnClickListener {
                val isEditing = !adapter.getIsEditing()
                adapter.setIsEditing(isEditing)
                button.setText(if (isEditing) R.string.multirt_swap_setdefault else R.string.multirt_delete_runtime)
            }
        }
    }
}
