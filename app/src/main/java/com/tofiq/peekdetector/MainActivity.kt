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
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme // Change to your theme name

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PeekDetectorTheme {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Background gradient
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
                    
                    // Pattern overlay
                    Image(
                        painter = painterResource(id = R.drawable.pattern_overlay),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.05f),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Main content
                    PeekAppScreen()
                }
            }
        }
    }
}

@Composable
fun PeekAppScreen() {
    // This is the state we will observe from the Service
    val isServiceRunning by PeekDetectionService.isRunning

    val context = LocalContext.current
    // ... (The permission handling logic remains exactly the same)
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Notification permission state (for Android 13+)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Not required for older versions
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
                // Camera permission request UI
                PermissionRequestUI(cameraPermissionLauncher, context)
            }
            !hasNotificationPermission -> {
                // Notification permission request UI
                NotificationPermissionRequestUI(
                    notificationPermissionLauncher,
                    onSkip = { hasNotificationPermission = true }
                )
            }
            else -> {
                // Both permissions granted - show main UI
                ServiceStatus(isServiceRunning)
                Spacer(modifier = Modifier.height(32.dp))
                ControlButtons(isServiceRunning, context)
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
    // AnimatedVisibility provides a nice fade-in/out effect
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
    // Check for "Draw over other apps" permission
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
