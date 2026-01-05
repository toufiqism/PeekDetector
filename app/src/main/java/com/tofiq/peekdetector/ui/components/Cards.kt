package com.tofiq.peekdetector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tofiq.peekdetector.ui.theme.PeekDetectorTheme

/**
 * Card style variants
 */
enum class CardStyle {
    ELEVATED,
    GLASS
}

/**
 * Unified card component that supports elevated and glass styles.
 * Use ELEVATED for solid cards, GLASS for translucent cards on gradient backgrounds.
 */
@Composable
fun StyledCard(
    modifier: Modifier = Modifier,
    style: CardStyle = CardStyle.ELEVATED,
    elevation: Dp = 4.dp,
    glassAlpha: Float = 0.12f,
    content: @Composable ColumnScope.() -> Unit
) {
    when (style) {
        CardStyle.ELEVATED -> ElevatedStyledCard(modifier, elevation, content)
        CardStyle.GLASS -> GlassStyledCard(modifier, glassAlpha, content)
    }
}

@Composable
private fun ElevatedStyledCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = PeekDetectorTheme.extendedColors
    Card(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun GlassStyledCard(
    modifier: Modifier = Modifier,
    alpha: Float = 0.12f,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = alpha)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Gradient background that adapts to theme.
 * Use as a wrapper for screens that need the app's gradient background.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = PeekDetectorTheme.extendedColors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colors.gradientStart,
                        colors.gradientMid,
                        colors.gradientEnd
                    )
                )
            ),
        content = content
    )
}
