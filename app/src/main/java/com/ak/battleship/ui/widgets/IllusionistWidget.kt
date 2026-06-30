package com.ak.battleship.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.model.PlaybackCommand
import com.ak.battleship.model.ShotOutcome
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos

// --- NEW: Magic Theme State for 7 Stages ---
data class MagicThemeState(
    val status: String,
    val stageLabel: String,
    val themeColor: Color,
    val apparatus: MagicApparatus
)

enum class MagicApparatus {
    GRAND_FINALE,       // 1.0
    THE_PRESTIGE,       // > 0.85
    THE_TURN,           // > 0.60
    THE_PLEDGE,         // > 0.40
    MISDIRECTION,      // > 0.15
    TRICK_EXPOSED,      // > 0
    VANISHED            // 0.0
}

@Preview
@Composable
fun IllusionistWidgetPreview() {
    Box(modifier = Modifier.size(300.dp, 120.dp)) {
        IllusionistWidget(
            probability = 0.9f,
            shotOutcome = ShotOutcome.SUNK,
            commandKey = 1,
            command = PlaybackCommand.PLAY_SUNK
        )
    }
}

@Composable
fun IllusionistWidget(
    probability: Float,
    shotOutcome: ShotOutcome = ShotOutcome.MISS,
    commandKey: Int = 0,
    isPlayerTurn: Boolean = true,
    command: PlaybackCommand = PlaybackCommand.IDLE
) {
    // --- CORE MAGIC ENGINE ---
    val internalProb = remember { Animatable(0.5f) }
    val magicSequence = remember { Animatable(0f) }
    val smokeAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "MystiqueEngine")

    // Ambient Floating Motion for the Apparatus
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Float"
    )

    // Sparkle Flicker
    val sparklePulse by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "Sparkle"
    )

    LaunchedEffect(probability, command, commandKey) {
        when (command) {
            PlaybackCommand.PLAY_WIN_CINEMATIC, PlaybackCommand.PLAY_LOSS_CINEMATIC -> {
                internalProb.snapTo(0.5f)
                internalProb.animateTo(probability, tween(1500, easing = FastOutSlowInEasing))
            }
            PlaybackCommand.PLAY_JUMP -> internalProb.snapTo(probability)
            else -> internalProb.animateTo(probability, tween(1000))
        }
    }
    val animatedProb = internalProb.value

    LaunchedEffect(commandKey) {
        if (commandKey > 0 && command != PlaybackCommand.PLAY_JUMP) {
            magicSequence.snapTo(0f)
            smokeAlpha.snapTo(0.8f)

            smokeAlpha.animateTo(0f, tween(600))
            magicSequence.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
        }
    }

    // --- THE SEVEN STAGES OF MAGIC ---
    val targetTheme = when {
        animatedProb >= 1f -> MagicThemeState(
            "GRAND ILLUSION", "THE MASTER REVEALED", Color(0xFFFFD700), MagicApparatus.GRAND_FINALE
        )
        animatedProb > 0.85f -> MagicThemeState(
            "THE PRESTIGE", "GRAND FINALE READY", Color(0xFFE040FB), MagicApparatus.THE_PRESTIGE
        )
        animatedProb > 0.60f -> MagicThemeState(
            "THE TURN", "ILLUSION IN PROGRESS", Color(0xFF7C4DFF), MagicApparatus.THE_TURN
        )
        animatedProb > 0.40f -> MagicThemeState(
            "THE PLEDGE", "MISDIRECTION ACTIVE", Color(0xFF448AFF), MagicApparatus.THE_PLEDGE
        )
        animatedProb > 0.15f -> MagicThemeState(
            "MISDIRECTION", "THE EYE IS DECEIVED", Color(0xFF03DAC5), MagicApparatus.MISDIRECTION
        )
        animatedProb > 0f -> MagicThemeState(
            "TRICK EXPOSED", "THE VEIL IS TORN", Color(0xFFFF5252), MagicApparatus.TRICK_EXPOSED
        )
        else -> MagicThemeState(
            "VANISHED", "WITHOUT A TRACE", Color(0xFFD32F2F), MagicApparatus.VANISHED
        )
    }

    val themeColor by animateColorAsState(targetValue = targetTheme.themeColor, tween(1500), label = "ThemeColor")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A0A0A))
            .border(2.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f

            // 1. BACKGROUND: The Astral Plane
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(themeColor.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = w * 0.8f
                )
            )

            // Sparkle System
            for (i in 0..20) {
                val seed = i * 45.67f
                val sX = (sin(seed) * 0.5f + 0.5f) * w
                val sY = (cos(seed * 0.8f) * 0.5f + 0.5f) * h
                val sAlpha = (sin(System.currentTimeMillis() * 0.002f + i) * 0.5f + 0.5f) * sparklePulse * 0.4f
                drawCircle(themeColor, 1.dp.toPx(), Offset(sX, sY), alpha = sAlpha)
            }

            // 2. THE MAGICAL APPARATUS: Stage-specific visuals
            val apparatusY = centerY + 10.dp.toPx() + floatAnim.dp.toPx()

            when (targetTheme.apparatus) {
                MagicApparatus.GRAND_FINALE -> drawGrandFinale(centerX, apparatusY, themeColor)
                MagicApparatus.THE_PRESTIGE -> drawTopHat(centerX, apparatusY, themeColor)
                MagicApparatus.THE_TURN -> drawCrystalBall(centerX, apparatusY, themeColor)
                MagicApparatus.THE_PLEDGE -> drawCardDeck(centerX, apparatusY)
                MagicApparatus.MISDIRECTION -> drawMagicWandApparatus(centerX, apparatusY, themeColor)
                MagicApparatus.TRICK_EXPOSED -> drawBrokenWand(centerX, apparatusY)
                MagicApparatus.VANISHED -> {} // Empty stage
            }

            // 3. COMBAT SEQUENCES: The Magic in Action
            if (magicSequence.value > 0f) {
                val p = magicSequence.value
                when (shotOutcome) {
                    ShotOutcome.HIT -> {
                        drawEnchantedDagger(centerX, apparatusY - 40.dp.toPx() * p, p, themeColor)
                    }
                    ShotOutcome.SUNK -> {
                        drawMagicDove(centerX + (p * 100.dp.toPx() * (if(isPlayerTurn) 1 else -1)), apparatusY - (p * 80.dp.toPx()), p)
                    }
                    ShotOutcome.MISS -> {
                        drawMagicWandAction(centerX, apparatusY - 20.dp.toPx(), p, themeColor)
                    }
                }
            }

            // 4. SMOKE BURST
            if (smokeAlpha.value > 0f) {
                drawCircle(
                    Color.White.copy(alpha = smokeAlpha.value * 0.5f),
                    radius = 40.dp.toPx() * (1f - smokeAlpha.value),
                    center = Offset(centerX, apparatusY - 20.dp.toPx())
                )
            }
        }

        // 5. HUD LAYER: The Mystic Display
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = targetTheme.status,
                color = themeColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = targetTheme.stageLabel,
                color = Color.Gray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = "${(animatedProb * 100).roundToInt()}% MYSTIQUE",
                color = themeColor.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// --- HELPER DRAWING FUNCTIONS ---

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrandFinale(x: Float, y: Float, color: Color) {
    val size = 40.dp.toPx()
    val yOffset = 20.dp.toPx()
    // A golden sunburst effect
    for (i in 0 until 8) {
        val angle = (i * 45f) * (PI.toFloat() / 180f)
        drawLine(
            color = color,
            start = Offset(x, y - yOffset),
            end = Offset(x + cos(angle) * size, y - yOffset + sin(angle) * size),
            strokeWidth = 3.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
    drawTopHat(x, y, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTopHat(x: Float, y: Float, color: Color) {
    val hatW = 50.dp.toPx()
    val hatH = 40.dp.toPx()
    val brimW = 70.dp.toPx()
    val bandH = 8.dp.toPx()
    val bandYOffset = 10.dp.toPx()
    val brimYOffset = 5.dp.toPx()
    val brimH = 10.dp.toPx()

    drawRect(Color(0xFF1A1A1A), Offset(x - hatW / 2, y - hatH), Size(hatW, hatH))
    drawRect(Color.Black, Offset(x - brimW / 2, y - brimYOffset), Size(brimW, brimH))
    drawRect(color, Offset(x - hatW / 2, y - bandYOffset), Size(hatW, bandH))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrystalBall(x: Float, y: Float, color: Color) {
    val radius = 25.dp.toPx()
    val baseW = 15.dp.toPx()
    val baseH = 10.dp.toPx()
    
    drawPath(
        path = Path().apply {
            moveTo(x - baseW, y)
            lineTo(x + baseW, y)
            lineTo(x + 10.dp.toPx(), y - baseH)
            lineTo(x - 10.dp.toPx(), y - baseH)
            close()
        },
        color = Color(0xFF333333)
    )
    drawCircle(
        brush = androidx.compose.ui.graphics.Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.6f), color.copy(alpha = 0.2f), Color.Transparent),
            center = Offset(x, y - radius),
            radius = radius
        ),
        radius = radius,
        center = Offset(x, y - radius)
    )
    drawCircle(color.copy(alpha = 0.8f), radius * 0.4f, Offset(x + sin(System.currentTimeMillis() * 0.005f) * 5f, y - radius + cos(System.currentTimeMillis() * 0.003f) * 5f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCardDeck(x: Float, y: Float) {
    val cardW = 30.dp.toPx()
    val cardH = 45.dp.toPx()
    val offsetStep = 2.dp.toPx()
    
    for (i in 0..3) {
        drawRoundRect(
            color = if (i == 3) Color.White else Color.Gray,
            topLeft = Offset(x - cardW / 2 + (i * offsetStep), y - cardH - (i * offsetStep)),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = if (i == 3) Stroke(2f) else Fill
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMagicWandApparatus(x: Float, y: Float, color: Color) {
    val tipXOffset = 20.dp.toPx()
    val tipYOffset = 20.dp.toPx()
    val wandL = 40.dp.toPx()
    val wandW = 4.dp.toPx()
    val tipL = 10.dp.toPx()
    
    val tipX = x + tipXOffset
    val tipY = y - tipYOffset
    withTransform({
        translate(x, y - tipYOffset)
        rotate(45f)
    }) {
        drawRect(Color.Black, Offset(-wandL / 2, -wandW / 2), Size(wandL, wandW))
        drawRect(Color.White, Offset(wandL / 2 - tipL, -wandW / 2), Size(tipL, wandW))
    }
    drawCircle(color.copy(alpha = 0.5f), 5.dp.toPx(), Offset(tipX, tipY))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBrokenWand(x: Float, y: Float) {
    val part1X = x - 10.dp.toPx()
    val part1Y = y - 20.dp.toPx()
    val part2X = x + 10.dp.toPx()
    val part2Y = y - 25.dp.toPx()
    
    val wandPartL = 20.dp.toPx()
    val wandW = 4.dp.toPx()
    val tipL = 5.dp.toPx()

    withTransform({ translate(part1X, part1Y); rotate(-30f) }) {
        drawRect(Color.Black, Offset(-wandPartL / 2, -wandW / 2), Size(wandPartL, wandW))
    }
    withTransform({ translate(part2X, part2Y); rotate(30f) }) {
        drawRect(Color.Black, Offset(-wandPartL / 2, -wandW / 2), Size(wandPartL, wandW))
        drawRect(Color.White, Offset(wandPartL / 2 - tipL, -wandW / 2), Size(tipL, wandW))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEnchantedDagger(x: Float, y: Float, p: Float, color: Color) {
    withTransform({
        translate(x, y)
        rotate(p * 720f)
        scale(1.2f, 1.2f)
    }) {
        val daggerPath = Path().apply {
            moveTo(0f, -20f)
            lineTo(5f, 0f)
            lineTo(2f, 0f)
            lineTo(2f, 10f)
            lineTo(-2f, 10f)
            lineTo(-2f, 0f)
            lineTo(-5f, 0f)
            close()
        }
        drawPath(daggerPath, color)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMagicDove(x: Float, y: Float, p: Float) {
    val wingSway = sin(p * 20f) * 10f
    withTransform({
        translate(x, y)
        scale(0.8f + p * 0.4f, 0.8f + p * 0.4f)
    }) {
        val body = Path().apply {
            moveTo(0f, 0f)
            quadraticTo(10f, -5f, 20f, 0f)
            quadraticTo(10f, 5f, 0f, 0f)
        }
        val leftWing = Path().apply {
            moveTo(10f, 0f)
            quadraticTo(0f, -15f + wingSway, -10f, 0f)
        }
        val rightWing = Path().apply {
            moveTo(10f, 0f)
            quadraticTo(20f, -15f - wingSway, 30f, 0f)
        }
        drawPath(body, Color.White)
        drawPath(leftWing, Color.White, style = Stroke(2.dp.toPx()))
        drawPath(rightWing, Color.White, style = Stroke(2.dp.toPx()))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMagicWandAction(x: Float, y: Float, p: Float, color: Color) {
    val wandL = 40.dp.toPx()
    val wandW = 4.dp.toPx()
    val tipL = 10.dp.toPx()
    
    withTransform({
        translate(x, y)
        rotate(sin(p * PI.toFloat()) * 45f)
    }) {
        drawRect(Color.Black, Offset(-wandL / 2, -wandW / 2), Size(wandL, wandW))
        drawRect(Color.White, Offset(wandL / 2 - tipL, -wandW / 2), Size(tipL, wandW))
        if (p > 0.5f) {
            drawCircle(color, 4.dp.toPx() * (p - 0.5f) * 2f, Offset(wandL / 2, 0f))
        }
    }
}
