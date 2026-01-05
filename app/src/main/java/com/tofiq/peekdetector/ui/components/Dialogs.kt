package com.tofiq.peekdetector.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme

/**
 * Reusable confirmation dialog for destructive actions.
 * Provides consistent styling for confirm/cancel dialogs throughout the app.
 */
@Composable
fun ConfirmationDialog(
    showDialog: Boolean,
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDangerous: Boolean = true
) {
    if (showDialog) {
        val colors = PeekDetectorTheme.extendedColors
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = message)
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                    colors = if (isDangerous) {
                        ButtonDefaults.textButtonColors(contentColor = colors.danger)
                    } else {
                        ButtonDefaults.textButtonColors()
                    }
                ) {
                    Text(text = confirmText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = cancelText)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
