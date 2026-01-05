package com.tofiq.peekdetector.feature.panic.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tofiq.peekdetector.MainActivity
import com.tofiq.peekdetector.R
import com.tofiq.peekdetector.feature.panic.PanicAlertConstants
import com.tofiq.peekdetector.feature.panic.service.PanicAlertService

/**
 * Helper object for creating and managing panic alert notifications.
 * Handles notification channel creation and foreground notification building.
 * 
 * Requirements:
 * - 5.2: Display a notification while the siren is active
 * - 5.3: When the user taps the notification, open the app
 */
object PanicAlertNotificationHelper {

    /**
     * Creates the notification channel for panic alerts.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PanicAlertConstants.NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.panic_alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.panic_alert_channel_description)
                setShowBadge(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the foreground notification displayed while the siren is active.
     */
    fun createActiveNotification(context: Context): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = PanicAlertConstants.ACTION_OPEN_FROM_NOTIFICATION
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(context, PanicAlertService::class.java).apply {
            action = PanicAlertConstants.ACTION_STOP_SIREN
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, PanicAlertConstants.NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.panic_alert_active))
            .setContentText(context.getString(R.string.panic_alert_tap_to_open))
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(
                android.R.drawable.ic_media_pause,
                context.getString(R.string.stop),
                stopPendingIntent
            )
            .build()
    }
}
