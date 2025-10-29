package com.tofiq.peekdetector

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.tofiq.peekdetector.data.AppDatabase
import com.tofiq.peekdetector.data.DetectionEvent
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Helper class for exporting detection reports
 * Handles data extraction, file creation, zipping, and saving to Downloads folder
 * 
 * Follows SOLID principles:
 * - Single Responsibility: Only handles report export operations
 * - Error Handling: Gracefully handles null values and exceptions
 */
class ReportExportHelper(private val context: Context) {

    companion object {
        private const val TAG = "ReportExportHelper"
        private const val BUFFER_SIZE = 8192
    }

    /**
     * Exports all detection reports to a zip file in the Downloads folder
     * 
     * @return Result<String> Success with file path or Failure with exception
     */
    suspend fun exportReportsToDownloads(): Result<String> {
        return try {
            // Step 1: Fetch all detection events from database
            val detections = fetchAllDetections()
            
            if (detections.isEmpty()) {
                Log.w(TAG, "No detection events to export")
                return Result.failure(Exception("No detection events found"))
            }

            // Step 2: Create report files
            val reportFiles = createReportFiles(detections)
            
            // Step 3: Zip the report files
            val zipFile = zipReportFiles(reportFiles)
            
            // Step 4: Move to Downloads folder
            val downloadPath = saveToDownloads(zipFile)
            
            // Step 5: Clean up temporary files
            cleanupTempFiles(reportFiles, zipFile)
            
            Log.i(TAG, "Successfully exported reports to: $downloadPath")
            Result.success(downloadPath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting reports", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all detection events from the database
     */
    private suspend fun fetchAllDetections(): List<DetectionEvent> {
        val database = AppDatabase.getDatabase(context)
        val dao = database.detectionEventDao()
        
        // Get all detections (not using Flow, just get the list)
        return try {
            // We need to query synchronously
            database.query("SELECT * FROM detection_events ORDER BY timestamp DESC", null).use { cursor ->
                val detections = mutableListOf<DetectionEvent>()
                
                val idIndex = cursor.getColumnIndexOrThrow("id")
                val faceCountIndex = cursor.getColumnIndexOrThrow("faceCount")
                val timestampIndex = cursor.getColumnIndexOrThrow("timestamp")
                
                while (cursor.moveToNext()) {
                    val detection = DetectionEvent(
                        id = cursor.getLong(idIndex),
                        faceCount = cursor.getInt(faceCountIndex),
                        timestamp = cursor.getLong(timestampIndex)
                    )
                    detections.add(detection)
                }
                
                detections
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching detections from database", e)
            emptyList()
        }
    }

    /**
     * Creates report files (JSON and CSV) from detection events
     */
    private fun createReportFiles(detections: List<DetectionEvent>): List<File> {
        val files = mutableListOf<File>()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        
        // Create JSON report
        val jsonFile = createJsonReport(detections, timestamp)
        if (jsonFile != null) files.add(jsonFile)
        
        // Create CSV report
        val csvFile = createCsvReport(detections, timestamp)
        if (csvFile != null) files.add(csvFile)
        
        // Create summary report
        val summaryFile = createSummaryReport(detections, timestamp)
        if (summaryFile != null) files.add(summaryFile)
        
        return files
    }

    /**
     * Creates a JSON format report
     */
    private fun createJsonReport(detections: List<DetectionEvent>, timestamp: String): File? {
        return try {
            val jsonArray = JSONArray()
            
            for (detection in detections) {
                val jsonObject = JSONObject().apply {
                    put("id", detection.id)
                    put("faceCount", detection.faceCount)
                    put("timestamp", detection.timestamp)
                    put("dateTime", formatTimestamp(detection.timestamp))
                }
                jsonArray.put(jsonObject)
            }
            
            val reportObject = JSONObject().apply {
                put("exportDate", timestamp)
                put("totalDetections", detections.size)
                put("totalFaces", detections.sumOf { it.faceCount })
                put("detections", jsonArray)
            }
            
            val file = File(context.cacheDir, "detection_report_$timestamp.json")
            file.writeText(reportObject.toString(4)) // Pretty print with 4 space indent
            
            Log.d(TAG, "Created JSON report: ${file.name}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error creating JSON report", e)
            null
        }
    }

    /**
     * Creates a CSV format report
     */
    private fun createCsvReport(detections: List<DetectionEvent>, timestamp: String): File? {
        return try {
            val file = File(context.cacheDir, "detection_report_$timestamp.csv")
            
            file.bufferedWriter().use { writer ->
                // Write CSV header
                writer.write("ID,Face Count,Timestamp (ms),Date Time\n")
                
                // Write data rows
                for (detection in detections) {
                    writer.write("${detection.id},${detection.faceCount},${detection.timestamp},\"${formatTimestamp(detection.timestamp)}\"\n")
                }
            }
            
            Log.d(TAG, "Created CSV report: ${file.name}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error creating CSV report", e)
            null
        }
    }

    /**
     * Creates a summary text report with statistics
     */
    private fun createSummaryReport(detections: List<DetectionEvent>, timestamp: String): File? {
        return try {
            val file = File(context.cacheDir, "detection_summary_$timestamp.txt")
            
            val totalDetections = detections.size
            val totalFaces = detections.sumOf { it.faceCount }
            val avgFaces = if (totalDetections > 0) totalFaces.toDouble() / totalDetections else 0.0
            val maxFaces = detections.maxOfOrNull { it.faceCount } ?: 0
            val minFaces = detections.minOfOrNull { it.faceCount } ?: 0
            
            val firstDetection = detections.lastOrNull()
            val lastDetection = detections.firstOrNull()
            
            file.bufferedWriter().use { writer ->
                writer.write("PeekDetector - Detection Report Summary\n")
                writer.write("=" .repeat(50) + "\n\n")
                writer.write("Export Date: ${formatTimestamp(System.currentTimeMillis())}\n")
                writer.write("Report Period: ${if (firstDetection != null) formatTimestamp(firstDetection.timestamp) else "N/A"} to ${if (lastDetection != null) formatTimestamp(lastDetection.timestamp) else "N/A"}\n\n")
                
                writer.write("Statistics:\n")
                writer.write("-" .repeat(50) + "\n")
                writer.write("Total Detections: $totalDetections\n")
                writer.write("Total Faces Detected: $totalFaces\n")
                writer.write("Average Faces per Detection: ${"%.2f".format(avgFaces)}\n")
                writer.write("Maximum Faces in Single Detection: $maxFaces\n")
                writer.write("Minimum Faces in Single Detection: $minFaces\n\n")
                
                writer.write("Detection Severity Breakdown:\n")
                writer.write("-" .repeat(50) + "\n")
                val lowSeverity = detections.count { it.faceCount == 2 }
                val highSeverity = detections.count { it.faceCount >= 3 }
                writer.write("⚠️  Low Severity (2 faces): $lowSeverity detections\n")
                writer.write("🚨 High Severity (3+ faces): $highSeverity detections\n\n")
                
                writer.write("Recent Detections (Last 10):\n")
                writer.write("-" .repeat(50) + "\n")
                detections.take(10).forEach { detection ->
                    writer.write("ID ${detection.id}: ${detection.faceCount} faces at ${formatTimestamp(detection.timestamp)}\n")
                }
            }
            
            Log.d(TAG, "Created summary report: ${file.name}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error creating summary report", e)
            null
        }
    }

    /**
     * Zips multiple files into a single archive
     */
    private fun zipReportFiles(files: List<File>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val zipFile = File(context.cacheDir, "PeekDetector_Reports_$timestamp.zip")
        
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
            for (file in files) {
                FileInputStream(file).use { fis ->
                    val zipEntry = ZipEntry(file.name)
                    zipOut.putNextEntry(zipEntry)
                    
                    val buffer = ByteArray(BUFFER_SIZE)
                    var length: Int
                    while (fis.read(buffer).also { length = it } > 0) {
                        zipOut.write(buffer, 0, length)
                    }
                    
                    zipOut.closeEntry()
                }
            }
        }
        
        Log.d(TAG, "Created zip file: ${zipFile.name}")
        return zipFile
    }

    /**
     * Saves the zip file to the Downloads folder using MediaStore API
     * Works for Android 10+ (API 29+)
     */
    private fun saveToDownloads(zipFile: File): String {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, zipFile.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Failed to create MediaStore entry")
        
        resolver.openOutputStream(uri)?.use { outputStream ->
            FileInputStream(zipFile).use { inputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
            }
        } ?: throw IOException("Failed to open output stream")
        
        return "${Environment.DIRECTORY_DOWNLOADS}/${zipFile.name}"
    }

    /**
     * Cleans up temporary files after successful export
     */
    private fun cleanupTempFiles(reportFiles: List<File>, zipFile: File) {
        reportFiles.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "Deleted temp file: ${file.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete temp file: ${file.name}", e)
            }
        }
        
        try {
            if (zipFile.exists()) {
                zipFile.delete()
                Log.d(TAG, "Deleted temp zip file: ${zipFile.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete temp zip file: ${zipFile.name}", e)
        }
    }

    /**
     * Formats timestamp to readable date string
     */
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return sdf.format(Date(timestamp))
    }
}

