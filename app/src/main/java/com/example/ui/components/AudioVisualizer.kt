package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MiniVisualizer(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_vis")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Canvas(modifier = modifier.size(20.dp)) {
        val spacing = 2.dp.toPx()
        val barWidth = (size.width - spacing * 2f) / 3f
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val heights = floatArrayOf(h1, h2, h3)

        for (i in 0..2) {
            val barHeight = (size.height * heights[i]).coerceAtLeast(2f)
            val left = i * (barWidth + spacing)
            val top = size.height - barHeight
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = radius
            )
        }
    }
}

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val animationProgress = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "visualizer")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress"
        )
        progress
    } else {
        0f
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val totalSpacingUnit = barCount * 2 - 1
        val barWidth = width / totalSpacingUnit

        for (i in 0 until barCount) {
            val factor = if (isPlaying) {
                val angle = (animationProgress * 2 * Math.PI) + (i * 1.5)
                val baseValue = sin(angle) * 0.45 + 0.55
                val noise = cos(angle * 1.7 + i) * 0.15
                (baseValue + noise).toFloat().coerceIn(0.1f, 1.0f)
            } else {
                0.08f
            }
            val barHeight = height * factor
            val x = i * barWidth * 2
            val y = height - barHeight

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
