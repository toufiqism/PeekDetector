package com.tofiq.peekdetector.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Implementation of SettingsRepository using Jetpack DataStore.
 * 
 * Requirements: 7.1, 7.2, 7.3, 8.3
 */
class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    
    companion object {
        private const val TAG = "SettingsRepository"
    }
    
    // Detection Settings
    override val sensitivityLevel: Flow<SensitivityLevel> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading sensitivity level", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val value = preferences[SettingsKeys.SENSITIVITY_KEY]
            if (value != null) {
                try {
                    SensitivityLevel.valueOf(value)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Unknown sensitivity level: $value, using default")
                    SettingsDefaults.SENSITIVITY
                }
            } else {
                SettingsDefaults.SENSITIVITY
            }
        }
    
    override suspend fun setSensitivityLevel(level: SensitivityLevel) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.SENSITIVITY_KEY] = level.name
        }
    }
    
    // Alert Settings
    override val notificationCooldown: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading notification cooldown", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SettingsKeys.COOLDOWN_KEY] ?: SettingsDefaults.COOLDOWN
        }

    
    override suspend fun setNotificationCooldown(seconds: Int) {
        // Clamp value to valid range
        val clampedValue = seconds.coerceIn(SettingsDefaults.COOLDOWN_MIN, SettingsDefaults.COOLDOWN_MAX)
        dataStore.edit { preferences ->
            preferences[SettingsKeys.COOLDOWN_KEY] = clampedValue
        }
    }
    
    override val vibrationEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading vibration setting", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SettingsKeys.VIBRATION_KEY] ?: SettingsDefaults.VIBRATION
        }
    
    override suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.VIBRATION_KEY] = enabled
        }
    }
    
    // Appearance Settings
    override val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading theme mode", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val value = preferences[SettingsKeys.THEME_KEY]
            if (value != null) {
                try {
                    ThemeMode.valueOf(value)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Unknown theme mode: $value, using default")
                    SettingsDefaults.THEME
                }
            } else {
                SettingsDefaults.THEME
            }
        }
    
    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.THEME_KEY] = mode.name
        }
    }
    
    // Power Settings
    override val smartDetectionEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading smart detection setting", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SettingsKeys.SMART_DETECTION_KEY] ?: SettingsDefaults.SMART_DETECTION
        }
    
    override suspend fun setSmartDetectionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.SMART_DETECTION_KEY] = enabled
        }
    }
    
    // Reset
    override suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.SENSITIVITY_KEY] = SettingsDefaults.SENSITIVITY.name
            preferences[SettingsKeys.COOLDOWN_KEY] = SettingsDefaults.COOLDOWN
            preferences[SettingsKeys.VIBRATION_KEY] = SettingsDefaults.VIBRATION
            preferences[SettingsKeys.THEME_KEY] = SettingsDefaults.THEME.name
            preferences[SettingsKeys.SMART_DETECTION_KEY] = SettingsDefaults.SMART_DETECTION
        }
    }
}
