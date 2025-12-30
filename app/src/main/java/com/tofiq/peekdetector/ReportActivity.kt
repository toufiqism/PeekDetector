package com.tofiq.peekdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tofiq.peekdetector.data.AppDatabase
import com.tofiq.peekdetector.data.DetectionEvent
import com.tofiq.peekdetector.data.DetectionRepository
import com.tofiq.peekdetector.data.SettingsRepositoryImpl
import com.tofiq.peekdetector.data.ThemeMode
import com.tofiq.peekdetector.data.settingsDataStore
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Report Activity showing detection statistics
 * Displays weekly, monthly, and yearly reports
 */
class ReportActivity : ComponentActivity() {
    
    private lateinit var repository: DetectionRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize repository
        val database = AppDatabase.getDatabase(this)
        repository = DetectionRepository(database.detectionEventDao())
        
        setContent {
            val settingsRepository = remember {
                SettingsRepositoryImpl(applicationContext.settingsDataStore)
            }
            
            // Collect theme mode to apply correct theme
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            
            // Determine if dark theme should be used based on setting
            // Requirements: 5.2, 5.3, 5.4, 5.6
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            
            PeekDetectorTheme(darkTheme = darkTheme) {
                ReportScreen(
                    repository = repository,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    repository: DetectionRepository,
    onBackPressed: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Weekly", "Monthly", "Yearly")
    
    val scope = rememberCoroutineScope()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background gradient (same as MainActivity)
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
                            "Detection Reports",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear All Data",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab Row - pass lambda to defer state read
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF1565C0),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                    }
                }
                
                // Content based on selected tab - only collect data for active tab
                // This defers state collection to the leaf composables
                when (selectedTabIndex) {
                    0 -> WeeklyDetectionContent(repository = repository)
                    1 -> MonthlyDetectionContent(repository = repository)
                    2 -> YearlyDetectionContent(repository = repository)
                }
            }
        }
        
        // Delete confirmation dialog
        DeleteConfirmationDialog(
            showDialog = showDeleteDialog,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                scope.launch {
                    repository.deleteAllDetections()
                    showDeleteDialog = false
                }
            }
        )
    }
}

/**
 * Stateful composable that only collects weekly data when this tab is active
 * Defers state read to prevent unnecessary recomposition of other tabs
 */
@Composable
private fun WeeklyDetectionContent(repository: DetectionRepository) {
    val weeklyDetections by repository.getWeeklyDetections().collectAsState(initial = emptyList())
    DetectionListContent(
        title = "This Week",
        detections = weeklyDetections,
        emptyMessage = "No detections this week"
    )
}

/**
 * Stateful composable that only collects monthly data when this tab is active
 */
@Composable
private fun MonthlyDetectionContent(repository: DetectionRepository) {
    val monthlyDetections by repository.getMonthlyDetections().collectAsState(initial = emptyList())
    DetectionListContent(
        title = "This Month",
        detections = monthlyDetections,
        emptyMessage = "No detections this month"
    )
}

/**
 * Stateful composable that only collects yearly data when this tab is active
 */
@Composable
private fun YearlyDetectionContent(repository: DetectionRepository) {
    val yearlyDetections by repository.getYearlyDetections().collectAsState(initial = emptyList())
    DetectionListContent(
        title = "This Year",
        detections = yearlyDetections,
        emptyMessage = "No detections this year"
    )
}

/**
 * Extracted dialog composable for better recomposition isolation
 */
@Composable
private fun DeleteConfirmationDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Clear All Data") },
            text = { Text("Are you sure you want to delete all detection records? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF5252)
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetectionListContent(
    title: String,
    detections: List<DetectionEvent>,
    emptyMessage: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D47A1)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${detections.size}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (detections.isEmpty()) Color(0xFF4CAF50) else Color(0xFFFF5252)
                )
                Text(
                    text = if (detections.size == 1) "Detection" else "Detections",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                
                // Additional stats
                if (detections.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Total Faces",
                            value = detections.sumOf { it.faceCount }.toString()
                        )
                        StatItem(
                            label = "Avg Faces",
                            value = String.format("%.1f", detections.map { it.faceCount }.average())
                        )
                        StatItem(
                            label = "Max Faces",
                            value = (detections.maxOfOrNull { it.faceCount } ?: 0).toString()
                        )
                    }
                }
            }
        }
        
        // Detection List
        Text(
            text = "Detection History",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (detections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = emptyMessage,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keep your privacy protected!",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(detections) { detection ->
                    DetectionCard(detection)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1565C0)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun DetectionCard(detection: DetectionEvent) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(detection.timestamp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
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
                    text = formattedDate,
                    fontSize = 14.sp,
                    color = Color(0xFF0D47A1),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${detection.faceCount} faces detected",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // Alert indicator
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                    detection.faceCount == 2 -> Color(0xFFFF9800)
                    detection.faceCount >= 3 -> Color(0xFFFF5252)
                    else -> Color(0xFF4CAF50)
                }
            ) {
                Text(
                    text = when {
                        detection.faceCount == 2 -> "⚠️"
                        detection.faceCount >= 3 -> "🚨"
                        else -> "✓"
                    },
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

