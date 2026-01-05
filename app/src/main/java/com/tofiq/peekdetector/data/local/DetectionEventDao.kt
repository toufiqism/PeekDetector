package com.tofiq.peekdetector.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tofiq.peekdetector.data.model.DailyDetectionCount
import com.tofiq.peekdetector.data.model.DetectionEvent
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for detection events.
 * Provides methods for CRUD operations on detection data.
 */
@Dao
interface DetectionEventDao {
    
    @Insert
    suspend fun insertDetection(event: DetectionEvent)
    
    @Query("SELECT * FROM detection_events ORDER BY timestamp DESC")
    fun getAllDetections(): Flow<List<DetectionEvent>>
    
    @Query("SELECT COUNT(*) FROM detection_events")
    fun getTotalDetectionsCount(): Flow<Int>
    
    @Query("SELECT * FROM detection_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getDetectionsInRange(startTime: Long, endTime: Long): Flow<List<DetectionEvent>>
    
    @Query("SELECT COUNT(*) FROM detection_events WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getDetectionsCountInRange(startTime: Long, endTime: Long): Int
    
    @Query("""
        SELECT 
            strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') as date,
            COUNT(*) as count
        FROM detection_events 
        WHERE timestamp BETWEEN :startTime AND :endTime
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDetectionsGroupedByDay(startTime: Long, endTime: Long): List<DailyDetectionCount>
    
    @Query("DELETE FROM detection_events")
    suspend fun deleteAllDetections()
    
    @Query("DELETE FROM detection_events WHERE timestamp < :timestamp")
    suspend fun deleteOldDetections(timestamp: Long)
}
