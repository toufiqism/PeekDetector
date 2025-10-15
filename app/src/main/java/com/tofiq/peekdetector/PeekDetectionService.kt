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

    override fun onCreate() {
        super.onCreate()
        isRunning.value = true // Set state to running
        cameraExecutor = Executors.newSingleThreadExecutor()
        serviceLifecycleOwner.start()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private val imageAnalyzer by lazy {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, PeekDetectorAnalyzer { numFaces ->
                    if (numFaces > 1) {
                        Log.d("PeekDetectionService", "PEEKING DETECTED!")
                        triggerPeekAlertOverlay()
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
        val channelId = "peek_detection_channel"
        val channelName = "Peek Detection Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your own icon
            .setContentTitle("Peek Protection is Active")
            .setContentText("Monitoring for shoulder surfers.")
            .setPriority(NotificationManager.IMPORTANCE_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        startForeground(1, notification) // NOTIFICATION_ID = 1
    }
    // Copy the implementation of startForegroundService, triggerPeekAlertOverlay, and hideOverlay
    // methods from the previous answer here. They are identical.
}