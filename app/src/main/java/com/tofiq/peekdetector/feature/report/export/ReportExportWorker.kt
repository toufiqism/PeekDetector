package com.tofiq.peekdetector.feature.report.export

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tofiq.peekdetector.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager Worker for periodic report export.
 * Runs daily to export detection reports to Downloads folder.
 */
class ReportExportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ReportExportWorker"
        const val WORK_NAME = "report_export_work"
        
        private const val CHANNEL_ID = "report_export_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting report export work")
        
        try {
            createNotificationChannel()
            showProgressNotification()
            
            val exportHelper = ReportExportHelper(applicationContext)
            val result = exportHelper.exportReportsToDownloads()
            
            return@withContext if (result.isSuccess) {
                val filePath = result.getOrNull() ?: "Downloads folder"
                Log.i(TAG, "Report export succeeded: $filePath")
                showSuccessNotification(filePath)
                Result.success()
            } else {
                val exception = result.exceptionOrNull()
                Log.e(TAG, "Report export failed", exception)
                
                if (exception?.message != "No detection events found") {
                    showFailureNotification(exception?.message ?: "Unknown error")
                } else {
                    Log.i(TAG, "No detection events to export, skipping notification")
                }
                
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during report export", e)
            showFailureNotification(e.message ?: "Unexpected error")
            return@withContext Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.report_export_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.report_export_channel_description)
                setShowBadge(true)
            }
            
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun showProgressNotification() {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(applicationContext.getString(R.string.exporting_reports))
            .setContentText(applicationContext.getString(R.string.preparing_detection_reports))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun showSuccessNotification(filePath: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(applicationContext.getString(R.string.reports_exported))
            .setContentText(applicationContext.getString(R.string.detection_reports_saved_to, filePath))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun showFailureNotification(errorMessage: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(applicationContext.getString(R.string.report_export_failed))
            .setContentText(errorMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}
