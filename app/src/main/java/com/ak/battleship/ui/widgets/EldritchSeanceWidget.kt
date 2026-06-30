package com.ak.battleship.ui.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// A clean data class to handle the dark arts type-safely
data class EldritchState(
    val prophecyText: String,
    val mysticColor: Color,
    val textColor: Color,
    val activeCandles: Int
)

@Composable
fun EldritchSeanceWidget(probability: Float) {
    val animatedProb by animateFloatAsState(targetValue = probability, tween(1000), label = "SeanceAnim")
    val scrollState = rememberScrollState()

    // 1. Unified 5-Stage Type-Safe Matrix
    val state = when {
        animatedProb > 0.85f -> EldritchState(
            prophecyText = "The celestial alignment solidifies. The stars orchestrate their execution.",
            mysticColor = Color(0xFFFFD54F), // Amber/Gold
            textColor = Color(0xFFFFF8E1),
            activeCandles = 3
        )
        animatedProb > 0.60f -> EldritchState(
            prophecyText = "The ether remains steady. Fate holds the thread tightly.",
            mysticColor = Color(0xFFB39DDB), // Ethereal Purple
            textColor = Color(0xFFEDE7F6),
            activeCandles = 2
        )
        animatedProb > 0.40f -> EldritchState(
            prophecyText = "Shadows lengthen across the astral plane. The spirits grow restless.",
            mysticColor = Color(0xFF4DB6AC), // Ghostly Teal
            textColor = Color(0xFFE0F2F1),
            activeCandles = 1
        )
        animatedProb > 0.15f -> EldritchState(
            prophecyText = "The veil tears! Dark forces converge upon your vessel's coordinates.",
            mysticColor = Color(0xFFFF8A65), // Warning Orange
            textColor = Color(0xFFFBE9E7),
            activeCandles = 0
        )
        else -> EldritchState(
            prophecyText = "The eternal abyss cracks open. Darkness claims this targeted vessel.",
            mysticColor = Color(0xFFE53935), // Blood Red
            textColor = Color(0xFFFFEBEE),
            activeCandles = 0
        )
    }

    // 2. Supernatural Animation Engines
    val infiniteTransition = rememberInfiniteTransition(label = "EldritchEngine")

    // Smooth, ambient fog drift across the entire widget
    val fogBillow by infiniteTransition.animateFloat(
        initialValue = -0.5f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "FogBillow"
    )

    // Constant slow swirl of the mist inside the orb
    val mistRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "MistSwirl"
    )

    // Pulsing aura of the orb and the candle flames
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "GlowPulse"
    )

    // Frantic candle flicker
    val flickerY by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(100, easing = LinearEasing), RepeatMode.Reverse),
        label = "Flicker"
    )

    // Violent shaking when the abyss opens
    val doomShake by infiniteTransition.animateFloat(
        initialValue = -1.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(50, easing = LinearEasing), RepeatMode.Reverse),
        label = "DoomShake"
    )

    // 3. The Crawl, Pause, and Teleport-Reset Marquee Engine
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

    // THE MASTER CONTAINER
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F0518)) // Deep Void Purple/Black
            .border(2.dp, state.mysticColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {

        // --- BACKGROUND AMBIENT FOG LAYER ---
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val billowOffset = w * fogBillow

            // Mist Plume 1 (Drifting Left/Right)
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(state.mysticColor.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(w * 0.3f + billowOffset, h * 0.5f),
                    radius = h * 0.9f
                ),
                radius = h * 0.9f,
                center = Offset(w * 0.3f + billowOffset, h * 0.5f)
            )

            // Mist Plume 2 (Drifting the opposite direction for parallax)
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(state.mysticColor.copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(w * 0.7f - billowOffset, h * 0.6f),
                    radius = h * 1.2f
                ),
                radius = h * 1.2f,
                center = Offset(w * 0.7f - billowOffset, h * 0.6f)
            )
        }

        // --- FOREGROUND CONTENT ---
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Apply doom shake only when odds drop below 15%
            val orbOffset = if (animatedProb <= 0.15f) doomShake.dp else 0.dp

            // --- SACRED ORACLE ORB CANVAS ---
            Canvas(modifier = Modifier.size(64.dp).offset(x = orbOffset, y = orbOffset)) {
                val center = Offset(size.width / 2, size.height / 2)
                val orbRadius = 22.dp.toPx()

                // 1. The Pedestal
                drawRoundRect(
                    color = Color(0xFF261A30),
                    topLeft = Offset(size.width * 0.25f, size.height * 0.75f),
                    size = Size(size.width * 0.5f, 8.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF1E1226),
                    topLeft = Offset(size.width * 0.35f, size.height * 0.85f),
                    size = Size(size.width * 0.3f, 6.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // 2. The Glass Orb Base
                drawCircle(Color.Black, radius = orbRadius, center = center)

                // 3. The Swirling Mist Layers (Inside the Glass)
                clipPath(
                    androidx.compose.ui.graphics.Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(center.x - orbRadius, center.y - orbRadius, center.x + orbRadius, center.y + orbRadius))
                    }
                ) {
                    rotate(mistRotation, pivot = center) {
                        drawArc(
                            color = state.mysticColor.copy(alpha = 0.5f),
                            startAngle = 0f, sweepAngle = 140f, useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(center.x - orbRadius * 0.7f, center.y - orbRadius * 0.7f),
                            size = Size(orbRadius * 1.4f, orbRadius * 1.4f)
                        )
                        drawArc(
                            color = state.mysticColor.copy(alpha = 0.3f),
                            startAngle = 180f, sweepAngle = 100f, useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(center.x - orbRadius * 0.4f, center.y - orbRadius * 0.4f),
                            size = Size(orbRadius * 0.8f, orbRadius * 0.8f)
                        )
                    }

                    // 4. Emerging Eyes (Visible only in Stage 5 doom)
                    if (animatedProb <= 0.15f) {
                        val eyeY = center.y - 4.dp.toPx()
                        drawOval(Color.White.copy(alpha = 0.9f), topLeft = Offset(center.x - 10.dp.toPx(), eyeY), size = Size(6.dp.toPx(), 4.dp.toPx()))
                        drawCircle(Color.Black, radius = 1.dp.toPx(), center = Offset(center.x - 7.dp.toPx(), eyeY + 2.dp.toPx()))
                        drawOval(Color.White.copy(alpha = 0.9f), topLeft = Offset(center.x + 4.dp.toPx(), eyeY), size = Size(6.dp.toPx(), 4.dp.toPx()))
                        drawCircle(Color.Black, radius = 1.dp.toPx(), center = Offset(center.x + 7.dp.toPx(), eyeY + 2.dp.toPx()))
                    }
                }

                // 5. The Glass Glare & Glowing Aura
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(state.mysticColor.copy(alpha = glowPulse), Color.Transparent),
                        center = center, radius = orbRadius
                    ),
                    radius = orbRadius, center = center
                )
                drawCircle(state.mysticColor.copy(alpha = 0.8f), radius = orbRadius, center = center, style = Stroke(width = 1.dp.toPx()))
                drawArc(
                    color = Color.White.copy(alpha = 0.2f),
                    startAngle = 200f, sweepAngle = 50f, useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(center.x - orbRadius * 0.8f, center.y - orbRadius * 0.8f),
                    size = Size(orbRadius * 1.6f, orbRadius * 1.6f)
                )

                // 6. FOREGROUND: The Ritual Candles
                // Placed in an arc around the front lip of the pedestal
                val candlePositions = listOf(
                    Pair(size.width * 0.18f, size.height * 0.72f), // Back Left
                    Pair(size.width * 0.50f, size.height * 0.85f), // Front Center
                    Pair(size.width * 0.82f, size.height * 0.76f)  // Back Right
                )

                for (i in 0 until 3) {
                    val cX = candlePositions[i].first
                    val cY = candlePositions[i].second
                    val candleHeight = if (i == 1) size.height * 0.15f else size.height * 0.25f // Front center is shortest

                    // Shadow to help them pop off the glowing glass
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(cX - 4.dp.toPx(), cY + 2.dp.toPx()),
                        size = Size(8.dp.toPx(), candleHeight)
                    )

                    // The wax pillar
                    drawRect(
                        color = Color(0xFFCFD8DC),
                        topLeft = Offset(cX - 3.dp.toPx(), cY),
                        size = Size(6.dp.toPx(), candleHeight)
                    )

                    if (i < state.activeCandles) {
                        val fY = cY - 2.dp.toPx() + flickerY.dp.toPx()
                        // Flame halo
                        drawCircle(Color(0xFFFFB300).copy(alpha = glowPulse * 0.6f), radius = 6.dp.toPx(), center = Offset(cX, fY))
                        // Core flame
                        drawCircle(Color(0xFFFFF8E1), radius = 2.dp.toPx(), center = Offset(cX, fY))
                    } else {
                        // Blown out wick smoke
                        drawLine(Color.Gray, Offset(cX, cY), Offset(cX - 2.dp.toPx(), cY - 6.dp.toPx()), strokeWidth = 1.dp.toPx())
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- THE ASTRAL READOUT ---
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${(animatedProb * 100).roundToInt()}%",
                        fontSize = 32.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        color = state.mysticColor,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        Text("ASTRAL PROJECTION", fontSize = 10.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = state.mysticColor.copy(alpha = 0.7f), letterSpacing = 1.sp)
                        Text("SEANCE LINK ACTIVE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = state.textColor.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = state.prophecyText,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    color = state.textColor,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)
                )
            }
        }
    }
}