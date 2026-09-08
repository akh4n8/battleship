package com.ak.battleship.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import battleship.app.generated.resources.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.model.PlaybackCommand
import com.ak.battleship.model.ShotOutcome
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.cos

// --- NEW: Clean Data Class for the Dynamic Sky ---
data class NavalSkyState(
    val skyTop: Color,
    val skyBottom: Color,
    val orbCore: Color,
    val orbGlow: Color,
    val cloudColor: Color,
    val textOverlay: Color
)

@Composable
fun BroadsideNavalWidget(
    probability: Float,
    shotOutcome: ShotOutcome = ShotOutcome.MISS,
    commandKey: Int = 0,
    isPlayerTurn: Boolean = true,
    command: PlaybackCommand = PlaybackCommand.IDLE
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

    val textMeasurer = rememberTextMeasurer()
    val cannonProgress = remember { Animatable(0f) }

    val reusableStarPath = remember { Path() }
    val reusableGoldNeedle = remember { Path() }
    val reusableIronNeedle = remember { Path() }
    val reusableGullPath = remember { Path() }
    val reusableBgWave = remember { Path() }
    val reusableFgWave = remember { Path() }

    LaunchedEffect(key1 = commandKey) {
        if (commandKey > 0 && command != PlaybackCommand.PLAY_JUMP) {
            cannonProgress.snapTo(0f)
            cannonProgress.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(durationMillis = 850, easing = LinearEasing)
            )
            cannonProgress.snapTo(0f)
        }
    }

    val shipDrawables = remember {
        arrayOf(
            Res.drawable.ship_state_1, Res.drawable.ship_state_2, Res.drawable.ship_state_3,
            Res.drawable.ship_state_4, Res.drawable.ship_state_5, Res.drawable.ship_state_6, Res.drawable.ship_state_7
        )
    }

    val playerIndex = when {
        animatedProb >= 1f -> 0
        animatedProb > 0.85f -> 1
        animatedProb > 0.60f -> 2
        animatedProb > 0.40f -> 3
        animatedProb > 0.15f -> 4
        animatedProb > 0f -> 5
        else -> 6
    }
    val oppIndex = 6 - playerIndex

    val targetSkyState = when {
        animatedProb >= 1f -> NavalSkyState(
            Color(0xFFFFB74D), Color(0xFFFFF9C4), Color(0xFFFF9800), Color(0xFFFFEB3B), Color.White.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.7f)
        )
        animatedProb > 0.85f -> NavalSkyState(
            Color(0xFF29B6F6), Color(0xFFE1F5FE), Color(0xFFFFF59D), Color(0xFFFFF9C4), Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.9f)
        )
        animatedProb > 0.60f -> NavalSkyState(
            Color(0xFF81D4FA).copy(alpha = 0.5f), Color.Transparent, Color(0xFFFF9800), Color(0xFFE91E63), Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.9f)
        )
        animatedProb > 0.40f -> NavalSkyState(
            Color(0xFF78909C), Color(0xFFCFD8DC), Color(0xFFB0BEC5), Color(0xFF90A4AE), Color(0xFFECEFF1).copy(alpha = 0.4f), Color.White.copy(alpha = 0.9f)
        )
        animatedProb > 0.15f -> NavalSkyState(
            Color(0xFF263238), Color(0xFF546E7A), Color(0xFF455A64), Color(0xFF37474F), Color(0xFF1E2B32).copy(alpha = 0.5f), Color.White.copy(alpha = 0.8f)
        )
        animatedProb > 0f -> NavalSkyState(
            Color(0xFF000000), Color(0xFF121212), Color(0xFF424242), Color(0xFF212121), Color(0xFF1E1E1E).copy(alpha = 0.3f), Color(0xFF9E9E9E)
        )
        else -> NavalSkyState(
            Color(0xFF120000), Color(0xFF5D0000), Color(0xFFD32F2F), Color(0xFFB71C1C), Color(0xFF2B0000).copy(alpha = 0.6f), Color(0xFFFFCDD2)
        )
    }

    val skyTop by animateColorAsState(targetValue = targetSkyState.skyTop, tween(1500), label = "SkyTop")
    val skyBottom by animateColorAsState(targetValue = targetSkyState.skyBottom, tween(1500), label = "SkyBot")
    val orbCore by animateColorAsState(targetValue = targetSkyState.orbCore, tween(1500), label = "OrbCore")
    val orbGlow by animateColorAsState(targetValue = targetSkyState.orbGlow, tween(1500), label = "OrbGlow")
    val cloudColor by animateColorAsState(targetValue = targetSkyState.cloudColor, tween(1500), label = "Clouds")
    val textColor by animateColorAsState(targetValue = targetSkyState.textOverlay, tween(1500), label = "Text")

    val infiniteTransition = rememberInfiniteTransition(label = "SeaEngine")

    // THE FIX: Dropped the target from 6f to 4f for a very gentle, subtle rock.
    val waveBob1 by infiniteTransition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "WaveBob1"
    )
    val waveBob2 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "WaveBob2"
    )
    val cloudDrift by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing), RepeatMode.Restart), label = "CloudDrift")
    val gullDrift by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Restart), label = "GullDrift")
    val fireFlicker by infiniteTransition.animateFloat(initialValue = 0.5f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(100, easing = LinearEasing), RepeatMode.Reverse), label = "FireFlicker")

    // --- THE KRAKEN ENGINE ---
    // Reduced from 8000ms to 5000ms for a much more frantic, dangerous thrash
    val krakenWrithe by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "KrakenWrithe"
    )

    val topBleedClip = remember {
        object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                return Outline.Rectangle(Rect(left = -200f, top = -800f, right = size.width + 200f, bottom = size.height))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(topBleedClip)
    ) {
        // --- LAYER 0: DYNAMIC SKY, ORB, CLOUDS, & SEAGULLS ---
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val waterLineY = h - 30.dp.toPx()

            // 1. Dynamic Sky Gradient
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(skyTop, skyBottom),
                    startY = 0f, endY = h * 0.6f
                )
            )

            // 2. The Celestial Compass
            val centerX = w / 2f
            val compassY = waterLineY
            val compassRadius = 55.dp.toPx()

            // THE FIX: Massive Glowing Aura restored and boosted
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        orbCore.copy(alpha = 0.8f),
                        orbGlow.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, compassY),
                    radius = compassRadius * 2.2f
                ),
                radius = compassRadius * 2.2f,
                center = Offset(centerX, compassY)
            )

            // THE FIX: Restored the Compass Star back-plate, using dynamic glow color!
            reusableStarPath.reset()
            reusableStarPath.moveTo(centerX, compassY - compassRadius) // Top
            reusableStarPath.lineTo(centerX + 6.dp.toPx(), compassY - compassRadius * 0.7f)
            reusableStarPath.lineTo(centerX + compassRadius, compassY) // Right
            reusableStarPath.lineTo(centerX + 6.dp.toPx(), compassY + compassRadius * 0.7f)
            reusableStarPath.lineTo(centerX, compassY + compassRadius) // Bottom
            reusableStarPath.lineTo(centerX - 6.dp.toPx(), compassY + compassRadius * 0.7f)
            reusableStarPath.lineTo(centerX - compassRadius, compassY) // Left
            reusableStarPath.lineTo(centerX - 6.dp.toPx(), compassY - compassRadius * 0.7f)
            reusableStarPath.close()
            drawPath(reusableStarPath, color = orbGlow.copy(alpha = 0.35f))

            // Dynamic Astrolabe Orbital Rings
            drawCircle(color = orbCore.copy(alpha = 0.6f), radius = compassRadius, center = Offset(centerX, compassY), style = Stroke(3f))
            drawCircle(color = orbGlow.copy(alpha = 0.4f), radius = compassRadius * 0.75f, center = Offset(centerX, compassY), style = Stroke(1.5f))
            // THE FIX: Restored the outer astrolabe ring
            drawCircle(color = orbGlow.copy(alpha = 0.2f), radius = compassRadius * 1.15f, center = Offset(centerX, compassY), style = Stroke(1f))

            // Navigation Crosshairs
            drawLine(orbGlow.copy(alpha = 0.5f), start = Offset(centerX, compassY - compassRadius * 1.2f), end = Offset(centerX, compassY + compassRadius * 1.2f), strokeWidth = 2f)
            drawLine(orbGlow.copy(alpha = 0.5f), start = Offset(centerX - compassRadius * 1.2f, compassY), end = Offset(centerX + compassRadius * 1.2f, compassY), strokeWidth = 2f)

            // Dynamic Spinning Needle
            val needleAngle = (0.5f - animatedProb) * 180f
            withTransform({ rotate(degrees = needleAngle, pivot = Offset(centerX, compassY)) }) {
                reusableGoldNeedle.reset()
                reusableGoldNeedle.moveTo(centerX, compassY - compassRadius + 8.dp.toPx())
                reusableGoldNeedle.lineTo(centerX + 6.dp.toPx(), compassY)
                reusableGoldNeedle.lineTo(centerX - 6.dp.toPx(), compassY)
                reusableGoldNeedle.close()
                drawPath(reusableGoldNeedle, color = Color(0xFFFFD54F).copy(alpha = 0.9f))

                reusableIronNeedle.reset()
                reusableIronNeedle.moveTo(centerX, compassY + compassRadius - 8.dp.toPx())
                reusableIronNeedle.lineTo(centerX + 6.dp.toPx(), compassY)
                reusableIronNeedle.lineTo(centerX - 6.dp.toPx(), compassY)
                reusableIronNeedle.close()
                drawPath(reusableIronNeedle, color = Color(0xFF5D4037).copy(alpha = 0.8f))

                drawCircle(Color(0xFFFFF8E1), radius = 4.dp.toPx(), center = Offset(centerX, compassY))
            }

            // Probability Text
            val probText = "${(animatedProb * 100).roundToInt()}%"
            val textLayoutResult = textMeasurer.measure(
                text = probText,
                style = TextStyle(color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(centerX - (textLayoutResult.size.width / 2f), compassY - (textLayoutResult.size.height / 2f) - 50f)
            )

            // 3. Drifting Background Clouds
            val driftPx = w * cloudDrift
            fun drawCloud(baseX: Float, baseY: Float, scale: Float = 1f) {
                drawOval(cloudColor, topLeft = Offset(baseX, baseY), size = Size(50.dp.toPx() * scale, 16.dp.toPx() * scale))
                drawCircle(cloudColor, radius = 12.dp.toPx() * scale, center = Offset(baseX + 15.dp.toPx() * scale, baseY + 4.dp.toPx() * scale))
                drawCircle(cloudColor, radius = 16.dp.toPx() * scale, center = Offset(baseX + 30.dp.toPx() * scale, baseY - 2.dp.toPx() * scale))
                drawCircle(cloudColor, radius = 10.dp.toPx() * scale, center = Offset(baseX + 42.dp.toPx() * scale, baseY + 6.dp.toPx() * scale))
            }

            // Draw clouds that are far away (These render BEHIND the text)
            for (offsetX in listOf(driftPx, driftPx - w)) {
                drawCloud(offsetX + w * 0.1f, h * 0.1f, scale = 0.8f)
                drawCloud(offsetX + w * 0.8f, h * 0.05f, scale = 0.6f)
            }

            // --- 4. CINEMATIC ATMOSPHERIC TEXT ---
            val bannerText = when {
                animatedProb >= 1f -> "VICTORY DECLARED"
                animatedProb > 0.85f -> "DOMINATING THE SEAS"
                animatedProb > 0.60f -> "HOLD THE LINE"
                animatedProb > 0.40f -> "BATTLE RAGES ON"
                animatedProb > 0.15f -> "TAKING ON WATER"
                animatedProb > 0f -> "CRITICAL DAMAGE"
                else -> "ABANDON SHIP"
            }

            // Deep shadows for daytime, pitch black shadows for storms to make it glow
            val shadowColor = if (animatedProb <= 0.15f) Color.Black else Color(0xFF1E1E1E).copy(alpha = 0.5f)

            val bannerLayout = textMeasurer.measure(
                text = bannerText,
                style = TextStyle(
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp, // Massive tracking for that cinematic, epic feel
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = shadowColor,
                        blurRadius = 12f,
                        offset = Offset(0f, 4f)
                    )
                )
            )

            val textWidth = bannerLayout.size.width.toFloat()
            val textHeight = bannerLayout.size.height.toFloat()
            val textStartX = (w / 2f) - (textWidth / 2f)
            val textStartY = h * 0.14f

            // Fading Nautical Map "Rhumb Lines" to anchor the text
            val lineY = textStartY + (textHeight / 2f)
            val lineAlpha = if (animatedProb <= 0.15f) 0.3f else 0.6f

            // Left Line
            drawPath(
                path = Path().apply { moveTo(0f, lineY); lineTo(textStartX - 16.dp.toPx(), lineY) },
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, textColor.copy(alpha = lineAlpha)),
                    startX = 0f, endX = textStartX - 16.dp.toPx()
                ),
                style = Stroke(1.dp.toPx())
            )

            // Right Line
            drawPath(
                path = Path().apply { moveTo(textStartX + textWidth + 16.dp.toPx(), lineY); lineTo(w, lineY) },
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(textColor.copy(alpha = lineAlpha), Color.Transparent),
                    startX = textStartX + textWidth + 16.dp.toPx(), endX = w
                ),
                style = Stroke(1.dp.toPx())
            )

            drawText(
                textLayoutResult = bannerLayout,
                topLeft = Offset(textStartX, textStartY)
            )

            // --- 5. FOREGROUND CLOUDS ---
            // Draw the massive clouds LAST so they physically drift IN FRONT of the text!
            for (offsetX in listOf(driftPx, driftPx - w)) {
                drawCloud(offsetX + w * 0.5f, h * 0.18f, scale = 1.4f)
            }

            // --- 6. FOREGROUND SEAGULLS ---
            val birdPx = w * gullDrift
            fun drawGull(baseX: Float, baseY: Float, scale: Float = 1f) {
                val gullAlpha = if (animatedProb <= 0.15f) 0f else 0.7f
                val flap = sin(com.ak.battleship.PlatformServices.getCurrentTimeMillis() / 150.0).toFloat() * 3.dp.toPx()

                reusableGullPath.reset()
                reusableGullPath.moveTo(baseX, baseY)
                reusableGullPath.quadraticBezierTo(baseX + 4.dp.toPx() * scale, baseY - flap, baseX + 8.dp.toPx() * scale, baseY + 2.dp.toPx() * scale)
                reusableGullPath.quadraticBezierTo(baseX + 12.dp.toPx() * scale, baseY - flap, baseX + 16.dp.toPx() * scale, baseY)

                drawPath(path = reusableGullPath, color = Color(0xFF37474F).copy(alpha = gullAlpha), style = Stroke(width = 1.5.dp.toPx() * scale, cap = StrokeCap.Round))
            }

            for (offsetX in listOf(birdPx, birdPx - w)) {
                drawGull(offsetX + w * 0.3f, h * 0.15f, scale = 1f)
                drawGull(offsetX + w * 0.35f, h * 0.12f, scale = 0.8f)
                drawGull(offsetX + w * 0.75f, h * 0.35f, scale = 0.6f)
            }
        }

        // --- LAYER 1: BACKGROUND WATER ---
        Canvas(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(12.dp).offset(y = (-15).dp)) {
            val w = size.width
            val h = size.height
            val bob1 = waveBob2.dp.toPx()
            val bob2 = waveBob1.dp.toPx()

            reusableBgWave.reset()
            reusableBgWave.moveTo(0f, bob1)
            // THE FIX: Softened the control point multiplier from 2f to 1.5f
            reusableBgWave.quadraticBezierTo(w * 0.25f, -bob1 * 1.5f, w * 0.5f, 0f)
            reusableBgWave.quadraticBezierTo(w * 0.75f, bob2 * 1.5f, w, -bob2)
            reusableBgWave.lineTo(w, h + 15.dp.toPx())
            reusableBgWave.lineTo(0f, h + 15.dp.toPx())
            reusableBgWave.close()

            val waterBase = if (animatedProb <= 0.15f) Color(0xCC004D40) else Color(0xCC0077BE)
            drawPath(path = reusableBgWave, color = waterBase)

        }

        // --- LAYER 1.5: THE KRAKEN (V3: Traveling Waves & 3D Perspective Cups) ---
        if (animatedProb <= 0.15f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val waterLineY = h - 10.dp.toPx()

                val krakenColor = Color(0xFF041417).copy(alpha = 0.98f)
                val cupBaseColor = Color(0xFF13524C).copy(alpha = 0.8f) // Brighter teal to pop against the dark flesh
                val cupHoleColor = Color(0xFF010A0B).copy(alpha = 0.95f)

                fun drawTentacle(startX: Float, height: Float, width: Float, phaseOffset: Float, swayMultiplier: Float = 1f) {

                    // 1. True Traveling Wave Math (Dynamically scaled to height!)
                    val time = krakenWrithe + phaseOffset

                    // THE FIX: Sway is now a percentage of the tentacle's current visible height.
                    // If the tentacle is 150px tall, it swings 33px.
                    // If it retreats to 10px tall, it only swings 2px!
                    val lowerSway = sin(time) * (height * 0.10f * swayMultiplier)
                    val upperSway = sin(time - 1.2f) * (height * 0.16f * swayMultiplier)
                    val tipSway = sin(time - 2.4f) * (height * 0.22f * swayMultiplier)

                    val leftBaseX = startX - width
                    val rightBaseX = startX + width

                    val lP0x = leftBaseX
                    val lP0y = waterLineY
                    val lP1x = leftBaseX + lowerSway
                    val lP1y = waterLineY - (height * 0.35f)
                    val lP2x = leftBaseX + upperSway
                    val lP2y = waterLineY - (height * 0.7f)
                    val tipX = startX + tipSway
                    val tipY = waterLineY - height

                    val rP0x = rightBaseX
                    val rP0y = waterLineY
                    val rP1x = rightBaseX + lowerSway
                    val rP1y = waterLineY - (height * 0.35f)
                    val rP2x = rightBaseX + upperSway
                    val rP2y = waterLineY - (height * 0.7f)

                    // 2. Draw the Main Tentacle Body FIRST
                    val tentacle = Path()
                    tentacle.moveTo(lP0x, lP0y)
                    tentacle.cubicTo(lP1x, lP1y, lP2x, lP2y, tipX, tipY)
                    tentacle.cubicTo(rP2x, rP2y, rP1x, rP1y, rP0x, rP0y)
                    tentacle.close()

                    drawPath(tentacle, krakenColor)

                    // 3. Slimy Underbelly Highlight
                    val belly = Path()
                    belly.moveTo(lP0x, lP0y)
                    belly.cubicTo(lP1x, lP1y, lP2x, lP2y, tipX, tipY)
                    drawPath(belly, Color(0xFF26A69A).copy(alpha = 0.25f), style = Stroke(width * 0.6f))

                    // 4. 3D Perspective Suction Cups
                    val numCups = (height / 12.dp.toPx()).toInt().coerceAtLeast(4)

                    for (i in 1 until numCups) {
                        val t = i.toFloat() / numCups
                        val invT = 1f - t
                        val invT2 = invT * invT
                        val t2 = t * t

                        val curveX = (invT * invT2 * lP0x) + (3 * invT2 * t * lP1x) + (3 * invT * t2 * lP2x) + (t * t2 * tipX)
                        val curveY = (invT * invT2 * lP0y) + (3 * invT2 * t * lP1y) + (3 * invT * t2 * lP2y) + (t * t2 * tipY)

                        val dx = 3 * invT2 * (lP1x - lP0x) + 6 * invT * t * (lP2x - lP1x) + 3 * t2 * (tipX - lP2x)
                        val dy = 3 * invT2 * (lP1y - lP0y) + 6 * invT * t * (lP2y - lP1y) + 3 * t2 * (tipY - lP2y)

                        val tangentAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                        val inwardNormalAngle = tangentAngle + (PI / 2).toFloat() // Point slightly inward

                        // Adjust scaling so cups don't get microscopically small at the tip
                        val cupRadius = (width * 0.85f) * (1f - (t * 0.8f))

                        // Pull the cup INWARD slightly so it overlaps the main body
                        val cupCenterX = curveX + (cos(inwardNormalAngle.toDouble()).toFloat() * cupRadius * 0.4f)
                        val cupCenterY = curveY + (sin(inwardNormalAngle.toDouble()).toFloat() * cupRadius * 0.4f)

                        // Rotate the Canvas to draw ellipses perfectly aligned with the curve!
                        withTransform({
                            rotate(degrees = (tangentAngle * 180 / PI).toFloat(), pivot = Offset(cupCenterX, cupCenterY))
                        }) {
                            drawOval(
                                color = cupBaseColor,
                                topLeft = Offset(cupCenterX - cupRadius, cupCenterY - cupRadius * 0.4f),
                                size = Size(cupRadius * 2f, cupRadius * 0.8f) // Stretched wide, squashed flat
                            )
                            drawOval(
                                color = cupHoleColor,
                                topLeft = Offset(cupCenterX - cupRadius * 0.5f, cupCenterY - cupRadius * 0.2f),
                                size = Size(cupRadius, cupRadius * 0.4f)
                            )
                        }
                    }
                }

                // Smoothly scales height between 0.0 and 1.0
                val emergeProgress = ((0.15f - animatedProb) / 0.15f).coerceIn(0f, 1f)

                // THE FIX: The width now has a 40% floor.
                // When they first breach, they are 40% of their max width (nice and thick),
                // and they smoothly bulk up to 100% as they rise!
                val widthScale = 0.4f + (emergeProgress * 0.6f)

                val t1Height = 110.dp.toPx() * emergeProgress
                val t1Width = 7.dp.toPx() * widthScale
                drawTentacle(startX = w * 0.15f, height = t1Height, width = t1Width, phaseOffset = 0f, swayMultiplier = -1.1f)

                val t2Height = 160.dp.toPx() * emergeProgress
                val t2Width = 11.dp.toPx() * widthScale
                drawTentacle(startX = w * 0.4f, height = t2Height, width = t2Width, phaseOffset = 1.5f, swayMultiplier = 1.2f)

                val t3Height = 130.dp.toPx() * emergeProgress
                val t3Width = 9.dp.toPx() * widthScale
                drawTentacle(startX = w * 0.7f, height = t3Height, width = t3Width, phaseOffset = 3.2f, swayMultiplier = 1.4f)

                val t4Height = 90.dp.toPx() * emergeProgress
                val t4Width = 6.dp.toPx() * widthScale
                drawTentacle(startX = w * 0.9f, height = t4Height, width = t4Width, phaseOffset = 4.8f, swayMultiplier = -1.0f)
            }
        }

        // --- LAYER 2: MASSIVE POP-OUT SHIPS ---
        val shipWidth = 110.dp
        val shipHeight = 160.dp
        val baseOffsetY = (-30).dp

        val pDynamicSink = if (playerIndex == 6) 30.dp else 0.dp
        val oDynamicSink = if (oppIndex == 6) 30.dp else 0.dp

        val pTiltAngle = when (playerIndex) { 4 -> -2f; 5 -> -4f; 6 -> -6f; else -> 0f }
        val oTiltAngle = when (oppIndex) { 4 -> 2f; 5 -> 4f; 6 -> 6f; else -> 0f }

        // THE FIX: Ships now bob up and down with the massive new wave heights
        Image(
            painter = painterResource(shipDrawables[playerIndex]),
            contentDescription = "Player Ship", contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 4.dp, y = baseOffsetY + waveBob1.dp + pDynamicSink)
                .requiredSize(shipWidth, shipHeight)
                .graphicsLayer { scaleX = -1f; rotationZ = pTiltAngle }
        )

        Image(
            painter = painterResource(shipDrawables[oppIndex]),
            contentDescription = "Opponent Ship", contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-4).dp, y = baseOffsetY + waveBob2.dp + oDynamicSink)
                .requiredSize(shipWidth, shipHeight)
                .graphicsLayer { rotationZ = oTiltAngle }
        )

        // --- LAYER 2.5: EVENT-DRIVEN CANNON FIRE ---
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val waterLineY = h - 15.dp.toPx()
            val progress = cannonProgress.value

            if (progress > 0f) {
                val startX = if (isPlayerTurn) w * 0.25f else w * 0.75f
                val baseFireY = waterLineY - 45.dp.toPx()

                val endX = when (shotOutcome) {
                    ShotOutcome.MISS -> w * 0.5f
                    else -> if (isPlayerTurn) w * 0.75f else w * 0.25f
                }

                val targetY = when (shotOutcome) {
                    ShotOutcome.MISS -> waterLineY
                    else -> waterLineY - 30.dp.toPx()
                }

                val projectileRadius = if (shotOutcome == ShotOutcome.SUNK) 6.dp.toPx() else 3.dp.toPx()

                // Phase 1: Muzzle Flash
                if (progress < 0.15f) {
                    val scale = (1f - (progress / 0.15f))
                    drawCircle(Color(0xFFFF9100).copy(alpha = scale), radius = 12.dp.toPx() * scale, center = Offset(startX, baseFireY))
                }

                // Phase 2: Flight Arc
                if (progress <= 1f) {
                    val currentX = startX + ((endX - startX) * progress)
                    val linearY = baseFireY + ((targetY - baseFireY) * progress)
                    val arcHeight = 45.dp.toPx() * sin(progress * PI).toFloat()
                    val currentY = linearY - arcHeight
                    drawCircle(color = Color(0xFF121212), radius = projectileRadius, center = Offset(currentX, currentY))
                }

                // Phase 3: Terminal Effects
                if (progress >= 0.95f) {
                    val effectScale = ((progress - 0.95f) / 0.25f).coerceIn(0f, 1f)
                    val alphaFade = 1f - effectScale

                    when (shotOutcome) {
                        ShotOutcome.MISS -> {
                            drawOval(
                                color = Color.White.copy(alpha = 0.8f * alphaFade),
                                topLeft = Offset(endX - (6.dp.toPx() * effectScale), targetY - (20.dp.toPx() * effectScale)),
                                size = Size(12.dp.toPx() * effectScale, 24.dp.toPx() * effectScale)
                            )
                        }
                        ShotOutcome.HIT -> {
                            drawCircle(Color(0xFFFF3D00).copy(alpha = fireFlicker * alphaFade), radius = 14.dp.toPx() * effectScale, center = Offset(endX, targetY))
                        }
                        ShotOutcome.SUNK -> {
                            drawCircle(Color(0xFFFF3D00).copy(alpha = fireFlicker * alphaFade), radius = 26.dp.toPx() * effectScale, center = Offset(endX, targetY))
                            drawCircle(Color(0xFFFFEA00).copy(alpha = 0.9f * alphaFade), radius = 14.dp.toPx() * effectScale, center = Offset(endX, targetY))
                        }
                    }
                }
            }
        }

        // --- LAYER 3: FOREGROUND WATER ---
        Canvas(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(15.dp)) {
            val w = size.width
            val h = size.height
            val bob1 = waveBob1.dp.toPx()
            val bob2 = waveBob2.dp.toPx()

            reusableFgWave.reset()
            reusableFgWave.moveTo(0f, bob1)
            // THE FIX: Softened the control point multiplier from 2f to 1.5f
            reusableFgWave.quadraticBezierTo(w * 0.25f, -bob1 * 1.5f, w * 0.5f, 0f)
            reusableFgWave.quadraticBezierTo(w * 0.75f, bob2 * 1.5f, w, -bob2)
            reusableFgWave.lineTo(w, h)
            reusableFgWave.lineTo(0f, h)
            reusableFgWave.close()

            val waterBase = if (animatedProb <= 0.15f) Color(0xCC004D40) else Color(0xCC0077BE)
            drawPath(
                path = reusableFgWave,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(waterBase, Color(0xFFFFFFFF))
                )
            )
        }
    }
}
