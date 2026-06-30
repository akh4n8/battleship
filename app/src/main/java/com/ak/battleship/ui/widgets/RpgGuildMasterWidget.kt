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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// A clean data class to handle the retro state type-safely
data class RpgState(
    val avatar: String,
    val dialogueText: String,
    val themeColor: Color,
    val statusLabel: String
)

@Composable
fun RpgGuildMasterWidget(probability: Float) {
    val animatedProb by animateFloatAsState(targetValue = probability, tween(800), label = "RpgAnim")
    val scrollState = rememberScrollState()

    // 1. Unified 5-Stage Type-Safe Matrix
    val state = when {
        animatedProb > 0.85f -> RpgState(
            avatar = "[⌐■_■]",
            dialogueText = "Pure perfection. I've seen training drones put up a better fight. Roll right over them.",
            themeColor = Color(0xFF00FF66), // Retro Neon Green
            statusLabel = "DOMINATING"
        )
        animatedProb > 0.60f -> RpgState(
            avatar = "[•‿• ]",
            dialogueText = "Calculations check out. Fleet positioning is mathematically sound. Maintain this rhythm.",
            themeColor = Color(0xFF00E5FF), // Cyber Cyan
            statusLabel = "ADVANTAGE"
        )
        animatedProb > 0.40f -> RpgState(
            avatar = "[•_• ]",
            dialogueText = "A bit sluggish on the draw, but parameters are stable. Just don't do anything stupid.",
            themeColor = Color(0xFFFFEA00), // High-Voltage Yellow
            statusLabel = "STALEMATE"
        )
        animatedProb > 0.15f -> RpgState(
            avatar = "[ಠ_ಠ ]",
            dialogueText = "Are your targeting sensors inverted, or are you just testing the water tension? Fix it!",
            themeColor = Color(0xFFFF9100), // Warning Orange
            statusLabel = "WARNING"
        )
        else -> RpgState(
            avatar = "[╥﹏╥]",
            dialogueText = "Absolute systemic catastrophe! Tell the engine room I hate them all. Scrap the boat. GG.",
            themeColor = Color(0xFFFF1744), // Critical Red
            statusLabel = "FATALITY"
        )
    }

    // 2. Retro Animation Engines
    val infiniteTransition = rememberInfiniteTransition(label = "RpgEngine")

    // Jitters the character's portrait faster the lower your odds get
    val portraitJitter by infiniteTransition.animateFloat(
        initialValue = -1.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (animatedProb <= 0.15f) 60 else 120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Jitter"
    )

    // Smooth text terminal cursor pulse
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "Cursor"
    )

    // 3. The Crawl, Pause, and Teleport-Reset Marquee Engine
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            while (true) {
                delay(1500) // Pause at start
                scrollState.animateScrollTo(scrollState.maxValue, tween(3500, easing = LinearEasing))
                delay(1500) // Pause at end
                scrollState.scrollTo(0) // Teleport back instantly
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            // Authentic 16-bit double nested frame styling
            .border(4.dp, Color(0xFF212124), RoundedCornerShape(0.dp))
            .border(2.dp, state.themeColor, RoundedCornerShape(0.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- LAYER 1: CRT FACEPORT BOX ---
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.Black)
                .border(2.dp, Color(0xFF323238))
                // Apply spatial jittering to simulate pain/panic at low odds
                .offset(
                    x = if (animatedProb <= 0.40f) (portraitJitter * 0.5f).dp else 0.dp,
                    y = if (animatedProb <= 0.40f) portraitJitter.dp else 0.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.avatar,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = state.themeColor
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // --- LAYER 2: SYSTEM COMBAT LOGGER ---
        Column(modifier = Modifier.weight(1f)) {
            // Retro Header Row with Stats Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "TACTICAL_CMDR v1.0",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // The Big Data Number: Styled like arcade damage fonts
                    Text(
                        text = "${(animatedProb * 100).roundToInt()}% COMBAT_EFF",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = state.themeColor,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Text status flag box
                Text(
                    text = "[${state.statusLabel}]",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = state.themeColor,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Authentic Segmented Health/XP Progress Bar
            Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                val segments = 10
                val spacing = 2.dp.toPx()
                val totalWidth = size.width
                val segmentWidth = (totalWidth - (spacing * (segments - 1))) / segments
                val filledSegments = (animatedProb * segments).roundToInt()

                for (i in 0 until segments) {
                    val x = i * (segmentWidth + spacing)
                    val color = if (i < filledSegments) state.themeColor else Color(0xFF1C1C22)
                    drawRect(
                        color = color,
                        topLeft = Offset(x, 0f),
                        size = Size(segmentWidth, size.height)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dialogue Box Row with flashing cursor block
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "> ${state.dialogueText}",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState)
                )
                Text(
                    text = "█",
                    fontSize = 12.sp,
                    color = state.themeColor.copy(alpha = cursorAlpha),
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}