package com.tofiq.peekdetector.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.tofiq.peekdetector.data.local.SettingsDefaults
import com.tofiq.peekdetector.data.model.SensitivityLevel
import com.tofiq.peekdetector.data.model.ThemeMode
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

    @Test
    fun `Property 3 - Cooldown values are always clamped to valid bounds`(): Unit = runBlocking {
        checkAll(100, Arb.int(-100, 100)) { inputValue ->
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
                
                assertTrue(
                    storedValue >= SettingsDefaults.COOLDOWN_MIN,
                    "Stored cooldown $storedValue should be >= ${SettingsDefaults.COOLDOWN_MIN} for input $inputValue"
                )
                assertTrue(
                    storedValue <= SettingsDefaults.COOLDOWN_MAX,
                    "Stored cooldown $storedValue should be <= ${SettingsDefaults.COOLDOWN_MAX} for input $inputValue"
                )
                
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

    @Test
    fun `Property 5 - Reset restores all settings to defaults`(): Unit = runBlocking {
        checkAll(
            100,
            Arb.int(0, SensitivityLevel.entries.size - 1),
            Arb.int(0, ThemeMode.entries.size - 1),
            Arb.int(SettingsDefaults.COOLDOWN_MIN, SettingsDefaults.COOLDOWN_MAX),
            Arb.boolean(),
            Arb.boolean()
        ) { sensitivityIndex, themeIndex, cooldown, vibration, smartDetection ->
            val iterationFile = File.createTempFile("test_ds_reset_${UUID.randomUUID()}", ".preferences_pb")
            val iterationScope = CoroutineScope(Dispatchers.IO + Job())
            try {
                val iterationDataStore = PreferenceDataStoreFactory.create(
                    scope = iterationScope,
                    produceFile = { iterationFile }
                )
                val iterationRepository = SettingsRepositoryImpl(iterationDataStore)
                
                val sensitivity = SensitivityLevel.entries[sensitivityIndex]
                val theme = ThemeMode.entries[themeIndex]
                
                iterationRepository.setSensitivityLevel(sensitivity)
                iterationRepository.setThemeMode(theme)
                iterationRepository.setNotificationCooldown(cooldown)
                iterationRepository.setVibrationEnabled(vibration)
                iterationRepository.setSmartDetectionEnabled(smartDetection)
                
                assertEquals(sensitivity, iterationRepository.sensitivityLevel.first())
                assertEquals(theme, iterationRepository.themeMode.first())
                assertEquals(cooldown, iterationRepository.notificationCooldown.first())
                assertEquals(vibration, iterationRepository.vibrationEnabled.first())
                assertEquals(smartDetection, iterationRepository.smartDetectionEnabled.first())
                
                iterationRepository.resetToDefaults()
                
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
