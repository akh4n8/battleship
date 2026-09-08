package com.ak.battleship.ui.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.model.ShotOutcome
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.roundToInt
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.ak.battleship.model.PlaybackCommand

// ============================================================================
// 1. STATE CONFIGURATION
// ============================================================================

data class SymphonyState(
    val movement: String,
    val statusText: String,
    val critiqueColor: Color,
    val reactiveNoteColor: Color
)

data class SymphonyEvent(
    val outcome: ShotOutcome,
    val isPlayerTurn: Boolean,
    val frozenState: SymphonyState
)

fun getSymphonyStateForProb(prob: Float): SymphonyState {
    return when {
        prob >= 1f -> SymphonyState("MVT VII: FINALE", "A masterful crescendo. The grand finale is yours.", Color(0xFFD4AF37), Color(0xFFD4AF37))
        prob > 0.85f -> SymphonyState("MVT VI: VIVACE", "A commanding tempo. Keep the rhythm steady.", Color(0xFFFFF59D), Color(0xFFFFF59D))
        prob > 0.60f -> SymphonyState("MVT V: ALLEGRO", "A steady melody. The orchestra is aligned.", Color(0xFFE0E0E0), Color(0xFFE0E0E0))
        prob > 0.40f -> SymphonyState("MVT IV: ANDANTE", "The brass section is wavering...", Color(0xFFFB8C00), Color(0xFFFB8C00))
        prob > 0.15f -> SymphonyState("MVT III: ADAGIO", "Dissonance! We are losing the harmony!", Color(0xFFE53935), Color(0xFFE53935))
        prob > 0f -> SymphonyState("MVT II: GRAVE", "An atonal disaster! Who is writing this score?!", Color(0xFFC62828), Color(0xFFC62828))
        else -> SymphonyState("MVT I: REQUIEM", "A catastrophic finale. Bring down the curtain.", Color(0xFFB71C1C), Color(0xFFD32F2F))
    }
}

// ============================================================================
// 2. THE LIVE PROBABILITY CONDUCTOR
// ============================================================================

@Composable
fun SymphonyConductorWidget(
    probability: Float,
    shotOutcome: ShotOutcome = ShotOutcome.MISS,
    isPlayerTurn: Boolean = true,
    // --- THE NEW COMMAND PARAMETERS ---
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

    // --- 1. DYNAMIC TEMPO & AMBIENT ANIMATION ---
    val currentBpm = 40 + (animatedProb * 140).roundToInt()
    val currentBpmState = rememberUpdatedState(currentBpm)
    var ambientProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        while (true) {
            val frameTime = withFrameNanos { it }
            val deltaMs = (frameTime - lastFrameTime) / 1_000_000f
            lastFrameTime = frameTime

            val dynamicLoopDuration = (60000f / currentBpmState.value) * 16f
            ambientProgress = (ambientProgress + (deltaMs / dynamicLoopDuration)) % 1f
        }
    }

    // --- 2. THE EXPLICIT COMMAND ARCHITECTURE ---
    val burstProgress = remember { Animatable(0f) }
    var activeEvent by remember { mutableStateOf<SymphonyEvent?>(null) }

    LaunchedEffect(commandKey, command) {
        when (command) {
            PlaybackCommand.IDLE,
            PlaybackCommand.PLAY_JUMP,
            PlaybackCommand.TIME_TRAVEL_BACKWARD,
            PlaybackCommand.TIME_TRAVEL_FORWARD -> {
                // INSTANT KILL: Scrubbing timeline
                activeEvent = null
                burstProgress.snapTo(0f)
            }
            PlaybackCommand.PLAY_WIN_CINEMATIC, PlaybackCommand.PLAY_LOSS_CINEMATIC -> {
                activeEvent = null
                burstProgress.snapTo(0f)
            }
            PlaybackCommand.PLAY_MISS,
            PlaybackCommand.PLAY_HIT,
            PlaybackCommand.PLAY_SUNK,
            PlaybackCommand.PLAY_WIN_LIVE -> {

                // Enforce the outcome so the UI cannot physically mismatch the command
                val enforcedOutcome = when (command) {
                    PlaybackCommand.PLAY_WIN_LIVE, PlaybackCommand.PLAY_SUNK -> ShotOutcome.SUNK
                    PlaybackCommand.PLAY_HIT -> ShotOutcome.HIT
                    else -> ShotOutcome.MISS
                }

                // Freeze the state at the exact moment of the command
                val frozenState = getSymphonyStateForProb(probability)
                activeEvent = SymphonyEvent(enforcedOutcome, isPlayerTurn, frozenState)

                burstProgress.snapTo(0f)
                burstProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1800, easing = LinearOutSlowInEasing)
                )
                activeEvent = null
            }
        }
    }

    // --- 3. THE 7-STAGE AUTONOMOUS PIANO ENGINE ---
    val state = getSymphonyStateForProb(animatedProb)
    val isVictory = animatedProb >= 1f
    val isDefeat = animatedProb <= 0f

    var ambientWhiteKeys by remember { mutableStateOf(emptyList<Int>()) }
    var ambientBlackKeys by remember { mutableStateOf(emptyList<Int>()) }

    LaunchedEffect(state.movement) {
        val allBlacks = listOf(0, 1, 3, 4, 5, 7, 8, 10, 11, 12)

        while (true) {
            when (state.movement) {
                "MVT VII: FINALE" -> {
                    // THE ARCADE JACKPOT (100% Win)
                    val shockwaveWhites = listOf(listOf(6, 7), listOf(5, 8), listOf(4, 9), listOf(3, 10), listOf(2, 11), listOf(1, 12), listOf(0, 13))
                    val shockwaveBlacks = listOf(listOf(5, 7), listOf(4, 8), listOf(3, 10), listOf(1, 11), listOf(0, 12), emptyList(), emptyList())

                    for (step in 0..6) {
                        ambientWhiteKeys = shockwaveWhites[step]; ambientBlackKeys = shockwaveBlacks[step]; delay(50)
                    }
                    for (i in 0..1) {
                        ambientWhiteKeys = (0..13).filter { it % 2 == 0 }; ambientBlackKeys = allBlacks.filterIndexed { index, _ -> index % 2 == 0 }; delay(120)
                        ambientWhiteKeys = (0..13).filter { it % 2 != 0 }; ambientBlackKeys = allBlacks.filterIndexed { index, _ -> index % 2 != 0 }; delay(120)
                    }
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(100)

                    for (step in 6 downTo 0) {
                        ambientWhiteKeys = shockwaveWhites[step]; ambientBlackKeys = shockwaveBlacks[step]; delay(40)
                    }

                    ambientWhiteKeys = (0..13).toList(); ambientBlackKeys = allBlacks; delay(150)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(80)
                    ambientWhiteKeys = (0..13).toList(); ambientBlackKeys = allBlacks; delay(800)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(600)
                }

                "MVT VI: VIVACE" -> {
                    // FAST & BOUNCY (> 85%)
                    ambientWhiteKeys = listOf(7, 9, 11); ambientBlackKeys = emptyList(); delay(150)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(50)
                    ambientWhiteKeys = listOf(9, 11, 13); ambientBlackKeys = emptyList(); delay(150)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(50)
                    ambientWhiteKeys = listOf(8, 10, 12); ambientBlackKeys = emptyList(); delay(300)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(400)
                }

                "MVT V: ALLEGRO" -> {
                    // STEADY & CONFIDENT (> 60%)
                    ambientWhiteKeys = listOf(4, 6); ambientBlackKeys = emptyList(); delay(300)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(100)
                    ambientWhiteKeys = listOf(5, 7); ambientBlackKeys = emptyList(); delay(300)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(400)
                }

                "MVT IV: ANDANTE" -> {
                    // TENSE & SUSPENSEFUL (> 40%) - The "Sonar Ping"
                    ambientWhiteKeys = listOf(3, 5); ambientBlackKeys = listOf(4); delay(150)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(850)
                    ambientWhiteKeys = listOf(2, 4); ambientBlackKeys = emptyList(); delay(150)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(850)
                }

                "MVT III: ADAGIO" -> {
                    // SLOW & SORROWFUL (> 15%)
                    ambientWhiteKeys = listOf(4, 5); ambientBlackKeys = listOf(4); delay(600)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(200)
                    ambientWhiteKeys = listOf(3, 4); ambientBlackKeys = listOf(3); delay(600)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(600)
                }

                "MVT II: GRAVE" -> {
                    // HEAVY & DISSONANT (> 0%)
                    ambientWhiteKeys = listOf(1, 2); ambientBlackKeys = listOf(1); delay(1000)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(400)
                    ambientWhiteKeys = listOf(0, 1); ambientBlackKeys = listOf(0); delay(1000)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(1200)
                }

                "MVT I: REQUIEM" -> {
                    // CATASTROPHIC SYSTEM FAILURE (0% Loss)
                    val glitchWhites = listOf(listOf(2, 9, 11), listOf(0, 5, 12), listOf(4, 7, 13), listOf(1, 6, 10))
                    val glitchBlacks = listOf(listOf(1, 10), listOf(4, 11), listOf(3, 8), listOf(0, 5))

                    for (i in 0..3) {
                        ambientWhiteKeys = glitchWhites[i]; ambientBlackKeys = glitchBlacks[i]; delay(60)
                        ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(40)
                    }
                    delay(200)

                    for (step in 13 downTo 0) {
                        ambientWhiteKeys = (0..step).toList()
                        ambientBlackKeys = allBlacks.filter { it <= step }
                        delay(60)
                    }

                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(400)

                    ambientWhiteKeys = (0..13).toList(); ambientBlackKeys = allBlacks; delay(100)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(100)
                    ambientWhiteKeys = (0..13).toList(); ambientBlackKeys = allBlacks; delay(100)

                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(400)
                    ambientWhiteKeys = listOf(0, 1); ambientBlackKeys = listOf(0); delay(150)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(150)
                    ambientWhiteKeys = listOf(0, 1); ambientBlackKeys = listOf(0); delay(1000)
                    ambientWhiteKeys = emptyList(); ambientBlackKeys = emptyList(); delay(1500)
                }
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val clefTextLayout = remember(textMeasurer) {
        textMeasurer.measure(
            text = "\uD834\uDD1E",
            style = TextStyle(color = Color.White.copy(alpha = 0.15f), fontSize = 64.sp, fontFamily = FontFamily.Serif)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    0.0f to Color(0xFF0A0A0A), 0.2f to Color(0xFF1E1E1E), 0.48f to Color(0xFF141414),
                    0.5f to Color(0xFF000000), 0.52f to Color(0xFF141414), 0.8f to Color(0xFF1E1E1E),
                    1.0f to Color(0xFF0A0A0A)
                )
            )
            .border(1.dp, Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val staffTop = h * 0.28f
            val staffBottom = h * 0.58f
            val lineSpacing = (staffBottom - staffTop) / 4

            for (i in 0..4) {
                val yPos = staffTop + (i * lineSpacing)
                drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, yPos), Offset(w, yPos), 1f)
            }
            drawLine(Color.White.copy(alpha = 0.15f), Offset(w * 0.25f, staffTop), Offset(w * 0.25f, staffBottom), 2f)
            drawLine(Color.White.copy(alpha = 0.15f), Offset(w * 0.75f, staffTop), Offset(w * 0.75f, staffBottom), 2f)

            drawText(textLayoutResult = clefTextLayout, topLeft = Offset(w * 0.05f, staffTop - (lineSpacing * 1.5f)))

            fun drawNativeNote(xOffset: Float, yLineIndex: Float, color: Color, alpha: Float, isEighthNote: Boolean = false) {
                if (xOffset !in -20f..w + 20f) return
                val headX = xOffset
                val headY = staffTop + (yLineIndex * lineSpacing)
                val headRadius = 4.dp.toPx()
                val stemHeight = 16.dp.toPx()

                withTransform({ rotate(-15f, pivot = Offset(headX, headY)) }) {
                    drawOval(color.copy(alpha = alpha), Offset(headX - headRadius, headY - (headRadius * 0.7f)), Size(headRadius * 2.2f, headRadius * 1.5f))
                }
                drawLine(color.copy(alpha = alpha), Offset(headX + (headRadius * 0.9f), headY), Offset(headX + (headRadius * 0.9f), headY - stemHeight), 1.5.dp.toPx(), StrokeCap.Round)

                if (isEighthNote) {
                    val path = androidx.compose.ui.graphics.Path()
                    path.moveTo(headX + (headRadius * 0.9f), headY - stemHeight)
                    path.quadraticBezierTo(headX + (headRadius * 3f), headY - (stemHeight * 0.5f), headX + (headRadius * 3.5f), headY - (stemHeight * 0.1f))
                    path.quadraticBezierTo(headX + (headRadius * 2f), headY - (stemHeight * 0.4f), headX + (headRadius * 0.9f), headY - (stemHeight * 0.7f))
                    drawPath(path, color.copy(alpha = alpha))
                }
            }

            // --- DRAW PIANO ---
            val pianoTop = h * 0.68f
            val pianoBottom = h
            val whiteKeyWidth = w / 14f
            val blackKeyWidth = whiteKeyWidth * 0.55f
            val blackKeyHeight = (pianoBottom - pianoTop) * 0.6f

            val isLiveKeystroke = activeEvent != null && burstProgress.value > 0.01f && burstProgress.value < 0.35f

            val liveWhiteKeys = if (isLiveKeystroke && activeEvent != null) {
                val offset = if (activeEvent!!.isPlayerTurn) 0 else 7
                when (activeEvent!!.outcome) {
                    ShotOutcome.MISS -> listOf(1 + offset, 2 + offset)
                    ShotOutcome.HIT -> listOf(0 + offset, 2 + offset, 4 + offset)
                    ShotOutcome.SUNK -> listOf(0 + offset, 2 + offset, 4 + offset, 7 + offset)
                }
            } else emptyList()

            val liveBlackKeys = if (isLiveKeystroke && activeEvent != null) {
                val offset = if (activeEvent!!.isPlayerTurn) 0 else 7
                if (activeEvent!!.outcome == ShotOutcome.MISS) listOf(1 + offset) else emptyList()
            } else emptyList()

            for (i in 0 until 14) {
                val isLive = i in liveWhiteKeys
                val isAmbient = i in ambientWhiteKeys
                val keyPressed = isLive || isAmbient
                val keyBottomY = if (keyPressed) pianoBottom - 6.dp.toPx() else pianoBottom

                val keyColor = when {
                    isLive -> {
                        when(activeEvent!!.outcome) {
                            ShotOutcome.SUNK -> Color(0xFFD32F2F)
                            ShotOutcome.HIT -> Color(0xFFFF5722)
                            ShotOutcome.MISS -> Color(0xFF64B5F6)
                        }
                    }
                    isAmbient -> {
                        if (isVictory) Color(0xFFFFC107)
                        else if (isDefeat) Color(0xFF757575)
                        else Color(0xFF90A4AE)
                    }
                    else -> Color(0xFFF5F5F5)
                }

                drawRect(keyColor, Offset(i * whiteKeyWidth, pianoTop), Size(whiteKeyWidth, keyBottomY - pianoTop))
                drawRect(Color.Black.copy(alpha = 0.8f), Offset(i * whiteKeyWidth, pianoTop), Size(whiteKeyWidth, keyBottomY - pianoTop), style = Stroke(2f))

                if (keyPressed) {
                    drawRect(Color.Black.copy(alpha = 0.5f), Offset(i * whiteKeyWidth, keyBottomY), Size(whiteKeyWidth, 6.dp.toPx()))
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent),
                            startY = pianoTop,
                            endY = pianoTop + 16.dp.toPx()
                        ),
                        topLeft = Offset(i * whiteKeyWidth, pianoTop),
                        size = Size(whiteKeyWidth, 16.dp.toPx())
                    )
                }
            }

            val blackKeyIndices = listOf(0, 1, 3, 4, 5, 7, 8, 10, 11, 12)
            for (i in blackKeyIndices) {
                val isLive = i in liveBlackKeys
                val isAmbient = i in ambientBlackKeys
                val keyPressed = i in ambientBlackKeys || (activeEvent != null && i in liveBlackKeys && burstProgress.value > 0.01f && burstProgress.value < 0.35f)

                val keyBottomY = if (keyPressed) pianoTop + blackKeyHeight - 6.dp.toPx() else pianoTop + blackKeyHeight

                val keyColor = when {
                    isLive && keyPressed -> Color(0xFF64B5F6)
                    isAmbient -> {
                        if (isVictory) Color(0xFFFFC107)
                        else if (isDefeat) Color(0xFF757575)
                        else Color(0xFF78909C)
                    }
                    else -> Color(0xFF151515)
                }

                val startX = (i * whiteKeyWidth) + (whiteKeyWidth - blackKeyWidth / 2f)

                drawRect(keyColor, Offset(startX, pianoTop), Size(blackKeyWidth, keyBottomY - pianoTop))
                drawRect(Color.Black, Offset(startX, pianoTop), Size(blackKeyWidth, keyBottomY - pianoTop), style = Stroke(2f))

                if (keyPressed) {
                    drawRect(Color.Black.copy(alpha = 0.7f), Offset(startX, keyBottomY), Size(blackKeyWidth, 6.dp.toPx()))
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent),
                            startY = pianoTop,
                            endY = pianoTop + 16.dp.toPx()
                        ),
                        topLeft = Offset(startX, pianoTop),
                        size = Size(blackKeyWidth, 16.dp.toPx())
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(startX + 1.5f, pianoTop),
                        end = Offset(startX + 1.5f, keyBottomY),
                        strokeWidth = 2f
                    )
                } else {
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(startX + 2f, pianoTop + 2f),
                        end = Offset(startX + 2f, keyBottomY - 4f),
                        strokeWidth = 2f
                    )
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(startX, keyBottomY - 4.dp.toPx()),
                        size = Size(blackKeyWidth, 4.dp.toPx())
                    )
                }
            }

            // --- DRAW AMBIENT SHEET MUSIC ---
            val ambientAlpha = 0.08f
            val loopWidth = w * 1.5f
            val targetNoteCount = 6 + ((currentBpm - 40) / 140f * 8).roundToInt()
            val melodyLines = listOf(1.5f, 3f, 0.5f, 2.5f, 1f, 3.5f, 2f, 0f, 1.5f, 2.5f)

            for (i in 0 until targetNoteCount) {
                val spacingOffset = i.toFloat() / targetNoteCount
                val yPos = melodyLines[i % melodyLines.size]
                val isEighth = i % 2 != 0
                drawNativeNote(
                    xOffset = (((ambientProgress + spacingOffset) * loopWidth) % loopWidth) - 50f,
                    yLineIndex = yPos,
                    color = Color.White,
                    alpha = ambientAlpha,
                    isEighthNote = isEighth
                )
            }

            // --- DRAW REACTIVE SHEET MUSIC ---
            if (activeEvent != null && burstProgress.value > 0f && burstProgress.value < 1f) {
                val bProg = burstProgress.value
                val burstAlpha = (1f - bProg) * 0.9f

                val startX = if (activeEvent!!.isPlayerTurn) w * 0.15f else w * 0.85f
                val endX = if (activeEvent!!.isPlayerTurn) w * 0.85f else w * 0.15f
                val currentX = startX + ((endX - startX) * bProg)

                val noteColor = activeEvent!!.frozenState.reactiveNoteColor

                when (activeEvent!!.outcome) {
                    ShotOutcome.MISS -> {
                        val erraticY = sin(bProg * 25f) * 0.8f
                        drawNativeNote(currentX, 1.5f + erraticY, noteColor, burstAlpha, true)
                        drawNativeNote(currentX - 25f, 2.5f - erraticY, noteColor, burstAlpha, true)
                    }
                    ShotOutcome.HIT -> {
                        drawNativeNote(currentX, 1f, noteColor, burstAlpha, true)
                        drawNativeNote(currentX - 15f, 1.5f, noteColor, burstAlpha)
                        drawNativeNote(currentX - 30f, 2f, noteColor, burstAlpha, true)
                    }
                    ShotOutcome.SUNK -> {
                        drawNativeNote(currentX, 0.5f, Color(0xFFD32F2F), burstAlpha, true)
                        drawNativeNote(currentX - 15f, 1f, Color(0xFFD32F2F), burstAlpha)
                        drawNativeNote(currentX - 30f, 1.5f, Color(0xFFD32F2F), burstAlpha, true)
                        drawNativeNote(currentX - 45f, 2f, Color(0xFFD32F2F), burstAlpha)
                        drawNativeNote(currentX - 60f, 3.5f, Color(0xFFD32F2F), burstAlpha, true)
                    }
                }
            }
        }

        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.maxValue) {
            if (scrollState.maxValue > 0) {
                while (true) {
                    delay(1500)
                    scrollState.animateScrollTo(scrollState.maxValue, tween(3000, easing = LinearEasing))
                    delay(1500)
                    scrollState.scrollTo(0)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp)
        ) {
            val currentOpus = (animatedProb * 100).roundToInt()
            Text(
                text = "${state.movement}  //  OP. $currentOpus",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Black,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.statusText,
                fontSize = 15.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = state.critiqueColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            )
        }
    }
}
