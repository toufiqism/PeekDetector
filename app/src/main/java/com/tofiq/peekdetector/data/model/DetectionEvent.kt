package com.tofiq.peekdetector.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a face detection event.
 * Stored in Room database for historical tracking.
 */
@Entity(tableName = "detection_events")
data class DetectionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val faceCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class for daily detection count aggregation.
 * Used for charting and statistics.
 */
data class DailyDetectionCount(
    val date: String,
    val count: Int
)
