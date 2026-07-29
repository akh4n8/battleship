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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class MagicThemeState(
    val status: String,
    val stageLabel: String,
    val themeColor: Color,
    val apparatus: MagicApparatus
)

enum class MagicApparatus {
    GRAND_FINALE,
    THE_PRESTIGE,
    THE_TURN,
    THE_PLEDGE,
    MISDIRECTION,
    TRICK_EXPOSED,
    VANISHED
}

@Preview
@Composable
fun IllusionistWidgetPreview() {
    Box(modifier = Modifier.size(350.dp, 120.dp)) {
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
    val internalProb = remember { Animatable(0.5f) }
    val magicSequence = remember { Animatable(0f) }
    val smokeAlpha = remember { Animatable(0f) }
    val combatPulse = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "MystiqueEngine")

    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "Float"
    )

    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "Aura"
    )
    
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing), RepeatMode.Restart),
        label = "Time"
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
            smokeAlpha.snapTo(0.9f)
            combatPulse.snapTo(1f)

            smokeAlpha.animateTo(0f, tween(800, easing = EaseOutCubic))
            combatPulse.animateTo(0f, tween(500, easing = EaseOutQuad))
            magicSequence.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }

    val targetTheme = when {
        animatedProb >= 1f -> MagicThemeState("GRAND ILLUSION", "THE MASTER REVEALED", Color(0xFFFFD700), MagicApparatus.GRAND_FINALE)
        animatedProb > 0.85f -> MagicThemeState("THE PRESTIGE", "GRAND FINALE READY", Color(0xFFE040FB), MagicApparatus.THE_PRESTIGE)
        animatedProb > 0.60f -> MagicThemeState("THE TURN", "ILLUSION IN PROGRESS", Color(0xFF7C4DFF), MagicApparatus.THE_TURN)
        animatedProb > 0.40f -> MagicThemeState("THE PLEDGE", "MISDIRECTION ACTIVE", Color(0xFF448AFF), MagicApparatus.THE_PLEDGE)
        animatedProb > 0.15f -> MagicThemeState("MISDIRECTION", "THE EYE IS DECEIVED", Color(0xFF03DAC5), MagicApparatus.MISDIRECTION)
        animatedProb > 0f -> MagicThemeState("TRICK EXPOSED", "THE VEIL IS TORN", Color(0xFFFF5252), MagicApparatus.TRICK_EXPOSED)
        else -> MagicThemeState("VANISHED", "WITHOUT A TRACE", Color(0xFFD32F2F), MagicApparatus.VANISHED)
    }

    val themeColor by animateColorAsState(targetValue = targetTheme.themeColor, tween(1000), label = "ThemeColor")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0C0A10)) // Dark ambient backstage color
            .border(2.dp, themeColor.copy(alpha = 0.4f + combatPulse.value * 0.4f), RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f

            // 1. STAGE & CURTAINS
            drawStageAndCurtains(w, h, time)

            // 2. ADVANCED PARTICLE SYSTEM (MAGIC DUST)
            for (i in 0..20) {
                val seed = i * 13.37f
                val speed = (i % 3 + 1) * 0.05f
                val angle = (time * speed + seed) % (2 * PI)
                val radius = (20.dp.toPx() + (i * 2).dp.toPx()) * (1f + sin(time * 0.01f + seed) * 0.2f)
                val pX = centerX + cos(angle.toFloat()) * radius
                val pY = centerY + sin(angle.toFloat()) * radius
                val pAlpha = (sin(time * 0.1f + seed) * 0.5f + 0.5f) * 0.8f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(themeColor.copy(alpha = pAlpha), Color.Transparent),
                        center = Offset(pX, pY),
                        radius = 4.dp.toPx()
                    ),
                    center = Offset(pX, pY),
                    radius = 4.dp.toPx()
                )
            }

            // 3. THE APPARATUS
            val apparatusY = centerY + floatAnim.dp.toPx()

            when (targetTheme.apparatus) {
                MagicApparatus.GRAND_FINALE -> drawGrandFinale(centerX, apparatusY, themeColor, time)
                MagicApparatus.THE_PRESTIGE -> drawPremiumTopHat(centerX, apparatusY, themeColor)
                MagicApparatus.THE_TURN -> drawMysticCrystalBall(centerX, apparatusY, themeColor, time)
                MagicApparatus.THE_PLEDGE -> drawFloatingCards(centerX, apparatusY, time)
                MagicApparatus.MISDIRECTION -> drawElegantWand(centerX, apparatusY, themeColor, time)
                MagicApparatus.TRICK_EXPOSED -> drawShatteredWand(centerX, apparatusY, themeColor)
                MagicApparatus.VANISHED -> drawVanishingSmoke(centerX, apparatusY, themeColor, time)
            }

            // 4. COMBAT SEQUENCES (The Magic Action)
            if (magicSequence.value > 0f) {
                val p = magicSequence.value
                when (shotOutcome) {
                    ShotOutcome.HIT -> {
                        drawSpectralBlades(centerX, apparatusY, p, themeColor, isPlayerTurn)
                    }
                    ShotOutcome.SUNK -> {
                        drawEtherealPhoenix(centerX, apparatusY, p, themeColor, isPlayerTurn)
                    }
                    ShotOutcome.MISS -> {
                        drawMissDeflection(centerX, apparatusY, p, themeColor)
                    }
                }
            }

            // 5. SMOKE BURST (Transition Effect)
            if (smokeAlpha.value > 0f) {
                for (i in 0..5) {
                    val angle = (i * 60f) * (PI.toFloat() / 180f)
                    val expansion = 50.dp.toPx() * (1f - smokeAlpha.value)
                    val sx = centerX + cos(angle) * expansion
                    val sy = apparatusY + sin(angle) * expansion
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = smokeAlpha.value * 0.5f), Color.Transparent),
                            center = Offset(sx, sy),
                            radius = 35.dp.toPx() * (1f - smokeAlpha.value)
                        ),
                        center = Offset(sx, sy),
                        radius = 35.dp.toPx() * (1f - smokeAlpha.value)
                    )
                }
            }
        }

        // 6. PREMIUM HUD
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = targetTheme.status,
                color = themeColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.Monospace,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = Offset(0f, 4f),
                        blurRadius = 4f
                    )
                )
            )
            Text(
                text = targetTheme.stageLabel,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black, blurRadius = 2f)
                )
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${(animatedProb * 100).roundToInt()}% MYSTIQUE",
                color = themeColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// --- HIGH-FIDELITY DRAWING FUNCTIONS ---

private fun DrawScope.drawStageAndCurtains(w: Float, h: Float, time: Float) {
    val curtainRed = Color(0xFF6B0000)
    val curtainShadow = Color(0xFF2E0000)
    val goldTrim = Color(0xFFFFD700)
    val stageWood = Color(0xFF2E1503)
    val floorHeight = h * 0.25f

    // 1. Stage Floor
    val floorPath = Path().apply {
        moveTo(0f, h - floorHeight)
        lineTo(w, h - floorHeight)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(floorPath, Brush.verticalGradient(listOf(stageWood, Color.Black), startY = h - floorHeight, endY = h))

    // 2. Spotlight
    val spotSway = sin(time * 0.05f) * w * 0.1f
    val spotPath = Path().apply {
        moveTo(w / 2 + spotSway * 0.2f, -20f)
        lineTo(w * 0.2f + spotSway, h - floorHeight + 20f)
        lineTo(w * 0.8f + spotSway, h - floorHeight + 20f)
        close()
    }
    drawPath(spotPath, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent), startY = 0f, endY = h - floorHeight))
    
    // Spotlight Floor ellipse
    drawOval(
        color = Color.White.copy(alpha = 0.08f),
        topLeft = Offset(w * 0.2f + spotSway, h - floorHeight),
        size = Size(w * 0.6f, floorHeight * 0.8f)
    )

    // 3. Left Curtain (Draped)
    val leftCurtain = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.35f, 0f)
        quadraticTo(w * 0.2f, h * 0.5f, w * 0.05f, h - floorHeight)
        lineTo(0f, h - floorHeight)
        close()
    }
    drawPath(leftCurtain, Brush.horizontalGradient(listOf(curtainShadow, curtainRed, curtainShadow), startX = 0f, endX = w * 0.35f))
    
    // Left Curtain folds
    drawPath(leftCurtain, Color.Black.copy(alpha = 0.3f), style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 20f))))

    // 4. Right Curtain (Draped)
    val rightCurtain = Path().apply {
        moveTo(w, 0f)
        lineTo(w * 0.65f, 0f)
        quadraticTo(w * 0.8f, h * 0.5f, w * 0.95f, h - floorHeight)
        lineTo(w, h - floorHeight)
        close()
    }
    drawPath(rightCurtain, Brush.horizontalGradient(listOf(curtainShadow, curtainRed, curtainShadow), startX = w * 0.65f, endX = w))
    
    // 5. Top Valance (Scalloped)
    val valance = Path().apply {
        moveTo(0f, 0f)
        lineTo(w, 0f)
        lineTo(w, h * 0.18f)
        quadraticTo(w * 0.85f, h * 0.28f, w * 0.7f, h * 0.18f)
        quadraticTo(w * 0.5f, h * 0.32f, w * 0.3f, h * 0.18f)
        quadraticTo(w * 0.15f, h * 0.28f, 0f, h * 0.18f)
        close()
    }
    drawPath(valance, Brush.verticalGradient(listOf(curtainShadow, curtainRed), startY = 0f, endY = h * 0.3f))
    
    // Gold Trim on Valance
    val trimPath = Path().apply {
        moveTo(w, h * 0.18f)
        quadraticTo(w * 0.85f, h * 0.28f, w * 0.7f, h * 0.18f)
        quadraticTo(w * 0.5f, h * 0.32f, w * 0.3f, h * 0.18f)
        quadraticTo(w * 0.15f, h * 0.28f, 0f, h * 0.18f)
    }
    drawPath(trimPath, goldTrim, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    
    // Gold tassels
    drawCircle(goldTrim, radius = 4.dp.toPx(), center = Offset(w * 0.7f, h * 0.18f + 2.dp.toPx()))
    drawCircle(goldTrim, radius = 4.dp.toPx(), center = Offset(w * 0.3f, h * 0.18f + 2.dp.toPx()))
}


private fun DrawScope.drawPremiumTopHat(x: Float, y: Float, color: Color) {
    val hatW = 56.dp.toPx()
    val hatH = 46.dp.toPx()
    val brimW = 86.dp.toPx()
    val bandH = 12.dp.toPx()
    val brimH = 8.dp.toPx()

    // Brim
    drawRoundRect(
        color = Color(0xFF111111),
        topLeft = Offset(x - brimW / 2, y - brimH),
        size = Size(brimW, brimH),
        cornerRadius = CornerRadius(brimH / 2, brimH / 2)
    )
    
    // Brim highlight
    drawRoundRect(
        color = Color(0xFF333333),
        topLeft = Offset(x - brimW / 2 + 2.dp.toPx(), y - brimH + 1.dp.toPx()),
        size = Size(brimW - 4.dp.toPx(), 2.dp.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
    )

    // Main Hat Body
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF0F0F0F), Color(0xFF2A2A2A), Color(0xFF0F0F0F)),
            startX = x - hatW / 2,
            endX = x + hatW / 2
        ),
        topLeft = Offset(x - hatW / 2, y - hatH),
        size = Size(hatW, hatH - brimH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )

    // Hat Band
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(color.copy(alpha = 0.6f), color, color.copy(alpha = 0.6f)),
            startX = x - hatW / 2,
            endX = x + hatW / 2
        ),
        topLeft = Offset(x - hatW / 2, y - brimH - bandH),
        size = Size(hatW, bandH)
    )
    
    // Sparkle on band
    drawCircle(Color.White.copy(alpha = 0.8f), radius = 2.dp.toPx(), center = Offset(x - hatW / 4, y - brimH - bandH / 2))
}

private fun DrawScope.drawMysticCrystalBall(x: Float, y: Float, color: Color, time: Float) {
    val radius = 28.dp.toPx()
    val baseW = 18.dp.toPx()
    val baseH = 12.dp.toPx()
    val offset4 = 4.dp.toPx()
    
    // Golden ornate base
    val basePath = Path().apply {
        moveTo(x - baseW, y)
        quadraticTo(x, y - baseH / 2, x + baseW, y)
        lineTo(x + baseW * 0.7f, y - baseH)
        lineTo(x - baseW * 0.7f, y - baseH)
        close()
    }
    drawPath(basePath, Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFB8860B)), startY = y - baseH, endY = y))
    
    // Crystal Ball Glass
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.1f), color.copy(alpha = 0.5f), Color(0x33FFFFFF)),
            center = Offset(x, y - radius - offset4),
            radius = radius
        ),
        radius = radius,
        center = Offset(x, y - radius - offset4)
    )

    // Swirling inner mist
    val mistAngle = time * 0.05f
    val dDensity = density
    withTransform({
        translate(x, y - radius - offset4)
        rotate(mistAngle)
    }) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(color, Color.Transparent),
                radius = radius * 0.8f
            ),
            topLeft = Offset(-radius * 0.8f, -radius * 0.4f),
            size = Size(radius * 1.6f, radius * 0.8f)
        )
    }

    // Specular Highlight (Glass curve)
    val highlightPath = Path().apply {
        addArc(Rect(x - radius * 0.8f, y - radius * 2 - offset4 + radius * 0.2f, x + radius * 0.8f, y - offset4 - radius * 0.2f), 190f, 70f)
    }
    drawPath(highlightPath, Color.White.copy(alpha = 0.4f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawGrandFinale(x: Float, y: Float, color: Color, time: Float) {
    val offset10 = 10.dp.toPx()
    val off25 = 25f * density
    // Rotating Sunburst
    withTransform({
        translate(x, y - off25)
        rotate(time * 0.1f)
    }) {
        for (i in 0 until 12) {
            val angle = (i * 30f) * (PI.toFloat() / 180f)
            val burstSize = 50f * density
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(color, Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(cos(angle) * burstSize, sin(angle) * burstSize)
                ),
                start = Offset(cos(angle) * offset10, sin(angle) * offset10),
                end = Offset(cos(angle) * burstSize, sin(angle) * burstSize),
                strokeWidth = 3f * density,
                cap = StrokeCap.Round
            )
        }
    }
    drawPremiumTopHat(x, y, color)
}

private fun DrawScope.drawFloatingCards(x: Float, y: Float, time: Float) {
    val cardW = 28.dp.toPx()
    val cardH = 42.dp.toPx()
    val rad4 = 4.dp.toPx()
    val rad2 = 2.dp.toPx()
    val off4 = 4.dp.toPx()
    val off8 = 8.dp.toPx()
    val off20 = 20f * density
    val off30 = 30f * density
    
    for (i in 0..2) {
        val floatOffset = sin(time * 0.05f + i) * 10f
        val rotation = sin(time * 0.02f + i) * 15f
        withTransform({
            translate(x + (i - 1) * off20, y - off30 + floatOffset)
            rotate(rotation + (i - 1) * 15f)
        }) {
            drawRoundRect(
                color = Color(0xFFF5F5F5),
                topLeft = Offset(-cardW / 2, -cardH / 2),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(rad4)
            )
            // Card Back Pattern
            drawRoundRect(
                color = Color(0xFF8B0000),
                topLeft = Offset(-cardW / 2 + off4, -cardH / 2 + off4),
                size = Size(cardW - off8, cardH - off8),
                cornerRadius = CornerRadius(rad2)
            )
            // Center emblem
            drawCircle(Color(0xFFFFD700), radius = rad4, center = Offset(0f, 0f))
        }
    }
}

private fun DrawScope.drawElegantWand(x: Float, y: Float, color: Color, time: Float) {
    val wandL = 70.dp.toPx()
    val wandW = 6.dp.toPx()
    val tipL = 14.dp.toPx()
    val rad2 = 2.dp.toPx()
    val rad20 = 20.dp.toPx()
    val off20 = 20f * density
    
    withTransform({
        translate(x, y - off20)
        rotate(-45f + sin(time * 0.03f) * 10f) // Gentle waving
    }) {
        // Wand body with gradient
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFF111111), Color(0xFF333333), Color(0xFF111111))),
            topLeft = Offset(-wandL / 2, -wandW / 2),
            size = Size(wandL, wandW),
            cornerRadius = CornerRadius(rad2)
        )
        // White Tip
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(wandL / 2 - tipL, -wandW / 2),
            size = Size(tipL, wandW),
            cornerRadius = CornerRadius(rad2)
        )
        // Glow effect
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.6f), Color.Transparent)),
            radius = rad20,
            center = Offset(wandL / 2, 0f)
        )
    }
}

private fun DrawScope.drawShatteredWand(x: Float, y: Float, color: Color) {
    val wandL = 30.dp.toPx()
    val wandW = 6.dp.toPx()
    val rad2 = 2.dp.toPx()
    val off10 = 10.dp.toPx()
    val trans15 = 15f * density
    val trans10 = 10f * density
    
    // Left piece
    withTransform({ translate(x - trans15, y - trans15); rotate(-60f) }) {
        drawRoundRect(Color.DarkGray, Offset(-wandL / 2, -wandW / 2), Size(wandL, wandW), CornerRadius(rad2))
    }
    // Right piece
    withTransform({ translate(x + trans15, y - trans10); rotate(70f) }) {
        drawRoundRect(Color.DarkGray, Offset(-wandL / 2, -wandW / 2), Size(wandL, wandW), CornerRadius(rad2))
        drawRoundRect(Color.LightGray, Offset(wandL / 2 - off10, -wandW / 2), Size(off10, wandW), CornerRadius(rad2))
    }
    // Sparks
    for(i in 0..4) {
        drawCircle(color, 2.dp.toPx(), Offset(x + (i - 2) * 8.dp.toPx(), y - 20.dp.toPx() + (i%2) * 10.dp.toPx()))
    }
}

private fun DrawScope.drawVanishingSmoke(x: Float, y: Float, color: Color, time: Float) {
    val drift = time * 0.02f
    for (i in 0..3) {
        val size = 20.dp.toPx() + i * 5.dp.toPx()
        val sX = x + sin(drift + i) * 15.dp.toPx()
        val sY = y - 10.dp.toPx() - (drift * 20f % 40.dp.toPx()) + i * 10.dp.toPx()
        val alpha = maxOf(0f, 1f - (drift * 20f % 40.dp.toPx()) / 40.dp.toPx()) * 0.3f
        
        drawCircle(color.copy(alpha = alpha), size, Offset(sX, sY))
    }
}

// --- COMBAT ANIMATIONS ---

private fun DrawScope.drawSpectralBlades(x: Float, y: Float, p: Float, color: Color, isPlayerTurn: Boolean) {
    val dir = if (isPlayerTurn) 1f else -1f
    val travel = p * 120.dp.toPx()
    val startX = x - dir * 100.dp.toPx()
    val currentX = startX + dir * travel
    val off30 = 30f * density
    
    withTransform({
        translate(currentX, y - off30)
        rotate(if (isPlayerTurn) 45f else -135f)
        val s = 1f + p * 0.5f
        scale(scaleX = s, scaleY = s)
    }) {
        val blade = Path().apply {
            moveTo(0f, -30f)
            lineTo(8f, 0f)
            lineTo(2f, 0f)
            lineTo(2f, 15f)
            lineTo(-2f, 15f)
            lineTo(-2f, 0f)
            lineTo(-8f, 0f)
            close()
        }
        // Outer glow
        drawPath(blade, color.copy(alpha = 0.4f), style = Stroke(6f, join = StrokeJoin.Round))
        // Inner core
        drawPath(blade, Color.White)
    }
}

private fun DrawScope.drawEtherealPhoenix(x: Float, y: Float, p: Float, color: Color, isPlayerTurn: Boolean) {
    val dir = if (isPlayerTurn) 1f else -1f
    // Rises up and swoops across
    val currentX = x + dir * (p * 150.dp.toPx() - 50.dp.toPx())
    val currentY = y - 30.dp.toPx() - sin(p * PI.toFloat()) * 80.dp.toPx()
    val wingFlap = sin(p * 40f) * 20f

    withTransform({
        translate(currentX, currentY)
        scale(scaleX = if (isPlayerTurn) 1.5f else -1.5f, scaleY = 1.5f)
    }) {
        // Body
        val body = Path().apply {
            moveTo(20f, 0f) // Beak
            quadraticTo(0f, -10f, -20f, 5f) // Tail
            quadraticTo(0f, 10f, 20f, 0f)
        }
        // Wings
        val wing = Path().apply {
            moveTo(0f, 0f)
            quadraticTo(-10f, -30f + wingFlap, -30f, -10f + wingFlap)
            quadraticTo(-10f, -10f, 0f, 0f)
        }
        val lowerWing = Path().apply {
            moveTo(0f, 0f)
            quadraticTo(-10f, 30f - wingFlap, -30f, 10f - wingFlap)
            quadraticTo(-10f, 10f, 0f, 0f)
        }
        
        drawPath(body, color)
        drawPath(wing, color.copy(alpha = 0.8f))
        drawPath(lowerWing, color.copy(alpha = 0.8f))
        
        // Eye
        drawCircle(Color.White, 2f, Offset(10f, -2f))
    }
}

private fun DrawScope.drawMissDeflection(x: Float, y: Float, p: Float, color: Color) {
    val radius = p * 60.dp.toPx()
    val thickness = maxOf(0f, 1f - p) * 10.dp.toPx()
    
    drawCircle(
        color = color.copy(alpha = 1f - p),
        radius = radius,
        center = Offset(x, y - 20.dp.toPx()),
        style = Stroke(width = thickness)
    )
    
    // Shattering lines
    if (p > 0.5f) {
        val shatterP = (p - 0.5f) * 2f
        for (i in 0..5) {
            val angle = (i * 60f) * (PI.toFloat() / 180f)
            val rStart = 60.dp.toPx()
            val rEnd = rStart + shatterP * 30.dp.toPx()
            drawLine(
                color = color.copy(alpha = 1f - shatterP),
                start = Offset(x + cos(angle) * rStart, y - 20.dp.toPx() + sin(angle) * rStart),
                end = Offset(x + cos(angle) * rEnd, y - 20.dp.toPx() + sin(angle) * rEnd),
                strokeWidth = 2.dp.toPx() * (1f - shatterP),
                cap = StrokeCap.Round
            )
        }
    }
}
