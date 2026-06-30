package com.ak.battleship.ui.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// A clean data class to handle the weather state type-safely
data class WeatherState(
    val forecastText: String,
    val accentColor: Color,
    val textColor: Color,
    val bgTop: Color,
    val bgBottom: Color
)

@Composable
fun WeatherBroadcastWidget(probability: Float) {
    val animatedProb by animateFloatAsState(targetValue = probability, tween(1000), label = "WeatherAnim")
    val scrollState = rememberScrollState()

    // 1. Unified 5-Stage Type-Safe Matrix
    val state = when {
        animatedProb > 0.85f -> WeatherState(
            forecastText = "100% Chance of Smooth Sailing. High pressure system securing victory.",
            accentColor = Color(0xFFFBC02D), // Sun Yellow
            textColor = Color(0xFF1B5E20),  // Dark Green
            bgTop = Color(0xFFE3F2FD), bgBottom = Color(0xFFBBDEFB) // Clear Blue Sky
        )
        animatedProb > 0.60f -> WeatherState(
            forecastText = "Fair winds and following seas. Visibility optimal for tactical strikes.",
            accentColor = Color(0xFF00ACC1), // Cyan
            textColor = Color(0xFF006064),  // Deep Teal
            bgTop = Color(0xFFE1F5FE), bgBottom = Color(0xFFB3E5FC) // Light Cloudy Sky
        )
        animatedProb > 0.40f -> WeatherState(
            forecastText = "Partly Cloudy with a Chance of Misses. Scattered tactical fog rolls in.",
            accentColor = Color(0xFFFF9800), // Warning Orange
            textColor = Color(0xFFE65100),  // Dark Orange
            bgTop = Color(0xFFFFF3E0), bgBottom = Color(0xFFFFCC80) // Overcast Sunset
        )
        animatedProb > 0.15f -> WeatherState(
            forecastText = "Gale Warning. Heavy incoming fire detected. Secure the deck!",
            accentColor = Color(0xFFF4511E), // Red-Orange
            textColor = Color(0xFFBF360C),  // Deep Red-Brown
            bgTop = Color(0xFFCFD8DC), bgBottom = Color(0xFF90A4AE) // Stormy Gray
        )
        else -> WeatherState(
            forecastText = "CRITICAL: CATEGORY 5 DOOM STORM ACTIVATED. SEEK SHELTER IMMEDIATELY.",
            accentColor = Color(0xFFFF1744), // Neon Red
            textColor = Color(0xFFFFEBEE),  // Pale Pink
            bgTop = Color(0xFF263238), bgBottom = Color(0xFFB71C1C) // Dark Gray to Blood Red
        )
    }

    // 2. Weather Animation Engines
    val infiniteTransition = rememberInfiniteTransition(label = "WeatherEngine")

    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "SunSpin"
    )

    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "CloudDrift"
    )

    val lightningFlash by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(150, easing = LinearEasing), RepeatMode.Reverse),
        label = "Lightning"
    )

    // 3. The Ping-Pong Text Marquee
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            while (true) {
                delay(1500)
                scrollState.animateScrollTo(scrollState.maxValue, tween(3500, easing = LinearEasing))
                delay(1500)
                scrollState.scrollTo(0)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(state.bgTop, state.bgBottom)))
            .border(2.dp, state.accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // --- LIVE METEOROLOGY RADAR CANVAS ---
        Canvas(modifier = Modifier.size(64.dp)) {
            val center = Offset(size.width / 2, size.height / 2)

            // LAYER 1: The Sun (Visible above 40%)
            if (animatedProb > 0.40f) {
                rotate(sunRotation, pivot = center) {
                    drawCircle(Color(0xFFFFD54F), radius = 14.dp.toPx(), center = center)
                    for (i in 0..7) {
                        rotate(i * 45f, pivot = center) {
                            drawLine(
                                color = Color(0xFFFFB300),
                                start = Offset(center.x, center.y - 18.dp.toPx()),
                                end = Offset(center.x, center.y - 24.dp.toPx()),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // LAYER 2: The Rain (Visible below 40%)
            if (animatedProb <= 0.40f) {
                val rainColor = if (animatedProb > 0.15f) Color(0xFF546E7A) else Color(0xFFEF9A9A)
                for (i in 0..4) {
                    val xPos = (10 + (i * 10)).dp.toPx()
                    drawLine(
                        color = rainColor.copy(alpha = 0.6f),
                        start = Offset(xPos + cloudDrift.dp.toPx(), size.height * 0.4f),
                        end = Offset(xPos - 10.dp.toPx() + cloudDrift.dp.toPx(), size.height * 0.9f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // LAYER 3: The Clouds (Visible below 85%)
            if (animatedProb <= 0.85f) {
                val cloudColor = if (animatedProb > 0.40f) Color.White else if (animatedProb > 0.15f) Color(0xFF78909C) else Color(0xFF424242)
                val cOffset = cloudDrift.dp.toPx()

                drawCircle(cloudColor, radius = 12.dp.toPx(), center = Offset(size.width * 0.4f + cOffset, size.height * 0.5f))
                drawCircle(cloudColor, radius = 16.dp.toPx(), center = Offset(size.width * 0.65f + cOffset, size.height * 0.45f))
                drawRoundRect(
                    color = cloudColor,
                    topLeft = Offset(size.width * 0.25f + cOffset, size.height * 0.45f),
                    size = Size(size.width * 0.6f, 20.dp.toPx()),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
            }

            // LAYER 4: The Lightning (Visible below 15%)
            if (animatedProb <= 0.15f) {
                val lightningPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.45f)
                    lineTo(size.width * 0.35f, size.height * 0.7f)
                    lineTo(size.width * 0.55f, size.height * 0.65f)
                    lineTo(size.width * 0.45f, size.height * 0.95f)
                }
                drawPath(lightningPath, color = Color(0xFFFFEA00).copy(alpha = lightningFlash), style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Miter))
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- THE FORECAST READOUT ---
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${(animatedProb * 100).roundToInt()}%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = state.accentColor,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text("WIN PROBABILITY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = state.textColor.copy(alpha = 0.7f))
                    Text("LIVE LOCAL RADAR", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = state.textColor.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = state.forecastText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = state.textColor, // Fix: Completely type-safe now!
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)
            )
        }
    }
}