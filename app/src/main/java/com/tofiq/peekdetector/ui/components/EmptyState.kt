package com.tofiq.peekdetector.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme

/**
 * Empty state display for lists and content areas.
 * Use when there's no data to display.
 */
@Composable
fun EmptyStateDisplay(
    icon: String,
    message: String,
    modifier: Modifier = Modifier,
    submessage: String? = null
) {
    val colors = PeekDetectorTheme.extendedColors
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = icon,
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textOnGradient.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            if (submessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = submessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textOnGradient.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
