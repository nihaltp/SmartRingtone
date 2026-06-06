package com.nihaltp.smartringtone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "ringtone_sync_channel"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Ringtone Sync Progress"
            val descriptionText = "Shows progress of background contact ringtone updates"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel =
                NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    setSound(null, null)
                    enableVibration(false)
                    enableLights(false)
                }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgressNotification(
        context: Context,
        progress: Int,
        total: Int,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        createNotificationChannel(context)

        val percentage = if (total > 0) (progress * 100) / total else 0
        val contentText = "$progress/$total ($percentage%)"

        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Updating contact ringtones")
                .setContentText(contentText)
                .setProgress(total, progress, false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        progress: Int = 0,
        maxProgress: Int = 0,
        indeterminate: Boolean = false,
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        createNotificationChannel(context)

        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(title)
                .setContentText(message)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)

        if (maxProgress > 0 || indeterminate) {
            builder.setProgress(maxProgress, progress, indeterminate)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun dismissNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
