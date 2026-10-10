package me.shadow.eclipselauncher.utils.runtime

import android.content.Context
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.ui.dialog.SelectRuntimeDialog

class SelectRuntimeUtils {
    companion object {
        private val selectScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName("SelectRuntimeUtils"))

        @JvmStatic
        fun selectRuntime(context: Context, dialogTitle: String?, selectedListener: RuntimeSelectedListener) {
            // Was posted to the UI thread through TaskExecutors; Dispatchers.Main does the same
            selectScope.launch {
                when (AllSettings.selectRuntimeMode.getValue()) {
                    "ask_me" -> SelectRuntimeDialog(context, selectedListener).apply {
                        dialogTitle?.let { setTitleText(it) }
                    }.show()
                    "default" -> selectedListener.onSelected(AllSettings.defaultRuntime.getValue().takeIf { it.isNotEmpty() })
                    "auto" -> selectedListener.onSelected(null)
                    else -> {}
                }
            }
        }
    }
}
