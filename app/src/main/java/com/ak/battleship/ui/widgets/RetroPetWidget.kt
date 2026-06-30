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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.roundToInt

// A clean data class to hold the pet's mood data
data class PetMoodState(val face: String, val moodText: String, val bgColor: Color, val inkColor: Color)

@Composable
fun RetroPetWidget(probability: Float) {
    val animatedProb by animateFloatAsState(targetValue = probability, tween(800), label = "ProbAnim")

    // 1. Terminal States
    val isVictory = animatedProb >= 1f
    val isDefeat = animatedProb <= 0f
    val isTerminal = isVictory || isDefeat

    // 2. The Multi-Stage Matrix
    val state = when {
        isVictory -> PetMoodState("ᕙ(⌐■_■)ᕗ", "ABSOLUTE CHAMPION", Color(0xFFFFF8E1), Color(0xFFF57F17))
        animatedProb > 0.85f -> PetMoodState("(⌐■_■)", "CRUSHING IT!!", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        animatedProb > 0.60f -> PetMoodState("(•‿•)ﾉ", "Doing great!", Color(0xFFF1F8E9), Color(0xFF558B2F))
        animatedProb > 0.40f -> PetMoodState("(•_•;)", "Slightly tense...", Color(0xFFFFFDE7), Color(0xFFF57F17))
        animatedProb > 0.15f -> PetMoodState("(°Д°)!?", "PANICKING!!!", Color(0xFFFBE9E7), Color(0xFFD84315))
        animatedProb > 0f -> PetMoodState("(╥﹏╥)", "Bury me at sea.", Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> PetMoodState("(x_x)", "SYSTEM FATALITY.", Color(0xFF212121), Color(0xFFD32F2F))
    }

    // 3. Animation Engines
    val infiniteTransition = rememberInfiniteTransition(label = "PetEngine")

    // Smooth physics-like bounce
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "Bounce"
    )

    // Frantic left/right shake for low health
    val jitter by infiniteTransition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(50, easing = LinearEasing), RepeatMode.Reverse),
        label = "Jitter"
    )

    // Slow ghost float upwards for death
    val ghostFloat by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "Ghost"
    )

    // The fluid digital particle stream
    val dataStream by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "DataStream"
    )

    // Calculate final physical Y and X offsets based on health
    val petY = when {
        isDefeat -> -ghostFloat
        animatedProb <= 0.15f -> -(bounce * 4f) + jitter
        isVictory -> -(bounce * 16f)
        else -> -(bounce * 8f)
    }

    val petX = if (animatedProb <= 0.15f && !isDefeat) jitter else 0f

    // THE MASTER CONTAINER
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(state.bgColor)
            // The thick hardware casing border
            .border(4.dp, Color(0xFF2C2C2E), RoundedCornerShape(16.dp))
    ) {

        // --- LAYER 1: DIGITAL HABITAT CANVAS ---
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val floorY = h * 0.75f

            // 1. Fluid Particle Data Stream (Drawn behind the pet and floor)
            val particleCount = 12
            for (i in 0 until particleCount) {
                // Stagger the vertical position
                val normalizedY = (dataStream + (i.toFloat() / particleCount)) % 1f
                val pY = h - (h * normalizedY) // Float upwards

                // Use a Sine wave to create fluid horizontal drifting
                val waveOffset = kotlin.math.sin((normalizedY * PI * 2) + i).toFloat() * 15f
                val pX = (w * (i.toFloat() / particleCount)) + waveOffset

                // Scale the particle based on health (Panic = chaotic large pixels)
                val pSize = if (animatedProb <= 0.15f) (4..8).random().dp.toPx() else 4.dp.toPx()
                val pAlpha = (1f - normalizedY) * 0.3f // Fade out as they reach the top

                drawRect(
                    color = state.inkColor.copy(alpha = pAlpha),
                    topLeft = Offset(pX, pY),
                    size = Size(pSize, pSize)
                )
            }

            // 2. Subtle LCD Scanlines
            for (i in 0..h.toInt() step 4) {
                drawLine(
                    color = state.inkColor.copy(alpha = 0.05f),
                    start = Offset(0f, i.toFloat()),
                    end = Offset(w, i.toFloat()),
                    strokeWidth = 1f
                )
            }

            // 3. The dashed digital floor
            drawLine(
                color = state.inkColor.copy(alpha = 0.3f),
                start = Offset(0f, floorY),
                end = Offset(w, floorY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )

            // 4. Dynamic Pet Cast Shadow
            if (!isDefeat) {
                val jumpHeightMultiplier = if (isVictory) 16f else if (animatedProb <= 0.15f) 4f else 8f
                val currentJumpPercent = (bounce * jumpHeightMultiplier) / 16f

                val shadowWidth = 40.dp.toPx() * (1f - (currentJumpPercent * 0.4f))
                val shadowAlpha = 0.2f * (1f - (currentJumpPercent * 0.5f))

                drawOval(
                    color = state.inkColor.copy(alpha = shadowAlpha),
                    topLeft = Offset((w * 0.25f) - (shadowWidth / 2f), floorY - 2.dp.toPx()),
                    size = Size(shadowWidth, 6.dp.toPx())
                )
            }

            // --- THE CUTOUT OVERLAYS ---

            // 5. Deep Inner Shadow (Creates the recessed bezel illusion)
            drawRoundRect(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                    center = Offset(w / 2f, h / 2f),
                    radius = w * 0.8f
                ),
                size = Size(w, h)
            )

            // 6. Diagonal Glass Glare (Sells the physical screen effect)
            val glarePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.5f, 0f)
                lineTo(w * 0.1f, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = glarePath,
                color = Color.White.copy(alpha = 0.08f)
            )
        }

        // --- LAYER 2: FOREGROUND CONTENT ---
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // THE PET'S OPEN ENCLOSURE
            Box(
                modifier = Modifier.weight(0.9f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 26.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = state.face,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = state.inkColor,
                        modifier = Modifier.offset(x = petX.dp, y = petY.dp)
                    )

                    if (isDefeat) {
                        Text(
                            text = "O",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light,
                            color = state.inkColor.copy(alpha = 0.5f),
                            modifier = Modifier
                                .offset(x = petX.dp, y = (petY - 22).dp)
                                .scale(scaleX = 1.8f, scaleY = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // THE DIALOGUE READOUT
            Column(
                modifier = Modifier.weight(1.2f).padding(vertical = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                val headerText = when {
                    isVictory -> "VIRTUAL PET LINK:"
                    isDefeat -> "PET OFFLINE."
                    else -> "YOUR METER PET SAYS:"
                }

                Text(
                    text = headerText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = state.inkColor.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = state.moodText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = state.inkColor,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (!isTerminal) {
                    val activeHearts = (animatedProb * 4).roundToInt().coerceIn(0, 4)
                    val hearts = "♥ ".repeat(activeHearts)
                    val brokenHearts = "♡ ".repeat(4 - activeHearts)

                    Text(
                        text = hearts + brokenHearts,
                        fontSize = 14.sp,
                        color = if (animatedProb <= 0.15f) Color(0xFFE53935) else state.inkColor,
                        letterSpacing = 2.sp
                    )
                } else {
                    Text(
                        text = if (isVictory) "EXP +9999" else "GAME OVER",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = state.inkColor
                    )
                }
            }
        }
    }
}