package me.shadow.eclipselauncher.feature.unpack

import android.content.Context
import me.shadow.eclipselauncher.feature.log.Logging.e
import me.shadow.eclipselauncher.utils.CopyDefaultFromAssets.Companion.copyFromAssets
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.pojav.Tools

class UnpackSingleFilesTask(val context: Context) : AbstractUnpackTask() {
    override fun isNeedUnpack(): Boolean = true

    override fun run() {
        runCatching {
            copyFromAssets(context)
            Tools.copyAssetFile(context, "resolv.conf", PathManager.DIR_DATA, false)
        }.getOrElse { e("AsyncAssetManager", "Failed to unpack critical components !") }
    }
}