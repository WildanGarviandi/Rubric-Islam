package com.kellinreaver.rubricislam.ui.qiblat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblatScreen(viewModel: QiblatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Smoothly animate the direction changes
    val animatedDirection by animateFloatAsState(
        targetValue = uiState.direction,
        animationSpec = tween(durationMillis = 500),
        label = "CompassRotation"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "QIBLAT", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Light
                        )
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.secondary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                ModernCompassView(direction = animatedDirection)
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Text(
                    text = "${uiState.direction.toInt()}°",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraLight
                    )
                )
                
                Text(
                    text = "ALIGN THE ARROW TO THE KAABA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@Composable
fun ModernCompassView(direction: Float) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .size(320.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Rings and Markings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Draw outer decorative ring
            drawCircle(
                color = outlineColor,
                style = Stroke(width = 1.dp.toPx()),
                radius = radius
            )

            // Draw degree notches
            for (i in 0 until 72) {
                val angle = i * 5f
                val angleRad = Math.toRadians(angle.toDouble())
                val isMajor = i % 18 == 0
                val length = if (isMajor) 15.dp.toPx() else 8.dp.toPx()
                
                val start = Offset(
                    (center.x + (radius - 5.dp.toPx()) * Math.cos(angleRad)).toFloat(),
                    (center.y + (radius - 5.dp.toPx()) * Math.sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + (radius - 5.dp.toPx() - length) * Math.cos(angleRad)).toFloat(),
                    (center.y + (radius - 5.dp.toPx() - length) * Math.sin(angleRad)).toFloat()
                )
                
                drawLine(
                    color = if (isMajor) primaryColor else outlineColor,
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
            }
        }

        // The Rotating Compass Part
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(-direction),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                
                // Draw Qiblat Arrow
                val arrowPath = Path().apply {
                    moveTo(center.x, center.y - 120.dp.toPx()) // Tip
                    lineTo(center.x - 12.dp.toPx(), center.y - 80.dp.toPx())
                    lineTo(center.x + 12.dp.toPx(), center.y - 80.dp.toPx())
                    close()
                }
                
                drawPath(
                    path = arrowPath,
                    color = primaryColor
                )
                
                // Draw line to arrow
                drawLine(
                    color = primaryColor.copy(alpha = 0.3f),
                    start = center,
                    end = Offset(center.x, center.y - 80.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
            
            // Modern Kaaba Illustration at the top of the rotating part
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .offset(y = (-110).dp),
                contentAlignment = Alignment.Center
            ) {
                KaabaIllustration()
            }
        }
        
        // Center Point
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(primaryColor, CircleShape)
        )
    }
}

@Composable
fun KaabaIllustration() {
    val gold = Color(0xFFD4AF37)
    val black = Color(0xFF1A1A1A)
    
    Canvas(modifier = Modifier.size(40.dp)) {
        // Main Body
        drawRoundRect(
            color = black,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        
        // Kiswah (Gold Belt)
        drawRect(
            color = gold,
            topLeft = Offset(0f, size.height * 0.25f),
            size = Size(size.width, size.height * 0.08f)
        )
        
        // Door (Gold)
        drawRect(
            color = gold,
            topLeft = Offset(size.width * 0.65f, size.height * 0.45f),
            size = Size(size.width * 0.2f, size.height * 0.35f)
        )
        
        // Top shadow/perspective line
        drawLine(
            color = Color.White.copy(alpha = 0.1f),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx()
        )
    }
}
