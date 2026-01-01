package com.tofiq.peekdetector

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
import com.tofiq.peekdetector.data.AppDatabase
import com.tofiq.peekdetector.data.DetectionRepository
import com.tofiq.peekdetector.data.SettingsRepositoryImpl
import com.tofiq.peekdetector.data.settingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
    // Default: 5 seconds, updated from settings repository
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
     * 
     * Requirements: 2.2, 2.3, 2.4, 3.3
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
     * When smart detection is enabled, registers the receiver and observes screen state.
     * When disabled, unregisters the receiver and ensures detection is always active.
     * 
     * Requirements: 6.2, 6.3, 6.4
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
    
    /**
     * Registers the ScreenStateReceiver to listen for screen on/off events.
     * 
     * Requirements: 6.2
     */
    private fun registerScreenStateReceiver() {
        if (screenStateReceiver == null) {
            screenStateReceiver = ScreenStateReceiver()
            val filter = ScreenStateReceiver.createIntentFilter()
            registerReceiver(screenStateReceiver, filter)
            Log.d(TAG, "ScreenStateReceiver registered")
        }
    }
    
    /**
     * Unregisters the ScreenStateReceiver when smart detection is disabled.
     * 
     * Requirements: 6.4
     */
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
    
    /**
     * Observes screen state changes and pauses/resumes detection accordingly.
     * 
     * Requirements: 6.2, 6.3
     */
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
    
    /**
     * Pauses face detection by unbinding the camera use case.
     * Called when screen turns off and smart detection is enabled.
     * 
     * Requirements: 6.2
     */
    private fun pauseDetection() {
        if (!isDetectionPaused) {
            Log.d(TAG, "Pausing detection (screen off)")
            cameraProvider?.unbindAll()
            isDetectionPaused = true
        }
    }
    
    /**
     * Resumes face detection by rebinding the camera use case.
     * Called when screen turns on and smart detection is enabled.
     * 
     * Requirements: 6.3
     */
    private fun resumeDetection() {
        if (isDetectionPaused) {
            Log.d(TAG, "Resuming detection (screen on)")
            startCamera()
            isDetectionPaused = false
        }
    }

    /**
     * Optimized ImageAnalysis configuration for battery efficiency:
     * - Reduced resolution (640x480) reduces CPU/GPU processing by ~75%
     * - Lower resolution still sufficient for face detection accuracy
     * - YUV_420_888 format optimized for ML Kit processing
     * - KEEP_ONLY_LATEST strategy drops old frames to prevent backlog
     * - Frame skip is configurable via settings (Requirements: 2.2, 2.3, 2.4)
     */
    private val imageAnalyzer by lazy {
        ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, PeekDetectorAnalyzer(
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
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false
        
        // Clean up settings observer
        settingsObserverJob?.cancel()
        
        // Clean up smart detection
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
        Handler(Looper.getMainLooper()).post {
            if (overlayView == null) {
                overlayView = FrameLayout(this).apply {
                    setBackgroundColor(Color.argb(150, 0, 0, 0))
                    
                    // Add text view to show face count
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

    /**
     * Shows a notification when multiple faces are detected
     * Implements cooldown to prevent notification spam
     * 
     * Requirements: 3.3
     */
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

    /**
     * Save detection event to database
     */
    private fun saveDetectionToDatabase(numFaces: Int) {
        serviceScope.launch {
            try {
                detectionRepository.insertDetection(numFaces)
                Log.d(TAG, "Detection saved to database: $numFaces faces")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save detection to database", e)
            }
        }
    }
}
