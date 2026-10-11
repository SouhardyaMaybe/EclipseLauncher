package me.shadow.eclipselauncher.core.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process

import androidx.core.app.NotificationCompat

import me.shadow.eclipselauncher.InfoCenter
import me.shadow.eclipselauncher.R

import me.shadow.eclipselauncher.core.MainActivity
import me.shadow.eclipselauncher.core.Tools
import me.shadow.eclipselauncher.core.utils.NotificationUtils

class GameService : Service() {
    private val mMessenger = Messenger(IncomingHandler())

    override fun onCreate() {
        Tools.buildNotificationChannel(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.getBooleanExtra("kill", false)) {
            stopSelf()
            Process.killProcess(Process.myPid())
            return START_NOT_STICKY
        }
        val killIntent = Intent(applicationContext, GameService::class.java)
        killIntent.putExtra("kill", true)
        val pendingKillIntent = PendingIntent.getService(this, NotificationUtils.PENDINGINTENT_CODE_KILL_GAME_SERVICE,
            killIntent, PendingIntent.FLAG_IMMUTABLE)
        val contentIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            PendingIntent.FLAG_IMMUTABLE)
        val notificationBuilder = NotificationCompat.Builder(this, Tools.NOTIFICATION_CHANNEL_DEFAULT)
            .setContentTitle(InfoCenter.replaceName(this, R.string.lazy_service_default_title))
            .setContentText(getString(R.string.notification_game_runs))
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_terminate), pendingKillIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setSilent(true)

        val notification: Notification = notificationBuilder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { //SDK 34+
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            }
            startForeground(NotificationUtils.NOTIFICATION_ID_GAME_SERVICE, notification, serviceType)
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_GAME_SERVICE, notification)
        }
        return START_NOT_STICKY // non-sticky so android wont try restarting the game after the user uses the "Quit" button
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        //At this point in time  only the game runs and the user poofed the window, time to die
        stopSelf()
        Process.killProcess(Process.myPid())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mMessenger.binder
    }

    /**
     * The Messenger binder is a platform IPC primitive with no coroutine equivalent, so the
     * main-looper Handler stays exactly as it was.
     */
    private class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
        }
    }

    companion object {
        private var sIsActive = false

        @JvmStatic
        fun isActive(): Boolean {
            return sIsActive
        }

        @JvmStatic
        fun setActive(active: Boolean) {
            sIsActive = active
        }
    }
}
