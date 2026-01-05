package com.tofiq.peekdetector.data.model

/**
 * Enum representing detection sensitivity levels.
 * Each level has a corresponding frame skip value for battery optimization.
 * 
 * Requirements: 2.2, 2.3, 2.4
 */
enum class SensitivityLevel(val frameSkip: Int) {
    /**
     * Low sensitivity - processes every 5th frame.
     * Best for battery life, lower detection accuracy.
     */
    LOW(5),
    
    /**
     * Medium sensitivity - processes every 3rd frame.
     * Balanced performance and battery usage.
     */
    MEDIUM(3),
    
    /**
     * High sensitivity - processes every frame.
     * Maximum accuracy, higher battery consumption.
     */
    HIGH(1);
    
    companion object {
        /**
         * Returns the default sensitivity level.
         */
        fun default(): SensitivityLevel = MEDIUM
        
        /**
         * Parses a string to SensitivityLevel, returning default if invalid.
         */
        fun fromString(value: String?): SensitivityLevel {
            return entries.find { it.name == value } ?: default()
        }
    }
}
