package com.tofiq.peekdetector.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * DataStore extension property for settings.
 * Creates a single instance of DataStore for the application.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Keys for settings preferences.
 */
object SettingsKeys {
    val SENSITIVITY_LEVEL = stringPreferencesKey("sensitivity_level")
    val NOTIFICATION_COOLDOWN = intPreferencesKey("notification_cooldown")
    val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val SMART_DETECTION_ENABLED = booleanPreferencesKey("smart_detection_enabled")
}

/**
 * Default values for settings.
 */
object SettingsDefaults {
    const val COOLDOWN = 5
    const val COOLDOWN_MIN = 3
    const val COOLDOWN_MAX = 30
    const val VIBRATION = true
    const val SMART_DETECTION = true
    
    // Default enum values (for testing)
    val SENSITIVITY = com.tofiq.peekdetector.data.model.SensitivityLevel.MEDIUM
    val THEME = com.tofiq.peekdetector.data.model.ThemeMode.SYSTEM
}
