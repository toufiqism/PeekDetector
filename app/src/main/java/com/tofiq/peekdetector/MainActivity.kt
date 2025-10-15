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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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

    val permissionsToRequest = remember {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.toTypedArray()
    }

    var hasAllPermissions by remember {
        mutableStateOf(
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            // Check if all requested permissions were granted
            hasAllPermissions = permissionsMap.values.all { it }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (hasAllPermissions) {
            ServiceStatus(isServiceRunning)
            Spacer(modifier = Modifier.height(32.dp))
            ControlButtons(isServiceRunning, context)
        } else {
            PermissionRequestUI(
                onGrantClick = { permissionLauncher.launch(permissionsToRequest) },context
            )
        }
    }
}

@Composable
fun ControlButtons(context: Context) {
    Text(
        "Service is ready. Start protection to monitor in the background.",
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = {
            val intent = Intent(context, PeekDetectionService::class.java)
            context.startService(intent)
        }) {
            Text("Start Protection")
        }
        Button(
            onClick = {
                val intent = Intent(context, PeekDetectionService::class.java)
                context.stopService(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Stop Protection")
        }
    }
}

@Composable
fun ServiceStatus(isServiceRunning: Boolean) {
    Text(
        text = "Service Status",
        style = MaterialTheme.typography.headlineSmall
    )
    Spacer(modifier = Modifier.height(8.dp))
    val statusText = if (isServiceRunning) "Active" else "Inactive"
    val statusColor = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

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
            }) {
            Text("Start Protection")
        }
    }

    AnimatedVisibility(visible = isServiceRunning) {
        Button(
            modifier = Modifier.fillMaxWidth(0.8f),
            onClick = {
                val intent = Intent(context, PeekDetectionService::class.java)
                context.stopService(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Stop Protection")
        }
    }
}

@Composable
fun PermissionRequestUI(onGrantClick: () -> Unit, context: Context) {
    Text("Camera Permission Required", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(8.dp))
    Text("This app needs camera access to detect faces.", textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = {
       onGrantClick
    }) {
        Text("Grant Permission")
    }
    // Check for "Draw over other apps" permission
    val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
    if (!canDrawOverlays) {
        Text(
            text = "For alerts to work, please grant the 'Draw over other apps' permission.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }) {
            Text("Open Settings")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PeekDetectorTheme {
        PeekAppScreen()
    }
}
