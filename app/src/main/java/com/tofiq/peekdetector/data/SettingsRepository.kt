package com.tofiq.peekdetector.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing app settings.
 * Provides Flow-based access for reactive UI updates.
 * 
 * Requirements: 7.1, 7.4
 */
interface SettingsRepository {
    
    // Detection Settings
    /**
     * Flow of the current sensitivity level setting.
     */
    val sensitivityLevel: Flow<SensitivityLevel>
    
    /**
     * Sets the detection sensitivity level.
     * @param level The sensitivity level to set
     */
    suspend fun setSensitivityLevel(level: SensitivityLevel)
    
    // Alert Settings
    /**
     * Flow of the notification cooldown in seconds.
     */
    val notificationCooldown: Flow<Int>
    
    /**
     * Sets the notification cooldown period.
     * @param seconds Cooldown duration in seconds (will be clamped to valid range 3-30)
     */
    suspend fun setNotificationCooldown(seconds: Int)
    
    /**
     * Flow of the vibration enabled setting.
     */
    val vibrationEnabled: Flow<Boolean>
    
    /**
     * Sets whether vibration is enabled for alerts.
     * @param enabled True to enable vibration, false to disable
     */
    suspend fun setVibrationEnabled(enabled: Boolean)
    
    // Appearance Settings
    /**
     * Flow of the current theme mode setting.
     */
    val themeMode: Flow<ThemeMode>
    
    /**
     * Sets the app theme mode.
     * @param mode The theme mode to set
     */
    suspend fun setThemeMode(mode: ThemeMode)
    
    // Power Settings
    /**
     * Flow of the smart detection enabled setting.
     */
    val smartDetectionEnabled: Flow<Boolean>
    
    /**
     * Sets whether smart detection (pause when screen off) is enabled.
     * @param enabled True to enable smart detection, false to disable
     */
    suspend fun setSmartDetectionEnabled(enabled: Boolean)
    
    // Reset
    /**
     * Resets all settings to their default values.
     */
    suspend fun resetToDefaults()
}
