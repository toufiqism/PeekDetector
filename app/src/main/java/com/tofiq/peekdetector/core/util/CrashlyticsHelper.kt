package com.tofiq.peekdetector.core.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tofiq.peekdetector.BuildConfig

/**
 * Helper object for Firebase Crashlytics operations.
 * Provides convenient methods for logging, recording exceptions, and setting context.
 */
object CrashlyticsHelper {

    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }
    
    /**
     * Log a message to Crashlytics for context in crash reports.
     */
    fun log(message: String) {
        if (!BuildConfig.DEBUG) {
            crashlytics.log(message)
        }
    }

    /**
     * Record a non-fatal exception to Crashlytics.
     * Use this for caught exceptions that you want to track.
     */
    fun recordException(throwable: Throwable, context: String? = null) {
        if (!BuildConfig.DEBUG) {
            context?.let { crashlytics.log("Context: $it") }
            crashlytics.recordException(throwable)
        }
    }

    /**
     * Set a custom key-value pair for crash context.
     */
    fun setCustomKey(key: String, value: String) {
        if (!BuildConfig.DEBUG) {
            crashlytics.setCustomKey(key, value)
        }
    }

    /**
     * Set a custom key-value pair for crash context.
     */
    fun setCustomKey(key: String, value: Int) {
        if (!BuildConfig.DEBUG) {
            crashlytics.setCustomKey(key, value)
        }
    }

    /**
     * Set a custom key-value pair for crash context.
     */
    fun setCustomKey(key: String, value: Boolean) {
        if (!BuildConfig.DEBUG) {
            crashlytics.setCustomKey(key, value)
        }
    }

    /**
     * Set user identifier for crash reports.
     * Note: Do not use PII - use anonymous identifiers only.
     */
    fun setUserId(userId: String) {
        if (!BuildConfig.DEBUG) {
            crashlytics.setUserId(userId)
        }
    }

    /**
     * Log detection service state for crash context.
     */
    fun logDetectionServiceState(isRunning: Boolean) {
        setCustomKey("detection_service_running", isRunning)
        log("Detection service state: ${if (isRunning) "running" else "stopped"}")
    }

    /**
     * Log panic alert state for crash context.
     */
    fun logPanicAlertState(isActive: Boolean) {
        setCustomKey("panic_alert_active", isActive)
        log("Panic alert state: ${if (isActive) "active" else "inactive"}")
    }

    /**
     * Log face detection event for crash context.
     */
    fun logFaceDetectionEvent(faceCount: Int) {
        setCustomKey("last_face_count", faceCount)
        log("Face detection: $faceCount faces detected")
    }
}
