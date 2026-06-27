package com.kellinreaver.rubricislam.ui.prayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kellinreaver.rubricislam.domain.model.PrayerTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CelestialPrayerChart(prayerTimes: List<PrayerTime>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(top = 32.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val radius = width * 0.8f
            val arcCenterY = height + (radius * 0.2f)

            // Draw the Celestial Arc
            drawArc(
                color = secondaryColor.copy(alpha = 0.2f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - radius, arcCenterY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw Prayer Points
            val angleStep = 180f / (prayerTimes.size + 1)
            prayerTimes.forEachIndexed { index, prayer ->
                val angle = 180f + (index + 1) * angleStep
                val angleRad = Math.toRadians(angle.toDouble())

                val x = centerX + radius * cos(angleRad).toFloat()
                val y = arcCenterY + radius * sin(angleRad).toFloat()

                // Draw Marker
                drawCircle(
                    color = if (prayer.isNext) secondaryColor else primaryColor.copy(alpha = 0.6f),
                    radius = if (prayer.isNext) 8.dp.toPx() else 4.dp.toPx(),
                    center = Offset(x, y)
                )

                if (prayer.isNext) {
                    // Glow effect for next prayer
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(secondaryColor.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(x, y),
                            radius = 20.dp.toPx()
                        ),
                        radius = 20.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
