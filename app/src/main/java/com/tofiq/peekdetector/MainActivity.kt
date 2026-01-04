package com.tofiq.peekdetector

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tofiq.peekdetector.data.local.AppDatabase
import com.tofiq.peekdetector.data.local.settingsDataStore
import com.tofiq.peekdetector.data.model.ThemeMode
import com.tofiq.peekdetector.data.repository.DetectionRepository
import com.tofiq.peekdetector.data.repository.SettingsRepositoryImpl
import com.tofiq.peekdetector.feature.detection.service.PeekDetectionService
import com.tofiq.peekdetector.feature.panic.service.PanicAlertService
import com.tofiq.peekdetector.feature.panic.ui.PanicAlertActiveUI
import com.tofiq.peekdetector.feature.panic.ui.SlideToAlertComponent
import com.tofiq.peekdetector.feature.report.export.ReportExportWorker
import com.tofiq.peekdetector.feature.report.ui.ReportActivity
import com.tofiq.peekdetector.feature.settings.ui.SettingsActivity
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scheduleReportExport()

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

                    Image(
                        painter = painterResource(id = R.drawable.pattern_overlay),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.05f),
                        contentScale = ContentScale.Crop
                    )

                    PeekAppScreen()
                }
            }
        }
    }

    private fun scheduleReportExport() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ReportExportWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            ReportExportWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

@Composable
fun PeekAppScreen() {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        }
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            !hasCameraPermission -> {
                PermissionRequestUI(cameraPermissionLauncher, context)
            }

            !hasNotificationPermission -> {
                NotificationPermissionRequestUI(
                    notificationPermissionLauncher,
                    onSkip = { hasNotificationPermission = true }
                )
            }

            else -> {
                MainContent()
            }
        }
    }
}

@Composable
private fun MainContent() {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        IconButton(
            onClick = {
                val intent = Intent(context, SettingsActivity::class.java)
                context.startActivity(intent)
            }
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    DetectionCounterCardStateful(context)

    Spacer(modifier = Modifier.height(24.dp))

    ServiceStatusStateful()

    Spacer(modifier = Modifier.height(32.dp))

    ControlButtonsStateful()

    Spacer(modifier = Modifier.height(16.dp))

    ViewReportsButton()

    Spacer(modifier = Modifier.height(24.dp))

    PanicAlertSectionStateful()
}

@Composable
private fun DetectionCounterCardStateful(context: Context) {
    val repository = remember {
        val database = AppDatabase.getDatabase(context)
        DetectionRepository(database.detectionEventDao())
    }
    val totalDetections by repository.getTotalDetectionsCount().collectAsState(initial = 0)
    
    DetectionCounterCard(
        totalDetections = totalDetections,
        context = context
    )
}

@Composable
private fun ServiceStatusStateful() {
    val isServiceRunning by PeekDetectionService.isRunning
    ServiceStatus(isServiceRunning = isServiceRunning)
}

@Composable
private fun ControlButtonsStateful() {
    val context = LocalContext.current
    val isServiceRunning by PeekDetectionService.isRunning
    ControlButtons(isServiceRunning = isServiceRunning, context = context)
}

@Composable
private fun ViewReportsButton() {
    val context = LocalContext.current
    Button(
        modifier = Modifier.fillMaxWidth(0.8f),
        onClick = {
            val intent = Intent(context, ReportActivity::class.java)
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF64B5F6)
        )
    ) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "Reports",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("View Reports", color = Color.White)
    }
}

@Composable
private fun PanicAlertSectionStateful() {
    val context = LocalContext.current
    val isPanicAlertActive by PanicAlertService.isActive
    
    AnimatedVisibility(visible = !isPanicAlertActive) {
        SlideToAlertComponent(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 8.dp),
            enabled = true,
            onAlertTriggered = {
                PanicAlertService.start(context)
            }
        )
    }
    
    AnimatedVisibility(visible = isPanicAlertActive) {
        PanicAlertActiveUI(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 8.dp),
            onStopClicked = {
                PanicAlertService.stop(context)
            }
        )
    }
}

@Composable
fun DetectionCounterCard(totalDetections: Int, context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Detections",
                fontSize = 16.sp,
                color = Color(0xFF0D47A1),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = totalDetections.toString(),
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (totalDetections == 0) Color(0xFF4CAF50) else Color(0xFFFF5252)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (totalDetections == 1) "Shoulder Surfer" else "Shoulder Surfers",
                fontSize = 14.sp,
                color = Color.Gray
            )

            if (totalDetections > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "🔒 Stay vigilant!",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Medium
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "✅ All clear!",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ServiceStatus(isServiceRunning: Boolean) {
    Text(
        text = "Service Status",
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(8.dp))
    val statusText = if (isServiceRunning) "Active" else "Inactive"
    val statusColor = if (isServiceRunning) Color(0xFF4CAF50) else Color(0xFFFF5252)

    Text(
        text = statusText,
        color = statusColor,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
}

@Composable
fun ControlButtons(isServiceRunning: Boolean, context: Context) {
    AnimatedVisibility(visible = !isServiceRunning) {
        Button(
            modifier = Modifier.fillMaxWidth(0.8f),
            onClick = {
                val intent = Intent(context, PeekDetectionService::class.java)
                context.startService(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Text("Start Protection", color = Color.White)
        }
    }

    AnimatedVisibility(visible = isServiceRunning) {
        Button(
            modifier = Modifier.fillMaxWidth(0.8f),
            onClick = {
                val intent = Intent(context, PeekDetectionService::class.java)
                context.stopService(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
        ) {
            Text("Stop Protection", color = Color.White)
        }
    }
}

@Composable
fun PermissionRequestUI(launcher: ActivityResultLauncher<String>, context: Context) {
    Text(
        "Camera Permission Required",
        style = MaterialTheme.typography.headlineSmall,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "This app needs camera access to detect faces.",
        textAlign = TextAlign.Center,
        color = Color.White.copy(alpha = 0.9f)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = {
            launcher.launch(Manifest.permission.CAMERA)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF64B5F6)
        )
    ) {
        Text("Grant Camera Permission", color = Color.White)
    }
    
    val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
    if (!canDrawOverlays) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "For screen overlay alerts to work, please grant the 'Draw over other apps' permission.",
            textAlign = TextAlign.Center,
            color = Color(0xFFFF5252)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF5252)
            )
        ) {
            Text("Open Settings", color = Color.White)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun NotificationPermissionRequestUI(
    launcher: ActivityResultLauncher<String>,
    onSkip: () -> Unit
) {
    Text(
        "Notification Permission Required",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "This app needs notification permission to alert you when multiple faces are detected.",
        textAlign = TextAlign.Center,
        color = Color.White.copy(alpha = 0.9f)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF64B5F6)
        )
    ) {
        Text("Grant Notification Permission", color = Color.White)
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onSkip) {
        Text("Skip (Can enable later in settings)", color = Color.White.copy(alpha = 0.7f))
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PeekDetectorTheme {
        PeekAppScreen()
    }
}
