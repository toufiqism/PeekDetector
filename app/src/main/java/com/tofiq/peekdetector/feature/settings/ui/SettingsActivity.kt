package com.tofiq.peekdetector.feature.settings.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tofiq.peekdetector.R
import com.tofiq.peekdetector.data.local.SettingsDefaults
import com.tofiq.peekdetector.data.local.settingsDataStore
import com.tofiq.peekdetector.data.model.SensitivityLevel
import com.tofiq.peekdetector.data.model.ThemeMode
import com.tofiq.peekdetector.data.repository.SettingsRepository
import com.tofiq.peekdetector.data.repository.SettingsRepositoryImpl
import com.tofiq.peekdetector.ui.components.GlassCard
import com.tofiq.peekdetector.ui.components.GradientBackground
import com.tofiq.peekdetector.ui.components.SectionHeader
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsRepository = remember { SettingsRepositoryImpl(applicationContext.settingsDataStore) }
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PeekDetectorTheme(darkTheme = darkTheme) {
                SettingsScreen(repository = settingsRepository, onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repository: SettingsRepository, onBackClick: () -> Unit) {
    val sensitivity by repository.sensitivityLevel.collectAsState(initial = SensitivityLevel.MEDIUM)
    val cooldown by repository.notificationCooldown.collectAsState(initial = SettingsDefaults.COOLDOWN)
    val vibrationEnabled by repository.vibrationEnabled.collectAsState(initial = SettingsDefaults.VIBRATION)
    val scope = rememberCoroutineScope()
    val colors = PeekDetectorTheme.extendedColors

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings), color = colors.textOnGradient, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.textOnGradient)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { SectionHeader(title = stringResource(R.string.section_detection)) }
                item { SensitivitySelector(selected = sensitivity, onSelect = { scope.launch { repository.setSensitivityLevel(it) } }) }
                item { SectionHeader(title = stringResource(R.string.section_alerts)) }
                item { CooldownSlider(value = cooldown, onValueChange = { scope.launch { repository.setNotificationCooldown(it) } }) }
                item { SwitchPreference(title = stringResource(R.string.vibration), subtitle = stringResource(R.string.vibration_description), checked = vibrationEnabled, onCheckedChange = { scope.launch { repository.setVibrationEnabled(it) } }) }
                item { SectionHeader(title = stringResource(R.string.section_appearance)) }
                item {
                    val themeMode by repository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
                    ThemeSelector(selected = themeMode, onSelect = { scope.launch { repository.setThemeMode(it) } })
                }
                item { SectionHeader(title = stringResource(R.string.section_power)) }
                item {
                    val smartDetectionEnabled by repository.smartDetectionEnabled.collectAsState(initial = SettingsDefaults.SMART_DETECTION)
                    SwitchPreference(title = stringResource(R.string.smart_detection), subtitle = stringResource(R.string.smart_detection_description), checked = smartDetectionEnabled, onCheckedChange = { scope.launch { repository.setSmartDetectionEnabled(it) } })
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { ResetToDefaultsButton(onReset = { scope.launch { repository.resetToDefaults() } }) }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}


@Composable
fun SensitivitySelector(selected: SensitivityLevel, onSelect: (SensitivityLevel) -> Unit) {
    val colors = PeekDetectorTheme.extendedColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.detection_sensitivity), color = colors.textOnGradient, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            SensitivityLevel.entries.forEach { level ->
                SensitivityOption(level = level, isSelected = selected == level, onSelect = { onSelect(level) })
            }
        }
    }
}

@Composable
private fun SensitivityOption(level: SensitivityLevel, isSelected: Boolean, onSelect: () -> Unit) {
    val colors = PeekDetectorTheme.extendedColors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = colors.textOnGradient, unselectedColor = colors.textOnGradient.copy(alpha = 0.6f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = when (level) { SensitivityLevel.LOW -> stringResource(R.string.sensitivity_low); SensitivityLevel.MEDIUM -> stringResource(R.string.sensitivity_medium); SensitivityLevel.HIGH -> stringResource(R.string.sensitivity_high) },
                color = colors.textOnGradient,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = when (level) {
                    SensitivityLevel.LOW -> stringResource(R.string.sensitivity_low_desc)
                    SensitivityLevel.MEDIUM -> stringResource(R.string.sensitivity_medium_desc)
                    SensitivityLevel.HIGH -> stringResource(R.string.sensitivity_high_desc)
                },
                color = colors.textOnGradient.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun CooldownSlider(value: Int, onValueChange: (Int) -> Unit) {
    val colors = PeekDetectorTheme.extendedColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.notification_cooldown), color = colors.textOnGradient, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                Text(text = "${value}s", color = colors.textOnGradient, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.minimum_time_between_alerts), color = colors.textOnGradient.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = SettingsDefaults.COOLDOWN_MIN.toFloat()..SettingsDefaults.COOLDOWN_MAX.toFloat(),
                steps = SettingsDefaults.COOLDOWN_MAX - SettingsDefaults.COOLDOWN_MIN - 1,
                colors = SliderDefaults.colors(thumbColor = colors.textOnGradient, activeTrackColor = colors.textOnGradient, inactiveTrackColor = colors.textOnGradient.copy(alpha = 0.3f))
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "${SettingsDefaults.COOLDOWN_MIN}s", color = colors.textOnGradient.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                Text(text = "${SettingsDefaults.COOLDOWN_MAX}s", color = colors.textOnGradient.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun SwitchPreference(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = PeekDetectorTheme.extendedColors
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = colors.textOnGradient, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, color = colors.textOnGradient.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.textOnGradient,
                    checkedTrackColor = colors.textOnGradient.copy(alpha = 0.5f),
                    uncheckedThumbColor = colors.textOnGradient.copy(alpha = 0.7f),
                    uncheckedTrackColor = colors.textOnGradient.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = PeekDetectorTheme.extendedColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.theme), color = colors.textOnGradient, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            ThemeMode.entries.forEach { mode ->
                ThemeOption(mode = mode, isSelected = selected == mode, onSelect = { onSelect(mode) })
            }
        }
    }
}

@Composable
private fun ThemeOption(mode: ThemeMode, isSelected: Boolean, onSelect: () -> Unit) {
    val colors = PeekDetectorTheme.extendedColors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = colors.textOnGradient, unselectedColor = colors.textOnGradient.copy(alpha = 0.6f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = when (mode) { ThemeMode.SYSTEM -> stringResource(R.string.theme_system); ThemeMode.LIGHT -> stringResource(R.string.theme_light); ThemeMode.DARK -> stringResource(R.string.theme_dark) },
                color = colors.textOnGradient,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = when (mode) {
                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system_desc)
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light_desc)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark_desc)
                },
                color = colors.textOnGradient.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ResetToDefaultsButton(onReset: () -> Unit) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val colors = PeekDetectorTheme.extendedColors
    
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { showConfirmDialog = true }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.reset_to_defaults), color = colors.danger, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
        }
    }
    
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(text = stringResource(R.string.reset_settings_title), fontWeight = FontWeight.Bold) },
            text = { Text(text = stringResource(R.string.reset_settings_message)) },
            confirmButton = {
                TextButton(onClick = { onReset(); showConfirmDialog = false }) {
                    Text(text = stringResource(R.string.reset), color = colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text(text = stringResource(R.string.cancel)) }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
