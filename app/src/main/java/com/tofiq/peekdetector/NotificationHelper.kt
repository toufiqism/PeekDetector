package com.tofiq.peekdetector

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tofiq.peekdetector.data.SettingsRepositoryImpl
import com.tofiq.peekdetector.data.settingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Helper class to manage notifications for the PeekDetector app
 * Handles notification channels and creation with proper Android versioning support
 * 
 * Requirements: 4.2, 4.3
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
    
    // Settings repository for reading vibration preference
    private val settingsRepository: SettingsRepositoryImpl by lazy {
        SettingsRepositoryImpl(context.settingsDataStore)
    }
    
    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Cached vibration setting (updated asynchronously)
    @Volatile
    private var vibrationEnabled = true

    init {
        createNotificationChannels()
        // Start observing vibration setting
        observeVibrationSetting()
    }
    
    /**
     * Observes the vibration setting and updates the cached value.
     * 
     * Requirements: 4.2, 4.3
     */
    private fun observeVibrationSetting() {
        scope.launch {
            settingsRepository.vibrationEnabled.collect { enabled ->
                vibrationEnabled = enabled
            }
        }
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
     * Conditionally includes vibration based on user settings.
     * 
     * @param faceCount Number of faces detected
     * 
     * Requirements: 4.2, 4.3
     */
    fun showMultipleFacesAlert(faceCount: Int) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
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
        
        // Conditionally add vibration based on user setting (Requirements: 4.2, 4.3)
        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 500, 200, 500)) // Vibration pattern
        } else {
            builder.setVibrate(longArrayOf(0)) // No vibration
        }

        notificationManager.notify(ALERT_NOTIFICATION_ID, builder.build())
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

