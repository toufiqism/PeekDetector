package com.tofiq.peekdetector.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * DataStore extension for Context to access settings preferences.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

/**
 * Preference keys for all app settings.
 */
object SettingsKeys {
    // Detection Settings
    val SENSITIVITY_KEY = stringPreferencesKey("sensitivity_level")
    
    // Alert Settings
    val COOLDOWN_KEY = intPreferencesKey("notification_cooldown")
    val VIBRATION_KEY = booleanPreferencesKey("vibration_enabled")
    
    // Appearance Settings
    val THEME_KEY = stringPreferencesKey("theme_mode")
    
    // Power Settings
    val SMART_DETECTION_KEY = booleanPreferencesKey("smart_detection_enabled")
}

/**
 * Default values for all settings.
 */
object SettingsDefaults {
    val SENSITIVITY = SensitivityLevel.MEDIUM
    const val COOLDOWN = 5
    const val VIBRATION = true
    val THEME = ThemeMode.SYSTEM
    const val SMART_DETECTION = false
    
    // Cooldown bounds
    const val COOLDOWN_MIN = 3
    const val COOLDOWN_MAX = 30
}
