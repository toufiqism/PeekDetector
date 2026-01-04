package com.tofiq.peekdetector.data.model

/**
 * Enum representing app theme modes.
 * 
 * Requirements: 5.2, 5.3, 5.4
 */
enum class ThemeMode {
    /**
     * Follow system theme settings.
     */
    SYSTEM,
    
    /**
     * Always use light theme.
     */
    LIGHT,
    
    /**
     * Always use dark theme.
     */
    DARK;
    
    companion object {
        /**
         * Returns the default theme mode.
         */
        fun default(): ThemeMode = SYSTEM
        
        /**
         * Parses a string to ThemeMode, returning default if invalid.
         */
        fun fromString(value: String?): ThemeMode {
            return entries.find { it.name == value } ?: default()
        }
    }
}
