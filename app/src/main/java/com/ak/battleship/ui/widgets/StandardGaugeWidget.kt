package com.ak.battleship.ui.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.model.PlaybackCommand
import kotlin.math.roundToInt

@Composable
fun StandardGaugeWidget(
    probability: Float,
    command: PlaybackCommand = PlaybackCommand.IDLE,
    commandKey: Int = 0
) {
    // THE REVAMP: Orchestrated Probability State
    val internalProb = remember { Animatable(0.5f) }

    LaunchedEffect(probability, command, commandKey) {
        when (command) {
            PlaybackCommand.PLAY_WIN_CINEMATIC, PlaybackCommand.PLAY_LOSS_CINEMATIC -> {
                // FORCE the sweep from 50%
                internalProb.snapTo(0.5f)
                internalProb.animateTo(probability, tween(1000))
            }
            PlaybackCommand.PLAY_JUMP -> {
                // Instant snap for scrubbing
                internalProb.snapTo(probability)
            }
            else -> {
                // Standard smooth tracking for live gameplay
                internalProb.animateTo(probability, tween(800))
            }
        }
    }

    val animatedProb = internalProb.value

    // Detect if the gauge has mathematically reached the end of its animation
    val isVictory = animatedProb >= 1f
    val isDefeat = animatedProb <= 0f
    val isTerminal = isVictory || isDefeat

    // A smooth breathing pulse that ONLY activates when the game is over
    val infiniteTransition = rememberInfiniteTransition(label = "TerminalPulse")
    val terminalPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "Pulse"
    )

    // The 7-Stage matrix (Now including 0% and 100% terminal states!)
    val (statusText, baseColor, headerText) = when {
        isVictory -> Triple("TACTICAL VICTORY SECURED. EXCELLENT WORK.", Color(0xFFC89B3C), "MATCH CONCLUDED")
        animatedProb > 0.85f -> Triple("Opponent is Panicking!", Color(0xFF388E3C), "Historical Win Prob.")
        animatedProb > 0.60f -> Triple("Looking Good Commander", Color(0xFF4CAF50), "Historical Win Prob.")
        animatedProb > 0.40f -> Triple("A Dead Heat...", Color(0xFFFFC107), "Historical Win Prob.")
        animatedProb > 0.15f -> Triple("Taking Heavy Fire", Color(0xFFFF5722), "Historical Win Prob.")
        animatedProb > 0f -> Triple("Critical Condition!", Color(0xFFD32F2F), "Historical Win Prob.")
        else -> Triple("TOTAL FLEET ANNIHILATION. SYSTEM OFFLINE.", Color(0xFF8B0000), "MATCH CONCLUDED") // Deep Blood Red
    }

    // Apply the pulsing glow if the game is over, otherwise keep the color solid
    val finalColor = if (isTerminal) baseColor.copy(alpha = terminalPulse) else baseColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // --- THE GAUGE ---
        Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )

                // If it's a victory, we add a glowing shadow behind the gauge
                if (isVictory) {
                    drawArc(
                        color = finalColor.copy(alpha = 0.4f),
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProb,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Active Foreground Track
                drawArc(
                    color = finalColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProb,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Swap out the percentage for text when the game ends!
            val centerText = when {
                isVictory -> "WIN"
                isDefeat -> "LOSS"
                else -> "${(animatedProb * 100).roundToInt()}%"
            }

            Text(
                text = centerText,
                fontSize = if (isTerminal) 18.sp else 16.sp,
                fontWeight = FontWeight.Black,
                color = finalColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- THE READOUT ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headerText,
                fontSize = 12.sp,
                color = if (isTerminal) finalColor else Color.Gray, // Header glows too!
                fontWeight = FontWeight.Bold
            )
            Text(
                text = statusText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = finalColor,
                maxLines = 1,
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    repeatDelayMillis = 1500,
                    velocity = 30.dp
                )
            )
        }
    }
}
