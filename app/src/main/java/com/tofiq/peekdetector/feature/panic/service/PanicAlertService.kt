package com.tofiq.peekdetector.feature.panic.service

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
import com.tofiq.peekdetector.core.util.CrashlyticsHelper
import com.tofiq.peekdetector.feature.panic.PanicAlertConstants
import com.tofiq.peekdetector.feature.panic.notification.PanicAlertNotificationHelper

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
         */
        val isActive: MutableState<Boolean> = mutableStateOf(false)
        
        /**
         * Starts the panic alert service.
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
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PeekDetector:PanicAlertWakeLock"
        )
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == PanicAlertConstants.ACTION_STOP_SIREN) {
            stopSiren()
            stopSelf()
            return START_NOT_STICKY
        }
        
        startForeground(PanicAlertConstants.NOTIFICATION_ID, PanicAlertNotificationHelper.createActiveNotification(this))
        
        wakeLock?.let { lock ->
            if (!lock.isHeld) {
                lock.acquire(30 * 60 * 1000L)
                Log.d(TAG, "Wake lock acquired for background audio")
            }
        }
        
        startSiren()
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopSiren()
        releaseWakeLock()
        isActive.value = false
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startSiren() {
        try {
            CrashlyticsHelper.logPanicAlertState(true)
            audioManager?.let { am ->
                previousVolume = am.getStreamVolume(PanicAlertConstants.AUDIO_STREAM_TYPE)
                
                val maxVolume = am.getStreamMaxVolume(PanicAlertConstants.AUDIO_STREAM_TYPE)
                am.setStreamVolume(
                    PanicAlertConstants.AUDIO_STREAM_TYPE,
                    maxVolume,
                    0
                )
                Log.d(TAG, "Volume set to max: $maxVolume (previous: $previousVolume)")
            }
            
            val sirenResId = resources.getIdentifier("siren", "raw", packageName)
            if (sirenResId != 0) {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    
                    val afd = resources.openRawResourceFd(sirenResId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    
                    prepare()
                    isLooping = true
                    setWakeMode(this@PanicAlertService, PowerManager.PARTIAL_WAKE_LOCK)
                    start()
                    Log.d(TAG, "Siren started - looping: $isLooping")
                }
            } else {
                Log.e(TAG, "Siren audio resource not found")
                CrashlyticsHelper.log("Siren audio resource not found")
            }
            
            isActive.value = true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting siren", e)
            CrashlyticsHelper.recordException(e, "Error starting panic siren")
            restoreVolume()
        }
    }
    
    private fun stopSiren() {
        try {
            CrashlyticsHelper.logPanicAlertState(false)
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
                Log.d(TAG, "Siren stopped and released")
            }
            mediaPlayer = null
            
            restoreVolume()
            releaseWakeLock()
            
            isActive.value = false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping siren", e)
            CrashlyticsHelper.recordException(e, "Error stopping panic siren")
            isActive.value = false
        }
    }
    
    private fun restoreVolume() {
        if (previousVolume >= 0) {
            audioManager?.setStreamVolume(
                PanicAlertConstants.AUDIO_STREAM_TYPE,
                previousVolume,
                0
            )
            Log.d(TAG, "Volume restored to: $previousVolume")
            previousVolume = -1
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
                Log.d(TAG, "Wake lock released")
            }
        }
    }
}
