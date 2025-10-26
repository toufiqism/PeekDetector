package com.tofiq.peekdetector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing a shoulder surfer detection event
 * Stores the number of faces detected and the timestamp
 */
@Entity(tableName = "detection_events")
data class DetectionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val faceCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

