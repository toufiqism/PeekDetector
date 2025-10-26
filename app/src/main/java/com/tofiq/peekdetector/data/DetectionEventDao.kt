package com.tofiq.peekdetector.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for detection events
 * Provides methods to query detection statistics
 */
@Dao
interface DetectionEventDao {
    
    /**
     * Insert a new detection event
     */
    @Insert
    suspend fun insertDetection(event: DetectionEvent)
    
    /**
     * Get total count of all detections
     */
    @Query("SELECT COUNT(*) FROM detection_events")
    fun getTotalDetectionsCount(): Flow<Int>
    
    /**
     * Get all detection events ordered by timestamp descending
     */
    @Query("SELECT * FROM detection_events ORDER BY timestamp DESC")
    fun getAllDetections(): Flow<List<DetectionEvent>>
    
    /**
     * Get detections within a specific time range
     * @param startTime Start timestamp in milliseconds
     * @param endTime End timestamp in milliseconds
     */
    @Query("SELECT * FROM detection_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getDetectionsInRange(startTime: Long, endTime: Long): Flow<List<DetectionEvent>>
    
    /**
     * Get count of detections within a time range
     */
    @Query("SELECT COUNT(*) FROM detection_events WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getDetectionsCountInRange(startTime: Long, endTime: Long): Int
    
    /**
     * Get detections grouped by day for chart visualization
     * Returns a list of daily counts
     */
    @Query("""
        SELECT COUNT(*) as count, 
        (timestamp / 86400000) * 86400000 as day 
        FROM detection_events 
        WHERE timestamp BETWEEN :startTime AND :endTime 
        GROUP BY day 
        ORDER BY day ASC
    """)
    suspend fun getDetectionsGroupedByDay(startTime: Long, endTime: Long): List<DailyDetectionCount>
    
    /**
     * Delete all detection events
     */
    @Query("DELETE FROM detection_events")
    suspend fun deleteAllDetections()
    
    /**
     * Delete detections older than a specific timestamp
     */
    @Query("DELETE FROM detection_events WHERE timestamp < :timestamp")
    suspend fun deleteOldDetections(timestamp: Long)
}

/**
 * Data class for daily detection count results
 */
data class DailyDetectionCount(
    val count: Int,
    val day: Long
)

