package com.tofiq.peekdetector.feature.panic

import android.media.AudioManager

/**
 * Constants for the Panic Alert feature.
 * 
 * Requirements:
 * - 1.4: SWIPE_THRESHOLD set to 80% of slider width to prevent accidental activation
 * - 2.5: Uses STREAM_ALARM audio stream for maximum audibility
 */
object PanicAlertConstants {
    /**
     * The minimum swipe distance as a percentage of slider width required to trigger the alert.
     * Set to 80% (0.8f) to prevent accidental activation.
     */
    const val SWIPE_THRESHOLD = 0.8f

    /**
     * Notification channel ID for panic alert notifications.
     */
    const val NOTIFICATION_CHANNEL_ID = "panic_alert_channel"

    /**
     * Notification ID for the panic alert foreground service notification.
     */
    const val NOTIFICATION_ID = 2001

    /**
     * Intent action to stop the siren from notification.
     */
    const val ACTION_STOP_SIREN = "com.tofiq.peekdetector.STOP_SIREN"

    /**
     * Intent action when opening app from panic alert notification.
     */
    const val ACTION_OPEN_FROM_NOTIFICATION = "com.tofiq.peekdetector.OPEN_FROM_PANIC_NOTIFICATION"

    /**
     * Audio stream type for siren playback.
     * Uses STREAM_ALARM for maximum audibility.
     */
    const val AUDIO_STREAM_TYPE = AudioManager.STREAM_ALARM
}
