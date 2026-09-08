package com.ak.battleship.ui.widgets


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.ak.battleship.model.PlaybackCommand
import com.ak.battleship.model.ShotOutcome
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class AquaState(
    val species: String,
    val fishColor: Color,
    val scale: Float,
    val isPike: Boolean = false,
    val isWalleye: Boolean = false,
    val isPerch: Boolean = false,
    val isMusky: Boolean = false,
    val isMinnow: Boolean = false,
    val isShrimp: Boolean = false
)

data class CameraEvent(
    val outcome: ShotOutcome,
    val isPlayerTurn: Boolean,
    val frozenFish: AquaState
)

object FishGeometry {
    val pikeSnout = Path().apply { moveTo(95f, 6f); lineTo(115f, 8f); lineTo(95f, 14f); close() }
    val pikeFin1 = Path().apply { moveTo(15f, 2f); lineTo(25f, -10f); lineTo(32f, 2f); close() }
    val pikeFin2 = Path().apply { moveTo(15f, 16f); lineTo(25f, 28f); lineTo(32f, 16f); close() }
    val pikeFin3 = Path().apply { moveTo(5f, 9f); lineTo(-15f, -8f); lineTo(-10f, 9f); lineTo(-15f, 26f); close() }

    val walleyeFin1 = Path().apply { moveTo(30f, 2f); lineTo(45f, -20f); lineTo(60f, 2f); close() }
    val walleyeFin2 = Path().apply { moveTo(10f, 2f); lineTo(18f, -10f); lineTo(25f, 2f); close() }
    val walleyeFin3 = Path().apply { moveTo(5f, 12f); lineTo(-15f, -5f); lineTo(-10f, 12f); lineTo(-15f, 29f); close() }
    val walleyeFin4 = Path().apply { moveTo(-10f, 12f); lineTo(-15f, 29f); lineTo(-2f, 20f); close() }

    val perchFin1 = Path().apply { moveTo(25f, -2f); lineTo(35f, -22f); lineTo(48f, -2f); close() }
    val perchFin2 = Path().apply { moveTo(10f, -2f); lineTo(15f, -12f); lineTo(22f, -2f); close() }
    val perchStripe1 = Path().apply { moveTo(20f, -2f); lineTo(25f, 24f); lineTo(15f, -2f); close() }
    val perchStripe2 = Path().apply { moveTo(35f, -4f); lineTo(40f, 22f); lineTo(30f, -4f); close() }
    val perchStripe3 = Path().apply { moveTo(50f, 0f); lineTo(52f, 16f); lineTo(45f, 0f); close() }
    val perchOrangeFin1 = Path().apply { moveTo(25f, 26f); lineTo(20f, 40f); lineTo(35f, 26f); close() }
    val perchOrangeFin2 = Path().apply { moveTo(45f, 20f); lineTo(40f, 32f); lineTo(50f, 20f); close() }
    val perchBottomFin = Path().apply { moveTo(5f, 12f); lineTo(-12f, -2f); lineTo(-8f, 12f); lineTo(-12f, 26f); close() }

    val muskyFin3 = Path().apply { moveTo(5f, 10f); lineTo(-15f, -8f); lineTo(-10f, 10f); lineTo(-15f, 28f); close() }

    val minnowTail = Path().apply { moveTo(5f, 6f); lineTo(-5f, 0f); lineTo(-5f, 12f); close() }

    val shrimpBody = Path().apply {
        moveTo(25f, 10f)
        quadraticBezierTo(10f, -5f, 0f, 10f)
        quadraticBezierTo(10f, 8f, 25f, 10f)
        close()
    }
    val shrimpTail = Path().apply { moveTo(0f, 10f); lineTo(-5f, 5f); lineTo(-5f, 15f); close() }
}

fun getAquaStateForProb(prob: Float): AquaState {
    return when {
        prob >= 1f -> AquaState("MONSTER MUSKY", Color(0xFF4E5C4E), 3.6f, isMusky = true)
        prob > 0.85f -> AquaState("NORTHERN PIKE", Color(0xFF43A047), 2.2f, isPike = true)
        prob > 0.60f -> AquaState("KEEPER WALLEYE", Color(0xFFFFB300), 1.1f, isWalleye = true)
        prob > 0.40f -> AquaState("JUMBO PERCH", Color(0xFFFFEB3B), 0.8f, isPerch = true)
        prob > 0.15f -> AquaState("MINNOW", Color(0xFFB0BEC5), 0.4f, isMinnow = true)
        prob > 0f -> AquaState("SMALL SHRIMP", Color(0xFFFFCCBC), 0.3f, isShrimp = true)
        else -> AquaState("WEEDS / DEBRIS", Color(0xFF558B2F), 0f)
    }
}

@Suppress("RestrictedApi")
@Composable
fun SubmersibleCameraWidget(
    probability: Float,
    stats: List<Pair<String, String>> = emptyList(),
    shotOutcome: ShotOutcome = ShotOutcome.MISS,
    isPlayerTurn: Boolean = true,
    command: PlaybackCommand = PlaybackCommand.IDLE,
    commandKey: Int = 0
) {
    val animatedProb by animateFloatAsState(targetValue = probability, tween(1200), label = "DepthAnim")

    val state = getAquaStateForProb(animatedProb)

    val infiniteTransition = rememberInfiniteTransition(label = "AquaAnim")
    val patrolOffset = remember { Animatable(-0.3f) }
    var patrolState by remember { mutableStateOf(AquaState("WEEDS / DEBRIS", Color(0xFF558B2F), 0f)) }

    LaunchedEffect(Unit) {
        while (true) {
            val safePatrolProb = if (animatedProb >= 1f) 0.99f else animatedProb
            patrolState = getAquaStateForProb(safePatrolProb)
            patrolOffset.snapTo(-0.3f)
            patrolOffset.animateTo(1.3f, animationSpec = tween(18000, easing = LinearEasing))
        }
    }

    val blink by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), label = "Blink")
    val weedClock by infiniteTransition.animateFloat(0f, 1000f, infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse), label = "WeedClock")
    val lureBob by infiniteTransition.animateFloat(0f, 1000f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = "LureBob")
    val lureSway by infiniteTransition.animateFloat(0f, 1000f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "LureSway")

    val strikeProgress = remember { Animatable(0f) }
    val historicalProg = remember { Animatable(0f) }
    val ambientLoopClock = remember { Animatable(0f) }

    var activeEvent by remember { mutableStateOf<CameraEvent?>(null) }
    var processedCommandKey by remember { mutableStateOf(commandKey) }

    LaunchedEffect(commandKey, command) {
        processedCommandKey = commandKey

        activeEvent = null
        strikeProgress.snapTo(0f)
        historicalProg.snapTo(0f)
        ambientLoopClock.snapTo(0f)

        when (command) {
            PlaybackCommand.IDLE,
            PlaybackCommand.TIME_TRAVEL_BACKWARD,
            PlaybackCommand.TIME_TRAVEL_FORWARD,
            PlaybackCommand.PLAY_JUMP -> {
                // Resolved.
            }
            PlaybackCommand.PLAY_WIN_CINEMATIC -> {
                delay(300)
                historicalProg.animateTo(1f, animationSpec = tween(2500, easing = FastOutSlowInEasing))
            }
            PlaybackCommand.PLAY_LOSS_CINEMATIC -> {
                // Handled implicitly by `isDefeat` triggering the ambient boot loop
            }
            PlaybackCommand.PLAY_MISS,
            PlaybackCommand.PLAY_HIT,
            PlaybackCommand.PLAY_SUNK,
            PlaybackCommand.PLAY_WIN_LIVE -> {

                val enforcedOutcome = when (command) {
                    PlaybackCommand.PLAY_WIN_LIVE, PlaybackCommand.PLAY_SUNK -> ShotOutcome.SUNK
                    PlaybackCommand.PLAY_HIT -> ShotOutcome.HIT
                    else -> ShotOutcome.MISS
                }

                // THE FIX: Explicitly force the Musky state if this is the winning command!
                val rawState = if (command == PlaybackCommand.PLAY_WIN_LIVE) {
                    getAquaStateForProb(1.0f)
                } else {
                    getAquaStateForProb(probability)
                }

                val lockedState = if (rawState.scale == 0f && (enforcedOutcome == ShotOutcome.HIT || enforcedOutcome == ShotOutcome.SUNK)) {
                    AquaState("SURPRISE PERCH", Color(0xFFFFC107), 0.8f, isPerch = true)
                } else rawState

                activeEvent = CameraEvent(enforcedOutcome, isPlayerTurn, lockedState)
                strikeProgress.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing))

                if (enforcedOutcome != ShotOutcome.SUNK) {
                    activeEvent = null
                }
            }
        }
    }

    val isVictory = probability >= 1f
    val isDefeat = probability <= 0f
    val animOutcome = activeEvent?.outcome ?: ShotOutcome.MISS
    val animIsPlayer = activeEvent?.isPlayerTurn ?: true

    val isPendingAnimation = commandKey != processedCommandKey

    // THE FIX: Remove "> 0f". Frame 0.0f counts as active!
    val isHistoricalIntroPlaying = command == PlaybackCommand.PLAY_WIN_CINEMATIC && historicalProg.value < 1f

    // THE FIX: Remove "> 0f". Frame 0.0f counts as active!
    val isLiveActionPlaying = activeEvent != null && strikeProgress.value < 1f

    val isActionOverlayActive = isHistoricalIntroPlaying || isLiveActionPlaying || isPendingAnimation

    LaunchedEffect(isVictory, isDefeat, isActionOverlayActive) {
        if ((isVictory || isDefeat) && !isActionOverlayActive) {
            ambientLoopClock.snapTo(0f)
            ambientLoopClock.animateTo(1000f, animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing)))
        } else {
            ambientLoopClock.stop()
        }
    }

    val weedPaths = remember { Array(150) { Path() } }
    val rootPaths = remember { Array(150) { Path() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(3.dp, Color(0xFF424242), RoundedCornerShape(12.dp))
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val shallowSurface = Color(0xFF006064)
            val shallowDeep = Color(0xFF002B36)
            val shallowVignette = 0.4f

            val abyssSurface = Color(0xFF003B46)
            val abyssDeep = Color(0xFF001218)
            val abyssVignette = 0.7f

            val currentSurfaceColor = androidx.compose.ui.graphics.lerp(shallowSurface, abyssSurface, animatedProb)
            val currentDeepColor = androidx.compose.ui.graphics.lerp(shallowDeep, abyssDeep, animatedProb)
            val currentVignetteAlpha = androidx.compose.ui.util.lerp(shallowVignette, abyssVignette, animatedProb)

            drawRect(brush = androidx.compose.ui.graphics.Brush.verticalGradient(colors = listOf(currentSurfaceColor, currentDeepColor)))
            drawRect(brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = currentVignetteAlpha)),
                center = Offset(w / 2, h / 2),
                radius = w * 0.7f
            ))

            fun drawFish(x: Float, y: Float, drawState: AquaState) {
                if (drawState.scale == 0f) return

                withTransform({
                    translate(x, y)
                    scale(drawState.scale, drawState.scale, pivot = Offset.Zero)
                    rotate(sin(x / 20f) * 5f, pivot = Offset.Zero)
                }) {
                    drawOval(Color.Black.copy(alpha = 0.4f), Offset(-2f, 4f), Size(60f, 20f))

                    if (drawState.isPike) {
                        val pikeGreen = Color(0xFF43A047)
                        val pikeFins = Color(0xFFE65100)
                        drawOval(pikeGreen, Offset(0f, 0f), Size(100f, 18f))
                        drawPath(FishGeometry.pikeSnout, pikeGreen)
                        drawPath(FishGeometry.pikeFin1, pikeFins)
                        drawPath(FishGeometry.pikeFin2, pikeFins)
                        drawPath(FishGeometry.pikeFin3, pikeFins)
                        val spotColor = Color(0xFFA5D6A7).copy(alpha = 0.7f)
                        drawCircle(spotColor, 1.5f, Offset(40f, 6f)); drawCircle(spotColor, 1f, Offset(60f, 10f))
                        drawCircle(spotColor, 1.5f, Offset(80f, 8f)); drawCircle(spotColor, 1f, Offset(50f, 12f))
                    } else if (drawState.isWalleye) {
                        val wallGold = Color(0xFFD4AF37)
                        val wallDark = Color(0xFF5D4037)
                        drawOval(wallGold, Offset(0f, 0f), Size(80f, 24f))
                        drawPath(FishGeometry.walleyeFin1, wallDark)
                        drawPath(FishGeometry.walleyeFin2, wallGold)
                        drawPath(FishGeometry.walleyeFin3, wallGold)
                        drawPath(FishGeometry.walleyeFin4, Color.White)
                        drawCircle(Color.White, 3f, Offset(65f, 8f)); drawCircle(Color.Red.copy(alpha = 0.8f), 1.5f, Offset(65f, 8f))
                    } else if (drawState.isPerch) {
                        val perchYellow = Color(0xFFFFCA28)
                        val perchGreen = Color(0xFF558B2F)
                        val perchOrange = Color(0xFFFF5722)
                        drawOval(perchYellow, Offset(0f, -4f), Size(60f, 32f))
                        drawPath(FishGeometry.perchFin1, perchGreen)
                        drawPath(FishGeometry.perchFin2, perchYellow)
                        val stripeColor = Color.Black.copy(alpha = 0.7f)
                        drawPath(FishGeometry.perchStripe1, stripeColor)
                        drawPath(FishGeometry.perchStripe2, stripeColor)
                        drawPath(FishGeometry.perchStripe3, stripeColor)
                        drawPath(FishGeometry.perchOrangeFin1, perchOrange)
                        drawPath(FishGeometry.perchOrangeFin2, perchOrange)
                        drawPath(FishGeometry.perchBottomFin, perchGreen)
                    } else if (drawState.isMusky) {
                        val muskyBase = Color(0xFF546E7A)
                        val muskyFins = Color(0xFF8D6E63)
                        val muskyMarkings = Color(0xFF263238)
                        drawOval(muskyBase, Offset(0f, 0f), Size(100f, 20f))
                        drawPath(FishGeometry.pikeSnout, muskyBase)
                        drawPath(FishGeometry.pikeFin1, muskyFins)
                        drawPath(FishGeometry.pikeFin2, muskyFins)
                        drawPath(FishGeometry.muskyFin3, muskyFins)
                        for (i in 0..5) drawRect(muskyMarkings.copy(alpha = 0.5f), Offset(35f + (i * 10f), 4f), Size(3f, 12f))
                        drawCircle(Color.Black, 1.5f, Offset(100f, 8f))
                    } else if (drawState.isMinnow) {
                        val minnowSilver = Color(0xFFCFD8DC)
                        val minnowDark = Color(0xFF90A4AE)
                        drawOval(minnowSilver, Offset(0f, 0f), Size(45f, 12f))
                        drawPath(FishGeometry.minnowTail, minnowDark)
                        drawCircle(Color.Black, 1f, Offset(35f, 5f))
                        drawLine(minnowDark, Offset(10f, 6f), Offset(40f, 6f), 1f)
                    } else if (drawState.isShrimp) {
                        val shrimpColor = Color(0xFFFFAB91)
                        drawPath(FishGeometry.shrimpBody, shrimpColor)
                        drawPath(FishGeometry.shrimpTail, shrimpColor)
                        for (i in 0..2) drawLine(shrimpColor, Offset(5f + (i * 5f), 10f), Offset(5f + (i * 5f) - 2f, 14f), 1f)
                        drawLine(shrimpColor, Offset(25f, 10f), Offset(35f, 15f), 1f)
                    }
                }
            }

            val numWeeds = (w / 15f).toInt()
            drawRect(Color(0xFF051C08), Offset(0f, h - 10f), Size(w, 10f))

            for (i in 0..numWeeds) {
                if (i >= 150) break
                val baseX = i * 15f
                val sway = kotlin.math.sin(weedClock * 0.03f + i) * 12f
                val isForeground = i % 2 == 0
                val height = if (i % 5 == 0) 65f else if (isForeground) 35f else 45f
                val weedColor = if (isForeground) Color(0xFF006721) else Color(0xFF27672B)

                val weedPath = weedPaths[i]
                weedPath.reset()
                weedPath.moveTo(baseX, h)
                weedPath.quadraticBezierTo(baseX + 10f + (sway * 0.5f), h - (height * 0.5f), baseX - 5f + sway, h - height)
                drawPath(path = weedPath, color = weedColor, style = Stroke(if (isForeground) 6f else 4f, cap = StrokeCap.Round))

                if (i < numWeeds - 1) {
                    val tangleSway = kotlin.math.cos(weedClock * 0.04f + i) * 6f
                    val rootColor = if (isForeground) Color(0xFF14571A) else Color(0xFF103D14)
                    val rootPath = rootPaths[i]
                    rootPath.reset()
                    rootPath.moveTo(baseX, h - 5f)
                    rootPath.quadraticBezierTo(baseX + 20f, h - 20f + tangleSway, baseX + 45f, h - 2f)
                    drawPath(path = rootPath, color = rootColor, style = Stroke(8f, cap = StrokeCap.Round))
                }
            }

            val lureX = w / 2f
            val baseLureY = h * 0.4f
            val hookY = baseLureY + 12.dp.toPx()

            val actionState = activeEvent?.frozenFish ?: if (state.scale == 0f) AquaState("SURPRISE PERCH", Color(0xFFFFC107), 0.8f, isPerch = true) else state

            val snoutOffset = when {
                actionState.isMusky || actionState.isPike -> 115f
                actionState.isWalleye -> 80f
                actionState.isPerch -> 60f
                actionState.isMinnow -> 45f
                actionState.isShrimp -> 25f
                else -> 60f
            } * actionState.scale

            val isHookSet = (isLiveActionPlaying && animOutcome == ShotOutcome.SUNK && animIsPlayer && strikeProgress.value > 0.3f) ||
                    (isHistoricalIntroPlaying && historicalProg.value > 0.22f)

            // THE FIX: Define when the ambient victory loop actually owns the screen
            val hideForVictoryLoop = isVictory && !isActionOverlayActive

            // THE FIX: Replace `!isVictory` with `!hideForVictoryLoop` so the lure stays alive during the ambush!
            // Idle Lure
            if (!isDefeat && !isHookSet && !hideForVictoryLoop) {
                val currentBob = kotlin.math.sin(lureBob * 0.03f) * 3f
                val currentSway = kotlin.math.sin(lureSway * 0.02f) * 5f
                val activeLureX = lureX + currentSway
                val activeHookY = hookY + currentBob

                drawLine(Color.White.copy(alpha = 0.3f), Offset(lureX, 0f), Offset(activeLureX, activeHookY - 30f), 1.5f)

                withTransform({
                    translate(activeLureX, activeHookY - 30f)
                    rotate((currentSway / 5f) * -2f, pivot = Offset.Zero)
                }) {
                    drawRect(Color.White, Offset(-2f, 0f), Size(4f, 15f))
                    drawCircle(Color(0xFFFF1744), 6f, Offset(0f, 0f))
                }
            }

            // --- AMBIENT LAYER ---
            if (isVictory && !isActionOverlayActive) {
                val battleY = hookY
                val safeTime = if (!ambientLoopClock.isRunning) 0f else ambientLoopClock.value
                val time = ((safeTime * 0.015f) + 0.175f) % 1f
                val maxStruggle = 90f

                val struggleX: Float; val smoothSign: Float; val tiltAngle: Float; val depthMultiplier: Float

                if (time < 0.35f) {
                    val p = time / 0.35f
                    struggleX = androidx.compose.ui.util.lerp(-maxStruggle, maxStruggle, p)
                    smoothSign = 1f; tiltAngle = 0f; depthMultiplier = 1f
                } else if (time < 0.50f) {
                    val p = (time - 0.35f) / 0.15f
                    val turnTheta = p * kotlin.math.PI.toFloat()
                    struggleX = maxStruggle + sin(turnTheta) * 15f
                    smoothSign = cos(turnTheta); tiltAngle = sin(turnTheta) * -5f; depthMultiplier = 1f - sin(turnTheta) * 0.06f
                } else if (time < 0.85f) {
                    val p = (time - 0.50f) / 0.35f
                    struggleX = androidx.compose.ui.util.lerp(maxStruggle, -maxStruggle, p)
                    smoothSign = -1f; tiltAngle = 0f; depthMultiplier = 1f
                } else {
                    val p = (time - 0.85f) / 0.15f
                    val turnTheta = p * kotlin.math.PI.toFloat()
                    struggleX = -maxStruggle - sin(turnTheta) * 15f
                    smoothSign = -cos(turnTheta); tiltAngle = sin(turnTheta) * 5f; depthMultiplier = 1f - sin(turnTheta) * 0.06f
                }

                val currentLureX = lureX + struggleX
                val tiltRad = tiltAngle * (kotlin.math.PI.toFloat() / 180f)

                val rawVictoryState = getAquaStateForProb(1.0f)
                val localSnoutY = 8f * rawVictoryState.scale
                val localSnoutOffset = 115f * rawVictoryState.scale

                val rotX = (localSnoutOffset * cos(tiltRad)) - (localSnoutY * sin(tiltRad))
                val rotY = (localSnoutOffset * sin(tiltRad)) + (localSnoutY * cos(tiltRad))

                drawLine(Color.White, Offset(lureX, 0f), Offset(currentLureX + (rotX * smoothSign * depthMultiplier), battleY + (rotY * depthMultiplier)), 3f)

                withTransform({
                    translate(currentLureX, battleY)
                    scale(scaleX = smoothSign * depthMultiplier, scaleY = depthMultiplier, pivot = Offset.Zero)
                    rotate(tiltAngle, pivot = Offset.Zero)
                }) { drawFish(0f, 0f, rawVictoryState) }

            } else if (isDefeat && !isActionOverlayActive) {
                val tugTime = (ambientLoopClock.value * 0.04f) % 1f
                val tugYOffset = if (tugTime < 0.15f) {
                    val p = tugTime / 0.15f
                    -14f * (1f - p) * kotlin.math.sin(p * kotlin.math.PI.toFloat()).toFloat()
                } else 0f

                val stuckHookX = lureX - 50.dp.toPx()
                val bootBaseY = h
                val bootTilt = if (tugTime < 0.15f) tugYOffset * 0.4f else 0f

                withTransform({ translate(stuckHookX, bootBaseY); rotate(bootTilt, pivot = Offset(60f, 0f)) }) {
                    drawRoundRect(Color(0xFF212121), Offset(-15f, -8f), Size(85f, 10f), CornerRadius(4f))
                    drawRect(Color(0xFF4E342E), Offset(-15f, -16f), Size(25f, 10f))
                    drawRoundRect(Color(0xFF5D4037), Offset(-10f, -32f), Size(75f, 26f), CornerRadius(6f))
                    drawRoundRect(Color(0xFF5D4037), Offset(-15f, -65f), Size(38f, 45f), CornerRadius(6f))
                    drawOval(Color(0xFF4E342E), Offset(-15f, -68f), Size(38f, 8f))
                    drawRoundRect(Color.Black.copy(alpha = 0.4f), Offset(-15f, -65f), Size(38f, 45f), CornerRadius(6f), style = Stroke(2f))
                    drawRoundRect(Color.Black.copy(alpha = 0.4f), Offset(-10f, -32f), Size(75f, 26f), CornerRadius(6f), style = Stroke(2f))
                }

                val tiltRad = bootTilt * (kotlin.math.PI.toFloat() / 180f)
                val rotatedCollarX = stuckHookX + (-15f * kotlin.math.cos(tiltRad) - (-65f) * kotlin.math.sin(tiltRad))
                val rotatedCollarY = bootBaseY + (-15f * kotlin.math.sin(tiltRad) + (-65f) * kotlin.math.cos(tiltRad))

                drawLine(Color.White, Offset(lureX, 0f), Offset(rotatedCollarX, rotatedCollarY - 15f), 2.5f)
                drawCircle(Color(0xFFB71C1C), 6f, Offset(rotatedCollarX, rotatedCollarY - 15f))
                drawRect(Color.White, Offset(rotatedCollarX - 2f, rotatedCollarY - 15f), Size(4f, 12f))

                val hookPath = Path().apply {
                    moveTo(rotatedCollarX, rotatedCollarY - 3f)
                    quadraticBezierTo(rotatedCollarX + 6f, rotatedCollarY + 5f, rotatedCollarX + 10f, rotatedCollarY - 3f)
                    lineTo(rotatedCollarX + 12f, rotatedCollarY - 1f)
                }
                drawPath(hookPath, Color(0xFF757575), style = Stroke(2.5f))
            } else if (!isVictory) {
                drawFish(w * patrolOffset.value, h * 0.6f, patrolState)
            }

            // --- 5. CINEMATICS (The Historic Ambush) ---
            if (isHistoricalIntroPlaying) {
                val hProg = historicalProg.value
                val battleY = hookY
                val bossState = AquaState("MONSTER MUSKY", Color(0xFF4E5C4E), 3.6f, isMusky = true)
                val bossSnoutOffset = 115f * bossState.scale
                val bossSnoutYOffset = 8f * bossState.scale
                val overshootX = lureX + (80f * bossState.scale)

                if (hProg <= 0.4f) {
                    val phaseProg = hProg / 0.4f
                    val startX = -(150f * bossState.scale)
                    val easeOut = phaseProg * (2f - phaseProg)
                    val mouthX = androidx.compose.ui.util.lerp(startX, overshootX, easeOut)

                    if (hProg > 0.22f) drawLine(Color.White, Offset(lureX, 0f), Offset(mouthX, battleY), 4f)

                    withTransform({
                        translate(mouthX, battleY)
                        translate(-bossSnoutOffset, -bossSnoutYOffset)
                    }) { drawFish(0f, 0f, bossState) }
                } else {
                    val phaseProg = (hProg - 0.4f) / 0.6f
                    val driftX = androidx.compose.ui.util.lerp(overshootX, lureX + bossSnoutOffset, phaseProg)
                    val driftY = androidx.compose.ui.util.lerp(battleY, battleY + bossSnoutYOffset, phaseProg)

                    // THE FIX: Slower, head-focused thrashing
                    val thrashX = kotlin.math.sin(phaseProg * kotlin.math.PI.toFloat() * 6f) * 10f
                    val thrashY = kotlin.math.sin(phaseProg * kotlin.math.PI.toFloat() * 5f) * 20f
                    val thrashTilt = kotlin.math.sin(phaseProg * kotlin.math.PI.toFloat() * 4f) * 10f

                    val mouthX = driftX + thrashX
                    val mouthY = driftY + thrashY

                    drawLine(Color.White, Offset(lureX, 0f), Offset(mouthX, mouthY), 4f)

                    withTransform({
                        translate(mouthX, mouthY)
                        rotate(thrashTilt, pivot = Offset.Zero)
                        translate(-bossSnoutOffset, -bossSnoutYOffset)
                    }) { drawFish(0f, 0f, bossState) }
                }
            }

            // --- 6. ACTION EVENTS (Live Gameplay) ---
            if (isLiveActionPlaying) {
                val prog = strikeProgress.value
                when {
                    animOutcome == ShotOutcome.SUNK && animIsPlayer -> {
                        val battleY = hookY
                        val snoutYOffset = 8f * actionState.scale

                        if (actionState.isMusky) {
                            val overshootX = lureX + (80f * actionState.scale)

                            if (prog <= 0.35f) {
                                val phaseProg = prog / 0.35f
                                val startX = -(150f * actionState.scale)
                                val easeOut = phaseProg * (2f - phaseProg)
                                val mouthX = androidx.compose.ui.util.lerp(startX, overshootX, easeOut)

                                if (prog > 0.3f) drawLine(Color.White, Offset(lureX, 0f), Offset(mouthX, battleY), 4f)

                                withTransform({
                                    translate(mouthX, battleY)
                                    translate(-snoutOffset, -snoutYOffset)
                                }) { drawFish(0f, 0f, actionState) }
                            } else {
                                val phaseProg = (prog - 0.35f) / 0.65f
                                val driftX = androidx.compose.ui.util.lerp(overshootX, lureX + snoutOffset, phaseProg)
                                val driftY = androidx.compose.ui.util.lerp(battleY, battleY + snoutYOffset, phaseProg)

                                // THE FIX: Slower, head-focused thrashing
                                val thrashX = kotlin.math.sin(phaseProg * kotlin.math.PI.toFloat() * 6f) * 10f
                                val thrashY = kotlin.math.sin(phaseProg * kotlin.math.PI.toFloat() * 5f) * 20f
                                val thrashTilt = kotlin.math.sin(phaseProg * kotlin.math.PI.toFloat() * 4f) * 10f

                                val mouthX = driftX + thrashX
                                val mouthY = driftY + thrashY

                                drawLine(Color.White, Offset(lureX, 0f), Offset(mouthX, mouthY), 4f)

                                withTransform({
                                    translate(mouthX, mouthY)
                                    rotate(thrashTilt, pivot = Offset.Zero)
                                    translate(-snoutOffset, -snoutYOffset)
                                }) { drawFish(0f, 0f, actionState) }
                            }
                        } else {
                            if (prog <= 0.35f) {
                                val phaseProg = prog / 0.35f
                                val startX = w + snoutOffset
                                val targetX = lureX
                                val mouthX = androidx.compose.ui.util.lerp(startX, targetX, phaseProg)

                                withTransform({
                                    translate(mouthX, battleY)
                                    scale(-1f, 1f, pivot = Offset.Zero)
                                    translate(-snoutOffset, -snoutYOffset)
                                }) { drawFish(0f, 0f, actionState) }
                            } else if (prog <= 0.55f) {
                                val phaseProg = (prog - 0.35f) / 0.20f
                                val thrashX = kotlin.math.sin(phaseProg * kotlin.math.PI.toFloat() * 6f) * 8f
                                val thrashTilt = kotlin.math.sin(phaseProg * kotlin.math.PI.toFloat() * 5f) * 10f
                                val mouthX = lureX + thrashX

                                drawLine(Color.White, Offset(lureX, 0f), Offset(mouthX, battleY), 3f)

                                withTransform({
                                    translate(mouthX, battleY)
                                    scale(-1f, 1f, pivot = Offset.Zero)
                                    rotate(thrashTilt, pivot = Offset.Zero)
                                    translate(-snoutOffset, -snoutYOffset)
                                }) { drawFish(0f, 0f, actionState) }
                            } else {
                                val phaseProg = (prog - 0.55f) / 0.45f
                                val easeUp = phaseProg * phaseProg
                                val pullY = androidx.compose.ui.util.lerp(battleY, -120f * actionState.scale, easeUp)
                                val wobbleX = kotlin.math.sin(phaseProg * kotlin.math.PI.toFloat() * 3f) * 15f
                                val dragTilt = androidx.compose.ui.util.lerp(0f, -70f, easeUp)

                                drawLine(Color.White, Offset(lureX, 0f), Offset(lureX + wobbleX, pullY), 3f)

                                withTransform({
                                    translate(lureX + wobbleX, pullY)
                                    scale(-1f, 1f, pivot = Offset.Zero)
                                    rotate(dragTilt, pivot = Offset.Zero)
                                    translate(-snoutOffset, -snoutYOffset)
                                }) { drawFish(0f, 0f, actionState) }
                            }
                        }
                    }
                    animOutcome == ShotOutcome.SUNK && !animIsPlayer -> {
                        drawRect(Color.Red.copy(alpha = 0.3f * prog))
                        for (i in 0..20) {
                            val glY = kotlin.random.Random.nextFloat() * h
                            drawLine(Color.White.copy(alpha = 0.8f), Offset(0f, glY), Offset(w, glY), 4f)
                        }
                    }
                    animOutcome == ShotOutcome.HIT && animIsPlayer -> {
                        val fishLength = 120f * actionState.scale
                        val swipeX = when {
                            prog < 0.3f -> androidx.compose.ui.util.lerp(w + fishLength, lureX + (60f * actionState.scale), prog / 0.3f)
                            prog < 0.7f -> {
                                val p = (prog - 0.3f) / 0.4f
                                val smoothP = p * p * (3f - 2f * p)
                                androidx.compose.ui.util.lerp(lureX + (60f * actionState.scale), lureX - (60f * actionState.scale), smoothP)
                            }
                            else -> androidx.compose.ui.util.lerp(lureX - (60f * actionState.scale), -fishLength, (prog - 0.7f) / 0.3f)
                        }
                        withTransform({
                            translate(swipeX, hookY)
                            scale(-1f, 1f, pivot = Offset.Zero)
                        }) { drawFish(0f, 0f, actionState) }
                    }
                    animOutcome == ShotOutcome.HIT && !animIsPlayer -> {
                        drawRect(Color.White.copy(alpha = 0.1f * prog))
                        if (prog in 0.1f..0.8f) {
                            for (i in 0..12) {
                                val glY = kotlin.random.Random.nextFloat() * h
                                drawLine(Color.White.copy(alpha = 0.6f), Offset(0f, glY), Offset(w, glY), 3f)
                            }
                        }
                    }
                    else -> {
                        val bootY = -100f + (h + 100f) * prog
                        val bootX = if (animIsPlayer) (w * 0.25f) + (w * 0.1f) * prog else (w * 0.70f) - (w * 0.1f) * prog
                        val tumbleAngle = if (animIsPlayer) prog * 120f else -prog * 120f

                        withTransform({
                            translate(bootX, bootY)
                            rotate(tumbleAngle, pivot = Offset(35f, 40f))
                            if (!animIsPlayer) scale(-1f, 1f, pivot = Offset(35f, 40f))
                        }) {
                            drawRoundRect(Color(0xFF5D4037), Offset(0f,0f), Size(45f, 75f), CornerRadius(8f))
                            drawRoundRect(Color(0xFF5D4037), Offset(0f, 55f), Size(70f, 25f), CornerRadius(8f))
                            drawRoundRect(Color.Black.copy(alpha = 0.5f), Offset(0f,0f), Size(45f, 75f), CornerRadius(8f), style = Stroke(3f))
                            drawRoundRect(Color.Black.copy(alpha = 0.5f), Offset(0f, 55f), Size(70f, 25f), CornerRadius(8f), style = Stroke(3f))
                        }
                    }
                }
            }
        }

        // --- 7. HUD LAYER ---
        val isBotSinkingYou = activeEvent?.outcome == ShotOutcome.SUNK && activeEvent?.isPlayerTurn == false && strikeProgress.value > 0.8f

        if (isBotSinkingYou) {
            Box(modifier = Modifier.matchParentSize().background(Color.Red.copy(alpha = 0.4f))) {
                Text("SIGNAL LOST\nTRANSDUCER DISCONNECTED", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = Color.White, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.align(Alignment.Center))
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(8.dp)) { drawCircle(Color.Red.copy(alpha = if (blink > 0.5f) 1f else 0.2f)) }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("REC", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                val isWinningState = command == PlaybackCommand.PLAY_WIN_CINEMATIC || command == PlaybackCommand.PLAY_WIN_LIVE || probability >= 1f
                val displaySpecies = if (isWinningState) "MONSTER MUSKY" else state.species
                Text("TARGET: $displaySpecies", color = Color(0xFF00E676), fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 4.dp))
            }
            Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    val currentStat = remember(commandKey, stats) { stats.randomOrNull() }
                    val actualStatName = currentStat?.first?.uppercase() ?: "SYS"
                    val statValue = if (currentStat != null) {
                        val pureNumber = currentStat.second.filter { it.isDigit() || it == '.' }
                        if (pureNumber.isNotEmpty()) "$pureNumber°F" else "34.2°F"
                    } else "34.2°F"

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("TEMP", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("[$actualStatName]", color = Color.Gray.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 6.sp)
                    }
                    Text(statValue, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DEPTH", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 8.sp)
                    Text("${(animatedProb * 100).roundToInt()} ft", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}