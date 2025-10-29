package com.tofiq.peekdetector

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager Worker for periodic report export
 * Runs daily to export detection reports to Downloads folder
 * 
 * Follows SOLID principles:
 * - Single Responsibility: Only handles periodic report export
 * - Dependency Injection: Uses ReportExportHelper for actual export logic
 * - Error Handling: Gracefully handles failures and reports status
 */
class ReportExportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ReportExportWorker"
        const val WORK_NAME = "report_export_work"
        
        // Notification channels
        private const val CHANNEL_ID = "report_export_channel"
        private const val CHANNEL_NAME = "Report Export"
        private const val NOTIFICATION_ID = 1001
    }

    /**
     * Main work execution method
     * Called by WorkManager on a background thread
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting report export work")
        
        try {
            // Create notification channel if needed
            createNotificationChannel()
            
            // Show progress notification
            showProgressNotification()
            
            // Perform the export
            val exportHelper = ReportExportHelper(applicationContext)
            val result = exportHelper.exportReportsToDownloads()
            
            return@withContext if (result.isSuccess) {
                val filePath = result.getOrNull() ?: "Downloads folder"
                Log.i(TAG, "Report export succeeded: $filePath")
                
                // Show success notification
                showSuccessNotification(filePath)
                
                Result.success()
            } else {
                val exception = result.exceptionOrNull()
                Log.e(TAG, "Report export failed", exception)
                
                // Show failure notification only if there was actual data to export
                if (exception?.message != "No detection events found") {
                    showFailureNotification(exception?.message ?: "Unknown error")
                } else {
                    Log.i(TAG, "No detection events to export, skipping notification")
                }
                
                // Return success even if no data to prevent retry spam
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during report export", e)
            showFailureNotification(e.message ?: "Unexpected error")
            
            // Return retry for unexpected errors (max 3 retries by default)
            return@withContext Result.retry()
        }
    }

    /**
     * Creates notification channel for Android O+
     * Required for showing notifications
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for detection report export status"
                setShowBadge(true)
            }
            
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a notification while export is in progress
     */
    private fun showProgressNotification() {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Exporting Reports")
            .setContentText("Preparing detection reports...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Shows a success notification when export completes
     */
    private fun showSuccessNotification(filePath: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Reports Exported")
            .setContentText("Detection reports saved to $filePath")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Shows a failure notification when export fails
     */
    private fun showFailureNotification(errorMessage: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Report Export Failed")
            .setContentText(errorMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}

