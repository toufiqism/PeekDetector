package com.tofiq.peekdetector.feature.detection.service

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.tofiq.peekdetector.core.notification.NotificationHelper
import com.tofiq.peekdetector.core.receiver.ScreenStateReceiver
import com.tofiq.peekdetector.core.util.CrashlyticsHelper
import com.tofiq.peekdetector.core.util.ServiceLifecycleOwner
import com.tofiq.peekdetector.data.local.AppDatabase
import com.tofiq.peekdetector.data.local.settingsDataStore
import com.tofiq.peekdetector.data.repository.DetectionRepository
import com.tofiq.peekdetector.data.repository.SettingsRepositoryImpl
import com.tofiq.peekdetector.feature.detection.analyzer.PeekDetectionAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Foreground service for continuous peek detection using the front camera.
 * Supports smart detection mode that pauses detection when screen is off.
 * 
 * Requirements: 6.2, 6.3, 6.4
 */
class PeekDetectionService : Service() {
    
    companion object {
        private const val TAG = "PeekDetectionService"
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
    
    // Configurable notification cooldown from settings (in milliseconds)
    private var notificationCooldownMs = 5000L

    // Repository for database operations
    private lateinit var detectionRepository: DetectionRepository
    
    // Settings repository for reading user preferences
    private lateinit var settingsRepository: SettingsRepositoryImpl

    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Smart detection components
    private var screenStateReceiver: ScreenStateReceiver? = null
    private var smartDetectionJob: Job? = null
    private var isDetectionPaused = false
    
    // Current sensitivity level frame skip value (updated from settings)
    @Volatile
    private var currentFrameSkip = 3 // Default: MEDIUM sensitivity
    
    // Job for observing settings changes
    private var settingsObserverJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        isRunning.value = true
        CrashlyticsHelper.logDetectionServiceState(true)
        cameraExecutor = Executors.newSingleThreadExecutor()
        serviceLifecycleOwner.start()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        notificationHelper = NotificationHelper(this)

        // Initialize database repository
        val database = AppDatabase.getDatabase(this)
        detectionRepository = DetectionRepository(database.detectionEventDao())
        
        // Initialize settings repository
        settingsRepository = SettingsRepositoryImpl(applicationContext.settingsDataStore)
        
        // Start observing settings
        observeSettings()
        
        // Start observing smart detection setting
        observeSmartDetectionSetting()
    }
    
    /**
     * Observes settings changes and updates service behavior accordingly.
     */
    private fun observeSettings() {
        settingsObserverJob = serviceScope.launch {
            // Observe sensitivity level changes
            launch {
                settingsRepository.sensitivityLevel.collectLatest { level ->
                    currentFrameSkip = level.frameSkip
                    Log.d(TAG, "Sensitivity level updated: $level (frameSkip: $currentFrameSkip)")
                }
            }
            
            // Observe notification cooldown changes
            launch {
                settingsRepository.notificationCooldown.collectLatest { seconds ->
                    notificationCooldownMs = seconds * 1000L
                    Log.d(TAG, "Notification cooldown updated: ${seconds}s")
                }
            }
        }
    }

    /**
     * Observes the smart detection setting and manages the ScreenStateReceiver accordingly.
     */
    private fun observeSmartDetectionSetting() {
        smartDetectionJob = serviceScope.launch(Dispatchers.Main) {
            settingsRepository.smartDetectionEnabled.collectLatest { enabled ->
                if (enabled) {
                    registerScreenStateReceiver()
                    observeScreenState()
                } else {
                    unregisterScreenStateReceiver()
                    resumeDetection()
                }
            }
        }
    }
    
    private fun registerScreenStateReceiver() {
        if (screenStateReceiver == null) {
            screenStateReceiver = ScreenStateReceiver()
            val filter = ScreenStateReceiver.createIntentFilter()
            registerReceiver(screenStateReceiver, filter)
            Log.d(TAG, "ScreenStateReceiver registered")
        }
    }
    
    private fun unregisterScreenStateReceiver() {
        screenStateReceiver?.let {
            try {
                unregisterReceiver(it)
                Log.d(TAG, "ScreenStateReceiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "ScreenStateReceiver was not registered", e)
            }
            screenStateReceiver = null
            ScreenStateReceiver.resetState()
        }
    }
    
    private fun observeScreenState() {
        serviceScope.launch(Dispatchers.Main) {
            ScreenStateReceiver.isScreenOn.collectLatest { isScreenOn ->
                if (isScreenOn) {
                    resumeDetection()
                } else {
                    pauseDetection()
                }
            }
        }
    }
    
    private fun pauseDetection() {
        if (!isDetectionPaused) {
            Log.d(TAG, "Pausing detection (screen off)")
            cameraProvider?.unbindAll()
            isDetectionPaused = true
        }
    }
    
    private fun resumeDetection() {
        if (isDetectionPaused) {
            Log.d(TAG, "Resuming detection (screen on)")
            startCamera()
            isDetectionPaused = false
        }
    }

    private val imageAnalyzer by lazy {
        ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, PeekDetectionAnalyzer(
                    listener = { numFaces ->
                        if (numFaces > 1) {
                            Log.d(TAG, "PEEKING DETECTED! $numFaces faces")
                            showMultipleFacesNotification(numFaces)
                            triggerPeekAlertOverlay(numFaces)
                            saveDetectionToDatabase(numFaces)
                        }
                    },
                    getFrameSkip = { currentFrameSkip }
                ))
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
                Log.e(TAG, "Use case binding failed", exc)
                CrashlyticsHelper.recordException(exc, "Camera binding failed in PeekDetectionService")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false
        CrashlyticsHelper.logDetectionServiceState(false)
        
        settingsObserverJob?.cancel()
        smartDetectionJob?.cancel()
        unregisterScreenStateReceiver()
        
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        serviceLifecycleOwner.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun hideOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
    }

    private fun triggerPeekAlertOverlay(faceCount: Int) {
        // Check for overlay permission before attempting to show overlay
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted, skipping overlay alert")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            if (overlayView == null) {
                overlayView = FrameLayout(this).apply {
                    setBackgroundColor(Color.argb(150, 0, 0, 0))
                    
                    val textView = android.widget.TextView(this@PeekDetectionService).apply {
                        text = "⚠️ $faceCount faces detected!\nSomeone might be peeking!"
                        setTextColor(Color.WHITE)
                        textSize = 24f
                        gravity = Gravity.CENTER
                        setPadding(32, 32, 32, 32)
                    }
                    addView(textView, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    ))
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )
                params.gravity = Gravity.CENTER
                windowManager.addView(overlayView, params)

                Handler(Looper.getMainLooper()).postDelayed({
                    hideOverlay()
                }, 3000)
            }
        }
    }

    private fun startForegroundService() {
        val notification = notificationHelper.createForegroundNotification()
        startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun showMultipleFacesNotification(numFaces: Int) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastNotificationTime >= notificationCooldownMs) {
            if (notificationHelper.hasNotificationPermission()) {
                notificationHelper.showMultipleFacesAlert(numFaces)
                lastNotificationTime = currentTime
                Log.d(TAG, "Alert notification shown for $numFaces faces")
            } else {
                Log.w(TAG, "Notification permission not granted, skipping notification")
            }
        }
    }

    private fun saveDetectionToDatabase(numFaces: Int) {
        CrashlyticsHelper.logFaceDetectionEvent(numFaces)
        serviceScope.launch {
            try {
                detectionRepository.insertDetection(numFaces)
                Log.d(TAG, "Detection saved to database: $numFaces faces")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save detection to database", e)
                CrashlyticsHelper.recordException(e, "Failed to save detection to database")
            }
        }
    }
}
