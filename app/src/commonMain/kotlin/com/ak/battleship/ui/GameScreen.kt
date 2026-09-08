package com.ak.battleship.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.lerp
import com.ak.battleship.data.Move
import com.ak.battleship.viewmodel.BattleshipViewModel
import com.ak.battleship.model.GamePhase
import com.ak.battleship.model.Ship
import com.ak.battleship.utils.reconstructFleetFromMoves
import com.ak.battleship.data.Game
import com.ak.battleship.model.ShotOutcome
import com.ak.battleship.model.isShipData
import com.ak.battleship.ui.game.FiniteDefeatOverlay
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.*
import com.ak.battleship.ai.TacticalEngine
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.text.font.FontFamily
import com.ak.battleship.model.PlaybackCommand
import com.ak.battleship.ui.analytics.SonarLoader

/**
 * PRESENTATION LAYER: The Game Screen
 * * A purely reactive UI. It observes StateFlows from the ViewModel and redraws.
 * * Contains ZERO game logic or mathematical deductions.
 */
@Composable
fun GameScreen(viewModel: BattleshipViewModel) {
    

    // THE FIX: Intercept the Android System Back Gesture
    PlatformBackHandler {
        viewModel.pauseGame()
    }

    // --- STATE OBSERVATION ---
    val isOffense = viewModel.isOffenseMode
    val phase = viewModel.currentPhase

    // 1. The absolute truth. No buffers, no hidden pegs!
    val moves by viewModel.displayMoves.collectAsState()

    val winProb by viewModel.liveWinProbability.collectAsState()
    val widgetTheme by viewModel.currentWidgetTheme.collectAsState()
    val game = viewModel.currentGame

    val isPlaybackMode = viewModel.isPlaybackMode
    val playbackIndex = viewModel.playbackIndex
    val playbackMaxIndex = viewModel.playbackMaxIndex

    // --- DERIVED UI STATE ---
    val dbOffense = if (viewModel.activePlayer == 2) !isOffense else isOffense
    val isBotGame = game?.gameMode == "Bot"
    val isPassAndPlay = game?.gameMode == "PassAndPlay"
    val isCompanion = game?.gameMode == "Companion"

    // The ViewModel dictates the absolute truth of the game state
    val hasWonMatch = game?.result == "WIN" || viewModel.pendingResult == "WIN"
    val hasLostMatch = game?.result == "LOSS" || viewModel.pendingResult == "LOSS"
    val isGameOverNow = hasWonMatch || hasLostMatch

    // NEW: Only show the victory banner if we are at the very end of the timeline!
    val isAtEndOfTime = !isPlaybackMode || playbackIndex == -1 || playbackIndex == playbackMaxIndex
    val showGameOverBanner = isGameOverNow && phase != GamePhase.OPPONENT_SHIPS && isAtEndOfTime

    // --- TIME MACHINE TAB AUTO-TRACKING ---
    LaunchedEffect(moves, isPlaybackMode) {
        if (isPlaybackMode) {
            // Find the very last shot that was fired in the current timeline
            val lastCombatMove = moves.lastOrNull { !isShipData(it.result) }

            if (lastCombatMove != null) {
                // Instantly flip the tab to match the board!
                viewModel.setOffenseTab(lastCombatMove.isOffense)
            }
        }
    }

    var selectedCell by remember(viewModel.currentGameId) { mutableStateOf<Pair<Int, Int>?>(null) }
    

    // --- ASYNC TICKER DATA ---
    var tickerStats by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(viewModel.currentGameId, moves) {
        val (basicStats, wildStats) = viewModel.getStats(
            playerName = viewModel.currentPlayer,
            opponentName = game?.opponentName
        )
        tickerStats = listOf(
            Pair("WIN RATE", basicStats["Win Rate"] ?: "N/A"),
            Pair("STRK ACC", basicStats["Strike Accuracy"] ?: "N/A"),
            Pair("OPP ACC", basicStats["Opp. Accuracy"] ?: "N/A"),
            Pair("1ST BLD WINS", wildStats["First Blood Win %"] ?: "N/A"),
            Pair("CLUTCH WINS", wildStats["Clutch Victories"] ?: "0"),
            Pair("FLAWLESS", wildStats["Flawless Victories"] ?: "0")
        )
    }

    // --- FLEET RESOLUTION ---
    val activeFleet = remember(moves, phase, isOffense, viewModel.activePlayer, viewModel.placementFleet.toList(), isGameOverNow, isBotGame) {
        when {
            phase == GamePhase.PLACEMENT || phase == GamePhase.OPPONENT_SHIPS -> viewModel.placementFleet.toList()
            isGameOverNow && isOffense && isBotGame -> {
                if (viewModel.botSecretFleet.isNotEmpty()) viewModel.botSecretFleet
                else reconstructFleetFromMoves(moves.filter { it.isOffense == dbOffense })
            }
            phase == GamePhase.BATTLE && isOffense && !isGameOverNow -> emptyList()
            else -> reconstructFleetFromMoves(moves.filter { it.isOffense == dbOffense })
        }
    }

    // --- DYNAMIC X-RAY FOR TIME MACHINE ---
    val activeDiagnosticMap = remember(moves, viewModel.isDiagnosticVisible, isBotGame, game?.opponentName) {
        if (viewModel.isDiagnosticVisible && isBotGame) {
            val botMovesSoFar = moves.filter { !it.isOffense && !isShipData(it.result) }
            TacticalEngine.getLiveDiagnostics(game?.opponentName ?: "Unknown", botMovesSoFar)
        } else null
    }

    // NEW: Grab the bot's internal target list!
    val activeLivingFleet = remember(moves, viewModel.isDiagnosticVisible, isBotGame, game?.opponentName) {
        if (viewModel.isDiagnosticVisible && isBotGame) {
            val botMovesSoFar = moves.filter { !it.isOffense && !isShipData(it.result) }
            TacticalEngine.getLiveLivingFleet(game?.opponentName ?: "Unknown", botMovesSoFar)
        } else null
    }

    val activeHeatmap = remember(
        moves.size,
        viewModel.isHeatmapVisible,
        isBotGame,
        game?.opponentName,
        viewModel.adlerOffensiveMatrix,
        viewModel.moriartyPriorMatrix // <-- THE MISSING KEY: Compose will now redraw when Moriarty finishes thinking!
    ) {
        if (viewModel.isHeatmapVisible && isBotGame) {
            val botMovesSoFar = moves.filter { !it.isOffense && !isShipData(it.result) }
            TacticalEngine.getLiveHeatmap(
                opponentName = game?.opponentName ?: "Unknown",
                moves = botMovesSoFar,
                context = null,
                gameId = game?.id ?: 0,
                adlerOffensivePrior = viewModel.adlerOffensiveMatrix,
                moriartyPrior = viewModel.moriartyPriorMatrix
            )
        } else null
    }

    // --- MAIN RENDER TREE ---
    Box(modifier = Modifier.fillMaxSize()) {
        if (phase == GamePhase.HANDOFF) {
            HandoffScreen(viewModel)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                GameTopBar(viewModel, isOffense, phase)

                val canvasRatio = if (phase == GamePhase.PLACEMENT || phase == GamePhase.OPPONENT_SHIPS) 11f / 16f else 1f
                val maxGridWidth = if (phase == GamePhase.PLACEMENT || phase == GamePhase.OPPONENT_SHIPS) 380.dp else 420.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = maxGridWidth)
                        .aspectRatio(canvasRatio),
                    contentAlignment = Alignment.Center
                ) {
                    InteractiveCanvasGrid(
                        moves = moves,
                        fleet = activeFleet,
                        selectedCell = selectedCell,
                        isOffense = isOffense,
                        dbOffense = dbOffense,
                        phase = phase,
                        isBotGame = isBotGame,
                        botHeatmap = activeHeatmap,
                        botDiagnosticMap = activeDiagnosticMap,
                        botLivingFleet = activeLivingFleet,
                        botLog = viewModel.lastBotDecision,
                        isGameOverNow = isGameOverNow,
                        isHeatmapVisible = viewModel.isHeatmapVisible,
                        isHeatmapNumbersVisible = viewModel.isHeatmapNumbersVisible,
                        isDiagnosticVisible = viewModel.isDiagnosticVisible,
                        onCellTapped = { cell ->
                            val canTapGrid = if (isBotGame || isPassAndPlay) isOffense else true
                            if (!isGameOverNow && phase == GamePhase.BATTLE && canTapGrid && !viewModel.isActionLocked) {
                                selectedCell = cell
                            }
                        },
                        onShipRotated = { idx -> val s = viewModel.placementFleet[idx]; if (s.isPlaced) viewModel.updateShipPlacement(idx, s.x, s.y, !s.isVertical) },
                        onShipMoved = { idx, col, row -> val s = viewModel.placementFleet[idx]; viewModel.updateShipPlacement(idx, col, row, s.isVertical) }
                    )

                    // NEW: Full-screen overlay for AI memory reconstruction
                    if (viewModel.isAiCalculating) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            SonarLoader(text = "DEDUCING HISTORICAL BIAS...")
                        }
                    }
                }

                if (phase == GamePhase.BATTLE) {
                    val isVictoryTheme = hasWonMatch || (isPassAndPlay && hasLostMatch)
                    val activeProbability = if (isPassAndPlay && viewModel.activePlayer == 2) 1f - winProb else winProb

                    // THE REVAMP: No more Spinup delay here. Widgets will handle it.
                    val trueTargetProb = if (showGameOverBanner) { if (isVictoryTheme) 1f else 0f } else activeProbability

                    val lastCombatMove = remember(moves) { moves.lastOrNull { !isShipData(it.result) } }
                    val currentShotOutcome = if (isPlaybackMode && lastCombatMove != null) {
                        when {
                            lastCombatMove.isSunk -> ShotOutcome.SUNK
                            lastCombatMove.result == "HIT" -> ShotOutcome.HIT
                            else -> ShotOutcome.MISS
                        }
                    } else {
                        when (viewModel.widgetShotOutcome) { "SUNK" -> ShotOutcome.SUNK; "HIT" -> ShotOutcome.HIT; else -> ShotOutcome.MISS }
                    }

                    // --- 2. SYNCHRONOUS COMMAND ENGINE ---
                    val indexTracker = remember(viewModel.currentGameId) { object { var lastIndex = -2 } }
                    if (indexTracker.lastIndex == -2) { indexTracker.lastIndex = playbackIndex }

                    val nextCommand = if (!isPlaybackMode) {
                        if (viewModel.widgetShotTriggerKey == 0) {
                            if (showGameOverBanner) {
                                if (isVictoryTheme) PlaybackCommand.PLAY_WIN_LIVE else PlaybackCommand.IDLE
                            } else PlaybackCommand.IDLE
                        } else {
                            when (viewModel.widgetShotOutcome) {
                                "SUNK" -> if (showGameOverBanner) PlaybackCommand.PLAY_WIN_LIVE else PlaybackCommand.PLAY_SUNK
                                "HIT" -> PlaybackCommand.PLAY_HIT
                                else -> PlaybackCommand.PLAY_MISS
                            }
                        }
                    } else {
                        val currentEffectiveIndex = if (playbackIndex == -1) playbackMaxIndex else playbackIndex
                        val prevEffectiveIndex = if (indexTracker.lastIndex == -1) playbackMaxIndex else indexTracker.lastIndex

                        // THE FIX: If the destination is the final winning shot, FORCE the cinematic to play,
                        // regardless of how the user got there (scrubbing or stepping).
                        if (currentEffectiveIndex == playbackMaxIndex && isVictoryTheme) {
                            PlaybackCommand.PLAY_WIN_LIVE
                        }
                        // Normal playback routing
                        else if (currentEffectiveIndex < prevEffectiveIndex) {
                            PlaybackCommand.TIME_TRAVEL_BACKWARD
                        } else if (currentEffectiveIndex - prevEffectiveIndex > 1) {
                            PlaybackCommand.PLAY_JUMP
                        } else if (currentEffectiveIndex == prevEffectiveIndex) {
                            PlaybackCommand.IDLE
                        } else {
                            if (currentShotOutcome == ShotOutcome.SUNK) {
                                PlaybackCommand.PLAY_SUNK
                            } else if (currentShotOutcome == ShotOutcome.HIT) {
                                PlaybackCommand.PLAY_HIT
                            } else {
                                PlaybackCommand.PLAY_MISS
                            }
                        }
                    }
                    indexTracker.lastIndex = playbackIndex

                    val activeCommandKey = if (isPlaybackMode) {
                        if (playbackIndex == -1) 0 else playbackIndex + 1000
                    } else {
                        viewModel.widgetShotTriggerKey
                    }

                    val activePlayerFiring = if (isPlaybackMode && lastCombatMove != null) {
                        val myDbOffense = viewModel.activePlayer != 2; lastCombatMove.isOffense == myDbOffense
                    } else { viewModel.widgetIsPlayerFiring }

                    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                        DynamicWinProbabilityWidget(
                            probability = trueTargetProb,
                            currentTheme = widgetTheme,
                            onCycleTheme = { viewModel.cycleWidgetTheme() },
                            stats = tickerStats,
                            shotOutcome = currentShotOutcome,
                            isPlayerFiring = activePlayerFiring,
                            command = nextCommand,
                            commandKey = activeCommandKey
                        )
                    }
                }

                // UI Controllers Route
                if (isPlaybackMode) {
                    PlaybackActionPanel(viewModel, playbackIndex, playbackMaxIndex)
                    MoveHistoryLog(
                        moves = moves,
                        activePlayer = viewModel.activePlayer,
                        showHeader = true,
                        isHeatmapVisible = viewModel.isHeatmapVisible,
                        isHeatmapNumbersVisible = viewModel.isHeatmapNumbersVisible,
                        isDiagnosticVisible = viewModel.isDiagnosticVisible,
                        onToggleHeatmap = { viewModel.toggleHeatmap() },
                        onToggleNumbers = { viewModel.toggleHeatmapNumbers() },
                        onToggleDiagnostic = { viewModel.toggleDiagnostic() }
                    )

                } else if (!showGameOverBanner) {
                    if (phase == GamePhase.PLACEMENT || phase == GamePhase.OPPONENT_SHIPS) {
                        PlacementActionPanel(viewModel, phase, game)
                    } else {
                        CombatActionPanel(viewModel, moves, isBotGame, isPassAndPlay, isOffense, dbOffense, isGameOverNow, selectedCell, activeFleet) { selectedCell = null }
                    }
                }

                // Draw standard history for live games
                if (!isPlaybackMode && (phase == GamePhase.BATTLE || phase == GamePhase.OPPONENT_SHIPS)) {
                    MoveHistoryLog(
                        moves = moves,
                        activePlayer = viewModel.activePlayer,
                        showHeader = true,
                        isHeatmapVisible = viewModel.isHeatmapVisible,
                        isHeatmapNumbersVisible = viewModel.isHeatmapNumbersVisible,
                        isDiagnosticVisible = viewModel.isDiagnosticVisible,
                        onToggleHeatmap = { viewModel.toggleHeatmap() },
                        onToggleNumbers = { viewModel.toggleHeatmapNumbers() },
                        onToggleDiagnostic = { viewModel.toggleDiagnostic() }
                    )
                }
            }

            // --- UPDATED GAME OVER BANNER LOGIC ---
            if (showGameOverBanner) {
                if (hasWonMatch || (isPassAndPlay && hasLostMatch)) {
                    VictoryConfettiOverlay()
                } else if (hasLostMatch) {
                    var showDefeatAnimation by remember { mutableStateOf(true) }
                    if (showDefeatAnimation) {
                        FiniteDefeatOverlay(
                            onAnimationFinished = { showDefeatAnimation = false }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// EXTRACTED MODULAR COMPONENTS
// ============================================================================

@Composable
fun HandoffScreen(viewModel: BattleshipViewModel) {
    val p1Name = viewModel.currentGame?.playerName?.replaceFirstChar { it.uppercase() } ?: "Player 1"
    val p2Name = viewModel.currentGame?.opponentName?.replaceFirstChar { it.uppercase() } ?: "Player 2"

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when {
                viewModel.isP1SetupPending -> "$p1Name: Fleet Deployment"
                viewModel.isP2SetupPending -> "$p2Name: Fleet Deployment"
                viewModel.activePlayer == 2 -> "$p2Name's Turn"
                else -> "$p1Name's Turn"
            },
            fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Pass the device to the next commander.\nNo peeking.", fontSize = 16.sp, textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = { viewModel.completeHandoff() }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Text("I am ready", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GameTopBar(viewModel: BattleshipViewModel, isOffense: Boolean, phase: GamePhase) {
    val p1Name = viewModel.currentGame?.playerName?.replaceFirstChar { it.uppercase() } ?: "Player 1"
    val p2Name = viewModel.currentGame?.opponentName?.replaceFirstChar { it.uppercase() } ?: "Player 2"

    Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { viewModel.pauseGame() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Pause and Return Home") }

        if (phase == GamePhase.PLACEMENT) {
            Text(if (viewModel.activePlayer == 1) "$p1Name: Deploy Fleet" else "$p2Name: Deploy Fleet", modifier = Modifier.weight(1f).padding(12.dp), fontWeight = FontWeight.Bold, color = Color.Gray)
        } else if (phase == GamePhase.OPPONENT_SHIPS) {
            Text("Map Opponent Ships", modifier = Modifier.weight(1f).padding(12.dp), fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
        } else {
            TabRow(selectedTabIndex = if (isOffense) 0 else 1, modifier = Modifier.weight(1f)) {
                Tab(selected = isOffense, onClick = { viewModel.setMode(true) }) { Text("Offense", modifier = Modifier.padding(12.dp)) }
                Tab(selected = !isOffense, onClick = { viewModel.setMode(false) }) { Text("Defense", modifier = Modifier.padding(12.dp)) }
            }
        }

        IconButton(onClick = { viewModel.openTutorial() }) {
            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "How to Play / Rules")
        }
    }
}

@Composable
fun PlacementActionPanel(viewModel: BattleshipViewModel, phase: GamePhase, game: Game?) {
    

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Drag from dock below. Tap on grid to rotate. Drag back to remove.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))

        if (phase == GamePhase.PLACEMENT) {
            if (game?.result == null) {
                Button(
                    onClick = { viewModel.confirmShipPlacement() },
                    enabled = viewModel.placementFleet.all { it.isPlaced },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confirm Layout") }
            }
        } else if (phase == GamePhase.OPPONENT_SHIPS) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.saveOpponentShipsAndFinish() },
                    enabled = viewModel.placementFleet.all { it.isPlaced },
                    modifier = Modifier.weight(1f)
                ) { Text("Save & Finish Game") }

                OutlinedButton(onClick = { viewModel.skipOpponentShips() }) { Text("Skip") }
            }
        }
    }
}

@Composable
fun CombatActionPanel(
    viewModel: BattleshipViewModel, moves: List<Move>, isBotGame: Boolean, isPassAndPlay: Boolean,
    isOffense: Boolean, dbOffense: Boolean, isGameOverNow: Boolean,
    selectedCell: Pair<Int, Int>?, activeFleet: List<Ship>, 
    onClearSelection: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = selectedCell?.let { "Target: ${(it.second + 65).toChar()}${it.first + 1}" } ?: "Select a target", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val hasCombatMoves = moves.any { !isShipData(it.result) }
            Button(
                onClick = { viewModel.undoLastMove(); onClearSelection() },
                enabled = !viewModel.isActionLocked && hasCombatMoves,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                modifier = Modifier.height(36.dp)
            ) { Text("Undo", fontSize = 12.sp) }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val cellAlreadyShot = selectedCell?.let { cell -> moves.any { it.isOffense == dbOffense && it.x == cell.first && it.y == cell.second && !isShipData(it.result) } } ?: false

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (isBotGame || isPassAndPlay) {
                if (isOffense) {
                    Button(
                        enabled = selectedCell != null && !isGameOverNow && !cellAlreadyShot && !viewModel.isActionLocked,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE64A19)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        onClick = {
                            selectedCell?.let { cell ->
                                if (isBotGame) viewModel.fireAtBot(cell.first, cell.second)
                                else viewModel.firePassAndPlay(cell.first, cell.second)
                            }
                            onClearSelection()
                        }
                    ) { Text(if (cellAlreadyShot) "ALREADY ENGAGED" else "FIRE!", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                        Text("Fleet Status: Read Only", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val activeTurnIsOffense = remember(moves) {
                    val combatMoves = moves.filter { !isShipData(it.result) }
                    val lastMove = combatMoves.lastOrNull()
                    if (lastMove == null) null else if (lastMove.result == "MISS") !lastMove.isOffense else lastMove.isOffense
                }
                val hasTurnControl = activeTurnIsOffense == null || activeTurnIsOffense == isOffense
                val actionsEnabled = selectedCell != null && !cellAlreadyShot

                if (!hasTurnControl) {
                    Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                        Text(if (isOffense) "Opponent's Turn (Switch to Defense tab)" else "Your Turn (Switch to Offense tab)", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                } else if (isOffense) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Button(enabled = actionsEnabled, colors = ButtonDefaults.buttonColors(Color(0xFF6650a4)), onClick = { selectedCell?.let { viewModel.recordMove(it.first, it.second, "MISS", false) }; onClearSelection() }) { Text("MISS") }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Button(enabled = actionsEnabled, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), onClick = { selectedCell?.let { viewModel.recordMove(it.first, it.second, "HIT", false) }; onClearSelection() }) { Text("HIT") }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Button(enabled = actionsEnabled, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)), onClick = { selectedCell?.let { viewModel.recordMove(it.first, it.second, "HIT", true) }; onClearSelection() }) { Text("SUNK!") }
                    }
                } else {
                    val hitShipForDefense = selectedCell?.let { cell -> activeFleet.find { it.getCells().contains(cell) } }
                    val isAutoHit = hitShipForDefense != null
                    val autoSunk = if (isAutoHit && selectedCell != null) {
                        val pastHitsCount = moves.count { it.isOffense == dbOffense && it.result == "HIT" && hitShipForDefense!!.getCells().contains(Pair(it.x, it.y)) }
                        (pastHitsCount + 1) == hitShipForDefense!!.size
                    } else false

                    val btnText = if (!actionsEnabled) "SELECT TARGET" else if (!isAutoHit) "MISS" else if (autoSunk) "SUNK!" else "HIT"
                    val btnColor = if (!actionsEnabled) Color.LightGray else if (!isAutoHit) Color(0xFF6650a4) else if (autoSunk) Color(0xFF1976D2) else Color(0xFFD32F2F)

                    Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Button(
                            enabled = actionsEnabled, colors = ButtonDefaults.buttonColors(containerColor = btnColor), modifier = Modifier.fillMaxWidth(),
                            onClick = { selectedCell?.let { cell -> viewModel.recordMove(cell.first, cell.second, if (isAutoHit) "HIT" else "MISS", autoSunk) }; onClearSelection() }
                        ) { Text(btnText, fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.MoveHistoryLog(
    moves: List<Move>,
    activePlayer: Int,
    showHeader: Boolean = true,
    isHeatmapVisible: Boolean,
    isHeatmapNumbersVisible: Boolean,
    isDiagnosticVisible: Boolean,
    onToggleHeatmap: () -> Unit,
    onToggleNumbers: () -> Unit,
    onToggleDiagnostic: () -> Unit
) {
    var isToolbarExpanded by remember { mutableStateOf(false) }

    if (showHeader) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Move History", fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(
                    visible = isToolbarExpanded,
                    enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(onClick = onToggleDiagnostic, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Toggle Structural Diagnostics",
                                tint = if (isDiagnosticVisible) Color(0xFFD32F2F) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                onToggleHeatmap()
                                if (isHeatmapVisible && isHeatmapNumbersVisible) {
                                    onToggleNumbers()
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Toggle Heatmap",
                                tint = if (isHeatmapVisible) Color(0xFFFF9800) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isHeatmapVisible,
                            enter = expandHorizontally() + fadeIn(),
                            exit = shrinkHorizontally() + fadeOut()
                        ) {
                            IconButton(onClick = onToggleNumbers, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = "Toggle Heatmap Numbers",
                                    tint = if (isHeatmapNumbersVisible) Color(0xFF1976D2) else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        isToolbarExpanded = !isToolbarExpanded
                        if (!isToolbarExpanded) {
                            if (isDiagnosticVisible) onToggleDiagnostic()
                            if (isHeatmapVisible) onToggleHeatmap()
                            if (isHeatmapNumbersVisible) onToggleNumbers()
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isToolbarExpanded) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Debug Toolbar",
                        tint = if (isToolbarExpanded) Color(0xFFD32F2F) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    val combatMoves = moves.filter { !isShipData(it.result) }.reversed()
    val groupedMoves = combatMoves.groupBy { it.turnNumber }
    val listState = rememberLazyListState()

    LaunchedEffect(moves.size) {
        if (combatMoves.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
        groupedMoves.forEach { (turnNum, turnMoves) ->
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    turnMoves.forEachIndexed { index, move ->
                        val isTopInUi = index == 0
                        val isBottomInUi = index == turnMoves.lastIndex
                        val isOnlyShot = turnMoves.size == 1
                        val color = if (move.result == "HIT" && move.isSunk) Color(0xFF1976D2) else if (move.result == "HIT") Color(0xFFD32F2F) else Color.Gray
                        val displayGrid = if (activePlayer == 2) { if (move.isOffense) "Defense" else "Offense" } else { if (move.isOffense) "Offense" else "Defense" }
                        val dotColor = if (displayGrid == "Offense") Color(0xFF64B5F6) else Color(0xFF81C784)

                        Row(
                            modifier = Modifier.fillMaxWidth().drawBehind {
                                val textWidth = 48.dp.toPx()
                                val gap = 6.dp.toPx()
                                val dynamicStartX = if (turnNum < 10) textWidth else textWidth + gap
                                val spineX = textWidth + gap + 8.dp.toPx()
                                val branchEndX = spineX + 12.dp.toPx()
                                val midY = size.height / 2f
                                if (isOnlyShot) {
                                    drawLine(Color.LightGray, Offset(dynamicStartX, midY), Offset(branchEndX, midY), 2.dp.toPx())
                                } else {
                                    val startY = if (isTopInUi) midY else 0f
                                    val endY = if (isBottomInUi) midY else size.height
                                    drawLine(Color.LightGray, Offset(spineX, startY), Offset(spineX, endY), 2.dp.toPx())
                                    val startX = if (isBottomInUi) dynamicStartX else spineX
                                    drawLine(Color.LightGray, Offset(startX, midY), Offset(branchEndX, midY), 2.dp.toPx())
                                }
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                                if (isBottomInUi) Text(text = "Turn $turnNum", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6650a4), textAlign = TextAlign.Center)
                            }
                            Spacer(modifier = Modifier.width(28.dp))
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text("Shot ${move.shotNumber}", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.weight(1.2f))
                                Row(modifier = Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically) {
                                    Canvas(modifier = Modifier.size(6.dp)) { drawCircle(color = dotColor) }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(displayGrid, fontSize = 13.sp, color = Color.Gray)
                                }
                                Text("${(move.y + 65).toChar()}${move.x + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(if (move.isSunk) "SUNK" else move.result, fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveCanvasGrid(
    moves: List<Move>,
    fleet: List<Ship>,
    selectedCell: Pair<Int, Int>?,
    isOffense: Boolean,
    dbOffense: Boolean,
    phase: GamePhase,
    isBotGame: Boolean,
    botHeatmap: Array<IntArray>?,
    isGameOverNow: Boolean,
    botDiagnosticMap: Pair<List<List<Pair<Int, Int>>>, List<Pair<Int, Int>>>?,
    botLivingFleet: List<Int>?,
    botLog: String?,
    isHeatmapVisible: Boolean,
    isHeatmapNumbersVisible: Boolean,
    isDiagnosticVisible: Boolean,
    onCellTapped: (Pair<Int, Int>) -> Unit,
    onShipRotated: (Int) -> Unit,
    onShipMoved: (Int, Float, Float) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var draggingShipIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val currentFleet by rememberUpdatedState(fleet)
    val currentPhase by rememberUpdatedState(phase)
    val currentOnCellTapped by rememberUpdatedState(onCellTapped)
    val currentOnShipRotated by rememberUpdatedState(onShipRotated)
    val currentOnShipMoved by rememberUpdatedState(onShipMoved)

    val latestPegsMap = remember(moves, dbOffense) {
        moves.filter { it.isOffense == dbOffense && !isShipData(it.result) }.groupBy { Pair(it.x, it.y) }.mapValues { (_, cellMoves) -> cellMoves.maxByOrNull { it.turnNumber } }
    }

    val densityMap = remember(botHeatmap, phase, isOffense, isBotGame, moves.size) {
        if (!isOffense && isBotGame && phase == GamePhase.BATTLE) botHeatmap else null
    }

    val maxDensityVal = remember(densityMap) {
        var runningMax = 0
        densityMap?.let { matrix -> for (c in 0..9) for (r in 0..9) { if (matrix[c][r] > runningMax) runningMax = matrix[c][r] } }
        runningMax
    }

    val heatmapTextLayouts = remember(densityMap, isHeatmapNumbersVisible) {
        val layouts = mutableMapOf<Pair<Int, Int>, androidx.compose.ui.text.TextLayoutResult>()
        if (isHeatmapNumbersVisible && densityMap != null) {
            for (col in 0..9) {
                for (row in 0..9) {
                    val drawDensity = densityMap[col][row]
                    if (drawDensity > 0) {
                        layouts[Pair(col, row)] = textMeasurer.measure(
                            text = drawDensity.toString(),
                            style = TextStyle(color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
        layouts
    }

    Canvas(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .pointerInput(Unit) {
            detectTapGestures(onTap = { offset ->
                val cellSize = size.width / 11f
                val col = (offset.x - cellSize) / cellSize
                val row = (offset.y - cellSize) / cellSize
                val tolerance = 0.4f
                if (currentPhase == GamePhase.PLACEMENT || currentPhase == GamePhase.OPPONENT_SHIPS) {
                    val clickedIndex = currentFleet.indexOfFirst { s ->
                        val endCol = s.x + if (!s.isVertical) s.size else 1
                        val endRow = s.y + if (s.isVertical) s.size else 1
                        s.isPlaced && col >= (s.x - tolerance) && col < (endCol + tolerance) && row >= (s.y - tolerance) && row < (endRow + tolerance)
                    }
                    if (clickedIndex != -1) currentOnShipRotated(clickedIndex)
                } else if (col >= 0 && col < 10 && row >= 0 && row < 10) {
                    currentOnCellTapped(Pair(col.toInt(), row.toInt()))
                }
            })
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    if (currentPhase == GamePhase.PLACEMENT || currentPhase == GamePhase.OPPONENT_SHIPS) {
                        val cellSize = size.width / 11f
                        val col = (offset.x - cellSize) / cellSize
                        val row = (offset.y - cellSize) / cellSize
                        val tolerance = 0.4f
                        val idx = currentFleet.indexOfFirst { s ->
                            val endCol = s.x + if (!s.isVertical) s.size else 1
                            val endRow = s.y + if (s.isVertical) s.size else 1
                            col >= (s.x - tolerance) && col < (endCol + tolerance) && row >= (s.y - tolerance) && row < (endRow + tolerance)
                        }
                        if (idx != -1) {
                            draggingShipIndex = idx
                            val s = currentFleet[idx]
                            val sWidth = if (s.isVertical) cellSize else cellSize * s.size
                            val sHeight = if (s.isVertical) cellSize * s.size else cellSize
                            dragOffset = offset - Offset(cellSize + s.x * cellSize, cellSize + s.y * cellSize) - Offset(sWidth / 2f, sHeight / 2f)
                        }
                    }
                },
                onDrag = { change, dragAmount -> if (draggingShipIndex != null) { change.consume(); dragOffset += dragAmount } },
                onDragEnd = {
                    draggingShipIndex?.let { idx ->
                        val s = currentFleet[idx]
                        val cellSize = size.width / 11f
                        val dragTopLeft = Offset(cellSize + s.x * cellSize, cellSize + s.y * cellSize) + dragOffset
                        currentOnShipMoved(idx, (dragTopLeft.x - cellSize) / cellSize, (dragTopLeft.y - cellSize) / cellSize)
                    }
                    draggingShipIndex = null
                    dragOffset = Offset.Zero
                },
                onDragCancel = { draggingShipIndex = null; dragOffset = Offset.Zero }
            )
        }
    ) {
        val cellSize = size.width / 11f
        val baseWaterColor = if (isOffense) Color(0xFFE3F2FD) else Color(0xFFE8F5E9)

        for (col in 0..9) {
            for (row in 0..9) {
                val topLeft = Offset(cellSize + col * cellSize, cellSize + row * cellSize)
                drawRect(color = baseWaterColor, topLeft = topLeft, size = Size(cellSize, cellSize))
                drawRect(color = Color.Black.copy(alpha = 0.3f), topLeft = topLeft, size = Size(cellSize, cellSize), style = Stroke(2f))
            }
        }

        for (i in 0..9) {
            drawText(textLayoutResult = textMeasurer.measure((i + 1).toString(), TextStyle(Color.Black, 14.sp, FontWeight.Bold)), topLeft = Offset(cellSize + (i * cellSize) + (cellSize * 0.3f), cellSize * 0.2f))
            drawText(textLayoutResult = textMeasurer.measure((i + 65).toChar().toString(), TextStyle(Color.Black, 14.sp, FontWeight.Bold)), topLeft = Offset(cellSize * 0.2f, cellSize + (i * cellSize) + (cellSize * 0.2f)))
        }

        if (phase == GamePhase.PLACEMENT || phase == GamePhase.OPPONENT_SHIPS) {
            drawRect(color = Color.LightGray.copy(alpha = 0.3f), topLeft = Offset(cellSize, cellSize * 12), size = Size(cellSize * 10, cellSize * 4))
        }

        fleet.forEachIndexed { index, ship ->
            if (index == draggingShipIndex) return@forEachIndexed
            val shipTopLeft = Offset(cellSize + ship.x * cellSize, cellSize + ship.y * cellSize)
            val sWidth = if (ship.isVertical) cellSize else cellSize * ship.size
            val sHeight = if (ship.isVertical) cellSize * ship.size else cellSize

            drawRoundRect(
                color = Color.DarkGray,
                topLeft = Offset(shipTopLeft.x + 4f, shipTopLeft.y + 4f),
                size = Size(sWidth - 8f, sHeight - 8f),
                cornerRadius = CornerRadius(16f, 16f)
            )
        }

        if (densityMap != null) {
            val actualShots = moves.filter { it.isOffense == dbOffense && (it.result == "HIT" || it.result == "MISS") }
            for (col in 0..9) {
                for (row in 0..9) {
                    val shipOnCell = fleet.find { it.isPlaced && it.getCells().contains(Pair(col, row)) }
                    val isSunk = shipOnCell?.let { s -> s.getCells().all { c -> actualShots.any { shot -> shot.x == c.first && shot.y == c.second && shot.result == "HIT" } } } ?: false
                    val isHit = actualShots.any { it.x == col && it.y == row }

                    var drawDensity = 0f
                    var shouldDraw = false

                    if (!isHit) {
                        if (densityMap[col][row] > 0) {
                            drawDensity = densityMap[col][row].toFloat()
                            shouldDraw = true
                        } else if (shipOnCell != null && !isSunk) {
                            drawDensity = 0f
                            shouldDraw = true
                        }
                    } else if (shipOnCell != null && !isSunk) {
                        val remainingHeat = shipOnCell.getCells().filter { c -> !actualShots.any { shot -> shot.x == c.first && shot.y == c.second } }.maxOfOrNull { c -> densityMap[c.first][c.second] } ?: 0
                        drawDensity = remainingHeat.toFloat()
                        shouldDraw = true
                    }

                    if (shouldDraw) {
                        val rawWeight = if (maxDensityVal > 0) (drawDensity / maxDensityVal.toFloat()) else 0f
                        val contrastWeight = rawWeight * rawWeight
                        val heatColor = if (contrastWeight < 0.5f) lerp(baseWaterColor, Color(0xFFFFC107), contrastWeight * 2f) else lerp(Color(0xFFFFC107), Color(0xFFD32F2F), (contrastWeight - 0.5f) * 2f)
                        val overlayAlpha = if (shipOnCell != null) 0.7f else 0.5f
                        drawRect(color = heatColor.copy(alpha = overlayAlpha), topLeft = Offset(cellSize + col * cellSize, cellSize + row * cellSize), size = Size(cellSize, cellSize))

                        if (isHeatmapNumbersVisible) {
                            heatmapTextLayouts[Pair(col, row)]?.let { textLayout ->
                                val textOffsetX = (cellSize + col * cellSize) + (cellSize - textLayout.size.width) / 2f
                                val textOffsetY = (cellSize + row * cellSize) + (cellSize - textLayout.size.height) / 2f
                                drawText(textLayoutResult = textLayout, topLeft = Offset(textOffsetX, textOffsetY))
                            }
                        }
                    }
                }
            }
        }

        for (col in 0..9) {
            for (row in 0..9) {
                val topLeft = Offset(cellSize + col * cellSize, cellSize + row * cellSize)
                latestPegsMap[Pair(col, row)]?.let { move ->
                    if (move.result == "HIT") {
                        val strokeColor = if (move.isSunk) Color.Blue else Color.Red
                        drawLine(strokeColor, start = Offset(topLeft.x + 8f, topLeft.y + 8f), end = Offset(topLeft.x + cellSize - 8f, topLeft.y + cellSize - 8f), strokeWidth = 6f)
                        drawLine(strokeColor, start = Offset(topLeft.x + cellSize - 8f, topLeft.y + 8f), end = Offset(topLeft.x + 8f, topLeft.y + cellSize - 8f), strokeWidth = 6f)
                    } else if (move.result == "MISS") {
                        drawCircle(Color.Black, radius = cellSize * 0.12f, center = Offset(topLeft.x + cellSize / 2, topLeft.y + cellSize / 2))
                    }
                }
                if (selectedCell?.first == col && selectedCell?.second == row) {
                    drawRect(Color.Black.copy(alpha = 0.4f), topLeft, Size(cellSize, cellSize))
                    drawRect(Color(0xFF6650a4), topLeft, Size(cellSize, cellSize), style = Stroke(4f))
                }
            }
        }

        if (isDiagnosticVisible && botDiagnosticMap != null && !isOffense) {
            val deadShips = botDiagnosticMap.first
            val activeHits = botDiagnosticMap.second

            for (deadShipPegs in deadShips) {
                if (deadShipPegs.isEmpty()) continue

                val minCol = deadShipPegs.minOf { it.first }
                val maxCol = deadShipPegs.maxOf { it.first }
                val minRow = deadShipPegs.minOf { it.second }
                val maxRow = deadShipPegs.maxOf { it.second }

                val shipTopLeft = Offset(cellSize + minCol * cellSize, cellSize + minRow * cellSize)
                val shipWidth = (maxCol - minCol + 1) * cellSize
                val shipHeight = (maxRow - minRow + 1) * cellSize

                drawRoundRect(
                    color = Color(0xFF000000),
                    topLeft = Offset(shipTopLeft.x + 4f, shipTopLeft.y + 4f),
                    size = Size(shipWidth - 8f, shipHeight - 8f),
                    cornerRadius = CornerRadius(15f, 15f),
                    style = Stroke(width = 4f)
                )
            }

            for (hit in activeHits) {
                val col = hit.first
                val row = hit.second
                val topLeft = Offset(cellSize + col * cellSize, cellSize + row * cellSize)

                drawRoundRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(topLeft.x + 4f, topLeft.y + 4f),
                    size = Size(cellSize - 8f, cellSize - 8f),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = 5f)
                )

                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                    radius = cellSize * 0.25f,
                    center = Offset(topLeft.x + cellSize / 2f, topLeft.y + cellSize / 2f),
                    style = Stroke(width = 3f)
                )
            }
        }

        if (isDiagnosticVisible && botLog != null && !isOffense) {
            val textLayout = textMeasurer.measure(
                text = botLog,
                style = TextStyle(
                    color = Color(0xFF673AB7),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                )
            )

            val startX = cellSize + 16f
            val startY = size.height - textLayout.size.height - 16f

            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(startX, startY)
            )
        }

        draggingShipIndex?.let { idx ->
            val ship = fleet[idx]
            val sWidth = if (ship.isVertical) cellSize else cellSize * ship.size
            val sHeight = if (ship.isVertical) cellSize * ship.size else cellSize
            val dragTopLeft = Offset(cellSize + ship.x * cellSize, cellSize + ship.y * cellSize) + dragOffset
            drawRoundRect(color = Color.DarkGray, topLeft = Offset(dragTopLeft.x + 4f, dragTopLeft.y + 4f), size = Size(sWidth - 8f, sHeight - 8f), cornerRadius = CornerRadius(16f, 16f))
        }
    }
}

@Composable
fun PlaybackActionPanel(viewModel: BattleshipViewModel, playbackIndex: Int, playbackMaxIndex: Int) {
    val currentIndex = if (playbackIndex == -1) playbackMaxIndex else playbackIndex
    var isAutoPlaying by remember { mutableStateOf(false) }
    var isSliderExpanded by remember { mutableStateOf(false) }
    val allMoves by viewModel.currentMoves.collectAsState()

    val combatMoves = remember(allMoves) {
        allMoves.filter { !isShipData(it.result) }.sortedWith(compareBy({ it.turnNumber }, { it.shotNumber }))
    }

    val scope = rememberCoroutineScope()
    var manualStepJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var pendingTargetIndex by remember { mutableIntStateOf(-1) }

    fun snapToTimeline(targetIndex: Int) {
        if (manualStepJob?.isActive == true) manualStepJob?.cancel()
        isAutoPlaying = false

        val safeIndex = if (targetIndex <= 0) 0 else targetIndex - 1
        val targetMove = combatMoves.getOrNull(safeIndex)
        if (targetMove != null) {
            viewModel.setOffenseTab(targetMove.isOffense)
        }

        viewModel.setPlaybackPosition(targetIndex)
    }

    fun fastForwardPendingStep() {
        if (manualStepJob?.isActive == true) {
            manualStepJob?.cancel()
            val targetMove = combatMoves.getOrNull(pendingTargetIndex - 1)
            if (targetMove != null) viewModel.setOffenseTab(targetMove.isOffense)
            viewModel.setPlaybackPosition(pendingTargetIndex)
        }
    }

    LaunchedEffect(isAutoPlaying) {
        if (manualStepJob?.isActive == true) manualStepJob?.cancel()

        while (isAutoPlaying) {
            val liveIndex = if (viewModel.playbackIndex == -1) viewModel.playbackMaxIndex else viewModel.playbackIndex

            if (liveIndex < viewModel.playbackMaxIndex) {
                val nextMove = combatMoves.getOrNull(liveIndex)

                if (nextMove != null && viewModel.isOffenseMode != nextMove.isOffense) {
                    viewModel.setOffenseTab(nextMove.isOffense)
                    delay(600)
                }

                viewModel.setPlaybackPosition(liveIndex + 1)
                delay(800)
            } else {
                isAutoPlaying = false
            }
        }
    }

    val progressFraction = if (playbackMaxIndex > 0) currentIndex.toFloat() / playbackMaxIndex.toFloat() else 0f

    val outerGap by animateDpAsState(targetValue = if (isSliderExpanded) 0.dp else 36.dp, label = "outerGapGlide")
    val innerGap by animateDpAsState(targetValue = if (isSliderExpanded) 8.dp else 8.dp, label = "innerGapGlide")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(horizontalArrangement = Arrangement.spacedBy(outerGap)) {
                IconButton(onClick = { snapToTimeline(0) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "First Shot", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { if (currentIndex > 0) snapToTimeline(currentIndex - 1) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Step Back", modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.width(innerGap))

            AnimatedContent(
                targetState = isSliderExpanded,
                modifier = Modifier.weight(1f),
                transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.9f)).togetherWith(fadeOut(tween(300)) + scaleOut(targetScale = 0.9f)) },
                label = "CenterControlMorph"
            ) { expanded ->
                if (expanded) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = currentIndex.toFloat(),
                            onValueChange = { snapToTimeline(it.roundToInt()) },
                            valueRange = 0f..playbackMaxIndex.toFloat(),
                            steps = if (playbackMaxIndex > 0) playbackMaxIndex - 1 else 0,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        IconButton(onClick = { isSliderExpanded = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Collapse to Halo")
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(28.dp))
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { progressFraction }, modifier = Modifier.size(52.dp), color = Color(0xFF6650a4), trackColor = Color.LightGray.copy(alpha = 0.5f), strokeWidth = 3.dp
                                )
                                FilledIconButton(
                                    onClick = {
                                        if (manualStepJob?.isActive == true) manualStepJob?.cancel()
                                        if (currentIndex == playbackMaxIndex) viewModel.setPlaybackPosition(0)
                                        isAutoPlaying = !isAutoPlaying
                                    },
                                    modifier = Modifier.size(40.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF6650a4), contentColor = Color.White)
                                ) {
                                    Icon(if (isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", modifier = Modifier.size(20.dp))
                                }
                            }
                            IconButton(onClick = { isSliderExpanded = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Expand to Slider")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(innerGap))

            Row(horizontalArrangement = Arrangement.spacedBy(outerGap)) {

                IconButton(
                    onClick = {
                        val baseIndex = if (manualStepJob?.isActive == true) pendingTargetIndex else currentIndex

                        if (baseIndex < playbackMaxIndex) {
                            isAutoPlaying = false

                            fastForwardPendingStep()

                            val nextTarget = baseIndex + 1
                            pendingTargetIndex = nextTarget

                            manualStepJob = scope.launch {

                                delay(50)

                                val nextMove = combatMoves.getOrNull(nextTarget - 1)

                                if (nextMove != null && viewModel.isOffenseMode != nextMove.isOffense) {
                                    viewModel.setOffenseTab(nextMove.isOffense)
                                    delay(600)
                                } else {
                                    delay(150)
                                }

                                viewModel.setPlaybackPosition(nextTarget)
                            }
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "Next Shot", modifier = Modifier.size(22.dp))
                }

                IconButton(onClick = { snapToTimeline(-1) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Final Blow", modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
