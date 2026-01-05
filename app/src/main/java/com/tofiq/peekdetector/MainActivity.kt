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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tofiq.peekdetector.core.util.CrashlyticsHelper
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
import com.tofiq.peekdetector.ui.components.*
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
                GradientBackground {
                    Image(
                        painter = painterResource(id = R.drawable.pattern_overlay),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().alpha(0.03f),
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
        ).setConstraints(constraints).build()
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
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasCameraPermission = isGranted }
    )
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasNotificationPermission = isGranted }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            !hasCameraPermission -> PermissionRequestUI(cameraPermissionLauncher, context)
            !hasNotificationPermission -> NotificationPermissionRequestUI(notificationPermissionLauncher, onSkip = { hasNotificationPermission = true })
            else -> MainContent()
        }
    }
}

@Composable
private fun MainContent() {
    val context = LocalContext.current
    val colors = PeekDetectorTheme.extendedColors

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = colors.textOnGradient, modifier = Modifier.size(28.dp))
            }
        }
        DetectionCounterCardStateful(context)
        Spacer(modifier = Modifier.height(32.dp))
        ServiceStatusStateful()
        Spacer(modifier = Modifier.height(32.dp))
        ControlButtonsStateful()
        Spacer(modifier = Modifier.height(16.dp))
        ViewReportsButton()
        Spacer(modifier = Modifier.height(16.dp))
        PanicAlertSectionStateful()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetectionCounterCardStateful(context: Context) {
    val repository = remember {
        val database = AppDatabase.getDatabase(context)
        DetectionRepository(database.detectionEventDao())
    }
    val totalDetections by repository.getTotalDetectionsCount().collectAsState(initial = 0)
    DetectionCounterCard(totalDetections = totalDetections)
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
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(0.85f).height(52.dp),
        onClick = { context.startActivity(Intent(context, ReportActivity::class.java)) },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.5f))
        )
    ) {
        Icon(Icons.Outlined.MoreVert, contentDescription = "Reports", modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("View Reports", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PanicAlertSectionStateful() {
    val context = LocalContext.current
    val isPanicAlertActive by PanicAlertService.isActive
    AnimatedVisibility(visible = !isPanicAlertActive, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
        SlideToAlertComponent(
            modifier = Modifier.fillMaxWidth(0.9f).padding(vertical = 8.dp),
            enabled = true,
            onAlertTriggered = { PanicAlertService.start(context) }
        )
    }
    AnimatedVisibility(visible = isPanicAlertActive, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
        PanicAlertActiveUI(
            modifier = Modifier.fillMaxWidth(0.9f).padding(vertical = 8.dp),
            onStopClicked = { PanicAlertService.stop(context) }
        )
    }
}


@Composable
fun DetectionCounterCard(totalDetections: Int) {
    val colors = PeekDetectorTheme.extendedColors
    AppCard(modifier = Modifier.fillMaxWidth(0.9f).padding(vertical = 8.dp), elevation = 8.dp) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Total Detections", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = totalDetections.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (totalDetections == 0) colors.success else colors.danger
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (totalDetections == 1) "Shoulder Surfer" else "Shoulder Surfers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth(0.8f), color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))
            if (totalDetections > 0) {
                StatusBadge(text = "Stay vigilant!", isActive = false)
            } else {
                StatusBadge(text = "All clear!", isActive = true)
            }
        }
    }
}

@Composable
fun ServiceStatus(isServiceRunning: Boolean) {
    val colors = PeekDetectorTheme.extendedColors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Protection Status", style = MaterialTheme.typography.titleMedium, color = colors.textOnGradient.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(12.dp))
        StatusBadge(text = if (isServiceRunning) "Active" else "Inactive", isActive = isServiceRunning)
    }
}

@Composable
fun ControlButtons(isServiceRunning: Boolean, context: Context) {
    AnimatedVisibility(visible = !isServiceRunning, enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut() + slideOutVertically { -it / 2 }) {
        SuccessButton(
            text = "Start Protection",
            onClick = { context.startService(Intent(context, PeekDetectionService::class.java)) },
            modifier = Modifier.fillMaxWidth(0.85f),
            icon = Icons.Default.PlayArrow
        )
    }
    AnimatedVisibility(visible = isServiceRunning, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
        DangerButton(
            text = "Stop Protection",
            onClick = { context.stopService(Intent(context, PeekDetectionService::class.java)) },
            modifier = Modifier.fillMaxWidth(0.85f)
        )
    }
}

@Composable
fun PermissionRequestUI(launcher: ActivityResultLauncher<String>, context: Context) {
    val colors = PeekDetectorTheme.extendedColors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Text(text = "📷", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Camera Permission Required", style = MaterialTheme.typography.headlineSmall, color = colors.textOnGradient, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text("This app needs camera access to detect faces and protect your privacy.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge, color = colors.textOnGradient.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(text = "Grant Camera Permission", onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth(0.85f))
        
        val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
        if (!canDrawOverlays) {
            Spacer(modifier = Modifier.height(24.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(0.9f), alpha = 0.15f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⚠️ Overlay Permission", style = MaterialTheme.typography.titleMedium, color = colors.warning, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "For screen overlay alerts to work, please grant the 'Draw over other apps' permission.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = colors.textOnGradient.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(16.dp))
                    DangerButton(
                        text = "Open Settings",
                        onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationPermissionRequestUI(launcher: ActivityResultLauncher<String>, onSkip: () -> Unit) {
    val colors = PeekDetectorTheme.extendedColors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Text(text = "🔔", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Notification Permission", style = MaterialTheme.typography.headlineSmall, color = colors.textOnGradient, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Enable notifications to receive alerts when multiple faces are detected.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge, color = colors.textOnGradient.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(
            text = "Enable Notifications",
            onClick = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onSkip) {
            Text("Skip for now", style = MaterialTheme.typography.labelLarge, color = colors.textOnGradient.copy(alpha = 0.6f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PeekDetectorTheme {
        GradientBackground { PeekAppScreen() }
    }
}
