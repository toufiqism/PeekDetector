package com.tofiq.peekdetector

// ... (all the imports from the previous example)
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class PeekDetectionService : Service() {
    // --- NEW: Companion object to hold the service state ---
    companion object {
        // This state is observable by Jetpack Compose
        val isRunning = mutableStateOf(false)
    }
    private lateinit var cameraExecutor: ExecutorService
    private val serviceLifecycleOwner = ServiceLifecycleOwner()
    private var cameraProvider: ProcessCameraProvider? = null

    // For the screen overlay alert
    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager
    
    // Notification helper
    private lateinit var notificationHelper: NotificationHelper
    
    // Track last notification time to avoid spam
    private var lastNotificationTime: Long = 0
    private val notificationCooldown = 5000L // 5 seconds cooldown

    override fun onCreate() {
        super.onCreate()
        isRunning.value = true // Set state to running
        cameraExecutor = Executors.newSingleThreadExecutor()
        serviceLifecycleOwner.start()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        notificationHelper = NotificationHelper(this)
    }

    private val imageAnalyzer by lazy {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, PeekDetectorAnalyzer { numFaces ->
                    if (numFaces > 1) {
                        Log.d("PeekDetectionService", "PEEKING DETECTED! $numFaces faces")
//                        triggerPeekAlertOverlay()
                        showMultipleFacesNotification(numFaces)
                    }
                })
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        startCamera()
        return START_STICKY
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    serviceLifecycleOwner,
                    cameraSelector,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("PeekDetectionService", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ... (startForegroundService() method from previous example)

    // ... (triggerPeekAlertOverlay() and hideOverlay() methods from previous example)

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false // Set state to not running
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        serviceLifecycleOwner.stop()
        hideOverlay() // Ensure overlay is removed when service stops
    }

    override fun onBind(intent: Intent?): IBinder? = null
    private fun hideOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
    }
    // This is the new alert method
    private fun triggerPeekAlertOverlay() {
        // Ensure we run on the main thread
        Handler(Looper.getMainLooper()).post {
            if (overlayView == null) {
                overlayView = FrameLayout(this)
                overlayView?.setBackgroundColor(Color.argb(150, 0, 0, 0)) // Semi-transparent black

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )
                params.gravity = Gravity.CENTER
                windowManager.addView(overlayView, params)

                // Hide the overlay after a few seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    hideOverlay()
                }, 3000) // Hide after 3 seconds
            }
        }
    }
    private fun startForegroundService() {
        val notification = notificationHelper.createForegroundNotification()
        startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
    }
    
    /**
     * Shows a notification when multiple faces are detected
     * Implements cooldown to prevent notification spam
     */
    private fun showMultipleFacesNotification(numFaces: Int) {
        val currentTime = System.currentTimeMillis()
        
        // Check if enough time has passed since last notification (cooldown period)
        if (currentTime - lastNotificationTime >= notificationCooldown) {
            // Check if notification permission is granted
            if (notificationHelper.hasNotificationPermission()) {
                notificationHelper.showMultipleFacesAlert(numFaces)
                lastNotificationTime = currentTime
                Log.d("PeekDetectionService", "Alert notification shown for $numFaces faces")
            } else {
                Log.w("PeekDetectionService", "Notification permission not granted, skipping notification")
            }
        }
    }
}