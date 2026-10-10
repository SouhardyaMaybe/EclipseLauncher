package me.shadow.eclipselauncher.core.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

import androidx.core.app.NotificationCompat

import me.shadow.eclipselauncher.R

import me.shadow.eclipselauncher.core.Tools

object NotificationUtils {

    const val NOTIFICATION_ID_PROGRESS_SERVICE = 1
    const val NOTIFICATION_ID_GAME_SERVICE = 2
    const val NOTIFICATION_ID_DOWNLOAD_LISTENER = 3
    const val NOTIFICATION_ID_SHOW_ERROR = 4
    const val NOTIFICATION_ID_GAME_START = 5
    const val PENDINGINTENT_CODE_KILL_PROGRESS_SERVICE = 1
    const val PENDINGINTENT_CODE_KILL_GAME_SERVICE = 2
    const val PENDINGINTENT_CODE_DOWNLOAD_SERVICE = 3
    const val PENDINGINTENT_CODE_SHOW_ERROR = 4
    const val PENDINGINTENT_CODE_GAME_START = 5

    @JvmStatic
    fun sendBasicNotification(
        context: Context,
        contentTitle: Int,
        contentText: Int,
        actionIntent: Intent?,
        pendingIntentCode: Int,
        notificationId: Int
    ) {

        val pendingIntent = PendingIntent.getActivity(
            context, pendingIntentCode, actionIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(context, Tools.NOTIFICATION_CHANNEL_DEFAULT)
        if (contentTitle != -1) notificationBuilder.setContentTitle(context.getString(contentTitle))
        if (contentText != -1) notificationBuilder.setContentText(context.getString(contentText))
        if (actionIntent != null) notificationBuilder.setContentIntent(pendingIntent)
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
