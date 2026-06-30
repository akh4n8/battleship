package com.ak.battleship.ui.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SonarLoader(text: String = "SCANNING DATABASE...") {
    val infiniteTransition = rememberInfiniteTransition(label = "SonarAnim")

    // The continuous radar sweep
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "Sweep"
    )

    // The expanding/fading sonar ping
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing)),
        label = "Pulse"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.width / 2f
                val center = Offset(radius, radius)
                val baseGreen = Color(0xFF6650a4)

                // 1. Static Background Rings
                drawCircle(baseGreen.copy(alpha = 0.15f), radius, center, style = Stroke(2f))
                drawCircle(baseGreen.copy(alpha = 0.15f), radius * 0.66f, center, style = Stroke(1f))
                drawCircle(baseGreen.copy(alpha = 0.15f), radius * 0.33f, center, style = Stroke(1f))
                drawLine(baseGreen.copy(alpha = 0.1f), Offset(center.x, 0f), Offset(center.x, size.height), 1f)
                drawLine(baseGreen.copy(alpha = 0.1f), Offset(0f, center.y), Offset(size.width, center.y), 1f)

                // 2. The Expanding Ping
                drawCircle(
                    color = baseGreen.copy(alpha = 1f - pulse),
                    radius = radius * pulse,
                    center = center,
                    style = Stroke(4f * (1f - pulse)) // Stroke gets thinner as it expands
                )

                // 3. The Radar Sweep
                rotate(sweepAngle, center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color.Transparent, baseGreen.copy(alpha = 0.6f)),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = size
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = text,
            color = Color(0xFF6650a4),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}