package com.tofiq.peekdetector

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tofiq.peekdetector.data.local.SettingsDefaults
import com.tofiq.peekdetector.data.local.settingsDataStore
import com.tofiq.peekdetector.data.model.SensitivityLevel
import com.tofiq.peekdetector.data.model.ThemeMode
import com.tofiq.peekdetector.data.repository.SettingsRepositoryImpl
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
        
        runBlocking {
            context.settingsDataStore.edit { it.clear() }
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            context.settingsDataStore.edit { it.clear() }
        }
    }

    @Test
    fun settingsIconIsDisplayedOnMainScreen() {
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
    }

    @Test
    fun clickingSettingsIconNavigatesToSettingsScreen() {
        composeTestRule.onNodeWithContentDescription("Settings")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Settings")
            .assertIsDisplayed()
    }

    @Test
    fun settingsArePersisted() = runBlocking {
        settingsRepository.setSensitivityLevel(SensitivityLevel.HIGH)
        settingsRepository.setNotificationCooldown(15)
        settingsRepository.setVibrationEnabled(false)
        settingsRepository.setThemeMode(ThemeMode.DARK)
        settingsRepository.setSmartDetectionEnabled(true)
        
        val newRepository = SettingsRepositoryImpl(context.settingsDataStore)
        
        assertEquals(SensitivityLevel.HIGH, newRepository.sensitivityLevel.first())
        assertEquals(15, newRepository.notificationCooldown.first())
        assertEquals(false, newRepository.vibrationEnabled.first())
        assertEquals(ThemeMode.DARK, newRepository.themeMode.first())
        assertEquals(true, newRepository.smartDetectionEnabled.first())
    }

    @Test
    fun defaultValuesAreUsedWhenNoSettingsExist() = runBlocking {
        context.settingsDataStore.edit { it.clear() }
        
        val newRepository = SettingsRepositoryImpl(context.settingsDataStore)
        
        assertEquals(SettingsDefaults.SENSITIVITY, newRepository.sensitivityLevel.first())
        assertEquals(SettingsDefaults.COOLDOWN, newRepository.notificationCooldown.first())
        assertEquals(SettingsDefaults.VIBRATION, newRepository.vibrationEnabled.first())
        assertEquals(SettingsDefaults.THEME, newRepository.themeMode.first())
        assertEquals(SettingsDefaults.SMART_DETECTION, newRepository.smartDetectionEnabled.first())
    }

    @Test
    fun resetToDefaultsRestoresAllSettings() = runBlocking {
        settingsRepository.setSensitivityLevel(SensitivityLevel.LOW)
        settingsRepository.setNotificationCooldown(25)
        settingsRepository.setVibrationEnabled(false)
        settingsRepository.setThemeMode(ThemeMode.LIGHT)
        settingsRepository.setSmartDetectionEnabled(true)
        
        settingsRepository.resetToDefaults()
        
        assertEquals(SettingsDefaults.SENSITIVITY, settingsRepository.sensitivityLevel.first())
        assertEquals(SettingsDefaults.COOLDOWN, settingsRepository.notificationCooldown.first())
        assertEquals(SettingsDefaults.VIBRATION, settingsRepository.vibrationEnabled.first())
        assertEquals(SettingsDefaults.THEME, settingsRepository.themeMode.first())
        assertEquals(SettingsDefaults.SMART_DETECTION, settingsRepository.smartDetectionEnabled.first())
    }
}
