package com.tofiq.peekdetector.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.tofiq.peekdetector.data.local.SettingsDefaults
import com.tofiq.peekdetector.data.local.SettingsKeys
import com.tofiq.peekdetector.data.model.SensitivityLevel
import com.tofiq.peekdetector.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of SettingsRepository using DataStore.
 * Provides reactive access to app settings with persistence.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4
 */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    
    // Detection Settings
    override val sensitivityLevel: Flow<SensitivityLevel> = dataStore.data.map { preferences ->
        SensitivityLevel.fromString(preferences[SettingsKeys.SENSITIVITY_LEVEL])
    }
    
    override suspend fun setSensitivityLevel(level: SensitivityLevel) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.SENSITIVITY_LEVEL] = level.name
        }
    }
    
    // Alert Settings
    override val notificationCooldown: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SettingsKeys.NOTIFICATION_COOLDOWN] ?: SettingsDefaults.COOLDOWN
    }
    
    override suspend fun setNotificationCooldown(seconds: Int) {
        val clampedValue = seconds.coerceIn(SettingsDefaults.COOLDOWN_MIN, SettingsDefaults.COOLDOWN_MAX)
        dataStore.edit { preferences ->
            preferences[SettingsKeys.NOTIFICATION_COOLDOWN] = clampedValue
        }
    }
    
    override val vibrationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SettingsKeys.VIBRATION_ENABLED] ?: SettingsDefaults.VIBRATION
    }
    
    override suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.VIBRATION_ENABLED] = enabled
        }
    }
    
    // Appearance Settings
    override val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.fromString(preferences[SettingsKeys.THEME_MODE])
    }
    
    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.THEME_MODE] = mode.name
        }
    }
    
    // Power Settings
    override val smartDetectionEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SettingsKeys.SMART_DETECTION_ENABLED] ?: SettingsDefaults.SMART_DETECTION
    }
    
    override suspend fun setSmartDetectionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.SMART_DETECTION_ENABLED] = enabled
        }
    }
    
    // Reset
    override suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.SENSITIVITY_LEVEL] = SensitivityLevel.default().name
            preferences[SettingsKeys.NOTIFICATION_COOLDOWN] = SettingsDefaults.COOLDOWN
            preferences[SettingsKeys.VIBRATION_ENABLED] = SettingsDefaults.VIBRATION
            preferences[SettingsKeys.THEME_MODE] = ThemeMode.default().name
            preferences[SettingsKeys.SMART_DETECTION_ENABLED] = SettingsDefaults.SMART_DETECTION
        }
    }
}
