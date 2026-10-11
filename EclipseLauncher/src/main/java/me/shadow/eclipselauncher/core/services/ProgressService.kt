package me.shadow.eclipselauncher.core.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import me.shadow.eclipselauncher.InfoCenter
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.utils.EclipseTools

import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.progresskeeper.ProgressKeeper
import me.shadow.eclipselauncher.core.progresskeeper.TaskCountListener
import me.shadow.eclipselauncher.core.utils.NotificationUtils

/**
 * Lazy service which allows the process not to get killed.
 * Can be created from context, can be killed statically
 */
class ProgressService : Service(), TaskCountListener {

    private lateinit var notificationManagerCompat: NotificationManagerCompat

    private lateinit var mNotificationBuilder: NotificationCompat.Builder

    override fun onCreate() {
        Tools.buildNotificationChannel(applicationContext)
        notificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        val killIntent = Intent(applicationContext, ProgressService::class.java)
        killIntent.putExtra("kill", true)
        val pendingKillIntent = PendingIntent.getService(this, NotificationUtils.PENDINGINTENT_CODE_KILL_PROGRESS_SERVICE,
            killIntent, PendingIntent.FLAG_IMMUTABLE)
        mNotificationBuilder = NotificationCompat.Builder(this, Tools.NOTIFICATION_CHANNEL_DEFAULT)
            .setContentTitle(InfoCenter.replaceName(this, R.string.lazy_service_default_title))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_terminate), pendingKillIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setSilent(true)
    }

    @SuppressLint("StringFormatInvalid")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.getBooleanExtra("kill", false)) {
                stopSelf() // otherwise Android tries to restart the service since it "crashed"
                Process.killProcess(Process.myPid())
                return START_NOT_STICKY
            }
        }
        Logging.d("ProgressService", "Started!")
        mNotificationBuilder.setContentText(getString(R.string.progresslayout_tasks_in_progress, ProgressKeeper.getTaskCount()))
        val notification: Notification = mNotificationBuilder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationUtils.NOTIFICATION_ID_PROGRESS_SERVICE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_PROGRESS_SERVICE, notification)
        }
        if (ProgressKeeper.getTaskCount() < 1) stopSelf()
        else ProgressKeeper.addTaskCountListener(this, false)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        ProgressKeeper.removeTaskCountListener(this)
    }

    override fun onUpdateTaskCount(taskCount: Int) {
        if (EclipseTools.checkForNotificationPermission()) {
            // Was TaskExecutors.runInUIThread: a Dispatchers.Main coroutine parks this update
            // on the UI thread exactly like the old main-looper Handler posting did.
            uiScope.launch {
                if (taskCount > 0) {
                    mNotificationBuilder.setContentText(getString(R.string.progresslayout_tasks_in_progress, taskCount))
                    notificationManagerCompat.notify(1, mNotificationBuilder.build())
                } else {
                    stopSelf()
                }
            }
        }
    }

    companion object {
        /** Runs where TaskExecutors.runInUIThread did: on the main thread */
        private val uiScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName("ProgressService"))

        /** Simple wrapper to start the service */
        @JvmStatic
        fun startService(context: Context) {
            val intent = Intent(context, ProgressService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
