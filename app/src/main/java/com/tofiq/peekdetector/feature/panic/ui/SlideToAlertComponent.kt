package com.tofiq.peekdetector.feature.panic.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.tofiq.peekdetector.feature.panic.PanicAlertConstants
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme
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
    val colors = PeekDetectorTheme.extendedColors
    
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    
    val thumbSizeDp = 56.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    
    val dragOffset = remember { Animatable(0f) }
    
    val maxDragDistance = (trackSize.width - thumbSizePx).coerceAtLeast(0f)
    
    val progress = if (maxDragDistance > 0) {
        (dragOffset.value / maxDragDistance).coerceIn(0f, 1f)
    } else {
        0f
    }
    
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
            .background(colors.surfaceElevated.copy(alpha = 0.15f))
            .onSizeChanged { trackSize = it },
        contentAlignment = Alignment.CenterStart
    ) {
        // Progress fill background
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(RoundedCornerShape(36.dp))
                .background(
                    colors.danger.copy(alpha = 0.3f + (progress * 0.4f))
                )
        )
        
        // Instructional text
        Text(
            text = "Slide to Alert →",
            color = colors.textOnGradient.copy(alpha = 0.7f),
            style = MaterialTheme.typography.titleMedium,
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
                        colors.danger
                    } else {
                        colors.danger.copy(alpha = 0.7f)
                    }
                )
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    onDragStopped = {
                        coroutineScope.launch {
                            if (progress >= PanicAlertConstants.SWIPE_THRESHOLD) {
                                onAlertTriggered()
                                dragOffset.animateTo(0f, animationSpec = tween(300))
                            } else {
                                dragOffset.animateTo(0f, animationSpec = tween(300))
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Slide to alert",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Calculates the swipe progress as a percentage of the track width.
 * Exposed for testing purposes.
 */
fun calculateSwipeProgress(dragOffset: Float, maxDragDistance: Float): Float {
    if (maxDragDistance <= 0) return 0f
    return (dragOffset / maxDragDistance).coerceIn(0f, 1f)
}

/**
 * Determines if the swipe should trigger the alert based on progress.
 * Exposed for testing purposes.
 */
fun shouldTriggerAlert(progress: Float): Boolean {
    return progress >= PanicAlertConstants.SWIPE_THRESHOLD
}

/**
 * Determines if the slider should reset based on progress.
 * Exposed for testing purposes.
 */
fun shouldResetSlider(progress: Float): Boolean {
    return progress < PanicAlertConstants.SWIPE_THRESHOLD
}
