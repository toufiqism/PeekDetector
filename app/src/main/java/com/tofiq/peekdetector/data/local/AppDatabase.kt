package com.tofiq.peekdetector.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tofiq.peekdetector.data.model.DetectionEvent

/**
 * Room database for storing detection events.
 * Uses singleton pattern to ensure only one instance exists.
 */
@Database(entities = [DetectionEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun detectionEventDao(): DetectionEventDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "peek_detector_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
