package com.ak.battleship.ui.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.model.PlaybackCommand
import kotlin.math.roundToInt

@Composable
fun WallStreetWidget(
    probability: Float,
    stats: List<Pair<String, String>>,
    command: PlaybackCommand = PlaybackCommand.IDLE,
    commandKey: Int = 0
) {
    // THE REVAMP: Orchestrated Probability State
    val internalProb = remember { Animatable(0.5f) }
    LaunchedEffect(probability, command, commandKey) {
        when (command) {
            PlaybackCommand.PLAY_WIN_CINEMATIC, PlaybackCommand.PLAY_LOSS_CINEMATIC -> {
                internalProb.snapTo(0.5f)
                internalProb.animateTo(probability, tween(1000))
            }
            PlaybackCommand.PLAY_JUMP -> internalProb.snapTo(probability)
            else -> internalProb.animateTo(probability, tween(800))
        }
    }
    val animatedProb = internalProb.value

    // 1. TRACK TRUE HISTORY & ACCUMULATE VISUAL VOLATILITY
    var probHistory by remember { mutableStateOf(listOf(0.5f)) }
    var visualHistory by remember { mutableStateOf(listOf(0.5f)) } // The fake, exaggerated graph data

    LaunchedEffect(probability) {
        val lastActual = probHistory.lastOrNull() ?: 0.5f

        if (probability != lastActual) {
            val rawDelta = probability - lastActual

            // Exaggerate the delta: Big moves get a 4x multiplier, small moves get 1.5x
            val volatilityMultiplier = if (kotlin.math.abs(rawDelta) > 0.05f) 4.0f else 1.5f

            // THE FIX: Add the massive swing to the LAST PLOTTED POINT.
            // This guarantees that if rawDelta is positive, the line ALWAYS draws upward!
            val lastVisual = visualHistory.lastOrNull() ?: 0.5f
            val newVisual = lastVisual + (rawDelta * volatilityMultiplier)

            probHistory = probHistory + probability
            visualHistory = visualHistory + newVisual
        }
    }

    val maxDataPoints = 15
    val displayHistory = visualHistory.takeLast(maxDataPoints)

    // 2. DYNAMIC VOLATILITY SCALING (Auto-Zoom)
    val vMin = displayHistory.minOrNull() ?: 0f
    val vMax = displayHistory.maxOrNull() ?: 1f

    // We can loosen the spread back up to 0.1f since the visual numbers are naturally massive now!
    val spread = (vMax - vMin).coerceAtLeast(0.1f)
    val vMid = (vMax + vMin) / 2f
    val yMin = vMid - (spread / 2f)
    val yMax = vMid + (spread / 2f)

    val tickerString = remember(stats) {
        stats.joinToString("   •   ") { "${it.first} ${it.second}" } + "   •   "
    }

    val infiniteTransition = rememberInfiniteTransition(label = "MarketAnim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "Pulse"
    )

    val isBullish = animatedProb >= 0.5f
    val themeColor = if (isBullish) Color(0xFF00E676) else Color(0xFFFF1744)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0D1117))
            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(6.dp))
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("BTLSHP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Canvas(modifier = Modifier.size(6.dp)) { drawCircle(color = Color.Red.copy(alpha = pulseAlpha)) }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$${(animatedProb * 1000).roundToInt()}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = themeColor,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBullish) "▲" else "▼",
                    color = themeColor,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        // --- NATIVE AUTO-SCALING CHART ---
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {

                // Background Grid Lines
                for (i in 1..3) {
                    val y = size.height * (i / 4f)
                    drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y), 1f)
                }

                // SWAPPED: Now reading directly from displayHistory
                if (displayHistory.size > 1) {
                    val path = androidx.compose.ui.graphics.Path()

                    val pointsToDraw = maxOf(displayHistory.size - 1, 1)
                    val xStep = size.width / pointsToDraw.toFloat()

                    var lastX = 0f
                    var lastY = 0f

                    // SWAPPED: Iterate over displayHistory
                    displayHistory.forEachIndexed { index, prob ->
                        val x = index * xStep
                        val normalizedY = (prob - yMin) / (yMax - yMin)
                        val y = size.height - (size.height * normalizedY.toFloat()).coerceIn(0f, size.height)

                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                        lastX = x
                        lastY = y
                    }

                    // 1. Draw the translucent Under-Glow Fill
                    val fillPath = androidx.compose.ui.graphics.Path().apply {
                        addPath(path)
                        lineTo(lastX, size.height)
                        lineTo(0f, size.height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(themeColor.copy(alpha = 0.35f), Color.Transparent),
                            startY = 0f, endY = size.height
                        )
                    )

                    // 2. Draw the Sharp Data Line
                    drawPath(
                        path = path,
                        color = themeColor,
                        // Using Miter joints keeps the peaks mathematically sharp and aggressive
                        style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Miter)
                    )

                    // 3. Draw the live "Current Price" glowing dot
                    drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(lastX, lastY))
                    drawCircle(themeColor, radius = 1.5.dp.toPx(), center = Offset(lastX, lastY))
                }
            }
        }

        // --- SINGLE LINE TICKER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF010409))
                .border(1.dp, Color(0xFF30363D))
                .padding(vertical = 4.dp)
                .clipToBounds()
        ) {
            Text(
                text = tickerString,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 0,
                        initialDelayMillis = 0,
                        velocity = 40.dp
                    )
            )
        }
    }
}
