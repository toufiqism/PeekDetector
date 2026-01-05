package com.tofiq.peekdetector

import android.app.Application
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Application class for PeekDetector with optimized Firebase Crashlytics setup.
 */
class PeekDetectorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeCrashlytics()
    }

    private fun initializeCrashlytics() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        
        // Disable Crashlytics in debug builds to avoid polluting crash reports
        val isDebug = BuildConfig.DEBUG
        crashlytics.isCrashlyticsCollectionEnabled = !isDebug
        
        if (!isDebug) {
            // Set custom keys for better crash context
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            crashlytics.setCustomKey("device_model", android.os.Build.MODEL)
            crashlytics.setCustomKey("android_version", android.os.Build.VERSION.SDK_INT)
            
            // Set up uncaught exception handler for additional logging
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                crashlytics.log("Uncaught exception on thread: ${thread.name}")
                crashlytics.recordException(throwable)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
        
        Log.d(TAG, "Crashlytics initialized. Collection enabled: ${!isDebug}")
    }

    companion object {
        private const val TAG = "PeekDetectorApp"
    }
}
