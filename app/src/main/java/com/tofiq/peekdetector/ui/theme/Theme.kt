package com.tofiq.peekdetector.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light color scheme
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

// Dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

// Extended colors for app-specific needs
data class ExtendedColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val gradientStart: Color,
    val gradientMid: Color,
    val gradientEnd: Color,
    val cardBackground: Color,
    val surfaceElevated: Color,
    val textOnGradient: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = SuccessLight,
        warning = WarningLight,
        danger = DangerLight,
        gradientStart = GradientStartLight,
        gradientMid = GradientMidLight,
        gradientEnd = GradientEndLight,
        cardBackground = CardBackgroundLight,
        surfaceElevated = SurfaceElevatedLight,
        textOnGradient = Color.White
    )
}

private val LightExtendedColors = ExtendedColors(
    success = SuccessLight,
    warning = WarningLight,
    danger = DangerLight,
    gradientStart = GradientStartLight,
    gradientMid = GradientMidLight,
    gradientEnd = GradientEndLight,
    cardBackground = CardBackgroundLight,
    surfaceElevated = SurfaceElevatedLight,
    textOnGradient = Color.White
)

private val DarkExtendedColors = ExtendedColors(
    success = SuccessDark,
    warning = WarningDark,
    danger = DangerDark,
    gradientStart = GradientStartDark,
    gradientMid = GradientMidDark,
    gradientEnd = GradientEndDark,
    cardBackground = CardBackgroundDark,
    surfaceElevated = SurfaceElevatedDark,
    textOnGradient = Color.White
)

@Composable
fun PeekDetectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Extension property for easy access to extended colors
object PeekDetectorTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}
