package com.ak.battleship.ui.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ak.battleship.model.ShotOutcome
import kotlin.math.PI
import kotlin.math.roundToInt

data class PokerState(
    val narrative: String,
    val handName: String,
    val cards: List<Pair<String, String>>,
    val suitColors: List<Color>
)

@Composable
fun HighStakesPokerWidget(
    probability: Float,
    // --- EVENT TRIGGERS INJECTED ---
    shotOutcome: ShotOutcome = ShotOutcome.MISS,
    commandKey: Int = 0,
    isPlayerTurn: Boolean = true
) {
    val animatedProb by animateFloatAsState(targetValue = probability, tween(1000), label = "PokerAnim")
    val textMeasurer = rememberTextMeasurer()

    // --- ANIMATION ENGINE: The Sequential Chip Toss ---
    val payProgress = remember { Animatable(0f) }
    val takeProgress = remember { Animatable(0f) }

    LaunchedEffect(commandKey) {
        if (commandKey > 0) {
            payProgress.snapTo(0f)
            takeProgress.snapTo(0f)

            // 1. The loser pushes chips into the pot
            payProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )

            // 2. The dramatic pause on the felt
            kotlinx.coroutines.delay(200)

            // 3. The winner drags the pot to their stack
            takeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }
    }

    val redSuit = Color(0xFFD32F2F)
    val blackSuit = Color(0xFF212121)

    // --- 7 STAGE ENGINE ---
    val state = when {
        animatedProb >= 1f -> PokerState(
            "All in. The nuts. They are drawing dead.", "ROYAL FLUSH",
            listOf("A" to "♠", "K" to "♠", "Q" to "♠", "J" to "♠", "10" to "♠"),
            listOf(blackSuit, blackSuit, blackSuit, blackSuit, blackSuit)
        )
        animatedProb > 0.85f -> PokerState(
            "Massive advantage. Extract maximum value.", "FOUR OF A KIND",
            listOf("A" to "♥", "A" to "♦", "A" to "♣", "A" to "♠", "K" to "♥"),
            listOf(redSuit, redSuit, blackSuit, blackSuit, redSuit)
        )
        animatedProb > 0.60f -> PokerState(
            "Strong position. Force them to fold.", "FULL HOUSE",
            listOf("K" to "♥", "K" to "♦", "K" to "♣", "8" to "♠", "8" to "♥"),
            listOf(redSuit, redSuit, blackSuit, blackSuit, redSuit)
        )
        animatedProb > 0.40f -> PokerState(
            "Playing the flop. Proceed with caution.", "TWO PAIR",
            listOf("J" to "♣", "J" to "♦", "4" to "♠", "4" to "♥", "9" to "♣"),
            listOf(blackSuit, redSuit, blackSuit, redSuit, blackSuit)
        )
        animatedProb > 0.15f -> PokerState(
            "Bluffing with air. Getting expensive.", "WEAK PAIR",
            listOf("5" to "♠", "5" to "♦", "Q" to "♣", "8" to "♥", "2" to "♠"),
            listOf(blackSuit, redSuit, blackSuit, redSuit, blackSuit)
        )
        animatedProb > 0f -> PokerState(
            "Busted draw. Don't chase the river.", "7-HIGH JUNK",
            listOf("7" to "♣", "5" to "♦", "4" to "♥", "3" to "♠", "2" to "♣"),
            listOf(blackSuit, redSuit, redSuit, blackSuit, blackSuit)
        )
        else -> PokerState(
            "Liquidated. The house takes it all.", "FOLDED",
            listOf("2" to "♣", "4" to "♦", "8" to "♠", "J" to "♥", "Q" to "♣"),
            listOf(blackSuit, redSuit, blackSuit, redSuit, blackSuit)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // --- 1. THE CUTOUT TABLE ---
            val tableMarginX = w * 0.05f
            val tableMarginY = h * 0.18f
            val tableRect = androidx.compose.ui.geometry.Rect(
                left = tableMarginX, top = tableMarginY,
                right = w - tableMarginX, bottom = h - (tableMarginY * 0.5f)
            )

            drawRoundRect(
                color = Color(0xFF1E1E1E),
                topLeft = Offset(tableRect.left, tableRect.top),
                size = Size(tableRect.width, tableRect.height),
                cornerRadius = CornerRadius(30.dp.toPx(), 30.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF1B5E20),
                topLeft = Offset(tableRect.left + 8.dp.toPx(), tableRect.top + 8.dp.toPx()),
                size = Size(tableRect.width - 16.dp.toPx(), tableRect.height - 16.dp.toPx()),
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
            )

            // --- 2. THE CHIP ENGINE ---
            val chipRadius = 10.dp.toPx()
            val chipWidth = chipRadius * 2
            val chipHeight = chipWidth * 0.5f
            val chipThickness = 3.5.dp.toPx()

            fun drawIsometricChipStack(baseX: Float, baseY: Float, chipCount: Int, faceColor: Color, edgeColor: Color) {
                if (chipCount <= 0) return
                for (i in 0 until chipCount) {
                    val currentY = baseY - (i * chipThickness)
                    drawOval(edgeColor, topLeft = Offset(baseX, currentY + chipThickness), size = Size(chipWidth, chipHeight))
                    drawOval(faceColor, topLeft = Offset(baseX, currentY), size = Size(chipWidth, chipHeight))
                    drawOval(
                        color = Color.White.copy(alpha = 0.5f),
                        topLeft = Offset(baseX + (chipWidth * 0.15f), currentY + (chipHeight * 0.15f)),
                        size = Size(chipWidth * 0.7f, chipHeight * 0.7f),
                        style = Stroke(width = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))
                    )
                }
            }

            fun drawChipMountain(baseX: Float, baseY: Float, totalChips: Int, faceColor: Color, edgeColor: Color) {
                if (totalChips <= 0) return
                val base = totalChips / 3
                val remainder = totalChips % 3
                val leftCount = base + (if (remainder > 0) 1 else 0)
                val centerCount = base + (if (remainder > 1) 1 else 0)
                val rightCount = base
                val offsetX = chipRadius * 1.8f
                val offsetY = chipRadius * 0.6f
                val shadowColor = Color.Black
                val highlightColor = Color.White
                val leftFace = androidx.compose.ui.graphics.lerp(faceColor, shadowColor, 0.2f)
                val leftEdge = androidx.compose.ui.graphics.lerp(edgeColor, shadowColor, 0.2f)
                val rightFace = androidx.compose.ui.graphics.lerp(faceColor, highlightColor, 0.15f)
                val rightEdge = androidx.compose.ui.graphics.lerp(edgeColor, highlightColor, 0.15f)

                drawIsometricChipStack(baseX - offsetX, baseY - offsetY, leftCount, leftFace, leftEdge)
                drawIsometricChipStack(baseX + offsetX, baseY - offsetY, rightCount, rightFace, rightEdge)
                drawIsometricChipStack(baseX, baseY, centerCount, faceColor, edgeColor)
            }

            val maxChips = 45
            val playerChips = (animatedProb * maxChips).roundToInt().coerceIn(1, maxChips - 1)
            val oppChips = maxChips - playerChips

            drawChipMountain(w * 0.75f, h * 0.68f, oppChips, Color(0xFFD32F2F), Color(0xFF8E0000))
            drawChipMountain(w * 0.18f, h * 0.68f, playerChips, Color(0xFF1976D2), Color(0xFF0D47A1))

            // --- THE NEW POT (Center of table) ---
            val potX = w * 0.48f
            val potY = h * 0.60f
            // Base chips already sitting in the pot
            drawIsometricChipStack(potX - chipRadius, potY - chipRadius, 3, Color(0xFFD32F2F), Color(0xFF8E0000))
            drawIsometricChipStack(potX + chipRadius, potY, 5, Color(0xFF1976D2), Color(0xFF0D47A1))

            // --- ANIMATION: CHIP TOSS (Sequential Exchange) ---
            val payProg = payProgress.value
            val takeProg = takeProgress.value

            if (payProg > 0f || takeProg > 0f) {

                // THE FIX: Account for whose turn it actually is!
                val isPlayerWinningExchange = if (isPlayerTurn) {
                    shotOutcome == ShotOutcome.HIT || shotOutcome == ShotOutcome.SUNK
                } else {
                    shotOutcome == ShotOutcome.MISS
                }

                val chipsToToss = if (shotOutcome == ShotOutcome.SUNK) 3 else 1

                val playerStackX = w * 0.18f
                val oppStackX = w * 0.75f
                val stackY = h * 0.68f

                val playerFace = Color(0xFF1976D2)
                val playerEdge = Color(0xFF0D47A1)
                val oppFace = Color(0xFFD32F2F)
                val oppEdge = Color(0xFF8E0000)

                // Helper to draw a clean arc based on the SPECIFIC progress variable passed to it
                fun drawArc(prog: Float, startX: Float, startY: Float, endX: Float, endY: Float, faceColor: Color, edgeColor: Color) {
                    if (prog <= 0f || prog >= 1f) return

                    val currentX = startX + ((endX - startX) * prog)
                    val linearY = startY + ((endY - startY) * prog)
                    val arcHeight = 40.dp.toPx() * kotlin.math.sin(prog * PI).toFloat()
                    val currentY = linearY - arcHeight
                    drawIsometricChipStack(currentX, currentY, chipsToToss, faceColor, edgeColor)
                }

                if (isPlayerWinningExchange) {
                    // SCENARIO 1: Player Wins (Opponent pays pot, pause, Player takes pot)
                    drawArc(payProg, oppStackX, stackY, potX, potY, oppFace, oppEdge)
                    drawArc(takeProg, potX, potY, playerStackX, stackY, playerFace, playerEdge)
                } else {
                    // SCENARIO 2: Opponent Wins (Player pays pot, pause, Opponent takes pot)
                    drawArc(payProg, playerStackX, stackY, potX, potY, playerFace, playerEdge)
                    drawArc(takeProg, potX, potY, oppStackX, stackY, oppFace, oppEdge)
                }
            }

            // --- 3. THE NARROW CARDS ---
            // Z-Sorted to draw ON TOP of the chips
            val cardHeight = h * 0.45f
            val cardWidth = cardHeight * 0.7f
            val pivotY = h * 1.4f
            val angles = listOf(-18f, -9f, 0f, 9f, 18f)

            for (i in 0..4) {
                rotate(degrees = angles[i], pivot = Offset(w / 2f, pivotY)) {
                    val cardTopLeft = Offset((w / 2f) - (cardWidth / 2f), h * 0.36f)

                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(cardTopLeft.x + 4f, cardTopLeft.y + 6f),
                        size = Size(cardWidth, cardHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = cardTopLeft,
                        size = Size(cardWidth, cardHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color.LightGray,
                        topLeft = cardTopLeft,
                        size = Size(cardWidth, cardHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 1f)
                    )

                    val rankLayout = textMeasurer.measure(
                        text = state.cards[i].first,
                        style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Black, color = state.suitColors[i])
                    )
                    drawText(textLayoutResult = rankLayout, topLeft = Offset(cardTopLeft.x + 4.dp.toPx(), cardTopLeft.y + 2.dp.toPx()))

                    val suitLayout = textMeasurer.measure(
                        text = state.cards[i].second,
                        style = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = state.suitColors[i])
                    )
                    drawText(textLayoutResult = suitLayout, topLeft = Offset(cardTopLeft.x + 4.dp.toPx(), cardTopLeft.y + 14.dp.toPx()))
                }
            }
        }

        // --- LAYER 3: FLOATING DEALER PLAQUE ---
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.8f))
                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${(animatedProb * 100).roundToInt()}%",
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = state.handName,
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
