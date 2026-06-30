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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// A clean data class to handle the explosive tension type-safely
data class BombState(
    val directive: String,
    val accentColor: Color,
    val textColor: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val dangerLevel: Int // 0: Calm, 1: Steady, 2: Tense, 3: Panic, 4: Detonation Imminent
)

@Composable
fun BombDefusalWidget(probability: Float) {
    val animatedProb by animateFloatAsState(targetValue = probability, tween(1000), label = "BombAnim")
    val scrollState = rememberScrollState()

    // 1. Unified 5-Stage Type-Safe Matrix
    val state = when {
        animatedProb > 0.85f -> BombState(
            directive = "Threat neutralized. Cut the blue wire at your leisure. Good work, Commander.",
            accentColor = Color(0xFF00E676), // Safe Green
            textColor = Color(0xFFB9F6CA),
            bgTop = Color(0xFF1B5E20), bgBottom = Color(0xFF000000), // Dark green to black
            dangerLevel = 0
        )
        animatedProb > 0.60f -> BombState(
            directive = "Circuitry stable. Bypass the secondary detonator to maintain the tactical advantage.",
            accentColor = Color(0xFF00B0FF), // Cool Blue
            textColor = Color(0xFF84FFFF),
            bgTop = Color(0xFF01579B), bgBottom = Color(0xFF000000),
            dangerLevel = 1
        )
        animatedProb > 0.40f -> BombState(
            directive = "Uncertain pathways detected. Avoid parsing structural loops. Hands are getting sweaty.",
            accentColor = Color(0xFFFFEA00), // Warning Yellow
            textColor = Color(0xFFFFFF8D),
            bgTop = Color(0xFF4E342E), bgBottom = Color(0xFF000000),
            dangerLevel = 2
        )
        animatedProb > 0.15f -> BombState(
            directive = "WARNING: Primary timer accelerated! Structural breach detected! Hurry it up!",
            accentColor = Color(0xFFFF3D00), // Panic Orange
            textColor = Color(0xFFFF9E80),
            bgTop = Color(0xFF3E2723), bgBottom = Color(0xFF000000),
            dangerLevel = 3
        )
        else -> BombState(
            directive = "CRITICAL TIMELINE FAILURE: DETONATION IMMINENT. CUT ANYTHING!! BRACE FOR IMPACT!",
            accentColor = Color(0xFFFF1744), // Critical Red
            textColor = Color(0xFFFF8A80),
            bgTop = Color(0xFFB71C1C), bgBottom = Color(0xFF000000),
            dangerLevel = 4
        )
    }

    // 2. High-Tension Animation Engines
    val infiniteTransition = rememberInfiniteTransition(label = "DefusalEngine")

    // Sweeping background emergency light / laser scanner
    val scannerSweep by infiniteTransition.animateFloat(
        initialValue = -0.2f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state.dangerLevel >= 3) 800 else 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScannerSweep"
    )

    // Strobe light on the bomb pack (blinks frantically at high danger)
    val ledStrobe by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state.dangerLevel >= 4) 50 else if (state.dangerLevel == 3) 200 else 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LedStrobe"
    )

    // Violent shaking of the wire cutters
    val cutterShake by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state.dangerLevel >= 3) 40 else 120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CutterShake"
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
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(state.bgTop, state.bgBottom)))
            .border(2.dp, state.accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {

        // --- BACKGROUND AMBIENT LAYER (Laser Grid & Sirens) ---
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Draw a subtle tactical grid
            for (i in 1..5) {
                drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, h * (i / 6f)), Offset(w, h * (i / 6f)), 1f)
                drawLine(Color.White.copy(alpha = 0.05f), Offset(w * (i / 6f), 0f), Offset(w * (i / 6f), h), 1f)
            }

            // Draw the sweeping emergency scanner light
            val scanX = w * scannerSweep
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, state.accentColor.copy(alpha = 0.15f), Color.Transparent),
                    startX = scanX - w * 0.2f, endX = scanX + w * 0.2f
                ),
                topLeft = Offset(0f, 0f),
                size = Size(w, h)
            )
            // The bright laser core
            drawLine(
                color = state.accentColor.copy(alpha = 0.3f),
                start = Offset(scanX, 0f), end = Offset(scanX, h),
                strokeWidth = 2.dp.toPx()
            )
        }

        // --- FOREGROUND CONTENT ---
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // --- HOLLYWOOD DYNAMITE CANVAS ---
            Canvas(modifier = Modifier.size(64.dp)) {
                val w = size.width
                val h = size.height

                val stickWidth = w * 0.22f
                val stickHeight = h * 0.8f
                val startY = h * 0.15f

                // 1. Draw 3 distinct localized dynamite tubes
                val redExplosive = Color(0xFFC62828)
                val darkExplosive = Color(0xFF8E0000)

                for (i in 0..2) {
                    val xPos = w * 0.1f + (i * stickWidth * 1.1f)

                    drawRoundRect(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(darkExplosive, redExplosive, darkExplosive),
                            startX = xPos, endX = xPos + stickWidth
                        ),
                        topLeft = Offset(xPos, startY),
                        size = Size(stickWidth, stickHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                // 2. Electrical system control tape
                drawRect(Color(0xFF212121), topLeft = Offset(w * 0.05f, h * 0.4f), size = Size(w * 0.8f, h * 0.15f))
                drawRect(Color.Black, topLeft = Offset(w * 0.05f, h * 0.65f), size = Size(w * 0.8f, h * 0.1f))

                // 3. Strobe LED & Timer Box
                drawRoundRect(
                    color = Color(0xFF1E1E1E),
                    topLeft = Offset(w * 0.25f, h * 0.42f),
                    size = Size(w * 0.4f, h * 0.11f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // The strobing LED light
                val ledColor = if (state.dangerLevel >= 3) Color.Red else Color(0xFF00E676)
                drawCircle(ledColor.copy(alpha = ledStrobe), radius = 2.dp.toPx(), center = Offset(w * 0.3f, h * 0.475f))
                drawCircle(Color.White.copy(alpha = 0.8f), radius = 0.5.dp.toPx(), center = Offset(w * 0.3f, h * 0.475f))

                // 4. Tangled Structural Circuitry Wiring
                val wirePath1 = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.5f, h * 0.42f)
                    quadraticBezierTo(w * 0.8f, h * 0.2f, w * 0.6f, h * 0.15f)
                    quadraticBezierTo(w * 0.4f, h * 0.1f, w * 0.3f, 0f)
                }
                val wirePath2 = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.65f, h * 0.5f)
                    quadraticBezierTo(w * 0.9f, h * 0.7f, w * 0.8f, h * 0.9f)
                    quadraticBezierTo(w * 0.7f, h, w * 0.5f, h * 0.95f)
                }

                drawPath(wirePath1, color = Color(0xFF2962FF), style = Stroke(width = 2.5.dp.toPx())) // Blue Wire
                drawPath(wirePath2, color = Color(0xFFFFD600), style = Stroke(width = 2.5.dp.toPx())) // Yellow Wire

                // 5. The Hovering, Sweaty Wire Cutters
                // Intense shaking applied based on danger level
                val cShakeX = if (state.dangerLevel >= 2) cutterShake.dp.toPx() * (state.dangerLevel) else 0f
                val cShakeY = if (state.dangerLevel >= 2) cutterShake.dp.toPx() * (state.dangerLevel * 0.5f) else 0f

                rotate(15f, pivot = Offset(w * 0.8f, h * 0.2f)) {
                    val cutterCenter = Offset(w * 0.75f + cShakeX, h * 0.25f + cShakeY)

                    // Handles
                    drawLine(Color(0xFFD32F2F), start = cutterCenter, end = Offset(cutterCenter.x + w * 0.3f, cutterCenter.y - h * 0.15f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(Color(0xFFD32F2F), start = cutterCenter, end = Offset(cutterCenter.x + w * 0.25f, cutterCenter.y + h * 0.15f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)

                    // Metal Blades
                    val bladePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cutterCenter.x, cutterCenter.y)
                        lineTo(cutterCenter.x - w * 0.25f, cutterCenter.y - h * 0.1f)
                        lineTo(cutterCenter.x - w * 0.3f, cutterCenter.y)
                        lineTo(cutterCenter.x - w * 0.25f, cutterCenter.y + h * 0.1f)
                        close()
                    }
                    drawPath(bladePath, color = Color(0xFF9E9E9E)) // Silver Blade

                    // Hinge Bolt
                    drawCircle(Color(0xFF424242), radius = 2.dp.toPx(), center = cutterCenter)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- THE EOD READOUT ---
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    // Large numeric safety telemetry
                    Text(
                        text = "${(animatedProb * 100).roundToInt()}%",
                        fontSize = 32.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = state.accentColor,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        Text("EOD_CORE_SYS_FEED", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = state.textColor.copy(alpha = 0.7f))
                        Text("THREAT LEVEL: ${state.dangerLevel}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = state.textColor.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // The Ping-Pong Marquee
                Text(
                    text = state.directive,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = state.textColor,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)
                )
            }
        }
    }
}