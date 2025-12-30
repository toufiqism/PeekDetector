package com.tofiq.peekdetector.data

/**
 * Enum representing detection sensitivity levels.
 * Each level defines how many frames to skip between processing.
 * 
 * @property frameSkip Number of frames to skip between processing.
 *                     Lower values = higher sensitivity (more CPU usage).
 */
enum class SensitivityLevel(val frameSkip: Int) {
    LOW(5),      // Process every 5th frame - lowest CPU usage
    MEDIUM(3),   // Process every 3rd frame - balanced (default)
    HIGH(1)      // Process every frame - highest accuracy
}
