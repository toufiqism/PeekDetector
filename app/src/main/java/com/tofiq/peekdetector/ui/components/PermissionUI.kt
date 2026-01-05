package com.tofiq.peekdetector.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme

/**
 * Reusable permission request card.
 * Provides consistent UI for requesting various permissions throughout the app.
 */
@Composable
fun PermissionRequestCard(
    icon: String,
    title: String,
    description: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    additionalContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    val colors = PeekDetectorTheme.extendedColors
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(24.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textOnGradient,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textOnGradient.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(
            text = primaryButtonText,
            onClick = onPrimaryClick,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        
        if (secondaryButtonText != null && onSecondaryClick != null) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onSecondaryClick) {
                Text(
                    text = secondaryButtonText,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textOnGradient.copy(alpha = 0.6f)
                )
            }
        }
        
        additionalContent?.invoke(this)
    }
}

/**
 * Permission warning banner for inline permission requests.
 * Use when you need to show a permission warning within existing content.
 */
@Composable
fun PermissionWarningBanner(
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PeekDetectorTheme.extendedColors
    
    GlassCard(modifier = modifier.fillMaxWidth(), alpha = 0.15f) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.warning,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textOnGradient.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onButtonClick) {
                Text(
                    text = buttonText,
                    color = colors.warning
                )
            }
        }
    }
}
