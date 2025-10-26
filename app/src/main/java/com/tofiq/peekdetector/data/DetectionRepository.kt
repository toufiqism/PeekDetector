package com.tofiq.peekdetector.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repository for managing detection events
 * Single source of truth for detection data
 */
class DetectionRepository(private val dao: DetectionEventDao) {
    
    /**
     * Get total count of all detections as a Flow
     */
    fun getTotalDetectionsCount(): Flow<Int> = dao.getTotalDetectionsCount()
    
    /**
     * Get all detection events
     */
    fun getAllDetections(): Flow<List<DetectionEvent>> = dao.getAllDetections()
    
    /**
     * Insert a new detection event
     */
    suspend fun insertDetection(faceCount: Int) {
        dao.insertDetection(DetectionEvent(faceCount = faceCount))
    }
    
    /**
     * Get detections for the current week
     */
    fun getWeeklyDetections(): Flow<List<DetectionEvent>> {
        val (startTime, endTime) = getWeekRange()
        return dao.getDetectionsInRange(startTime, endTime)
    }
    
    /**
     * Get detections for the current month
     */
    fun getMonthlyDetections(): Flow<List<DetectionEvent>> {
        val (startTime, endTime) = getMonthRange()
        return dao.getDetectionsInRange(startTime, endTime)
    }
    
    /**
     * Get detections for the current year
     */
    fun getYearlyDetections(): Flow<List<DetectionEvent>> {
        val (startTime, endTime) = getYearRange()
        return dao.getDetectionsInRange(startTime, endTime)
    }
    
    /**
     * Get count of detections in the current week
     */
    suspend fun getWeeklyCount(): Int {
        val (startTime, endTime) = getWeekRange()
        return dao.getDetectionsCountInRange(startTime, endTime)
    }
    
    /**
     * Get count of detections in the current month
     */
    suspend fun getMonthlyCount(): Int {
        val (startTime, endTime) = getMonthRange()
        return dao.getDetectionsCountInRange(startTime, endTime)
    }
    
    /**
     * Get count of detections in the current year
     */
    suspend fun getYearlyCount(): Int {
        val (startTime, endTime) = getYearRange()
        return dao.getDetectionsCountInRange(startTime, endTime)
    }
    
    /**
     * Get daily detection counts for charting
     */
    suspend fun getDailyDetectionCounts(startTime: Long, endTime: Long): List<DailyDetectionCount> {
        return dao.getDetectionsGroupedByDay(startTime, endTime)
    }
    
    /**
     * Delete all detections
     */
    suspend fun deleteAllDetections() {
        dao.deleteAllDetections()
    }
    
    /**
     * Delete detections older than specified days
     */
    suspend fun deleteOldDetections(daysOld: Int) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysOld)
        dao.deleteOldDetections(calendar.timeInMillis)
    }
    
    /**
     * Helper function to get the start and end of current week
     */
    private fun getWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val endTime = calendar.timeInMillis
        
        return Pair(startTime, endTime)
    }
    
    /**
     * Helper function to get the start and end of current month
     */
    private fun getMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        val endTime = calendar.timeInMillis
        
        return Pair(startTime, endTime)
    }
    
    /**
     * Helper function to get the start and end of current year
     */
    private fun getYearRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.YEAR, 1)
        val endTime = calendar.timeInMillis
        
        return Pair(startTime, endTime)
    }
}

