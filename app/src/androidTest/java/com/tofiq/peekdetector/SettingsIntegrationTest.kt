package com.tofiq.peekdetector

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tofiq.peekdetector.data.SensitivityLevel
import com.tofiq.peekdetector.data.SettingsDefaults
import com.tofiq.peekdetector.data.SettingsRepositoryImpl
import com.tofiq.peekdetector.data.ThemeMode
import com.tofiq.peekdetector.data.settingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for Settings Screen functionality.
 * Tests settings navigation flow, persistence across app restart, and service behavior.
 * 
 * Requirements: 1.1, 1.2, 1.3, 7.1, 7.2
 */
@RunWith(AndroidJUnit4::class)
class SettingsIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = SettingsRepositoryImpl(context.settingsDataStore)
        
        // Clear settings before each test
        runBlocking {
            context.settingsDataStore.edit { it.clear() }
        }
    }

    @After
    fun tearDown() {
        // Clean up settings after each test
        runBlocking {
            context.settingsDataStore.edit { it.clear() }
        }
    }

    /**
     * Test that settings icon is displayed on MainActivity.
     * Requirements: 1.1
     */
    @Test
    fun settingsIconIsDisplayedOnMainScreen() {
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
    }

    /**
     * Test that clicking settings icon navigates to SettingsActivity.
     * Requirements: 1.1
     */
    @Test
    fun clickingSettingsIconNavigatesToSettingsScreen() {
        // Click the settings icon
        composeTestRule.onNodeWithContentDescription("Settings")
            .performClick()
        
        // Wait for navigation and verify Settings screen is displayed
        composeTestRule.waitForIdle()
        
        // The Settings title should be visible in the top bar
        composeTestRule.onNodeWithText("Settings")
            .assertIsDisplayed()
    }

    /**
     * Test that settings are persisted and loaded correctly.
     * Requirements: 7.1, 7.2
     */
    @Test
    fun settingsArePersisted() = runBlocking {
        // Set custom values
        settingsRepository.setSensitivityLevel(SensitivityLevel.HIGH)
        settingsRepository.setNotificationCooldown(15)
        settingsRepository.setVibrationEnabled(false)
        settingsRepository.setThemeMode(ThemeMode.DARK)
        settingsRepository.setSmartDetectionEnabled(true)
        
        // Create a new repository instance to simulate app restart
        val newRepository = SettingsRepositoryImpl(context.settingsDataStore)
        
        // Verify values are persisted
        assertEquals(SensitivityLevel.HIGH, newRepository.sensitivityLevel.first())
        assertEquals(15, newRepository.notificationCooldown.first())
        assertEquals(false, newRepository.vibrationEnabled.first())
        assertEquals(ThemeMode.DARK, newRepository.themeMode.first())
        assertEquals(true, newRepository.smartDetectionEnabled.first())
    }

    /**
     * Test that default values are used when no settings exist.
     * Requirements: 7.3
     */
    @Test
    fun defaultValuesAreUsedWhenNoSettingsExist() = runBlocking {
        // Clear all settings
        context.settingsDataStore.edit { it.clear() }
        
        // Create a new repository instance
        val newRepository = SettingsRepositoryImpl(context.settingsDataStore)
        
        // Verify default values
        assertEquals(SettingsDefaults.SENSITIVITY, newRepository.sensitivityLevel.first())
        assertEquals(SettingsDefaults.COOLDOWN, newRepository.notificationCooldown.first())
        assertEquals(SettingsDefaults.VIBRATION, newRepository.vibrationEnabled.first())
        assertEquals(SettingsDefaults.THEME, newRepository.themeMode.first())
        assertEquals(SettingsDefaults.SMART_DETECTION, newRepository.smartDetectionEnabled.first())
    }

    /**
     * Test that reset to defaults restores all settings.
     * Requirements: 8.3, 8.5
     */
    @Test
    fun resetToDefaultsRestoresAllSettings() = runBlocking {
        // Set custom values
        settingsRepository.setSensitivityLevel(SensitivityLevel.LOW)
        settingsRepository.setNotificationCooldown(25)
        settingsRepository.setVibrationEnabled(false)
        settingsRepository.setThemeMode(ThemeMode.LIGHT)
        settingsRepository.setSmartDetectionEnabled(true)
        
        // Reset to defaults
        settingsRepository.resetToDefaults()
        
        // Verify all values are reset to defaults
        assertEquals(SettingsDefaults.SENSITIVITY, settingsRepository.sensitivityLevel.first())
        assertEquals(SettingsDefaults.COOLDOWN, settingsRepository.notificationCooldown.first())
        assertEquals(SettingsDefaults.VIBRATION, settingsRepository.vibrationEnabled.first())
        assertEquals(SettingsDefaults.THEME, settingsRepository.themeMode.first())
        assertEquals(SettingsDefaults.SMART_DETECTION, settingsRepository.smartDetectionEnabled.first())
    }
}
