package com.tofiq.peekdetector

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Helper class to manage notifications for the PeekDetector app
 * Handles notification channels and creation with proper Android versioning support
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val FOREGROUND_CHANNEL_ID = "peek_detection_channel"
        const val ALERT_CHANNEL_ID = "peek_alert_channel"
        const val FOREGROUND_NOTIFICATION_ID = 1
        const val ALERT_NOTIFICATION_ID = 2
        
        private const val FOREGROUND_CHANNEL_NAME = "Peek Detection Service"
        private const val ALERT_CHANNEL_NAME = "Peek Detection Alerts"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Creates notification channels for Android O and above
     * Separate channels for foreground service and alerts
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for foreground service
            val foregroundChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                FOREGROUND_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Peek Detection is running"
                setShowBadge(false)
            }

            // Channel for peek detection alerts
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when multiple faces are detected"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(foregroundChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    /**
     * Creates a foreground service notification
     * @return Notification object for the foreground service
     */
    fun createForegroundNotification(): android.app.Notification {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Peek Protection is Active")
            .setContentText("Monitoring for shoulder surfers.")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Shows an alert notification when multiple faces are detected
     * @param faceCount Number of faces detected
     */
    fun showMultipleFacesAlert(faceCount: Int) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ Multiple Faces Detected!")
            .setContentText("$faceCount faces detected. Someone might be peeking!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$faceCount faces detected. Someone might be looking at your screen. Please be aware of your surroundings.")
            )
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibration pattern
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    /**
     * Cancels the alert notification
     */
    fun cancelAlertNotification() {
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
    }

    /**
     * Checks if notification permission is granted (for Android 13+)
     * @return true if permission is granted or not required, false otherwise
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationManager.areNotificationsEnabled()
        } else {
            true // Permission not required for older versions
        }
    }
}

