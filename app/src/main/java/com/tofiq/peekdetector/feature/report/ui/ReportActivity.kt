package com.tofiq.peekdetector.feature.report.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tofiq.peekdetector.R
import com.tofiq.peekdetector.data.local.AppDatabase
import com.tofiq.peekdetector.data.local.settingsDataStore
import com.tofiq.peekdetector.data.model.DetectionEvent
import com.tofiq.peekdetector.data.model.ThemeMode
import com.tofiq.peekdetector.data.repository.DetectionRepository
import com.tofiq.peekdetector.data.repository.SettingsRepositoryImpl
import com.tofiq.peekdetector.ui.components.AppCard
import com.tofiq.peekdetector.ui.components.GradientBackground
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReportActivity : ComponentActivity() {
    private lateinit var repository: DetectionRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        repository = DetectionRepository(database.detectionEventDao())
        
        setContent {
            val settingsRepository = remember { SettingsRepositoryImpl(applicationContext.settingsDataStore) }
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PeekDetectorTheme(darkTheme = darkTheme) {
                ReportScreen(repository = repository, onBackPressed = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(repository: DetectionRepository, onBackPressed: () -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(R.string.tab_weekly, R.string.tab_monthly, R.string.tab_yearly)
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val colors = PeekDetectorTheme.extendedColors

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.detection_reports), color = colors.textOnGradient, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.textOnGradient)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear_all_data), tint = colors.textOnGradient)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = colors.gradientMid.copy(alpha = 0.5f),
                    contentColor = colors.textOnGradient,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, titleRes ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(stringResource(titleRes), fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
                when (selectedTabIndex) {
                    0 -> WeeklyDetectionContent(repository = repository)
                    1 -> MonthlyDetectionContent(repository = repository)
                    2 -> YearlyDetectionContent(repository = repository)
                }
            }
        }
        
        DeleteConfirmationDialog(
            showDialog = showDeleteDialog,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { scope.launch { repository.deleteAllDetections(); showDeleteDialog = false } }
        )
    }
}

@Composable
private fun WeeklyDetectionContent(repository: DetectionRepository) {
    val weeklyDetections by repository.getWeeklyDetections().collectAsState(initial = emptyList())
    DetectionListContent(title = stringResource(R.string.this_week), detections = weeklyDetections, emptyMessage = stringResource(R.string.no_detections_this_week))
}

@Composable
private fun MonthlyDetectionContent(repository: DetectionRepository) {
    val monthlyDetections by repository.getMonthlyDetections().collectAsState(initial = emptyList())
    DetectionListContent(title = stringResource(R.string.this_month), detections = monthlyDetections, emptyMessage = stringResource(R.string.no_detections_this_month))
}

@Composable
private fun YearlyDetectionContent(repository: DetectionRepository) {
    val yearlyDetections by repository.getYearlyDetections().collectAsState(initial = emptyList())
    DetectionListContent(title = stringResource(R.string.this_year), detections = yearlyDetections, emptyMessage = stringResource(R.string.no_detections_this_year))
}

@Composable
private fun DeleteConfirmationDialog(showDialog: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colors = PeekDetectorTheme.extendedColors
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.clear_all_data_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.clear_all_data_message)) },
            confirmButton = {
                TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = colors.danger)) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            shape = RoundedCornerShape(16.dp)
        )
    }
}


@Composable
fun DetectionListContent(title: String, detections: List<DetectionEvent>, emptyMessage: String) {
    val colors = PeekDetectorTheme.extendedColors
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        AppCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), elevation = 6.dp) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${detections.size}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (detections.isEmpty()) colors.success else colors.danger
                )
                Text(text = if (detections.size == 1) stringResource(R.string.detection) else stringResource(R.string.detections), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                if (detections.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(label = stringResource(R.string.total_faces), value = detections.sumOf { it.faceCount }.toString())
                        StatItem(label = stringResource(R.string.avg_faces), value = String.format("%.1f", detections.map { it.faceCount }.average()))
                        StatItem(label = stringResource(R.string.max_faces), value = (detections.maxOfOrNull { it.faceCount } ?: 0).toString())
                    }
                }
            }
        }
        
        Text(text = stringResource(R.string.detection_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textOnGradient, modifier = Modifier.padding(bottom = 12.dp))
        
        if (detections.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🎉", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge, color = colors.textOnGradient.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(R.string.keep_privacy_protected), style = MaterialTheme.typography.bodyMedium, color = colors.textOnGradient.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(detections) { detection -> DetectionCard(detection) }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DetectionCard(detection: DetectionEvent) {
    val colors = PeekDetectorTheme.extendedColors
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(detection.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = formattedDate, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.faces_detected, detection.faceCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                    detection.faceCount == 2 -> colors.warning
                    detection.faceCount >= 3 -> colors.danger
                    else -> colors.success
                }
            ) {
                Text(
                    text = when {
                        detection.faceCount == 2 -> "⚠️"
                        detection.faceCount >= 3 -> "🚨"
                        else -> "✓"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}
