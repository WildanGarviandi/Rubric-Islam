package com.kellinreaver.rubricislam.ui.qiblat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.kellinreaver.rubricislam.ui.theme.RubricIslamTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblatScreen(viewModel: QiblatViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    RubricIslamTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = { Text("Qiblat Compass") })
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    CompassView(direction = uiState.direction)
                }
            }
        }
    }
}

@Composable
fun CompassView(direction: Float) {
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw the compass ring
            drawCircle(
                color = outlineColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // Draw the Qiblat arrow
            // We rotate the arrow by the direction to point to Qiblat
            rotate(degrees = direction) {
                drawLine(
                    color = Color.Red,
                    start = Offset(size.width / 2, size.height / 2),
                    end = Offset(size.width / 2, size.height / 2 - size.height / 2.5f),
                    strokeWidth = 10.dp.toPx()
                )
            }
            
            // Draw a simple North indicator
            drawLine(
                color = Color.Black,
                start = Offset(size.width / 2, size.height / 2 - size.height / 2.2f),
                end = Offset(size.width / 2, size.height / 2 - size.height / 2),
                strokeWidth = 4.dp.toPx()
            )
        }
    }
}
