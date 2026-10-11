package me.shadow.eclipselauncher.core.lifecycle

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Process

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.context.ContextExecutor
import me.shadow.eclipselauncher.feature.version.Version
import me.shadow.eclipselauncher.setting.AllSettings

import me.shadow.eclipselauncher.core.MainActivity
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.progresskeeper.ProgressKeeper
import me.shadow.eclipselauncher.core.utils.NotificationUtils

/**
 * NOTE: onDownloadDone()/onDownloadFailed() are plain public methods (not overrides): the
 * downloader listener interface they used to implement was removed by the tasks rewrite, while
 * launch/LaunchGame.kt still calls them directly on this object.
 */
class ContextAwareDoneListener(baseContext: Context, private val mVersion: Version) : ContextExecutorTask {
    private val mErrorString: String = baseContext.getString(R.string.mc_download_failed)

    private fun createGameStartIntent(context: Context): Intent {
        val mainIntent = Intent(context, MainActivity::class.java)
        mainIntent.putExtra(MainActivity.INTENT_VERSION, mVersion)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return mainIntent
    }

    private fun executeTask() {
        ProgressKeeper.waitUntilDone { ContextExecutor.executeTask(this) }
    }

    fun onDownloadDone() {
        executeTask()
    }

    fun onDownloadFailed(throwable: Throwable) {
        Tools.showErrorRemote(mErrorString, throwable)
    }

    override fun executeWithActivity(activity: Activity) {
        try {
            val gameStartIntent = createGameStartIntent(activity)
            activity.startActivity(gameStartIntent)
            if (AllSettings.quitLauncher.getValue()) {
                activity.finish()
                Process.killProcess(Process.myPid()) //You should kill yourself, NOW!
            }
        } catch (e: Throwable) {
            Tools.showError(activity.baseContext, e)
        }
    }

    override fun executeWithApplication(context: Context) {
        val gameStartIntent = createGameStartIntent(context)
        // Since the game is a separate process anyway, it does not matter if it gets invoked
        // from somewhere other than the launcher activity.
        // The only problem may arise if the launcher starts doing something when the user starts the notification.
        // So, the notification is automatically removed once there are tasks ongoing in the ProgressKeeper
        NotificationUtils.sendBasicNotification(
            context,
            R.string.notif_download_finished,
            R.string.notif_download_finished_desc,
            gameStartIntent,
            NotificationUtils.PENDINGINTENT_CODE_GAME_START,
            NotificationUtils.NOTIFICATION_ID_GAME_START
        )
        // You should keep yourself safe, NOW!
        // otherwise android does weird things...
    }
}
