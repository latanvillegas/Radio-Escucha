package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 3D Volumetric Color & Depth System for OpenRadio.
 * Combines multi-stop directional lighting gradients, top-left specular rim highlights,
 * bottom-right ambient occlusion borders, and colored 3D elevation shadows.
 */
object Surface3D {

    @Composable
    fun ambientScreenBrush(): Brush {
        val bg = MaterialTheme.colorScheme.background
        val surface = MaterialTheme.colorScheme.surface
        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary
        return remember(bg, surface, primary, tertiary) {
            Brush.verticalGradient(
                0.0f to lerp(bg, primary, 0.16f),
                0.28f to lerp(bg, surface, 0.55f),
                0.68f to bg,
                1.0f to lerp(bg, tertiary, 0.08f)
            )
        }
    }

    @Composable
    fun heroDashboardBrush(): Brush {
        val surface = MaterialTheme.colorScheme.surface
        val bg = MaterialTheme.colorScheme.background
        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary
        return remember(surface, bg, primary, tertiary) {
            Brush.linearGradient(
                colors = listOf(
                    lerp(surface, primary, 0.28f),
                    lerp(surface, Color.White, 0.05f),
                    lerp(surface, bg, 0.45f),
                    lerp(bg, tertiary, 0.16f)
                ),
                start = Offset(0f, 0f),
                end = Offset(900f, 550f)
            )
        }
    }

    @Composable
    fun heroDashboardBorderBrush(): Brush {
        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary
        return remember(primary, tertiary) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.42f),
                    primary.copy(alpha = 0.65f),
                    tertiary.copy(alpha = 0.35f),
                    Color.Black.copy(alpha = 0.65f)
                ),
                start = Offset(0f, 0f),
                end = Offset(800f, 500f)
            )
        }
    }

    @Composable
    fun cardBrush(isSelectedOrPlaying: Boolean = false): Brush {
        val surface = MaterialTheme.colorScheme.surface
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val bg = MaterialTheme.colorScheme.background
        return remember(surface, surfaceVariant, primary, tertiary, bg, isSelectedOrPlaying) {
            if (isSelectedOrPlaying) {
                Brush.linearGradient(
                    colors = listOf(
                        lerp(surfaceVariant, primary, 0.36f),
                        lerp(surface, primary, 0.18f),
                        lerp(surface, tertiary, 0.22f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(700f, 260f)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        lerp(surfaceVariant, Color.White, 0.08f).copy(alpha = 0.88f),
                        surface.copy(alpha = 0.92f),
                        lerp(surface, bg, 0.55f).copy(alpha = 0.96f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(600f, 240f)
                )
            }
        }
    }

    @Composable
    fun cardBorderBrush(isSelectedOrPlaying: Boolean = false): Brush {
        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary
        return remember(primary, tertiary, isSelectedOrPlaying) {
            if (isSelectedOrPlaying) {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        primary.copy(alpha = 0.9f),
                        tertiary.copy(alpha = 0.65f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(600f, 220f)
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        primary.copy(alpha = 0.20f),
                        Color.Black.copy(alpha = 0.55f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(600f, 220f)
                )
            }
        }
    }

    @Composable
    fun convexOrbBrush(baseColor: Color = MaterialTheme.colorScheme.primary): Brush {
        val tertiary = MaterialTheme.colorScheme.tertiary
        return remember(baseColor, tertiary) {
            Brush.linearGradient(
                colors = listOf(
                    lerp(baseColor, Color.White, 0.42f),
                    baseColor,
                    lerp(baseColor, tertiary, 0.35f),
                    lerp(baseColor, Color.Black, 0.38f)
                ),
                start = Offset(0f, 0f),
                end = Offset(180f, 180f)
            )
        }
    }

    @Composable
    fun convexOrbBorderBrush(baseColor: Color = MaterialTheme.colorScheme.primary): Brush {
        return remember(baseColor) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.65f),
                    baseColor.copy(alpha = 0.5f),
                    Color.Black.copy(alpha = 0.55f)
                ),
                start = Offset(0f, 0f),
                end = Offset(160f, 160f)
            )
        }
    }

    @Composable
    fun glassButtonBrush(): Brush {
        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
        val surface = MaterialTheme.colorScheme.surface
        return remember(surfaceVariant, surface) {
            Brush.linearGradient(
                colors = listOf(
                    lerp(surfaceVariant, Color.White, 0.16f).copy(alpha = 0.85f),
                    surface.copy(alpha = 0.85f),
                    lerp(surface, Color.Black, 0.35f).copy(alpha = 0.9f)
                ),
                start = Offset(0f, 0f),
                end = Offset(120f, 120f)
            )
        }
    }

    @Composable
    fun recessedInputBrush(): Brush {
        val bg = MaterialTheme.colorScheme.background
        val surface = MaterialTheme.colorScheme.surface
        return remember(bg, surface) {
            Brush.verticalGradient(
                colors = listOf(
                    lerp(bg, Color.Black, 0.35f).copy(alpha = 0.85f),
                    lerp(bg, surface, 0.55f).copy(alpha = 0.85f),
                    lerp(surface, Color.White, 0.06f).copy(alpha = 0.85f)
                )
            )
        }
    }

    @Composable
    fun recessedBorderBrush(isFocused: Boolean = false): Brush {
        val primary = MaterialTheme.colorScheme.primary
        return remember(primary, isFocused) {
            if (isFocused) {
                Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.9f),
                        Color.White.copy(alpha = 0.45f),
                        primary.copy(alpha = 0.65f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.65f),
                        primary.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.20f)
                    )
                )
            }
        }
    }

    @Composable
    fun bottomBar3DBrush(): Brush {
        val surface = MaterialTheme.colorScheme.surface
        val bg = MaterialTheme.colorScheme.background
        val primary = MaterialTheme.colorScheme.primary
        return remember(surface, bg, primary) {
            Brush.verticalGradient(
                colors = listOf(
                    lerp(surface, primary, 0.18f),
                    surface,
                    lerp(bg, Color.Black, 0.25f)
                )
            )
        }
    }
}

@Composable
fun Modifier.card3D(
    shape: Shape = RoundedCornerShape(18.dp),
    isSelectedOrPlaying: Boolean = false,
    elevation: Dp = if (isSelectedOrPlaying) 10.dp else 5.dp
): Modifier {
    val primary = MaterialTheme.colorScheme.primary
    val brush = Surface3D.cardBrush(isSelectedOrPlaying)
    val borderBrush = Surface3D.cardBorderBrush(isSelectedOrPlaying)
    val borderWidth = if (isSelectedOrPlaying) 1.4.dp else 1.dp

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (isSelectedOrPlaying) primary else Color.Black,
            spotColor = if (isSelectedOrPlaying) primary else primary.copy(alpha = 0.45f)
        )
        .clip(shape)
        .background(brush, shape)
        .border(BorderStroke(borderWidth, borderBrush), shape)
}

@Composable
fun Modifier.heroDashboard3D(
    shape: Shape = RoundedCornerShape(22.dp),
    elevation: Dp = 12.dp
): Modifier {
    val primary = MaterialTheme.colorScheme.primary
    val brush = Surface3D.heroDashboardBrush()
    val borderBrush = Surface3D.heroDashboardBorderBrush()

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = primary,
            spotColor = primary
        )
        .clip(shape)
        .background(brush, shape)
        .border(BorderStroke(1.3.dp, borderBrush), shape)
}

@Composable
fun Modifier.orbButton3D(
    baseColor: Color = MaterialTheme.colorScheme.primary,
    elevation: Dp = 8.dp
): Modifier {
    val brush = Surface3D.convexOrbBrush(baseColor)
    val borderBrush = Surface3D.convexOrbBorderBrush(baseColor)

    return this
        .shadow(
            elevation = elevation,
            shape = CircleShape,
            ambientColor = baseColor,
            spotColor = baseColor
        )
        .clip(CircleShape)
        .background(brush, CircleShape)
        .border(BorderStroke(1.2.dp, borderBrush), CircleShape)
}

@Composable
fun Modifier.glassCircle3D(
    elevation: Dp = 4.dp
): Modifier {
    val primary = MaterialTheme.colorScheme.primary
    val brush = Surface3D.glassButtonBrush()
    val borderBrush = Surface3D.cardBorderBrush(isSelectedOrPlaying = false)

    return this
        .shadow(
            elevation = elevation,
            shape = CircleShape,
            ambientColor = Color.Black,
            spotColor = primary.copy(alpha = 0.4f)
        )
        .clip(CircleShape)
        .background(brush, CircleShape)
        .border(BorderStroke(1.dp, borderBrush), CircleShape)
}
