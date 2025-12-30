package com.tofiq.peekdetector.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

/**
 * Property-based tests for SettingsRepository.
 * 
 * Feature: settings-screen, Property 1: Settings Persistence Round Trip
 * Validates: Requirements 7.1, 7.2
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryPropertyTest {

    private lateinit var testScope: CoroutineScope
    private lateinit var testFile: File
    private lateinit var repository: SettingsRepositoryImpl

    @BeforeEach
    fun setup() {
        testScope = CoroutineScope(Dispatchers.IO + Job())
        testFile = File.createTempFile("test_ds_${UUID.randomUUID()}", ".preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        repository = SettingsRepositoryImpl(dataStore)
    }

    @AfterEach
    fun teardown() {
        testScope.cancel()
        testFile.delete()
    }

    /**
     * Property 1: Settings Persistence Round Trip
     * For any valid settings value, storing it via the repository and then 
     * reading it back SHALL return an equivalent value.
     * 
     * Validates: Requirements 7.1, 7.2
     */
    @Test
    fun `Property 1 - All SensitivityLevel values round trip correctly`() = runBlocking {
        SensitivityLevel.entries.forEach { level ->
            repository.setSensitivityLevel(level)
            val retrieved = repository.sensitivityLevel.first()
            assertEquals(level, retrieved, "SensitivityLevel $level should round trip")
        }
    }

    @Test
    fun `Property 1 - All ThemeMode values round trip correctly`() = runBlocking {
        ThemeMode.entries.forEach { mode ->
            repository.setThemeMode(mode)
            val retrieved = repository.themeMode.first()
            assertEquals(mode, retrieved, "ThemeMode $mode should round trip")
        }
    }

    @Test
    fun `Property 1 - Cooldown values within bounds round trip correctly`() = runBlocking {
        val testValues = listOf(
            SettingsDefaults.COOLDOWN_MIN,
            SettingsDefaults.COOLDOWN_MAX,
            (SettingsDefaults.COOLDOWN_MIN + SettingsDefaults.COOLDOWN_MAX) / 2,
            10, 15, 20, 25
        )
        
        testValues.forEach { cooldown ->
            repository.setNotificationCooldown(cooldown)
            val retrieved = repository.notificationCooldown.first()
            assertEquals(cooldown, retrieved, "Cooldown $cooldown should round trip")
        }
    }

    @Test
    fun `Property 1 - Vibration enabled round trips correctly`() = runBlocking {
        listOf(true, false).forEach { enabled ->
            repository.setVibrationEnabled(enabled)
            val retrieved = repository.vibrationEnabled.first()
            assertEquals(enabled, retrieved, "Vibration $enabled should round trip")
        }
    }

    @Test
    fun `Property 1 - Smart detection enabled round trips correctly`() = runBlocking {
        listOf(true, false).forEach { enabled ->
            repository.setSmartDetectionEnabled(enabled)
            val retrieved = repository.smartDetectionEnabled.first()
            assertEquals(enabled, retrieved, "Smart detection $enabled should round trip")
        }
    }

    @Test
    fun `Property 1 - Default values are returned when no value is stored`() = runBlocking {
        assertEquals(SettingsDefaults.SENSITIVITY, repository.sensitivityLevel.first())
        assertEquals(SettingsDefaults.THEME, repository.themeMode.first())
        assertEquals(SettingsDefaults.COOLDOWN, repository.notificationCooldown.first())
        assertEquals(SettingsDefaults.VIBRATION, repository.vibrationEnabled.first())
        assertEquals(SettingsDefaults.SMART_DETECTION, repository.smartDetectionEnabled.first())
    }

    /**
     * Property 3: Cooldown Value Bounds
     * For any cooldown value set through the UI, the stored value SHALL be 
     * within the valid range of 3 to 30 seconds.
     * 
     * Feature: settings-screen, Property 3: Cooldown Value Bounds
     * Validates: Requirements 3.1, 3.2
     */
    @Test
    fun `Property 3 - Cooldown values are always clamped to valid bounds`(): Unit = runBlocking {
        // Test with arbitrary integers including values outside bounds
        checkAll(100, Arb.int(-100, 100)) { inputValue ->
            // Create fresh repository for each iteration to avoid state pollution
            val iterationFile = File.createTempFile("test_ds_iter_${UUID.randomUUID()}", ".preferences_pb")
            val iterationScope = CoroutineScope(Dispatchers.IO + Job())
            try {
                val iterationDataStore = PreferenceDataStoreFactory.create(
                    scope = iterationScope,
                    produceFile = { iterationFile }
                )
                val iterationRepository = SettingsRepositoryImpl(iterationDataStore)
                
                iterationRepository.setNotificationCooldown(inputValue)
                val storedValue = iterationRepository.notificationCooldown.first()
                
                // Property: stored value must always be within bounds [3, 30]
                assertTrue(
                    storedValue >= SettingsDefaults.COOLDOWN_MIN,
                    "Stored cooldown $storedValue should be >= ${SettingsDefaults.COOLDOWN_MIN} for input $inputValue"
                )
                assertTrue(
                    storedValue <= SettingsDefaults.COOLDOWN_MAX,
                    "Stored cooldown $storedValue should be <= ${SettingsDefaults.COOLDOWN_MAX} for input $inputValue"
                )
                
                // Additional property: if input is within bounds, it should be stored exactly
                if (inputValue in SettingsDefaults.COOLDOWN_MIN..SettingsDefaults.COOLDOWN_MAX) {
                    assertEquals(
                        inputValue, 
                        storedValue, 
                        "Valid input $inputValue should be stored exactly"
                    )
                }
            } finally {
                iterationScope.cancel()
                iterationFile.delete()
            }
        }
    }

    /**
     * Property 5: Reset Restores Defaults
     * For any combination of modified settings, calling resetToDefaults SHALL 
     * restore all settings to their defined default values.
     * 
     * Feature: settings-screen, Property 5: Reset Restores Defaults
     * Validates: Requirements 8.3, 8.5
     */
    @Test
    fun `Property 5 - Reset restores all settings to defaults`(): Unit = runBlocking {
        // Generate arbitrary combinations of settings
        checkAll(
            100,
            Arb.int(0, SensitivityLevel.entries.size - 1),
            Arb.int(0, ThemeMode.entries.size - 1),
            Arb.int(SettingsDefaults.COOLDOWN_MIN, SettingsDefaults.COOLDOWN_MAX),
            Arb.boolean(),
            Arb.boolean()
        ) { sensitivityIndex, themeIndex, cooldown, vibration, smartDetection ->
            // Create fresh repository for each iteration
            val iterationFile = File.createTempFile("test_ds_reset_${UUID.randomUUID()}", ".preferences_pb")
            val iterationScope = CoroutineScope(Dispatchers.IO + Job())
            try {
                val iterationDataStore = PreferenceDataStoreFactory.create(
                    scope = iterationScope,
                    produceFile = { iterationFile }
                )
                val iterationRepository = SettingsRepositoryImpl(iterationDataStore)
                
                // Apply arbitrary settings
                val sensitivity = SensitivityLevel.entries[sensitivityIndex]
                val theme = ThemeMode.entries[themeIndex]
                
                iterationRepository.setSensitivityLevel(sensitivity)
                iterationRepository.setThemeMode(theme)
                iterationRepository.setNotificationCooldown(cooldown)
                iterationRepository.setVibrationEnabled(vibration)
                iterationRepository.setSmartDetectionEnabled(smartDetection)
                
                // Verify settings were applied
                assertEquals(sensitivity, iterationRepository.sensitivityLevel.first())
                assertEquals(theme, iterationRepository.themeMode.first())
                assertEquals(cooldown, iterationRepository.notificationCooldown.first())
                assertEquals(vibration, iterationRepository.vibrationEnabled.first())
                assertEquals(smartDetection, iterationRepository.smartDetectionEnabled.first())
                
                // Reset to defaults
                iterationRepository.resetToDefaults()
                
                // Property: All settings must be restored to defaults
                assertEquals(
                    SettingsDefaults.SENSITIVITY,
                    iterationRepository.sensitivityLevel.first(),
                    "Sensitivity should be reset to default"
                )
                assertEquals(
                    SettingsDefaults.THEME,
                    iterationRepository.themeMode.first(),
                    "Theme should be reset to default"
                )
                assertEquals(
                    SettingsDefaults.COOLDOWN,
                    iterationRepository.notificationCooldown.first(),
                    "Cooldown should be reset to default"
                )
                assertEquals(
                    SettingsDefaults.VIBRATION,
                    iterationRepository.vibrationEnabled.first(),
                    "Vibration should be reset to default"
                )
                assertEquals(
                    SettingsDefaults.SMART_DETECTION,
                    iterationRepository.smartDetectionEnabled.first(),
                    "Smart detection should be reset to default"
                )
            } finally {
                iterationScope.cancel()
                iterationFile.delete()
            }
        }
    }
}
