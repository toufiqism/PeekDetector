package com.tofiq.peekdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tofiq.peekdetector.data.SensitivityLevel
import com.tofiq.peekdetector.data.SettingsDefaults
import com.tofiq.peekdetector.data.SettingsRepository
import com.tofiq.peekdetector.data.SettingsRepositoryImpl
import com.tofiq.peekdetector.data.ThemeMode
import com.tofiq.peekdetector.data.settingsDataStore
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme
import kotlinx.coroutines.launch

/**
 * Settings Activity for managing app preferences.
 * 
 * Requirements: 1.1, 1.2
 */
class SettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val settingsRepository = remember {
                SettingsRepositoryImpl(applicationContext.settingsDataStore)
            }
            
            // Collect theme mode to apply correct theme
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            
            // Determine if dark theme should be used based on setting
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            
            PeekDetectorTheme(darkTheme = darkTheme) {
                SettingsScreen(
                    repository = settingsRepository,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

/**
 * Main Settings Screen composable.
 * 
 * Requirements: 1.2, 1.3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    onBackClick: () -> Unit
) {
    // Collect settings as state
    val sensitivity by repository.sensitivityLevel.collectAsState(initial = SensitivityLevel.MEDIUM)
    val cooldown by repository.notificationCooldown.collectAsState(initial = SettingsDefaults.COOLDOWN)
    val vibrationEnabled by repository.vibrationEnabled.collectAsState(initial = SettingsDefaults.VIBRATION)
    
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background gradient (consistent with app style)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0D47A1),
                            Color(0xFF1565C0),
                            Color(0xFF1976D2)
                        )
                    )
                )
        )
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Settings",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D47A1)
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Detection Settings Section
                item {
                    SettingsSectionHeader(title = "Detection")
                }
                item {
                    SensitivitySelector(
                        selected = sensitivity,
                        onSelect = { level ->
                            scope.launch { repository.setSensitivityLevel(level) }
                        }
                    )
                }
                
                // Alert Settings Section
                item {
                    SettingsSectionHeader(title = "Alerts")
                }
                item {
                    CooldownSlider(
                        value = cooldown,
                        onValueChange = { value ->
                            scope.launch { repository.setNotificationCooldown(value) }
                        }
                    )
                }
                item {
                    SwitchPreference(
                        title = "Vibration",
                        subtitle = "Vibrate on detection alerts",
                        checked = vibrationEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { repository.setVibrationEnabled(enabled) }
                        }
                    )
                }
                
                // Appearance Settings Section
                item {
                    SettingsSectionHeader(title = "Appearance")
                }
                item {
                    val themeMode by repository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
                    ThemeSelector(
                        selected = themeMode,
                        onSelect = { mode ->
                            scope.launch { repository.setThemeMode(mode) }
                        }
                    )
                }
                
                // Power Settings Section
                item {
                    SettingsSectionHeader(title = "Power")
                }
                item {
                    val smartDetectionEnabled by repository.smartDetectionEnabled.collectAsState(initial = SettingsDefaults.SMART_DETECTION)
                    SwitchPreference(
                        title = "Smart Detection",
                        subtitle = "Pause detection when screen is off to save battery",
                        checked = smartDetectionEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { repository.setSmartDetectionEnabled(enabled) }
                        }
                    )
                }
                
                // Reset Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    ResetToDefaultsButton(
                        onReset = { scope.launch { repository.resetToDefaults() } }
                    )
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Section header for grouping related settings.
 */
@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

/**
 * Sensitivity selector composable with three radio button options.
 * Displays Low, Medium, and High sensitivity levels with descriptions.
 * 
 * Requirements: 2.1, 2.5
 * 
 * @param selected The currently selected sensitivity level
 * @param onSelect Callback when a sensitivity level is selected
 */
@Composable
fun SensitivitySelector(
    selected: SensitivityLevel,
    onSelect: (SensitivityLevel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Detection Sensitivity",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SensitivityLevel.entries.forEach { level ->
                SensitivityOption(
                    level = level,
                    isSelected = selected == level,
                    onSelect = { onSelect(level) }
                )
            }
        }
    }
}

/**
 * Individual sensitivity option with radio button, title, and description.
 */
@Composable
private fun SensitivityOption(
    level: SensitivityLevel,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.White,
                unselectedColor = Color.White.copy(alpha = 0.6f)
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = getSensitivityTitle(level),
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            Text(
                text = getSensitivityDescription(level),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Returns the display title for a sensitivity level.
 */
private fun getSensitivityTitle(level: SensitivityLevel): String {
    return when (level) {
        SensitivityLevel.LOW -> "Low"
        SensitivityLevel.MEDIUM -> "Medium (Default)"
        SensitivityLevel.HIGH -> "High"
    }
}

/**
 * Returns the description for a sensitivity level.
 */
private fun getSensitivityDescription(level: SensitivityLevel): String {
    return when (level) {
        SensitivityLevel.LOW -> "Lower battery usage, processes every 5th frame"
        SensitivityLevel.MEDIUM -> "Balanced performance, processes every 3rd frame"
        SensitivityLevel.HIGH -> "Maximum accuracy, processes every frame"
    }
}

/**
 * Cooldown slider composable for configuring notification cooldown period.
 * Displays a slider with range 3-30 seconds and current value label.
 * 
 * Requirements: 3.1, 3.4
 * 
 * @param value The current cooldown value in seconds
 * @param onValueChange Callback when the cooldown value changes
 */
@Composable
fun CooldownSlider(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notification Cooldown",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = "${value}s",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Minimum time between alerts",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = SettingsDefaults.COOLDOWN_MIN.toFloat()..SettingsDefaults.COOLDOWN_MAX.toFloat(),
                steps = SettingsDefaults.COOLDOWN_MAX - SettingsDefaults.COOLDOWN_MIN - 1,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${SettingsDefaults.COOLDOWN_MIN}s",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Text(
                    text = "${SettingsDefaults.COOLDOWN_MAX}s",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Reusable switch preference composable with title and subtitle.
 * 
 * Requirements: 4.1, 4.4
 * 
 * @param title The main title text
 * @param subtitle The subtitle/description text
 * @param checked Whether the switch is currently checked
 * @param onCheckedChange Callback when the switch state changes
 */
@Composable
fun SwitchPreference(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.White.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }
    }
}

/**
 * Theme selector composable with three options: System, Light, Dark.
 * Displays radio button options for selecting the app theme mode.
 * 
 * Requirements: 5.1, 5.5
 * 
 * @param selected The currently selected theme mode
 * @param onSelect Callback when a theme mode is selected
 */
@Composable
fun ThemeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Theme",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ThemeMode.entries.forEach { mode ->
                ThemeOption(
                    mode = mode,
                    isSelected = selected == mode,
                    onSelect = { onSelect(mode) }
                )
            }
        }
    }
}

/**
 * Individual theme option with radio button, title, and description.
 */
@Composable
private fun ThemeOption(
    mode: ThemeMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.White,
                unselectedColor = Color.White.copy(alpha = 0.6f)
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = getThemeTitle(mode),
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            Text(
                text = getThemeDescription(mode),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Returns the display title for a theme mode.
 */
private fun getThemeTitle(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> "System Default"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
}

/**
 * Returns the description for a theme mode.
 */
private fun getThemeDescription(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> "Follow device theme settings"
        ThemeMode.LIGHT -> "Always use light theme"
        ThemeMode.DARK -> "Always use dark theme"
    }
}

/**
 * Reset to defaults button with confirmation dialog.
 * Displays a button that shows a confirmation dialog before resetting all settings.
 * 
 * Requirements: 8.1, 8.2, 8.4
 * 
 * @param onReset Callback when the user confirms the reset action
 */
@Composable
fun ResetToDefaultsButton(
    onReset: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showConfirmDialog = true },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reset to Defaults",
                color = Color(0xFFFF6B6B),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }
    }
    
    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Reset Settings?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will restore all settings to their default values. This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showConfirmDialog = false
                    }
                ) {
                    Text(
                        text = "Reset",
                        color = Color(0xFFFF6B6B)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}
