package com.kellinreaver.rubricislam.ui.prayer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kellinreaver.rubricislam.domain.model.PrayerTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CelestialPrayerChart(prayerTimes: List<PrayerTime>, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "celestial_pulse")
    // Breathing effect for the soft halo and core dot
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    // Notification "ping" progress (expanding ring)
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            val width = size.width
            val height = size.height
            val arcPadding = 16.dp.toPx()

            // 1. Atmospheric Elements (Subtle Stars)
            val starColor = secondaryColor.copy(alpha = 0.3f)
            listOf(
                Offset(width * 0.15f, height * 0.2f),
                Offset(width * 0.8f, height * 0.15f),
                Offset(width * 0.45f, height * 0.05f),
                Offset(width * 0.7f, height * 0.4f)
            ).forEach { drawCircle(starColor, radius = 1.dp.toPx(), center = it) }

            // 2. Define the Celestial Arc Bounding Box
            // We use a Rect that pulls the bottom far below the canvas to create a shallow arch
            val arcRect = Rect(
                left = 0f,
                top = arcPadding,
                right = width,
                bottom = height * 2.5f
            )

            // 3. Draw stylized Celestial Path (Dashed light trail)
            drawArc(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.0f),
                        secondaryColor.copy(alpha = 0.4f),
                        secondaryColor.copy(alpha = 0.0f)
                    )
                ),
                startAngle = 180f + 15f,
                sweepAngle = 180f - 30f,
                useCenter = false,
                topLeft = arcRect.topLeft,
                size = arcRect.size,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 16f), 0f)
                )
            )

            // 4. Calculate points on the same ellipse
            val a = width / 2f // semi-major axis
            val b = (height * 2.5f - arcPadding) / 2f // semi-minor axis
            val centerX = width / 2f
            val centerY = (height * 2.5f + arcPadding) / 2f

            if (prayerTimes.isNotEmpty()) {
                val total = prayerTimes.size
                prayerTimes.forEachIndexed { index, prayer ->
                    // Map points across the arch (195 to 345 degrees)
                    val angle = 195f + (index.toFloat() / (total - 1).coerceAtLeast(1)) * 150f
                    val angleRad = Math.toRadians(angle.toDouble())

                    val x = centerX + a * cos(angleRad).toFloat()
                    val y = centerY + b * sin(angleRad).toFloat()

                    val isNext = prayer.isNext
                    val color = if (isNext) secondaryColor else primaryColor.copy(alpha = 0.4f)

                    if (isNext) {
                        // 1. Soft Breathing Solar Halo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    secondaryColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                center = Offset(x, y),
                                radius = 32.dp.toPx() * glowScale
                            ),
                            radius = 32.dp.toPx() * glowScale,
                            center = Offset(x, y)
                        )

                        // 2. Notification "Ping" Ring (Expands and fades)
                        drawCircle(
                            color = secondaryColor.copy(alpha = 0.6f * (1f - pulseProgress)),
                            radius = 8.dp.toPx() + (32.dp.toPx() * pulseProgress),
                            center = Offset(x, y),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Marker (Grows slightly if it's the next prayer)
                    drawCircle(
                        color = color,
                        radius = if (isNext) 6.dp.toPx() * glowScale else 3.5.dp.toPx(),
                        center = Offset(x, y)
                    )

                    // Outer definition ring
                    drawCircle(
                        color = color.copy(alpha = 0.2f),
                        radius = (if (isNext) 10.dp.toPx() else 6.dp.toPx()),
                        center = Offset(x, y),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // 5. Minimalist Horizon Line
            drawLine(
                color = secondaryColor.copy(alpha = 0.1f),
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}
