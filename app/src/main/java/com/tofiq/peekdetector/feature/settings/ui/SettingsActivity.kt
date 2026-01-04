package com.tofiq.peekdetector.feature.settings.ui

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
import com.tofiq.peekdetector.data.local.SettingsDefaults
import com.tofiq.peekdetector.data.local.settingsDataStore
import com.tofiq.peekdetector.data.model.SensitivityLevel
import com.tofiq.peekdetector.data.model.ThemeMode
import com.tofiq.peekdetector.data.repository.SettingsRepository
import com.tofiq.peekdetector.data.repository.SettingsRepositoryImpl
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme
import kotlinx.coroutines.launch

/**
 * Settings Activity for managing app preferences.
 */
class SettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val settingsRepository = remember {
                SettingsRepositoryImpl(applicationContext.settingsDataStore)
            }
            
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    onBackClick: () -> Unit
) {
    val sensitivity by repository.sensitivityLevel.collectAsState(initial = SensitivityLevel.MEDIUM)
    val cooldown by repository.notificationCooldown.collectAsState(initial = SettingsDefaults.COOLDOWN)
    val vibrationEnabled by repository.vibrationEnabled.collectAsState(initial = SettingsDefaults.VIBRATION)
    
    val scope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize()) {
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
                item { SettingsSectionHeader(title = "Detection") }
                item {
                    SensitivitySelector(
                        selected = sensitivity,
                        onSelect = { level ->
                            scope.launch { repository.setSensitivityLevel(level) }
                        }
                    )
                }
                
                item { SettingsSectionHeader(title = "Alerts") }
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
                
                item { SettingsSectionHeader(title = "Appearance") }
                item {
                    val themeMode by repository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
                    ThemeSelector(
                        selected = themeMode,
                        onSelect = { mode ->
                            scope.launch { repository.setThemeMode(mode) }
                        }
                    )
                }
                
                item { SettingsSectionHeader(title = "Power") }
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
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    ResetToDefaultsButton(
                        onReset = { scope.launch { repository.resetToDefaults() } }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

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

private fun getSensitivityTitle(level: SensitivityLevel): String {
    return when (level) {
        SensitivityLevel.LOW -> "Low"
        SensitivityLevel.MEDIUM -> "Medium (Default)"
        SensitivityLevel.HIGH -> "High"
    }
}

private fun getSensitivityDescription(level: SensitivityLevel): String {
    return when (level) {
        SensitivityLevel.LOW -> "Lower battery usage, processes every 5th frame"
        SensitivityLevel.MEDIUM -> "Balanced performance, processes every 3rd frame"
        SensitivityLevel.HIGH -> "Maximum accuracy, processes every frame"
    }
}

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
            Column(modifier = Modifier.weight(1f)) {
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

private fun getThemeTitle(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> "System Default"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
}

private fun getThemeDescription(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> "Follow device theme settings"
        ThemeMode.LIGHT -> "Always use light theme"
        ThemeMode.DARK -> "Always use dark theme"
    }
}

@Composable
fun ResetToDefaultsButton(onReset: () -> Unit) {
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
