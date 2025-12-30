package com.tofiq.peekdetector

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Foreground service for panic alert audio playback.
 * Manages siren sound playback with volume control and background operation.
 * 
 * Requirements:
 * - 2.1: Play siren sound when activated
 * - 2.2: Set device volume to maximum when playing
 * - 2.3: Loop siren sound continuously until stopped
 * - 2.5: Use STREAM_ALARM audio stream for maximum audibility
 * - 3.2: Stop siren immediately when stop action is triggered
 * - 3.3: Restore device volume to previous level when stopped
 * - 5.1: Continue playing when app is backgrounded
 * - 5.2: Display notification while siren is active
 */
class PanicAlertService : Service() {

    companion object {
        private const val TAG = "PanicAlertService"
        
        /**
         * Observable state for UI to track whether the panic alert is active.
         * Exposed as MutableState for Jetpack Compose observation.
         */
        val isActive: MutableState<Boolean> = mutableStateOf(false)
        
        /**
         * Starts the panic alert service.
         * @param context The context to use for starting the service
         */
        fun start(context: Context) {
            val intent = Intent(context, PanicAlertService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * Stops the panic alert service.
         * @param context The context to use for stopping the service
         */
        fun stop(context: Context) {
            val intent = Intent(context, PanicAlertService::class.java)
            context.stopService(intent)
        }
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var previousVolume: Int = -1
    private var wakeLock: PowerManager.WakeLock? = null
    
    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        PanicAlertNotificationHelper.createNotificationChannel(this)
        
        // Acquire a partial wake lock to ensure audio continues in background
        // Requirement 5.1: Continue playing when app is backgrounded
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PeekDetector:PanicAlertWakeLock"
        )
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check if this is a stop action from notification
        if (intent?.action == PanicAlertConstants.ACTION_STOP_SIREN) {
            stopSiren()
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Start foreground service with notification (Requirement 5.2)
        startForeground(PanicAlertConstants.NOTIFICATION_ID, PanicAlertNotificationHelper.createActiveNotification(this))
        
        // Acquire wake lock to keep CPU running for audio playback (Requirement 5.1)
        wakeLock?.let { lock ->
            if (!lock.isHeld) {
                lock.acquire(30 * 60 * 1000L) // 30 minutes max
                Log.d(TAG, "Wake lock acquired for background audio")
            }
        }
        
        // Start the siren
        startSiren()
        
        // START_STICKY ensures service restarts if killed by system
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopSiren()
        releaseWakeLock()
        isActive.value = false
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * Starts the siren audio playback.
     * - Saves current volume level
     * - Sets volume to maximum
     * - Initializes and starts MediaPlayer with looping
     * 
     * Requirements: 2.1, 2.2, 2.3, 2.5, 5.1
     */
    private fun startSiren() {
        try {
            // Save previous volume (Requirement 3.3)
            audioManager?.let { am ->
                previousVolume = am.getStreamVolume(PanicAlertConstants.AUDIO_STREAM_TYPE)
                
                // Set volume to maximum (Requirement 2.2)
                val maxVolume = am.getStreamMaxVolume(PanicAlertConstants.AUDIO_STREAM_TYPE)
                am.setStreamVolume(
                    PanicAlertConstants.AUDIO_STREAM_TYPE,
                    maxVolume,
                    0 // No flags - silent volume change
                )
                Log.d(TAG, "Volume set to max: $maxVolume (previous: $previousVolume)")
            }
            
            // Initialize MediaPlayer with siren audio (Requirement 2.1)
            // Try to load the siren audio resource
            val sirenResId = resources.getIdentifier("siren", "raw", packageName)
            if (sirenResId != 0) {
                mediaPlayer = MediaPlayer().apply {
                    // Set audio attributes for alarm stream (Requirement 2.5)
                    // This ensures audio plays even in background (Requirement 5.1)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    
                    // Set data source from raw resource
                    val afd = resources.openRawResourceFd(sirenResId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    
                    // Prepare the player
                    prepare()
                    
                    // Enable looping (Requirement 2.3)
                    isLooping = true
                    
                    // Set wake mode to keep playing in background (Requirement 5.1)
                    setWakeMode(this@PanicAlertService, PowerManager.PARTIAL_WAKE_LOCK)
                    
                    // Start playback
                    start()
                    Log.d(TAG, "Siren started - looping: $isLooping")
                }
                
                if (mediaPlayer == null) {
                    Log.e(TAG, "Failed to create MediaPlayer - siren audio may be corrupted")
                }
            } else {
                Log.e(TAG, "Siren audio resource not found - please add siren.wav to res/raw/")
            }
            
            isActive.value = true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting siren", e)
            // Restore volume on error
            restoreVolume()
        }
    }
    
    /**
     * Stops the siren audio playback.
     * - Stops and releases MediaPlayer
     * - Restores previous volume level
     * 
     * Requirements: 3.2, 3.3
     */
    private fun stopSiren() {
        try {
            // Stop and release MediaPlayer (Requirement 3.2)
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
                Log.d(TAG, "Siren stopped and released")
            }
            mediaPlayer = null
            
            // Restore previous volume (Requirement 3.3)
            restoreVolume()
            
            // Release wake lock
            releaseWakeLock()
            
            isActive.value = false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping siren", e)
            isActive.value = false
        }
    }
    
    /**
     * Restores the device volume to the previously saved level.
     * 
     * Requirement: 3.3
     */
    private fun restoreVolume() {
        if (previousVolume >= 0) {
            audioManager?.setStreamVolume(
                PanicAlertConstants.AUDIO_STREAM_TYPE,
                previousVolume,
                0 // No flags - silent volume change
            )
            Log.d(TAG, "Volume restored to: $previousVolume")
            previousVolume = -1
        }
    }
    
    /**
     * Releases the wake lock if held.
     */
    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
                Log.d(TAG, "Wake lock released")
            }
        }
    }
    
}
