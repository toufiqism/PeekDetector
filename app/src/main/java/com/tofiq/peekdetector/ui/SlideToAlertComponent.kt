package com.tofiq.peekdetector.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tofiq.peekdetector.PanicAlertConstants
import kotlinx.coroutines.launch

/**
 * Slide-to-alert component that allows users to trigger a panic alert by sliding.
 * 
 * Requirements:
 * - 1.1: Display a clearly visible slider element on the main screen
 * - 1.2: Activate siren when swipe passes SWIPE_THRESHOLD (80%)
 * - 1.3: Animate slider back to start when released below threshold
 * - 1.5: Provide visual feedback showing progress toward activation
 *
 * @param modifier Modifier for the component
 * @param enabled Whether the slider is enabled
 * @param onAlertTriggered Callback invoked when the alert is triggered
 */
@Composable
fun SlideToAlertComponent(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onAlertTriggered: () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    
    // Track the size of the track for calculating progress
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Thumb size in pixels
    val thumbSizeDp = 56.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    
    // Current drag offset (animated)
    val dragOffset = remember { Animatable(0f) }
    
    // Calculate the maximum drag distance (track width minus thumb width)
    val maxDragDistance = (trackSize.width - thumbSizePx).coerceAtLeast(0f)
    
    // Calculate progress as percentage (0.0 to 1.0)
    val progress = if (maxDragDistance > 0) {
        (dragOffset.value / maxDragDistance).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    // Draggable state
    val draggableState = rememberDraggableState { delta ->
        if (enabled) {
            coroutineScope.launch {
                val newOffset = (dragOffset.value + delta).coerceIn(0f, maxDragDistance)
                dragOffset.snapTo(newOffset)
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(
                // Progress indicator: fill from left based on progress
                Color(0xFF2D2D2D)
            )
            .onSizeChanged { trackSize = it },
        contentAlignment = Alignment.CenterStart
    ) {
        // Progress fill background (Requirement 1.5: visual feedback)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Color(0xFFFF5252).copy(alpha = 0.3f + (progress * 0.4f))
                )
        )
        
        // Instructional text (Requirement 4.1)
        Text(
            text = "Slide to Alert →",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 60.dp)
        )
        
        // Draggable thumb
        Box(
            modifier = Modifier
                .offset(x = with(density) { dragOffset.value.toDp() })
                .padding(8.dp)
                .size(thumbSizeDp)
                .clip(CircleShape)
                .background(
                    if (progress >= PanicAlertConstants.SWIPE_THRESHOLD) {
                        Color(0xFFFF5252) // Red when threshold reached
                    } else {
                        Color(0xFFFF8A80) // Lighter red during drag
                    }
                )
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    onDragStopped = {
                        coroutineScope.launch {
                            if (progress >= PanicAlertConstants.SWIPE_THRESHOLD) {
                                // Threshold reached - trigger alert (Requirement 1.2)
                                onAlertTriggered()
                                // Reset slider after triggering
                                dragOffset.animateTo(0f, animationSpec = tween(300))
                            } else {
                                // Below threshold - animate back to start (Requirement 1.3)
                                dragOffset.animateTo(0f, animationSpec = tween(300))
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Slide to alert",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Calculates the swipe progress as a percentage of the track width.
 * This is exposed for testing purposes.
 * 
 * @param dragOffset Current drag offset in pixels
 * @param maxDragDistance Maximum drag distance in pixels
 * @return Progress as a float between 0.0 and 1.0
 */
fun calculateSwipeProgress(dragOffset: Float, maxDragDistance: Float): Float {
    if (maxDragDistance <= 0) return 0f
    return (dragOffset / maxDragDistance).coerceIn(0f, 1f)
}

/**
 * Determines if the swipe should trigger the alert based on progress.
 * This is exposed for testing purposes.
 * 
 * @param progress Current swipe progress (0.0 to 1.0)
 * @return true if progress >= SWIPE_THRESHOLD, false otherwise
 */
fun shouldTriggerAlert(progress: Float): Boolean {
    return progress >= PanicAlertConstants.SWIPE_THRESHOLD
}

/**
 * Determines if the slider should reset based on progress.
 * This is exposed for testing purposes.
 * 
 * @param progress Current swipe progress (0.0 to 1.0)
 * @return true if progress < SWIPE_THRESHOLD, false otherwise
 */
fun shouldResetSlider(progress: Float): Boolean {
    return progress < PanicAlertConstants.SWIPE_THRESHOLD
}
