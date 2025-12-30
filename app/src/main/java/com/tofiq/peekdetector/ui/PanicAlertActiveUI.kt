package com.tofiq.peekdetector.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * UI component displayed when the panic alert is active.
 * Shows a pulsing red background, "ALERT ACTIVE" text, and a prominent stop button.
 * 
 * Requirements:
 * - 3.1: Display a prominent stop button while alert is active
 * - 4.2: Display a pulsing or animated visual indicator while alert is active
 * 
 * @param onStopClicked Callback invoked when the stop button is clicked
 * @param modifier Modifier for the component
 */
@Composable
fun PanicAlertActiveUI(
    onStopClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulsing animation for the background (Requirement 4.2)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFF5252).copy(alpha = pulseAlpha))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "ALERT ACTIVE" text
            Text(
                text = "ALERT ACTIVE",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Prominent stop button (Requirement 3.1)
            Button(
                onClick = onStopClicked,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFFF5252)
                ),
                shape = CircleShape
            ) {
                Text(
                    text = "STOP",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Determines which UI component should be visible based on alert state.
 * This is exposed for testing purposes.
 * 
 * @param isAlertActive Whether the panic alert is currently active
 * @return A pair of (sliderVisible, stopButtonVisible)
 */
fun getUIVisibility(isAlertActive: Boolean): Pair<Boolean, Boolean> {
    return if (isAlertActive) {
        Pair(false, true) // Slider hidden, stop button visible
    } else {
        Pair(true, false) // Slider visible, stop button hidden
    }
}

/**
 * Determines if the slider should be visible based on alert state.
 * This is exposed for testing purposes.
 * 
 * @param isAlertActive Whether the panic alert is currently active
 * @return true if slider should be visible, false otherwise
 */
fun isSliderVisible(isAlertActive: Boolean): Boolean {
    return !isAlertActive
}

/**
 * Determines if the stop button should be visible based on alert state.
 * This is exposed for testing purposes.
 * 
 * @param isAlertActive Whether the panic alert is currently active
 * @return true if stop button should be visible, false otherwise
 */
fun isStopButtonVisible(isAlertActive: Boolean): Boolean {
    return isAlertActive
}
