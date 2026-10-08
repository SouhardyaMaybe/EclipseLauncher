package me.shadow.eclipselauncher.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.DialogUpdateBinding
import me.shadow.eclipselauncher.feature.update.LauncherVersion
import me.shadow.eclipselauncher.feature.update.LauncherVersion.WhatsNew
import me.shadow.eclipselauncher.feature.update.UpdateLauncher
import me.shadow.eclipselauncher.feature.update.UpdateUtils.Companion.getFileSize
import me.shadow.eclipselauncher.setting.AllSettings.Companion.ignoreUpdate
import me.shadow.eclipselauncher.task.TaskExecutors.Companion.runInUIThread
import me.shadow.eclipselauncher.ui.dialog.DraggableDialog.DialogInitializationListener
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.utils.file.FileTools.Companion.formatFileSize
import me.shadow.eclipselauncher.utils.stringutils.StringUtils

class UpdateDialog(context: Context, private val launcherVersion: LauncherVersion) :
    FullScreenDialog(context), DialogInitializationListener {
    private val binding = DialogUpdateBinding.inflate(
        layoutInflater
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setCancelable(false)
        this.setContentView(binding.root)

        init()
        DraggableDialog.initDialog(this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun init() {
        binding.apply {
            getLanguageText(launcherVersion.title).takeIf { it != "NONE" }?.let { titleString ->
                titleText.visibility = View.VISIBLE
                titleText.text = titleString
            }

            versionName.text = StringUtils.insertSpace(context.getString(R.string.update_dialog_version), launcherVersion.versionName)
            updateTime.text = StringUtils.insertSpace(context.getString(R.string.update_dialog_time), StringUtils.formattingTime(launcherVersion.publishedAt))
            fileSize.text = StringUtils.insertSpace(context.getString(R.string.update_dialog_file_size), formatFileSize(getFileSize(launcherVersion.fileSize)))
            versionType.text = StringUtils.insertSpace(context.getString(R.string.about_version_status), getVersionType())

            ZHTools.getWebViewAfterProcessing(description)

            description.settings.javaScriptEnabled = true
            description.loadDataWithBaseURL(null, StringUtils.markdownToHtml(getLanguageText(launcherVersion.description)), "text/html", "UTF-8", null)

            updateButton.setOnClickListener {
                dismiss()
                runInUIThread {
                    Toast.makeText(context, context.getString(R.string.update_downloading_tip, "Github Release"), Toast.LENGTH_SHORT).show()
                }
                val updateLauncher = UpdateLauncher(context, launcherVersion)
                updateLauncher.start()
            }
            cancelButton.setOnClickListener { dismiss() }
            ignoreButton.setOnClickListener {
                ignoreUpdate.put(launcherVersion.versionName).save()
                dismiss()
            }
        }
    }

    private fun getVersionType(): String {
        return context.getString(if (launcherVersion.isPreRelease) R.string.generic_pre_release else R.string.generic_release)
    }

    private fun getLanguageText(whatsNew: WhatsNew): String {
        return whatsNew.enUS
    }

    override fun onInit(): Window? {
        return window
    }
}
