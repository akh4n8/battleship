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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt

// A clean data class to handle the culinary chaos type-safely
data class CookState(
    val chefReview: String,
    val accentColor: Color,
    val textColor: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val heatLevel: Int // 0: Simmer, 1: Boil, 2: Rattle, 3: Fire, 4: Inferno
)

@Composable
fun LetHimCookWidget(probability: Float) {
    val animatedProb by animateFloatAsState(targetValue = probability, tween(1000), label = "CookAnim")
    val scrollState = rememberScrollState()

    // 1. Unified 5-Stage Type-Safe Matrix
    val state = when {
        animatedProb > 0.85f -> CookState(
            chefReview = "Cooking with pure gas! A Michelin Star execution is inbound. Let it simmer.",
            accentColor = Color(0xFF00C853), // Fresh Herb Green
            textColor = Color(0xFF1B5E20),
            bgTop = Color(0xFFE8F5E9), bgBottom = Color(0xFFC8E6C9), // Clean Kitchen White/Green
            heatLevel = 0
        )
        animatedProb > 0.60f -> CookState(
            chefReview = "A steady boil. The broth is reducing perfectly. Do not over-season the strategy.",
            accentColor = Color(0xFF00B0FF), // Gas Flame Blue
            textColor = Color(0xFF01579B),
            bgTop = Color(0xFFE1F5FE), bgBottom = Color(0xFFB3E5FC),
            heatLevel = 1
        )
        animatedProb > 0.40f -> CookState(
            chefReview = "Plating metrics are unstable. The heat is rising too fast. Stir carefully!",
            accentColor = Color(0xFFFF8F00), // Warning Amber
            textColor = Color(0xFFE65100),
            bgTop = Color(0xFFFFF8E1), bgBottom = Color(0xFFFFECB3),
            heatLevel = 2
        )
        animatedProb > 0.15f -> CookState(
            chefReview = "WARNING: Grease fire detected! The broth is spilling over the containment vessel!",
            accentColor = Color(0xFFFF3D00), // Fire Orange
            textColor = Color(0xFFBF360C),
            bgTop = Color(0xFFFFEBEE), bgBottom = Color(0xFFFFCCBC),
            heatLevel = 3
        )
        else -> CookState(
            // FIX: Universal text applied here!
            chefReview = "WHO LET HIM COOK?! COMPLETE SYSTEMIC KITCHEN FIRE INCIDENT. EVACUATE!",
            accentColor = Color(0xFFFF1744), // Critical Red
            textColor = Color(0xFFFFF8E1),
            bgTop = Color(0xFF3E2723), bgBottom = Color(0xFFB71C1C), // Charred Black to Fire Red
            heatLevel = 4
        )
    }

    // 2. Culinary Animation Engines
    val infiniteTransition = rememberInfiniteTransition(label = "KitchenEngine")

    // Ambient steam drifting across the entire background
    val kitchenSteam by infiniteTransition.animateFloat(
        initialValue = -0.5f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "KitchenSteam"
    )

    // Floating background particles (spices/embers)
    val particleRise by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(if (state.heatLevel >= 3) 1500 else 4000, easing = LinearEasing), RepeatMode.Restart),
        label = "ParticleRise"
    )

    // Controls the violent shaking of the pot lid
    val boilRattle by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (state.heatLevel >= 3) 50 else 150, easing = LinearEasing), RepeatMode.Reverse),
        label = "BoilRattle"
    )

    // Controls the height and flicker of the flames
    val flameFlicker by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(200, easing = LinearEasing), RepeatMode.Reverse),
        label = "FlameFlicker"
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

        // --- BACKGROUND AMBIENT LAYER (Steam & Particles) ---
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val steamOffset = w * kitchenSteam

            // The steam turns dark and smoky during a fire
            val steamColor = if (state.heatLevel >= 4) Color.Black else Color.White
            val steamAlpha = if (state.heatLevel >= 4) 0.2f else 0.4f

            // Drifting Steam Cloud 1
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(steamColor.copy(alpha = steamAlpha), Color.Transparent),
                    center = Offset(w * 0.4f + steamOffset, h * 0.5f),
                    radius = h * 0.8f
                ),
                radius = h * 0.8f,
                center = Offset(w * 0.4f + steamOffset, h * 0.5f)
            )

            // Drifting Steam Cloud 2 (Counter-movement)
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(steamColor.copy(alpha = steamAlpha * 0.7f), Color.Transparent),
                    center = Offset(w * 0.8f - steamOffset, h * 0.6f),
                    radius = h
                ),
                radius = h,
                center = Offset(w * 0.8f - steamOffset, h * 0.6f)
            )

            // Rising Background Particles
            val particleCount = 6
            val particleColor = if (state.heatLevel >= 3) Color(0xFFFFEA00) else state.accentColor.copy(alpha = 0.5f)

            for (i in 0 until particleCount) {
                // Stagger the vertical position of each particle
                val normalizedY = (particleRise + (i * 0.16f)) % 1f
                val pY = h * normalizedY

                // Add a slight horizontal sine-wave drift so they don't go straight up
                val waveOffset = kotlin.math.sin((normalizedY + i) * PI * 2).toFloat() * 15f
                val pX = w * (0.15f + (i * 0.14f)) + waveOffset

                // Embers glow, spices/bubbles are solid
                val pRadius = if (state.heatLevel >= 3) 3.dp.toPx() else 2.dp.toPx()

                if (state.heatLevel >= 3) {
                    // Glowing Ember effect
                    drawCircle(particleColor.copy(alpha = 0.4f), radius = pRadius * 2f, center = Offset(pX, pY))
                }
                drawCircle(particleColor, radius = pRadius, center = Offset(pX, pY))
            }
        }

        // --- FOREGROUND CONTENT ---
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // --- KITCHEN CAULDRON SYSTEM CANVAS ---
            Canvas(modifier = Modifier.size(64.dp)) {
                val w = size.width
                val h = size.height

                // 1. Render Fire Threat Engine Beneath Pot (Visible at Heat 3 & 4)
                if (state.heatLevel >= 3) {
                    val fireHeight = if (state.heatLevel == 4) h * 0.6f else h * 0.3f
                    val fHeight = fireHeight * flameFlicker

                    val firePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.15f, h * 0.95f)
                        // Left Flame
                        quadraticBezierTo(w * 0.2f, h - fHeight * 0.8f, w * 0.35f, h - fHeight)
                        quadraticBezierTo(w * 0.4f, h - fHeight * 0.5f, w * 0.5f, h - fHeight * 1.2f)
                        // Right Flame
                        quadraticBezierTo(w * 0.6f, h - fHeight * 0.5f, w * 0.65f, h - fHeight * 0.9f)
                        quadraticBezierTo(w * 0.8f, h - fHeight * 0.8f, w * 0.85f, h * 0.95f)
                        close()
                    }

                    // Draw Outer Orange Flame and Inner Yellow Flame
                    drawPath(firePath, color = Color(0xFFFF3D00))
                    scale(scaleX = 0.6f, scaleY = 0.7f, pivot = Offset(w * 0.5f, h * 0.95f)) {
                        drawPath(firePath, color = Color(0xFFFFEA00))
                    }
                }

                // 2. Draw the Iron Pot Body
                val potPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.15f, h * 0.4f)
                    lineTo(w * 0.85f, h * 0.4f)
                    // Curve down to the base
                    quadraticBezierTo(w * 0.9f, h * 0.85f, w * 0.65f, h * 0.9f)
                    lineTo(w * 0.35f, h * 0.9f)
                    quadraticBezierTo(w * 0.1f, h * 0.85f, w * 0.15f, h * 0.4f)
                }

                // Fill pot body
                drawPath(potPath, color = Color(0xFF263238)) // Dark Iron
                // Pot Highlight for 3D effect
                drawPath(potPath, brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                    startX = w * 0.15f, endX = w * 0.5f
                ))

                // Pot Handles
                drawArc(Color(0xFF263238), startAngle = 90f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.05f, h * 0.45f), size = Size(w * 0.15f, h * 0.2f), style = Stroke(width = 3.dp.toPx()))
                drawArc(Color(0xFF263238), startAngle = 270f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.8f, h * 0.45f), size = Size(w * 0.15f, h * 0.2f), style = Stroke(width = 3.dp.toPx()))

                // 3. Dynamic Bubbling Lid Displacement
                // The rattle intensifies based on the heat level
                val rattleMultiplier = when (state.heatLevel) {
                    0, 1 -> 0f      // Calm / Steady Boil
                    2 -> 1.5f       // Rattle
                    3 -> 3f         // Violent shake
                    else -> 6f      // Blowing off the top!
                }
                val lidOffset = boilRattle.dp.toPx() * rattleMultiplier

                // Draw Lid
                val lidY = (h * 0.38f) + lidOffset
                drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(w * 0.12f, lidY),
                    size = Size(w * 0.76f, h * 0.06f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                // Draw Lid Knob
                drawRoundRect(
                    color = state.accentColor,
                    topLeft = Offset(w * 0.42f, lidY - h * 0.05f),
                    size = Size(w * 0.16f, h * 0.05f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- THE CULINARY READOUT ---
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    // Large numeric read out
                    Text(
                        text = "${(animatedProb * 100).roundToInt()}%",
                        fontSize = 32.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        color = state.accentColor,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        Text("CULINARY METRICS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = state.textColor.copy(alpha = 0.7f), letterSpacing = 1.sp)
                        Text("HEAT LEVEL: ${state.heatLevel}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = state.textColor.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // The Ping-Pong Marquee
                Text(
                    text = state.chefReview,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = state.textColor,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)
                )
            }
        }
    }
}