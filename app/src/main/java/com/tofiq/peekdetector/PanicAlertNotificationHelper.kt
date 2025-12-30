package com.tofiq.peekdetector

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Helper object for creating and managing panic alert notifications.
 * Handles notification channel creation and foreground notification building.
 * 
 * Requirements:
 * - 5.2: Display a notification while the siren is active, allowing the user to stop it
 * - 5.3: When the user taps the notification, open the app and display the stop button
 */
object PanicAlertNotificationHelper {

    /**
     * Creates the notification channel for panic alerts.
     * Required for Android O (API 26) and above.
     * 
     * @param context The context to use for creating the channel
     * 
     * Requirement: 5.2
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PanicAlertConstants.NOTIFICATION_CHANNEL_ID,
                "Panic Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Panic alert siren notifications"
                setShowBadge(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the foreground notification displayed while the siren is active.
     * Includes a stop action to allow stopping from the notification.
     * 
     * @param context The context to use for creating the notification
     * @return The notification to display
     * 
     * Requirements: 5.2, 5.3
     */
    fun createActiveNotification(context: Context): Notification {
        // Intent to open the app when notification is tapped (Requirement 5.3)
        // Using SINGLE_TOP launch mode in manifest ensures we return to existing activity
        // FLAG_ACTIVITY_SINGLE_TOP prevents creating a new instance if one exists
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Add action to indicate this is from panic alert notification
            action = PanicAlertConstants.ACTION_OPEN_FROM_NOTIFICATION
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intent to stop the siren from notification (Requirement 5.2)
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
            .setContentTitle("🚨 PANIC ALERT ACTIVE")
            .setContentText("Tap to open app or press STOP to silence")
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(
                android.R.drawable.ic_media_pause,
                "STOP",
                stopPendingIntent
            )
            .build()
    }
}
