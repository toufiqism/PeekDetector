package com.tofiq.peekdetector.ui.components

/**
 * This file re-exports all UI components for backward compatibility.
 * Components have been separated into individual files for better reusability:
 * 
 * - Buttons.kt: AppButton, ButtonStyle
 * - Cards.kt: StyledCard, CardStyle, GradientBackground
 * - Dialogs.kt: ConfirmationDialog
 * - EmptyState.kt: EmptyStateDisplay
 * - PermissionUI.kt: PermissionRequestCard, PermissionWarningBanner
 * - Preferences.kt: PreferenceItem, SwitchPreferenceItem, RadioOptionItem, SliderPreferenceItem
 * - Statistics.kt: StatusBadge, CounterDisplay, StatItem, LargeStatDisplay, SectionHeader
 * 
 * Legacy components (PrimaryButton, SuccessButton, DangerButton, AppCard, GlassCard)
 * are kept below for backward compatibility but delegate to the new unified components.
 */

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * @deprecated Use AppButton with ButtonStyle.PRIMARY instead
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    AppButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = ButtonStyle.PRIMARY,
        enabled = enabled,
        icon = icon
    )
}

/**
 * @deprecated Use AppButton with ButtonStyle.SUCCESS instead
 */
@Composable
fun SuccessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    AppButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = ButtonStyle.SUCCESS,
        enabled = enabled,
        icon = icon
    )
}

/**
 * @deprecated Use AppButton with ButtonStyle.DANGER instead
 */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    AppButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = ButtonStyle.DANGER,
        enabled = enabled,
        icon = icon
    )
}

/**
 * @deprecated Use StyledCard with CardStyle.ELEVATED instead
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    StyledCard(
        modifier = modifier,
        style = CardStyle.ELEVATED,
        elevation = elevation,
        content = content
    )
}

/**
 * @deprecated Use StyledCard with CardStyle.GLASS instead
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    alpha: Float = 0.12f,
    content: @Composable ColumnScope.() -> Unit
) {
    StyledCard(
        modifier = modifier,
        style = CardStyle.GLASS,
        glassAlpha = alpha,
        content = content
    )
}
